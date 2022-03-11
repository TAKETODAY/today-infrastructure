/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.core;

import java.io.Serial;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.AnnotationsAnnotatedElementAdapter;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ReflectionUtils;

/**
 * Contextual descriptor about a type to convert from or to.
 * Capable of representing arrays and generic collection types.
 *
 * @author TODAY 2021/3/22 20:37
 * @since 3.0
 */
public class TypeDescriptor implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  private static final HashMap<Class<?>, TypeDescriptor> commonTypesCache = new HashMap<>(32);
  private static final Class<?>[] CACHED_COMMON_TYPES = {
          boolean.class, Boolean.class, byte.class, Byte.class, char.class, Character.class,
          double.class, Double.class, float.class, Float.class, int.class, Integer.class,
          long.class, Long.class, short.class, Short.class, String.class, Object.class
  };

  static {
    for (Class<?> preCachedClass : CACHED_COMMON_TYPES) {
      commonTypesCache.put(preCachedClass, valueOf(preCachedClass));
    }
  }

  private final Class<?> type;
  private final ResolvableType resolvableType;
  private final AnnotatedElement annotatedElement;

  /**
   * Create a new type descriptor from a {@link Field}.
   * <p>Use this constructor when a source or target conversion point is a field.
   *
   * @param field the field
   */
  public TypeDescriptor(Field field) {
    this.resolvableType = ResolvableType.fromField(field);
    this.type = this.resolvableType.resolve(field.getType());
    this.annotatedElement = new TypeDescriptorAnnotatedElementAdapter(field.getAnnotations());
  }

  /**
   * Create a new type descriptor from a {@link ResolvableType}.
   * <p>This constructor is used internally and may also be used by subclasses
   * that support non-Java languages with extended type systems.
   *
   * @param resolvableType the resolvable type
   * @param type the backing type (or {@code null} if it should get resolved)
   * @param annotations the type annotations
   */
  public TypeDescriptor(
          ResolvableType resolvableType, @Nullable Class<?> type, @Nullable Annotation[] annotations) {
    this.resolvableType = resolvableType;
    this.type = type != null ? type : resolvableType.toClass();
    this.annotatedElement = new TypeDescriptorAnnotatedElementAdapter(annotations);
  }

  /**
   * Create a new type descriptor from a {@link ResolvableType}.
   * <p>This constructor is used internally and may also be used by subclasses
   * that support non-Java languages with extended type systems.
   *
   * @param resolvableType the resolvable type
   * @param type the backing type (or {@code null} if it should get resolved)
   * @param annotated annotated-element
   * @since 4.0
   */
  public TypeDescriptor(ResolvableType resolvableType, @Nullable Class<?> type, AnnotatedElement annotated) {
    this.annotatedElement = annotated;
    this.resolvableType = resolvableType;
    this.type = type != null ? type : resolvableType.toClass();
  }

  /**
   * Create a new type descriptor from a {@link MethodParameter}.
   * <p>Use this constructor when a source or target conversion point is a
   * constructor parameter, method parameter, or method return value.
   *
   * @param methodParameter the method parameter
   * @since 4.0
   */
  public TypeDescriptor(MethodParameter methodParameter) {
    this.resolvableType = ResolvableType.forMethodParameter(methodParameter);
    this.type = this.resolvableType.resolve(methodParameter.getNestedParameterType());
    this.annotatedElement = new TypeDescriptorAnnotatedElementAdapter(
            methodParameter.getParameterIndex() == -1 ?
            methodParameter.getMethodAnnotations() : methodParameter.getParameterAnnotations());
  }

  /**
   * Variation of {@link #getType()} that accounts for a primitive type by
   * returning its object wrapper type.
   * <p>This is useful for conversion service implementations that wish to
   * normalize to object-based types and not work with primitive types directly.
   */
  public Class<?> getObjectType() {
    return ClassUtils.resolvePrimitiveIfNecessary(getType());
  }

  /**
   * The type of the backing class, method parameter, field, or property
   * described by this TypeDescriptor.
   * <p>Returns primitive types as-is. See {@link #getObjectType()} for a
   * variation of this operation that resolves primitive types to their
   * corresponding Object types if necessary.
   *
   * @see #getObjectType()
   */
  public Class<?> getType() {
    return type;
  }

  public boolean isArray() {
    return type.isArray();
  }

  public boolean isCollection() {
    return CollectionUtils.isCollection(type);
  }

  public Class<?> getComponentType() {
    return type.getComponentType();
  }

  public boolean isInstance(Object source) {
    return type.isInstance(source);
  }

  public boolean is(Class<?> testClass) {
    return type == testClass;
  }

  public boolean isAssignableFrom(Class<?> subType) {
    return type.isAssignableFrom(subType);
  }

  public boolean isAssignableTo(Class<?> superType) {
    return superType.isAssignableFrom(type);
  }

  public boolean isEnum() {
    return type.isEnum();
  }

  /**
   * Return the name of this type: the fully qualified class name.
   */
  public Object getName() {
    return ClassUtils.getQualifiedName(getType());
  }

  public String getSimpleName() {
    return type.getSimpleName();
  }

  /**
   * Return the underlying {@link ResolvableType}.
   */
  public ResolvableType getResolvableType() {
    return this.resolvableType;
  }

  /**
   * Return the underlying source of the descriptor. Will return a {@link Field},
   * {@link java.lang.reflect.Parameter} or {@link Type} depending on how the {@link TypeDescriptor}
   * was constructed. This method is primarily to provide access to additional
   * type information or meta-data that alternative JVM languages may provide.
   */
  public Object getSource() {
    return this.resolvableType.getSource();
  }

  /**
   * Narrows this {@link TypeDescriptor} by setting its type to the class of the
   * provided value.
   * <p>If the value is {@code null}, no narrowing is performed and this TypeDescriptor
   * is returned unchanged.
   * <p>Designed to be called by binding frameworks when they read property, field,
   * or method return values. Allows such frameworks to narrow a TypeDescriptor built
   * from a declared property, field, or method return value type. For example, a field
   * declared as {@code java.lang.Object} would be narrowed to {@code java.util.HashMap}
   * if it was set to a {@code java.util.HashMap} value. The narrowed TypeDescriptor
   * can then be used to convert the HashMap to some other type. Annotation and nested
   * type context is preserved by the narrowed copy.
   *
   * @param value the value to use for narrowing this type descriptor
   * @return this TypeDescriptor narrowed (returns a copy with its type updated to the
   * class of the provided value)
   */
  public TypeDescriptor narrow(Object value) {
    if (value == null) {
      return this;
    }
    ResolvableType narrowed = ResolvableType.fromType(value.getClass(), getResolvableType());
    return new TypeDescriptor(narrowed, value.getClass(), getAnnotations());
  }

  /**
   * Cast this {@link TypeDescriptor} to a superclass or implemented interface
   * preserving annotations and nested type context.
   *
   * @param superType the super type to cast to (can be {@code null})
   * @return a new TypeDescriptor for the up-cast type
   * @throws IllegalArgumentException if this type is not assignable to the super-type
   */
  @Nullable
  public TypeDescriptor upcast(Class<?> superType) {
    if (superType == null) {
      return null;
    }
    Assert.isAssignable(superType, getType());
    return new TypeDescriptor(getResolvableType().as(superType), superType, getAnnotations());
  }

  /**
   * Is this type a primitive type?
   */
  public boolean isPrimitive() {
    return type.isPrimitive();
  }

  /**
   * Return the annotations associated with this type descriptor, if any.
   *
   * @return the annotations, or an empty array if none
   */
  public Annotation[] getAnnotations() {
    return this.annotatedElement.getAnnotations();
  }

  /**
   * Determine if this type descriptor has the specified annotation.
   *
   * @param annotationType the annotation type
   * @return <tt>true</tt> if the annotation is present
   */
  public boolean hasAnnotation(Class<? extends Annotation> annotationType) {
    return AnnotatedElementUtils.hasAnnotation(this.annotatedElement, annotationType);
  }

  /**
   * Obtain the annotation of the specified {@code annotationType} that is on this type descriptor.
   * <p>this method supports arbitrary levels of meta-annotations.
   *
   * @param annotationType the annotation type
   * @return the annotation, or {@code null} if no such annotation exists on this type descriptor
   */
  @Nullable
  public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
    return AnnotationUtils.findAnnotation(this.annotatedElement, annotationType);
  }

  /**
   * Returns true if an object of this type descriptor can be assigned to the location
   * described by the given type descriptor.
   * <p>For example, {@code valueOf(String.class).isAssignableTo(valueOf(CharSequence.class))}
   * returns {@code true} because a String value can be assigned to a CharSequence variable.
   * On the other hand, {@code valueOf(Number.class).isAssignableTo(valueOf(Integer.class))}
   * returns {@code false} because, while all Integers are Numbers, not all Numbers are Integers.
   * <p>For arrays, collections, and maps, element and key/value types are checked if declared.
   * For example, a List&lt;String&gt; field value is assignable to a Collection&lt;CharSequence&gt;
   * field, but List&lt;Number&gt; is not assignable to List&lt;Integer&gt;.
   *
   * @return {@code true} if this type is assignable to the type represented by the provided
   * type descriptor
   */
  public boolean isAssignableTo(TypeDescriptor typeDescriptor) {
    boolean typesAssignable = typeDescriptor.getObjectType().isAssignableFrom(getObjectType());
    if (!typesAssignable) {
      return false;
    }
    if (isArray() && typeDescriptor.isArray()) {
      return isNestedAssignable(getElementDescriptor(), typeDescriptor.getElementDescriptor());
    }
    else if (isCollection() && typeDescriptor.isCollection()) {
      return isNestedAssignable(getElementDescriptor(), typeDescriptor.getElementDescriptor());
    }
    else if (isMap() && typeDescriptor.isMap()) {
      return isNestedAssignable(getMapKeyDescriptor(), typeDescriptor.getMapKeyDescriptor()) &&
              isNestedAssignable(getMapValueDescriptor(), typeDescriptor.getMapValueDescriptor());
    }
    else {
      return true;
    }
  }

  private boolean isNestedAssignable(TypeDescriptor nestedTypeDescriptor,
          TypeDescriptor otherNestedTypeDescriptor) {

    return (nestedTypeDescriptor == null || otherNestedTypeDescriptor == null ||
            nestedTypeDescriptor.isAssignableTo(otherNestedTypeDescriptor));
  }

  /**
   * If this type is an array, returns the array's component type.
   * If this type is a {@code Stream}, returns the stream's component type.
   * If this type is a {@link Collection} and it is parameterized, returns the Collection's element type.
   * If the Collection is not parameterized, returns {@code null} indicating the element type is not declared.
   *
   * @return the array component type or Collection element type, or {@code null} if this type is not
   * an array type or a {@code java.util.Collection} or if its element type is not parameterized
   * @see #elementDescriptor(Object)
   */
  @Nullable
  public TypeDescriptor getElementDescriptor() {
    if (getResolvableType().isArray()) {
      return new TypeDescriptor(getResolvableType().getComponentType(), null, getAnnotations());
    }
    if (Stream.class.isAssignableFrom(getType())) {
      return getRelatedIfResolvable(this, getResolvableType().as(Stream.class).getGeneric(0));
    }
    return getRelatedIfResolvable(this, getResolvableType().asCollection().getGeneric(0));
  }

  /**
   * If this type is a {@link Collection} or an array, creates a element TypeDescriptor
   * from the provided collection or array element.
   * <p>Narrows the {@link #getElementDescriptor() elementType} property to the class
   * of the provided collection or array element. For example, if this describes a
   * {@code java.util.List&lt;java.lang.Number&lt;} and the element argument is an
   * {@code java.lang.Integer}, the returned TypeDescriptor will be {@code java.lang.Integer}.
   * If this describes a {@code java.util.List&lt;?&gt;} and the element argument is an
   * {@code java.lang.Integer}, the returned TypeDescriptor will be {@code java.lang.Integer}
   * as well.
   * <p>Annotation and nested type context will be preserved in the narrowed
   * TypeDescriptor that is returned.
   *
   * @param element the collection or array element
   * @return a element type descriptor, narrowed to the type of the provided element
   * @see #getElementDescriptor()
   * @see #narrow(Object)
   */
  @Nullable
  public TypeDescriptor elementDescriptor(Object element) {
    return narrow(element, getElementDescriptor());
  }

  /**
   * Is this type a {@link Map} type?
   */
  public boolean isMap() {
    return Map.class.isAssignableFrom(getType());
  }

  /**
   * If this type is a {@link Map} and its key type is parameterized,
   * returns the map's key type. If the Map's key type is not parameterized,
   * returns {@code null} indicating the key type is not declared.
   *
   * @return the Map key type, or {@code null} if this type is a Map
   * but its key type is not parameterized
   * @throws IllegalStateException if this type is not a {@code java.util.Map}
   */
  @Nullable
  public TypeDescriptor getMapKeyDescriptor() {
    Assert.state(isMap(), "Not a [java.util.Map]");
    return getRelatedIfResolvable(this, getResolvableType().asMap().getGeneric(0));
  }

  /**
   * If this type is a {@link Map}, creates a mapKey {@link TypeDescriptor}
   * from the provided map key.
   * <p>Narrows the {@link #getMapKeyDescriptor() mapKeyType} property
   * to the class of the provided map key. For example, if this describes a
   * {@code java.util.Map&lt;java.lang.Number, java.lang.String&lt;} and the key
   * argument is a {@code java.lang.Integer}, the returned TypeDescriptor will be
   * {@code java.lang.Integer}. If this describes a {@code java.util.Map&lt;?, ?&gt;}
   * and the key argument is a {@code java.lang.Integer}, the returned
   * TypeDescriptor will be {@code java.lang.Integer} as well.
   * <p>Annotation and nested type context will be preserved in the narrowed
   * TypeDescriptor that is returned.
   *
   * @param mapKey the map key
   * @return the map key type descriptor
   * @throws IllegalStateException if this type is not a {@code java.util.Map}
   * @see #narrow(Object)
   */
  @Nullable
  public TypeDescriptor getMapKeyDescriptor(Object mapKey) {
    return narrow(mapKey, getMapKeyDescriptor());
  }

  /**
   * If this type is a {@link Map} and its value type is parameterized,
   * returns the map's value type.
   * <p>If the Map's value type is not parameterized, returns {@code null}
   * indicating the value type is not declared.
   *
   * @return the Map value type, or {@code null} if this type is a Map
   * but its value type is not parameterized
   * @throws IllegalStateException if this type is not a {@code java.util.Map}
   */
  @Nullable
  public TypeDescriptor getMapValueDescriptor() {
    Assert.state(isMap(), "Not a [java.util.Map]");
    return getRelatedIfResolvable(this, getResolvableType().asMap().getGeneric(1));
  }

  /**
   * If this type is a {@link Map}, creates a mapValue {@link TypeDescriptor}
   * from the provided map value.
   * <p>Narrows the {@link #getMapValueDescriptor() mapValueType} property
   * to the class of the provided map value. For example, if this describes a
   * {@code java.util.Map&lt;java.lang.String, java.lang.Number&lt;} and the value
   * argument is a {@code java.lang.Integer}, the returned TypeDescriptor will be
   * {@code java.lang.Integer}. If this describes a {@code java.util.Map&lt;?, ?&gt;}
   * and the value argument is a {@code java.lang.Integer}, the returned
   * TypeDescriptor will be {@code java.lang.Integer} as well.
   * <p>Annotation and nested type context will be preserved in the narrowed
   * TypeDescriptor that is returned.
   *
   * @param mapValue the map value
   * @return the map value type descriptor
   * @throws IllegalStateException if this type is not a {@code java.util.Map}
   * @see #narrow(Object)
   */
  @Nullable
  public TypeDescriptor getMapValueDescriptor(Object mapValue) {
    return narrow(mapValue, getMapValueDescriptor());
  }

  @Nullable
  private TypeDescriptor narrow(Object value, TypeDescriptor typeDescriptor) {
    if (typeDescriptor != null) {
      return typeDescriptor.narrow(value);
    }
    if (value != null) {
      return narrow(value);
    }
    return null;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof TypeDescriptor otherDesc)) {
      return false;
    }
    if (getType() != otherDesc.getType()) {
      return false;
    }
    if (!annotationsMatch(otherDesc)) {
      return false;
    }
    if (isCollection() || isArray()) {
      return Objects.equals(getElementDescriptor(), otherDesc.getElementDescriptor());
    }
    else if (isMap()) {
      return (Objects.equals(getMapKeyDescriptor(), otherDesc.getMapKeyDescriptor()) &&
              Objects.equals(getMapValueDescriptor(), otherDesc.getMapValueDescriptor()));
    }
    else {
      return true;
    }
  }

  private boolean annotationsMatch(TypeDescriptor otherDesc) {
    Annotation[] anns = getAnnotations();
    Annotation[] otherAnns = otherDesc.getAnnotations();
    if (anns == otherAnns) {
      return true;
    }
    if (anns.length != otherAnns.length) {
      return false;
    }
    if (anns.length > 0) {
      for (int i = 0; i < anns.length; i++) {
        if (!annotationEquals(anns[i], otherAnns[i])) {
          return false;
        }
      }
    }
    return true;
  }

  private boolean annotationEquals(Annotation ann, Annotation otherAnn) {
    // Annotation.equals is reflective and pretty slow, so let's check identity and proxy type first.
    return (ann == otherAnn || (ann.getClass() == otherAnn.getClass() && ann.equals(otherAnn)));
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, resolvableType, annotatedElement);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    for (Annotation ann : getAnnotations()) {
      builder.append("@").append(ann.annotationType().getName()).append(' ');
    }
    builder.append(getResolvableType());
    return builder.toString();
  }

  // static factory methods

  /**
   * Create a new type descriptor for an object.
   * <p>Use this factory method to introspect a source object before asking the
   * conversion system to convert it to some another type.
   * <p>If the provided object is {@code null}, returns {@code null}, else calls
   * {@link #valueOf(Class)} to build a TypeDescriptor from the object's class.
   *
   * @param source the source object
   * @return the type descriptor
   */
  @Nullable
  public static TypeDescriptor fromObject(@Nullable Object source) {
    return source != null ? valueOf(source.getClass()) : null;
  }

  /**
   * Create a new type descriptor from the given type.
   * <p>Use this to instruct the conversion system to convert an object to a
   * specific target type, when no type location such as a method parameter or
   * field is available to provide additional conversion context.
   * <p>Generally prefer use of {@link #fromObject(Object)} for constructing type
   * descriptors from source objects, as it handles the {@code null} object case.
   *
   * @param type the class (may be {@code null} to indicate {@code Object.class})
   * @return the corresponding type descriptor
   */
  public static TypeDescriptor valueOf(Class<?> type) {
    if (type == null) {
      type = Object.class;
    }
    TypeDescriptor desc = commonTypesCache.get(type);
    return (desc != null ? desc : new TypeDescriptor(ResolvableType.fromClass(type), null, (Annotation[]) null));
  }

  public static TypeDescriptor collection(Class<?> collectionType, Class<?> element) {
    return collection(collectionType, valueOf(element));
  }

  /**
   * Create a new type descriptor from a {@link java.util.Collection} type.
   * <p>Useful for converting to typed Collections.
   * <p>For example, a {@code List<String>} could be converted to a
   * {@code List<EmailAddress>} by converting to a targetType built with this method.
   * The method call to construct such a {@code TypeDescriptor} would look something
   * like: {@code collection(List.class, TypeDescriptor.valueOf(EmailAddress.class));}
   *
   * @param collectionType the collection type, which must implement {@link Collection}.
   * @param elementDescriptor a descriptor for the collection's element type,
   * used to convert collection elements
   * @return the collection type descriptor
   */
  public static TypeDescriptor collection(Class<?> collectionType, TypeDescriptor elementDescriptor) {
    Assert.notNull(collectionType, "Collection type must not be null");
    if (!Collection.class.isAssignableFrom(collectionType)) {
      throw new IllegalArgumentException("Collection type must be a [java.util.Collection]");
    }
    ResolvableType element = (elementDescriptor != null ? elementDescriptor.resolvableType : null);
    return new TypeDescriptor(ResolvableType.fromClassWithGenerics(collectionType, element), null, (Annotation[]) null);
  }

  public static TypeDescriptor map(Class<?> mapType, Class<?> key, Class<?> value) {
    return map(mapType, valueOf(key), valueOf(value));
  }

  /**
   * Create a new type descriptor from a {@link java.util.Map} type.
   * <p>Useful for converting to typed Maps.
   * <p>For example, a Map&lt;String, String&gt; could be converted to a Map&lt;Id, EmailAddress&gt;
   * by converting to a targetType built with this method:
   * The method call to construct such a TypeDescriptor would look something like:
   * <pre class="code">
   * map(Map.class, TypeDescriptor.valueOf(Id.class), TypeDescriptor.valueOf(EmailAddress.class));
   * </pre>
   *
   * @param mapType the map type, which must implement {@link Map}
   * @param keyDescriptor a descriptor for the map's key type, used to convert map keys
   * @param valueDescriptor the map's value type, used to convert map values
   * @return the map type descriptor
   */
  public static TypeDescriptor map(
          Class<?> mapType, TypeDescriptor keyDescriptor, TypeDescriptor valueDescriptor) {
    Assert.notNull(mapType, "Map type must not be null");
    if (!Map.class.isAssignableFrom(mapType)) {
      throw new IllegalArgumentException("Map type must be a [java.util.Map]");
    }
    ResolvableType key = (keyDescriptor != null ? keyDescriptor.resolvableType : null);
    ResolvableType value = (valueDescriptor != null ? valueDescriptor.resolvableType : null);
    return new TypeDescriptor(ResolvableType.fromClassWithGenerics(mapType, key, value), null, (Annotation[]) null);
  }

  /**
   * Create a new type descriptor as an array of the specified type.
   * <p>For example to create a {@code Map<String,String>[]} use:
   * <pre class="code">
   * TypeDescriptor.array(TypeDescriptor.map(Map.class, TypeDescriptor.value(String.class), TypeDescriptor.value(String.class)));
   * </pre>
   *
   * @param elementDescriptor the {@link TypeDescriptor} of the array element or {@code null}
   * @return an array {@link TypeDescriptor} or {@code null} if {@code elementDescriptor} is {@code null}
   */
  public static TypeDescriptor array(TypeDescriptor elementDescriptor) {
    if (elementDescriptor == null) {
      return null;
    }
    return new TypeDescriptor(
            ResolvableType.fromArrayComponent(elementDescriptor.resolvableType),
            null, elementDescriptor.getAnnotations());
  }

  /**
   * Create a type descriptor for a nested type declared within the field.
   * <p>For example, if the field is a {@code List<String>} and the nesting
   * level is 1, the nested type descriptor will be {@code String.class}.
   * <p>If the field is a {@code List<List<String>>} and the nesting level is
   * 2, the nested type descriptor will also be a {@code String.class}.
   * <p>If the field is a {@code Map<Integer, String>} and the nesting level
   * is 1, the nested type descriptor will be String, derived from the map value.
   * <p>If the field is a {@code List<Map<Integer, String>>} and the nesting
   * level is 2, the nested type descriptor will be String, derived from the map value.
   * <p>Returns {@code null} if a nested type cannot be obtained because it was not
   * declared. For example, if the field is a {@code List<?>}, the nested type
   * descriptor returned will be {@code null}.
   *
   * @param field the field
   * @param nestingLevel the nesting level of the collection/array element or
   * map key/value declaration within the field
   * @return the nested type descriptor at the specified nesting level,
   * or {@code null} if it could not be obtained
   * @throws IllegalArgumentException if the types up to the specified nesting
   * level are not of collection, array, or map types
   */
  public static TypeDescriptor nested(Field field, int nestingLevel) {
    return nested(new TypeDescriptor(field), nestingLevel);
  }

  public static TypeDescriptor nested(TypeDescriptor typeDescriptor, int nestingLevel) {
    ResolvableType nested = typeDescriptor.resolvableType;
    for (int i = 0; i < nestingLevel; i++) {
      if (Object.class != nested.getType()) {
        nested = nested.getNested(2);
      }
      // else {
      // Could be a collection type but we don't know about its element type,
      // so let's just assume there is an element type of type Object...
      // }
    }
    if (nested == ResolvableType.NONE) {
      return null;
    }
    return getRelatedIfResolvable(typeDescriptor, nested);
  }

  @Nullable
  private static TypeDescriptor getRelatedIfResolvable(TypeDescriptor source, ResolvableType type) {
    if (type.resolve() == null) {
      return null;
    }
    return new TypeDescriptor(type, null, source.getAnnotations());
  }

  public static TypeDescriptor fromField(Field beanProperty) {
    return new TypeDescriptor(beanProperty);
  }

  public static TypeDescriptor forParameter(Executable executable, int parameterIndex) {
    Parameter parameter = ReflectionUtils.getParameter(executable, parameterIndex);
    return fromParameter(parameter);
  }

  /**
   * @since 3.0.2
   */
  public static TypeDescriptor fromParameter(Parameter parameter) {
    ResolvableType resolvableType = ResolvableType.fromParameter(parameter);
    return new TypeDescriptor(resolvableType, parameter.getType(), parameter);
  }

  /**
   * Adapter class for exposing a {@code TypeDescriptor}'s annotations as an
   * {@link AnnotatedElement}, in particular to {@link ClassUtils}.
   *
   * @see AnnotationUtils#isPresent(AnnotatedElement, Class)
   * @see AnnotationUtils#getAnnotation(AnnotatedElement, Class)
   */
  class TypeDescriptorAnnotatedElementAdapter extends AnnotationsAnnotatedElementAdapter {

    public TypeDescriptorAnnotatedElementAdapter(Annotation[] annotations) {
      super(annotations);
    }

    @Override
    public String toString() {
      return TypeDescriptor.this.toString();
    }
  }

}
