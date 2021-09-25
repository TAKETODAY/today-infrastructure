/**
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
package cn.taketoday.core.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;

import cn.taketoday.core.bytecode.ClassVisitor;
import cn.taketoday.core.bytecode.Type;
import cn.taketoday.beans.factory.BeanInstantiationException;
import cn.taketoday.beans.support.BeanUtils;
import cn.taketoday.core.bytecode.core.ClassEmitter;
import cn.taketoday.core.bytecode.core.ClassGenerator;
import cn.taketoday.core.bytecode.core.CodeEmitter;
import cn.taketoday.core.bytecode.core.CodeGenerationException;
import cn.taketoday.core.bytecode.core.DefaultGeneratorStrategy;
import cn.taketoday.core.bytecode.core.EmitUtils;
import cn.taketoday.core.Assert;
import cn.taketoday.core.NestedRuntimeException;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.Mappings;
import cn.taketoday.util.ReflectionUtils;

import static cn.taketoday.core.bytecode.Opcodes.ACC_FINAL;
import static cn.taketoday.core.bytecode.Opcodes.ACC_PUBLIC;
import static cn.taketoday.core.bytecode.Opcodes.ALOAD;
import static cn.taketoday.core.bytecode.Opcodes.INVOKESTATIC;

/**
 * @author TODAY
 * 2020/9/11 16:32
 */
public abstract class GeneratorSupport<T extends Accessor> {

  static final Type GENERATOR_SUPPORT_TYPE = Type.fromClass(GeneratorSupport.class);
  static final String GENERATOR_SUPPORT_TYPE_INTERNAL_NAME = GENERATOR_SUPPORT_TYPE.getInternalName();

  static final String DEFAULT_SUPER = "Ljava/lang/Object;";

  protected String className;
  protected ClassLoader classLoader;
  protected final Class<?> targetClass;

  protected static final Mappings<Accessor, GeneratorSupport<?>> mappings = new Mappings<Accessor, GeneratorSupport<?>>() {
    @Override
    protected Accessor createValue(Object key, GeneratorSupport<?> generator) {
      try {
        return generator.createInternal();
      }
      catch (Exception e) {
        return generator.fallback(e);
      }
    }
  };

  protected GeneratorSupport(final Class<?> targetClass) {
    Assert.notNull(targetClass, "targetClass must not be null");
    this.targetClass = targetClass;
  }

  @SuppressWarnings("unchecked")
  public T create() {
    final Object cacheKey = cacheKey();
    return (T) mappings.get(cacheKey, this);
  }

  protected T fallback(Exception exception) {
    if (exception instanceof InvocationTargetException) {
      if (((InvocationTargetException) exception).getTargetException() instanceof SecurityException) {
        return fallbackInstance();
      }
    }
    else if (exception instanceof SecurityException) {
      return fallbackInstance();
    }
    else if (exception instanceof NestedRuntimeException &&
            ((NestedRuntimeException) exception).getRootCause() instanceof SecurityException) {
      return fallbackInstance();
    }
    throw new CodeGenerationException(exception);
  }

  /**
   * @throws Exception
   *         cannot generate class
   * @throws BeanInstantiationException
   *         cannot create a {@link Accessor}
   * @see BeanUtils#newInstance(Constructor, Object[])
   */
  private T createInternal() throws Exception {
    if (cannotAccess()) {
      return fallbackInstance();
    }
    final Class<T> accessorClass = generateIfNecessary(getClassLoader());
    return newInstance(accessorClass);
  }

  /**
   * @since 3.0.2
   */
  protected T newInstance(Class<T> accessorClass) throws Exception {
    final Constructor<T> constructor = BeanUtils.obtainConstructor(accessorClass);
    return BeanUtils.newInstance(constructor, null);
  }

  @SuppressWarnings("unchecked")
  private Class<T> generateIfNecessary(ClassLoader classLoader) throws Exception {
    try {
      return (Class<T>) classLoader.loadClass(getClassName());
    }
    catch (ClassNotFoundException ignored) {
    }
    final byte[] bytes = DefaultGeneratorStrategy.INSTANCE.generate(getClassGenerator());
    return ReflectionUtils.defineClass(
            getClassName(), bytes, classLoader, ReflectionUtils.getProtectionDomain(targetClass));
  }

