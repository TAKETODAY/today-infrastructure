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

package infra.beans;

import org.assertj.core.api.SoftAssertions;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import infra.beans.factory.BeanFactory;
import infra.beans.propertyeditors.CustomDateEditor;
import infra.beans.testfixture.beans.DerivedTestBean;
import infra.beans.testfixture.beans.ITestBean;
import infra.beans.testfixture.beans.TestBean;
import infra.bytecode.proxy.Enhancer;
import infra.bytecode.proxy.MethodInterceptor;
import infra.core.ConstructorNotFoundException;
import infra.core.io.Resource;
import infra.core.io.ResourceEditor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link BeanUtils}.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Chris Beams
 * @author Sebastien Deleuze
 * @author Sam Brannen
 * @since 19.05.2003
 */
class BeanUtilsTests {

  @Test
  void newInstanceGivenInterface() {
    assertThatExceptionOfType(ConstructorNotFoundException.class)
            .isThrownBy(() -> BeanUtils.newInstance(List.class));
  }

  @Test
  void newInstanceGivenClassWithoutDefaultConstructor() {
    assertThatExceptionOfType(ConstructorNotFoundException.class)
            .isThrownBy(() -> BeanUtils.newInstance(CustomDateEditor.class));
  }

  @Test
    // gh-22531
  void newInstanceWithOptionalNullableType() throws NoSuchMethodException {
    Constructor<BeanWithNullableTypes> ctor = BeanWithNullableTypes.class.getDeclaredConstructor(
            Integer.class, Boolean.class, String.class);
    BeanWithNullableTypes bean = BeanUtils.newInstance(ctor, new Object[] { null, null, "foo" });
    assertThat(bean.getCounter()).isNull();
    assertThat(bean.isFlag()).isNull();
    assertThat(bean.getValue()).isEqualTo("foo");
  }

  @Test
    // gh-22531
  void newInstanceWithFewerArgsThanParameters() throws NoSuchMethodException {
    Constructor<BeanWithPrimitiveTypes> constructor = getBeanWithPrimitiveTypesConstructor();

    assertThatExceptionOfType(BeanInstantiationException.class).isThrownBy(() ->
            BeanUtils.newInstance(constructor, new Object[] { null, null, "foo" }));
  }

  @Test
    // gh-22531
  void newInstanceWithMoreArgsThanParameters() throws NoSuchMethodException {
    Constructor<BeanWithPrimitiveTypes> constructor = getBeanWithPrimitiveTypesConstructor();

    assertThatExceptionOfType(BeanInstantiationException.class).isThrownBy(() ->
            BeanUtils.newInstance(constructor, new Object[] { null, null, null, null, null, null, null, null, "foo", null }));
  }

  @Test
  @Disabled
    // gh-22531, gh-27390
  void newInstanceWithOptionalPrimitiveTypes() throws NoSuchMethodException {
    Constructor<BeanWithPrimitiveTypes> constructor = getBeanWithPrimitiveTypesConstructor();

    BeanWithPrimitiveTypes bean = BeanUtils.newInstance(constructor,
            null, null, null, null, null, null, null, null, "foo");

    SoftAssertions.assertSoftly(softly -> {
      softly.assertThat(bean.isFlag()).isFalse();
      softly.assertThat(bean.getByteCount()).isEqualTo((byte) 0);
      softly.assertThat(bean.getShortCount()).isEqualTo((short) 0);
      softly.assertThat(bean.getIntCount()).isEqualTo(0);
      softly.assertThat(bean.getLongCount()).isEqualTo(0L);
      softly.assertThat(bean.getFloatCount()).isEqualTo(0F);
      softly.assertThat(bean.getDoubleCount()).isEqualTo(0D);
      softly.assertThat(bean.getCharacter()).isEqualTo('\0');
      softly.assertThat(bean.getText()).isEqualTo("foo");
    });
  }

  private Constructor<BeanWithPrimitiveTypes> getBeanWithPrimitiveTypesConstructor() throws NoSuchMethodException {
    return BeanWithPrimitiveTypes.class.getConstructor(boolean.class, byte.class, short.class, int.class,
            long.class, float.class, double.class, char.class, String.class);
  }

  @Test
  void instantiatePrivateClassWithPrivateConstructor() throws NoSuchMethodException {
    Constructor<PrivateBeanWithPrivateConstructor> ctor = PrivateBeanWithPrivateConstructor.class.getDeclaredConstructor();
    BeanUtils.newInstance(ctor);
  }

