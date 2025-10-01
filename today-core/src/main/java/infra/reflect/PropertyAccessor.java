/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.reflect;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import infra.bytecode.ClassVisitor;
import infra.bytecode.Opcodes;
import infra.bytecode.Type;
import infra.bytecode.core.ClassEmitter;
import infra.bytecode.core.ClassGenerator;
import infra.bytecode.core.CodeEmitter;
import infra.bytecode.core.EmitUtils;
import infra.bytecode.core.MethodInfo;
import infra.lang.Assert;
import infra.util.ReflectionUtils;

/**
 * @author TODAY 2020/9/11 11:06
 */
public abstract class PropertyAccessor implements SetterMethod, GetterMethod, Accessor {

  @Nullable
  @Override
  public abstract Object get(Object obj) throws ReflectionException;

  @Override
  public abstract void set(Object obj, @Nullable Object value) throws ReflectionException;

  /**
   * can write
   *
   * @since 4.0
   */
  public boolean isWriteable() {
    return true;
  }

  // static

  /**
   * @throws ReflectionException No property in target class
   */
  public static PropertyAccessor from(Class<?> targetClass, String name) {
    Field field = ReflectionUtils.findField(targetClass, name);
    if (field == null) {
      throw new ReflectionException("No such property: '%s' in class: %s".formatted(name, targetClass));
    }
    return forField(field);
  }

  /**
   * getter setter is exists in a bean or pojo, use fast invoke tech {@link MethodInvoker}
   *
   * @param writeMethod setter method
   * @param readMethod getter method
   * @return PropertyAccessor
   */
  public static PropertyAccessor forMethod(@Nullable Method readMethod, @Nullable Method writeMethod) {
    if (readMethod != null) {
      MethodInvoker readInvoker = MethodInvoker.forMethod(readMethod);
      if (writeMethod == null) {
        return new ReadOnlyMethodAccessorPropertyAccessor(readInvoker);
      }
      else {
        return new MethodAccessorPropertyAccessor(readInvoker, MethodInvoker.forMethod(writeMethod));
      }
    }
    if (writeMethod != null) {
      MethodInvoker writeInvoker = MethodInvoker.forMethod(writeMethod);
      return new WriteOnlyPropertyAccessor() {

        @Override
        public Method getWriteMethod() {
          return writeMethod;
        }

        @Override
        public void set(Object obj, @Nullable Object value) {
          writeInvoker.invoke(obj, new Object[] { value });
        }
      };
    }
    throw new IllegalArgumentException("read-write cannot be null at the same time");
  }

  /**
   * use GetterMethod and SetterMethod tech to access property
   *
   * @param writeMethod setter method
   * @param readMethod getter method
   * @return PropertyAccessor
   */
  public static PropertyAccessor forMethod(GetterMethod readMethod, @Nullable SetterMethod writeMethod) {
    Assert.notNull(readMethod, "readMethod is required");
    if (writeMethod != null) {
      return new GetterSetterPropertyAccessor(readMethod, writeMethod);
    }
    return new ReadOnlyGetterMethodPropertyAccessor(readMethod);
  }

  /**
   * PropertyAccessor
   *
   * @param field Field
   * @return PropertyAccessor
   */
  public static PropertyAccessor forField(Field field) {
    return forField(field, null, null);
  }

  /**
   * @param field Field
   * @return PropertyAccessor
   * @throws NullPointerException field is null
   */
  public static PropertyAccessor forField(Field field, @Nullable Method readMethod, @Nullable Method writeMethod) {
    boolean isReadOnly = Modifier.isFinal(field.getModifiers()) && writeMethod == null;
    if (isReadOnly && readMethod != null) {
      MethodInvoker invoker = MethodInvoker.forMethod(readMethod);
      return new ReadOnlyMethodAccessorPropertyAccessor(invoker);
    }
    // using read/write method
    if (writeMethod != null && readMethod != null) {
      return forMethod(readMethod, writeMethod);
    }

    // public field
    if (Modifier.isPublic(field.getModifiers())) {
      return new PublicPropertyAccessorGenerator(field, writeMethod).create();
    }

    if (writeMethod != null) {
      MethodInvoker accessor = MethodInvoker.forMethod(writeMethod);
      ReflectionUtils.makeAccessible(field);
      return getPropertyAccessor(field, accessor, writeMethod);
    }

    if (readMethod != null) {
      ReflectionUtils.makeAccessible(field);
      MethodInvoker accessor = MethodInvoker.forMethod(readMethod);
      return getPropertyAccessor(accessor, field, readMethod);
    }

    // readMethod == null && setMethod == null
    return forReflective(field);
  }

  private static PropertyAccessor getPropertyAccessor(Field field, MethodInvoker accessor, Method writeMethod) {
    return new PropertyAccessor() {

      @Nullable
      @Override
      public Object get(Object obj) {
        return ReflectionUtils.getField(field, obj);
      }

      @Override
      public void set(Object obj, @Nullable Object value) {
        accessor.invoke(obj, new Object[] { value });
      }

      @Override
      public Method getWriteMethod() {
        return writeMethod;
      }
    };
  }

  private static PropertyAccessor getPropertyAccessor(MethodInvoker accessor, Field field, Method readMethod) {
    return new PropertyAccessor() {
      @Nullable
      @Override
      public Object get(Object obj) {
        return accessor.invoke(obj, null);
      }

      @Override
      public void set(Object obj, @Nullable Object value) {
        ReflectionUtils.setField(field, obj, value);
      }

      @Override
      public Method getReadMethod() {
        return readMethod;
      }
    };
  }

