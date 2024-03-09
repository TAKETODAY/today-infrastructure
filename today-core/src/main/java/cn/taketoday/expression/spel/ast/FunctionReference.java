/*
 * Copyright 2017 - 2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.expression.spel.ast;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.StringJoiner;

import cn.taketoday.bytecode.MethodVisitor;
import cn.taketoday.bytecode.core.CodeFlow;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.expression.EvaluationException;
import cn.taketoday.expression.TypeConverter;
import cn.taketoday.expression.TypedValue;
import cn.taketoday.expression.spel.ExpressionState;
import cn.taketoday.expression.spel.SpelEvaluationException;
import cn.taketoday.expression.spel.SpelMessage;
import cn.taketoday.expression.spel.support.ReflectionHelper;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ReflectionUtils;

/**
 * A function reference is of the form "#someFunction(a,b,c)". Functions may be defined
 * in the context prior to the expression being evaluated. Functions may also be static
 * Java methods, registered in the context prior to invocation of the expression.
 *
 * <p>Functions are very simplistic. The arguments are not part of the definition
 * (right now), so the names must be unique.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class FunctionReference extends SpelNodeImpl {

  private final String name;

  // Captures the most recently used method for the function invocation *if* the method
  // can safely be used for compilation (i.e. no argument conversion is going on)
  @Nullable
  private volatile Method method;

  public FunctionReference(String functionName, int startPos, int endPos, SpelNodeImpl... arguments) {
    super(startPos, endPos, arguments);
    this.name = functionName;
  }

  @Override
  public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
    TypedValue value = state.lookupVariable(this.name);
    if (value == TypedValue.NULL) {
      throw new SpelEvaluationException(getStartPosition(), SpelMessage.FUNCTION_NOT_DEFINED, this.name);
    }
    Object function = value.getValue();

    // Static Java method registered via a Method.
    // Note: "javaMethod" cannot be named "method" due to a bug in Checkstyle.
    if (function instanceof Method javaMethod) {
      try {
        return executeFunctionViaMethod(state, javaMethod);
      }
      catch (SpelEvaluationException ex) {
        ex.setPosition(getStartPosition());
        throw ex;
      }
    }

    // Function registered via a MethodHandle.
    if (function instanceof MethodHandle methodHandle) {
      try {
        return executeFunctionViaMethodHandle(state, methodHandle);
      }
      catch (SpelEvaluationException ex) {
        ex.setPosition(getStartPosition());
        throw ex;
      }
    }

    // Neither a Method nor a MethodHandle?
    throw new SpelEvaluationException(
            SpelMessage.FUNCTION_REFERENCE_CANNOT_BE_INVOKED, this.name, value.getClass());
  }

  /**
   * Execute a function represented as a {@link Method}.
   *
   * @param state the expression evaluation state
   * @param method the method to invoke
   * @return the return value of the invoked Java method
   * @throws EvaluationException if there is any problem invoking the method
   */
  private TypedValue executeFunctionViaMethod(ExpressionState state, Method method) throws EvaluationException {
    Object[] functionArgs = getArguments(state);

    if (!method.isVarArgs()) {
      int declaredParamCount = method.getParameterCount();
      if (declaredParamCount != functionArgs.length) {
        throw new SpelEvaluationException(SpelMessage.INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNCTION,
                this.name, functionArgs.length, declaredParamCount);
      }
    }
    if (!Modifier.isStatic(method.getModifiers())) {
      throw new SpelEvaluationException(getStartPosition(),
              SpelMessage.FUNCTION_MUST_BE_STATIC, ClassUtils.getQualifiedMethodName(method), this.name);
    }

    // Convert arguments if necessary and remap them for varargs if required
    TypeConverter converter = state.getEvaluationContext().getTypeConverter();
    boolean argumentConversionOccurred = ReflectionHelper.convertAllArguments(converter, functionArgs, method);
    if (method.isVarArgs()) {
      functionArgs = ReflectionHelper.setupArgumentsForVarargsInvocation(
              method.getParameterTypes(), functionArgs);
    }
    boolean compilable = false;

    try {
      ReflectionUtils.makeAccessible(method);
      Object result = method.invoke(method.getClass(), functionArgs);
      compilable = !argumentConversionOccurred;
      return new TypedValue(result, new TypeDescriptor(new MethodParameter(method, -1)).narrow(result));
    }
    catch (Exception ex) {
      throw new SpelEvaluationException(getStartPosition(), ex, SpelMessage.EXCEPTION_DURING_FUNCTION_CALL,
              this.name, ex.getMessage());
    }
    finally {
      if (compilable) {
        this.exitTypeDescriptor = CodeFlow.toDescriptor(method.getReturnType());
        this.method = method;
      }
      else {
        this.exitTypeDescriptor = null;
        this.method = null;
      }
    }
  }

  /**
   * Execute a function represented as {@link MethodHandle}.
   * <p>Method types that take no arguments (fully bound handles or static methods
   * with no parameters) can use {@link MethodHandle#invoke()} which is the most
   * efficient. Otherwise, {@link MethodHandle#invokeWithArguments()} is used.
   *
   * @param state the expression evaluation state
   * @param methodHandle the method handle to invoke
   * @return the return value of the invoked Java method
   * @throws EvaluationException if there is any problem invoking the method
   */
  private TypedValue executeFunctionViaMethodHandle(ExpressionState state, MethodHandle methodHandle) throws EvaluationException {
    Object[] functionArgs = getArguments(state);
    MethodType declaredParams = methodHandle.type();
    int spelParamCount = functionArgs.length;
    int declaredParamCount = declaredParams.parameterCount();

    boolean isSuspectedVarargs = declaredParams.lastParameterType().isArray();

    if (spelParamCount < declaredParamCount || (spelParamCount > declaredParamCount && !isSuspectedVarargs)) {
      // incorrect number, including more arguments and not a vararg
      // perhaps a subset of arguments was provided but the MethodHandle wasn't bound?
      throw new SpelEvaluationException(SpelMessage.INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNCTION,
              this.name, functionArgs.length, declaredParamCount);
    }

    // simplest case: the MethodHandle is fully bound or represents a static method with no params:
    if (declaredParamCount == 0) {
      try {
        return new TypedValue(methodHandle.invoke());
      }
      catch (Throwable ex) {
        throw new SpelEvaluationException(getStartPosition(), ex, SpelMessage.EXCEPTION_DURING_FUNCTION_CALL,
                this.name, ex.getMessage());
      }
      finally {
        // Note: we consider MethodHandles not compilable
        this.exitTypeDescriptor = null;
        this.method = null;
      }
    }

    // more complex case, we need to look at conversion and vararg repacking
    Integer varArgPosition = null;
    if (isSuspectedVarargs) {
      varArgPosition = declaredParamCount - 1;
    }
    TypeConverter converter = state.getEvaluationContext().getTypeConverter();
    ReflectionHelper.convertAllMethodHandleArguments(converter, functionArgs, methodHandle, varArgPosition);

    if (isSuspectedVarargs && declaredParamCount == 1) {
      // we only repack the varargs if it is the ONLY argument
      functionArgs = ReflectionHelper.setupArgumentsForVarargsInvocation(
              methodHandle.type().parameterArray(), functionArgs);
    }

    try {
      return new TypedValue(methodHandle.invokeWithArguments(functionArgs));
    }
    catch (Throwable ex) {
      throw new SpelEvaluationException(getStartPosition(), ex, SpelMessage.EXCEPTION_DURING_FUNCTION_CALL,
              this.name, ex.getMessage());
    }
    finally {
      // Note: we consider MethodHandles not compilable
      this.exitTypeDescriptor = null;
      this.method = null;
    }
  }

  @Override
  public String toStringAST() {
    StringJoiner sj = new StringJoiner(",", "(", ")");
    for (SpelNodeImpl child : children) {
      sj.add(child.toStringAST());
    }
    return '#' + this.name + sj;
  }

  /**
   * Compute the arguments to the function, they are the children of this expression node.
   *
   * @return an array of argument values for the function call
   */
  private Object[] getArguments(ExpressionState state) throws EvaluationException {
    // Compute arguments to the function
    Object[] arguments = new Object[getChildCount()];
    for (int i = 0; i < arguments.length; i++) {
      arguments[i] = this.children[i].getValueInternal(state).getValue();
    }
    return arguments;
  }

  @Override
  public boolean isCompilable() {
    Method method = this.method;
    if (method == null) {
      return false;
    }
    int methodModifiers = method.getModifiers();
    if (!Modifier.isStatic(methodModifiers) || !Modifier.isPublic(methodModifiers) ||
            !Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
      return false;
    }
    for (SpelNodeImpl child : this.children) {
      if (!child.isCompilable()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void generateCode(MethodVisitor mv, CodeFlow cf) {
    Method method = this.method;
    Assert.state(method != null, "No method handle");
    String classDesc = method.getDeclaringClass().getName().replace('.', '/');
    generateCodeForArguments(mv, cf, method, this.children);
    mv.visitMethodInsn(INVOKESTATIC, classDesc, method.getName(),
            CodeFlow.createSignatureDescriptor(method), false);
    cf.pushDescriptor(this.exitTypeDescriptor);
  }

}
