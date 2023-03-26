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
import java.util.Set;

import cn.taketoday.util.DefaultMultiValueMap;
import cn.taketoday.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY 2021/3/23 11:50
 * @since 3.0
 */
public class TypeDescriptorTests {

  @Test
  public void parameterPrimitive() throws Exception {
    final Method testParameterPrimitive = getClass().getMethod("testParameterPrimitive", int.class);
    final TypeDescriptor desc = TypeDescriptor.forParameter(testParameterPrimitive, 0);

    assertThat(desc.getType()).isEqualTo(int.class);
    assertThat(desc.getObjectType()).isEqualTo(Integer.class);
    assertThat(desc.getName()).isEqualTo("int");
    assertThat(desc.toString()).isEqualTo("int");
    assertThat(desc.isPrimitive()).isTrue();
    assertThat(desc.getAnnotations().length).isEqualTo(0);
    assertThat(desc.isCollection()).isFalse();
    assertThat(desc.isMap()).isFalse();
  }

  @Test
  public void parameterScalar() throws Exception {
    final Method testParameterScalar = getClass().getMethod("testParameterScalar", String.class);
    final TypeDescriptor desc = TypeDescriptor.forParameter(testParameterScalar, 0);

    assertThat(desc.getType()).isEqualTo(String.class);
    assertThat(desc.getObjectType()).isEqualTo(String.class);
    assertThat(desc.getName()).isEqualTo("java.lang.String");
    assertThat(desc.toString()).isEqualTo("java.lang.String");
    assertThat(!desc.isPrimitive()).isTrue();
    assertThat(desc.getAnnotations().length).isEqualTo(0);
    assertThat(desc.isCollection()).isFalse();
    assertThat(desc.isArray()).isFalse();
    assertThat(desc.isMap()).isFalse();
  }

  @Test
  public void parameterList() throws Exception {
    final Method testParameterList = getClass().getMethod("testParameterList", List.class);
    final TypeDescriptor desc = TypeDescriptor.forParameter(testParameterList, 0);

    assertThat(desc.getType()).isEqualTo(List.class);
    assertThat(desc.getObjectType()).isEqualTo(List.class);
    assertThat(desc.getName()).isEqualTo("java.util.List");
    assertThat(desc.toString()).isEqualTo("java.util.List<java.util.List<java.util.Map<java.lang.Integer, java.lang.Enum<?>>>>");
    assertThat(!desc.isPrimitive()).isTrue();
    assertThat(desc.getAnnotations().length).isEqualTo(0);
    assertThat(desc.isCollection()).isTrue();
    assertThat(desc.isArray()).isFalse();
    assertThat(desc.getElementDescriptor().getType()).isEqualTo(List.class);
    assertThat(desc.getElementDescriptor()).isEqualTo(TypeDescriptor.nested(desc, 1));
    assertThat(desc.getElementDescriptor().getElementDescriptor()).isEqualTo(TypeDescriptor.nested(desc, 2));
    assertThat(desc.getElementDescriptor().getElementDescriptor().getMapValueDescriptor()).isEqualTo(TypeDescriptor.nested(desc, 3));
    assertThat(desc.getElementDescriptor().getElementDescriptor().getMapKeyDescriptor().getType()).isEqualTo(Integer.class);
    assertThat(desc.getElementDescriptor().getElementDescriptor().getMapValueDescriptor().getType()).isEqualTo(Enum.class);
    assertThat(desc.isMap()).isFalse();
  }

  @Test
  public void parameterListNoParamTypes() throws Exception {
    final Method testParameterListNoParamTypes = getClass().getMethod("testParameterListNoParamTypes", List.class);
    TypeDescriptor desc = TypeDescriptor.forParameter(testParameterListNoParamTypes, 0);

    assertThat(desc.getType()).isEqualTo(List.class);
    assertThat(desc.getObjectType()).isEqualTo(List.class);
    assertThat(desc.getName()).isEqualTo("java.util.List");
    assertThat(desc.toString()).isEqualTo("java.util.List<?>");
    assertThat(!desc.isPrimitive()).isTrue();
    assertThat(desc.getAnnotations().length).isEqualTo(0);
    assertThat(desc.isCollection()).isTrue();
    assertThat(desc.isArray()).isFalse();
    assertThat((Object) desc.getElementDescriptor()).isNull();
    assertThat(desc.isMap()).isFalse();
  }

