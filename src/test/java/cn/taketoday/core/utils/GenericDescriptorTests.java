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

package cn.taketoday.core.utils;

import org.junit.Test;

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

import cn.taketoday.beans.BeanProperty;
import cn.taketoday.core.DefaultMultiValueMap;
import cn.taketoday.core.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author TODAY 2021/3/23 11:50
 * @since 3.0
 */
public class GenericDescriptorTests {

  @Test
  public void parameterPrimitive() throws Exception {
    final Method testParameterPrimitive = getClass().getMethod("testParameterPrimitive", int.class);
    final GenericDescriptor desc = GenericDescriptor.ofParameter(testParameterPrimitive, 0);

    assertThat(desc.getType()).isEqualTo(int.class);
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
    final GenericDescriptor desc = GenericDescriptor.ofParameter(testParameterScalar, 0);

    assertThat(desc.getType()).isEqualTo(String.class);
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
    final GenericDescriptor desc = GenericDescriptor.ofParameter(testParameterList, 0);

    assertThat(desc.getType()).isEqualTo(List.class);
    assertThat(desc.getName()).isEqualTo("java.util.List");
    assertThat(desc.toString()).isEqualTo("java.util.List<java.util.List<java.util.Map<java.lang.Integer, java.lang.Enum<?>>>>");
    assertThat(!desc.isPrimitive()).isTrue();
    assertThat(desc.getAnnotations().length).isEqualTo(0);
    assertThat(desc.isCollection()).isTrue();
    assertThat(desc.isArray()).isFalse();
    assertThat(desc.getElementDescriptor().getType()).isEqualTo(List.class);
    assertThat(desc.getElementDescriptor()).isEqualTo(GenericDescriptor.nested(desc, 1));
    assertThat(desc.getElementDescriptor().getElementDescriptor()).isEqualTo(GenericDescriptor.nested(desc, 2));
    assertThat(desc.getElementDescriptor().getElementDescriptor().getMapValueGenericDescriptor()).isEqualTo(GenericDescriptor.nested(desc, 3));
    assertThat(desc.getElementDescriptor().getElementDescriptor().getMapKeyGenericDescriptor().getType()).isEqualTo(Integer.class);
    assertThat(desc.getElementDescriptor().getElementDescriptor().getMapValueGenericDescriptor().getType()).isEqualTo(Enum.class);
    assertThat(desc.isMap()).isFalse();
  }

  @Test
  public void parameterListNoParamTypes() throws Exception {
    final Method testParameterListNoParamTypes = getClass().getMethod("testParameterListNoParamTypes", List.class);
    GenericDescriptor desc = GenericDescriptor.ofParameter(testParameterListNoParamTypes, 0);

    assertThat(desc.getType()).isEqualTo(List.class);
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
    GenericDescriptor desc = GenericDescriptor.ofParameter(testParameterArray, 0);

    assertThat(desc.getType()).isEqualTo(Integer[].class);
    assertThat(desc.getName()).isEqualTo("java.lang.Integer[]");
    assertThat(desc.toString()).isEqualTo("java.lang.Integer[]");
    assertThat(!desc.isPrimitive()).isTrue();
    assertThat(desc.getAnnotations().length).isEqualTo(0);
    assertThat(desc.isCollection()).isFalse();
    assertThat(desc.isArray()).isTrue();
    assertThat(desc.getElementDescriptor().getType()).isEqualTo(Integer.class);
    assertThat(desc.getElementDescriptor()).isEqualTo(GenericDescriptor.valueOf(Integer.class));
    assertThat(desc.isMap()).isFalse();
  }

  @Test
  public void parameterMap() throws Exception {
    final Method testParameterMap = getClass().getMethod("testParameterMap", Map.class);

    GenericDescriptor desc = GenericDescriptor.ofParameter(testParameterMap, 0);

    assertThat(desc.getType()).isEqualTo(Map.class);
    assertThat(desc.getName()).isEqualTo("java.util.Map");
    assertThat(desc.toString()).isEqualTo("java.util.Map<java.lang.Integer, java.util.List<java.lang.String>>");
    assertThat(!desc.isPrimitive()).isTrue();
    assertThat(desc.getAnnotations().length).isEqualTo(0);
    assertThat(desc.isCollection()).isFalse();
    assertThat(desc.isArray()).isFalse();
    assertThat(desc.isMap()).isTrue();
    assertThat(desc.getMapValueGenericDescriptor()).isEqualTo(GenericDescriptor.nested(desc, 1));
    assertThat(desc.getMapValueGenericDescriptor().getElementDescriptor()).isEqualTo(GenericDescriptor.nested(desc, 2));
    assertThat(desc.getMapKeyGenericDescriptor().getType()).isEqualTo(Integer.class);
    assertThat(desc.getMapValueGenericDescriptor().getType()).isEqualTo(List.class);
    assertThat(desc.getMapValueGenericDescriptor().getElementDescriptor().getType()).isEqualTo(String.class);
  }

