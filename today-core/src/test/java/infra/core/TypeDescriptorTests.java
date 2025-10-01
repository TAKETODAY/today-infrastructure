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

package infra.core;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import infra.reflect.Property;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author TODAY 2021/3/23 11:50
 * @since 3.0
 */
class TypeDescriptorTests {

  @Test
  void parameterPrimitive() throws Exception {
    TypeDescriptor desc = new TypeDescriptor(new MethodParameter(getClass().getMethod("testParameterPrimitive", int.class), 0));
    assertThat(desc.getType()).isEqualTo(int.class);
    assertThat(desc.getObjectType()).isEqualTo(Integer.class);
    assertThat(desc.getName()).isEqualTo("int");
    assertThat(desc.toString()).isEqualTo("int");
    assertThat(desc.isPrimitive()).isTrue();
    assertThat(desc.getAnnotations()).isEmpty();
    assertThat(desc.isCollection()).isFalse();
    assertThat(desc.isMap()).isFalse();
  }

  @Test
  void parameterScalar() throws Exception {
    TypeDescriptor desc = new TypeDescriptor(new MethodParameter(getClass().getMethod("testParameterScalar", String.class), 0));
    assertThat(desc.getType()).isEqualTo(String.class);
    assertThat(desc.getObjectType()).isEqualTo(String.class);
    assertThat(desc.getName()).isEqualTo("java.lang.String");
    assertThat(desc.toString()).isEqualTo("java.lang.String");
    assertThat(desc.isPrimitive()).isFalse();
    assertThat(desc.getAnnotations()).isEmpty();
    assertThat(desc.isCollection()).isFalse();
    assertThat(desc.isArray()).isFalse();
    assertThat(desc.isMap()).isFalse();
  }

  @Test
  void parameterList() throws Exception {
    MethodParameter methodParameter = new MethodParameter(getClass().getMethod("testParameterList", List.class), 0);
    TypeDescriptor desc = new TypeDescriptor(methodParameter);
    assertThat(desc.getType()).isEqualTo(List.class);
    assertThat(desc.getObjectType()).isEqualTo(List.class);
    assertThat(desc.getName()).isEqualTo("java.util.List");
    assertThat(desc.toString()).isEqualTo("java.util.List<java.util.List<java.util.Map<java.lang.Integer, java.lang.Enum<?>>>>");
    assertThat(desc.isPrimitive()).isFalse();
    assertThat(desc.getAnnotations()).isEmpty();
    assertThat(desc.isCollection()).isTrue();
    assertThat(desc.isArray()).isFalse();
    assertThat(desc.getElementDescriptor().getType()).isEqualTo(List.class);
    assertThat(desc.getElementDescriptor()).isEqualTo(TypeDescriptor.nested(methodParameter, 1));
    assertThat(desc.getElementDescriptor().getElementDescriptor()).isEqualTo(TypeDescriptor.nested(methodParameter, 2));
    assertThat(desc.getElementDescriptor().getElementDescriptor().getMapValueDescriptor()).isEqualTo(TypeDescriptor.nested(methodParameter, 3));
    assertThat(desc.getElementDescriptor().getElementDescriptor().getMapKeyDescriptor().getType()).isEqualTo(Integer.class);
    assertThat(desc.getElementDescriptor().getElementDescriptor().getMapValueDescriptor().getType()).isEqualTo(Enum.class);
    assertThat(desc.isMap()).isFalse();
  }

  @Test
  void parameterListNoParamTypes() throws Exception {
    MethodParameter methodParameter = new MethodParameter(getClass().getMethod("testParameterListNoParamTypes", List.class), 0);
    TypeDescriptor desc = new TypeDescriptor(methodParameter);
    assertThat(desc.getType()).isEqualTo(List.class);
    assertThat(desc.getObjectType()).isEqualTo(List.class);
    assertThat(desc.getName()).isEqualTo("java.util.List");
    assertThat(desc.toString()).isEqualTo("java.util.List<?>");
    assertThat(desc.isPrimitive()).isFalse();
    assertThat(desc.getAnnotations()).isEmpty();
    assertThat(desc.isCollection()).isTrue();
    assertThat(desc.isArray()).isFalse();
    assertThat((Object) desc.getElementDescriptor()).isNull();
    assertThat(desc.isMap()).isFalse();
  }

  @Test
  void parameterArray() throws Exception {
    MethodParameter methodParameter = new MethodParameter(getClass().getMethod("testParameterArray", Integer[].class), 0);
    TypeDescriptor desc = new TypeDescriptor(methodParameter);
    assertThat(desc.getType()).isEqualTo(Integer[].class);
    assertThat(desc.getObjectType()).isEqualTo(Integer[].class);
    assertThat(desc.getName()).isEqualTo("java.lang.Integer[]");
    assertThat(desc.toString()).isEqualTo("java.lang.Integer[]");
    assertThat(desc.isPrimitive()).isFalse();
    assertThat(desc.getAnnotations()).isEmpty();
    assertThat(desc.isCollection()).isFalse();
    assertThat(desc.isArray()).isTrue();
    assertThat(desc.getElementDescriptor().getType()).isEqualTo(Integer.class);
    assertThat(desc.getElementDescriptor()).isEqualTo(TypeDescriptor.valueOf(Integer.class));
    assertThat(desc.isMap()).isFalse();
  }

  @Test
  void parameterMap() throws Exception {
    MethodParameter methodParameter = new MethodParameter(getClass().getMethod("testParameterMap", Map.class), 0);
    TypeDescriptor desc = new TypeDescriptor(methodParameter);
    assertThat(desc.getType()).isEqualTo(Map.class);
    assertThat(desc.getObjectType()).isEqualTo(Map.class);
    assertThat(desc.getName()).isEqualTo("java.util.Map");
    assertThat(desc.toString()).isEqualTo("java.util.Map<java.lang.Integer, java.util.List<java.lang.String>>");
    assertThat(desc.isPrimitive()).isFalse();
    assertThat(desc.getAnnotations()).isEmpty();
    assertThat(desc.isCollection()).isFalse();
    assertThat(desc.isArray()).isFalse();
    assertThat(desc.isMap()).isTrue();
    assertThat(desc.getMapValueDescriptor()).isEqualTo(TypeDescriptor.nested(methodParameter, 1));
    assertThat(desc.getMapValueDescriptor().getElementDescriptor()).isEqualTo(TypeDescriptor.nested(methodParameter, 2));
    assertThat(desc.getMapKeyDescriptor().getType()).isEqualTo(Integer.class);
    assertThat(desc.getMapValueDescriptor().getType()).isEqualTo(List.class);
    assertThat(desc.getMapValueDescriptor().getElementDescriptor().getType()).isEqualTo(String.class);
  }

  @Test
  void parameterAnnotated() throws Exception {
    TypeDescriptor t1 = new TypeDescriptor(new MethodParameter(getClass().getMethod("testAnnotatedMethod", String.class), 0));
    assertThat(t1.getType()).isEqualTo(String.class);
    assertThat(t1.getAnnotations()).hasSize(1);
    assertThat(t1.getAnnotation(ParameterAnnotation.class)).isNotNull();
    assertThat(t1.hasAnnotation(ParameterAnnotation.class)).isTrue();
    assertThat(t1.getAnnotation(ParameterAnnotation.class).value()).isEqualTo(123);
  }