  @Test
  public void parameterArray() throws Exception {
    final Method testParameterArray = getClass().getMethod("testParameterArray", Integer[].class);
    TypeDescriptor desc = TypeDescriptor.forParameter(testParameterArray, 0);

    assertThat(desc.getType()).isEqualTo(Integer[].class);
    assertThat(desc.getObjectType()).isEqualTo(Integer[].class);
    assertThat(desc.getName()).isEqualTo("java.lang.Integer[]");
    assertThat(desc.toString()).isEqualTo("java.lang.Integer[]");
    assertThat(!desc.isPrimitive()).isTrue();
    assertThat(desc.getAnnotations().length).isEqualTo(0);
    assertThat(desc.isCollection()).isFalse();
    assertThat(desc.isArray()).isTrue();
    assertThat(desc.getElementDescriptor().getType()).isEqualTo(Integer.class);
    assertThat(desc.getElementDescriptor()).isEqualTo(TypeDescriptor.valueOf(Integer.class));
    assertThat(desc.isMap()).isFalse();
  }

  @Test
  public void parameterMap() throws Exception {
    final Method testParameterMap = getClass().getMethod("testParameterMap", Map.class);

    TypeDescriptor desc = TypeDescriptor.forParameter(testParameterMap, 0);

    assertThat(desc.getType()).isEqualTo(Map.class);
    assertThat(desc.getObjectType()).isEqualTo(Map.class);
    assertThat(desc.getName()).isEqualTo("java.util.Map");
    assertThat(desc.toString()).isEqualTo("java.util.Map<java.lang.Integer, java.util.List<java.lang.String>>");
    assertThat(!desc.isPrimitive()).isTrue();
    assertThat(desc.getAnnotations().length).isEqualTo(0);
    assertThat(desc.isCollection()).isFalse();
    assertThat(desc.isArray()).isFalse();
    assertThat(desc.isMap()).isTrue();
    assertThat(desc.getMapValueDescriptor()).isEqualTo(TypeDescriptor.nested(desc, 1));
    assertThat(desc.getMapValueDescriptor().getElementDescriptor()).isEqualTo(TypeDescriptor.nested(desc, 2));
    assertThat(desc.getMapKeyDescriptor().getType()).isEqualTo(Integer.class);
    assertThat(desc.getMapValueDescriptor().getType()).isEqualTo(List.class);
    assertThat(desc.getMapValueDescriptor().getElementDescriptor().getType()).isEqualTo(String.class);
  }

  @Test
  public void parameterAnnotated() throws Exception {
    final Method testAnnotatedMethod = getClass().getMethod("testAnnotatedMethod", String.class);
    final TypeDescriptor t1 = TypeDescriptor.forParameter(testAnnotatedMethod, 0);

    assertThat(t1.getType()).isEqualTo(String.class);
    assertThat(t1.getAnnotations().length).isEqualTo(1);
    assertThat(t1.getAnnotation(ParameterAnnotation.class)).isNotNull();
    assertThat(t1.hasAnnotation(ParameterAnnotation.class)).isTrue();
    assertThat(t1.getAnnotation(ParameterAnnotation.class).value()).isEqualTo(123);
  }

  @Test
  public void getAnnotationsReturnsClonedArray() throws Exception {
    final Method testAnnotatedMethod = getClass().getMethod("testAnnotatedMethod", String.class);
    final TypeDescriptor t = TypeDescriptor.forParameter(testAnnotatedMethod, 0);
    t.getAnnotations()[0] = null;
    assertThat(t.getAnnotations()[0]).isNotNull();
  }

  @Test
  public void getAnnotationOnMethodThatIsLocallyAnnotated() throws Exception {
    assertAnnotationFoundOnMethod(MethodAnnotation1.class, "methodWithLocalAnnotation");
  }

  @Test
  public void getAnnotationOnMethodThatIsMetaAnnotated() throws Exception {
    assertAnnotationFoundOnMethod(MethodAnnotation1.class, "methodWithComposedAnnotation");
  }

  @Test
  public void getAnnotationOnMethodThatIsMetaMetaAnnotated() throws Exception {
    assertAnnotationFoundOnMethod(MethodAnnotation1.class, "methodWithComposedComposedAnnotation");
  }