  @Test
  public void parameterAnnotated() throws Exception {
    final Method testAnnotatedMethod = getClass().getMethod("testAnnotatedMethod", String.class);
    final GenericDescriptor t1 = GenericDescriptor.ofParameter(testAnnotatedMethod, 0);

    assertThat(t1.getType()).isEqualTo(String.class);
    assertThat(t1.getAnnotations().length).isEqualTo(1);
    assertThat(t1.getAnnotation(ParameterAnnotation.class)).isNotNull();
    assertThat(t1.hasAnnotation(ParameterAnnotation.class)).isTrue();
    assertThat(t1.getAnnotation(ParameterAnnotation.class).value()).isEqualTo(123);
  }

  @Test
  public void getAnnotationsReturnsClonedArray() throws Exception {
    final Method testAnnotatedMethod = getClass().getMethod("testAnnotatedMethod", String.class);
    final GenericDescriptor t = GenericDescriptor.ofParameter(testAnnotatedMethod, 0);
    t.getAnnotations()[0] = null;
    assertThat(t.getAnnotations()[0]).isNotNull();
  }

//  @Test
//  public void propertyComplex() throws Exception {
//    final BeanProperty complexProperty = BeanProperty.of(getClass(), "complexProperty");
//    GenericDescriptor desc = GenericDescriptor.ofProperty(complexProperty);
//
//    assertThat(desc.getMapKeyGenericDescriptor().getType()).isEqualTo(String.class);
//    assertThat(desc.getMapValueGenericDescriptor().getElementDescriptor().getElementDescriptor().getType())
//            .isEqualTo(Integer.class);
//  }

//  public void propertyGenericType() throws Exception {
//    GenericType<Integer> genericBean = new IntegerType();
//    final BeanProperty property = BeanProperty.of(genericBean.getClass(), "property");
//    GenericDescriptor desc = new GenericDescriptor(property.getField());
//    assertThat(desc.getType()).isEqualTo(Integer.class);
//  }

//  @Test
//  public void propertyTypeCovariance() throws Exception {
//    GenericType<Number> genericBean = new NumberType();
//    final BeanProperty property = BeanProperty.of(getClass(), "property");
//    GenericDescriptor desc = new GenericDescriptor(property.getField());
//    assertThat(desc.getType()).isEqualTo(Integer.class);
//  }

//  @Test
//  public void propertyGenericTypeList() throws Exception {
//    GenericType<Integer> genericBean = new IntegerType();
//    final BeanProperty listProperty = BeanProperty.of(getClass(), "listProperty");
//
//    GenericDescriptor desc = new GenericDescriptor(listProperty);
//    assertThat(desc.getType()).isEqualTo(List.class);
//    assertThat(desc.getElementDescriptor().getType()).isEqualTo(Integer.class);
//  }

//  @Test
//  public void propertyGenericClassList() throws Exception {
//    IntegerClass genericBean = new IntegerClass();
//    final BeanProperty listProperty = BeanProperty.of(getClass(), "listProperty");
//
//    GenericDescriptor desc = new GenericDescriptor(listProperty);
//    assertThat(desc.getType()).isEqualTo(List.class);
//    assertThat(desc.getElementDescriptor().getType()).isEqualTo(Integer.class);
//    assertThat(desc.getAnnotation(MethodAnnotation1.class)).isNotNull();
//    assertThat(desc.hasAnnotation(MethodAnnotation1.class)).isTrue();
//  }

  @Test
  public void property() throws Exception {
    final BeanProperty property = BeanProperty.of(getClass(), "property");

    GenericDescriptor desc = new GenericDescriptor(property);

    assertThat(desc.getType()).isEqualTo(Map.class);

    assertThat(desc.getMapKeyGenericDescriptor().getElementDescriptor().getType()).isEqualTo(Integer.class);
    assertThat(desc.getMapValueGenericDescriptor().getElementDescriptor().getType()).isEqualTo(Long.class);

    assertThat(desc.getAnnotation(MethodAnnotation1.class)).isNotNull();
    assertThat(desc.getAnnotation(MethodAnnotation2.class)).isNotNull();
    assertThat(desc.getAnnotation(MethodAnnotation3.class)).isNotNull();
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
    final GenericDescriptor descriptor = new GenericDescriptor(resolvableType, null, method.getAnnotations());
    assertThat(descriptor.getAnnotation(annotationType))
            .as("Should have found @" + annotationType.getSimpleName() + " on " + methodName + ".")
            .isNotNull();
  }