  @Test
  void getAnnotationsReturnsClonedArray() throws Exception {
    TypeDescriptor t = new TypeDescriptor(new MethodParameter(getClass().getMethod("testAnnotatedMethod", String.class), 0));
    t.getAnnotations()[0] = null;
    assertThat(t.getAnnotations()[0]).isNotNull();
  }

  @Test
  void propertyComplex() throws Exception {
    Property property = new Property(getClass(), getClass().getMethod("getComplexProperty"),
            getClass().getMethod("setComplexProperty", Map.class));
    TypeDescriptor desc = new TypeDescriptor(property);
    assertThat(desc.getMapKeyDescriptor().getType()).isEqualTo(String.class);
    assertThat(desc.getMapValueDescriptor().getElementDescriptor().getElementDescriptor().getType()).isEqualTo(Integer.class);
  }

  @Test
  void propertyGenericType() throws Exception {
    GenericType<Integer> genericBean = new IntegerType();
    Property property = new Property(getClass(), genericBean.getClass().getMethod("getProperty"),
            genericBean.getClass().getMethod("setProperty", Integer.class));
    TypeDescriptor desc = new TypeDescriptor(property);
    assertThat(desc.getType()).isEqualTo(Integer.class);
  }

  @Test
  void propertyTypeCovariance() throws Exception {
    GenericType<Number> genericBean = new NumberType();
    Property property = new Property(getClass(), genericBean.getClass().getMethod("getProperty"),
            genericBean.getClass().getMethod("setProperty", Number.class));
    TypeDescriptor desc = new TypeDescriptor(property);
    assertThat(desc.getType()).isEqualTo(Integer.class);
  }

  @Test
  void propertyGenericTypeList() throws Exception {
    GenericType<Integer> genericBean = new IntegerType();
    Property property = new Property(getClass(), genericBean.getClass().getMethod("getListProperty"),
            genericBean.getClass().getMethod("setListProperty", List.class));
    TypeDescriptor desc = new TypeDescriptor(property);
    assertThat(desc.getType()).isEqualTo(List.class);
    assertThat(desc.getElementDescriptor().getType()).isEqualTo(Integer.class);
  }

  @Test
  void propertyGenericClassList() throws Exception {
    IntegerClass genericBean = new IntegerClass();
    Property property = new Property(genericBean.getClass(), genericBean.getClass().getMethod("getListProperty"),
            genericBean.getClass().getMethod("setListProperty", List.class));
    TypeDescriptor desc = new TypeDescriptor(property);
    assertThat(desc.getType()).isEqualTo(List.class);
    assertThat(desc.getElementDescriptor().getType()).isEqualTo(Integer.class);
    assertThat(desc.getAnnotation(MethodAnnotation1.class)).isNotNull();
    assertThat(desc.hasAnnotation(MethodAnnotation1.class)).isTrue();
    assertThat(property.isNullable()).isTrue();
  }

  @Test
  void property() throws Exception {
    Property property = new Property(
            getClass(), getClass().getMethod("getProperty"), getClass().getMethod("setProperty", Map.class));
    TypeDescriptor desc = new TypeDescriptor(property);
    assertThat(desc.getType()).isEqualTo(Map.class);
    assertThat(desc.getMapKeyDescriptor().getElementDescriptor().getType()).isEqualTo(Integer.class);
    assertThat(desc.getMapValueDescriptor().getElementDescriptor().getType()).isEqualTo(Long.class);
    assertThat(desc.getAnnotation(MethodAnnotation1.class)).isNotNull();
    assertThat(desc.getAnnotation(MethodAnnotation2.class)).isNotNull();
    assertThat(desc.getAnnotation(MethodAnnotation3.class)).isNotNull();
    assertThat(property.isNullable()).isFalse();
  }

  @Test
  void getAnnotationOnMethodThatIsLocallyAnnotated() throws Exception {
    assertAnnotationFoundOnMethod(MethodAnnotation1.class, "methodWithLocalAnnotation");
  }

  @Test
  void getAnnotationOnMethodThatIsMetaAnnotated() throws Exception {
    assertAnnotationFoundOnMethod(MethodAnnotation1.class, "methodWithComposedAnnotation");
  }

  @Test
  void getAnnotationOnMethodThatIsMetaMetaAnnotated() throws Exception {
    assertAnnotationFoundOnMethod(MethodAnnotation1.class, "methodWithComposedComposedAnnotation");
  }

  private void assertAnnotationFoundOnMethod(Class<? extends Annotation> annotationType, String methodName) throws Exception {
    TypeDescriptor typeDescriptor = new TypeDescriptor(new MethodParameter(getClass().getMethod(methodName), -1));
    assertThat(typeDescriptor.getAnnotation(annotationType)).as("Should have found @" + annotationType.getSimpleName() + " on " + methodName + ".").isNotNull();
  }

  @Test
  void fieldScalar() throws Exception {
    TypeDescriptor typeDescriptor = new TypeDescriptor(getClass().getField("fieldScalar"));
    assertThat(typeDescriptor.isPrimitive()).isFalse();
    assertThat(typeDescriptor.isArray()).isFalse();
    assertThat(typeDescriptor.isCollection()).isFalse();
    assertThat(typeDescriptor.isMap()).isFalse();
    assertThat(typeDescriptor.getType()).isEqualTo(Integer.class);
    assertThat(typeDescriptor.getObjectType()).isEqualTo(Integer.class);
  }

  @Test
  void fieldList() throws Exception {
    TypeDescriptor typeDescriptor = new TypeDescriptor(TypeDescriptorTests.class.getDeclaredField("listOfString"));
    assertThat(typeDescriptor.isArray()).isFalse();
    assertThat(typeDescriptor.getType()).isEqualTo(List.class);
    assertThat(typeDescriptor.getElementDescriptor().getType()).isEqualTo(String.class);
    assertThat(typeDescriptor.toString()).isEqualTo("java.util.List<java.lang.String>");
  }

  @Test
  void fieldListOfListOfString() throws Exception {
    TypeDescriptor typeDescriptor = new TypeDescriptor(TypeDescriptorTests.class.getDeclaredField("listOfListOfString"));
    assertThat(typeDescriptor.isArray()).isFalse();
    assertThat(typeDescriptor.getType()).isEqualTo(List.class);
    assertThat(typeDescriptor.getElementDescriptor().getType()).isEqualTo(List.class);
    assertThat(typeDescriptor.getElementDescriptor().getElementDescriptor().getType()).isEqualTo(String.class);
    assertThat(typeDescriptor.toString()).isEqualTo("java.util.List<java.util.List<java.lang.String>>");
  }

