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

package cn.taketoday.context.properties.bind;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.internal.CharacterIndex;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import cn.taketoday.context.properties.source.ConfigurationPropertyName;
import cn.taketoday.context.properties.source.ConfigurationPropertySource;
import cn.taketoday.context.properties.source.MockConfigurationPropertySource;
import cn.taketoday.core.DefaultParameterNameDiscoverer;
import cn.taketoday.core.ParameterNameDiscoverer;
import cn.taketoday.core.ReflectiveParameterNameDiscoverer;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.test.tools.SourceFile;
import cn.taketoday.core.test.tools.TestCompiler;
import cn.taketoday.format.annotation.DateTimeFormat;
import cn.taketoday.lang.Assert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

/**
 * Tests for {@link ValueObjectBinder}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author Pavel Anisimov
 */
class ValueObjectBinderTests {

  private final List<ConfigurationPropertySource> sources = new ArrayList<>();

  private final Binder binder = new Binder(this.sources);

  @Test
  void bindToClassShouldCreateBoundBean() {
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("foo.int-value", "12");
    source.put("foo.long-value", "34");
    source.put("foo.boolean-value", "true");
    source.put("foo.string-value", "foo");
    source.put("foo.enum-value", "foo-bar");
    this.sources.add(source);
    ExampleValueBean bean = this.binder.bind("foo", Bindable.of(ExampleValueBean.class)).get();
    assertThat(bean.getIntValue()).isEqualTo(12);
    assertThat(bean.getLongValue()).isEqualTo(34);
    assertThat(bean.isBooleanValue()).isTrue();
    assertThat(bean.getStringValue()).isEqualTo("foo");
    assertThat(bean.getEnumValue()).isEqualTo(ExampleEnum.FOO_BAR);
  }

  @Test
  void bindToClassWhenHasNoPrefixShouldCreateBoundBean() {
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("int-value", "12");
    source.put("long-value", "34");
    source.put("boolean-value", "true");
    source.put("string-value", "foo");
    source.put("enum-value", "foo-bar");
    this.sources.add(source);
    ExampleValueBean bean = this.binder.bind(ConfigurationPropertyName.of(""), Bindable.of(ExampleValueBean.class))
            .get();
    assertThat(bean.getIntValue()).isEqualTo(12);
    assertThat(bean.getLongValue()).isEqualTo(34);
    assertThat(bean.isBooleanValue()).isTrue();
    assertThat(bean.getStringValue()).isEqualTo("foo");
    assertThat(bean.getEnumValue()).isEqualTo(ExampleEnum.FOO_BAR);
  }

  @Test
  void bindToAbstractClassWithShouldNotBind() {
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("foo.name", "test");
    this.sources.add(source);
    boolean bound = this.binder.bind("foo", Bindable.of(ExampleAbstractBean.class)).isBound();
    assertThat(bound).isFalse();
  }

  @Test
  void bindToClassWithMultipleConstructorsShouldNotBind() {
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("foo.int-value", "12");
    this.sources.add(source);
    boolean bound = this.binder.bind("foo", Bindable.of(MultipleConstructorsBean.class)).isBound();
    assertThat(bound).isFalse();
  }

  @Test
  void bindToClassWithMultipleConstructorsAndFilterShouldBind() {
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("foo.int-value", "12");
    this.sources.add(source);
    Constructor<?>[] constructors = MultipleConstructorsBean.class.getDeclaredConstructors();
    Constructor<?> constructor = (constructors[0].getParameterCount() == 1) ? constructors[0] : constructors[1];
    Binder binder = new Binder(this.sources, null, (ConversionService) null, null, null,
            (bindable, isNestedConstructorBinding) -> constructor);
    MultipleConstructorsBean bound = binder.bind("foo", Bindable.of(MultipleConstructorsBean.class)).get();
    assertThat(bound.getIntValue()).isEqualTo(12);
  }

  @Test
  void bindToClassWithOnlyDefaultConstructorShouldNotBind() {
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("foo.int-value", "12");
    this.sources.add(source);
    boolean bound = this.binder.bind("foo", Bindable.of(DefaultConstructorBean.class)).isBound();
    assertThat(bound).isFalse();
  }