  @Test
  public void fieldScalar() throws Exception {
    GenericDescriptor GenericDescriptor = new GenericDescriptor(getClass().getField("fieldScalar"));
    assertThat(GenericDescriptor.isPrimitive()).isFalse();
    assertThat(GenericDescriptor.isArray()).isFalse();
    assertThat(GenericDescriptor.isCollection()).isFalse();
    assertThat(GenericDescriptor.isMap()).isFalse();
    assertThat(GenericDescriptor.getType()).isEqualTo(Integer.class);
  }

  @Test
  public void fieldList() throws Exception {
    GenericDescriptor GenericDescriptor = new GenericDescriptor(GenericDescriptorTests.class.getDeclaredField("listOfString"));
    assertThat(GenericDescriptor.isArray()).isFalse();
    assertThat(GenericDescriptor.getType()).isEqualTo(List.class);
    assertThat(GenericDescriptor.getElementDescriptor().getType()).isEqualTo(String.class);
    assertThat(GenericDescriptor.toString()).isEqualTo("java.util.List<java.lang.String>");
  }

  @Test
  public void fieldListOfListOfString() throws Exception {
    GenericDescriptor GenericDescriptor = new GenericDescriptor(GenericDescriptorTests.class.getDeclaredField("listOfListOfString"));
    assertThat(GenericDescriptor.isArray()).isFalse();
    assertThat(GenericDescriptor.getType()).isEqualTo(List.class);
    assertThat(GenericDescriptor.getElementDescriptor().getType()).isEqualTo(List.class);
    assertThat(GenericDescriptor.getElementDescriptor().getElementDescriptor().getType()).isEqualTo(String.class);
    assertThat(GenericDescriptor.toString()).isEqualTo("java.util.List<java.util.List<java.lang.String>>");
  }

  @Test
  public void fieldListOfListUnknown() throws Exception {
    GenericDescriptor GenericDescriptor = new GenericDescriptor(GenericDescriptorTests.class.getDeclaredField("listOfListOfUnknown"));
    assertThat(GenericDescriptor.isArray()).isFalse();
    assertThat(GenericDescriptor.getType()).isEqualTo(List.class);
    assertThat(GenericDescriptor.getElementDescriptor().getType()).isEqualTo(List.class);
    assertThat(GenericDescriptor.getElementDescriptor().getElementDescriptor()).isNull();
    assertThat(GenericDescriptor.toString()).isEqualTo("java.util.List<java.util.List<?>>");
  }

  @Test
  public void fieldArray() throws Exception {
    GenericDescriptor GenericDescriptor = new GenericDescriptor(GenericDescriptorTests.class.getDeclaredField("intArray"));
    assertThat(GenericDescriptor.isArray()).isTrue();
    assertThat(GenericDescriptor.getElementDescriptor().getType()).isEqualTo(Integer.TYPE);
    assertThat(GenericDescriptor.toString()).isEqualTo("int[]");
  }

  @Test
  public void fieldComplexGenericDescriptor() throws Exception {
    GenericDescriptor GenericDescriptor = new GenericDescriptor(GenericDescriptorTests.class.getDeclaredField("arrayOfListOfString"));
    assertThat(GenericDescriptor.isArray()).isTrue();
    assertThat(GenericDescriptor.getElementDescriptor().getType()).isEqualTo(List.class);
    assertThat(GenericDescriptor.getElementDescriptor().getElementDescriptor().getType()).isEqualTo(String.class);
    assertThat(GenericDescriptor.toString()).isEqualTo("java.util.List<java.lang.String>[]");
  }

  @Test
  public void fieldComplexGenericDescriptor2() throws Exception {
    GenericDescriptor GenericDescriptor = new GenericDescriptor(GenericDescriptorTests.class.getDeclaredField("nestedMapField"));
    assertThat(GenericDescriptor.isMap()).isTrue();
    assertThat(GenericDescriptor.getMapKeyGenericDescriptor().getType()).isEqualTo(String.class);
    assertThat(GenericDescriptor.getMapValueGenericDescriptor().getType()).isEqualTo(List.class);
    assertThat(GenericDescriptor.getMapValueGenericDescriptor().getElementDescriptor().getType()).isEqualTo(Integer.class);
    assertThat(GenericDescriptor.toString()).isEqualTo("java.util.Map<java.lang.String, java.util.List<java.lang.Integer>>");
  }

  @Test
  public void fieldMap() throws Exception {
    GenericDescriptor desc = new GenericDescriptor(GenericDescriptorTests.class.getField("fieldMap"));
    assertThat(desc.isMap()).isTrue();
    assertThat(desc.getMapKeyGenericDescriptor().getElementDescriptor().getType()).isEqualTo(Integer.class);
    assertThat(desc.getMapValueGenericDescriptor().getElementDescriptor().getType()).isEqualTo(Long.class);
  }