  @Test
  void getPropertyDescriptors() throws Exception {
    PropertyDescriptor[] actual = Introspector.getBeanInfo(TestBean.class).getPropertyDescriptors();
    PropertyDescriptor[] descriptors = BeanUtils.getPropertyDescriptors(TestBean.class);
    assertThat(descriptors).as("Descriptors should not be null").isNotNull();
    assertThat(descriptors.length).as("Invalid number of descriptors returned").isEqualTo(actual.length);
  }

  @Test
  void beanPropertyIsArray() {
    PropertyDescriptor[] descriptors = BeanUtils.getPropertyDescriptors(ContainerBean.class);
    for (PropertyDescriptor descriptor : descriptors) {
      if ("containedBeans".equals(descriptor.getName())) {
        assertThat(descriptor.getPropertyType().isArray()).as("Property should be an array").isTrue();
        assertThat(ContainedBean.class).isEqualTo(descriptor.getPropertyType().getComponentType());
      }
    }
  }

  @Test
  void findEditorByConvention() {
    assertThat(BeanUtils.findEditorByConvention(Resource.class).getClass()).isEqualTo(ResourceEditor.class);
  }

  @Test
  void copyProperties() throws Exception {
    TestBean tb = new TestBean();
    tb.setName("rod");
    tb.setAge(32);
    tb.setTouchy("touchy");
    TestBean tb2 = new TestBean();
    assertThat(tb2.getName() == null).as("Name empty").isTrue();
    assertThat(tb2.getAge() == 0).as("Age empty").isTrue();
    assertThat(tb2.getTouchy() == null).as("Touchy empty").isTrue();
    BeanUtils.copyProperties(tb, tb2);
    assertThat(tb2.getName().equals(tb.getName())).as("Name copied").isTrue();
    assertThat(tb2.getAge() == tb.getAge()).as("Age copied").isTrue();
    assertThat(tb2.getTouchy().equals(tb.getTouchy())).as("Touchy copied").isTrue();
  }

  @Test
  void copyPropertiesWithDifferentTypes1() throws Exception {
    DerivedTestBean tb = new DerivedTestBean();
    tb.setName("rod");
    tb.setAge(32);
    tb.setTouchy("touchy");
    TestBean tb2 = new TestBean();
    assertThat(tb2.getName() == null).as("Name empty").isTrue();
    assertThat(tb2.getAge() == 0).as("Age empty").isTrue();
    assertThat(tb2.getTouchy() == null).as("Touchy empty").isTrue();
    BeanUtils.copyProperties(tb, tb2);
    assertThat(tb2.getName().equals(tb.getName())).as("Name copied").isTrue();
    assertThat(tb2.getAge() == tb.getAge()).as("Age copied").isTrue();
    assertThat(tb2.getTouchy().equals(tb.getTouchy())).as("Touchy copied").isTrue();
  }

  @Test
  void copyPropertiesWithDifferentTypes2() throws Exception {
    TestBean tb = new TestBean();
    tb.setName("rod");
    tb.setAge(32);
    tb.setTouchy("touchy");
    DerivedTestBean tb2 = new DerivedTestBean();
    assertThat(tb2.getName() == null).as("Name empty").isTrue();
    assertThat(tb2.getAge() == 0).as("Age empty").isTrue();
    assertThat(tb2.getTouchy() == null).as("Touchy empty").isTrue();
    BeanUtils.copyProperties(tb, tb2);
    assertThat(tb2.getName().equals(tb.getName())).as("Name copied").isTrue();
    assertThat(tb2.getAge() == tb.getAge()).as("Age copied").isTrue();
    assertThat(tb2.getTouchy().equals(tb.getTouchy())).as("Touchy copied").isTrue();
  }

  @Test
  void copyPropertiesHonorsGenericTypeMatches() {
    IntegerListHolder1 integerListHolder1 = new IntegerListHolder1();
    integerListHolder1.getList().add(42);
    IntegerListHolder2 integerListHolder2 = new IntegerListHolder2();

    BeanUtils.copyProperties(integerListHolder1, integerListHolder2);
    assertThat(integerListHolder1.getList()).containsOnly(42);
    assertThat(integerListHolder2.getList()).containsOnly(42);
  }