  @Test
  void bindToClassWithMultipleConstructorsWhenOnlyOneIsNotPrivateShouldBind() {
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("foo.int-value", "12");
    this.sources.add(source);
    MultipleConstructorsOnlyOneNotPrivateBean bean = this.binder
            .bind("foo", Bindable.of(MultipleConstructorsOnlyOneNotPrivateBean.class))
            .get();
    bean = bean.withString("test");
    assertThat(bean.getIntValue()).isEqualTo(12);
    assertThat(bean.getStringValue()).isEqualTo("test");
  }

  @Test
  void bindToClassShouldBindNested() {
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("foo.value-bean.int-value", "123");
    source.put("foo.value-bean.long-value", "34");
    source.put("foo.value-bean.string-value", "foo");
    this.sources.add(source);
    ExampleNestedBean bean = this.binder.bind("foo", Bindable.of(ExampleNestedBean.class)).get();
    assertThat(bean.getValueBean().getIntValue()).isEqualTo(123);
    assertThat(bean.getValueBean().getLongValue()).isEqualTo(34);
    assertThat(bean.getValueBean().isBooleanValue()).isFalse();
    assertThat(bean.getValueBean().getStringValue()).isEqualTo("foo");
    assertThat(bean.getValueBean().getEnumValue()).isNull();
  }

  @Test
  void bindToClassWithNoValueForPrimitiveShouldUseDefault() {
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("foo.string-value", "foo");
    this.sources.add(source);
    ExampleValueBean bean = this.binder.bind("foo", Bindable.of(ExampleValueBean.class)).get();
    assertThat(bean.getIntValue()).isZero();
    assertThat(bean.getLongValue()).isZero();
    assertThat(bean.isBooleanValue()).isFalse();
    assertThat(bean.getStringValue()).isEqualTo("foo");
  }

  @Test
  void bindToClassWithNoValueAndDefaultValueShouldNotBind() {
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("foo.string-value", "foo");
    this.sources.add(source);
    assertThat(this.binder.bind("foo", Bindable.of(ExampleDefaultValueBean.class)).isBound()).isFalse();
  }

  @Test
  void bindToClassWhenNoParameterBoundShouldReturnNull() {
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    this.sources.add(source.nonIterable());
    BindResult<ExampleFailingConstructorBean> result = this.binder.bind("foo",
            Bindable.of(ExampleFailingConstructorBean.class));
    assertThat(result.isBound()).isFalse();
  }

  @Test
  void bindWithAnnotations() {
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("foo.date", "2014-04-01");
    this.sources.add(source);
    ConverterAnnotatedExampleBean bean = this.binder.bind("foo", Bindable.of(ConverterAnnotatedExampleBean.class))
            .get();
    assertThat(bean.getDate()).hasToString("2014-04-01");
  }

  @Test
  void bindWithAnnotationsAndDefaultValue() {
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("foo.bar", "hello");
    this.sources.add(source);
    ConverterAnnotatedExampleBean bean = this.binder.bind("foo", Bindable.of(ConverterAnnotatedExampleBean.class))
            .get();
    assertThat(bean.getDate()).hasToString("2019-05-10");
  }

  @Test
  void bindToClassWhenHasPackagePrivateConstructorShouldBind() {
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("foo.property", "test");
    this.sources.add(source);
    ExamplePackagePrivateConstructorBean bound = this.binder
            .bind("foo", Bindable.of(ExamplePackagePrivateConstructorBean.class))
            .get();
    assertThat(bound.getProperty()).isEqualTo("test");
  }

  @Test
  void createShouldReturnCreatedValue() {
    ExampleValueBean value = this.binder.bindOrCreate("foo", Bindable.of(ExampleValueBean.class));
    assertThat(value.getIntValue()).isZero();
    assertThat(value.getLongValue()).isZero();
    assertThat(value.isBooleanValue()).isFalse();
    assertThat(value.getStringValue()).isNull();
    assertThat(value.getEnumValue()).isNull();
  }