  @Test
  public void fieldAnnotated() throws Exception {
    GenericDescriptor GenericDescriptor = new GenericDescriptor(getClass().getField("fieldAnnotated"));
    assertThat(GenericDescriptor.getAnnotations().length).isEqualTo(1);
    assertThat(GenericDescriptor.getAnnotation(FieldAnnotation.class)).isNotNull();
  }

  @Test
  public void valueOfScalar() {
    GenericDescriptor desc = GenericDescriptor.valueOf(Integer.class);
    assertThat(desc.isPrimitive()).isFalse();
    assertThat(desc.isArray()).isFalse();
    assertThat(desc.isCollection()).isFalse();
    assertThat(desc.isMap()).isFalse();
    assertThat(desc.getType()).isEqualTo(Integer.class);
  }

  @Test
  public void valueOfPrimitive() {
    GenericDescriptor descriptor = GenericDescriptor.valueOf(int.class);
    assertThat(descriptor.isPrimitive()).isTrue();
    assertThat(descriptor.isArray()).isFalse();
    assertThat(descriptor.isCollection()).isFalse();
    assertThat(descriptor.isMap()).isFalse();
    assertThat(descriptor.getType()).isEqualTo(Integer.TYPE);
  }

  @Test
  public void valueOfArray() throws Exception {
    GenericDescriptor descriptor = GenericDescriptor.valueOf(int[].class);
    assertThat(descriptor.isArray()).isTrue();
    assertThat(descriptor.isCollection()).isFalse();
    assertThat(descriptor.isMap()).isFalse();
    assertThat(descriptor.getElementDescriptor().getType()).isEqualTo(Integer.TYPE);
  }

  @Test
  public void valueOfCollection() throws Exception {
    GenericDescriptor descriptor = GenericDescriptor.valueOf(Collection.class);
    assertThat(descriptor.isCollection()).isTrue();
    assertThat(descriptor.isArray()).isFalse();
    assertThat(descriptor.isMap()).isFalse();
    assertThat((Object) descriptor.getElementDescriptor()).isNull();
  }

  @Test
  public void forObject() {
    GenericDescriptor desc = GenericDescriptor.forObject("3");
    assertThat(desc.getType()).isEqualTo(String.class);
  }

  @Test
  public void forObjectNullGenericDescriptor() {
    GenericDescriptor desc = GenericDescriptor.forObject(null);
    assertThat((Object) desc).isNull();
  }

  @Test
  public void nestedMethodParameterType2Levels() throws Exception {
    final Method test2 = getClass().getMethod("test2", List.class);
    final GenericDescriptor descriptor = GenericDescriptor.ofParameter(test2, 0);
    GenericDescriptor t1 = GenericDescriptor.nested(descriptor, 2);
    assertThat(t1.getType()).isEqualTo(String.class);
  }

  @Test
  public void nestedMethodParameterTypeMap() throws Exception {
    final Method test3 = getClass().getMethod("test3", Map.class);
    final GenericDescriptor descriptor = GenericDescriptor.ofParameter(test3, 0);
    GenericDescriptor t1 = GenericDescriptor.nested(descriptor, 1);
    assertThat(t1.getType()).isEqualTo(String.class);
  }

  @Test
  public void nestedMethodParameterTypeMapTwoLevels() throws Exception {
    final Method test4 = getClass().getMethod("test4", List.class);
    final GenericDescriptor descriptor = GenericDescriptor.ofParameter(test4, 0);
    GenericDescriptor t1 = GenericDescriptor.nested(descriptor, 2);
    assertThat(t1.getType()).isEqualTo(String.class);
  }

  @Test
  public void nestedTooManyLevels() throws Exception {
    final Method test4 = getClass().getMethod("test4", List.class);
    final GenericDescriptor descriptor = GenericDescriptor.ofParameter(test4, 0);
    GenericDescriptor t1 = GenericDescriptor.nested(descriptor, 3);
    assertThat((Object) t1).isNull();
  }

  @Test
  public void nestedMethodParameterTypeNotNestable() throws Exception {
    final Method test5 = getClass().getMethod("test5", String.class);
    final GenericDescriptor descriptor = GenericDescriptor.ofParameter(test5, 0);

    GenericDescriptor t1 = GenericDescriptor.nested(descriptor, 2);
    assertThat((Object) t1).isNull();
  }

  @Test
  public void nestedNotParameterized() throws Exception {
    final Method test6 = getClass().getMethod("test6", List.class);

    final GenericDescriptor descriptor = GenericDescriptor.ofParameter(test6, 0);

    GenericDescriptor t1 = GenericDescriptor.nested(descriptor, 1);
    assertThat(t1.getType()).isEqualTo(List.class);
    assertThat(t1.toString()).isEqualTo("java.util.List<?>");

    GenericDescriptor t2 = GenericDescriptor.nested(descriptor, 2);

    assertThat((Object) t2).isNull();
  }

