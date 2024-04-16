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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import cn.taketoday.bytecode.ClassVisitor;
import cn.taketoday.bytecode.Opcodes;
import cn.taketoday.bytecode.Type;
import cn.taketoday.bytecode.core.ClassEmitter;
import cn.taketoday.bytecode.core.ClassGenerator;
import cn.taketoday.bytecode.core.CodeEmitter;
import cn.taketoday.bytecode.core.EmitUtils;
import cn.taketoday.bytecode.core.MethodInfo;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ReflectionUtils;

/**
 * @author TODAY 2020/9/11 11:06
 */
public abstract class PropertyAccessor implements SetterMethod, GetterMethod, Accessor {

  @Override
  public abstract Object get(Object obj) throws ReflectionException;

  @Override
  public abstract void set(Object obj, Object value) throws ReflectionException;

  /**
   * read-only ?
   *
   * @since 4.0
   */
  public boolean isReadOnly() {
    return false;
  }

  // static

  /**
   * PropertyAccessor
   *
   * @param field Field
   * @return PropertyAccessor
   */
  public static PropertyAccessor forField(Field field) {
    boolean isPublic = Modifier.isPublic(field.getModifiers());
    boolean isReadOnly = Modifier.isFinal(field.getModifiers());

    if (isPublic) {
      return new PublicPropertyAccessorGenerator(field, isReadOnly).create();
    }

    Method readMethod = ReflectionUtils.getReadMethod(field);
    if (isReadOnly && readMethod != null) {
      MethodInvoker invoker = MethodInvoker.forMethod(readMethod);
      return new ReadOnlyMethodAccessorPropertyAccessor(invoker);
    }
    Method writeMethod = ReflectionUtils.getWriteMethod(field);
    if (writeMethod != null && readMethod != null) {
      return forMethod(readMethod, writeMethod);
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
        public void set(Object obj, Object value) {
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
    if (writeMethod != null && readMethod != null) {
      return forMethod(readMethod, writeMethod);
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

  private static PropertyAccessor getPropertyAccessor(Field field, MethodInvoker accessor, @NonNull Method writeMethod) {
    return new PropertyAccessor() {
      @Override
      public Object get(Object obj) {
        return ReflectionUtils.getField(field, obj);
      }

      @Override
      public void set(Object obj, Object value) {
        accessor.invoke(obj, new Object[] { value });
      }

      @Override
      public Method getWriteMethod() {
        return writeMethod;
      }
    };
  }

  private static PropertyAccessor getPropertyAccessor(MethodInvoker accessor, Field field, @NonNull Method readMethod) {
    return new PropertyAccessor() {
      @Override
      public Object get(Object obj) {
        return accessor.invoke(obj, null);
      }

      @Override
      public void set(Object obj, Object value) {
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
    boolean readOnly;
    if (field != null) {
      ReflectionUtils.makeAccessible(field);
      readOnly = Modifier.isFinal(field.getModifiers());
    }
    else {
      Assert.notNull(readMethod, "read-method is required");
      readOnly = writeMethod == null;
    }
    if (readOnly) {
      return new ReflectiveReadOnlyPropertyAccessor(field, readMethod);
    }
    return new ReflectivePropertyAccessor(field, readMethod, writeMethod);
  }

  //

  static class PublicPropertyAccessorGenerator extends GeneratorSupport<PropertyAccessor> implements ClassGenerator {
    private static final String superType = "Lcn/taketoday/reflect/PropertyAccessor;";

    private static final String readOnlySuperType = "Lcn/taketoday/reflect/ReadOnlyPropertyAccessor;";

    private static final MethodInfo getMethodInfo = MethodInfo.from(
            ReflectionUtils.getMethod(PropertyAccessor.class, "get", Object.class));

    private static final MethodInfo setMethodInfo = MethodInfo.from(
            ReflectionUtils.getMethod(PropertyAccessor.class, "set", Object.class, Object.class));

    private final Field field;

    private final boolean isReadOnly;

    protected PublicPropertyAccessorGenerator(Field field, boolean isReadOnly) {
      super(field.getDeclaringClass());
      this.field = field;
      this.isReadOnly = isReadOnly;
    }

    @Override
    protected Object cacheKey() {
      return field;
    }

    @Override
    public String getSuperType() {
      return isReadOnly ? readOnlySuperType : superType;
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
      Type owner = Type.fromClass(targetClass);
      Type type = Type.fromClass(field.getType());
      String fieldName = field.getName();

      // get method
      generateGetMethod(classEmitter, owner, fieldName, type);
      if (!isReadOnly) {
        // set method
        generateSetMethod(classEmitter, owner, fieldName, type);
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
        code.unbox(type);

        code.putField(owner, fieldName, type);
      }

      code.returnValue();
      code.endMethod();
    }
  }

}
