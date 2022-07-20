/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.expression.spel.ast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.bytecode.Label;
import cn.taketoday.core.bytecode.MethodVisitor;
import cn.taketoday.expression.AccessException;
import cn.taketoday.expression.EvaluationContext;
import cn.taketoday.expression.EvaluationException;
import cn.taketoday.expression.ExpressionInvocationTargetException;
import cn.taketoday.expression.MethodExecutor;
import cn.taketoday.expression.MethodResolver;
import cn.taketoday.expression.TypedValue;
import cn.taketoday.core.bytecode.core.CodeFlow;
import cn.taketoday.expression.spel.ExpressionState;
import cn.taketoday.expression.spel.SpelEvaluationException;
import cn.taketoday.expression.spel.SpelMessage;
import cn.taketoday.expression.spel.support.ReflectiveMethodExecutor;
import cn.taketoday.expression.spel.support.ReflectiveMethodResolver;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;

/**
 * Expression language AST node that represents a method reference.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 4.0
 */
public class MethodReference extends SpelNodeImpl {

  private final String name;

  private final boolean nullSafe;

  @Nullable
  private String originalPrimitiveExitTypeDescriptor;

  @Nullable
  private volatile CachedMethodExecutor cachedExecutor;

  public MethodReference(boolean nullSafe, String methodName, int startPos, int endPos, SpelNodeImpl... arguments) {
    super(startPos, endPos, arguments);
    this.name = methodName;
    this.nullSafe = nullSafe;
  }

  public final String getName() {
    return this.name;
  }

  @Override
  protected ValueRef getValueRef(ExpressionState state) throws EvaluationException {
    Object[] arguments = getArguments(state);
    if (state.getActiveContextObject().getValue() == null) {
      throwIfNotNullSafe(getArgumentTypes(arguments));
      return ValueRef.NullValueRef.INSTANCE;
    }
    return new MethodValueRef(state, arguments);
  }

