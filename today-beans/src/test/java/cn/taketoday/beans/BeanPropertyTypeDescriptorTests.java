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

package cn.taketoday.beans;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.reflect.PropertyAccessor;
import cn.taketoday.util.DefaultMultiValueMap;
import cn.taketoday.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/7/23 00:18
 */
class BeanPropertyTypeDescriptorTests {

  @Test
  public void propertyComplex() throws Exception {
    BeanProperty complexProperty = BeanProperty.valueOf(getClass(), "complexProperty");
    TypeDescriptor desc = complexProperty.getTypeDescriptor();

    assertThat(desc.getMapKeyDescriptor().getType()).isEqualTo(String.class);
    assertThat(desc.getMapValueDescriptor().getElementDescriptor()
            .getElementDescriptor().getType())
            .isEqualTo(Integer.class);
  }

  @Test
  void propertyGenericType() throws Exception {
    GenericType<Integer> genericBean = new IntegerType();
    BeanProperty property = BeanProperty.valueOf(
            genericBean.getClass().getMethod("getProperty"),
            genericBean.getClass().getMethod("setProperty", Integer.class));
    TypeDescriptor desc = property.getTypeDescriptor();
    assertThat(desc.getType()).isEqualTo(Integer.class);
  }

  @Test
  void propertyTypeCovariance() throws Exception {
    GenericType<Number> genericBean = new NumberType();
    BeanProperty property = BeanProperty.valueOf(
            genericBean.getClass().getMethod("getProperty"),
            genericBean.getClass().getMethod("setProperty", Number.class));
    TypeDescriptor desc = property.getTypeDescriptor();
    assertThat(desc.getType()).isEqualTo(Integer.class);
  }

  @Test
  void propertyGenericTypeList() throws Exception {
    GenericType<Integer> genericBean = new IntegerType();
    BeanProperty property = BeanProperty.valueOf(
            genericBean.getClass().getMethod("getListProperty"),
            genericBean.getClass().getMethod("setListProperty", List.class));
    TypeDescriptor desc = property.getTypeDescriptor();
    assertThat(desc.getType()).isEqualTo(List.class);
    assertThat(desc.getElementDescriptor().getType()).isEqualTo(Integer.class);
  }

  @Test
  void propertyGenericClassList() throws Exception {
    IntegerClass genericBean = new IntegerClass();
    BeanProperty property = BeanProperty.valueOf(
            genericBean.getClass().getMethod("getListProperty"),
            genericBean.getClass().getMethod("setListProperty", List.class), IntegerClass.class);
    TypeDescriptor desc = property.getTypeDescriptor();
    assertThat(desc.getType()).isEqualTo(List.class);
    assertThat(desc.getElementDescriptor().getType()).isEqualTo(Integer.class);
    assertThat(desc.getAnnotation(MethodAnnotation1.class)).isNotNull();
    assertThat(desc.hasAnnotation(MethodAnnotation1.class)).isTrue();
  }

  @Test
  public void property() throws Exception {
    BeanProperty property = BeanProperty.valueOf(getClass(), "property");

    TypeDescriptor desc = property.getTypeDescriptor();

    assertThat(desc.getType()).isEqualTo(Map.class);

    assertThat(desc.getMapKeyDescriptor().getElementDescriptor().getType()).isEqualTo(Integer.class);
    assertThat(desc.getMapValueDescriptor().getElementDescriptor().getType()).isEqualTo(Long.class);

    PropertyAccessor propertyAccessor = property.obtainAccessor();
    Method writeMethod = propertyAccessor.getWriteMethod();
    Method readMethod = propertyAccessor.getReadMethod();

    assertThat(writeMethod).isNotNull();
    assertThat(readMethod).isNotNull();

    assertThat(readMethod.getAnnotation(MethodAnnotation1.class)).isNotNull();

    assertThat(writeMethod.getAnnotation(MethodAnnotation2.class)).isNotNull();
    assertThat(desc.getAnnotation(MethodAnnotation3.class)).isNotNull();
  }

  @Test
  public void nestedPropertyTypeMapTwoLevels() throws Exception {

    final BeanProperty property = BeanProperty.valueOf(getClass(), "test4");
    TypeDescriptor t1 = TypeDescriptor.nested(property.getField(), 2);

    assertThat(t1.getType()).isEqualTo(String.class);
  }

  @Test
  public void upCast() throws Exception {
    final BeanProperty property = BeanProperty.valueOf(getClass(), "property");

    TypeDescriptor typeDescriptor = property.getTypeDescriptor();
    TypeDescriptor upCast = typeDescriptor.upcast(Object.class);
    assertThat(upCast.getAnnotation(MethodAnnotation3.class) != null).isTrue();
  }

  @Test
  public void upCastNotSuper() throws Exception {
    final BeanProperty property = BeanProperty.valueOf(getClass(), "property");

    TypeDescriptor descriptor = property.getTypeDescriptor();
    assertThatIllegalArgumentException()
            .isThrownBy(() -> descriptor.upcast(Collection.class))
            .withMessage("interface java.util.Map is not assignable to interface java.util.Collection");
  }

  //

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

  @MethodAnnotation1
  public Map<List<Integer>, List<Long>> getProperty() {
    return property;
  }

  @MethodAnnotation2
  public void setProperty(Map<List<Integer>, List<Long>> property) {
    this.property = property;
  }

  public Map<String, List<List<Integer>>> getComplexProperty() {
    return complexProperty;
  }

  Map<String, List<List<Integer>>> complexProperty;

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
