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
package cn.taketoday.context.reflect;

import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.Objects;

import cn.taketoday.context.asm.ClassVisitor;
import cn.taketoday.context.asm.Type;
import cn.taketoday.context.cglib.core.CglibReflectUtils;
import cn.taketoday.context.cglib.core.ClassEmitter;
import cn.taketoday.context.cglib.core.ClassGenerator;
import cn.taketoday.context.cglib.core.CodeEmitter;
import cn.taketoday.context.cglib.core.CodeGenerationException;
import cn.taketoday.context.cglib.core.DefaultGeneratorStrategy;
import cn.taketoday.context.cglib.core.EmitUtils;
import cn.taketoday.context.cglib.core.TypeUtils;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.ClassUtils;

import static cn.taketoday.context.asm.Opcodes.ACC_FINAL;
import static cn.taketoday.context.asm.Opcodes.ACC_PUBLIC;
import static cn.taketoday.context.asm.Opcodes.ALOAD;
import static cn.taketoday.context.asm.Opcodes.INVOKESTATIC;

/**
 * @author TODAY
 * 2020/9/11 16:32
 */
public abstract class GeneratorSupport<T> {

  static final Logger log = LoggerFactory.getLogger(GeneratorSupport.class);
  static final Type GENERATOR_SUPPORT_TYPE = Type.getType(GeneratorSupport.class);
  static final String GENERATOR_SUPPORT_TYPE_INTERNAL_NAME = GENERATOR_SUPPORT_TYPE.getInternalName();

  static final String DEFAULT_SUPER = "Ljava/lang/Object;";
  static final LinkedList<GeneratorNode> created = new LinkedList<>();

  String className;
  ClassLoader classLoader;
  final Class<?> targetClass;

  GeneratorSupport(final Class<?> targetClass) {
    Assert.notNull(targetClass, "targetClass must not be null");
    this.targetClass = targetClass;
  }

  @SuppressWarnings("unchecked")
  public T create() {

    final Object cacheKey = cacheKey();
    for (final GeneratorNode node : created) {
      if (Objects.equals(cacheKey, node.key)) {
        return (T) node.value;
      }
    }

    try {
      final Object ret = createInternal();
      created.add(new GeneratorNode(cacheKey, ret));
      return (T) ret;
    }
    catch (Exception e) {
      return fallback(e);
    }
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
    throw new CodeGenerationException(exception);
  }

  Object createInternal() throws Exception {
    if (cannotAccess()) {
      return fallbackInstance();
    }
    final ClassLoader classLoader = getClassLoader();
    try {
      return ClassUtils.newInstance(classLoader.loadClass(getClassName()));
    }
    catch (ClassNotFoundException e) {
      return ClassUtils.newInstance(generateClass(classLoader));
    }
  }

  abstract Object cacheKey();

  abstract T fallbackInstance();

  abstract boolean cannotAccess();

  Class<T> generateClass(final ClassLoader classLoader) throws Exception {
    final byte[] b = DefaultGeneratorStrategy.INSTANCE.generate(getClassGenerator());
    return CglibReflectUtils.defineClass(getClassName(), b, classLoader, CglibReflectUtils.getProtectionDomain(targetClass));
  }

  ClassLoader getClassLoader() {
    if (classLoader == null) {
      classLoader = targetClass.getClassLoader();
      if (classLoader == null) {
        classLoader = ClassUtils.getClassLoader();
      }
    }
    return classLoader;
  }

  abstract ClassGenerator getClassGenerator();

  String getClassName() {
    if (className == null) {
      this.className = createClassName();
    }
    return className;
  }

  abstract String createClassName();

  void buildClassNameSuffix(final StringBuilder builder, final Executable target) {
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

  int getArgsIndex() {
    return 2;
  }

  void prepareParameters(final CodeEmitter codeEmitter, Executable targetExecutable) {

    if (targetExecutable.getParameterCount() == 0) {
      return;
    }

    final Class<?>[] parameterTypes = targetExecutable.getParameterTypes();
    final int argsIndex = getArgsIndex();
    for (int i = 0; i < parameterTypes.length; i++) {
      codeEmitter.visitVarInsn(ALOAD, argsIndex);
      codeEmitter.aaload(i);

      Class<?> parameterClass = parameterTypes[i];
      final Type parameterType = Type.getType(parameterClass);
      if (parameterClass.isPrimitive()) {
        final Type boxedType = TypeUtils.getBoxedType(parameterType); // java.lang.Long ...
        codeEmitter.checkcast(boxedType);

        // use "convert" method
        final String descriptor = boxedType.getDescriptor();
        codeEmitter.visitMethodInsn(INVOKESTATIC,
                                    GENERATOR_SUPPORT_TYPE_INTERNAL_NAME,
                                    "convert", '(' + descriptor + ')' + parameterType.getDescriptor(), false);
      }
      else {
        codeEmitter.checkcast(parameterType);
      }
    }
  }

  ClassEmitter beginClass(ClassVisitor v) {
    final ClassEmitter ce = new ClassEmitter(v);
    ce.beginClass(ACC_PUBLIC | ACC_FINAL, getClassName().replace('.', '/'), getSuperType(), getInterfaces());
    EmitUtils.nullConstructor(ce);
    return ce;
  }

  String[] getInterfaces() {
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

class GeneratorNode {
  final Object key;
  final Object value;

  GeneratorNode(final Object key, final Object value) {
    this.key = key;
    this.value = value;
  }
}