  @Test
  void copyPropertiesDoesNotHonorGenericTypeMismatches() {
    IntegerListHolder1 integerListHolder = new IntegerListHolder1();
    integerListHolder.getList().add(42);
    LongListHolder longListHolder = new LongListHolder();

    BeanUtils.copyProperties(integerListHolder, longListHolder);
    assertThat(integerListHolder.getList()).containsOnly(42);
    assertThat(longListHolder.getList()).isEmpty();
  }

  @Test
  void copyPropertiesIgnoresGenericsIfSourceOrTargetHasUnresolvableGenerics() throws Exception {
    Order original = new Order("test", List.of("foo", "bar"));

    // Create a Proxy that loses the generic type information for the getLineItems() method.
    OrderSummary proxy = (OrderSummary) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[] { OrderSummary.class }, new OrderInvocationHandler(original));
    assertThat(OrderSummary.class.getDeclaredMethod("getLineItems").toGenericString())
            .contains("java.util.List<java.lang.String>");
    assertThat(proxy.getClass().getDeclaredMethod("getLineItems").toGenericString())
            .contains("java.util.List")
            .doesNotContain("<java.lang.String>");

    // Ensure that our custom Proxy works as expected.
    assertThat(proxy.getId()).isEqualTo("test");
    assertThat(proxy.getLineItems()).containsExactly("foo", "bar");