  private void assertAnnotationFoundOnMethod(Class<? extends Annotation> annotationType, String methodName) throws Exception {
    final Method method = getClass().getMethod(methodName);
    final ResolvableType resolvableType = ResolvableType.forReturnType(method);
    final TypeDescriptor descriptor = new TypeDescriptor(resolvableType, null, method.getAnnotations());
    assertThat(descriptor.getAnnotation(annotationType))
            .as("Should have found @" + annotationType.getSimpleName() + " on " + methodName + ".")
            .isNotNull();
  }

  @Test
  public void fieldScalar() throws Exception {
    TypeDescriptor descriptor = new TypeDescriptor(getClass().getField("fieldScalar"));
    assertThat(descriptor.isPrimitive()).isFalse();
    assertThat(descriptor.isArray()).isFalse();
    assertThat(descriptor.isCollection()).isFalse();
    assertThat(descriptor.isMap()).isFalse();
    assertThat(descriptor.getType()).isEqualTo(Integer.class);
    assertThat(descriptor.getObjectType()).isEqualTo(Integer.class);
  }

  @Test
  public void fieldList() throws Exception {
    TypeDescriptor typeDescriptor = new TypeDescriptor(TypeDescriptorTests.class.getDeclaredField("listOfString"));
    assertThat(typeDescriptor.isArray()).isFalse();
    assertThat(typeDescriptor.getType()).isEqualTo(List.class);
    assertThat(typeDescriptor.getElementDescriptor().getType()).isEqualTo(String.class);
    assertThat(typeDescriptor.toString()).isEqualTo("java.util.List<java.lang.String>");
  }

  @Test
  public void fieldListOfListOfString() throws Exception {
    TypeDescriptor typeDescriptor = new TypeDescriptor(TypeDescriptorTests.class.getDeclaredField("listOfListOfString"));
    assertThat(typeDescriptor.isArray()).isFalse();
    assertThat(typeDescriptor.getType()).isEqualTo(List.class);
    assertThat(typeDescriptor.getElementDescriptor().getType()).isEqualTo(List.class);
    assertThat(typeDescriptor.getElementDescriptor().getElementDescriptor().getType()).isEqualTo(String.class);
    assertThat(typeDescriptor.toString()).isEqualTo("java.util.List<java.util.List<java.lang.String>>");
  }

  @Test
  public void fieldListOfListUnknown() throws Exception {
    TypeDescriptor typeDescriptor = new TypeDescriptor(TypeDescriptorTests.class.getDeclaredField("listOfListOfUnknown"));
    assertThat(typeDescriptor.isArray()).isFalse();
    assertThat(typeDescriptor.getType()).isEqualTo(List.class);
    assertThat(typeDescriptor.getElementDescriptor().getType()).isEqualTo(List.class);
    assertThat(typeDescriptor.getElementDescriptor().getElementDescriptor()).isNull();
    assertThat(typeDescriptor.toString()).isEqualTo("java.util.List<java.util.List<?>>");
  }

  @Test
  public void fieldArray() throws Exception {
    TypeDescriptor typeDescriptor = new TypeDescriptor(TypeDescriptorTests.class.getDeclaredField("intArray"));
    assertThat(typeDescriptor.isArray()).isTrue();
    assertThat(typeDescriptor.getElementDescriptor().getType()).isEqualTo(Integer.TYPE);
    assertThat(typeDescriptor.toString()).isEqualTo("int[]");
  }

  @Test
  public void fieldComplexTypeDescriptor() throws Exception {
    TypeDescriptor typeDescriptor = new TypeDescriptor(TypeDescriptorTests.class.getDeclaredField("arrayOfListOfString"));
    assertThat(typeDescriptor.isArray()).isTrue();
    assertThat(typeDescriptor.getElementDescriptor().getType()).isEqualTo(List.class);
    assertThat(typeDescriptor.getElementDescriptor().getElementDescriptor().getType()).isEqualTo(String.class);
    assertThat(typeDescriptor.toString()).isEqualTo("java.util.List<java.lang.String>[]");
  }

  @Test
  public void fieldComplexTypeDescriptor2() throws Exception {
    TypeDescriptor typeDescriptor = new TypeDescriptor(TypeDescriptorTests.class.getDeclaredField("nestedMapField"));
    assertThat(typeDescriptor.isMap()).isTrue();
    assertThat(typeDescriptor.getMapKeyDescriptor().getType()).isEqualTo(String.class);
    assertThat(typeDescriptor.getMapValueDescriptor().getType()).isEqualTo(List.class);
    assertThat(typeDescriptor.getMapValueDescriptor().getElementDescriptor().getType()).isEqualTo(Integer.class);
    assertThat(typeDescriptor.toString()).isEqualTo("java.util.Map<java.lang.String, java.util.List<java.lang.Integer>>");
  }

