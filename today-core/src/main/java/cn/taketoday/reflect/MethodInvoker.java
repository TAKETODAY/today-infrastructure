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

package cn.taketoday.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;

import cn.taketoday.bytecode.ClassVisitor;
import cn.taketoday.bytecode.Opcodes;
import cn.taketoday.bytecode.Type;
import cn.taketoday.bytecode.commons.MethodSignature;
import cn.taketoday.bytecode.core.ClassEmitter;
import cn.taketoday.bytecode.core.ClassGenerator;
import cn.taketoday.bytecode.core.CodeEmitter;
import cn.taketoday.bytecode.core.EmitUtils;
import cn.taketoday.bytecode.core.MethodInfo;
import cn.taketoday.lang.Assert;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ReflectionUtils;

/**
 * Fast Method Invoker
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-10-18 22:35
 */
public abstract class MethodInvoker implements MethodAccessor, Invoker {
  private final Method method;

  public MethodInvoker(Method method) {
    Assert.notNull(method, "method is required");
    this.method = method;
  }

  /**
   * Invokes the underlying method represented by this {@code Invoker}
   * object, on the specified object with the specified parameters.
   * Individual parameters are automatically unwrapped to match
   * primitive formal parameters, and both primitive and reference
   * parameters are subject to method invocation conversions as
   * necessary.
   *
   * <p>If the underlying method is static, then the specified {@code obj}
   * argument is ignored. It may be null.
   *
   * <p>If the number of formal parameters required by the underlying method is
   * 0, the supplied {@code args} array may be of length 0 or null.
   *
   * <p>If the underlying method is static, the class that declared
   * the method is initialized if it has not already been initialized.
   *
   * <p>If the method completes normally, the value it returns is
   * returned to the caller of invoke; if the value has a primitive
   * type, it is first appropriately wrapped in an object. However,
   * if the value has the type of array of a primitive type, the
   * elements of the array are <i>not</i> wrapped in objects; in
   * other words, an array of primitive type is returned.  If the
   * underlying method return type is void, the invocation returns
   * null.
   *
   * @param obj the object the underlying method is invoked from
   * @param args the arguments used for the method call
   * @return the result of dispatching the method represented by
   * this object on {@code obj} with parameters
   * {@code args}
   * @throws NullPointerException if the specified object is null and the method is an instance method.
   * @throws ExceptionInInitializerError if the initialization provoked by this method fails.
   */
  @Override
  public abstract Object invoke(Object obj, Object[] args);

  @Override
  public Method getMethod() {
    return method;
  }

  /**
   * Create a {@link MethodInvoker}
   *
   * @param executable Target Method to invoke
   * @return {@link MethodInvoker} sub object
   */
  public static MethodInvoker forMethod(Method executable) {
    Assert.notNull(executable, "method is required");
    return new MethodInvokerGenerator(executable).create();
  }

  /**
   * Create a {@link MethodInvoker}
   *
   * @param executable Target Method to invoke
   * @param targetClass most specific target class
   * @return {@link MethodInvoker} sub object
   * @since 3.0
   */
  public static MethodInvoker forMethod(Method executable, Class<?> targetClass) {
    Assert.notNull(executable, "method is required");
    return new MethodInvokerGenerator(executable, targetClass).create();
  }

  /**
   * Create a {@link MethodInvoker}
   *
   * @param beanClass Bean Class
   * @param name Target method to invoke
   * @param parameters Target parameters classes
   * @return {@link MethodInvoker} sub object
   * @throws ReflectionException Thrown when a particular method cannot be found.
   */
  public static MethodInvoker forMethod(Class<?> beanClass, String name, Class<?>... parameters) {
    try {
      Method targetMethod = beanClass.getDeclaredMethod(name, parameters);
      return new MethodInvokerGenerator(targetMethod, beanClass).create();
    }
    catch (NoSuchMethodException e) {
      throw new ReflectionException("No such method: '%s' in %s".formatted(name, beanClass), e);
    }
  }

  public static MethodInvoker forReflective(Method method) {
    return forReflective(method, true);
  }

  public static MethodInvoker forReflective(Method method, boolean handleReflectionException) {
    Assert.notNull(method, "Method is required");
    ReflectionUtils.makeAccessible(method);
    return new ReflectiveMethodAccessor(method, handleReflectionException);
  }

  // MethodInvoker object generator
  // --------------------------------------------------------------

  public static class MethodInvokerGenerator extends GeneratorSupport<MethodInvoker> implements ClassGenerator {