  /**
   * use java reflect {@link Field} tech
   *
   * @param field Field
   * @return Reflective PropertyAccessor
   * @see Field#get(Object)
   * @see Field#set(Object, Object)
   * @see ReflectionUtils#getField(Field, Object)
   * @see ReflectionUtils#setField(Field, Object, Object)
   */
  public static PropertyAccessor forReflective(Field field) {
    return forReflective(field, null, null);
  }

  /**
   * use java reflect {@link Field} tech
   *
   * @param field Field
   * @return Reflective PropertyAccessor
   * @see Field#get(Object)
   * @see Field#set(Object, Object)
   * @see ReflectionUtils#getField(Field, Object)
   * @see ReflectionUtils#setField(Field, Object, Object)
   */
  public static PropertyAccessor forReflective(@Nullable Field field, @Nullable Method readMethod, @Nullable Method writeMethod) {
    if (writeMethod != null) {
      return new ReflectivePropertyAccessor(field, readMethod, writeMethod);
    }

    boolean readOnly = true;
    if (field != null) {
      readOnly = Modifier.isFinal(field.getModifiers());
    }
    else {
      Assert.notNull(readMethod, "read-method is required");
    }
    if (readOnly) {
      return new ReflectiveReadOnlyPropertyAccessor(field, readMethod);
    }
    return new ReflectivePropertyAccessor(field, readMethod, null);
  }

  //

  static class PublicPropertyAccessorGenerator extends GeneratorSupport<PropertyAccessor> implements ClassGenerator {
    private static final String superType = "Linfra/reflect/PropertyAccessor;";

    private static final String readOnlySuperType = "Linfra/reflect/ReadOnlyPropertyAccessor;";

    private static final MethodInfo getMethodInfo = MethodInfo.from(
            ReflectionUtils.getMethod(PropertyAccessor.class, "get", Object.class));

    private static final MethodInfo setMethodInfo = MethodInfo.from(
            ReflectionUtils.getMethod(PropertyAccessor.class, "set", Object.class, Object.class));

    private final Field field;

    @Nullable
    private final Method writeMethod;

    private final boolean isFinal;

    protected PublicPropertyAccessorGenerator(Field field, @Nullable Method writeMethod) {
      super(field.getDeclaringClass());
      this.field = field;
      this.writeMethod = writeMethod;
      this.isFinal = Modifier.isFinal(field.getModifiers());
    }

    @Override
    protected Object cacheKey() {
      return field;
    }

    @Override
    public String getSuperType() {
      return (isFinal && writeMethod == null) ? readOnlySuperType : superType;
    }

    @Override
    protected PropertyAccessor fallbackInstance() {
      return forReflective(field);
    }

    @Override
    protected boolean cannotAccess() {
      return Modifier.isPrivate(targetClass.getModifiers())
              || Modifier.isPrivate(field.getModifiers());
    }

    @Override
    protected ClassGenerator getClassGenerator() {
      return this;
    }

    @Override
    protected void appendClassName(StringBuilder builder) {
      builder.append('$')
              .append(field.getName());
    }

    @Override
    public void generateClass(ClassVisitor v) throws Exception {
      ClassEmitter classEmitter = beginClass(v);
      Type owner = Type.forClass(targetClass);
      Type type = Type.forClass(field.getType());
      String fieldName = field.getName();

      // get method
      generateGetMethod(classEmitter, owner, fieldName, type);
      if (!isFinal) {
        // set method
        generateSetMethod(classEmitter, owner, fieldName, type);
      }
      else if (writeMethod != null) {
        generateSetMethod(classEmitter, owner, writeMethod, type);
      }
      classEmitter.endClass();
    }

    private void generateGetMethod(ClassEmitter classEmitter, Type owner, String fieldName, Type type) {
      CodeEmitter code = EmitUtils.beginMethod(classEmitter, getMethodInfo, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL);
      if (Modifier.isStatic(field.getModifiers())) {
        code.getStatic(owner, fieldName, type);
      }
      else {
        code.loadArg(0);
        code.checkCast(owner);
        code.getField(owner, fieldName, type);
      }

      code.valueOf(type);

      code.returnValue();
      code.endMethod();
    }

    // public void set(Object obj, @Nullable Object value);

    private void generateSetMethod(ClassEmitter classEmitter, Type owner, String fieldName, Type type) {
      CodeEmitter code = EmitUtils.beginMethod(classEmitter, setMethodInfo, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL);

      if (Modifier.isStatic(field.getModifiers())) {
        code.loadArg(1);
        code.checkCast(type);
        code.putStatic(owner, fieldName, type);
      }
      else {
        code.loadArg(0);
        code.checkCast(owner);
        code.loadArg(1);
        checkCast(code, type);

        code.putField(owner, fieldName, type);
      }

      code.returnValue();
      code.endMethod();
    }

    private void generateSetMethod(ClassEmitter classEmitter, Type owner, Method writeMethod, Type type) {
      CodeEmitter code = EmitUtils.beginMethod(classEmitter, setMethodInfo, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL);

      if (!Modifier.isStatic(writeMethod.getModifiers())) {
        code.loadArg(0);
        code.checkCast(owner);
      }

      code.loadArg(1);
      checkCast(code, type);

      code.invoke(MethodInfo.from(writeMethod));

      code.returnValue();
      code.endMethod();
    }

  }

}
