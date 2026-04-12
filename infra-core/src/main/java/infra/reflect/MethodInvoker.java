/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.reflect;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;

import infra.bytecode.ClassVisitor;
import infra.bytecode.Opcodes;
import infra.bytecode.Type;
import infra.bytecode.commons.MethodSignature;
import infra.bytecode.core.ClassEmitter;
import infra.bytecode.core.ClassGenerator;
import infra.bytecode.core.CodeEmitter;
import infra.bytecode.core.EmitUtils;
import infra.bytecode.core.MethodInfo;
import infra.lang.Assert;
import infra.logging.LoggerFactory;
import infra.util.ReflectionUtils;

/**
 * A high-performance method invoker that uses bytecode generation to invoke methods,
 * avoiding the overhead of standard Java reflection.
 * <p>
 * This class serves as the base for generated subclasses that implement efficient
 * method invocation logic. It caches method metadata and provides a unified interface
 * for invoking methods on target objects.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-10-18 22:35
 */
public abstract class MethodInvoker implements MethodAccessor, Invoker {

  protected final Method method;

  public MethodInvoker(Method method) {
    Assert.notNull(method, "method is required");
    this.method = method;
  }

  /**
   * Invokes the underlying method represented by this {@code MethodInvoker}
   * on the specified object with the given arguments.
   * <p>
   * This method handles automatic unwrapping of parameters to match primitive
   * formal parameters and applies necessary method invocation conversions for
   * both primitive and reference types.
   *
   * @param obj the object on which the underlying method is invoked; may be {@code null} if the method is static
   * @param args the arguments used for the method call; may be {@code null} or empty if the method takes no parameters
   * @return the result of the method invocation, wrapped in an object if it is a primitive type;
   * {@code null} if the method return type is void
   * @throws NullPointerException if the specified object is {@code null} and the method is an instance method
   */
  @Override
  public abstract @Nullable Object invoke(@Nullable Object obj, @Nullable Object @Nullable [] args);

  /**
   * Returns the underlying {@link Method} represented by this invoker.
   *
   * @return the target method
   */
  @Override
  public Method getMethod() {
    return method;
  }

  /**
   * Creates a {@link MethodInvoker} for the specified method.
   * <p>
   * This method generates a high-performance invoker using bytecode generation.
   * If the specified method is a standard {@code java.lang.Object} method (such as
   * {@code toString}, {@code hashCode}, or {@code equals}), special handling may be applied.
   *
   * @param executable the target method to invoke
   * @return a {@link MethodInvoker} instance optimized for the specified method
   * @throws IllegalArgumentException if the executable method is null
   */
  public static MethodInvoker forMethod(Method executable) {
    return forMethod(executable, null);
  }

  /**
   * Creates a {@link MethodInvoker} for the specified method, optimized for the given target class.
   * <p>
   * This method generates a high-performance invoker using bytecode generation. The {@code targetClass}
   * is used to resolve the most specific implementation of the method, which is particularly useful
   * when dealing with interface methods or overridden methods in subclasses.
   *
   * @param executable the target method to invoke
   * @param targetClass the most specific target class to use for method resolution and invocation
   * @return a {@link MethodInvoker} instance optimized for the specified method and target class
   * @throws IllegalArgumentException if the executable method is null
   * @since 3.0
   */
  public static MethodInvoker forMethod(Method executable, @Nullable Class<?> targetClass) {
    Assert.notNull(executable, "method is required");
    ObjectMethodType methodType = ObjectMethodType.forMethod(executable);
    if (methodType != null) {
      return new ObjectMethodInvoker(executable, methodType);
    }
    return new MethodInvokerGenerator(executable, targetClass).generate();
  }

  /**
   * Creates a {@link MethodInvoker} for the specified method in the given bean class.
   * <p>
   * This method locates the declared method by name and parameter types, then generates
   * a high-performance invoker using bytecode generation. If the method is not found,
   * a {@link ReflectionException} is thrown.
   *
   * @param beanClass the class declaring the target method
   * @param name the name of the target method
   * @param parameters the parameter types of the target method
   * @return a {@link MethodInvoker} instance for the specified method
   * @throws ReflectionException if the specified method cannot be found in the given class
   */
  public static MethodInvoker forMethod(Class<?> beanClass, String name, Class<?>... parameters) {
    try {
      Method targetMethod = beanClass.getDeclaredMethod(name, parameters);
      return forMethod(targetMethod, beanClass);
    }
    catch (NoSuchMethodException e) {
      throw new ReflectionException("No such method: '%s' in %s".formatted(name, beanClass), e);
    }
  }

