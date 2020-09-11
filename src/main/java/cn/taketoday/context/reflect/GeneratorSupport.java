/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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

import java.lang.reflect.Executable;

import cn.taketoday.context.asm.ClassVisitor;
import cn.taketoday.context.asm.Type;
import cn.taketoday.context.cglib.core.ClassEmitter;
import cn.taketoday.context.cglib.core.ClassGenerator;
import cn.taketoday.context.cglib.core.CodeEmitter;
import cn.taketoday.context.cglib.core.CodeGenerationException;
import cn.taketoday.context.cglib.core.DefaultGeneratorStrategy;
import cn.taketoday.context.cglib.core.EmitUtils;
import cn.taketoday.context.cglib.core.ReflectUtils;
import cn.taketoday.context.cglib.core.TypeUtils;
import cn.taketoday.context.utils.ClassUtils;

import static cn.taketoday.context.asm.Opcodes.ACC_FINAL;
import static cn.taketoday.context.asm.Opcodes.ACC_PUBLIC;
import static cn.taketoday.context.asm.Opcodes.ALOAD;
import static cn.taketoday.context.asm.Opcodes.INVOKEVIRTUAL;

/**
 * @author TODAY
 * @date 2020/9/11 16:32
 */
abstract class GeneratorSupport<T> {

  private static final String DEFAULT_SUPER = "Ljava/lang/Object;";

  protected String className;
  private ClassLoader classLoader;
  protected final Class<?> targetClass;

  protected GeneratorSupport(final Class<?> targetClass) {
    this.targetClass = targetClass;
  }

  public T create() {
    final ClassLoader classLoader = getClassLoader();
    try {
      return (T) ClassUtils.newInstance(classLoader.loadClass(getClassName()));
    }
    catch (ClassNotFoundException e) {
      return ClassUtils.newInstance(generateClass(classLoader));
    }
  }

  protected Class<T> generateClass(final ClassLoader classLoader) {
    try {
      final byte[] b = DefaultGeneratorStrategy.INSTANCE.generate(getClassGenerator());
      return ReflectUtils.defineClass(getClassName(), b, classLoader, ReflectUtils.getProtectionDomain(targetClass));
    }
    catch (Exception e) {
      throw new CodeGenerationException(e);
    }
  }

  protected ClassLoader getClassLoader() {
    if (classLoader == null) {
      return targetClass.getClassLoader();
    }
    return classLoader;
  }

  protected abstract ClassGenerator getClassGenerator();

  protected String getClassName() {
    if (className == null) {
      this.className = createClassName();
    }
    return className;
  }

  protected abstract String createClassName();

  protected void buildClassNameSuffix(final StringBuilder builder, final Executable target) {
    if (target.getParameterCount() != 0) {
      for (final Class<?> parameterType : target.getParameterTypes()) {
        builder.append('$');
        if (parameterType.isArray()) {
          builder.append("A$");
          final String simpleName = parameterType.getSimpleName();
          builder.append(simpleName, 0, simpleName.length() - 2);
        }
        else {
          builder.append(parameterType.getSimpleName());
        }
      }
    }
  }

  protected void prepareParameters(final CodeEmitter codeEmitter, Executable targetExecutable) {

    if (targetExecutable.getParameterCount() == 0) {
      return;
    }

    final Class<?>[] parameterTypes = targetExecutable.getParameterTypes();

    final int a_load = ALOAD;
    for (int i = 0; i < parameterTypes.length; i++) {
      codeEmitter.visitVarInsn(a_load, 2);
      codeEmitter.aaload(i);

      Class<?> parameterClass = parameterTypes[i];
      final Type parameterType = Type.getType(parameterClass);
      if (parameterClass.isPrimitive()) {
        final Type boxedType = TypeUtils.getBoxedType(parameterType); // java.lang.Long ...

        codeEmitter.checkcast(boxedType);
        final String name = parameterClass.getName() + "Value";
        final String descriptor = "()" + parameterType.getDescriptor();

        codeEmitter.visitMethodInsn(INVOKEVIRTUAL, boxedType.getInternalName(), name, descriptor, false);
      }
      else {
        codeEmitter.checkcast(parameterType);
      }
    }
  }

  protected ClassEmitter beginClass(ClassVisitor v) {
    final ClassEmitter ce = new ClassEmitter(v);
    ce.beginClass(ACC_PUBLIC | ACC_FINAL, getClassName().replace('.', '/'), getSuperType(), getInterfaces());
    EmitUtils.nullConstructor(ce);
    return ce;
  }

  protected String[] getInterfaces() {
    return null;
  }

  public String getSuperType() {
    return DEFAULT_SUPER;
  }

  public void setClassLoader(final ClassLoader classLoader) {
    this.classLoader = classLoader;
  }
}