  @Test
  public void fieldMap() throws Exception {
    TypeDescriptor desc = new TypeDescriptor(TypeDescriptorTests.class.getField("fieldMap"));
    assertThat(desc.isMap()).isTrue();
    assertThat(desc.getMapKeyDescriptor().getElementDescriptor().getType()).isEqualTo(Integer.class);
    assertThat(desc.getMapValueDescriptor().getElementDescriptor().getType()).isEqualTo(Long.class);
  }

  @Test
  public void fieldAnnotated() throws Exception {
    TypeDescriptor typeDescriptor = new TypeDescriptor(getClass().getField("fieldAnnotated"));
    assertThat(typeDescriptor.getAnnotations().length).isEqualTo(1);
    assertThat(typeDescriptor.getAnnotation(FieldAnnotation.class)).isNotNull();
  }

  @Test
  public void valueOfScalar() {
    TypeDescriptor desc = TypeDescriptor.valueOf(Integer.class);
    assertThat(desc.isPrimitive()).isFalse();
    assertThat(desc.isArray()).isFalse();
    assertThat(desc.isCollection()).isFalse();
    assertThat(desc.isMap()).isFalse();
    assertThat(desc.getType()).isEqualTo(Integer.class);
    assertThat(desc.getObjectType()).isEqualTo(Integer.class);
  }

  @Test
  public void valueOfPrimitive() {
    TypeDescriptor descriptor = TypeDescriptor.valueOf(int.class);
    assertThat(descriptor.isPrimitive()).isTrue();
    assertThat(descriptor.isArray()).isFalse();
    assertThat(descriptor.isCollection()).isFalse();
    assertThat(descriptor.isMap()).isFalse();
    assertThat(descriptor.getType()).isEqualTo(Integer.TYPE);
    assertThat(descriptor.getObjectType()).isEqualTo(Integer.class);
  }

  @Test
  public void valueOfArray() throws Exception {
    TypeDescriptor descriptor = TypeDescriptor.valueOf(int[].class);
    assertThat(descriptor.isArray()).isTrue();
    assertThat(descriptor.isCollection()).isFalse();
    assertThat(descriptor.isMap()).isFalse();
    assertThat(descriptor.getElementDescriptor().getType()).isEqualTo(Integer.TYPE);
  }

  @Test
  public void valueOfCollection() throws Exception {
    TypeDescriptor descriptor = TypeDescriptor.valueOf(Collection.class);
    assertThat(descriptor.isCollection()).isTrue();
    assertThat(descriptor.isArray()).isFalse();
    assertThat(descriptor.isMap()).isFalse();
    assertThat((Object) descriptor.getElementDescriptor()).isNull();
  }

  @Test
  public void fromObject() {
    TypeDescriptor desc = TypeDescriptor.fromObject("3");
    assertThat(desc.getType()).isEqualTo(String.class);
  }

  @Test
  public void forObjectNullTypeDescriptor() {
    TypeDescriptor desc = TypeDescriptor.fromObject(null);
    assertThat((Object) desc).isNull();
  }

  @Test
  public void nestedMethodParameterType2Levels() throws Exception {
    final Method test2 = getClass().getMethod("test2", List.class);
    final TypeDescriptor descriptor = TypeDescriptor.forParameter(test2, 0);
    TypeDescriptor t1 = TypeDescriptor.nested(descriptor, 2);
    assertThat(t1.getType()).isEqualTo(String.class);
  }

  @Test
  public void nestedMethodParameterTypeMap() throws Exception {
    final Method test3 = getClass().getMethod("test3", Map.class);
    final TypeDescriptor descriptor = TypeDescriptor.forParameter(test3, 0);
    TypeDescriptor t1 = TypeDescriptor.nested(descriptor, 1);
    assertThat(t1.getType()).isEqualTo(String.class);
  }

  @Test
  public void nestedMethodParameterTypeMapTwoLevels() throws Exception {
    final Method test4 = getClass().getMethod("test4", List.class);
    final TypeDescriptor descriptor = TypeDescriptor.forParameter(test4, 0);
    TypeDescriptor t1 = TypeDescriptor.nested(descriptor, 2);
    assertThat(t1.getType()).isEqualTo(String.class);
  }

