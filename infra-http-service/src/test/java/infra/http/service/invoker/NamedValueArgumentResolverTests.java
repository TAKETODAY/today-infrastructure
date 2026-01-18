/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.http.service.invoker;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import infra.core.MethodParameter;
import infra.core.annotation.AliasFor;
import infra.format.annotation.DateTimeFormat;
import infra.format.support.DefaultFormattingConversionService;
import infra.http.service.annotation.GetExchange;
import infra.lang.Constant;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import infra.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link AbstractNamedValueArgumentResolver} through a
 * {@link TestValue @TestValue} annotation and {@link TestNamedValueArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
class NamedValueArgumentResolverTests {

  private final TestExchangeAdapter client = new TestExchangeAdapter();

  private final TestNamedValueArgumentResolver argumentResolver = new TestNamedValueArgumentResolver();

  private final Service service = HttpServiceProxyFactory.forAdapter(this.client)
          .customArgumentResolver(this.argumentResolver)
          .build()
          .createClient(Service.class);

  @Test
  void stringTestValue() {
    this.service.executeString("test");
    assertTestValue("value", "test");
  }

  @Test
  void dateTestValue() {
    this.service.executeDate(LocalDate.of(2022, 9, 16));
    assertTestValue("value", "2022-09-16");
  }

  @Test
  void dateNullValue() {
    this.service.executeDate(null);
    assertTestValue("value");
  }

  @Test
  void objectTestValue() {
    this.service.execute(Boolean.TRUE);
    assertTestValue("value", "true");
  }

  @Test
  void listTestValue() {
    this.service.executeList(List.of("test1", Boolean.TRUE, "test3"));
    assertTestValue("testValues", "test1", "true", "test3");
  }

  @Test
  void arrayTestValue() {
    this.service.executeArray("test1", Boolean.FALSE, "test3");
    assertTestValue("testValues", "test1", "false", "test3");
  }

  @Test
  void namedTestValue() {
    this.service.executeNamed("test");
    assertTestValue("valueRenamed", "test");
  }

  @SuppressWarnings("ConstantConditions")
  @Test
  void nullTestValueRequired() {
    assertThatIllegalArgumentException().isThrownBy(() -> this.service.executeString(null));
  }

  @Test
  void nullTestValueNotRequired() {
    this.service.executeNotRequired(null);
    assertTestValue("value");
  }

  @Test
  void nullTestValueWithDefaultValue() {
    this.service.executeWithDefaultValue(null);
    assertTestValue("value", "default");
  }

  @Test
  void nullTestValue() {
    this.service.executeOptional((Object) null);
    assertTestValue("value");
  }

  @Test
  void nullableStringTestValue() {
    this.service.executeOptional("test");
    assertTestValue("value", "test");
  }

  @Test
  void optionalStringTestValue() {
    this.service.executeOptional(Optional.of("test"));
    assertTestValue("value", "test");
  }

  @Test
  void optionalObjectTestValue() {
    this.service.executeOptional(Optional.of(true));
    assertTestValue("value", "true");
  }

  @Test
  void optionalEmpty() {
    this.service.executeOptional(Optional.empty());
    assertTestValue("value");
  }

  @Test
  void optionalEmpthyWithDefaultValue() {
    this.service.executeOptionalWithDefaultValue(Optional.empty());
    assertTestValue("value", "default");
  }

  @Test
  void mapOfTestValues() {
    this.service.executeMap(Map.of("value1", "true", "value2", "false"));
    assertTestValue("value1", "true");
    assertTestValue("value2", "false");
  }

  @Test
  void mapOfTestValuesIsNull() {
    assertThatIllegalArgumentException().isThrownBy(() -> this.service.executeMap(null));
  }

  @Test
  void mapOfTestValuesHasOptionalValue() {
    this.service.executeMapWithOptionalValue(Map.of("value", Optional.of("test")));
    assertTestValue("value", "test");
  }

  private void assertTestValue(String key, String... values) {
    List<String> actualValues = this.argumentResolver.getTestValues().get(key);
    if (ObjectUtils.isEmpty(values)) {
      assertThat(actualValues).isNull();
    }
    else {
      assertThat(actualValues).containsOnly(values);
    }
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private interface Service {

    @GetExchange
    void executeString(@TestValue String value);

    @GetExchange
    void executeDate(@Nullable @TestValue(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate value);

    @GetExchange
    void execute(@TestValue Object value);

    @GetExchange
    void executeList(@TestValue List<Object> testValues);

    @GetExchange
    void executeArray(@TestValue Object... testValues);

    @GetExchange
    void executeNamed(@TestValue(name = "valueRenamed") String value);

    @GetExchange
    void executeNotRequired(@Nullable @TestValue(required = false) String value);

    @GetExchange
    void executeWithDefaultValue(@Nullable @TestValue(defaultValue = "default") String value);

    @GetExchange
    void executeOptional(@TestValue @Nullable Object value);

    @GetExchange
    void executeOptional(@TestValue Optional<Object> value);

    @GetExchange
    void executeOptionalWithDefaultValue(@TestValue(defaultValue = "default") @Nullable Object value);

    @GetExchange
    void executeMap(@Nullable @TestValue Map<String, String> value);

    @GetExchange
    void executeMapWithOptionalValue(@TestValue Map<String, Optional<String>> values);

  }

  private static class TestNamedValueArgumentResolver extends AbstractNamedValueArgumentResolver {

    private final MultiValueMap<String, String> testValues = new LinkedMultiValueMap<>();

    TestNamedValueArgumentResolver() {
      super(new DefaultFormattingConversionService());
    }

    public MultiValueMap<String, String> getTestValues() {
      return this.testValues;
    }

    @Override
    protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
      TestValue annot = parameter.getParameterAnnotation(TestValue.class);
      return (annot == null ? null :
              new NamedValueInfo(annot.name(), annot.required(), annot.defaultValue(), "test value", true));
    }

    @Override
    protected void addRequestValue(String name, Object value, MethodParameter parameter, HttpRequestValues.Builder requestValues) {
      this.testValues.add(name, (String) value);
    }
  }

  @Target(ElementType.PARAMETER)
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  @interface TestValue {

    @AliasFor("name")
    String value() default "";

    @AliasFor("value")
    String name() default "";

    boolean required() default true;

    String defaultValue() default Constant.DEFAULT_NONE;

  }

}