  @Test
  public void nestedFieldTypeMapTwoLevels() throws Exception {
    GenericDescriptor t1 = GenericDescriptor.nested(getClass().getField("test4"), 2);
    assertThat(t1.getType()).isEqualTo(String.class);
  }

  @Test
  public void nestedPropertyTypeMapTwoLevels() throws Exception {

    final BeanProperty property = BeanProperty.of(getClass(), "test4");
    GenericDescriptor t1 = GenericDescriptor.nested(property.getField(), 2);

    assertThat(t1.getType()).isEqualTo(String.class);
  }

  @Test
  public void collection() {
    GenericDescriptor desc = GenericDescriptor.collection(List.class, GenericDescriptor.valueOf(Integer.class));
    assertThat(desc.getType()).isEqualTo(List.class);
    assertThat(desc.getName()).isEqualTo("java.util.List");
    assertThat(desc.toString()).isEqualTo("java.util.List<java.lang.Integer>");
    assertThat(!desc.isPrimitive()).isTrue();
    assertThat(desc.getAnnotations().length).isEqualTo(0);
    assertThat(desc.isCollection()).isTrue();
    assertThat(desc.isArray()).isFalse();
    assertThat(desc.getElementDescriptor().getType()).isEqualTo(Integer.class);
    assertThat(desc.getElementDescriptor()).isEqualTo(GenericDescriptor.valueOf(Integer.class));
    assertThat(desc.isMap()).isFalse();
  }

  @Test
  public void collectionNested() {
    GenericDescriptor desc = GenericDescriptor.collection(List.class, GenericDescriptor.collection(List.class, GenericDescriptor.valueOf(Integer.class)));
    assertThat(desc.getType()).isEqualTo(List.class);
    assertThat(desc.getName()).isEqualTo("java.util.List");
    assertThat(desc.toString()).isEqualTo("java.util.List<java.util.List<java.lang.Integer>>");
    assertThat(!desc.isPrimitive()).isTrue();
    assertThat(desc.getAnnotations().length).isEqualTo(0);
    assertThat(desc.isCollection()).isTrue();
    assertThat(desc.isArray()).isFalse();
    assertThat(desc.getElementDescriptor().getType()).isEqualTo(List.class);
    assertThat(desc.getElementDescriptor().getElementDescriptor()).isEqualTo(GenericDescriptor.valueOf(Integer.class));
    assertThat(desc.isMap()).isFalse();
  }

  @Test
  public void map() {
    GenericDescriptor desc = GenericDescriptor.map(Map.class, GenericDescriptor.valueOf(String.class), GenericDescriptor.valueOf(Integer.class));
    assertThat(desc.getType()).isEqualTo(Map.class);
    assertThat(desc.getName()).isEqualTo("java.util.Map");
    assertThat(desc.toString()).isEqualTo("java.util.Map<java.lang.String, java.lang.Integer>");
    assertThat(!desc.isPrimitive()).isTrue();
    assertThat(desc.getAnnotations().length).isEqualTo(0);
    assertThat(desc.isCollection()).isFalse();
    assertThat(desc.isArray()).isFalse();
    assertThat(desc.isMap()).isTrue();
    assertThat(desc.getMapKeyGenericDescriptor().getType()).isEqualTo(String.class);
    assertThat(desc.getMapValueGenericDescriptor().getType()).isEqualTo(Integer.class);
  }

  @Test
  public void mapNested() {
    GenericDescriptor desc = GenericDescriptor.map(Map.class, GenericDescriptor.valueOf(String.class),
                                             GenericDescriptor.map(Map.class, GenericDescriptor.valueOf(String.class), GenericDescriptor.valueOf(Integer.class)));
    assertThat(desc.getType()).isEqualTo(Map.class);
    assertThat(desc.getName()).isEqualTo("java.util.Map");
    assertThat(desc.toString()).isEqualTo("java.util.Map<java.lang.String, java.util.Map<java.lang.String, java.lang.Integer>>");
    assertThat(!desc.isPrimitive()).isTrue();
    assertThat(desc.getAnnotations().length).isEqualTo(0);
    assertThat(desc.isCollection()).isFalse();
    assertThat(desc.isArray()).isFalse();
    assertThat(desc.isMap()).isTrue();
    assertThat(desc.getMapKeyGenericDescriptor().getType()).isEqualTo(String.class);
    assertThat(desc.getMapValueGenericDescriptor().getMapKeyGenericDescriptor().getType()).isEqualTo(String.class);
    assertThat(desc.getMapValueGenericDescriptor().getMapValueGenericDescriptor().getType()).isEqualTo(Integer.class);
  }

  @Test
  public void narrow() {
    GenericDescriptor desc = GenericDescriptor.valueOf(Number.class);
    Integer value = Integer.valueOf(3);
    desc = desc.narrow(value);
    assertThat(desc.getType()).isEqualTo(Integer.class);
  }

