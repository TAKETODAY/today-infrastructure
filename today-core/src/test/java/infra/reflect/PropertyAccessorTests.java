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

import org.assertj.core.api.WithAssertions;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

import infra.util.ObjectUtils;

import static infra.reflect.ReadOnlyPropertyAccessor.classToString;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/4/16 14:07
 */
class PropertyAccessorTests implements WithAssertions {

  @Nested
  class PublicProperty {

    @Test
    void publicAccessor() throws Exception {
      PropertyAccessor name = PropertyAccessor.forField(BeanTest.class.getDeclaredField("name"));

      BeanTest beanTest = new BeanTest();
      name.set(beanTest, "name");

      assertThat(beanTest.name).isEqualTo("name");
      assertThat(beanTest.name).isEqualTo(name.get(beanTest));
    }

    @Test
    void primitiveBean() throws Exception {
      primitiveBeanTest("intValue", 31234);
      primitiveBeanTest("longValue", 41L);
      primitiveBeanTest("byteValue", (byte) 11);
      primitiveBeanTest("shortValue", (short) 31);
      primitiveBeanTest("booleanValue", true);
      primitiveBeanTest("booleanValue", false);
      primitiveBeanTest("doubleValue", 1143.89);
      primitiveBeanTest("floatValue", 112.4f);
      primitiveBeanTest("charValue", 'c');

      primitiveBeanTest("floatValue", null, 0.f);
      primitiveBeanTest("doubleValue", null, 0.d);
      primitiveBeanTest("intValue", null, 0);
      primitiveBeanTest("longValue", null, 0L);
      primitiveBeanTest("byteValue", null, (byte) 0);
      primitiveBeanTest("shortValue", null, (short) 0);
      primitiveBeanTest("booleanValue", null, false);
    }

    @Test
    void readOnlyBean() throws Exception {
      readOnlyTest("byteValue", (byte) 1);
      readOnlyTest("shortValue", (short) 2);
      readOnlyTest("intValue", 3);
      readOnlyTest("longValue", 4L);
      readOnlyTest("booleanValue", true);
      readOnlyTest("doubleValue", 6.0);
      readOnlyTest("floatValue", 7f);
      readOnlyTest("charValue", '8');
      readOnlyTest("stringValue", "9");
    }

    <T> void readOnlyTest(String name, T actual) throws Exception {
      ReadOnlyBean bean = new ReadOnlyBean();

      PropertyAccessor accessor = PropertyAccessor.forField(ReadOnlyBean.class.getDeclaredField(name));
      assertThatThrownBy(() -> accessor.set(bean, actual))
              .isInstanceOf(ReflectionException.class)
              .hasMessage("Can't set value '%s' to '%s' read only property".formatted(ObjectUtils.toHexString(actual), classToString(bean)));

      assertThat(actual).isEqualTo(accessor.get(bean));
    }

    <T> void primitiveBeanTest(String name, @Nullable T actual) throws Exception {
      primitiveBeanTest(name, actual, actual);
    }

    <T> void primitiveBeanTest(String name, @Nullable T actual, @Nullable Object expected) throws Exception {
      PrimitiveBean bean = new PrimitiveBean();

      PropertyAccessor accessor = PropertyAccessor.forField(PrimitiveBean.class.getDeclaredField(name));
      accessor.set(bean, actual);
      assertThat(expected).isEqualTo(accessor.get(bean));
    }
  }

  @Test
  void forMethod() throws NoSuchMethodException {
    Method getAgeMethod = ForMethod.class.getDeclaredMethod("getAge");
    Method setAgeMethod = ForMethod.class.getDeclaredMethod("setAge", int.class);
    GetterMethod getAge = GetterMethod.forMethod(getAgeMethod);
    SetterMethod setAge = SetterMethod.forMethod(setAgeMethod);

    PropertyAccessor age = PropertyAccessor.forMethod(getAge, setAge);
    assertThat(age).isInstanceOf(GetterSetterPropertyAccessor.class);

    ForMethod obj = new ForMethod();
    assertThat(age.get(obj)).isEqualTo(0);

    age.set(obj, 1);
    assertThat(age.get(obj)).isEqualTo(1);

    assertThat(age.getReadMethod()).isSameAs(getAge.getReadMethod()).isEqualTo(getAgeMethod);
    assertThat(age.getWriteMethod()).isSameAs(setAge.getWriteMethod()).isEqualTo(setAgeMethod);

    Method getNameMethod = ForMethod.class.getDeclaredMethod("getName");
    GetterMethod getName = GetterMethod.forMethod(getNameMethod);

    PropertyAccessor name = PropertyAccessor.forMethod(getName, null);
    assertThat(name).isInstanceOf(ReadOnlyGetterMethodPropertyAccessor.class);

    assertThat(name.get(obj)).isEqualTo("name");
    assertThat(name.getReadMethod()).isSameAs(getName.getReadMethod()).isEqualTo(getNameMethod);

    assertThatThrownBy(() -> name.set(obj, "name1"))
            .isInstanceOf(ReflectionException.class)
            .hasMessage("Can't set value '%s' to '%s' read only property"
                    .formatted(ObjectUtils.toHexString("name1"), classToString(obj)));

  }