  @Test
  void createWithNestedShouldReturnCreatedValue() {
    ExampleNestedBean value = this.binder.bindOrCreate("foo", Bindable.of(ExampleNestedBean.class));
    assertThat(value.getValueBean()).isNull();
  }

  @Test
  void createWithDefaultValuesShouldReturnCreatedWithDefaultValues() {
    ExampleDefaultValueBean value = this.binder.bindOrCreate("foo", Bindable.of(ExampleDefaultValueBean.class));
    assertThat(value.getIntValue()).isEqualTo(5);
    assertThat(value.getStringsList()).containsOnly("a", "b", "c");
    assertThat(value.getCustomList()).containsOnly("x,y,z");
  }

  @Test
  void createWithDefaultValuesAndAnnotationsShouldReturnCreatedWithDefaultValues() {
    ConverterAnnotatedExampleBean bean = this.binder.bindOrCreate("foo",
            Bindable.of(ConverterAnnotatedExampleBean.class));
    assertThat(bean.getDate()).hasToString("2019-05-10");
  }

  @Test
  void bindWhenAllPropertiesBoundShouldClearConfigurationProperty() { // gh-18704
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("foo.bar", "hello");
    this.sources.add(source);
    Bindable<ValidatingConstructorBean> target = Bindable.of(ValidatingConstructorBean.class);
    assertThatExceptionOfType(BindException.class).isThrownBy(() -> this.binder.bind("foo", target))
            .satisfies(this::noConfigurationProperty);
  }

  @Test
  void bindToClassShouldBindWithGenerics() {
    // gh-19156
    ResolvableType type = ResolvableType.forClassWithGenerics(Map.class, String.class, String.class);
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("foo.value.bar", "baz");
    this.sources.add(source);
    GenericValue<Map<String, String>> bean = this.binder
            .bind("foo", Bindable
                    .<GenericValue<Map<String, String>>>of(ResolvableType.forClassWithGenerics(GenericValue.class, type)))
            .get();
    assertThat(bean.getValue()).containsEntry("bar", "baz");
  }

  @Test
  void bindWhenParametersWithDefaultValueShouldReturnNonNullValues() {
    NestedConstructorBeanWithDefaultValue bound = this.binder.bindOrCreate("foo",
            Bindable.of(NestedConstructorBeanWithDefaultValue.class));
    assertThat(bound.getNestedImmutable().getFoo()).isEqualTo("hello");
    assertThat(bound.getNestedJavaBean()).isNotNull();
  }

  @Test
  void bindWhenJavaLangParameterWithEmptyDefaultValueShouldThrowException() {
    assertThatExceptionOfType(BindException.class)
            .isThrownBy(() -> this.binder.bindOrCreate("foo",
                    Bindable.of(NestedConstructorBeanWithEmptyDefaultValueForJavaLangTypes.class)))
            .withStackTraceContaining("Parameter of type java.lang.String must have a non-empty default value.");
  }

  @Test
  void bindWhenCollectionParameterWithEmptyDefaultValueShouldReturnEmptyInstance() {
    NestedConstructorBeanWithEmptyDefaultValueForCollectionTypes bound = this.binder.bindOrCreate("foo",
            Bindable.of(NestedConstructorBeanWithEmptyDefaultValueForCollectionTypes.class));
    assertThat(bound.getListValue()).isEmpty();
  }

  @Test
  void bindWhenMapParametersWithEmptyDefaultValueShouldReturnEmptyInstance() {
    NestedConstructorBeanWithEmptyDefaultValueForMapTypes bound = this.binder.bindOrCreate("foo",
            Bindable.of(NestedConstructorBeanWithEmptyDefaultValueForMapTypes.class));
    assertThat(bound.getMapValue()).isEmpty();
  }