  @Test
  public void nestedTooManyLevels() throws Exception {
    final Method test4 = getClass().getMethod("test4", List.class);
    final TypeDescriptor descriptor = TypeDescriptor.forParameter(test4, 0);
    TypeDescriptor t1 = TypeDescriptor.nested(descriptor, 3);
    assertThat((Object) t1).isNull();
  }

  @Test
  public void nestedMethodParameterTypeNotNestable() throws Exception {
    final Method test5 = getClass().getMethod("test5", String.class);
    final TypeDescriptor descriptor = TypeDescriptor.forParameter(test5, 0);

    TypeDescriptor t1 = TypeDescriptor.nested(descriptor, 2);
    assertThat((Object) t1).isNull();
  }

  @Test
  public void nestedNotParameterized() throws Exception {
    final Method test6 = getClass().getMethod("test6", List.class);

    final TypeDescriptor descriptor = TypeDescriptor.forParameter(test6, 0);

    TypeDescriptor t1 = TypeDescriptor.nested(descriptor, 1);
    assertThat(t1.getType()).isEqualTo(List.class);
    assertThat(t1.toString()).isEqualTo("java.util.List<?>");

    TypeDescriptor t2 = TypeDescriptor.nested(descriptor, 2);

    assertThat((Object) t2).isNull();
  }

  @Test
  public void nestedFieldTypeMapTwoLevels() throws Exception {
    TypeDescriptor t1 = TypeDescriptor.nested(getClass().getField("test4"), 2);
    assertThat(t1.getType()).isEqualTo(String.class);
  }

