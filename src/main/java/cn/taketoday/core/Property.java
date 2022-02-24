/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ConcurrentReferenceHashMap;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * A description of a JavaBeans Property that allows us to avoid a dependency on
 * {@code java.beans.PropertyDescriptor}. The {@code java.beans} package
 * is not available in a number of environments (e.g. Android, Java ME), so this is
 * desirable for portability of Spring's core conversion facility.
 *
 * <p>Used to build a {@link TypeDescriptor} from a property location. The built
 * {@code TypeDescriptor} can then be used to convert from/to the property type.
 *
 * @author Keith Donald
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see TypeDescriptor#TypeDescriptor(Property)
 * @see TypeDescriptor#nested(Property, int)
 * @since 4.0 2022/2/23 11:06
 */
public final class Property {
  private static final ConcurrentReferenceHashMap<Property, Annotation[]> annotationCache = new ConcurrentReferenceHashMap<>();

  private final Class<?> objectType;

  @Nullable
  private final Method readMethod;

  @Nullable
  private final Method writeMethod;

  private final String name;

  private final MethodParameter methodParameter;

  @Nullable
  private Annotation[] annotations;

  public Property(Class<?> objectType, @Nullable Method readMethod, @Nullable Method writeMethod) {
    this(objectType, readMethod, writeMethod, null);
  }

  public Property(
          Class<?> objectType, @Nullable Method readMethod, @Nullable Method writeMethod, @Nullable String name) {
    this.objectType = objectType;
    this.readMethod = readMethod;
    this.writeMethod = writeMethod;
    this.methodParameter = resolveMethodParameter();
    this.name = (name != null ? name : resolveName());
  }

  /**
   * The object declaring this property, either directly or in a superclass the object extends.
   */
  public Class<?> getObjectType() {
    return this.objectType;
  }

  /**
   * The name of the property: e.g. 'foo'
   */
  public String getName() {
    return this.name;
  }

  /**
   * The property type: e.g. {@code java.lang.String}
   */
  public Class<?> getType() {
    return this.methodParameter.getParameterType();
  }

  /**
   * The property getter method: e.g. {@code getFoo()}
   */
  @Nullable
  public Method getReadMethod() {
    return this.readMethod;
  }

  /**
   * The property setter method: e.g. {@code setFoo(String)}
   */
  @Nullable
  public Method getWriteMethod() {
    return this.writeMethod;
  }

  // Package private

  public MethodParameter getMethodParameter() {
    return this.methodParameter;
  }

  public Annotation[] getAnnotations() {
    if (this.annotations == null) {
      this.annotations = resolveAnnotations();
    }
    return this.annotations;
  }

  // Internal helpers

  private String resolveName() {
    if (this.readMethod != null) {
      int index = this.readMethod.getName().indexOf("get");
      if (index != -1) {
        index += 3;
      }
      else {
        index = this.readMethod.getName().indexOf("is");
        if (index != -1) {
          index += 2;
        }
        else {
          // Record-style plain accessor method, e.g. name()
          index = 0;
        }
      }
      return StringUtils.uncapitalize(this.readMethod.getName().substring(index));
    }
    else if (this.writeMethod != null) {
      int index = this.writeMethod.getName().indexOf("set");
      if (index == -1) {
        throw new IllegalArgumentException("Not a setter method");
      }
      index += 3;
      return StringUtils.uncapitalize(this.writeMethod.getName().substring(index));
    }
    else {
      throw new IllegalStateException("Property is neither readable nor writeable");
    }
  }

  private MethodParameter resolveMethodParameter() {
    MethodParameter read = resolveReadMethodParameter();
    MethodParameter write = resolveWriteMethodParameter();
    if (write == null) {
      if (read == null) {
        throw new IllegalStateException("Property is neither readable nor writeable");
      }
      return read;
    }
    if (read != null) {
      Class<?> readType = read.getParameterType();
      Class<?> writeType = write.getParameterType();
      if (!writeType.equals(readType) && writeType.isAssignableFrom(readType)) {
        return read;
      }
    }
    return write;
  }

  @Nullable
  private MethodParameter resolveReadMethodParameter() {
    if (getReadMethod() == null) {
      return null;
    }
    return new MethodParameter(getReadMethod(), -1).withContainingClass(getObjectType());
  }

  @Nullable
  private MethodParameter resolveWriteMethodParameter() {
    if (getWriteMethod() == null) {
      return null;
    }
    return new MethodParameter(getWriteMethod(), 0).withContainingClass(getObjectType());
  }

  private Annotation[] resolveAnnotations() {
    Annotation[] annotations = annotationCache.get(this);
    if (annotations == null) {
      Map<Class<? extends Annotation>, Annotation> annotationMap = new LinkedHashMap<>();
      addAnnotationsToMap(annotationMap, getReadMethod());
      addAnnotationsToMap(annotationMap, getWriteMethod());
      addAnnotationsToMap(annotationMap, getField());
      annotations = annotationMap.values().toArray(new Annotation[0]);
      annotationCache.put(this, annotations);
    }
    return annotations;
  }

  private void addAnnotationsToMap(
          Map<Class<? extends Annotation>, Annotation> annotationMap, @Nullable AnnotatedElement object) {

    if (object != null) {
      for (Annotation annotation : object.getAnnotations()) {
        annotationMap.put(annotation.annotationType(), annotation);
      }
    }
  }

  @Nullable
  private Field getField() {
    String name = getName();
    if (StringUtils.isEmpty(name)) {
      return null;
    }
    Field field = null;
    Class<?> declaringClass = declaringClass();
    if (declaringClass != null) {
      field = ReflectionUtils.findField(declaringClass, name);
      if (field == null) {
        // Same lenient fallback checking as in CachedIntrospectionResults...
        field = ReflectionUtils.findField(declaringClass, StringUtils.uncapitalize(name));
        if (field == null) {
          field = ReflectionUtils.findField(declaringClass, StringUtils.capitalize(name));
        }
      }
    }
    return field;
  }

  @Nullable
  private Class<?> declaringClass() {
    if (getReadMethod() != null) {
      return getReadMethod().getDeclaringClass();
    }
    else if (getWriteMethod() != null) {
      return getWriteMethod().getDeclaringClass();
    }
    else {
      return null;
    }
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof Property otherProperty)) {
      return false;
    }
    return Objects.equals(this.objectType, otherProperty.objectType)
            && Objects.equals(this.name, otherProperty.name)
            && Objects.equals(this.readMethod, otherProperty.readMethod)
            && Objects.equals(this.writeMethod, otherProperty.writeMethod);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.objectType) * 31 + Objects.hashCode(this.name);
  }

}