    // Copy from proxy to target.
    Order target = new Order();
    BeanUtils.copyProperties(proxy, target);
    assertThat(target.getId()).isEqualTo("test");
    assertThat(target.getLineItems()).containsExactly("foo", "bar");
  }

  @Test
  public void copyPropertiesWithGenericCglibClass() {
    Enhancer enhancer = new Enhancer();
    enhancer.setSuperclass(User.class);
    enhancer.setCallback((MethodInterceptor) (obj, method, args, proxy) -> proxy.invokeSuper(obj, args));
    User user = (User) enhancer.create();
    user.setId(1);
    user.setName("proxy");
    user.setAddress("addr");

    User target = new User();
    BeanUtils.copyProperties(user, target);
    assertThat(target.getId()).isEqualTo(user.getId());
    assertThat(target.getName()).isEqualTo(user.getName());
    assertThat(target.getAddress()).isEqualTo(user.getAddress());
  }

  @Test
  void copyPropertiesWithEditable() throws Exception {
    TestBean tb = new TestBean();
    assertThat(tb.getName() == null).as("Name empty").isTrue();
    tb.setAge(32);
    tb.setTouchy("bla");
    TestBean tb2 = new TestBean();
    tb2.setName("rod");
    assertThat(tb2.getAge() == 0).as("Age empty").isTrue();
    assertThat(tb2.getTouchy() == null).as("Touchy empty").isTrue();

    // "touchy" should not be copied: it's not defined in ITestBean
    BeanUtils.copyProperties(tb, tb2, ITestBean.class);
    assertThat(tb2.getName() == null).as("Name copied").isTrue();
    assertThat(tb2.getAge() == 32).as("Age copied").isTrue();
    assertThat(tb2.getTouchy() == null).as("Touchy still empty").isTrue();
  }

  @Test
  void copyPropertiesWithIgnore() throws Exception {
    TestBean tb = new TestBean();
    assertThat(tb.getName() == null).as("Name empty").isTrue();
    tb.setAge(32);
    tb.setTouchy("bla");
    TestBean tb2 = new TestBean();
    tb2.setName("rod");
    assertThat(tb2.getAge() == 0).as("Age empty").isTrue();
    assertThat(tb2.getTouchy() == null).as("Touchy empty").isTrue();

    // "spouse", "touchy", "age" should not be copied
    BeanUtils.copyProperties(tb, tb2, "spouse", "touchy", "age");
    assertThat(tb2.getName() == null).as("Name copied").isTrue();
    assertThat(tb2.getAge() == 0).as("Age still empty").isTrue();
    assertThat(tb2.getTouchy() == null).as("Touchy still empty").isTrue();
  }

  @Test
  void copyPropertiesWithIgnoredNonExistingProperty() {
    NameAndSpecialProperty source = new NameAndSpecialProperty();
    source.setName("name");
    TestBean target = new TestBean();
    BeanUtils.copyProperties(source, target, "specialProperty");
    assertThat("name").isEqualTo(target.getName());
  }

  @Test
  void copyPropertiesWithInvalidProperty() {
    InvalidProperty source = new InvalidProperty();
    source.setName("name");
    source.setFlag1(true);
    source.setFlag2(true);
    InvalidProperty target = new InvalidProperty();
    BeanUtils.copyProperties(source, target);
    assertThat(target.getName()).isEqualTo("name");
    assertThat((boolean) target.getFlag1()).isTrue();
    assertThat(target.getFlag2()).isTrue();
  }

  @Test
  void resolveSimpleSignature() throws Exception {
    Method desiredMethod = MethodSignatureBean.class.getMethod("doSomething");
    assertSignatureEquals(desiredMethod, "doSomething");
    assertSignatureEquals(desiredMethod, "doSomething()");
  }

  @Test
  void resolveInvalidSignatureEndParen() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            BeanUtils.resolveSignature("doSomething(", MethodSignatureBean.class));
  }

  @Test
  void resolveInvalidSignatureStartParen() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            BeanUtils.resolveSignature("doSomething)", MethodSignatureBean.class));
  }

  @Test
  void resolveWithAndWithoutArgList() throws Exception {
    Method desiredMethod = MethodSignatureBean.class.getMethod("doSomethingElse", String.class, int.class);
    assertSignatureEquals(desiredMethod, "doSomethingElse");
    assertThat(BeanUtils.resolveSignature("doSomethingElse()", MethodSignatureBean.class)).isNull();
  }

  @Test
  void resolveTypedSignature() throws Exception {
    Method desiredMethod = MethodSignatureBean.class.getMethod("doSomethingElse", String.class, int.class);
    assertSignatureEquals(desiredMethod, "doSomethingElse(java.lang.String, int)");
  }

  @Test
  void resolveOverloadedSignature() throws Exception {
    // test resolve with no args
    Method desiredMethod = MethodSignatureBean.class.getMethod("overloaded");
    assertSignatureEquals(desiredMethod, "overloaded()");

    // resolve with single arg
    desiredMethod = MethodSignatureBean.class.getMethod("overloaded", String.class);
    assertSignatureEquals(desiredMethod, "overloaded(java.lang.String)");

    // resolve with two args
    desiredMethod = MethodSignatureBean.class.getMethod("overloaded", String.class, BeanFactory.class);
    assertSignatureEquals(desiredMethod, "overloaded(java.lang.String, infra.beans.factory.BeanFactory)");
  }

  @Test
  void resolveSignatureWithArray() throws Exception {
    Method desiredMethod = MethodSignatureBean.class.getMethod("doSomethingWithAnArray", String[].class);
    assertSignatureEquals(desiredMethod, "doSomethingWithAnArray(java.lang.String[])");

    desiredMethod = MethodSignatureBean.class.getMethod("doSomethingWithAMultiDimensionalArray", String[][].class);
    assertSignatureEquals(desiredMethod, "doSomethingWithAMultiDimensionalArray(java.lang.String[][])");
  }

  @Test
  void spr6063() {
    PropertyDescriptor[] descrs = BeanUtils.getPropertyDescriptors(Bean.class);

    PropertyDescriptor keyDescr = BeanUtils.getPropertyDescriptor(Bean.class, "value");
    assertThat(keyDescr.getPropertyType()).isEqualTo(String.class);
    for (PropertyDescriptor propertyDescriptor : descrs) {
      if (propertyDescriptor.getName().equals(keyDescr.getName())) {
        assertThat(propertyDescriptor.getPropertyType()).as(propertyDescriptor.getName() + " has unexpected type").isEqualTo(keyDescr.getPropertyType());
      }
    }
  }

  @ParameterizedTest
  @ValueSource(classes = {
          boolean.class, char.class, byte.class, short.class, int.class, long.class, float.class, double.class,
          Boolean.class, Character.class, Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class,
          DayOfWeek.class, String.class, LocalDateTime.class, Date.class, UUID.class, URI.class, URL.class,
          Locale.class, Class.class
  })
  void isSimpleValueType(Class<?> type) {
    assertThat(BeanUtils.isSimpleValueType(type)).as("Type [" + type.getName() + "] should be a simple value type").isTrue();
  }

  @ParameterizedTest
  @ValueSource(classes = { int[].class, Object.class, List.class, void.class, Void.class })
  void isNotSimpleValueType(Class<?> type) {
    assertThat(BeanUtils.isSimpleValueType(type)).as("Type [" + type.getName() + "] should not be a simple value type").isFalse();
  }

  @ParameterizedTest
  @ValueSource(classes = {
          boolean.class, char.class, byte.class, short.class, int.class, long.class, float.class, double.class,
          Boolean.class, Character.class, Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class,
          DayOfWeek.class, String.class, LocalDateTime.class, Date.class, UUID.class, URI.class, URL.class,
          Locale.class, Class.class, boolean[].class, Boolean[].class, LocalDateTime[].class, Date[].class
  })
  void isSimpleProperty(Class<?> type) {
    assertThat(BeanUtils.isSimpleProperty(type)).as("Type [" + type.getName() + "] should be a simple property").isTrue();
  }

  @ParameterizedTest
  @ValueSource(classes = { Object.class, List.class, void.class, Void.class })
  void isNotSimpleProperty(Class<?> type) {
    assertThat(BeanUtils.isSimpleProperty(type)).as("Type [" + type.getName() + "] should not be a simple property").isFalse();
  }

  private void assertSignatureEquals(Method desiredMethod, String signature) {
    assertThat(BeanUtils.resolveSignature(signature, MethodSignatureBean.class)).isEqualTo(desiredMethod);
  }

  @Test
  void resolveMultipleRecordPublicConstructor() throws NoSuchMethodException {
    assertThat(BeanUtils.getConstructor(RecordWithMultiplePublicConstructors.class))
            .isEqualTo(RecordWithMultiplePublicConstructors.class.getDeclaredConstructor(String.class, String.class));
  }

  @Test
  void resolveMultipleRecordePackagePrivateConstructor() throws NoSuchMethodException {
    assertThat(BeanUtils.getConstructor(RecordWithMultiplePackagePrivateConstructors.class))
            .isEqualTo(RecordWithMultiplePackagePrivateConstructors.class.getDeclaredConstructor(String.class, String.class));
  }

  public record RecordWithMultiplePublicConstructors(String value, String name) {
    @SuppressWarnings("unused")
    public RecordWithMultiplePublicConstructors(String value) {
      this(value, "default value");
    }
  }

  record RecordWithMultiplePackagePrivateConstructors(String value, String name) {
    @SuppressWarnings("unused")
    RecordWithMultiplePackagePrivateConstructors(String value) {
      this(value, "default value");
    }
  }

  @SuppressWarnings("unused")
  private static class IntegerListHolder1 {

    private List<Integer> list = new ArrayList<>();

    public List<Integer> getList() {
      return list;
    }

    public void setList(List<Integer> list) {
      this.list = list;
    }
  }

  @SuppressWarnings("unused")
  private static class IntegerListHolder2 {

    private List<Integer> list = new ArrayList<>();

    public List<Integer> getList() {
      return list;
    }

    public void setList(List<Integer> list) {
      this.list = list;
    }
  }

  @SuppressWarnings("unused")
  private static class LongListHolder {

    private List<Long> list = new ArrayList<>();

    public List<Long> getList() {
      return list;
    }

    public void setList(List<Long> list) {
      this.list = list;
    }
  }

  @SuppressWarnings("unused")
  private static class NameAndSpecialProperty {

    private String name;

    private int specialProperty;

    public void setName(String name) {
      this.name = name;
    }

    public String getName() {
      return this.name;
    }

    public void setSpecialProperty(int specialProperty) {
      this.specialProperty = specialProperty;
    }

    public int getSpecialProperty() {
      return specialProperty;
    }
  }

  @SuppressWarnings("unused")
  private static class InvalidProperty {

    private String name;

    private String value;

    private boolean flag1;

    private boolean flag2;

    public void setName(String name) {
      this.name = name;
    }

    public String getName() {
      return this.name;
    }

    public void setValue(int value) {
      this.value = Integer.toString(value);
    }

    public String getValue() {
      return this.value;
    }

    public void setFlag1(boolean flag1) {
      this.flag1 = flag1;
    }

    public Boolean getFlag1() {
      return this.flag1;
    }

    public void setFlag2(Boolean flag2) {
      this.flag2 = flag2;
    }

    public boolean getFlag2() {
      return this.flag2;
    }
  }

  @SuppressWarnings("unused")
  private static class ContainerBean {

    private ContainedBean[] containedBeans;

    public ContainedBean[] getContainedBeans() {
      return containedBeans;
    }

    public void setContainedBeans(ContainedBean[] containedBeans) {
      this.containedBeans = containedBeans;
    }
  }

  @SuppressWarnings("unused")
  private static class ContainedBean {

    private String name;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  @SuppressWarnings("unused")
  private static class MethodSignatureBean {

    public void doSomething() {
    }

    public void doSomethingElse(String s, int x) {
    }

    public void overloaded() {
    }

    public void overloaded(String s) {
    }

    public void overloaded(String s, BeanFactory beanFactory) {
    }

    public void doSomethingWithAnArray(String[] strings) {
    }

    public void doSomethingWithAMultiDimensionalArray(String[][] strings) {
    }
  }

  private interface MapEntry<K, V> {

    K getKey();

    void setKey(V value);

    V getValue();

    void setValue(V value);
  }

  private static class Bean implements MapEntry<String, String> {

    private String key;

    private String value;

    @Override
    public String getKey() {
      return key;
    }

    @Override
    public void setKey(String aKey) {
      key = aKey;
    }

    @Override
    public String getValue() {
      return value;
    }

    @Override
    public void setValue(String aValue) {
      value = aValue;
    }
  }

  private static class BeanWithNullableTypes {

    private final Integer counter;

    private final Boolean flag;

    private final String value;

    @SuppressWarnings("unused")
    public BeanWithNullableTypes(@Nullable Integer counter, @Nullable Boolean flag, String value) {
      this.counter = counter;
      this.flag = flag;
      this.value = value;
    }

    @Nullable
    public Integer getCounter() {
      return counter;
    }

    @Nullable
    public Boolean isFlag() {
      return flag;
    }

    public String getValue() {
      return value;
    }
  }

  private static class BeanWithPrimitiveTypes {

    private final boolean flag;
    private final byte byteCount;
    private final short shortCount;
    private final int intCount;
    private final long longCount;
    private final float floatCount;
    private final double doubleCount;
    private final char character;
    private final String text;

    @SuppressWarnings("unused")
    public BeanWithPrimitiveTypes(boolean flag, byte byteCount, short shortCount, int intCount, long longCount,
            float floatCount, double doubleCount, char character, String text) {

      this.flag = flag;
      this.byteCount = byteCount;
      this.shortCount = shortCount;
      this.intCount = intCount;
      this.longCount = longCount;
      this.floatCount = floatCount;
      this.doubleCount = doubleCount;
      this.character = character;
      this.text = text;
    }

    public boolean isFlag() {
      return flag;
    }

    public byte getByteCount() {
      return byteCount;
    }

    public short getShortCount() {
      return shortCount;
    }

    public int getIntCount() {
      return intCount;
    }

    public long getLongCount() {
      return longCount;
    }

    public float getFloatCount() {
      return floatCount;
    }

    public double getDoubleCount() {
      return doubleCount;
    }

    public char getCharacter() {
      return character;
    }

    public String getText() {
      return text;
    }

  }

  private static class PrivateBeanWithPrivateConstructor {

    private PrivateBeanWithPrivateConstructor() {
    }
  }

  @SuppressWarnings("unused")
  private static class Order {

    private String id;
    private List<String> lineItems;

    Order() {
    }

    Order(String id, List<String> lineItems) {
      this.id = id;
      this.lineItems = lineItems;
    }

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public List<String> getLineItems() {
      return this.lineItems;
    }

    public void setLineItems(List<String> lineItems) {
      this.lineItems = lineItems;
    }

    @Override
    public String toString() {
      return "Order [id=" + this.id + ", lineItems=" + this.lineItems + "]";
    }
  }

  private interface OrderSummary {

    String getId();

    List<String> getLineItems();
  }

  private static class OrderInvocationHandler implements InvocationHandler {

    private final Order order;

    OrderInvocationHandler(Order order) {
      this.order = order;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      try {
        // Ignore args since OrderSummary doesn't declare any methods with arguments,
        // and we're not supporting equals(Object), etc.
        return Order.class.getDeclaredMethod(method.getName()).invoke(this.order);
      }
      catch (InvocationTargetException ex) {
        throw ex.getTargetException();
      }
    }
  }

  private static class GenericBaseModel<T> {

    private T id;

    private String name;

    public T getId() {
      return id;
    }

    public void setId(T id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  private static class User extends GenericBaseModel<Integer> {

    private String address;

    public User() {
      super();
    }

    public String getAddress() {
      return address;
    }

    public void setAddress(String address) {
      this.address = address;
    }
  }

}