  /**
   * Creates a {@link MethodInvoker} that uses standard Java reflection to invoke the given method.
   * <p>
   * This is a fallback mechanism when bytecode generation is not possible or fails.
   * It automatically handles accessibility checks and exception wrapping by default.
   *
   * @param method the target method to invoke
   * @return a reflective {@link MethodInvoker} instance
   * @throws IllegalArgumentException if the method is null
   */
  public static MethodInvoker forReflective(Method method) {
    return forReflective(method, true);
  }

  /**
   * Creates a {@link MethodInvoker} that uses standard Java reflection to invoke the given method.
   * <p>
   * This is a fallback mechanism when bytecode generation is not possible or fails.
   *
   * @param method the target method to invoke
   * @param handleReflectionException whether to handle reflection exceptions internally
   * (e.g., wrapping them in runtime exceptions) or propagate them
   * @return a reflective {@link MethodInvoker} instance
   * @throws IllegalArgumentException if the method is null
   */
  public static MethodInvoker forReflective(Method method, boolean handleReflectionException) {
    Assert.notNull(method, "Method is required");
    ReflectionUtils.makeAccessible(method);
    return new ReflectiveMethodAccessor(method, handleReflectionException);
  }

  // --------------------------------------------------------------
  // MethodInvoker object generator
  // --------------------------------------------------------------

  private static final class MethodInvokerGenerator extends GeneratorSupport<MethodInvoker> implements ClassGenerator {

    private static final String superType = "Linfra/reflect/MethodInvoker;";

    private static final String[] interfaces = { "Linfra/reflect/Invoker;" };

    private final Method targetMethod;

    public MethodInvokerGenerator(Method method, @Nullable Class<?> targetClass) {
      super(targetClass == null ? method.getDeclaringClass() : targetClass);
      this.targetMethod = targetClass == null ? method : ReflectionUtils.getMostSpecificMethod(method, targetClass);
    }

    @Override
    public void generateClass(ClassVisitor v) {
      ClassEmitter classEmitter = beginClass(v);
      MethodInfo invokeInfo = MethodInfo.from(
              ReflectionUtils.getMethod(MethodInvoker.class, "invoke", Object.class, Object[].class));

      CodeEmitter codeEmitter = EmitUtils.beginMethod(classEmitter, invokeInfo, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL);
      if (!Modifier.isStatic(targetMethod.getModifiers())) {
        codeEmitter.visitVarInsn(Opcodes.ALOAD, 1);
        codeEmitter.checkCast(Type.forClass(targetClass));
        // codeEmitter.dup();
      }

      prepareParameters(codeEmitter, targetMethod);

      MethodInfo methodInfo = MethodInfo.from(targetMethod);
      codeEmitter.invoke(methodInfo);
      codeEmitter.valueOf(Type.forClass(targetMethod.getReturnType()));

      codeEmitter.returnValue();
      codeEmitter.end_method();

      classEmitter.endClass();
    }

    /**
     * @since 3.0.2
     */
    @Override
    protected void generateConstructor(ClassEmitter ce) {
      var signature = MethodSignature.forConstructor(Type.forClass(Method.class));
      CodeEmitter e = ce.beginMethod(Opcodes.ACC_PUBLIC, signature);
      e.loadThis();
      e.loadArg(0);
      e.super_invoke_constructor(signature);
      e.returnValue();
      e.end_method();
    }

    /**
     * @throws NoSuchMethodException handle in fallback {@link #fallbackIfNecessary(Exception)}
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
    protected MethodInvoker fallbackInstance(@Nullable Throwable exception) {
      if (exception != null) {
        LoggerFactory.getLogger(MethodInvokerGenerator.class)
                .warn("Cannot access a Method: [{}], using fallback instance", targetMethod, exception);
      }
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