  @Test
  public void elementType() {
    GenericDescriptor desc = GenericDescriptor.valueOf(List.class);
    Integer value = Integer.valueOf(3);
    desc = desc.elementGenericDescriptor(value);
    assertThat(desc.getType()).isEqualTo(Integer.class);
  }

  @Test
  public void elementTypePreserveContext() throws Exception {
    GenericDescriptor desc = new GenericDescriptor(getClass().getField("listPreserveContext"));
    assertThat(desc.getElementDescriptor().getElementDescriptor().getType()).isEqualTo(Integer.class);
    List<Integer> value = new ArrayList<>(3);
    desc = desc.elementGenericDescriptor(value);
    assertThat(desc.getElementDescriptor().getType()).isEqualTo(Integer.class);
    assertThat(desc.getAnnotation(FieldAnnotation.class)).isNotNull();
  }

  @Test
  public void mapKeyType() {
    GenericDescriptor desc = GenericDescriptor.valueOf(Map.class);
    Integer value = Integer.valueOf(3);
    desc = desc.getMapKeyGenericDescriptor(value);
    assertThat(desc.getType()).isEqualTo(Integer.class);
  }

  @Test
  public void mapKeyTypePreserveContext() throws Exception {
    GenericDescriptor desc = new GenericDescriptor(getClass().getField("mapPreserveContext"));
    assertThat(desc.getMapKeyGenericDescriptor().getElementDescriptor().getType()).isEqualTo(Integer.class);
    List<Integer> value = new ArrayList<>(3);
    desc = desc.getMapKeyGenericDescriptor(value);
    assertThat(desc.getElementDescriptor().getType()).isEqualTo(Integer.class);
    assertThat(desc.getAnnotation(FieldAnnotation.class)).isNotNull();
  }

  @Test
  public void mapValueType() {
    GenericDescriptor desc = GenericDescriptor.valueOf(Map.class);
    Integer value = Integer.valueOf(3);
    desc = desc.getMapValueGenericDescriptor(value);
    assertThat(desc.getType()).isEqualTo(Integer.class);
  }

  @Test
  public void mapValueTypePreserveContext() throws Exception {
    GenericDescriptor desc = new GenericDescriptor(getClass().getField("mapPreserveContext"));
    assertThat(desc.getMapValueGenericDescriptor().getElementDescriptor().getType()).isEqualTo(Integer.class);
    List<Integer> value = new ArrayList<>(3);
    desc = desc.getMapValueGenericDescriptor(value);
    assertThat(desc.getElementDescriptor().getType()).isEqualTo(Integer.class);
    assertThat(desc.getAnnotation(FieldAnnotation.class)).isNotNull();
  }

  @Test
  public void equality() throws Exception {
    GenericDescriptor t1 = GenericDescriptor.valueOf(String.class);
    GenericDescriptor t2 = GenericDescriptor.valueOf(String.class);
    GenericDescriptor t3 = GenericDescriptor.valueOf(Date.class);
    GenericDescriptor t4 = GenericDescriptor.valueOf(Date.class);
    GenericDescriptor t5 = GenericDescriptor.valueOf(List.class);
    GenericDescriptor t6 = GenericDescriptor.valueOf(List.class);
    GenericDescriptor t7 = GenericDescriptor.valueOf(Map.class);
    GenericDescriptor t8 = GenericDescriptor.valueOf(Map.class);
    assertThat(t2).isEqualTo(t1);
    assertThat(t4).isEqualTo(t3);
    assertThat(t6).isEqualTo(t5);
    assertThat(t8).isEqualTo(t7);

    GenericDescriptor t9 = new GenericDescriptor(getClass().getField("listField"));
    GenericDescriptor t10 = new GenericDescriptor(getClass().getField("listField"));
    assertThat(t10).isEqualTo(t9);

    GenericDescriptor t11 = new GenericDescriptor(getClass().getField("mapField"));
    GenericDescriptor t12 = new GenericDescriptor(getClass().getField("mapField"));
    assertThat(t12).isEqualTo(t11);

    final Method testAnnotatedMethod1 = getClass().getMethod("testAnnotatedMethod", String.class);

    final GenericDescriptor t13 = GenericDescriptor.ofParameter(testAnnotatedMethod1, 0);
    final GenericDescriptor t14 = GenericDescriptor.ofParameter(testAnnotatedMethod1, 0);
    assertThat(t14).isEqualTo(t13);

    final GenericDescriptor t15 = GenericDescriptor.ofParameter(testAnnotatedMethod1, 0);

    final Method testAnnotatedMethodDifferentAnnotationValue = getClass()
            .getMethod("testAnnotatedMethodDifferentAnnotationValue", String.class);
    final GenericDescriptor t16 = GenericDescriptor.ofParameter(testAnnotatedMethodDifferentAnnotationValue, 0);

    assertThat(t16).isNotEqualTo(t15);

    final GenericDescriptor t17 = GenericDescriptor.ofParameter(testAnnotatedMethod1, 0);
    final Method test5 = getClass().getMethod("test5", String.class);

    final GenericDescriptor t18 = GenericDescriptor.ofParameter(test5, 0);
    assertThat(t18).isNotEqualTo(t17);
  }

