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
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;

import infra.bytecode.BytecodeCompiler;
import infra.bytecode.ClassVisitor;
import infra.bytecode.Opcodes;
import infra.bytecode.Type;
import infra.bytecode.core.ClassEmitter;
import infra.bytecode.core.ClassGenerator;
import infra.bytecode.core.CodeEmitter;
import infra.bytecode.core.CodeGenerationException;
import infra.bytecode.core.DefaultGeneratorStrategy;
import infra.bytecode.core.EmitUtils;
import infra.core.NestedRuntimeException;
import infra.lang.Assert;
import infra.util.ClassUtils;
import infra.util.MapCache;
import infra.util.ReflectionUtils;

import static infra.bytecode.Opcodes.ACC_FINAL;
import static infra.bytecode.Opcodes.ACC_PUBLIC;
import static infra.bytecode.Opcodes.INVOKESTATIC;

/**
 * @param <T> Target Accessor subclass
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2020/9/11 16:32
 */
public abstract class GeneratorSupport<T extends Accessor> {

  static final String DEFAULT_SUPER = "Ljava/lang/Object;";

  @Nullable
  protected String className;

  @Nullable
  protected ClassLoader classLoader;

  protected final Class<?> targetClass;

  protected static final MapCache<Object, Accessor, GeneratorSupport<?>> mappings = new MapCache<>() {
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

  protected GeneratorSupport(Class<?> targetClass) {
    Assert.notNull(targetClass, "targetClass is required");
    this.targetClass = targetClass;
  }

  @SuppressWarnings("unchecked")
  public T create() {
    Object cacheKey = cacheKey();
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
   * @throws Exception cannot generate class
   * @see ReflectionUtils#invokeConstructor(Constructor, Object[])
   */
  private T createInternal() throws Exception {
    if (cannotAccess()) {
      return fallbackInstance();
    }
    Class<T> accessorClass = generateIfNecessary(getClassLoader());
    return newInstance(accessorClass);
  }

  /**
   * @since 3.0.2
   */
  protected T newInstance(Class<T> accessorClass) throws Exception {
    Constructor<T> constructor = ReflectionUtils.getConstructor(accessorClass);
    return ReflectionUtils.invokeConstructor(constructor, null);
  }

  @SuppressWarnings("unchecked")
  private Class<T> generateIfNecessary(ClassLoader classLoader) throws Exception {
    try {
      return (Class<T>) classLoader.loadClass(getClassName());
    }
    catch (ClassNotFoundException ignored) {
    }
    byte[] classFile = DefaultGeneratorStrategy.INSTANCE.generate(getClassGenerator());
    return BytecodeCompiler.compile(getClassName(), targetClass,
            classLoader, ReflectionUtils.getProtectionDomain(targetClass), classFile);
  }

  protected abstract Object cacheKey();

  protected abstract T fallbackInstance();

  protected abstract boolean cannotAccess();

  @SuppressWarnings("NullAway")
  protected ClassLoader getClassLoader() {
    if (classLoader == null) {
      classLoader = targetClass.getClassLoader();
      if (classLoader == null) {
        classLoader = ClassUtils.getDefaultClassLoader();
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

  protected void buildClassNameSuffix(StringBuilder builder, Executable target) {
    if (target.getParameterCount() != 0) {
      for (Class<?> parameterType : target.getParameterTypes()) {
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

  protected void prepareParameters(CodeEmitter codeEmitter, Executable targetExecutable) {

    if (targetExecutable.getParameterCount() == 0) {
      return;
    }

    Class<?>[] parameterTypes = targetExecutable.getParameterTypes();
    int argsIndex = getArgsIndex();
    for (int i = 0; i < parameterTypes.length; i++) {
      codeEmitter.visitVarInsn(Opcodes.ALOAD, argsIndex);
      codeEmitter.aaload(i);

      Type parameterType = Type.forClass(parameterTypes[i]);
      checkCast(codeEmitter, parameterType);
    }
  }

  protected final void checkCast(CodeEmitter code, Type parameterType) {
    if (parameterType.isPrimitive()) {
      unbox(code, parameterType);
    }
    else {
      code.checkCast(parameterType);
    }
  }

  // nullable unbox
  protected final void unbox(CodeEmitter code, Type parameterType) {
    Type boxedType = parameterType.getBoxedType(); // java.lang.Long ...
    code.checkCast(boxedType);

    // use "convert" method
    String descriptor = boxedType.getDescriptor();
    code.visitMethodInsn(INVOKESTATIC, Type.forClass(GeneratorSupport.class).getInternalName(),
            "convert", '(' + descriptor + ')' + parameterType.getDescriptor(), false);
  }

  protected ClassEmitter beginClass(ClassVisitor v) {
    ClassEmitter ce = new ClassEmitter(v);
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

  protected String @Nullable [] getInterfaces() {
    return null;
  }

  public String getSuperType() {
    return DEFAULT_SUPER;
  }

  public void setClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  // converter

  public static long convert(@Nullable Long value) {
    return value == null ? 0 : value;
  }

  public static int convert(@Nullable Integer value) {
    return value == null ? 0 : value;
  }

  public static short convert(@Nullable Short value) {
    return value == null ? 0 : value;
  }

  public static byte convert(@Nullable Byte value) {
    return value == null ? 0 : value;
  }

  public static float convert(@Nullable Float value) {
    return value == null ? 0 : value;
  }

  public static double convert(@Nullable Double value) {
    return value == null ? 0 : value;
  }

  public static boolean convert(@Nullable Boolean value) {
    return value != null && value;
  }

  public static char convert(@Nullable Character value) {
    return value == null ? 0 : value;
  }

}