  @Test
  void bindWhenEnumMapParametersWithEmptyDefaultValueShouldReturnEmptyInstance() {
    NestedConstructorBeanWithEmptyDefaultValueForEnumMapTypes bound = this.binder.bindOrCreate("foo",
            Bindable.of(NestedConstructorBeanWithEmptyDefaultValueForEnumMapTypes.class));
    assertThat(bound.getMapValue()).isEmpty();
  }

  @Test
  void bindWhenArrayParameterWithEmptyDefaultValueShouldReturnEmptyInstance() {
    NestedConstructorBeanWithEmptyDefaultValueForArrayTypes bound = this.binder.bindOrCreate("foo",
            Bindable.of(NestedConstructorBeanWithEmptyDefaultValueForArrayTypes.class));
    assertThat(bound.getArrayValue()).isEmpty();
  }

  @Test
  void bindWhenOptionalParameterWithEmptyDefaultValueShouldReturnEmptyInstance() {
    NestedConstructorBeanWithEmptyDefaultValueForOptionalTypes bound = this.binder.bindOrCreate("foo",
            Bindable.of(NestedConstructorBeanWithEmptyDefaultValueForOptionalTypes.class));
    assertThat(bound.getOptionalValue()).isEmpty();
  }

  @Test
  void bindWhenEnumParameterWithEmptyDefaultValueShouldThrowException() {
    assertThatExceptionOfType(BindException.class)
            .isThrownBy(() -> this.binder.bindOrCreate("foo",
                    Bindable.of(NestedConstructorBeanWithEmptyDefaultValueForEnumTypes.class)))
            .withStackTraceContaining(
                    "Parameter of type cn.taketoday.context.properties.bind.ValueObjectBinderTests$NestedConstructorBeanWithEmptyDefaultValueForEnumTypes$Foo must have a non-empty default value.");
  }

  @Test
  void bindWhenPrimitiveParameterWithEmptyDefaultValueShouldThrowException() {
    assertThatExceptionOfType(BindException.class)
            .isThrownBy(() -> this.binder.bindOrCreate("foo",
                    Bindable.of(NestedConstructorBeanWithEmptyDefaultValueForPrimitiveTypes.class)))
            .withStackTraceContaining("Parameter of type int must have a non-empty default value.");
  }

  @Test
  void bindWhenBindingToPathTypeWithValue() { // gh-21263
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("test.name", "test");
    source.put("test.path", "specific_value");
    this.sources.add(source);
    Bindable<PathBean> target = Bindable.of(PathBean.class);
    PathBean bound = this.binder.bind("test", target).get();
    assertThat(bound.getName()).isEqualTo("test");
    assertThat(bound.getPath()).isEqualTo(Paths.get("specific_value"));
  }

  @Test
  void bindWhenBindingToPathTypeWithDefaultValue() { // gh-21263
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("test.name", "test");
    this.sources.add(source);
    Bindable<PathBean> target = Bindable.of(PathBean.class);
    PathBean bound = this.binder.bindOrCreate("test", target);
    assertThat(bound.getName()).isEqualTo("test");
    assertThat(bound.getPath()).isEqualTo(Paths.get("default_value"));
  }

  @Test
  void bindToAnnotationNamedConstructorParameter() {
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("test.import", "test");
    this.sources.add(source);
    Bindable<NamedConstructorParameter> target = Bindable.of(NamedConstructorParameter.class);
    NamedConstructorParameter bound = this.binder.bindOrCreate("test", target);
    assertThat(bound.getImportName()).isEqualTo("test");
  }

  @Test
  void bindToAnnotationNamedRecordComponent() {
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("test.import", "test");
    this.sources.add(source);
    Bindable<NamedRecordComponent> target = Bindable.of(NamedRecordComponent.class);
    NamedRecordComponent bound = this.binder.bindOrCreate("test", target);
    assertThat(bound.importName()).isEqualTo("test");
  }