    private static final String superType = "Lcn/taketoday/reflect/MethodInvoker;";
    private static final String[] interfaces = { "Lcn/taketoday/reflect/Invoker;" };
    private static final MethodInfo invokeInfo = MethodInfo.from(
            ReflectionUtils.getMethod(MethodInvoker.class, "invoke", Object.class, Object[].class));

    /** @since 3.0.2 */
    private static final MethodSignature SIG_CONSTRUCTOR
            = new MethodSignature(MethodSignature.CONSTRUCTOR_NAME, "(Ljava/lang/reflect/Method;)V");

    private final Method targetMethod;

    /**
     * @throws NullPointerException method maybe null
     */
    public MethodInvokerGenerator(Method method) {
      super(method.getDeclaringClass());
      this.targetMethod = method;
    }

    /**
     * @throws NullPointerException method maybe null
     */
    public MethodInvokerGenerator(Method method, Class<?> targetClass) {
      super(targetClass);
      this.targetMethod = ReflectionUtils.getMostSpecificMethod(method, targetClass);
    }

    @Override
    public void generateClass(ClassVisitor v) {
      ClassEmitter classEmitter = beginClass(v);

      CodeEmitter codeEmitter = EmitUtils.beginMethod(classEmitter, invokeInfo, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL);
      if (!Modifier.isStatic(targetMethod.getModifiers())) {
        codeEmitter.visitVarInsn(Opcodes.ALOAD, 1);
        codeEmitter.checkCast(Type.fromClass(targetClass));
        // codeEmitter.dup();
      }

      prepareParameters(codeEmitter, targetMethod);

      MethodInfo methodInfo = MethodInfo.from(targetMethod);
      codeEmitter.invoke(methodInfo);
      codeEmitter.valueOf(Type.fromClass(targetMethod.getReturnType()));

      codeEmitter.returnValue();
      codeEmitter.end_method();

      classEmitter.endClass();
    }

    /**
     * @since 3.0.2
     */
    @Override
    protected void generateConstructor(ClassEmitter ce) {
      CodeEmitter e = ce.beginMethod(Opcodes.ACC_PUBLIC, SIG_CONSTRUCTOR);
      e.loadThis();
      e.loadArg(0);
      e.super_invoke_constructor(SIG_CONSTRUCTOR);
      e.returnValue();
      e.end_method();
    }

    /**
     * @throws NoSuchMethodException handle in fallback {@link #fallback(Exception)}
     * @since 3.0.2
     */
    @Override
    protected MethodInvoker newInstance(Class<MethodInvoker> accessorClass) throws NoSuchMethodException {
      Constructor<MethodInvoker> constructor = accessorClass.getDeclaredConstructor(Method.class);
      return ReflectionUtils.invokeConstructor(constructor, new Object[] { targetMethod });
    }

    @Override
    protected void appendClassName(StringBuilder builder) {
      builder.append('$')
              .append(targetMethod.getName());
      buildClassNameSuffix(builder, targetMethod);
    }

    @Override
    protected MethodInvoker fallback(Exception exception) {
      LoggerFactory.getLogger(MethodInvokerGenerator.class)
              .warn("Cannot access a Method: [{}], using fallback instance", targetMethod, exception);
      return super.fallback(exception);
    }

    @Override
    protected MethodInvoker fallbackInstance() {
      return forReflective(targetMethod);
    }

    @Override
    protected boolean cannotAccess() {
      return Modifier.isPrivate(targetClass.getModifiers())
              || Modifier.isPrivate(targetMethod.getModifiers());
    }

    @Override
    protected ClassGenerator getClassGenerator() {
      return this;
    }

    @Override
    protected Object cacheKey() {
      return new MethodInvokerCacheKey(targetMethod, targetClass);
    }

    @Override
    public String getSuperType() {
      return superType;
    }

    @Override
    public String[] getInterfaces() {
      return interfaces;
    }
  }

  static class MethodInvokerCacheKey {
    int hash;
    final Method targetMethod;
    final Class<?> targetClass;

    MethodInvokerCacheKey(Method targetMethod, Class<?> targetClass) {
      this.targetMethod = targetMethod;
      this.targetClass = targetClass;
      this.hash = Objects.hash(targetMethod, targetClass);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (!(o instanceof MethodInvokerCacheKey that))
        return false;
      return Objects.equals(targetMethod, that.targetMethod) && Objects.equals(targetClass, that.targetClass);
    }

    @Override
    public int hashCode() {
      return hash;
    }

  }
}
