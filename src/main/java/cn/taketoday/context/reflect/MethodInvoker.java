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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import cn.taketoday.context.Constant;
import cn.taketoday.context.asm.ClassVisitor;
import cn.taketoday.context.asm.Type;
import cn.taketoday.context.cglib.core.ClassEmitter;
import cn.taketoday.context.cglib.core.ClassGenerator;
import cn.taketoday.context.cglib.core.CodeEmitter;
import cn.taketoday.context.cglib.core.EmitUtils;
import cn.taketoday.context.cglib.core.MethodInfo;
import cn.taketoday.context.exception.ContextException;
import cn.taketoday.context.utils.Assert;

import static cn.taketoday.context.asm.Opcodes.ACC_FINAL;
import static cn.taketoday.context.asm.Opcodes.ACC_PUBLIC;
import static cn.taketoday.context.cglib.core.CglibReflectUtils.getMethodInfo;

/**
 * @author TODAY <br>
 * 2019-10-18 22:35
 */
public abstract class MethodInvoker implements MethodAccessor, Invoker {

  @Override
  public abstract Object invoke(Object obj, Object[] args);

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
   * @param beanClass
   *         Bean Class
   * @param name
   *         Target method to invoke
   * @param parameters
   *         Target parameters classes
   *
   * @return {@link MethodInvoker} sub object
   *
   * @throws NoSuchMethodException
   *         Thrown when a particular method cannot be found.
   */
  public static MethodInvoker create(final Class<?> beanClass,
                                     final String name, final Class<?>... parameters) throws NoSuchMethodException {

    final Method targetMethod = beanClass.getDeclaredMethod(name, parameters);

    return new MethodInvokerGenerator(targetMethod, beanClass).create();
  }

  // MethodInvoker object generator
  // --------------------------------------------------------------

  public static class MethodInvokerGenerator
          extends GeneratorSupport<MethodInvoker> implements ClassGenerator {

    private final Method targetMethod;

    private static final String superType = "Lcn/taketoday/context/reflect/MethodInvoker;";
    private static final String[] interfaces = { "Lcn/taketoday/context/reflect/Invoker;" };

    private static final MethodInfo invokeInfo;

    static {
      try {
        invokeInfo = getMethodInfo(MethodInvoker.class.getDeclaredMethod("invoke", Object.class, Object[].class));
      }
      catch (NoSuchMethodException | SecurityException e) {
        throw new ContextException(e);
      }
    }

    public MethodInvokerGenerator(Method method) {
      this(method, method.getDeclaringClass());
    }

    public MethodInvokerGenerator(Method method, Class<?> targetClass) {
      super(targetClass);
      Assert.notNull(method, "method must not be null");
      this.targetMethod = method;
    }

    @Override
    public void generateClass(ClassVisitor v) {
      final Method target = this.targetMethod;
      final ClassEmitter classEmitter = beginClass(v);

      final CodeEmitter codeEmitter = EmitUtils.beginMethod(classEmitter, invokeInfo, ACC_PUBLIC | ACC_FINAL);
      if (!Modifier.isStatic(target.getModifiers())) {
        codeEmitter.visitVarInsn(Constant.ALOAD, 1);
        codeEmitter.checkcast(Type.getType(targetClass));
        // codeEmitter.dup();
      }

      prepareParameters(codeEmitter, target);

      final MethodInfo methodInfo = getMethodInfo(target);
      codeEmitter.invoke(methodInfo);
      codeEmitter.box(Type.getType(target.getReturnType()));

      codeEmitter.return_value();
      codeEmitter.end_method();

      classEmitter.endClass();
    }

    @Override
    protected String createClassName() {
      StringBuilder builder = new StringBuilder(targetClass.getName());
      builder.append('$').append(targetMethod.getName());
      buildClassNameSuffix(builder, targetMethod);
      return builder.toString();
    }

    @Override
    protected MethodInvoker privateInstance() {
      return new MethodMethodAccessor(targetMethod);
    }

    @Override
    protected boolean isPrivate() {
      return Modifier.isPrivate(targetClass.getModifiers())
              || Modifier.isPrivate(targetMethod.getModifiers());
    }

    @Override
    protected ClassGenerator getClassGenerator() {
      return this;
    }

    @Override
    protected Object cacheKey() {
      return targetMethod;
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

}