  @Test
  void bindToRecordWithDefaultValue() {
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("test.record.property1", "value-from-config-1");
    this.sources.add(source);
    String recordProperties = """
            public record RecordProperties(
            	@cn.taketoday.context.properties.bind.DefaultValue("default-value-1") String property1,
            	@cn.taketoday.context.properties.bind.DefaultValue("default-value-2") String property2) {
            }
            """;
    TestCompiler.forSystem().withSources(SourceFile.of(recordProperties)).compile((compiled) -> {
      try {
        ClassLoader cl = compiled.getClassLoader();
        Object bean = this.binder.bind("test.record", Class.forName("RecordProperties", true, cl)).get();
        assertThat(bean).hasFieldOrPropertyWithValue("property1", "value-from-config-1")
                .hasFieldOrPropertyWithValue("property2", "default-value-2");
      }
      catch (ClassNotFoundException ex) {
        fail("Expected generated class 'RecordProperties' not found", ex);
      }
    });
  }

  @Test
  void bindWhenNonExtractableParameterNamesOnPropertyAndNonIterablePropertySource() throws Exception {
    verifyJsonPathParametersCannotBeResolved();
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("test.value", "test");
    this.sources.add(source.nonIterable());
    Bindable<NonExtractableParameterName> target = Bindable.of(NonExtractableParameterName.class);
    NonExtractableParameterName bound = this.binder.bindOrCreate("test", target);
    assertThat(bound.getValue()).isEqualTo("test");
  }

  @Test
  void createWhenNonExtractableParameterNamesOnPropertyAndNonIterablePropertySource() throws Exception {
    assertThat(new ReflectiveParameterNameDiscoverer()
            .getParameterNames(CharacterIndex.class.getDeclaredConstructor(CharSequence.class))).isNull();
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    this.sources.add(source.nonIterable());
    Bindable<CharacterIndex> target = Bindable.of(CharacterIndex.class).withBindMethod(BindMethod.VALUE_OBJECT);
    assertThatExceptionOfType(BindException.class).isThrownBy(() -> this.binder.bindOrCreate("test", target))
            /*.withStackTraceContaining("Ensure that the compiler uses the '-parameters' flag")*/;
  }

  private void verifyJsonPathParametersCannotBeResolved() throws NoSuchFieldException {
    Class<?> jsonPathClass = NonExtractableParameterName.class.getDeclaredField("jsonPath").getType();
    Constructor<?>[] constructors = jsonPathClass.getDeclaredConstructors();
    assertThat(constructors).hasSize(1);
    constructors[0].setAccessible(true);
    assertThat(ParameterNameDiscoverer.findParameterNames(constructors[0])).hasSize(2);
  }

  private void noConfigurationProperty(BindException ex) {
    assertThat(ex.getProperty()).isNull();
  }

  static class ExampleValueBean {

    private final int intValue;

    private final long longValue;

    private final boolean booleanValue;

    private final String stringValue;

    private final ExampleEnum enumValue;

    ExampleValueBean(int intValue, long longValue, boolean booleanValue, String stringValue,
            ExampleEnum enumValue) {
      this.intValue = intValue;
      this.longValue = longValue;
      this.booleanValue = booleanValue;
      this.stringValue = stringValue;
      this.enumValue = enumValue;
    }

    int getIntValue() {
      return this.intValue;
    }

    long getLongValue() {
      return this.longValue;
    }

    boolean isBooleanValue() {
      return this.booleanValue;
    }

    String getStringValue() {
      return this.stringValue;
    }

    ExampleEnum getEnumValue() {
      return this.enumValue;
    }

  }

  public enum ExampleEnum {

    FOO_BAR,

    BAR_BAZ

  }

  static class MultipleConstructorsBean {

    private final int intValue;

    MultipleConstructorsBean(int intValue) {
      this(intValue, 23L, "hello");
    }

    MultipleConstructorsBean(int intValue, long longValue, String stringValue) {
      this.intValue = intValue;
    }

    int getIntValue() {
      return this.intValue;
    }

  }

  static class MultipleConstructorsOnlyOneNotPrivateBean {

    private final int intValue;

    private final String stringValue;

    MultipleConstructorsOnlyOneNotPrivateBean(int intValue) {
      this(intValue, 23L, "hello");
    }

