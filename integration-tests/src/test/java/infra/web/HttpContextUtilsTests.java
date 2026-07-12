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

package infra.web;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.TimeZone;

import infra.web.mock.MockHttpContext;
import infra.web.mock.MockRequest;
import infra.web.bind.MissingRequestParameterException;
import infra.web.bind.RequestBindingException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/22 23:23
 */
@SuppressWarnings("cast")
class HttpContextUtilsTests {

  private final MockRequest request = new MockRequest();

  MockHttpContext context = new MockHttpContext(null, request, null);

  @Test
  void testIntParameter() throws RequestBindingException {
    request.addParameter("param1", "5");
    request.addParameter("param2", "e");
    request.addParameter("paramEmpty", "");

    Assertions.assertThat(HttpContextUtils.getIntParameter(context, "param1")).isEqualTo(5);
    assertThat(HttpContextUtils.getIntParameter(context, "param1", 6)).isEqualTo(5);
    assertThat(HttpContextUtils.getRequiredIntParameter(context, "param1")).isEqualTo(5);

    assertThat(HttpContextUtils.getIntParameter(context, "param2", 6)).isEqualTo(6);
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            HttpContextUtils.getRequiredIntParameter(context, "param2"));

    assertThat(HttpContextUtils.getIntParameter(context, "param3")).isNull();
    assertThat(HttpContextUtils.getIntParameter(context, "param3", 6)).isEqualTo(6);
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            HttpContextUtils.getRequiredIntParameter(context, "param3"));

    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            HttpContextUtils.getRequiredIntParameter(context, "paramEmpty"));
  }

  @Test
  void testIntParameters() throws RequestBindingException {
    request.addParameter("param", "1", "2", "3");

    request.addParameter("param2", "1");
    request.addParameter("param2", "2");
    request.addParameter("param2", "bogus");

    int[] array = new int[] { 1, 2, 3 };
    int[] values = HttpContextUtils.getRequiredIntParameters(context, "param");
    assertThat(values).hasSize(3);
    for (int i = 0; i < array.length; i++) {
      assertThat(array[i]).isEqualTo(values[i]);
    }

    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            HttpContextUtils.getRequiredIntParameters(context, "param2"));
  }

  @Test
  void testLongParameter() throws RequestBindingException {
    request.addParameter("param1", "5");
    request.addParameter("param2", "e");
    request.addParameter("paramEmpty", "");

    assertThat(HttpContextUtils.getLongParameter(context, "param1")).isEqualTo(Long.valueOf(5L));
    assertThat(HttpContextUtils.getLongParameter(context, "param1", 6L)).isEqualTo(5L);
    assertThat(HttpContextUtils.getRequiredIntParameter(context, "param1")).isEqualTo(5L);

    assertThat(HttpContextUtils.getLongParameter(context, "param2", 6L)).isEqualTo(6L);
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            HttpContextUtils.getRequiredLongParameter(context, "param2"));

    assertThat(HttpContextUtils.getLongParameter(context, "param3")).isNull();
    assertThat(HttpContextUtils.getLongParameter(context, "param3", 6L)).isEqualTo(6L);
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            HttpContextUtils.getRequiredLongParameter(context, "param3"));

    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            HttpContextUtils.getRequiredLongParameter(context, "paramEmpty"));
  }

  @Test
  void testLongParameters() throws RequestBindingException {
    request.setParameter("param", new String[] { "1", "2", "3" });

    request.setParameter("param2", "0");
    request.setParameter("param2", "1");
    request.addParameter("param2", "2");
    request.addParameter("param2", "bogus");

    long[] values = HttpContextUtils.getRequiredLongParameters(context, "param");
    assertThat(values).containsExactly(1, 2, 3);

    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            HttpContextUtils.getRequiredLongParameters(context, "param2"));

    context.setParameter("param2", "1", "2");
    values = HttpContextUtils.getRequiredLongParameters(context, "param2");
    assertThat(values).containsExactly(1, 2);

    context.removeParameter("param2");
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            HttpContextUtils.getRequiredLongParameters(context, "param2"));
  }

  @Test
  void testFloatParameter() throws RequestBindingException {
    request.addParameter("param1", "5.5");
    request.addParameter("param2", "e");
    request.addParameter("paramEmpty", "");

    assertThat(HttpContextUtils.getFloatParameter(context, "param1")).isEqualTo(Float.valueOf(5.5f));
    assertThat(HttpContextUtils.getFloatParameter(context, "param1", 6.5f) == 5.5f).isTrue();
    assertThat(HttpContextUtils.getRequiredFloatParameter(context, "param1") == 5.5f).isTrue();

    assertThat(HttpContextUtils.getFloatParameter(context, "param2", 6.5f) == 6.5f).isTrue();
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            HttpContextUtils.getRequiredFloatParameter(context, "param2"));

    assertThat(HttpContextUtils.getFloatParameter(context, "param3")).isNull();
    assertThat(HttpContextUtils.getFloatParameter(context, "param3", 6.5f) == 6.5f).isTrue();
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            HttpContextUtils.getRequiredFloatParameter(context, "param3"));

    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            HttpContextUtils.getRequiredFloatParameter(context, "paramEmpty"));
  }

  @Test
  void testFloatParameters() throws RequestBindingException {
    request.addParameter("param", new String[] { "1.5", "2.5", "3" });

    request.addParameter("param2", "1.5");
    request.addParameter("param2", "2");
    request.addParameter("param2", "bogus");

    float[] values = HttpContextUtils.getRequiredFloatParameters(context, "param");
    assertThat(values).containsExactly(1.5F, 2.5F, 3F);

    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            HttpContextUtils.getRequiredFloatParameters(context, "param2"));
  }

  @Test
  void testDoubleParameter() throws RequestBindingException {
    request.addParameter("param1", "5.5");
    request.addParameter("param2", "e");
    request.addParameter("paramEmpty", "");

    assertThat(HttpContextUtils.getDoubleParameter(context, "param1")).isEqualTo(Double.valueOf(5.5));
    assertThat(HttpContextUtils.getDoubleParameter(context, "param1", 6.5) == 5.5).isTrue();
    assertThat(HttpContextUtils.getRequiredDoubleParameter(context, "param1") == 5.5).isTrue();

    assertThat(HttpContextUtils.getDoubleParameter(context, "param2", 6.5) == 6.5).isTrue();
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            HttpContextUtils.getRequiredDoubleParameter(context, "param2"));

    assertThat(HttpContextUtils.getDoubleParameter(context, "param3")).isNull();
    assertThat(HttpContextUtils.getDoubleParameter(context, "param3", 6.5) == 6.5).isTrue();
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            HttpContextUtils.getRequiredDoubleParameter(context, "param3"));

    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            HttpContextUtils.getRequiredDoubleParameter(context, "paramEmpty"));
  }

  @Test
  void testDoubleParameters() throws RequestBindingException {
    request.addParameter("param", new String[] { "1.5", "2.5", "3" });

    request.addParameter("param2", "1.5");
    request.addParameter("param2", "2");
    request.addParameter("param2", "bogus");

    double[] values = HttpContextUtils.getRequiredDoubleParameters(context, "param");
    assertThat(values).containsExactly(1.5, 2.5, 3);
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            HttpContextUtils.getRequiredDoubleParameters(context, "param2"));
  }

  @Test
  void testBooleanParameter() throws RequestBindingException {
    request.addParameter("param1", "true");
    request.addParameter("param2", "e");
    request.addParameter("param4", "yes");
    request.addParameter("param5", "1");
    request.addParameter("paramEmpty", "");

    assertThat(HttpContextUtils.getBooleanParameter(context, "param1").equals(Boolean.TRUE)).isTrue();
    assertThat(HttpContextUtils.getBooleanParameter(context, "param1", false)).isTrue();
    assertThat(HttpContextUtils.getRequiredBooleanParameter(context, "param1")).isTrue();

    assertThat(HttpContextUtils.getBooleanParameter(context, "param2", true)).isFalse();
    assertThat(HttpContextUtils.getRequiredBooleanParameter(context, "param2")).isFalse();

    assertThat(HttpContextUtils.getBooleanParameter(context, "param3")).isNull();
    assertThat(HttpContextUtils.getBooleanParameter(context, "param3", true)).isTrue();
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            HttpContextUtils.getRequiredBooleanParameter(context, "param3"));

    assertThat(HttpContextUtils.getBooleanParameter(context, "param4", false)).isTrue();
    assertThat(HttpContextUtils.getRequiredBooleanParameter(context, "param4")).isTrue();

    assertThat(HttpContextUtils.getBooleanParameter(context, "param5", false)).isTrue();
    assertThat(HttpContextUtils.getRequiredBooleanParameter(context, "param5")).isTrue();
    assertThat(HttpContextUtils.getRequiredBooleanParameter(context, "paramEmpty")).isFalse();
  }

  @Test
  void testBooleanParameters() throws RequestBindingException {
    request.addParameter("param", new String[] { "true", "yes", "off", "1", "bogus" });

    request.addParameter("param2", "false");
    request.addParameter("param2", "true");
    request.addParameter("param2", "");

    boolean[] array = new boolean[] { true, true, false, true, false };
    boolean[] values = HttpContextUtils.getRequiredBooleanParameters(context, "param");
    assertThat(array).hasSameSizeAs(values);
    for (int i = 0; i < array.length; i++) {
      assertThat(array[i]).isEqualTo(values[i]);
    }

    array = new boolean[] { false, true, false };
    values = HttpContextUtils.getRequiredBooleanParameters(context, "param2");
    assertThat(array).hasSameSizeAs(values);
    for (int i = 0; i < array.length; i++) {
      assertThat(array[i]).isEqualTo(values[i]);
    }
  }

  @Test
  void testStringParameter() throws RequestBindingException {
    request.addParameter("param1", "str");
    request.addParameter("paramEmpty", "");

    assertThat(HttpContextUtils.getStringParameter(context, "param1")).isEqualTo("str");
    assertThat(HttpContextUtils.getStringParameter(context, "param1", "string")).isEqualTo("str");
    assertThat(HttpContextUtils.getRequiredStringParameter(context, "param1")).isEqualTo("str");

    assertThat(HttpContextUtils.getStringParameter(context, "param3")).isNull();
    assertThat(HttpContextUtils.getStringParameter(context, "param3", "string")).isEqualTo("string");
    assertThat(HttpContextUtils.getStringParameter(context, "param3", null)).isNull();
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            HttpContextUtils.getRequiredStringParameter(context, "param3"));

    assertThat(HttpContextUtils.getStringParameter(context, "paramEmpty")).isEmpty();
    assertThat(HttpContextUtils.getRequiredStringParameter(context, "paramEmpty")).isEmpty();
  }

  @Test
  void getBeanWithNameAndClassShouldReturnBeanWhenAvailable() {
    MockHttpContext context = new MockHttpContext();

    assertThat((Object) HttpContextUtils.getBean(context, "testBean", String.class)).isNull();
  }

  @Test
  void getLocaleResolverShouldReturnNullWhenNotAvailable() {
    MockHttpContext context = new MockHttpContext();

    LocaleResolver localeResolver = HttpContextUtils.getLocaleResolver(context);

    assertThat(localeResolver).isNull();
  }

  @Test
  void getTimeZoneShouldReturnNullWhenNoLocaleResolver() {
    MockHttpContext context = new MockHttpContext();

    TimeZone timeZone = HttpContextUtils.getTimeZone(context);

    assertThat(timeZone).isNull();
  }

  @Test
  void getOutputRedirectModelShouldReturnNullWhenNotAvailable() {
    MockHttpContext context = new MockHttpContext();

    RedirectModel redirectModel = HttpContextUtils.getOutputRedirectModel(context);

    assertThat((Object) redirectModel).isNull();
  }

  @Test
  void getRedirectModelManagerShouldReturnNullWhenNotAvailable() {
    MockHttpContext context = new MockHttpContext();

    RedirectModelManager manager = HttpContextUtils.getRedirectModelManager(context);

    assertThat(manager).isNull();
  }

  @Test
  void saveRedirectModelShouldNotThrowExceptionWhenNoRedirectModel() {
    MockHttpContext context = new MockHttpContext();

    assertThatNoException().isThrownBy(() ->
            HttpContextUtils.saveRedirectModel("http://example.com", context));
  }

  @Test
  void saveRedirectModelShouldNotThrowExceptionWhenNoManager() {
    MockHttpContext context = new MockHttpContext();
    context.setAttribute(RedirectModel.OUTPUT_ATTRIBUTE, new RedirectModel());

    assertThatNoException().isThrownBy(() ->
            HttpContextUtils.saveRedirectModel("http://example.com", context));
  }

  @Test
  void getStringParameterShouldReturnNullWhenNotPresent() throws RequestBindingException {
    MockHttpContext context = new MockHttpContext();

    String value = HttpContextUtils.getStringParameter(context, "nonexistent");

    assertThat(value).isNull();
  }

  @Test
  void getStringParameterShouldReturnValueWhenPresent() throws RequestBindingException {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("param", "testValue");

    String value = HttpContextUtils.getStringParameter(context, "param");

    assertThat(value).isEqualTo("testValue");
  }

  @Test
  void getStringParameterWithDefaultShouldReturnDefaultWhenNotPresent() {
    MockHttpContext context = new MockHttpContext();

    String value = HttpContextUtils.getStringParameter(context, "nonexistent", "defaultValue");

    assertThat(value).isEqualTo("defaultValue");
  }

  @Test
  void getStringParameterWithDefaultShouldReturnValueWhenPresent() {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("param", "testValue");

    String value = HttpContextUtils.getStringParameter(context, "param", "defaultValue");

    assertThat(value).isEqualTo("testValue");
  }

  @Test
  void getStringParametersShouldReturnEmptyArrayWhenNotPresent() {
    MockHttpContext context = new MockHttpContext();

    String[] values = HttpContextUtils.getStringParameters(context, "nonexistent");

    assertThat(values).isEmpty();
  }

  @Test
  void getStringParametersShouldReturnValuesWhenPresent() {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("param", "value1", "value2");

    String[] values = HttpContextUtils.getStringParameters(context, "param");

    assertThat(values).containsExactly("value1", "value2");
  }

  @Test
  void getRequiredStringParameterShouldThrowExceptionWhenNotPresent() {
    MockHttpContext context = new MockHttpContext();

    assertThatExceptionOfType(MissingRequestParameterException.class)
            .isThrownBy(() -> HttpContextUtils.getRequiredStringParameter(context, "nonexistent"))
            .withMessage("Required request parameter 'nonexistent' for method parameter type string is not present");
  }

  @Test
  void getRequiredStringParameterShouldReturnValueWhenPresent() throws RequestBindingException {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("param", "testValue");

    String value = HttpContextUtils.getRequiredStringParameter(context, "param");

    assertThat(value).isEqualTo("testValue");
  }

  @Test
  void getRequiredStringParametersShouldThrowExceptionWhenNotPresent() {
    MockHttpContext context = new MockHttpContext();

    assertThatExceptionOfType(MissingRequestParameterException.class)
            .isThrownBy(() -> HttpContextUtils.getRequiredStringParameters(context, "nonexistent"));
  }

  @Test
  void getRequiredStringParametersShouldReturnValuesWhenPresent() throws RequestBindingException {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("param", "value1", "value2");

    String[] values = HttpContextUtils.getRequiredStringParameters(context, "param");

    assertThat(values).containsExactly("value1", "value2");
  }

  @Test
  void getFloatParameterShouldReturnNullWhenNotPresent() throws RequestBindingException {
    MockHttpContext context = new MockHttpContext();

    Float value = HttpContextUtils.getFloatParameter(context, "nonexistent");

    assertThat(value).isNull();
  }

  @Test
  void getFloatParameterShouldReturnValueWhenPresent() throws RequestBindingException {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("param", "3.14");

    Float value = HttpContextUtils.getFloatParameter(context, "param");

    assertThat(value).isEqualTo(3.14f);
  }

  @Test
  void getFloatParameterWithDefaultShouldReturnDefaultWhenNotPresent() {
    MockHttpContext context = new MockHttpContext();

    float value = HttpContextUtils.getFloatParameter(context, "nonexistent", 2.71f);

    assertThat(value).isEqualTo(2.71f);
  }

  @Test
  void getFloatParameterWithDefaultShouldReturnValueWhenPresent() {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("param", "3.14");

    float value = HttpContextUtils.getFloatParameter(context, "param", 2.71f);

    assertThat(value).isEqualTo(3.14f);
  }

  @Test
  void getFloatParametersShouldReturnEmptyArrayWhenNotPresent() {
    MockHttpContext context = new MockHttpContext();

    float[] values = HttpContextUtils.getFloatParameters(context, "nonexistent");

    assertThat(values).isEmpty();
  }

  @Test
  void getFloatParametersShouldReturnValuesWhenPresent() {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("param", "1.1", "2.2", "3.3");

    float[] values = HttpContextUtils.getFloatParameters(context, "param");

    assertThat(values).containsExactly(1.1f, 2.2f, 3.3f);
  }

  @Test
  void getRequiredFloatParameterShouldThrowExceptionWhenNotPresent() {
    MockHttpContext context = new MockHttpContext();

    assertThatExceptionOfType(MissingRequestParameterException.class)
            .isThrownBy(() -> HttpContextUtils.getRequiredFloatParameter(context, "nonexistent"));
  }

  @Test
  void getRequiredFloatParameterShouldThrowExceptionWhenInvalidValue() {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("param", "invalid");

    assertThatExceptionOfType(RequestBindingException.class)
            .isThrownBy(() -> HttpContextUtils.getRequiredFloatParameter(context, "param"));
  }

  @Test
  void getRequiredFloatParameterShouldReturnValueWhenPresent() throws RequestBindingException {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("param", "3.14");

    float value = HttpContextUtils.getRequiredFloatParameter(context, "param");

    assertThat(value).isEqualTo(3.14f);
  }

  @Test
  void getRequiredFloatParametersShouldThrowExceptionWhenNotPresent() {
    MockHttpContext context = new MockHttpContext();

    assertThatExceptionOfType(MissingRequestParameterException.class)
            .isThrownBy(() -> HttpContextUtils.getRequiredFloatParameters(context, "nonexistent"));
  }

  @Test
  void getRequiredFloatParametersShouldThrowExceptionWhenInvalidValue() {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("param", "1.1", "invalid", "3.3");

    assertThatExceptionOfType(RequestBindingException.class)
            .isThrownBy(() -> HttpContextUtils.getRequiredFloatParameters(context, "param"));
  }

  @Test
  void getRequiredFloatParametersShouldReturnValuesWhenPresent() throws RequestBindingException {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("param", "1.1", "2.2", "3.3");

    float[] values = HttpContextUtils.getRequiredFloatParameters(context, "param");

    assertThat(values).containsExactly(1.1f, 2.2f, 3.3f);
  }

  @Test
  void getDoubleParameterShouldReturnNullWhenNotPresent() throws RequestBindingException {
    MockHttpContext context = new MockHttpContext();

    Double value = HttpContextUtils.getDoubleParameter(context, "nonexistent");

    assertThat(value).isNull();
  }

  @Test
  void getDoubleParameterShouldReturnValueWhenPresent() throws RequestBindingException {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("param", "3.14159");

    Double value = HttpContextUtils.getDoubleParameter(context, "param");

    assertThat(value).isEqualTo(3.14159);
  }

  @Test
  void getDoubleParameterWithDefaultShouldReturnDefaultWhenNotPresent() {
    MockHttpContext context = new MockHttpContext();

    double value = HttpContextUtils.getDoubleParameter(context, "nonexistent", 2.71828);

    assertThat(value).isEqualTo(2.71828);
  }

  @Test
  void getDoubleParameterWithDefaultShouldReturnValueWhenPresent() {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("param", "3.14159");

    double value = HttpContextUtils.getDoubleParameter(context, "param", 2.71828);

    assertThat(value).isEqualTo(3.14159);
  }

  @Test
  void getDoubleParametersShouldReturnEmptyArrayWhenNotPresent() {
    MockHttpContext context = new MockHttpContext();

    double[] values = HttpContextUtils.getDoubleParameters(context, "nonexistent");

    assertThat(values).isEmpty();
  }

  @Test
  void getDoubleParametersShouldReturnValuesWhenPresent() {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("param", "1.11", "2.22", "3.33");

    double[] values = HttpContextUtils.getDoubleParameters(context, "param");

    assertThat(values).containsExactly(1.11, 2.22, 3.33);
  }

  @Test
  void getRequiredDoubleParameterShouldThrowExceptionWhenNotPresent() {
    MockHttpContext context = new MockHttpContext();

    assertThatExceptionOfType(MissingRequestParameterException.class)
            .isThrownBy(() -> HttpContextUtils.getRequiredDoubleParameter(context, "nonexistent"));
  }

  @Test
  void getRequiredDoubleParameterShouldThrowExceptionWhenInvalidValue() {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("param", "invalid");

    assertThatExceptionOfType(RequestBindingException.class)
            .isThrownBy(() -> HttpContextUtils.getRequiredDoubleParameter(context, "param"));
  }

  @Test
  void getRequiredDoubleParameterShouldReturnValueWhenPresent() throws RequestBindingException {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("param", "3.14159");

    double value = HttpContextUtils.getRequiredDoubleParameter(context, "param");

    assertThat(value).isEqualTo(3.14159);
  }

  @Test
  void getRequiredDoubleParametersShouldThrowExceptionWhenNotPresent() {
    MockHttpContext context = new MockHttpContext();

    assertThatExceptionOfType(MissingRequestParameterException.class)
            .isThrownBy(() -> HttpContextUtils.getRequiredDoubleParameters(context, "nonexistent"));
  }

  @Test
  void getRequiredDoubleParametersShouldThrowExceptionWhenInvalidValue() {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("param", "1.11", "invalid", "3.33");

    assertThatExceptionOfType(RequestBindingException.class)
            .isThrownBy(() -> HttpContextUtils.getRequiredDoubleParameters(context, "param"));
  }

  @Test
  void getRequiredDoubleParametersShouldReturnValuesWhenPresent() throws RequestBindingException {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("param", "1.11", "2.22", "3.33");

    double[] values = HttpContextUtils.getRequiredDoubleParameters(context, "param");

    assertThat(values).containsExactly(1.11, 2.22, 3.33);
  }

}
