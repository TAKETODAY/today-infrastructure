/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.context.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;

import cn.taketoday.asm.ClassVisitor;
import cn.taketoday.asm.Opcodes;
import cn.taketoday.asm.Type;
import cn.taketoday.cglib.core.ClassEmitter;
import cn.taketoday.cglib.core.ClassGenerator;
import cn.taketoday.cglib.core.CodeEmitter;
import cn.taketoday.cglib.core.EmitUtils;
import cn.taketoday.cglib.core.MethodInfo;
import cn.taketoday.cglib.core.Signature;
import cn.taketoday.context.ApplicationContextException;
import cn.taketoday.context.Constant;
import cn.taketoday.logger.LoggerFactory;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.ClassUtils;

import static cn.taketoday.cglib.core.CglibReflectUtils.getMethodInfo;

/**
 * @author TODAY <br>
 * 2019-10-18 22:35
 */
public abstract class MethodInvoker implements MethodAccessor, Invoker {
  private final Method method;

  public MethodInvoker(final Method method) {
    Assert.notNull(method, "method must not be null");
    this.method = method;
  }

  @Override
  public abstract Object invoke(Object obj, Object[] args);

  @Override
  public Method getMethod() {
    return method;
  }

  /**
   * Create a {@link MethodInvoker}
   *
   * @param executable
   *         Target Method to invoke
   *
   * @return {@link MethodInvoker} sub object
   */
  public static MethodInvoker create(Method executable) {
    return new MethodInvokerGenerator(executable).create();
  }

  /**
   * Create a {@link MethodInvoker}
   *
   * @param executable
   *         Target Method to invoke
   * @param targetClass
   *         most specific target class
   *
   * @return {@link MethodInvoker} sub object
   *
   * @since 3.0
   */
  public static MethodInvoker create(Method executable, Class<?> targetClass) {
    return new MethodInvokerGenerator(executable, targetClass).create();
  }

  /**
   * Create a {@link MethodInvoker}
   *
   * @param beanClass
   *         Bean Class
   * @param name
   *         Target method to invoke
   * @param parameters
   *         Target parameters classes
   *
   * @return {@link MethodInvoker} sub object
   *
   * @throws ReflectionException
   *         Thrown when a particular method cannot be found.
   */
  public static MethodInvoker create(final Class<?> beanClass,
                                     final String name, final Class<?>... parameters) {
    try {
      Method targetMethod = beanClass.getDeclaredMethod(name, parameters);
      return new MethodInvokerGenerator(targetMethod, beanClass).create();
    }
    catch (NoSuchMethodException e) {
      throw new ReflectionException("No such method", e);
    }
  }

  // MethodInvoker object generator
  // --------------------------------------------------------------

  public static class MethodInvokerGenerator
          extends GeneratorSupport<MethodInvoker> implements ClassGenerator {

    private final Method targetMethod;

    private static final String superType = "Lcn/taketoday/context/reflect/MethodInvoker;";
    private static final String[] interfaces = { "Lcn/taketoday/context/reflect/Invoker;" };

    private static final MethodInfo invokeInfo;

    /** @since 3.0.2 */
    private static final Signature SIG_CONSTRUCTOR
            = new Signature(Constant.CONSTRUCTOR_NAME, "(Ljava/lang/reflect/Method;)V");

    static {
      try {
        invokeInfo = getMethodInfo(MethodInvoker.class.getDeclaredMethod("invoke", Object.class, Object[].class));
      }
      catch (NoSuchMethodException | SecurityException e) {
        throw new ApplicationContextException(e);
      }
    }

    public MethodInvokerGenerator(Method method) {
      this(method, method.getDeclaringClass());
    }

    public MethodInvokerGenerator(Method method, Class<?> targetClass) {
      super(targetClass);
      Assert.notNull(method, "method must not be null");
      this.targetMethod = ClassUtils.getMostSpecificMethod(method, targetClass);
    }

    @Override
    public void generateClass(ClassVisitor v) {
      final Method target = this.targetMethod;
      final ClassEmitter classEmitter = beginClass(v);

      final CodeEmitter codeEmitter = EmitUtils.beginMethod(classEmitter, invokeInfo, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL);
      if (!Modifier.isStatic(target.getModifiers())) {
        codeEmitter.visitVarInsn(Opcodes.ALOAD, 1);
        codeEmitter.checkcast(Type.fromClass(targetClass));
        // codeEmitter.dup();
      }

      prepareParameters(codeEmitter, target);

      final MethodInfo methodInfo = getMethodInfo(target);
      codeEmitter.invoke(methodInfo);
      codeEmitter.box(Type.fromClass(target.getReturnType()));

      codeEmitter.return_value();
      codeEmitter.end_method();

      classEmitter.endClass();
    }

    /**
     * @since 3.0.2
     */
    @Override
    protected void generateConstructor(ClassEmitter ce) {
      CodeEmitter e = ce.beginMethod(Opcodes.ACC_PUBLIC, SIG_CONSTRUCTOR);
      e.load_this();
      e.load_arg(0);
      e.super_invoke_constructor(SIG_CONSTRUCTOR);
      e.return_value();
      e.end_method();
    }

    /**
     * @throws NoSuchMethodException
     *         handle in fallback {@link #fallback(Exception)}
     * @since 3.0.2
     */
    @Override
    protected MethodInvoker newInstance(Class<MethodInvoker> accessorClass) throws NoSuchMethodException {
      final Constructor<MethodInvoker> constructor = accessorClass.getDeclaredConstructor(Method.class);
      return ClassUtils.newInstance(constructor, new Object[] { targetMethod });
    }

    @Override
    protected String createClassName() {
      StringBuilder builder = new StringBuilder(targetClass.getName());
      builder.append('$').append(targetMethod.getName());
      buildClassNameSuffix(builder, targetMethod);
      return builder.toString();
    }

    @Override
    protected MethodInvoker fallback(Exception exception) {
      LoggerFactory.getLogger(MethodInvokerGenerator.class)
              .warn("Cannot access a Method: [{}], using fallback instance", targetMethod, exception);
      return super.fallback(exception);
    }

    @Override
    protected MethodInvoker fallbackInstance() {
      return new MethodMethodAccessor(targetMethod);
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
      if (!(o instanceof MethodInvokerCacheKey))
        return false;
      final MethodInvokerCacheKey that = (MethodInvokerCacheKey) o;
      return Objects.equals(targetMethod, that.targetMethod) && Objects.equals(targetClass, that.targetClass);
    }

    @Override
    public int hashCode() {
      return hash;
    }

  }
}