  @Override
  public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
    EvaluationContext evaluationContext = state.getEvaluationContext();
    Object value = state.getActiveContextObject().getValue();
    TypeDescriptor targetType = state.getActiveContextObject().getTypeDescriptor();
    Object[] arguments = getArguments(state);
    TypedValue result = getValueInternal(evaluationContext, value, targetType, arguments);
    updateExitTypeDescriptor();
    return result;
  }

  private TypedValue getValueInternal(EvaluationContext evaluationContext,
          @Nullable Object value, @Nullable TypeDescriptor targetType, Object[] arguments) {

    List<TypeDescriptor> argumentTypes = getArgumentTypes(arguments);
    if (value == null) {
      throwIfNotNullSafe(argumentTypes);
      return TypedValue.NULL;
    }

    MethodExecutor executorToUse = getCachedExecutor(evaluationContext, value, targetType, argumentTypes);
    if (executorToUse != null) {
      try {
        return executorToUse.execute(evaluationContext, value, arguments);
      }
      catch (AccessException ex) {
        // Two reasons this can occur:
        // 1. the method invoked actually threw a real exception
        // 2. the method invoked was not passed the arguments it expected and
        //    has become 'stale'

        // In the first case we should not retry, in the second case we should see
        // if there is a better suited method.

        // To determine the situation, the AccessException will contain a cause.
        // If the cause is an InvocationTargetException, a user exception was
        // thrown inside the method. Otherwise the method could not be invoked.
        throwSimpleExceptionIfPossible(value, ex);

        // At this point we know it wasn't a user problem so worth a retry if a
        // better candidate can be found.
        this.cachedExecutor = null;
      }
    }

    // either there was no accessor or it no longer existed
    executorToUse = findAccessorForMethod(argumentTypes, value, evaluationContext);
    this.cachedExecutor = new CachedMethodExecutor(
            executorToUse, (value instanceof Class ? (Class<?>) value : null), targetType, argumentTypes);
    try {
      return executorToUse.execute(evaluationContext, value, arguments);
    }
    catch (AccessException ex) {
      // Same unwrapping exception handling as above in above catch block
      throwSimpleExceptionIfPossible(value, ex);
      throw new SpelEvaluationException(getStartPosition(), ex,
              SpelMessage.EXCEPTION_DURING_METHOD_INVOCATION, this.name,
              value.getClass().getName(), ex.getMessage());
    }
  }

  private void throwIfNotNullSafe(List<TypeDescriptor> argumentTypes) {
    if (!this.nullSafe) {
      throw new SpelEvaluationException(getStartPosition(),
              SpelMessage.METHOD_CALL_ON_NULL_OBJECT_NOT_ALLOWED,
              FormatHelper.formatMethodForMessage(this.name, argumentTypes));
    }
  }

  private Object[] getArguments(ExpressionState state) {
    int i = 0;
    Object[] arguments = new Object[getChildCount()];
    for (SpelNodeImpl child : children) {
      // Make the root object the active context again for evaluating the parameter expressions
      try {
        state.pushActiveContextObject(state.getScopeRootContextObject());
        arguments[i++] = child.getValueInternal(state).getValue();
      }
      finally {
        state.popActiveContextObject();
      }
    }
    return arguments;
  }

  private List<TypeDescriptor> getArgumentTypes(Object... arguments) {
    ArrayList<TypeDescriptor> descriptors = new ArrayList<>(arguments.length);
    for (Object argument : arguments) {
      descriptors.add(TypeDescriptor.fromObject(argument));
    }
    return Collections.unmodifiableList(descriptors);
  }

  @Nullable
  private MethodExecutor getCachedExecutor(EvaluationContext evaluationContext, Object value,
          @Nullable TypeDescriptor target, List<TypeDescriptor> argumentTypes) {

    List<MethodResolver> methodResolvers = evaluationContext.getMethodResolvers();
    if (methodResolvers.size() != 1 || !(methodResolvers.get(0) instanceof ReflectiveMethodResolver)) {
      // Not a default ReflectiveMethodResolver - don't know whether caching is valid
      return null;
    }

    CachedMethodExecutor executorToCheck = this.cachedExecutor;
    if (executorToCheck != null && executorToCheck.isSuitable(value, target, argumentTypes)) {
      return executorToCheck.get();
    }
    this.cachedExecutor = null;
    return null;
  }

  private MethodExecutor findAccessorForMethod(List<TypeDescriptor> argumentTypes, Object targetObject,
          EvaluationContext evaluationContext) throws SpelEvaluationException {

    AccessException accessException = null;
    for (MethodResolver methodResolver : evaluationContext.getMethodResolvers()) {
      try {
        MethodExecutor methodExecutor = methodResolver.resolve(
                evaluationContext, targetObject, this.name, argumentTypes);
        if (methodExecutor != null) {
          return methodExecutor;
        }
      }
      catch (AccessException ex) {
        accessException = ex;
        break;
      }
    }

    String method = FormatHelper.formatMethodForMessage(this.name, argumentTypes);
    String className = FormatHelper.formatClassNameForMessage(
            targetObject instanceof Class ? ((Class<?>) targetObject) : targetObject.getClass());
    if (accessException != null) {
      throw new SpelEvaluationException(
              getStartPosition(), accessException, SpelMessage.PROBLEM_LOCATING_METHOD, method, className);
    }
    else {
      throw new SpelEvaluationException(getStartPosition(), SpelMessage.METHOD_NOT_FOUND, method, className);
    }
  }

  /**
   * Decode the AccessException, throwing a lightweight evaluation exception or,
   * if the cause was a RuntimeException, throw the RuntimeException directly.
   */
  private void throwSimpleExceptionIfPossible(Object value, AccessException ex) {
    if (ex.getCause() instanceof InvocationTargetException) {
      Throwable rootCause = ex.getCause().getCause();
      if (rootCause instanceof RuntimeException) {
        throw (RuntimeException) rootCause;
      }
      throw new ExpressionInvocationTargetException(getStartPosition(),
              "A problem occurred when trying to execute method '" + this.name +
                      "' on object of type [" + value.getClass().getName() + "]", rootCause);
    }
  }

  private void updateExitTypeDescriptor() {
    CachedMethodExecutor executorToCheck = this.cachedExecutor;
    if (executorToCheck != null && executorToCheck.get() instanceof ReflectiveMethodExecutor executor) {
      Method method = executor.getMethod();
      String descriptor = CodeFlow.toDescriptor(method.getReturnType());
      if (this.nullSafe && CodeFlow.isPrimitive(descriptor)) {
        this.originalPrimitiveExitTypeDescriptor = descriptor;
        this.exitTypeDescriptor = CodeFlow.toBoxedDescriptor(descriptor);
      }
      else {
        this.exitTypeDescriptor = descriptor;
      }
    }
  }

  @Override
  public String toStringAST() {
    StringJoiner sj = new StringJoiner(",", "(", ")");
    for (SpelNodeImpl child : children) {
      sj.add(child.toStringAST());
    }
    return this.name + sj;
  }

  /**
   * A method reference is compilable if it has been resolved to a reflectively accessible method
   * and the child nodes (arguments to the method) are also compilable.
   */
  @Override
  public boolean isCompilable() {
    CachedMethodExecutor executorToCheck = this.cachedExecutor;
    if (executorToCheck == null
            || executorToCheck.hasProxyTarget()
            || !(executorToCheck.get() instanceof ReflectiveMethodExecutor executor)) {
      return false;
    }

    for (SpelNodeImpl child : this.children) {
      if (!child.isCompilable()) {
        return false;
      }
    }

    if (executor.didArgumentConversionOccur()) {
      return false;
    }
    Class<?> clazz = executor.getMethod().getDeclaringClass();
    return Modifier.isPublic(clazz.getModifiers()) || executor.getPublicDeclaringClass() != null;
  }

  @Override
  public void generateCode(MethodVisitor mv, CodeFlow cf) {
    CachedMethodExecutor executorToCheck = this.cachedExecutor;
    if (executorToCheck == null || !(executorToCheck.get() instanceof ReflectiveMethodExecutor methodExecutor)) {
      throw new IllegalStateException("No applicable cached executor found: " + executorToCheck);
    }

    Method method = methodExecutor.getMethod();
    boolean isStaticMethod = Modifier.isStatic(method.getModifiers());
    String descriptor = cf.lastDescriptor();

    Label skipIfNull = null;
    if (descriptor == null && !isStaticMethod) {
      // Nothing on the stack but something is needed
      cf.loadTarget(mv);
    }
    if ((descriptor != null || !isStaticMethod) && this.nullSafe) {
      mv.visitInsn(DUP);
      skipIfNull = new Label();
      Label continueLabel = new Label();
      mv.visitJumpInsn(IFNONNULL, continueLabel);
      CodeFlow.insertCheckCast(mv, this.exitTypeDescriptor);
      mv.visitJumpInsn(GOTO, skipIfNull);
      mv.visitLabel(continueLabel);
    }
    if (descriptor != null && isStaticMethod) {
      // Something on the stack when nothing is needed
      mv.visitInsn(POP);
    }

    if (CodeFlow.isPrimitive(descriptor)) {
      CodeFlow.insertBoxIfNecessary(mv, descriptor.charAt(0));
    }

    String classDesc;
    if (Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
      classDesc = method.getDeclaringClass().getName().replace('.', '/');
    }
    else {
      Class<?> publicDeclaringClass = methodExecutor.getPublicDeclaringClass();
      Assert.state(publicDeclaringClass != null, "No public declaring class");
      classDesc = publicDeclaringClass.getName().replace('.', '/');
    }

    if (!isStaticMethod && (descriptor == null || !descriptor.substring(1).equals(classDesc))) {
      CodeFlow.insertCheckCast(mv, "L" + classDesc);
    }

    generateCodeForArguments(mv, cf, method, this.children);
    mv.visitMethodInsn((isStaticMethod ? INVOKESTATIC : (method.isDefault() ? INVOKEINTERFACE : INVOKEVIRTUAL)),
            classDesc, method.getName(), CodeFlow.createSignatureDescriptor(method),
            method.getDeclaringClass().isInterface());
    cf.pushDescriptor(this.exitTypeDescriptor);

    if (this.originalPrimitiveExitTypeDescriptor != null) {
      // The output of the accessor will be a primitive but from the block above it might be null,
      // so to have a 'common stack' element at skipIfNull target we need to box the primitive
      CodeFlow.insertBoxIfNecessary(mv, this.originalPrimitiveExitTypeDescriptor);
    }
    if (skipIfNull != null) {
      mv.visitLabel(skipIfNull);
    }
  }

  private class MethodValueRef implements ValueRef {

    private final EvaluationContext evaluationContext;

    @Nullable
    private final Object value;

    @Nullable
    private final TypeDescriptor targetType;

    private final Object[] arguments;

    public MethodValueRef(ExpressionState state, Object[] arguments) {
      this.evaluationContext = state.getEvaluationContext();
      this.value = state.getActiveContextObject().getValue();
      this.targetType = state.getActiveContextObject().getTypeDescriptor();
      this.arguments = arguments;
    }

    @Override
    public TypedValue getValue() {
      TypedValue result = MethodReference.this.getValueInternal(
              this.evaluationContext, this.value, this.targetType, this.arguments);
      updateExitTypeDescriptor();
      return result;
    }

    @Override
    public void setValue(@Nullable Object newValue) {
      throw new IllegalAccessError();
    }

    @Override
    public boolean isWritable() {
      return false;
    }
  }

  private record CachedMethodExecutor(
          MethodExecutor methodExecutor, @Nullable Class<?> staticClass,
          @Nullable TypeDescriptor target, List<TypeDescriptor> argumentTypes) {

    public boolean isSuitable(Object value, @Nullable TypeDescriptor target, List<TypeDescriptor> argumentTypes) {
      return ((this.staticClass == null || this.staticClass == value) &&
              ObjectUtils.nullSafeEquals(this.target, target) && this.argumentTypes.equals(argumentTypes));
    }

    public boolean hasProxyTarget() {
      return (this.target != null && Proxy.isProxyClass(this.target.getType()));
    }

    public MethodExecutor get() {
      return this.methodExecutor;
    }
  }

}