  @Test
  public void isAssignableTypes() {
    assertThat(GenericDescriptor.valueOf(Integer.class).isAssignableTo(GenericDescriptor.valueOf(Number.class))).isTrue();
    assertThat(GenericDescriptor.valueOf(Number.class).isAssignableTo(GenericDescriptor.valueOf(Integer.class))).isFalse();
    assertThat(GenericDescriptor.valueOf(String.class).isAssignableTo(GenericDescriptor.valueOf(String[].class))).isFalse();
  }

  @Test
  public void isAssignableElementTypes() throws Exception {
    assertThat(new GenericDescriptor(getClass().getField("listField")).isAssignableTo(new GenericDescriptor(getClass().getField("listField")))).isTrue();
    assertThat(new GenericDescriptor(getClass().getField("notGenericList")).isAssignableTo(new GenericDescriptor(getClass().getField("listField")))).isTrue();
    assertThat(new GenericDescriptor(getClass().getField("listField")).isAssignableTo(new GenericDescriptor(getClass().getField("notGenericList")))).isTrue();
    assertThat(new GenericDescriptor(getClass().getField("isAssignableElementTypes")).isAssignableTo(new GenericDescriptor(getClass().getField("listField")))).isFalse();
    assertThat(GenericDescriptor.valueOf(List.class).isAssignableTo(new GenericDescriptor(getClass().getField("listField")))).isTrue();
  }

  @Test
  public void isAssignableMapKeyValueTypes() throws Exception {
    assertThat(new GenericDescriptor(getClass().getField("mapField")).isAssignableTo(new GenericDescriptor(getClass().getField("mapField")))).isTrue();
    assertThat(new GenericDescriptor(getClass().getField("notGenericMap")).isAssignableTo(new GenericDescriptor(getClass().getField("mapField")))).isTrue();
    assertThat(new GenericDescriptor(getClass().getField("mapField")).isAssignableTo(new GenericDescriptor(getClass().getField("notGenericMap")))).isTrue();
    assertThat(new GenericDescriptor(getClass().getField("isAssignableMapKeyValueTypes")).isAssignableTo(new GenericDescriptor(getClass().getField("mapField")))).isFalse();
    assertThat(GenericDescriptor.valueOf(Map.class).isAssignableTo(new GenericDescriptor(getClass().getField("mapField")))).isTrue();
  }

  @Test
  public void multiValueMap() throws Exception {
    GenericDescriptor td = new GenericDescriptor(getClass().getField("multiValueMap"));
    assertThat(td.isMap()).isTrue();
    assertThat(td.getMapKeyGenericDescriptor().getType()).isEqualTo(String.class);
    assertThat(td.getMapValueGenericDescriptor().getType()).isEqualTo(List.class);
    assertThat(td.getMapValueGenericDescriptor().getElementDescriptor().getType()).isEqualTo(Integer.class);
  }

  @Test
  public void passDownGeneric() throws Exception {
    GenericDescriptor td = new GenericDescriptor(getClass().getField("passDownGeneric"));
    assertThat(td.getElementDescriptor().getType()).isEqualTo(List.class);
    assertThat(td.getElementDescriptor().getElementDescriptor().getType()).isEqualTo(Set.class);
    assertThat(td.getElementDescriptor().getElementDescriptor().getElementDescriptor().getType()).isEqualTo(Integer.class);
  }

  @Test
  public void upCast() throws Exception {
    final BeanProperty property = BeanProperty.of(getClass(), "property");

    GenericDescriptor GenericDescriptor = new GenericDescriptor(property);
    GenericDescriptor upCast = GenericDescriptor.upcast(Object.class);
    assertThat(upCast.getAnnotation(MethodAnnotation1.class) != null).isTrue();
  }

  @Test
  public void upCastNotSuper() throws Exception {
    final BeanProperty property = BeanProperty.of(getClass(), "property");

    GenericDescriptor descriptor = new GenericDescriptor(property);
    assertThatIllegalArgumentException()
            .isThrownBy(() -> descriptor.upcast(Collection.class))
            .withMessage("interface java.util.Map is not assignable to interface java.util.Collection");
  }

  @Test
  public void elementTypeForCollectionSubclass() throws Exception {
    @SuppressWarnings("serial")
    class CustomSet extends HashSet<String> {
    }

    assertThat(GenericDescriptor.valueOf(String.class)).isEqualTo(GenericDescriptor.valueOf(CustomSet.class).getElementDescriptor());
    assertThat(GenericDescriptor.valueOf(String.class)).isEqualTo(GenericDescriptor.forObject(new CustomSet()).getElementDescriptor());
  }