    private MultipleConstructorsOnlyOneNotPrivateBean(int intValue, long longValue, String stringValue) {
      this.intValue = intValue;
      this.stringValue = stringValue;
    }

    int getIntValue() {
      return this.intValue;
    }

    String getStringValue() {
      return this.stringValue;
    }

    MultipleConstructorsOnlyOneNotPrivateBean withString(String stringValue) {
      return new MultipleConstructorsOnlyOneNotPrivateBean(this.intValue, 0, stringValue);
    }

  }

  abstract static class ExampleAbstractBean {

    private final String name;

    ExampleAbstractBean(String name) {
      this.name = name;
    }

    String getName() {
      return this.name;
    }

  }

  static class DefaultConstructorBean {

    DefaultConstructorBean() {
    }

  }

  static class ExampleNestedBean {

    private final ExampleValueBean valueBean;

    ExampleNestedBean(ExampleValueBean valueBean) {
      this.valueBean = valueBean;
    }

    ExampleValueBean getValueBean() {
      return this.valueBean;
    }

  }

  static class ExampleDefaultValueBean {

    private final int intValue;

    private final List<String> stringsList;

    private final List<String> customList;

    ExampleDefaultValueBean(@DefaultValue("5") int intValue,
            @DefaultValue({ "a", "b", "c" }) List<String> stringsList,
            @DefaultValue("x,y,z") List<String> customList) {
      this.intValue = intValue;
      this.stringsList = stringsList;
      this.customList = customList;
    }

    int getIntValue() {
      return this.intValue;
    }

    List<String> getStringsList() {
      return this.stringsList;
    }

    List<String> getCustomList() {
      return this.customList;
    }

  }

  static class ExampleFailingConstructorBean {

    private final String name;

    private final Object value;

    ExampleFailingConstructorBean(String name, String value) {
      Assert.notNull(name, "'name' must be not null.");
      Assert.notNull(value, "'value' must be not null.");
      this.name = name;
      this.value = value;
    }

    String getName() {
      return this.name;
    }

    Object getValue() {
      return this.value;
    }

  }

  static class ConverterAnnotatedExampleBean {

    private final LocalDate date;

    private final String bar;

    ConverterAnnotatedExampleBean(
            @DefaultValue("2019-05-10") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, String bar) {
      this.date = date;
      this.bar = bar;
    }

    LocalDate getDate() {
      return this.date;
    }

    String getBar() {
      return this.bar;
    }

  }

  static class ExamplePackagePrivateConstructorBean {

    private final String property;

    ExamplePackagePrivateConstructorBean(String property) {
      this.property = property;
    }

    String getProperty() {
      return this.property;
    }

  }

  static class ValidatingConstructorBean {

    private final String foo;

    private final String bar;

    ValidatingConstructorBean(String foo, String bar) {
      Assert.notNull(foo, "Foo is required");
      this.foo = foo;
      this.bar = bar;
    }

    String getFoo() {
      return this.foo;
    }

    String getBar() {
      return this.bar;
    }

  }

  static class GenericValue<T> {

    private final T value;

    GenericValue(T value) {
      this.value = value;
    }

    T getValue() {
      return this.value;
    }

  }

  static class NestedConstructorBeanWithDefaultValue {

    private final NestedImmutable nestedImmutable;

    private final NestedJavaBean nestedJavaBean;

    NestedConstructorBeanWithDefaultValue(@DefaultValue NestedImmutable nestedImmutable,
            @DefaultValue NestedJavaBean nestedJavaBean) {
      this.nestedImmutable = nestedImmutable;
      this.nestedJavaBean = nestedJavaBean;
    }

    NestedImmutable getNestedImmutable() {
      return this.nestedImmutable;
    }

    NestedJavaBean getNestedJavaBean() {
      return this.nestedJavaBean;
    }

  }

  static class NestedImmutable {

    private final String foo;

    private final String bar;

    NestedImmutable(@DefaultValue("hello") String foo, String bar) {
      this.foo = foo;
      this.bar = bar;
    }

    String getFoo() {
      return this.foo;
    }

    String getBar() {
      return this.bar;
    }

  }