  @Test
  void fieldListOfListUnknown() throws Exception {
    TypeDescriptor typeDescriptor = new TypeDescriptor(TypeDescriptorTests.class.getDeclaredField("listOfListOfUnknown"));
    assertThat(typeDescriptor.isArray()).isFalse();
    assertThat(typeDescriptor.getType()).isEqualTo(List.class);
    assertThat(typeDescriptor.getElementDescriptor().getType()).isEqualTo(List.class);
    assertThat(typeDescriptor.getElementDescriptor().getElementDescriptor()).isNull();
    assertThat(typeDescriptor.toString()).isEqualTo("java.util.List<java.util.List<?>>");
  }

  @Test
  void fieldArray() throws Exception {
    TypeDescriptor typeDescriptor = new TypeDescriptor(TypeDescriptorTests.class.getDeclaredField("intArray"));
    assertThat(typeDescriptor.isArray()).isTrue();
    assertThat(typeDescriptor.getElementDescriptor().getType()).isEqualTo(int.class);
    assertThat(typeDescriptor.toString()).isEqualTo("int[]");
  }

  @Test
  void fieldComplexTypeDescriptor() throws Exception {
    TypeDescriptor typeDescriptor = new TypeDescriptor(TypeDescriptorTests.class.getDeclaredField("arrayOfListOfString"));
    assertThat(typeDescriptor.isArray()).isTrue();
    assertThat(typeDescriptor.getElementDescriptor().getType()).isEqualTo(List.class);
    assertThat(typeDescriptor.getElementDescriptor().getElementDescriptor().getType()).isEqualTo(String.class);
    assertThat(typeDescriptor.toString()).isEqualTo("java.util.List<java.lang.String>[]");
  }

  @Test
  void fieldComplexTypeDescriptor2() throws Exception {
    TypeDescriptor typeDescriptor = new TypeDescriptor(TypeDescriptorTests.class.getDeclaredField("nestedMapField"));
    assertThat(typeDescriptor.isMap()).isTrue();
    assertThat(typeDescriptor.getMapKeyDescriptor().getType()).isEqualTo(String.class);
    assertThat(typeDescriptor.getMapValueDescriptor().getType()).isEqualTo(List.class);
    assertThat(typeDescriptor.getMapValueDescriptor().getElementDescriptor().getType()).isEqualTo(Integer.class);
    assertThat(typeDescriptor.toString()).isEqualTo("java.util.Map<java.lang.String, java.util.List<java.lang.Integer>>");
  }

  @Test
  void fieldMap() throws Exception {
    TypeDescriptor desc = new TypeDescriptor(TypeDescriptorTests.class.getField("fieldMap"));
    assertThat(desc.isMap()).isTrue();
    assertThat(desc.getMapKeyDescriptor().getElementDescriptor().getType()).isEqualTo(Integer.class);
    assertThat(desc.getMapValueDescriptor().getElementDescriptor().getType()).isEqualTo(Long.class);
  }

  @Test
  void fieldAnnotated() throws Exception {
    TypeDescriptor typeDescriptor = new TypeDescriptor(getClass().getField("fieldAnnotated"));
    assertThat(typeDescriptor.getAnnotations()).hasSize(1);
    assertThat(typeDescriptor.getAnnotation(FieldAnnotation.class)).isNotNull();
  }

  @Test
  void valueOfScalar() {
    TypeDescriptor typeDescriptor = TypeDescriptor.valueOf(Integer.class);
    assertThat(typeDescriptor.isPrimitive()).isFalse();
    assertThat(typeDescriptor.isArray()).isFalse();
    assertThat(typeDescriptor.isCollection()).isFalse();
    assertThat(typeDescriptor.isMap()).isFalse();
    assertThat(typeDescriptor.getType()).isEqualTo(Integer.class);
    assertThat(typeDescriptor.getObjectType()).isEqualTo(Integer.class);
  }

  @Test
  void valueOfPrimitive() {
    TypeDescriptor typeDescriptor = TypeDescriptor.valueOf(int.class);
    assertThat(typeDescriptor.isPrimitive()).isTrue();
    assertThat(typeDescriptor.isArray()).isFalse();
    assertThat(typeDescriptor.isCollection()).isFalse();
    assertThat(typeDescriptor.isMap()).isFalse();
    assertThat(typeDescriptor.getType()).isEqualTo(int.class);
    assertThat(typeDescriptor.getObjectType()).isEqualTo(Integer.class);
  }

  @Test
  void valueOfArray() {
    TypeDescriptor typeDescriptor = TypeDescriptor.valueOf(int[].class);
    assertThat(typeDescriptor.isArray()).isTrue();
    assertThat(typeDescriptor.isCollection()).isFalse();
    assertThat(typeDescriptor.isMap()).isFalse();
    assertThat(typeDescriptor.getElementDescriptor().getType()).isEqualTo(int.class);
  }

  @Test
  void valueOfCollection() {
    TypeDescriptor typeDescriptor = TypeDescriptor.valueOf(Collection.class);
    assertThat(typeDescriptor.isCollection()).isTrue();
    assertThat(typeDescriptor.isArray()).isFalse();
    assertThat(typeDescriptor.isMap()).isFalse();
    assertThat((Object) typeDescriptor.getElementDescriptor()).isNull();
  }

  @Test
  void forObject() {
    TypeDescriptor desc = TypeDescriptor.forObject("3");
    assertThat(desc.getType()).isEqualTo(String.class);
  }

  @Test
  void forObjectNullTypeDescriptor() {
    TypeDescriptor desc = TypeDescriptor.forObject(null);
    assertThat((Object) desc).isNull();
  }

  @Test
  void nestedMethodParameterType2Levels() throws Exception {
    TypeDescriptor t1 = TypeDescriptor.nested(new MethodParameter(getClass().getMethod("test2", List.class), 0), 2);
    assertThat(t1.getType()).isEqualTo(String.class);
  }

  @Test
  void nestedMethodParameterTypeMap() throws Exception {
    TypeDescriptor t1 = TypeDescriptor.nested(new MethodParameter(getClass().getMethod("test3", Map.class), 0), 1);
    assertThat(t1.getType()).isEqualTo(String.class);
  }

  @Test
  void nestedMethodParameterTypeMapTwoLevels() throws Exception {
    TypeDescriptor t1 = TypeDescriptor.nested(new MethodParameter(getClass().getMethod("test4", List.class), 0), 2);
    assertThat(t1.getType()).isEqualTo(String.class);
  }

