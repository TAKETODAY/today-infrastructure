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

package cn.taketoday.context.utils;

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

import cn.taketoday.context.factory.BeanProperty;

/**
 * Generic Descriptor
 *
 * @author TODAY 2021/3/22 20:37
 * @since 3.0
 */
public class GenericDescriptor implements Serializable {
  private static final long serialVersionUID = 1L;
  private static final HashMap<Class<?>, GenericDescriptor> commonTypesCache = new HashMap<>(32);
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
   * @param field
   *         the field
   */
  public GenericDescriptor(Field field) {
    this.annotatedElement = field;
    this.resolvableType = ResolvableType.forField(field);
    this.type = this.resolvableType.resolve(field.getType());
  }

  public GenericDescriptor(BeanProperty property) {
    this.type = property.getType();
    this.annotatedElement = property;
    this.resolvableType = ResolvableType.forField(property.getField());
  }

  /**
   * Create a new type descriptor from a {@link ResolvableType}.
   * <p>This constructor is used internally and may also be used by subclasses
   * that support non-Java languages with extended type systems.
   *
   * @param resolvableType
   *         the resolvable type
   * @param type
   *         the backing type (or {@code null} if it should get resolved)
   * @param annotations
   *         the type annotations
   */
  public GenericDescriptor(ResolvableType resolvableType, Class<?> type, Annotation[] annotations) {
    this.resolvableType = resolvableType;
    this.type = (type != null ? type : resolvableType.toClass());
    this.annotatedElement = new AnnotatedElementAdapter(annotations);
  }

  public GenericDescriptor(ResolvableType resolvableType, Class<?> type, AnnotatedElement annotated) {
    this.annotatedElement = annotated;
    this.resolvableType = resolvableType;
    this.type = (type != null ? type : resolvableType.toClass());
  }

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

  public GenericDescriptor getGeneric(Class<?> genericIfc) {
    final ResolvableType generic = resolvableType.as(genericIfc).getGeneric(0);
    return getRelatedIfResolvable(this, generic);
  }

  /**
   * Return the underlying {@link ResolvableType}.
   */
  public ResolvableType getResolvableType() {
    return this.resolvableType;
  }

  /**
   * Return the underlying source of the descriptor. Will return a {@link Field},
   * {@link java.lang.reflect.Parameter} or {@link Type} depending on how the {@link GenericDescriptor}
   * was constructed. This method is primarily to provide access to additional
   * type information or meta-data that alternative JVM languages may provide.
   */
  public Object getSource() {
    return this.resolvableType.getSource();
  }

  /**
   * Narrows this {@link GenericDescriptor} by setting its type to the class of the
   * provided value.
   * <p>If the value is {@code null}, no narrowing is performed and this GenericDescriptor
   * is returned unchanged.
   * <p>Designed to be called by binding frameworks when they read property, field,
   * or method return values. Allows such frameworks to narrow a GenericDescriptor built
   * from a declared property, field, or method return value type. For example, a field
   * declared as {@code java.lang.Object} would be narrowed to {@code java.util.HashMap}
   * if it was set to a {@code java.util.HashMap} value. The narrowed GenericDescriptor
   * can then be used to convert the HashMap to some other type. Annotation and nested
   * type context is preserved by the narrowed copy.
   *
   * @param value
   *         the value to use for narrowing this type descriptor
   *
   * @return this GenericDescriptor narrowed (returns a copy with its type updated to the
   * class of the provided value)
   */
  public GenericDescriptor narrow(Object value) {
    if (value == null) {
      return this;
    }
    ResolvableType narrowed = ResolvableType.forType(value.getClass(), getResolvableType());
    return new GenericDescriptor(narrowed, value.getClass(), getAnnotations());
  }

  /**
   * Cast this {@link GenericDescriptor} to a superclass or implemented interface
   * preserving annotations and nested type context.
   *
   * @param superType
   *         the super type to cast to (can be {@code null})
   *
   * @return a new GenericDescriptor for the up-cast type
   *
   * @throws IllegalArgumentException
   *         if this type is not assignable to the super-type
   */