  static class NestedJavaBean {

    private String value;

    String getValue() {
      return this.value;
    }

  }

  static class NestedConstructorBeanWithEmptyDefaultValueForJavaLangTypes {

    private final String stringValue;

    NestedConstructorBeanWithEmptyDefaultValueForJavaLangTypes(@DefaultValue String stringValue) {
      this.stringValue = stringValue;
    }

    String getStringValue() {
      return this.stringValue;
    }

  }

  static class NestedConstructorBeanWithEmptyDefaultValueForCollectionTypes {

    private final List<String> listValue;

    NestedConstructorBeanWithEmptyDefaultValueForCollectionTypes(@DefaultValue List<String> listValue) {
      this.listValue = listValue;
    }

    List<String> getListValue() {
      return this.listValue;
    }

  }

  static class NestedConstructorBeanWithEmptyDefaultValueForMapTypes {

    private final Map<String, String> mapValue;

    NestedConstructorBeanWithEmptyDefaultValueForMapTypes(@DefaultValue Map<String, String> mapValue) {
      this.mapValue = mapValue;
    }

    Map<String, String> getMapValue() {
      return this.mapValue;
    }

  }

  static class NestedConstructorBeanWithEmptyDefaultValueForEnumMapTypes {

    private final EnumMap<ExampleEnum, String> mapValue;

    NestedConstructorBeanWithEmptyDefaultValueForEnumMapTypes(@DefaultValue EnumMap<ExampleEnum, String> mapValue) {
      this.mapValue = mapValue;
    }

    EnumMap<ExampleEnum, String> getMapValue() {
      return this.mapValue;
    }

  }

  static class NestedConstructorBeanWithEmptyDefaultValueForArrayTypes {

    private final String[] arrayValue;

    NestedConstructorBeanWithEmptyDefaultValueForArrayTypes(@DefaultValue String[] arrayValue) {
      this.arrayValue = arrayValue;
    }

    String[] getArrayValue() {
      return this.arrayValue;
    }

  }

  static class NestedConstructorBeanWithEmptyDefaultValueForOptionalTypes {

    private final Optional<String> optionalValue;

    NestedConstructorBeanWithEmptyDefaultValueForOptionalTypes(@DefaultValue Optional<String> optionalValue) {
      this.optionalValue = optionalValue;
    }

    Optional<String> getOptionalValue() {
      return this.optionalValue;
    }

  }

  static class NestedConstructorBeanWithEmptyDefaultValueForEnumTypes {

    private final Foo foo;

    NestedConstructorBeanWithEmptyDefaultValueForEnumTypes(@DefaultValue Foo foo) {
      this.foo = foo;
    }

    Foo getFoo() {
      return this.foo;
    }

    enum Foo {

      BAR, BAZ

    }

  }

  static class NestedConstructorBeanWithEmptyDefaultValueForPrimitiveTypes {

    private final int intValue;

    NestedConstructorBeanWithEmptyDefaultValueForPrimitiveTypes(@DefaultValue int intValue) {
      this.intValue = intValue;
    }

    int getIntValue() {
      return this.intValue;
    }

  }

  static class PathBean {

    private final String name;

    private final Path path;

    PathBean(String name, @DefaultValue("default_value") Path path) {
      this.name = name;
      this.path = path;
    }

    String getName() {
      return this.name;
    }

    Path getPath() {
      return this.path;
    }

  }

  static class NamedConstructorParameter {

    private final String importName;

    NamedConstructorParameter(@Name("import") String importName) {
      this.importName = importName;
    }

    String getImportName() {
      return this.importName;
    }

  }

  record NamedRecordComponent(@Name("import") String importName) {
  }

  static class NonExtractableParameterName {

    private String value;

    private JsonPath jsonPath;

    String getValue() {
      return this.value;
    }

    void setValue(String value) {
      this.value = value;
    }

    JsonPath getJsonPath() {
      return this.jsonPath;
    }

    void setJsonPath(JsonPath jsonPath) {
      this.jsonPath = jsonPath;
    }

  }

}