  @Test
  void nestedMethodParameterNot1NestedLevel() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            TypeDescriptor.nested(new MethodParameter(getClass().getMethod("test4", List.class), 0, 2), 2));
  }

  @Test
  void nestedTooManyLevels() throws Exception {
    TypeDescriptor t1 = TypeDescriptor.nested(new MethodParameter(getClass().getMethod("test4", List.class), 0), 3);
    assertThat((Object) t1).isNull();
  }

  @Test
  void nestedMethodParameterTypeNotNestable() throws Exception {
    TypeDescriptor t1 = TypeDescriptor.nested(new MethodParameter(getClass().getMethod("test5", String.class), 0), 2);
    assertThat((Object) t1).isNull();
  }

  @Test
  void nestedMethodParameterTypeInvalidNestingLevel() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            TypeDescriptor.nested(new MethodParameter(getClass().getMethod("test5", String.class), 0, 2), 2));
  }

  @Test
  void nestedNotParameterized() throws Exception {
    TypeDescriptor t1 = TypeDescriptor.nested(new MethodParameter(getClass().getMethod("test6", List.class), 0), 1);
    assertThat(t1.getType()).isEqualTo(List.class);
    assertThat(t1.toString()).isEqualTo("java.util.List<?>");
    TypeDescriptor t2 = TypeDescriptor.nested(new MethodParameter(getClass().getMethod("test6", List.class), 0), 2);
    assertThat((Object) t2).isNull();
  }

  @Test
  void nestedFieldTypeMapTwoLevels() throws Exception {
    TypeDescriptor t1 = TypeDescriptor.nested(getClass().getField("test4"), 2);
    assertThat(t1.getType()).isEqualTo(String.class);
  }

  @Test
  void nestedPropertyTypeMapTwoLevels() throws Exception {
    Property property = new Property(getClass(), getClass().getMethod("getTest4"), getClass().getMethod("setTest4", List.class));
    TypeDescriptor t1 = TypeDescriptor.nested(property, 2);
    assertThat(t1.getType()).isEqualTo(String.class);
  }

  @Test
  void collection() {
    TypeDescriptor desc = TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(Integer.class));
    assertThat(desc.getType()).isEqualTo(List.class);
    assertThat(desc.getObjectType()).isEqualTo(List.class);
    assertThat(desc.getName()).isEqualTo("java.util.List");
    assertThat(desc.toString()).isEqualTo("java.util.List<java.lang.Integer>");
    assertThat(desc.isPrimitive()).isFalse();
    assertThat(desc.getAnnotations()).isEmpty();
    assertThat(desc.isCollection()).isTrue();
    assertThat(desc.isArray()).isFalse();
    assertThat(desc.getElementDescriptor().getType()).isEqualTo(Integer.class);
    assertThat(desc.getElementDescriptor()).isEqualTo(TypeDescriptor.valueOf(Integer.class));
    assertThat(desc.isMap()).isFalse();
  }

  @Test
  void collectionNested() {
    TypeDescriptor desc = TypeDescriptor.collection(List.class, TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(Integer.class)));
    assertThat(desc.getType()).isEqualTo(List.class);
    assertThat(desc.getObjectType()).isEqualTo(List.class);
    assertThat(desc.getName()).isEqualTo("java.util.List");
    assertThat(desc.toString()).isEqualTo("java.util.List<java.util.List<java.lang.Integer>>");
    assertThat(desc.isPrimitive()).isFalse();
    assertThat(desc.getAnnotations()).isEmpty();
    assertThat(desc.isCollection()).isTrue();
    assertThat(desc.isArray()).isFalse();
    assertThat(desc.getElementDescriptor().getType()).isEqualTo(List.class);
    assertThat(desc.getElementDescriptor().getElementDescriptor()).isEqualTo(TypeDescriptor.valueOf(Integer.class));
    assertThat(desc.isMap()).isFalse();
  }

  @Test
  void map() {
    TypeDescriptor desc = TypeDescriptor.map(Map.class, TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Integer.class));
    assertThat(desc.getType()).isEqualTo(Map.class);
    assertThat(desc.getObjectType()).isEqualTo(Map.class);
    assertThat(desc.getName()).isEqualTo("java.util.Map");
    assertThat(desc.toString()).isEqualTo("java.util.Map<java.lang.String, java.lang.Integer>");
    assertThat(desc.isPrimitive()).isFalse();
    assertThat(desc.getAnnotations()).isEmpty();
    assertThat(desc.isCollection()).isFalse();
    assertThat(desc.isArray()).isFalse();
    assertThat(desc.isMap()).isTrue();
    assertThat(desc.getMapKeyDescriptor().getType()).isEqualTo(String.class);
    assertThat(desc.getMapValueDescriptor().getType()).isEqualTo(Integer.class);
  }

  @Test
  void mapNested() {
    TypeDescriptor desc = TypeDescriptor.map(Map.class, TypeDescriptor.valueOf(String.class),
            TypeDescriptor.map(Map.class, TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Integer.class)));
    assertThat(desc.getType()).isEqualTo(Map.class);
    assertThat(desc.getObjectType()).isEqualTo(Map.class);
    assertThat(desc.getName()).isEqualTo("java.util.Map");
    assertThat(desc.toString()).isEqualTo("java.util.Map<java.lang.String, java.util.Map<java.lang.String, java.lang.Integer>>");
    assertThat(desc.isPrimitive()).isFalse();
    assertThat(desc.getAnnotations()).isEmpty();
    assertThat(desc.isCollection()).isFalse();
    assertThat(desc.isArray()).isFalse();
    assertThat(desc.isMap()).isTrue();
    assertThat(desc.getMapKeyDescriptor().getType()).isEqualTo(String.class);
    assertThat(desc.getMapValueDescriptor().getMapKeyDescriptor().getType()).isEqualTo(String.class);
    assertThat(desc.getMapValueDescriptor().getMapValueDescriptor().getType()).isEqualTo(Integer.class);
  }

  @Test
  void narrow() {
    TypeDescriptor desc = TypeDescriptor.valueOf(Number.class);
    Integer value = 3;
    desc = desc.narrow(value);
    assertThat(desc.getType()).isEqualTo(Integer.class);
  }

  @Test
  void elementType() {
    TypeDescriptor desc = TypeDescriptor.valueOf(List.class);
    Integer value = 3;
    desc = desc.elementDescriptor(value);
    assertThat(desc.getType()).isEqualTo(Integer.class);
  }

  @Test
  void elementTypePreserveContext() throws Exception {
    TypeDescriptor desc = new TypeDescriptor(getClass().getField("listPreserveContext"));
    assertThat(desc.getElementDescriptor().getElementDescriptor().getType()).isEqualTo(Integer.class);
    List<Integer> value = new ArrayList<>(3);
    desc = desc.elementDescriptor(value);
    assertThat(desc.getElementDescriptor().getType()).isEqualTo(Integer.class);
    assertThat(desc.getAnnotation(FieldAnnotation.class)).isNotNull();
  }

  @Test
  void mapKeyType() {
    TypeDescriptor desc = TypeDescriptor.valueOf(Map.class);
    Integer value = 3;
    desc = desc.getMapKeyDescriptor(value);
    assertThat(desc.getType()).isEqualTo(Integer.class);
  }

  @Test
  void mapKeyTypePreserveContext() throws Exception {
    TypeDescriptor desc = new TypeDescriptor(getClass().getField("mapPreserveContext"));
    assertThat(desc.getMapKeyDescriptor().getElementDescriptor().getType()).isEqualTo(Integer.class);
    List<Integer> value = new ArrayList<>(3);
    desc = desc.getMapKeyDescriptor(value);
    assertThat(desc.getElementDescriptor().getType()).isEqualTo(Integer.class);
    assertThat(desc.getAnnotation(FieldAnnotation.class)).isNotNull();
  }

  @Test
  void mapValueType() {
    TypeDescriptor desc = TypeDescriptor.valueOf(Map.class);
    Integer value = 3;
    desc = desc.getMapValueDescriptor(value);
    assertThat(desc.getType()).isEqualTo(Integer.class);
  }

  @Test
  void mapValueTypePreserveContext() throws Exception {
    TypeDescriptor desc = new TypeDescriptor(getClass().getField("mapPreserveContext"));
    assertThat(desc.getMapValueDescriptor().getElementDescriptor().getType()).isEqualTo(Integer.class);
    List<Integer> value = new ArrayList<>(3);
    desc = desc.getMapValueDescriptor(value);
    assertThat(desc.getElementDescriptor().getType()).isEqualTo(Integer.class);
    assertThat(desc.getAnnotation(FieldAnnotation.class)).isNotNull();
  }

  @Test
  void equality() throws Exception {
    TypeDescriptor t1 = TypeDescriptor.valueOf(String.class);
    TypeDescriptor t2 = TypeDescriptor.valueOf(String.class);
    TypeDescriptor t3 = TypeDescriptor.valueOf(Date.class);
    TypeDescriptor t4 = TypeDescriptor.valueOf(Date.class);
    TypeDescriptor t5 = TypeDescriptor.valueOf(List.class);
    TypeDescriptor t6 = TypeDescriptor.valueOf(List.class);
    TypeDescriptor t7 = TypeDescriptor.valueOf(Map.class);
    TypeDescriptor t8 = TypeDescriptor.valueOf(Map.class);
    assertThat(t2).isEqualTo(t1);
    assertThat(t4).isEqualTo(t3);
    assertThat(t6).isEqualTo(t5);
    assertThat(t8).isEqualTo(t7);

    TypeDescriptor t9 = new TypeDescriptor(getClass().getField("listField"));
    TypeDescriptor t10 = new TypeDescriptor(getClass().getField("listField"));
    assertThat(t10).isEqualTo(t9);

    TypeDescriptor t11 = new TypeDescriptor(getClass().getField("mapField"));
    TypeDescriptor t12 = new TypeDescriptor(getClass().getField("mapField"));
    assertThat(t12).isEqualTo(t11);

    MethodParameter testAnnotatedMethod = new MethodParameter(getClass().getMethod("testAnnotatedMethod", String.class), 0);
    TypeDescriptor t13 = new TypeDescriptor(testAnnotatedMethod);
    TypeDescriptor t14 = new TypeDescriptor(testAnnotatedMethod);
    assertThat(t14).isEqualTo(t13);

    TypeDescriptor t15 = new TypeDescriptor(testAnnotatedMethod);
    TypeDescriptor t16 = new TypeDescriptor(new MethodParameter(getClass().getMethod("testAnnotatedMethodDifferentAnnotationValue", String.class), 0));
    assertThat(t16).isNotEqualTo(t15);

    TypeDescriptor t17 = new TypeDescriptor(testAnnotatedMethod);
    TypeDescriptor t18 = new TypeDescriptor(new MethodParameter(getClass().getMethod("test5", String.class), 0));
    assertThat(t18).isNotEqualTo(t17);
  }

  @Test
  void isAssignableTypes() {
    assertThat(TypeDescriptor.valueOf(Integer.class).isAssignableTo(TypeDescriptor.valueOf(Number.class))).isTrue();
    assertThat(TypeDescriptor.valueOf(Number.class).isAssignableTo(TypeDescriptor.valueOf(Integer.class))).isFalse();
    assertThat(TypeDescriptor.valueOf(String.class).isAssignableTo(TypeDescriptor.valueOf(String[].class))).isFalse();
  }

  @Test
  void isAssignableElementTypes() throws Exception {
    assertThat(new TypeDescriptor(getClass().getField("listField")).isAssignableTo(new TypeDescriptor(getClass().getField("listField")))).isTrue();
    assertThat(new TypeDescriptor(getClass().getField("notGenericList")).isAssignableTo(new TypeDescriptor(getClass().getField("listField")))).isTrue();
    assertThat(new TypeDescriptor(getClass().getField("listField")).isAssignableTo(new TypeDescriptor(getClass().getField("notGenericList")))).isTrue();
    assertThat(new TypeDescriptor(getClass().getField("isAssignableElementTypes")).isAssignableTo(new TypeDescriptor(getClass().getField("listField")))).isFalse();
    assertThat(TypeDescriptor.valueOf(List.class).isAssignableTo(new TypeDescriptor(getClass().getField("listField")))).isTrue();
  }

  @Test
  void isAssignableMapKeyValueTypes() throws Exception {
    assertThat(new TypeDescriptor(getClass().getField("mapField")).isAssignableTo(new TypeDescriptor(getClass().getField("mapField")))).isTrue();
    assertThat(new TypeDescriptor(getClass().getField("notGenericMap")).isAssignableTo(new TypeDescriptor(getClass().getField("mapField")))).isTrue();
    assertThat(new TypeDescriptor(getClass().getField("mapField")).isAssignableTo(new TypeDescriptor(getClass().getField("notGenericMap")))).isTrue();
    assertThat(new TypeDescriptor(getClass().getField("isAssignableMapKeyValueTypes")).isAssignableTo(new TypeDescriptor(getClass().getField("mapField")))).isFalse();
    assertThat(TypeDescriptor.valueOf(Map.class).isAssignableTo(new TypeDescriptor(getClass().getField("mapField")))).isTrue();
  }

  @Test
  void multiValueMap() throws Exception {
    TypeDescriptor td = new TypeDescriptor(getClass().getField("multiValueMap"));
    assertThat(td.isMap()).isTrue();
    assertThat(td.getMapKeyDescriptor().getType()).isEqualTo(String.class);
    assertThat(td.getMapValueDescriptor().getType()).isEqualTo(List.class);
    assertThat(td.getMapValueDescriptor().getElementDescriptor().getType()).isEqualTo(Integer.class);
  }

  @Test
  void passDownGeneric() throws Exception {
    TypeDescriptor td = new TypeDescriptor(getClass().getField("passDownGeneric"));
    assertThat(td.getElementDescriptor().getType()).isEqualTo(List.class);
    assertThat(td.getElementDescriptor().getElementDescriptor().getType()).isEqualTo(Set.class);
    assertThat(td.getElementDescriptor().getElementDescriptor().getElementDescriptor().getType()).isEqualTo(Integer.class);
  }

  @Test
  void upcast() throws Exception {
    Property property = new Property(getClass(), getClass().getMethod("getProperty"),
            getClass().getMethod("setProperty", Map.class));
    TypeDescriptor typeDescriptor = new TypeDescriptor(property);
    TypeDescriptor upcast = typeDescriptor.upcast(Object.class);
    assertThat(upcast.getAnnotation(MethodAnnotation1.class)).isNotNull();
  }

  @Test
  void upCastNotSuper() throws Exception {
    Property property = new Property(getClass(), getClass().getMethod("getProperty"),
            getClass().getMethod("setProperty", Map.class));
    TypeDescriptor typeDescriptor = new TypeDescriptor(property);
    assertThatIllegalArgumentException().isThrownBy(() ->
                    typeDescriptor.upcast(Collection.class))
            .withMessage("interface java.util.Map is not assignable to interface java.util.Collection");
  }

  @Test
  void elementTypeForCollectionSubclass() {
    @SuppressWarnings("serial")
    class CustomSet extends HashSet<String> {
    }

    assertThat(TypeDescriptor.valueOf(String.class)).isEqualTo(TypeDescriptor.valueOf(CustomSet.class).getElementDescriptor());
    assertThat(TypeDescriptor.valueOf(String.class)).isEqualTo(TypeDescriptor.forObject(new CustomSet()).getElementDescriptor());
  }

  @Test
  void elementTypeForMapSubclass() {
    @SuppressWarnings("serial")
    class CustomMap extends HashMap<String, Integer> {
    }

    assertThat(TypeDescriptor.valueOf(String.class)).isEqualTo(TypeDescriptor.valueOf(CustomMap.class).getMapKeyDescriptor());
    assertThat(TypeDescriptor.valueOf(Integer.class)).isEqualTo(TypeDescriptor.valueOf(CustomMap.class).getMapValueDescriptor());
    assertThat(TypeDescriptor.valueOf(String.class)).isEqualTo(TypeDescriptor.forObject(new CustomMap()).getMapKeyDescriptor());
    assertThat(TypeDescriptor.valueOf(Integer.class)).isEqualTo(TypeDescriptor.forObject(new CustomMap()).getMapValueDescriptor());
  }

  @Test
  void createMapArray() {
    TypeDescriptor mapType = TypeDescriptor.map(
            LinkedHashMap.class, TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Integer.class));
    TypeDescriptor arrayType = TypeDescriptor.array(mapType);
    assertThat(LinkedHashMap[].class).isEqualTo(arrayType.getType());
    assertThat(mapType).isEqualTo(arrayType.getElementDescriptor());
  }

  @Test
  void createStringArray() {
    TypeDescriptor arrayType = TypeDescriptor.array(TypeDescriptor.valueOf(String.class));
    assertThat(TypeDescriptor.valueOf(String[].class)).isEqualTo(arrayType);
  }

  @Test
  void createNullArray() {
    assertThat((Object) TypeDescriptor.array(null)).isNull();
  }

  @Test
  void serializable() throws Exception {
    TypeDescriptor typeDescriptor = TypeDescriptor.forObject("");
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ObjectOutputStream outputStream = new ObjectOutputStream(out);
    outputStream.writeObject(typeDescriptor);
    ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(
            out.toByteArray()));
    TypeDescriptor readObject = (TypeDescriptor) inputStream.readObject();
    assertThat(readObject).isEqualTo(typeDescriptor);
  }

  @Test
  void createCollectionWithNullElement() {
    TypeDescriptor typeDescriptor = TypeDescriptor.collection(List.class, (TypeDescriptor) null);
    assertThat(typeDescriptor.getElementDescriptor()).isNull();
  }

  @Test
  void createMapWithNullElements() {
    TypeDescriptor typeDescriptor = TypeDescriptor.map(LinkedHashMap.class, (TypeDescriptor) null, null);
    assertThat(typeDescriptor.getMapKeyDescriptor()).isNull();
    assertThat(typeDescriptor.getMapValueDescriptor()).isNull();
  }

  @Test
  void getSource() throws Exception {
    Field field = getClass().getField("fieldScalar");
    MethodParameter methodParameter = new MethodParameter(getClass().getMethod("testParameterPrimitive", int.class), 0);
    assertThat(new TypeDescriptor(field).getSource()).isEqualTo(field);
    assertThat(new TypeDescriptor(methodParameter).getSource()).isEqualTo(methodParameter);
    assertThat(TypeDescriptor.valueOf(Integer.class).getSource()).isEqualTo(Integer.class);
  }

  @Test
  void equalityWithGenerics() {
    ResolvableType rt1 = ResolvableType.forClassWithGenerics(Optional.class, Integer.class);
    ResolvableType rt2 = ResolvableType.forClassWithGenerics(Optional.class, String.class);

    TypeDescriptor td1 = new TypeDescriptor(rt1, null, (Annotation[]) null);
    TypeDescriptor td2 = new TypeDescriptor(rt2, null, (Annotation[]) null);

    assertThat(td1).isNotEqualTo(td2);
  }

  @Test
  void recursiveType() {
    assertThat(TypeDescriptor.valueOf(RecursiveMap.class)).isEqualTo(
            TypeDescriptor.valueOf(RecursiveMap.class));

    TypeDescriptor typeDescriptor1 = TypeDescriptor.map(Map.class,
            TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(RecursiveMap.class));
    TypeDescriptor typeDescriptor2 = TypeDescriptor.map(Map.class,
            TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(RecursiveMap.class));
    assertThat(typeDescriptor1).isEqualTo(typeDescriptor2);
  }

  @Test
  void recursiveTypeWithInterface() {
    assertThat(TypeDescriptor.valueOf(RecursiveMapWithInterface.class)).isEqualTo(
            TypeDescriptor.valueOf(RecursiveMapWithInterface.class));

    TypeDescriptor typeDescriptor1 = TypeDescriptor.map(Map.class,
            TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(RecursiveMapWithInterface.class));
    TypeDescriptor typeDescriptor2 = TypeDescriptor.map(Map.class,
            TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(RecursiveMapWithInterface.class));
    assertThat(typeDescriptor1).isEqualTo(typeDescriptor2);
  }

  @Test
  void arrayTypeDescriptor() {
    TypeDescriptor arrayDesc = TypeDescriptor.array(TypeDescriptor.valueOf(String.class));
    assertThat(arrayDesc.getType()).isEqualTo(String[].class);
    assertThat(arrayDesc.isArray()).isTrue();
    assertThat(arrayDesc.getElementDescriptor().getType()).isEqualTo(String.class);
  }

  @Test
  void arrayTypeDescriptorWithNull() {
    assertThat(TypeDescriptor.array(null)).isNull();
  }

  @Test
  void nestedMapValueType() {
    TypeDescriptor mapDesc = TypeDescriptor.map(Map.class,
            TypeDescriptor.valueOf(String.class),
            TypeDescriptor.map(Map.class,
                    TypeDescriptor.valueOf(Integer.class),
                    TypeDescriptor.valueOf(Boolean.class)));

    assertThat(mapDesc.getMapValueDescriptor().getMapKeyDescriptor().getType()).isEqualTo(Integer.class);
    assertThat(mapDesc.getMapValueDescriptor().getMapValueDescriptor().getType()).isEqualTo(Boolean.class);
  }

  @Test
  void fromParameterRetainsAnnotations() throws Exception {
    Method method = getClass().getMethod("methodWithAnnotatedParameter", String.class);
    Parameter param = method.getParameters()[0];
    TypeDescriptor desc = TypeDescriptor.fromParameter(param);

    assertThat(desc.hasAnnotation(ParameterAnnotation.class)).isTrue();
    assertThat(desc.getAnnotation(ParameterAnnotation.class).value()).isEqualTo(123);
  }

  @Test
  void getObjectTypeForPrimitive() {
    TypeDescriptor desc = TypeDescriptor.valueOf(int.class);
    assertThat(desc.getObjectType()).isEqualTo(Integer.class);
    assertThat(desc.isPrimitive()).isTrue();
  }

  @Test
  void isInstanceChecksDerivedTypes() {
    TypeDescriptor desc = TypeDescriptor.valueOf(Number.class);
    assertThat(desc.isInstance(42)).isTrue();
    assertThat(desc.isInstance(42.0)).isTrue();
    assertThat(desc.isInstance("string")).isFalse();
  }

  @Test
  void getNameReturnsCanonicalName() {
    TypeDescriptor desc = new TypeDescriptor(ResolvableType.forClass(Integer.class), Integer.class, (Annotation[]) null);
    assertThat(desc.getName()).isEqualTo("java.lang.Integer");
  }

  @Test
  void getSimpleNameReturnsClassName() {
    TypeDescriptor desc = TypeDescriptor.valueOf(String.class);
    assertThat(desc.getSimpleName()).isEqualTo("String");
  }

  @Test
  void equalsWithDifferentAnnotationValues() throws Exception {
    TypeDescriptor desc1 = new TypeDescriptor(MethodParameter.forExecutable(
            getClass().getMethod("methodWithAnnotatedParameter", String.class), 0));
    TypeDescriptor desc2 = new TypeDescriptor(MethodParameter.forExecutable(
            getClass().getMethod("methodWithDifferentAnnotationValue", String.class), 0));
    assertThat(desc1).isNotEqualTo(desc2);
  }

  @Test
  void nestedMapWithMultipleLevelsValueType() {
    TypeDescriptor mapDesc = TypeDescriptor.map(Map.class,
            TypeDescriptor.valueOf(String.class),
            TypeDescriptor.map(Map.class,
                    TypeDescriptor.valueOf(Integer.class),
                    TypeDescriptor.map(Map.class,
                            TypeDescriptor.valueOf(Long.class),
                            TypeDescriptor.valueOf(Boolean.class))));

    assertThat(mapDesc.getMapKeyDescriptor().getType()).isEqualTo(String.class);
    assertThat(mapDesc.getMapValueDescriptor().getMapKeyDescriptor().getType()).isEqualTo(Integer.class);
    assertThat(mapDesc.getMapValueDescriptor().getMapValueDescriptor().getMapKeyDescriptor().getType()).isEqualTo(Long.class);
    assertThat(mapDesc.getMapValueDescriptor().getMapValueDescriptor().getMapValueDescriptor().getType()).isEqualTo(Boolean.class);
    assertThat(mapDesc.toString()).isEqualTo("java.util.Map<java.lang.String, java.util.Map<java.lang.Integer, java.util.Map<java.lang.Long, java.lang.Boolean>>>");
  }

  @Test
  void arrayNestedCollectionType() {
    TypeDescriptor arrayDesc = TypeDescriptor.array(
            TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(String.class)));

    assertThat(arrayDesc.getType()).isEqualTo(List[].class);
    assertThat(arrayDesc.isArray()).isTrue();
    assertThat(arrayDesc.getElementDescriptor().getType()).isEqualTo(List.class);
    assertThat(arrayDesc.getElementDescriptor().getElementDescriptor().getType()).isEqualTo(String.class);
  }

  @Test
  void nestedCollectionWithArrayType() {
    TypeDescriptor desc = TypeDescriptor.collection(List.class,
            TypeDescriptor.array(TypeDescriptor.valueOf(String.class)));

    assertThat(desc.getType()).isEqualTo(List.class);
    assertThat(desc.getElementDescriptor().getType()).isEqualTo(String[].class);
    assertThat(desc.getElementDescriptor().getElementDescriptor().getType()).isEqualTo(String.class);
  }

  @Test
  void mapWithArrayKeyAndCollectionValue() {
    TypeDescriptor mapDesc = TypeDescriptor.map(Map.class,
            TypeDescriptor.array(TypeDescriptor.valueOf(Integer.class)),
            TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(String.class)));

    assertThat(mapDesc.getMapKeyDescriptor().getType()).isEqualTo(Integer[].class);
    assertThat(mapDesc.getMapKeyDescriptor().getElementDescriptor().getType()).isEqualTo(Integer.class);
    assertThat(mapDesc.getMapValueDescriptor().getType()).isEqualTo(List.class);
    assertThat(mapDesc.getMapValueDescriptor().getElementDescriptor().getType()).isEqualTo(String.class);
  }

  @Test
  void collectionOfOptionals() {
    TypeDescriptor desc = TypeDescriptor.collection(List.class,
            TypeDescriptor.valueOf(Optional.class));

    assertThat(desc.getType()).isEqualTo(List.class);
    assertThat(desc.getElementDescriptor().getType()).isEqualTo(Optional.class);
    assertThat(desc.isCollection()).isTrue();
    assertThat(desc.toString()).isEqualTo("java.util.List<java.util.Optional<?>>");
  }

  @Test
  void primitiveArrayDescriptor() {
    TypeDescriptor arrayDesc = TypeDescriptor.array(TypeDescriptor.valueOf(int.class));

    assertThat(arrayDesc.getType()).isEqualTo(int[].class);
    assertThat(arrayDesc.isArray()).isTrue();
    assertThat(arrayDesc.isPrimitive()).isFalse();
    assertThat(arrayDesc.getElementDescriptor().getType()).isEqualTo(int.class);
    assertThat(arrayDesc.getElementDescriptor().isPrimitive()).isTrue();
  }

  @Test
  void arrayTypeDescriptorDetectsArray() {
    TypeDescriptor desc = TypeDescriptor.valueOf(String[].class);
    assertThat(desc.isArray()).isTrue();
    assertThat(desc.getComponentType()).isEqualTo(String.class);
  }

  @Test
  void nonArrayTypeDescriptorHandling() {
    TypeDescriptor desc = TypeDescriptor.valueOf(String.class);
    assertThat(desc.isArray()).isFalse();
    assertThat(desc.getComponentType()).isNull();
  }

  @Test
  void primitiveArrayTypeDescriptorDetection() {
    TypeDescriptor desc = TypeDescriptor.valueOf(int[].class);
    assertThat(desc.isArray()).isTrue();
    assertThat(desc.getComponentType()).isEqualTo(int.class);
  }

  @Test
  void multiDimensionalArrayDetection() {
    TypeDescriptor desc = TypeDescriptor.valueOf(String[][].class);
    assertThat(desc.isArray()).isTrue();
    assertThat(desc.getComponentType()).isEqualTo(String[].class);
  }

  @Test
  void typeIsExactMatch() {
    TypeDescriptor desc = TypeDescriptor.valueOf(String.class);
    assertThat(desc.is(String.class)).isTrue();
    assertThat(desc.is(Object.class)).isFalse();
  }

  @Test
  void typeAssignability() {
    TypeDescriptor numberDesc = TypeDescriptor.valueOf(Number.class);
    assertThat(numberDesc.isAssignableFrom(Integer.class)).isTrue();
    assertThat(numberDesc.isAssignableFrom(String.class)).isFalse();
    assertThat(numberDesc.isAssignableTo(Object.class)).isTrue();
    assertThat(numberDesc.isAssignableTo(Integer.class)).isFalse();
  }

  @Test
  void interfaceAssignability() {
    TypeDescriptor comparableDesc = TypeDescriptor.valueOf(Comparable.class);
    assertThat(comparableDesc.isAssignableFrom(String.class)).isTrue();
    assertThat(comparableDesc.isAssignableTo(Object.class)).isTrue();
  }

  @Test
  void enumTypeDetection() {
    enum TestEnum {A, B}
    TypeDescriptor desc = TypeDescriptor.valueOf(TestEnum.class);
    assertThat(desc.isEnum()).isTrue();
  }

  @Test
  void nonEnumTypeHandling() {
    TypeDescriptor desc = TypeDescriptor.valueOf(String.class);
    assertThat(desc.isEnum()).isFalse();
  }

  public void methodWithAnnotatedParameter(@ParameterAnnotation(123) String param) {
  }

  public void methodWithDifferentAnnotationValue(@ParameterAnnotation(456) String param) {
  }

  // Methods designed for test introspection

  public void testParameterPrimitive(int primitive) {
  }

  public void testParameterScalar(String value) {
  }

  public void testParameterList(List<List<Map<Integer, Enum<?>>>> list) {
  }

  public void testParameterListNoParamTypes(List list) {
  }

  public void testParameterArray(Integer[] array) {
  }

  public void testParameterMap(Map<Integer, List<String>> map) {
  }

  public void test1(List<String> param1) {
  }

  public void test2(List<List<String>> param1) {
  }

  public void test3(Map<Integer, String> param1) {
  }

  public void test4(List<Map<Integer, String>> param1) {
  }

  public void test5(String param1) {
  }

  public void test6(List<List> param1) {
  }

  public List<Map<Integer, String>> getTest4() {
    return null;
  }

  public void setTest4(List<Map<Integer, String>> test4) {
  }

  public Map<String, List<List<Integer>>> getComplexProperty() {
    return null;
  }

  @MethodAnnotation1
  public Map<List<Integer>, List<Long>> getProperty() {
    return property;
  }

  @MethodAnnotation2
  public void setProperty(Map<List<Integer>, List<Long>> property) {
    this.property = property;
  }

  @MethodAnnotation1
  public void methodWithLocalAnnotation() {
  }

  @ComposedMethodAnnotation1
  public void methodWithComposedAnnotation() {
  }

  @ComposedComposedMethodAnnotation1
  public void methodWithComposedComposedAnnotation() {
  }

  public void setComplexProperty(Map<String, List<List<Integer>>> complexProperty) {
  }

  public void testAnnotatedMethod(@ParameterAnnotation(123) String parameter) {
  }

  public void testAnnotatedMethodDifferentAnnotationValue(@ParameterAnnotation(567) String parameter) {
  }

  // Fields designed for test introspection

  public Integer fieldScalar;

  public List<String> listOfString;

  public List<List<String>> listOfListOfString = new ArrayList<>();

  public List<List> listOfListOfUnknown = new ArrayList<>();

  public int[] intArray;

  public List<String>[] arrayOfListOfString;

  public List<Integer> listField = new ArrayList<>();

  public Map<String, Integer> mapField = new HashMap<>();

  public Map<String, List<Integer>> nestedMapField = new HashMap<>();

  public Map<List<Integer>, List<Long>> fieldMap;

  public List<Map<Integer, String>> test4;

  @FieldAnnotation
  public List<String> fieldAnnotated;

  @FieldAnnotation
  public List<List<Integer>> listPreserveContext;

  @FieldAnnotation
  public Map<List<Integer>, List<Integer>> mapPreserveContext;

  @MethodAnnotation3
  private Map<List<Integer>, List<Long>> property;

  public List notGenericList;

  public List<Number> isAssignableElementTypes;

  public Map notGenericMap;

  public Map<CharSequence, Number> isAssignableMapKeyValueTypes;

  public MultiValueMap<String, Integer> multiValueMap = new LinkedMultiValueMap<>();

  public PassDownGeneric<Integer> passDownGeneric = new PassDownGeneric<>();

  // Classes designed for test introspection

  @SuppressWarnings("serial")
  public static class PassDownGeneric<T> extends ArrayList<List<Set<T>>> {
  }

  public static class GenericClass<T> {

    public T getProperty() {
      return null;
    }

    public void setProperty(T t) {
    }

    @Nullable
    @MethodAnnotation1
    public List<T> getListProperty() {
      return null;
    }

    public void setListProperty(List<T> t) {
    }
  }

  public static class IntegerClass extends GenericClass<Integer> {
  }

  public interface GenericType<T> {

    T getProperty();

    void setProperty(T t);

    List<T> getListProperty();

    void setListProperty(List<T> t);
  }

  public class IntegerType implements GenericType<Integer> {

    @Override
    public Integer getProperty() {
      return null;
    }

    @Override
    public void setProperty(Integer t) {
    }

    @Override
    public List<Integer> getListProperty() {
      return null;
    }

    @Override
    public void setListProperty(List<Integer> t) {
    }
  }

  public class NumberType implements GenericType<Number> {

    @Override
    public Integer getProperty() {
      return null;
    }

    @Override
    public void setProperty(Number t) {
    }

    @Override
    public List<Number> getListProperty() {
      return null;
    }

    @Override
    public void setListProperty(List<Number> t) {
    }
  }

  @SuppressWarnings("serial")
  static class RecursiveMap extends HashMap<String, RecursiveMap> {
  }

  @SuppressWarnings("serial")
  static class RecursiveMapWithInterface extends HashMap<String, RecursiveMapWithInterface>
          implements Map<String, RecursiveMapWithInterface> {
  }

  // Annotations used on tested elements

  @Target({ ElementType.PARAMETER })
  @Retention(RetentionPolicy.RUNTIME)
  public @interface ParameterAnnotation {

    int value();
  }

  @Target({ ElementType.FIELD })
  @Retention(RetentionPolicy.RUNTIME)
  public @interface FieldAnnotation {
  }

  @Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
  @Retention(RetentionPolicy.RUNTIME)
  public @interface MethodAnnotation1 {
  }

  @Target({ ElementType.METHOD })
  @Retention(RetentionPolicy.RUNTIME)
  public @interface MethodAnnotation2 {
  }

  @Target({ ElementType.FIELD })
  @Retention(RetentionPolicy.RUNTIME)
  public @interface MethodAnnotation3 {
  }

  @MethodAnnotation1
  @Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
  @Retention(RetentionPolicy.RUNTIME)
  public @interface ComposedMethodAnnotation1 {
  }

  @ComposedMethodAnnotation1
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  public @interface ComposedComposedMethodAnnotation1 {
  }

}