  public GenericDescriptor upcast(Class<?> superType) {
    if (superType == null) {
      return null;
    }
    Assert.isAssignable(superType, getType());
    return new GenericDescriptor(getResolvableType().as(superType), superType, getAnnotations());
  }

  /**
   * Is this type a primitive type?
   */
  public boolean isPrimitive() {
    return getType().isPrimitive();
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
   * <p>As of Spring Framework 4.2, this method supports arbitrary levels
   * of meta-annotations.
   *
   * @param annotationType
   *         the annotation type
   *
   * @return <tt>true</tt> if the annotation is present
   */
  public boolean hasAnnotation(Class<? extends Annotation> annotationType) {
    return ClassUtils.isAnnotationPresent(this.annotatedElement, annotationType);
  }

  /**
   * Obtain the annotation of the specified {@code annotationType} that is on this type descriptor.
   * <p>this method supports arbitrary levels of meta-annotations.
   *
   * @param annotationType
   *         the annotation type
   *
   * @return the annotation, or {@code null} if no such annotation exists on this type descriptor
   */
  public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
    return ClassUtils.getAnnotation(annotationType, this.annotatedElement);
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
  public boolean isAssignableTo(GenericDescriptor genericDescriptor) {
    boolean typesAssignable = genericDescriptor.getType().isAssignableFrom(getType());
    if (!typesAssignable) {
      return false;
    }
    if (isArray() && genericDescriptor.isArray()) {
      return isNestedAssignable(getElementDescriptor(), genericDescriptor.getElementDescriptor());
    }
    else if (isCollection() && genericDescriptor.isCollection()) {
      return isNestedAssignable(getElementDescriptor(), genericDescriptor.getElementDescriptor());
    }
    else if (isMap() && genericDescriptor.isMap()) {
      return isNestedAssignable(getMapKeyGenericDescriptor(), genericDescriptor.getMapKeyGenericDescriptor()) &&
              isNestedAssignable(getMapValueGenericDescriptor(), genericDescriptor.getMapValueGenericDescriptor());
    }
    else {
      return true;
    }
  }

  private boolean isNestedAssignable(GenericDescriptor nestedGenericDescriptor,
                                     GenericDescriptor otherNestedGenericDescriptor) {

    return (nestedGenericDescriptor == null || otherNestedGenericDescriptor == null ||
            nestedGenericDescriptor.isAssignableTo(otherNestedGenericDescriptor));
  }

  /**
   * If this type is an array, returns the array's component type.
   * If this type is a {@code Stream}, returns the stream's component type.
   * If this type is a {@link Collection} and it is parameterized, returns the Collection's element type.
   * If the Collection is not parameterized, returns {@code null} indicating the element type is not declared.
   *
   * @return the array component type or Collection element type, or {@code null} if this type is not
   * an array type or a {@code java.util.Collection} or if its element type is not parameterized
   *
   * @see #elementGenericDescriptor(Object)
   */
  public GenericDescriptor getElementDescriptor() {
    if (getResolvableType().isArray()) {
      return new GenericDescriptor(getResolvableType().getComponentType(), null, getAnnotations());
    }
    if (Stream.class.isAssignableFrom(getType())) {
      return getRelatedIfResolvable(this, getResolvableType().as(Stream.class).getGeneric(0));
    }
    return getRelatedIfResolvable(this, getResolvableType().asCollection().getGeneric(0));
  }

  /**
   * If this type is a {@link Collection} or an array, creates a element GenericDescriptor
   * from the provided collection or array element.
   * <p>Narrows the {@link #getElementDescriptor() elementType} property to the class
   * of the provided collection or array element. For example, if this describes a
   * {@code java.util.List&lt;java.lang.Number&lt;} and the element argument is an
   * {@code java.lang.Integer}, the returned GenericDescriptor will be {@code java.lang.Integer}.
   * If this describes a {@code java.util.List&lt;?&gt;} and the element argument is an
   * {@code java.lang.Integer}, the returned GenericDescriptor will be {@code java.lang.Integer}
   * as well.
   * <p>Annotation and nested type context will be preserved in the narrowed
   * GenericDescriptor that is returned.
   *
   * @param element
   *         the collection or array element
   *
   * @return a element type descriptor, narrowed to the type of the provided element
   *
   * @see #getElementDescriptor()
   * @see #narrow(Object)
   */
  public GenericDescriptor elementGenericDescriptor(Object element) {
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
   *
   * @throws IllegalStateException
   *         if this type is not a {@code java.util.Map}
   */
  public GenericDescriptor getMapKeyGenericDescriptor() {
    Assert.state(isMap(), "Not a [java.util.Map]");
    return getRelatedIfResolvable(this, getResolvableType().asMap().getGeneric(0));
  }

  /**
   * If this type is a {@link Map}, creates a mapKey {@link GenericDescriptor}
   * from the provided map key.
   * <p>Narrows the {@link #getMapKeyGenericDescriptor() mapKeyType} property
   * to the class of the provided map key. For example, if this describes a
   * {@code java.util.Map&lt;java.lang.Number, java.lang.String&lt;} and the key
   * argument is a {@code java.lang.Integer}, the returned GenericDescriptor will be
   * {@code java.lang.Integer}. If this describes a {@code java.util.Map&lt;?, ?&gt;}
   * and the key argument is a {@code java.lang.Integer}, the returned
   * GenericDescriptor will be {@code java.lang.Integer} as well.
   * <p>Annotation and nested type context will be preserved in the narrowed
   * GenericDescriptor that is returned.
   *
   * @param mapKey
   *         the map key
   *
   * @return the map key type descriptor
   *
   * @throws IllegalStateException
   *         if this type is not a {@code java.util.Map}
   * @see #narrow(Object)
   */
  public GenericDescriptor getMapKeyGenericDescriptor(Object mapKey) {
    return narrow(mapKey, getMapKeyGenericDescriptor());
  }

  /**
   * If this type is a {@link Map} and its value type is parameterized,
   * returns the map's value type.
   * <p>If the Map's value type is not parameterized, returns {@code null}
   * indicating the value type is not declared.
   *
   * @return the Map value type, or {@code null} if this type is a Map
   * but its value type is not parameterized
   *
   * @throws IllegalStateException
   *         if this type is not a {@code java.util.Map}
   */
  public GenericDescriptor getMapValueGenericDescriptor() {
    Assert.state(isMap(), "Not a [java.util.Map]");
    return getRelatedIfResolvable(this, getResolvableType().asMap().getGeneric(1));
  }

  /**
   * If this type is a {@link Map}, creates a mapValue {@link GenericDescriptor}
   * from the provided map value.
   * <p>Narrows the {@link #getMapValueGenericDescriptor() mapValueType} property
   * to the class of the provided map value. For example, if this describes a
   * {@code java.util.Map&lt;java.lang.String, java.lang.Number&lt;} and the value
   * argument is a {@code java.lang.Integer}, the returned GenericDescriptor will be
   * {@code java.lang.Integer}. If this describes a {@code java.util.Map&lt;?, ?&gt;}
   * and the value argument is a {@code java.lang.Integer}, the returned
   * GenericDescriptor will be {@code java.lang.Integer} as well.
   * <p>Annotation and nested type context will be preserved in the narrowed
   * GenericDescriptor that is returned.
   *
   * @param mapValue
   *         the map value
   *
   * @return the map value type descriptor
   *
   * @throws IllegalStateException
   *         if this type is not a {@code java.util.Map}
   * @see #narrow(Object)
   */
  public GenericDescriptor getMapValueGenericDescriptor(Object mapValue) {
    return narrow(mapValue, getMapValueGenericDescriptor());
  }

  private GenericDescriptor narrow(Object value, GenericDescriptor genericDescriptor) {
    if (genericDescriptor != null) {
      return genericDescriptor.narrow(value);
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
    if (!(other instanceof GenericDescriptor)) {
      return false;
    }
    GenericDescriptor otherDesc = (GenericDescriptor) other;
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
      return (Objects.equals(getMapKeyGenericDescriptor(), otherDesc.getMapKeyGenericDescriptor()) &&
              Objects.equals(getMapValueGenericDescriptor(), otherDesc.getMapValueGenericDescriptor()));
    }
    else {
      return true;
    }
  }

  private boolean annotationsMatch(GenericDescriptor otherDesc) {
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
    return getType().hashCode();
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
   * {@link #valueOf(Class)} to build a GenericDescriptor from the object's class.
   *
   * @param source
   *         the source object
   *
   * @return the type descriptor
   */
  public static GenericDescriptor forObject(Object source) {
    return (source != null ? valueOf(source.getClass()) : null);
  }

  /**
   * Create a new type descriptor from the given type.
   * <p>Use this to instruct the conversion system to convert an object to a
   * specific target type, when no type location such as a method parameter or
   * field is available to provide additional conversion context.
   * <p>Generally prefer use of {@link #forObject(Object)} for constructing type
   * descriptors from source objects, as it handles the {@code null} object case.
   *
   * @param type
   *         the class (may be {@code null} to indicate {@code Object.class})
   *
   * @return the corresponding type descriptor
   */
  public static GenericDescriptor valueOf(Class<?> type) {
    if (type == null) {
      type = Object.class;
    }
    GenericDescriptor desc = commonTypesCache.get(type);
    return (desc != null ? desc : new GenericDescriptor(ResolvableType.forClass(type), null, (Annotation[]) null));
  }

  public static GenericDescriptor collection(Class<?> collectionType, Class<?> element) {
    return collection(collectionType, valueOf(element));
  }

  /**
   * Create a new type descriptor from a {@link java.util.Collection} type.
   * <p>Useful for converting to typed Collections.
   * <p>For example, a {@code List<String>} could be converted to a
   * {@code List<EmailAddress>} by converting to a targetType built with this method.
   * The method call to construct such a {@code GenericDescriptor} would look something
   * like: {@code collection(List.class, GenericDescriptor.valueOf(EmailAddress.class));}
   *
   * @param collectionType
   *         the collection type, which must implement {@link Collection}.
   * @param elementDescriptor
   *         a descriptor for the collection's element type,
   *         used to convert collection elements
   *
   * @return the collection type descriptor
   */
  public static GenericDescriptor collection(Class<?> collectionType, GenericDescriptor elementDescriptor) {
    Assert.notNull(collectionType, "Collection type must not be null");
    if (!Collection.class.isAssignableFrom(collectionType)) {
      throw new IllegalArgumentException("Collection type must be a [java.util.Collection]");
    }
    ResolvableType element = (elementDescriptor != null ? elementDescriptor.resolvableType : null);
    return new GenericDescriptor(ResolvableType.forClassWithGenerics(collectionType, element), null, (Annotation[]) null);
  }

  public static GenericDescriptor map(Class<?> mapType, Class<?> key, Class<?> value) {
    return map(mapType, valueOf(key), valueOf(value));
  }

  /**
   * Create a new type descriptor from a {@link java.util.Map} type.
   * <p>Useful for converting to typed Maps.
   * <p>For example, a Map&lt;String, String&gt; could be converted to a Map&lt;Id, EmailAddress&gt;
   * by converting to a targetType built with this method:
   * The method call to construct such a GenericDescriptor would look something like:
   * <pre class="code">
   * map(Map.class, GenericDescriptor.valueOf(Id.class), GenericDescriptor.valueOf(EmailAddress.class));
   * </pre>
   *
   * @param mapType
   *         the map type, which must implement {@link Map}
   * @param keyDescriptor
   *         a descriptor for the map's key type, used to convert map keys
   * @param valueDescriptor
   *         the map's value type, used to convert map values
   *
   * @return the map type descriptor
   */
  public static GenericDescriptor map(
          Class<?> mapType, GenericDescriptor keyDescriptor, GenericDescriptor valueDescriptor) {
    Assert.notNull(mapType, "Map type must not be null");
    if (!Map.class.isAssignableFrom(mapType)) {
      throw new IllegalArgumentException("Map type must be a [java.util.Map]");
    }
    ResolvableType key = (keyDescriptor != null ? keyDescriptor.resolvableType : null);
    ResolvableType value = (valueDescriptor != null ? valueDescriptor.resolvableType : null);
    return new GenericDescriptor(ResolvableType.forClassWithGenerics(mapType, key, value), null, (Annotation[]) null);
  }

  /**
   * Create a new type descriptor as an array of the specified type.
   * <p>For example to create a {@code Map<String,String>[]} use:
   * <pre class="code">
   * GenericDescriptor.array(GenericDescriptor.map(Map.class, GenericDescriptor.value(String.class), GenericDescriptor.value(String.class)));
   * </pre>
   *
   * @param elementDescriptor
   *         the {@link GenericDescriptor} of the array element or {@code null}
   *
   * @return an array {@link GenericDescriptor} or {@code null} if {@code elementGenericDescriptor} is {@code null}
   */
  public static GenericDescriptor array(GenericDescriptor elementDescriptor) {
    if (elementDescriptor == null) {
      return null;
    }
    return new GenericDescriptor(
            ResolvableType.forArrayComponent(elementDescriptor.resolvableType),
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
   * @param field
   *         the field
   * @param nestingLevel
   *         the nesting level of the collection/array element or
   *         map key/value declaration within the field
   *
   * @return the nested type descriptor at the specified nesting level,
   * or {@code null} if it could not be obtained
   *
   * @throws IllegalArgumentException
   *         if the types up to the specified nesting
   *         level are not of collection, array, or map types
   */
  public static GenericDescriptor nested(Field field, int nestingLevel) {
    return nested(new GenericDescriptor(field), nestingLevel);
  }

  public static GenericDescriptor nested(GenericDescriptor genericDescriptor, int nestingLevel) {
    ResolvableType nested = genericDescriptor.resolvableType;
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
    return getRelatedIfResolvable(genericDescriptor, nested);
  }

  private static GenericDescriptor getRelatedIfResolvable(GenericDescriptor source, ResolvableType type) {
    if (type.resolve() == null) {
      return null;
    }
    return new GenericDescriptor(type, null, source.getAnnotations());
  }

  public static GenericDescriptor ofProperty(Field beanProperty) {
    return new GenericDescriptor(beanProperty);
  }

  public static GenericDescriptor ofProperty(BeanProperty beanProperty) {
    return new GenericDescriptor(beanProperty);
  }

  public static GenericDescriptor ofParameter(final Executable executable, int parameterIndex) {
    final Parameter parameter = ClassUtils.getParameter(executable, parameterIndex);
    return ofParameter(parameter);
  }

  /**
   * @since 3.0.2
   */
  public static GenericDescriptor ofParameter(Parameter parameter) {
    final ResolvableType resolvableType = ResolvableType.forParameter(parameter);
    return new GenericDescriptor(resolvableType, parameter.getType(), parameter);
  }

  /**
   * Adapter class for exposing a {@code GenericDescriptor}'s annotations as an
   * {@link AnnotatedElement}, in particular to {@link ClassUtils}.
   *
   * @see ClassUtils#isAnnotationPresent(AnnotatedElement, Class)
   * @see ClassUtils#getAnnotation(AnnotatedElement, Class)
   */
  class AnnotatedElementAdapter extends cn.taketoday.context.utils.AnnotatedElementAdapter {

    public AnnotatedElementAdapter(Annotation[] annotations) {
      super(annotations);
    }

    @Override
    public String toString() {
      return GenericDescriptor.this.toString();
    }
  }

}