  @Test
  void forReflective() throws Exception {
    ForReflective obj = new ForReflective();
    Field intValue = ForReflective.class.getField("intValue");
    Method setIntValue = ForReflective.class.getMethod("setIntValue", IntValue.class);
    Field privateValue = ForReflective.class.getDeclaredField("privateValue");
    Method getPrivateValue = ForReflective.class.getMethod("getPrivateValue");
    Method setPrivateValue = ForReflective.class.getMethod("setPrivateValue", IntValue.class);

    assertThatThrownBy(() -> PropertyAccessor.forReflective(intValue).set(obj, null))
            .isInstanceOf(ReflectionException.class)
            .hasMessage("Can't set value '%s' to '%s' read only property"
                    .formatted("null", classToString(obj)));

    PropertyAccessor intValueAccessor = PropertyAccessor.forReflective(intValue, null, setIntValue);
    intValueAccessor.set(obj, new IntValue(1));
    assertThat(intValueAccessor.get(obj)).isEqualTo(new IntValue(1));
    assertThat(intValueAccessor.isWriteable()).isTrue();
    assertThat(intValueAccessor.getWriteMethod()).isEqualTo(setIntValue);
    assertThat(intValueAccessor.getReadMethod()).isNull();

    assertThatThrownBy(() -> intValueAccessor.set(obj, 1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("argument type mismatch");

    var privateValueAccessor = PropertyAccessor.forReflective(privateValue, getPrivateValue, setPrivateValue);

    privateValueAccessor.set(obj, new IntValue(2));
    assertThat(privateValueAccessor.get(obj)).isEqualTo(new IntValue(2));
    assertThat(privateValueAccessor.isWriteable()).isTrue();

    privateValueAccessor = PropertyAccessor.forReflective(privateValue);
    privateValueAccessor.set(obj, null);

    assertThat(privateValueAccessor.get(obj)).isNull();
    assertThat(privateValueAccessor.isWriteable()).isTrue();

    assertThatThrownBy(() -> PropertyAccessor.forReflective(null, getPrivateValue, null).set(obj, null))
            .isInstanceOf(ReflectionException.class)
            .hasMessage("Can't set value '%s' to '%s' read only property"
                    .formatted("null", classToString(obj)));

    assertThatThrownBy(() -> new ReflectivePropertyAccessor(null, getPrivateValue, null).set(obj, null))
            .isInstanceOf(ReflectionException.class)
            .hasMessage("Can't set value '%s' to '%s' read only property"
                    .formatted("null", classToString(obj)));

    assertThatThrownBy(() -> new ReflectivePropertyAccessor(null, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("field or read method must be specified one");

    assertThat(new ReflectivePropertyAccessor(null, getPrivateValue, null).isWriteable()).isFalse();
    assertThat(new ReflectivePropertyAccessor(privateValue, getPrivateValue, null).isWriteable()).isTrue();
    assertThat(new ReflectivePropertyAccessor(null, getPrivateValue, setPrivateValue).isWriteable()).isTrue();

    assertThatThrownBy(() -> new ReflectiveReadOnlyPropertyAccessor(null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("field or read method must be specified one");

    assertThat(new ReflectiveReadOnlyPropertyAccessor(privateValue, getPrivateValue).isWriteable()).isFalse();
    assertThat(new ReflectiveReadOnlyPropertyAccessor(privateValue, getPrivateValue).getReadMethod()).isEqualTo(getPrivateValue);
    assertThat(new ReflectiveReadOnlyPropertyAccessor(privateValue, getPrivateValue).get(new ForReflective())).isNull();
    assertThat(new ReflectiveReadOnlyPropertyAccessor(null, getPrivateValue).get(new ForReflective())).isNull();
    assertThat(new ReflectiveReadOnlyPropertyAccessor(privateValue, null).get(new ForReflective())).isNull();
    assertThat(new ReflectiveReadOnlyPropertyAccessor(privateValue, null).getReadMethod()).isNull();

  }

  @Test
  void forField() throws Exception {
    PublicFinalBean obj = new PublicFinalBean();

    Field intValue = PublicFinalBean.class.getField("intValue");
    Method setIntValue = PublicFinalBean.class.getMethod("setIntValue", IntValue.class);

    var intValueAccessor = PropertyAccessor.forField(intValue, null, setIntValue);
    assertThat(intValueAccessor.get(obj)).isEqualTo(new IntValue());
    intValueAccessor.set(obj, new IntValue(1));
    assertThat(intValueAccessor.get(obj)).isEqualTo(new IntValue(1));

    assertThatThrownBy(() -> PropertyAccessor.forField(ForReflective.class.getField("intValue"),
            null, null).set(obj, null))
            .isInstanceOf(ReflectionException.class)
            .hasMessage("Can't set value '%s' to '%s' read only property"
                    .formatted("null", classToString(obj)));
  }

  @Test
  void shouldCreatePropertyAccessorFromClassNameAndPropertyName() throws NoSuchFieldException {
    PropertyAccessor accessor = PropertyAccessor.from(BeanTest.class, "name");

    assertThat(accessor).isNotNull();
    BeanTest bean = new BeanTest();
    accessor.set(bean, "testName");
    assertThat(accessor.get(bean)).isEqualTo("testName");
  }

  @Test
  void shouldThrowExceptionWhenPropertyNotFound() {
    assertThatThrownBy(() -> PropertyAccessor.from(BeanTest.class, "nonExistent"))
            .isInstanceOf(ReflectionException.class)
            .hasMessageContaining("No such property");
  }

  @Test
  void shouldCreatePropertyAccessorForMethodWithGetterAndSetter() throws NoSuchMethodException {
    Method getAge = ForMethod.class.getDeclaredMethod("getAge");
    Method setAge = ForMethod.class.getDeclaredMethod("setAge", int.class);

    PropertyAccessor accessor = PropertyAccessor.forMethod(getAge, setAge);

    assertThat(accessor).isNotNull();
    ForMethod obj = new ForMethod();
    accessor.set(obj, 25);
    assertThat(accessor.get(obj)).isEqualTo(25);
  }

  @Test
  void shouldCreateReadOnlyPropertyAccessorForMethodWithOnlyGetter() throws NoSuchMethodException {
    Method getName = ForMethod.class.getDeclaredMethod("getName");

    PropertyAccessor accessor = PropertyAccessor.forMethod(GetterMethod.forMethod(getName), null);

    assertThat(accessor).isNotNull();
    ForMethod obj = new ForMethod();
    assertThat(accessor.get(obj)).isEqualTo("name");
    assertThat(accessor.isWriteable()).isFalse();
  }

  @Test
  void shouldThrowExceptionWhenBothMethodsAreNull() {
    assertThatThrownBy(() -> PropertyAccessor.forMethod((Method) null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("read-write cannot be null at the same time");
  }

  @Test
  void shouldCreatePropertyAccessorForField() throws NoSuchFieldException {
    Field nameField = BeanTest.class.getDeclaredField("name");

    PropertyAccessor accessor = PropertyAccessor.forField(nameField);

    assertThat(accessor).isNotNull();
    BeanTest bean = new BeanTest();
    accessor.set(bean, "fieldValue");
    assertThat(accessor.get(bean)).isEqualTo("fieldValue");
  }

  @Test
  void shouldCreatePropertyAccessorForPublicFinalField() throws NoSuchFieldException, NoSuchMethodException {
    Field intValueField = PublicFinalBean.class.getField("intValue");
    Method setIntValue = PublicFinalBean.class.getMethod("setIntValue", IntValue.class);

    PropertyAccessor accessor = PropertyAccessor.forField(intValueField, null, setIntValue);

    assertThat(accessor).isNotNull();
    PublicFinalBean bean = new PublicFinalBean();
    IntValue newValue = new IntValue(42);
    accessor.set(bean, newValue);
    assertThat(accessor.get(bean)).isEqualTo(newValue);
  }

  @Test
  void shouldCreateReflectivePropertyAccessor() throws NoSuchFieldException {
    Field privateValueField = ForReflective.class.getDeclaredField("privateValue");

    PropertyAccessor accessor = PropertyAccessor.forReflective(privateValueField);

    assertThat(accessor).isNotNull();
    ForReflective obj = new ForReflective();
    IntValue value = new IntValue(10);
    accessor.set(obj, value);
    assertThat(accessor.get(obj)).isEqualTo(value);
  }

  @Test
  void shouldCreateReflectivePropertyAccessorWithMethods() throws NoSuchFieldException, NoSuchMethodException {
    Field privateValueField = ForReflective.class.getDeclaredField("privateValue");
    Method getPrivateValue = ForReflective.class.getMethod("getPrivateValue");
    Method setPrivateValue = ForReflective.class.getMethod("setPrivateValue", IntValue.class);

    PropertyAccessor accessor = PropertyAccessor.forReflective(privateValueField, getPrivateValue, setPrivateValue);

    assertThat(accessor).isNotNull();
    ForReflective obj = new ForReflective();
    IntValue value = new IntValue(20);
    accessor.set(obj, value);
    assertThat(accessor.get(obj)).isEqualTo(value);
  }

  @Test
  void shouldHandleReadOnlyReflectiveProperty() throws NoSuchFieldException, NoSuchMethodException {
    Field intValueField = ForReflective.class.getField("intValue");

    PropertyAccessor accessor = PropertyAccessor.forReflective(intValueField, null, null);

    assertThat(accessor).isNotNull();
    ForReflective obj = new ForReflective();
    assertThat(accessor.get(obj)).isNotNull();
    assertThat(accessor.isWriteable()).isFalse();
  }

  @Test
  void shouldCreatePropertyAccessorForMethodWithGetterMethodAndSetterMethod() throws NoSuchMethodException {
    Method getName = ForMethod.class.getDeclaredMethod("getName");
    Method setAge = ForMethod.class.getDeclaredMethod("setAge", int.class);
    GetterMethod getter = GetterMethod.forMethod(getName);
    SetterMethod setter = SetterMethod.forMethod(setAge);

    PropertyAccessor accessor = PropertyAccessor.forMethod(getter, setter);

    assertThat(accessor).isNotNull();
    ForMethod obj = new ForMethod();
    accessor.set(obj, 30);
    // getter is still reading the original name field
    assertThat(accessor.get(obj)).isEqualTo("name");
  }

  static class ForMethod {

    private int age;

    private final String name = "name";

    public String getName() {
      return name;
    }

    public void setAge(int age) {
      this.age = age;
    }

    public int getAge() {
      return age;
    }
  }

  static class BeanTest {
    public String name;
  }

  static class PrimitiveBean {
    public byte byteValue;

    public short shortValue;

    public int intValue;

    public long longValue;

    public boolean booleanValue;

    public double doubleValue;

    public float floatValue;

    public char charValue;
  }

  static class ReadOnlyBean {
    public final byte byteValue = 1;

    public final short shortValue = 2;

    public final int intValue = 3;

    public final long longValue = 4;

    public final boolean booleanValue = true;

    public final double doubleValue = 6;

    public final float floatValue = 7;

    public final char charValue = '8';

    public final String stringValue = "9";

  }

  static class PublicFinalBean {

    public final IntValue intValue = new IntValue();

    public void setIntValue(IntValue in) {
      intValue.set(in.value);
    }

    public void setIntValue(int value) {
      intValue.set(value);
    }

  }

  static class ForReflective {

    @Nullable
    private IntValue privateValue;

    public final IntValue intValue = new IntValue();

    public void setIntValue(IntValue in) {
      intValue.set(in.value);
    }

    public void setIntValue(int value) {
      intValue.set(value);
    }

    @Nullable
    public IntValue getPrivateValue() {
      return privateValue;
    }

    public void setPrivateValue(@Nullable IntValue privateValue) {
      this.privateValue = privateValue;
    }

  }

  static class IntValue {

    private int value;

    public IntValue() {

    }

    public IntValue(int value) {
      this.value = value;
    }

    public void set(int value) {
      this.value = value;
    }

    public int get() {
      return value;
    }

    @Override
    public boolean equals(Object param) {
      if (this == param)
        return true;
      if (!(param instanceof IntValue intValue))
        return false;
      return value == intValue.value;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(value);
    }
  }

}