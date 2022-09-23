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

package cn.taketoday.context.properties.bind;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.InOrder;

import java.beans.PropertyEditorSupport;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.context.properties.bind.Bindable.BindRestriction;
import cn.taketoday.context.properties.bind.validation.ValidationBindHandler;
import cn.taketoday.context.properties.source.ConfigurationPropertyName;
import cn.taketoday.context.properties.source.ConfigurationPropertySource;
import cn.taketoday.context.properties.source.ConfigurationPropertySources;
import cn.taketoday.context.properties.source.MockConfigurationPropertySource;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.core.conversion.ConversionFailedException;
import cn.taketoday.core.env.MapPropertySource;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.core.io.Resource;
import cn.taketoday.format.annotation.DateTimeFormat;
import cn.taketoday.test.context.support.TestPropertySourceUtils;
import cn.taketoday.validation.Validator;
import cn.taketoday.validation.annotation.Validated;
import cn.taketoday.validation.beanvalidation.InfraValidatorAdapter;
import jakarta.validation.Validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link Binder}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class BinderTests {

  private final List<ConfigurationPropertySource> sources = new ArrayList<>();

  private Binder binder = new Binder(this.sources);

  @Test
  void createWhenSourcesIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new Binder((Iterable<ConfigurationPropertySource>) null))
            .withMessageContaining("Sources must not be null");
  }

  @Test
  void bindWhenNameIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> this.binder.bind((ConfigurationPropertyName) null,
            Bindable.of(String.class), BindHandler.DEFAULT)).withMessageContaining("Name must not be null");
  }

  @Test
  void bindWhenTargetIsNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.binder.bind(ConfigurationPropertyName.of("foo"), null, BindHandler.DEFAULT))
            .withMessageContaining("Target must not be null");
  }

  @Test
  void bindToValueWhenPropertyIsMissingShouldReturnUnbound() {
    this.sources.add(new MockConfigurationPropertySource());
    BindResult<String> result = this.binder.bind("foo", Bindable.of(String.class));
    assertThat(result.isBound()).isFalse();
  }

  @Test
  void bindToValueShouldReturnPropertyValue() {
    this.sources.add(new MockConfigurationPropertySource("foo", 123));
    BindResult<Integer> result = this.binder.bind("foo", Bindable.of(Integer.class));
    assertThat(result.get()).isEqualTo(123);
  }

  @Test
  void bindToValueShouldReturnPropertyValueFromSecondSource() {
    this.sources.add(new MockConfigurationPropertySource("foo", 123));
    this.sources.add(new MockConfigurationPropertySource("bar", 234));
    BindResult<Integer> result = this.binder.bind("bar", Bindable.of(Integer.class));
    assertThat(result.get()).isEqualTo(234);
  }

  @Test
  void bindToValueShouldReturnConvertedPropertyValue() {
    this.sources.add(new MockConfigurationPropertySource("foo", "123"));
    BindResult<Integer> result = this.binder.bind("foo", Bindable.of(Integer.class));
    assertThat(result.get()).isEqualTo(123);
  }

  @Test
  void bindToValueWhenMultipleCandidatesShouldReturnFirst() {
    this.sources.add(new MockConfigurationPropertySource("foo", 123));
    this.sources.add(new MockConfigurationPropertySource("foo", 234));
    BindResult<Integer> result = this.binder.bind("foo", Bindable.of(Integer.class));
    assertThat(result.get()).isEqualTo(123);
  }

  @Test
  void bindToValueWithPlaceholdersShouldResolve() {
    StandardEnvironment environment = new StandardEnvironment();
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(environment, "bar=23");
    this.sources.add(new MockConfigurationPropertySource("foo", "1${bar}"));
    this.binder = new Binder(this.sources, new PropertySourcesPlaceholdersResolver(environment));
    BindResult<Integer> result = this.binder.bind("foo", Bindable.of(Integer.class));
    assertThat(result.get()).isEqualTo(123);
  }

  @Test
  void bindToValueWithMissingPlaceholderShouldResolveToValueWithPlaceholder() {
    StandardEnvironment environment = new StandardEnvironment();
    this.sources.add(new MockConfigurationPropertySource("foo", "${bar}"));
    this.binder = new Binder(this.sources, new PropertySourcesPlaceholdersResolver(environment));
    BindResult<String> result = this.binder.bind("foo", Bindable.of(String.class));
    assertThat(result.get()).isEqualTo("${bar}");
  }

  @Test
  void bindToValueWithCustomPropertyEditorShouldReturnConvertedValue() {
    this.binder = new Binder(this.sources, null, null,
            (registry) -> registry.registerCustomEditor(JavaBean.class, new JavaBeanPropertyEditor()));
    this.sources.add(new MockConfigurationPropertySource("foo", "123"));
    BindResult<JavaBean> result = this.binder.bind("foo", Bindable.of(JavaBean.class));
    assertThat(result.get().getValue()).isEqualTo("123");
  }

  @Test
  void bindToValueShouldTriggerOnSuccess() {
    this.sources.add(new MockConfigurationPropertySource("foo", "1", "line1"));
    BindHandler handler = mock(BindHandler.class, Answers.CALLS_REAL_METHODS);
    Bindable<Integer> target = Bindable.of(Integer.class);
    this.binder.bind("foo", target, handler);
    InOrder ordered = inOrder(handler);
    ordered.verify(handler).onSuccess(eq(ConfigurationPropertyName.of("foo")), eq(target), any(), eq(1));
  }

  @Test
  void bindOrCreateWhenNotBoundShouldTriggerOnCreate() {
    BindHandler handler = mock(BindHandler.class, Answers.CALLS_REAL_METHODS);
    Bindable<JavaBean> target = Bindable.of(JavaBean.class);
    this.binder.bindOrCreate("foo", target, handler);
    InOrder ordered = inOrder(handler);
    ordered.verify(handler).onCreate(eq(ConfigurationPropertyName.of("foo")), eq(target), any(), any());
  }

  @Test
  void bindToJavaBeanShouldReturnPopulatedBean() {
    this.sources.add(new MockConfigurationPropertySource("foo.value", "bar"));
    JavaBean result = this.binder.bind("foo", Bindable.of(JavaBean.class)).get();
    assertThat(result.getValue()).isEqualTo("bar");
  }

  @Test
  void bindToJavaBeanWhenNonIterableShouldReturnPopulatedBean() {
    MockConfigurationPropertySource source = new MockConfigurationPropertySource("foo.value", "bar");
    this.sources.add(source.nonIterable());
    JavaBean result = this.binder.bind("foo", Bindable.of(JavaBean.class)).get();
    assertThat(result.getValue()).isEqualTo("bar");
  }

  @Test
  void bindToJavaBeanWhenHasPropertyWithSameNameShouldStillBind() {
    // gh-10945
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("foo", "boom");
    source.put("foo.value", "bar");
    this.sources.add(source);
    JavaBean result = this.binder.bind("foo", Bindable.of(JavaBean.class)).get();
    assertThat(result.getValue()).isEqualTo("bar");
  }

  @Test
  void bindToJavaBeanShouldTriggerOnSuccess() {
    this.sources.add(new MockConfigurationPropertySource("foo.value", "bar", "line1"));
    BindHandler handler = mock(BindHandler.class, Answers.CALLS_REAL_METHODS);
    Bindable<JavaBean> target = Bindable.of(JavaBean.class);
    this.binder.bind("foo", target, handler);
    InOrder inOrder = inOrder(handler);
    inOrder.verify(handler).onSuccess(eq(ConfigurationPropertyName.of("foo.value")), eq(Bindable.of(String.class)),
            any(), eq("bar"));
    inOrder.verify(handler).onSuccess(eq(ConfigurationPropertyName.of("foo")), eq(target), any(),
            isA(JavaBean.class));
  }

  @Test
  void bindWhenHasCustomDefaultHandlerShouldTriggerOnSuccess() {
    this.sources.add(new MockConfigurationPropertySource("foo.value", "bar", "line1"));
    BindHandler handler = mock(BindHandler.class, Answers.CALLS_REAL_METHODS);
    Binder binder = new Binder(this.sources, null, null, null, handler);
    Bindable<JavaBean> target = Bindable.of(JavaBean.class);
    binder.bind("foo", target);
    InOrder inOrder = inOrder(handler);
    inOrder.verify(handler).onSuccess(eq(ConfigurationPropertyName.of("foo.value")), eq(Bindable.of(String.class)),
            any(), eq("bar"));
    inOrder.verify(handler).onSuccess(eq(ConfigurationPropertyName.of("foo")), eq(target), any(),
            isA(JavaBean.class));
  }

  @Test
  void bindWhenHasMalformedDateShouldThrowException() {
    this.sources.add(new MockConfigurationPropertySource("foo", "2014-04-01T01:30:00.000-05:00"));
    assertThatExceptionOfType(BindException.class)
            .isThrownBy(() -> this.binder.bind("foo", Bindable.of(LocalDate.class)))
            .withCauseInstanceOf(ConversionFailedException.class);
  }

  @Test
  void bindWhenHasAnnotationsShouldChangeConvertedValue() {
    this.sources.add(new MockConfigurationPropertySource("foo", "2014-04-01T01:30:00.000-05:00"));
    DateTimeFormat annotation = AnnotationUtils.synthesizeAnnotation(
            Collections.singletonMap("iso", DateTimeFormat.ISO.DATE_TIME), DateTimeFormat.class, null);
    LocalDate result = this.binder.bind("foo", Bindable.of(LocalDate.class).withAnnotations(annotation)).get();
    assertThat(result.toString()).isEqualTo("2014-04-01");
  }

  @Test
  void bindToValidatedBeanWithResourceAndNonEnumerablePropertySource() {
    ConfigurationPropertySources.from(new PropertySource<String>("test") {

      @Override
      public Object getProperty(String name) {
        return null;
      }

    }).forEach(this.sources::add);
    Validator validator = new InfraValidatorAdapter(
            Validation.byDefaultProvider().configure().buildValidatorFactory().getValidator());
    this.binder.bind("foo", Bindable.of(ResourceBean.class), new ValidationBindHandler(validator));
  }

  @Test
  void bindToBeanWithCycle() {
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    this.sources.add(source.nonIterable());
    Bindable<CycleBean1> target = Bindable.of(CycleBean1.class);
    this.binder.bind("foo", target);
  }

  @Test
  @SuppressWarnings("rawtypes")
  void bindToBeanWithUnresolvableGenerics() {
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("foo.bar", "hello");
    this.sources.add(source);
    Bindable<GenericBean> target = Bindable.of(GenericBean.class);
    this.binder.bind("foo", target);
  }

  @Test
  void bindWithEmptyPrefixShouldIgnorePropertiesWithEmptyName() {
    Map<String, Object> source = new HashMap<>();
    source.put("value", "hello");
    source.put("", "bar");
    Iterable<ConfigurationPropertySource> propertySources = ConfigurationPropertySources
            .from(new MapPropertySource("test", source));
    propertySources.forEach(this.sources::add);
    Bindable<JavaBean> target = Bindable.of(JavaBean.class);
    JavaBean result = this.binder.bind("", target).get();
    assertThat(result.getValue()).isEqualTo("hello");
  }

  @Test
  void bindOrCreateWhenBindSuccessfulShouldReturnBoundValue() {
    this.sources.add(new MockConfigurationPropertySource("foo.value", "bar"));
    JavaBean result = this.binder.bindOrCreate("foo", Bindable.of(JavaBean.class));
    assertThat(result.getValue()).isEqualTo("bar");
    assertThat(result.getItems()).isEmpty();
  }

  @Test
  void bindOrCreateWhenUnboundShouldReturnCreatedValue() {
    JavaBean value = this.binder.bindOrCreate("foo", Bindable.of(JavaBean.class));
    assertThat(value).isNotNull();
    assertThat(value).isInstanceOf(JavaBean.class);
  }

  @Test
  void bindToJavaBeanWhenHandlerOnStartReturnsNullShouldReturnUnbound() { // gh-18129
    this.sources.add(new MockConfigurationPropertySource("foo.value", "bar"));
    BindResult<JavaBean> result = this.binder.bind("foo", Bindable.of(JavaBean.class), new BindHandler() {

      @Override
      public <T> Bindable<T> onStart(ConfigurationPropertyName name, Bindable<T> target, BindContext context) {
        return null;
      }

    });
    assertThat(result.isBound()).isFalse();
  }

  @Test
  void bindToJavaBeanWithPublicConstructor() {
    Bindable<JavaBeanWithPublicConstructor> bindable = Bindable.of(JavaBeanWithPublicConstructor.class);
    JavaBeanWithPublicConstructor result = bindToJavaBeanWithPublicConstructor(bindable);
    assertThat(result.getValue()).isEqualTo("constructor");
  }

  @Test
  void bindToJavaBeanWithPublicConstructorWhenHasBindRestriction() {
    Bindable<JavaBeanWithPublicConstructor> bindable = Bindable.of(JavaBeanWithPublicConstructor.class)
            .withBindRestrictions(BindRestriction.NO_DIRECT_PROPERTY);
    JavaBeanWithPublicConstructor result = bindToJavaBeanWithPublicConstructor(bindable);
    assertThat(result.getValue()).isEqualTo("setter");
  }

  private JavaBeanWithPublicConstructor bindToJavaBeanWithPublicConstructor(
          Bindable<JavaBeanWithPublicConstructor> bindable) {
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("foo", "constructor");
    source.put("foo.value", "setter");
    this.sources.add(source);
    return this.binder.bindOrCreate("foo", bindable);
  }

  static class JavaBean {

    private String value;

    private List<String> items = Collections.emptyList();

    String getValue() {
      return this.value;
    }

    void setValue(String value) {
      this.value = value;
    }

    List<String> getItems() {
      return this.items;
    }

  }

  static class NestedJavaBean {

    private DefaultValuesBean valuesBean = new DefaultValuesBean();

    DefaultValuesBean getValuesBean() {
      return this.valuesBean;
    }

    void setValuesBean(DefaultValuesBean valuesBean) {
      this.valuesBean = valuesBean;
    }

  }

  static class DefaultValuesBean {

    private String value = "hello";

    private List<String> items = Collections.emptyList();

    String getValue() {
      return this.value;
    }

    void setValue(String value) {
      this.value = value;
    }

    List<String> getItems() {
      return this.items;
    }

  }

  public enum ExampleEnum {

    FOO_BAR, BAR_BAZ, BAZ_BOO

  }

  @Validated
  static class ResourceBean {

    private Resource resource;

    Resource getResource() {
      return this.resource;
    }

    void setResource(Resource resource) {
      this.resource = resource;
    }

  }

  static class CycleBean1 {

    private CycleBean2 two;

    CycleBean2 getTwo() {
      return this.two;
    }

    void setTwo(CycleBean2 two) {
      this.two = two;
    }

  }

  static class CycleBean2 {

    private CycleBean1 one;

    CycleBean1 getOne() {
      return this.one;
    }

    void setOne(CycleBean1 one) {
      this.one = one;
    }

  }

  static class GenericBean<T> {

    private T bar;

    T getBar() {
      return this.bar;
    }

    void setBar(T bar) {
      this.bar = bar;
    }

  }

  static class JavaBeanPropertyEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(String text) {
      JavaBean value = new JavaBean();
      value.setValue(text);
      setValue(value);
    }

  }

}