  protected abstract Object cacheKey();

  protected abstract T fallbackInstance();

  protected abstract boolean cannotAccess();

  protected ClassLoader getClassLoader() {
    if (classLoader == null) {
      classLoader = targetClass.getClassLoader();
      if (classLoader == null) {
        classLoader = ClassUtils.getClassLoader();
      }
    }
    return classLoader;
  }

  protected abstract ClassGenerator getClassGenerator();

  protected String getClassName() {
    if (className == null) {
      String name = targetClass.getName();
      StringBuilder builder = new StringBuilder(name.length() + 16);
      if (name.startsWith("java.")) {
        builder.append("system.");
      }
      builder.append(name);
      appendClassName(builder);
      this.className = builder.toString();
    }
    return className;
  }

  protected abstract void appendClassName(StringBuilder builder);

  protected void buildClassNameSuffix(final StringBuilder builder, final Executable target) {
    if (target.getParameterCount() != 0) {
      for (final Class<?> parameterType : target.getParameterTypes()) {
        builder.append('$');
        if (parameterType.isArray()) {
          builder.append("A$");
          // fix Multidimensional Arrays bugs
          Class<?> componentType = parameterType;
          do {
            String simpleName = componentType.getSimpleName();
            // first char
            builder.append(simpleName, 0, simpleName.indexOf('['));
            componentType = componentType.getComponentType();
          }
          while (componentType != null && componentType.isArray());
        }
        else {
          builder.append(parameterType.getSimpleName());
        }
      }
    }
  }

  protected int getArgsIndex() {
    return 2;
  }

  protected void prepareParameters(final CodeEmitter codeEmitter, Executable targetExecutable) {

    if (targetExecutable.getParameterCount() == 0) {
      return;
    }

    final Class<?>[] parameterTypes = targetExecutable.getParameterTypes();
    final int argsIndex = getArgsIndex();
    for (int i = 0; i < parameterTypes.length; i++) {
      codeEmitter.visitVarInsn(ALOAD, argsIndex);
      codeEmitter.aaload(i);

      Class<?> parameterClass = parameterTypes[i];
      final Type parameterType = Type.fromClass(parameterClass);
      if (parameterClass.isPrimitive()) {
        final Type boxedType = parameterType.getBoxedType(); // java.lang.Long ...
        codeEmitter.checkCast(boxedType);

        // use "convert" method
        final String descriptor = boxedType.getDescriptor();
        codeEmitter.visitMethodInsn(
                INVOKESTATIC, GENERATOR_SUPPORT_TYPE_INTERNAL_NAME,
                "convert", '(' + descriptor + ')' + parameterType.getDescriptor(), false);
      }
      else {
        codeEmitter.checkCast(parameterType);
      }
    }
  }

  protected ClassEmitter beginClass(ClassVisitor v) {
    final ClassEmitter ce = new ClassEmitter(v);
    ce.beginClass(ACC_PUBLIC | ACC_FINAL, getClassName().replace('.', '/'), getSuperType(), getInterfaces());
    generateConstructor(ce);
    return ce;
  }

  /**
   * @since 3.0.2
   */
  protected void generateConstructor(ClassEmitter ce) {
    EmitUtils.nullConstructor(ce);
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

  // converter

  public static long convert(Long value) {
    return value == null ? 0 : value;
  }

  public static int convert(Integer value) {
    return value == null ? 0 : value;
  }

  public static short convert(Short value) {
    return value == null ? 0 : value;
  }

  public static byte convert(Byte value) {
    return value == null ? 0 : value;
  }

  public static float convert(Float value) {
    return value == null ? 0 : value;
  }

  public static double convert(Double value) {
    return value == null ? 0 : value;
  }

  public static boolean convert(Boolean value) {
    return value != null && value;
  }

  public static char convert(Character value) {
    return value == null ? 0 : value;
  }

}