  @Test
  public void collection() {
    TypeDescriptor desc = TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(Integer.class));
    assertThat(desc.getType()).isEqualTo(List.class);
    assertThat(desc.getObjectType()).isEqualTo(List.class);
    assertThat(desc.getName()).isEqualTo("java.util.List");
    assertThat(desc.toString()).isEqualTo("java.util.List<java.lang.Integer>");
    assertThat(!desc.isPrimitive()).isTrue();
    assertThat(desc.getAnnotations().length).isEqualTo(0);
    assertThat(desc.isCollection()).isTrue();
    assertThat(desc.isArray()).isFalse();
    assertThat(desc.getElementDescriptor().getType()).isEqualTo(Integer.class);
    assertThat(desc.getElementDescriptor()).isEqualTo(TypeDescriptor.valueOf(Integer.class));
    assertThat(desc.isMap()).isFalse();
  }

  @Test
  public void collectionNested() {
    TypeDescriptor desc = TypeDescriptor.collection(List.class, TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(Integer.class)));
    assertThat(desc.getType()).isEqualTo(List.class);
    assertThat(desc.getObjectType()).isEqualTo(List.class);
    assertThat(desc.getName()).isEqualTo("java.util.List");
    assertThat(desc.toString()).isEqualTo("java.util.List<java.util.List<java.lang.Integer>>");
    assertThat(!desc.isPrimitive()).isTrue();
    assertThat(desc.getAnnotations().length).isEqualTo(0);
    assertThat(desc.isCollection()).isTrue();
    assertThat(desc.isArray()).isFalse();
    assertThat(desc.getElementDescriptor().getType()).isEqualTo(List.class);
    assertThat(desc.getElementDescriptor().getElementDescriptor()).isEqualTo(TypeDescriptor.valueOf(Integer.class));
    assertThat(desc.isMap()).isFalse();
  }

  @Test
  public void map() {
    TypeDescriptor desc = TypeDescriptor.map(Map.class, TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Integer.class));
    assertThat(desc.getType()).isEqualTo(Map.class);
    assertThat(desc.getObjectType()).isEqualTo(Map.class);
    assertThat(desc.getName()).isEqualTo("java.util.Map");
    assertThat(desc.toString()).isEqualTo("java.util.Map<java.lang.String, java.lang.Integer>");
    assertThat(!desc.isPrimitive()).isTrue();
    assertThat(desc.getAnnotations().length).isEqualTo(0);
    assertThat(desc.isCollection()).isFalse();
    assertThat(desc.isArray()).isFalse();
    assertThat(desc.isMap()).isTrue();
    assertThat(desc.getMapKeyDescriptor().getType()).isEqualTo(String.class);
    assertThat(desc.getMapValueDescriptor().getType()).isEqualTo(Integer.class);
  }

  @Test
  public void mapNested() {
    TypeDescriptor desc = TypeDescriptor.map(Map.class, TypeDescriptor.valueOf(String.class),
            TypeDescriptor.map(Map.class, TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Integer.class)));
    assertThat(desc.getType()).isEqualTo(Map.class);
    assertThat(desc.getObjectType()).isEqualTo(Map.class);
    assertThat(desc.getName()).isEqualTo("java.util.Map");
    assertThat(desc.toString()).isEqualTo("java.util.Map<java.lang.String, java.util.Map<java.lang.String, java.lang.Integer>>");
    assertThat(!desc.isPrimitive()).isTrue();
    assertThat(desc.getAnnotations().length).isEqualTo(0);
    assertThat(desc.isCollection()).isFalse();
    assertThat(desc.isArray()).isFalse();
    assertThat(desc.isMap()).isTrue();
    assertThat(desc.getMapKeyDescriptor().getType()).isEqualTo(String.class);
    assertThat(desc.getMapValueDescriptor().getMapKeyDescriptor().getType()).isEqualTo(String.class);
    assertThat(desc.getMapValueDescriptor().getMapValueDescriptor().getType()).isEqualTo(Integer.class);
  }

  @Test
  public void narrow() {
    TypeDescriptor desc = TypeDescriptor.valueOf(Number.class);
    Integer value = Integer.valueOf(3);
    desc = desc.narrow(value);
    assertThat(desc.getType()).isEqualTo(Integer.class);
  }

  @Test
  public void elementType() {
    TypeDescriptor desc = TypeDescriptor.valueOf(List.class);
    Integer value = Integer.valueOf(3);
    desc = desc.elementDescriptor(value);
    assertThat(desc.getType()).isEqualTo(Integer.class);
  }

  @Test
  public void elementTypePreserveContext() throws Exception {
    TypeDescriptor desc = new TypeDescriptor(getClass().getField("listPreserveContext"));
    assertThat(desc.getElementDescriptor().getElementDescriptor().getType()).isEqualTo(Integer.class);
    List<Integer> value = new ArrayList<>(3);
    desc = desc.elementDescriptor(value);
    assertThat(desc.getElementDescriptor().getType()).isEqualTo(Integer.class);
    assertThat(desc.getAnnotation(FieldAnnotation.class)).isNotNull();
  }

  @Test
  public void mapKeyType() {
    TypeDescriptor desc = TypeDescriptor.valueOf(Map.class);
    Integer value = Integer.valueOf(3);
    desc = desc.getMapKeyDescriptor(value);
    assertThat(desc.getType()).isEqualTo(Integer.class);
  }

  @Test
  public void mapKeyTypePreserveContext() throws Exception {
    TypeDescriptor desc = new TypeDescriptor(getClass().getField("mapPreserveContext"));
    assertThat(desc.getMapKeyDescriptor().getElementDescriptor().getType()).isEqualTo(Integer.class);
    List<Integer> value = new ArrayList<>(3);
    desc = desc.getMapKeyDescriptor(value);
    assertThat(desc.getElementDescriptor().getType()).isEqualTo(Integer.class);
    assertThat(desc.getAnnotation(FieldAnnotation.class)).isNotNull();
  }

  @Test
  public void mapValueType() {
    TypeDescriptor desc = TypeDescriptor.valueOf(Map.class);
    Integer value = Integer.valueOf(3);
    desc = desc.getMapValueDescriptor(value);
    assertThat(desc.getType()).isEqualTo(Integer.class);
  }

  @Test
  public void mapValueTypePreserveContext() throws Exception {
    TypeDescriptor desc = new TypeDescriptor(getClass().getField("mapPreserveContext"));
    assertThat(desc.getMapValueDescriptor().getElementDescriptor().getType()).isEqualTo(Integer.class);
    List<Integer> value = new ArrayList<>(3);
    desc = desc.getMapValueDescriptor(value);
    assertThat(desc.getElementDescriptor().getType()).isEqualTo(Integer.class);
    assertThat(desc.getAnnotation(FieldAnnotation.class)).isNotNull();
  }

  @Test
  public void equality() throws Exception {
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

    final Method testAnnotatedMethod1 = getClass().getMethod("testAnnotatedMethod", String.class);

    final TypeDescriptor t13 = TypeDescriptor.forParameter(testAnnotatedMethod1, 0);
    final TypeDescriptor t14 = TypeDescriptor.forParameter(testAnnotatedMethod1, 0);
    assertThat(t14).isEqualTo(t13);

    final TypeDescriptor t15 = TypeDescriptor.forParameter(testAnnotatedMethod1, 0);

    final Method testAnnotatedMethodDifferentAnnotationValue = getClass()
            .getMethod("testAnnotatedMethodDifferentAnnotationValue", String.class);
    final TypeDescriptor t16 = TypeDescriptor.forParameter(testAnnotatedMethodDifferentAnnotationValue, 0);

    assertThat(t16).isNotEqualTo(t15);

    final TypeDescriptor t17 = TypeDescriptor.forParameter(testAnnotatedMethod1, 0);
    final Method test5 = getClass().getMethod("test5", String.class);

    final TypeDescriptor t18 = TypeDescriptor.forParameter(test5, 0);
    assertThat(t18).isNotEqualTo(t17);
  }

  @Test
  public void isAssignableTypes() {
    assertThat(TypeDescriptor.valueOf(Integer.class).isAssignableTo(TypeDescriptor.valueOf(Number.class))).isTrue();
    assertThat(TypeDescriptor.valueOf(Number.class).isAssignableTo(TypeDescriptor.valueOf(Integer.class))).isFalse();
    assertThat(TypeDescriptor.valueOf(String.class).isAssignableTo(TypeDescriptor.valueOf(String[].class))).isFalse();
  }

  @Test
  public void isAssignableElementTypes() throws Exception {
    assertThat(new TypeDescriptor(getClass().getField("listField")).isAssignableTo(new TypeDescriptor(getClass().getField("listField")))).isTrue();
    assertThat(new TypeDescriptor(getClass().getField("notGenericList")).isAssignableTo(new TypeDescriptor(getClass().getField("listField")))).isTrue();
    assertThat(new TypeDescriptor(getClass().getField("listField")).isAssignableTo(new TypeDescriptor(getClass().getField("notGenericList")))).isTrue();
    assertThat(new TypeDescriptor(getClass().getField("isAssignableElementTypes")).isAssignableTo(new TypeDescriptor(getClass().getField("listField")))).isFalse();
    assertThat(TypeDescriptor.valueOf(List.class).isAssignableTo(new TypeDescriptor(getClass().getField("listField")))).isTrue();
  }

  @Test
  public void isAssignableMapKeyValueTypes() throws Exception {
    assertThat(new TypeDescriptor(getClass().getField("mapField")).isAssignableTo(new TypeDescriptor(getClass().getField("mapField")))).isTrue();
    assertThat(new TypeDescriptor(getClass().getField("notGenericMap")).isAssignableTo(new TypeDescriptor(getClass().getField("mapField")))).isTrue();
    assertThat(new TypeDescriptor(getClass().getField("mapField")).isAssignableTo(new TypeDescriptor(getClass().getField("notGenericMap")))).isTrue();
    assertThat(new TypeDescriptor(getClass().getField("isAssignableMapKeyValueTypes")).isAssignableTo(new TypeDescriptor(getClass().getField("mapField")))).isFalse();
    assertThat(TypeDescriptor.valueOf(Map.class).isAssignableTo(new TypeDescriptor(getClass().getField("mapField")))).isTrue();
  }

  @Test
  public void multiValueMap() throws Exception {
    TypeDescriptor td = new TypeDescriptor(getClass().getField("multiValueMap"));
    assertThat(td.isMap()).isTrue();
    assertThat(td.getMapKeyDescriptor().getType()).isEqualTo(String.class);
    assertThat(td.getMapValueDescriptor().getType()).isEqualTo(List.class);
    assertThat(td.getMapValueDescriptor().getElementDescriptor().getType()).isEqualTo(Integer.class);
  }

  @Test
  public void passDownGeneric() throws Exception {
    TypeDescriptor td = new TypeDescriptor(getClass().getField("passDownGeneric"));
    assertThat(td.getElementDescriptor().getType()).isEqualTo(List.class);
    assertThat(td.getElementDescriptor().getElementDescriptor().getType()).isEqualTo(Set.class);
    assertThat(td.getElementDescriptor().getElementDescriptor().getElementDescriptor().getType()).isEqualTo(Integer.class);
  }

  @Test
  public void elementTypeForCollectionSubclass() throws Exception {
    @SuppressWarnings("serial")
    class CustomSet extends HashSet<String> {
    }

    assertThat(TypeDescriptor.valueOf(String.class)).isEqualTo(TypeDescriptor.valueOf(CustomSet.class).getElementDescriptor());
    assertThat(TypeDescriptor.valueOf(String.class)).isEqualTo(TypeDescriptor.fromObject(new CustomSet()).getElementDescriptor());
  }

  @Test
  public void elementTypeForMapSubclass() throws Exception {
    @SuppressWarnings("serial")
    class CustomMap extends HashMap<String, Integer> {
    }

    assertThat(TypeDescriptor.valueOf(String.class)).isEqualTo(TypeDescriptor.valueOf(CustomMap.class).getMapKeyDescriptor());
    assertThat(TypeDescriptor.valueOf(Integer.class)).isEqualTo(TypeDescriptor.valueOf(CustomMap.class).getMapValueDescriptor());
    assertThat(TypeDescriptor.valueOf(String.class)).isEqualTo(TypeDescriptor.fromObject(new CustomMap()).getMapKeyDescriptor());
    assertThat(TypeDescriptor.valueOf(Integer.class)).isEqualTo(TypeDescriptor.fromObject(new CustomMap()).getMapValueDescriptor());
  }

  @Test
  public void createMapArray() throws Exception {
    TypeDescriptor mapType = TypeDescriptor.map(
            LinkedHashMap.class, TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Integer.class));
    TypeDescriptor arrayType = TypeDescriptor.array(mapType);
    assertThat(LinkedHashMap[].class).isEqualTo(arrayType.getType());
    assertThat(mapType).isEqualTo(arrayType.getElementDescriptor());
  }

  @Test
  public void createStringArray() throws Exception {
    TypeDescriptor arrayType = TypeDescriptor.array(TypeDescriptor.valueOf(String.class));
    assertThat(TypeDescriptor.valueOf(String[].class)).isEqualTo(arrayType);
  }

  @Test
  public void createNullArray() throws Exception {
    assertThat((Object) TypeDescriptor.array(null)).isNull();
  }

  @Test
  public void serializable() throws Exception {
    TypeDescriptor descriptor = TypeDescriptor.fromObject("");
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ObjectOutputStream outputStream = new ObjectOutputStream(out);
    outputStream.writeObject(descriptor);
    ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(
            out.toByteArray()));
    TypeDescriptor readObject = (TypeDescriptor) inputStream.readObject();
    assertThat(readObject).isEqualTo(descriptor);
  }

  @Test
  public void createCollectionWithNullElement() throws Exception {
    TypeDescriptor descriptor = TypeDescriptor.collection(List.class, (TypeDescriptor) null);
    assertThat(descriptor.getElementDescriptor()).isNull();
  }

  @Test
  public void createMapWithNullElements() throws Exception {
    TypeDescriptor descriptor = TypeDescriptor.map(LinkedHashMap.class, (TypeDescriptor) null, null);
    assertThat(descriptor.getMapKeyDescriptor()).isNull();
    assertThat(descriptor.getMapValueDescriptor()).isNull();
  }

  @Test
  public void getSource() throws Exception {
    Field field = getClass().getField("fieldScalar");

    final Method testParameterPrimitive = getClass().getMethod("testParameterPrimitive", int.class);
    final Parameter[] parameters = testParameterPrimitive.getParameters();
    final Parameter parameter = parameters[0];

    final TypeDescriptor descriptor = TypeDescriptor.forParameter(testParameterPrimitive, 0);

    assertThat(new TypeDescriptor(field).getSource()).isEqualTo(field);

    assertThat(descriptor.getSource()).isEqualTo(parameter);

    assertThat(TypeDescriptor.valueOf(Integer.class).getSource()).isEqualTo(Integer.class);
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
    return complexProperty;
  }

  Map<String, List<List<Integer>>> complexProperty;

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
    this.complexProperty = complexProperty;
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

  public MultiValueMap<String, Integer> multiValueMap = new DefaultMultiValueMap<>();

  public PassDownGeneric<Integer> passDownGeneric = new PassDownGeneric<>();

  // Classes designed for test introspection

  @SuppressWarnings("serial")
  public static class PassDownGeneric<T> extends ArrayList<List<Set<T>>> {
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