  @Test
  public void elementTypeForMapSubclass() throws Exception {
    @SuppressWarnings("serial")
    class CustomMap extends HashMap<String, Integer> {
    }

    assertThat(GenericDescriptor.valueOf(String.class)).isEqualTo(GenericDescriptor.valueOf(CustomMap.class).getMapKeyGenericDescriptor());
    assertThat(GenericDescriptor.valueOf(Integer.class)).isEqualTo(GenericDescriptor.valueOf(CustomMap.class).getMapValueGenericDescriptor());
    assertThat(GenericDescriptor.valueOf(String.class)).isEqualTo(GenericDescriptor.forObject(new CustomMap()).getMapKeyGenericDescriptor());
    assertThat(GenericDescriptor.valueOf(Integer.class)).isEqualTo(GenericDescriptor.forObject(new CustomMap()).getMapValueGenericDescriptor());
  }

  @Test
  public void createMapArray() throws Exception {
    GenericDescriptor mapType = GenericDescriptor.map(
            LinkedHashMap.class, GenericDescriptor.valueOf(String.class), GenericDescriptor.valueOf(Integer.class));
    GenericDescriptor arrayType = GenericDescriptor.array(mapType);
    assertThat(LinkedHashMap[].class).isEqualTo(arrayType.getType());
    assertThat(mapType).isEqualTo(arrayType.getElementDescriptor());
  }

  @Test
  public void createStringArray() throws Exception {
    GenericDescriptor arrayType = GenericDescriptor.array(GenericDescriptor.valueOf(String.class));
    assertThat(GenericDescriptor.valueOf(String[].class)).isEqualTo(arrayType);
  }

  @Test
  public void createNullArray() throws Exception {
    assertThat((Object) GenericDescriptor.array(null)).isNull();
  }

  @Test
  public void serializable() throws Exception {
    GenericDescriptor descriptor = GenericDescriptor.forObject("");
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ObjectOutputStream outputStream = new ObjectOutputStream(out);
    outputStream.writeObject(descriptor);
    ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(
            out.toByteArray()));
    GenericDescriptor readObject = (GenericDescriptor) inputStream.readObject();
    assertThat(readObject).isEqualTo(descriptor);
  }

  @Test
  public void createCollectionWithNullElement() throws Exception {
    GenericDescriptor descriptor = GenericDescriptor.collection(List.class, (GenericDescriptor) null);
    assertThat(descriptor.getElementDescriptor()).isNull();
  }

  @Test
  public void createMapWithNullElements() throws Exception {
    GenericDescriptor descriptor = GenericDescriptor.map(LinkedHashMap.class, (GenericDescriptor) null, null);
    assertThat(descriptor.getMapKeyGenericDescriptor()).isNull();
    assertThat(descriptor.getMapValueGenericDescriptor()).isNull();
  }

  @Test
  public void getSource() throws Exception {
    Field field = getClass().getField("fieldScalar");

    final Method testParameterPrimitive = getClass().getMethod("testParameterPrimitive", int.class);
    final Parameter[] parameters = testParameterPrimitive.getParameters();
    final Parameter parameter = parameters[0];

    final GenericDescriptor descriptor = GenericDescriptor.ofParameter(testParameterPrimitive, 0);

    assertThat(new GenericDescriptor(field).getSource()).isEqualTo(field);

    assertThat(descriptor.getSource()).isEqualTo(parameter);

    assertThat(GenericDescriptor.valueOf(Integer.class).getSource()).isEqualTo(Integer.class);
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

  public MultiValueMap<String, Integer> multiValueMap = new DefaultMultiValueMap<>();

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

    public void setProperty(T t);

    List<T> getListProperty();

    public void setListProperty(List<T> t);
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


  // Annotations used on tested elements

  @Target({ElementType.PARAMETER})
  @Retention(RetentionPolicy.RUNTIME)
  public @interface ParameterAnnotation {

    int value();
  }


  @Target({ ElementType.FIELD})
  @Retention(RetentionPolicy.RUNTIME)
  public @interface FieldAnnotation {
  }


  @Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  public @interface MethodAnnotation1 {
  }


  @Target({ElementType.METHOD})
  @Retention(RetentionPolicy.RUNTIME)
  public @interface MethodAnnotation2 {
  }


  @Target({ElementType.FIELD})
  @Retention(RetentionPolicy.RUNTIME)
  public @interface MethodAnnotation3 {
  }


  @MethodAnnotation1
  @Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  public @interface ComposedMethodAnnotation1 {
  }


  @ComposedMethodAnnotation1
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  public @interface ComposedComposedMethodAnnotation1 {
  }

}
