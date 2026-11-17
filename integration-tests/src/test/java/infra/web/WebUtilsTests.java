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
import infra.web.util.WebUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/22 23:23
 */
@SuppressWarnings("cast")
class WebUtilsTests {

  private final MockRequest request = new MockRequest();

  MockHttpContext context = new MockHttpContext(null, request, null);

  @Test
  void testIntParameter() throws RequestBindingException {
    request.addParameter("param1", "5");
    request.addParameter("param2", "e");
    request.addParameter("paramEmpty", "");

    Assertions.assertThat(WebUtils.getIntParameter(context, "param1")).isEqualTo(5);
    assertThat(WebUtils.getIntParameter(context, "param1", 6)).isEqualTo(5);
    assertThat(WebUtils.getRequiredIntParameter(context, "param1")).isEqualTo(5);

    assertThat(WebUtils.getIntParameter(context, "param2", 6)).isEqualTo(6);
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            WebUtils.getRequiredIntParameter(context, "param2"));

    assertThat(WebUtils.getIntParameter(context, "param3")).isNull();
    assertThat(WebUtils.getIntParameter(context, "param3", 6)).isEqualTo(6);
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            WebUtils.getRequiredIntParameter(context, "param3"));

    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            WebUtils.getRequiredIntParameter(context, "paramEmpty"));
  }

  @Test
  void testIntParameters() throws RequestBindingException {
    request.addParameter("param", "1", "2", "3");

    request.addParameter("param2", "1");
    request.addParameter("param2", "2");
    request.addParameter("param2", "bogus");

    int[] array = new int[] { 1, 2, 3 };
    int[] values = WebUtils.getRequiredIntParameters(context, "param");
    assertThat(values).hasSize(3);
    for (int i = 0; i < array.length; i++) {
      assertThat(array[i]).isEqualTo(values[i]);
    }

    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            WebUtils.getRequiredIntParameters(context, "param2"));
  }

  @Test
  void testLongParameter() throws RequestBindingException {
    request.addParameter("param1", "5");
    request.addParameter("param2", "e");
    request.addParameter("paramEmpty", "");

    assertThat(WebUtils.getLongParameter(context, "param1")).isEqualTo(Long.valueOf(5L));
    assertThat(WebUtils.getLongParameter(context, "param1", 6L)).isEqualTo(5L);
    assertThat(WebUtils.getRequiredIntParameter(context, "param1")).isEqualTo(5L);

    assertThat(WebUtils.getLongParameter(context, "param2", 6L)).isEqualTo(6L);
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            WebUtils.getRequiredLongParameter(context, "param2"));

    assertThat(WebUtils.getLongParameter(context, "param3")).isNull();
    assertThat(WebUtils.getLongParameter(context, "param3", 6L)).isEqualTo(6L);
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            WebUtils.getRequiredLongParameter(context, "param3"));

    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            WebUtils.getRequiredLongParameter(context, "paramEmpty"));
  }

  @Test
  void testLongParameters() throws RequestBindingException {
    request.setParameter("param", new String[] { "1", "2", "3" });

    request.setParameter("param2", "0");
    request.setParameter("param2", "1");
    request.addParameter("param2", "2");
    request.addParameter("param2", "bogus");

    long[] values = WebUtils.getRequiredLongParameters(context, "param");
    assertThat(values).containsExactly(1, 2, 3);

    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            WebUtils.getRequiredLongParameters(context, "param2"));

    context.setParameter("param2", "1", "2");
    values = WebUtils.getRequiredLongParameters(context, "param2");
    assertThat(values).containsExactly(1, 2);

    context.removeParameter("param2");
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            WebUtils.getRequiredLongParameters(context, "param2"));
  }

  @Test
  void testFloatParameter() throws RequestBindingException {
    request.addParameter("param1", "5.5");
    request.addParameter("param2", "e");
    request.addParameter("paramEmpty", "");

    assertThat(WebUtils.getFloatParameter(context, "param1")).isEqualTo(Float.valueOf(5.5f));
    assertThat(WebUtils.getFloatParameter(context, "param1", 6.5f) == 5.5f).isTrue();
    assertThat(WebUtils.getRequiredFloatParameter(context, "param1") == 5.5f).isTrue();

    assertThat(WebUtils.getFloatParameter(context, "param2", 6.5f) == 6.5f).isTrue();
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            WebUtils.getRequiredFloatParameter(context, "param2"));

    assertThat(WebUtils.getFloatParameter(context, "param3")).isNull();
    assertThat(WebUtils.getFloatParameter(context, "param3", 6.5f) == 6.5f).isTrue();
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            WebUtils.getRequiredFloatParameter(context, "param3"));

    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            WebUtils.getRequiredFloatParameter(context, "paramEmpty"));
  }

  @Test
  void testFloatParameters() throws RequestBindingException {
    request.addParameter("param", new String[] { "1.5", "2.5", "3" });

    request.addParameter("param2", "1.5");
    request.addParameter("param2", "2");
    request.addParameter("param2", "bogus");

    float[] values = WebUtils.getRequiredFloatParameters(context, "param");
    assertThat(values).containsExactly(1.5F, 2.5F, 3F);

    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            WebUtils.getRequiredFloatParameters(context, "param2"));
  }

  @Test
  void testDoubleParameter() throws RequestBindingException {
    request.addParameter("param1", "5.5");
    request.addParameter("param2", "e");
    request.addParameter("paramEmpty", "");

    assertThat(WebUtils.getDoubleParameter(context, "param1")).isEqualTo(Double.valueOf(5.5));
    assertThat(WebUtils.getDoubleParameter(context, "param1", 6.5) == 5.5).isTrue();
    assertThat(WebUtils.getRequiredDoubleParameter(context, "param1") == 5.5).isTrue();

    assertThat(WebUtils.getDoubleParameter(context, "param2", 6.5) == 6.5).isTrue();
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            WebUtils.getRequiredDoubleParameter(context, "param2"));

    assertThat(WebUtils.getDoubleParameter(context, "param3")).isNull();
    assertThat(WebUtils.getDoubleParameter(context, "param3", 6.5) == 6.5).isTrue();
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            WebUtils.getRequiredDoubleParameter(context, "param3"));

    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            WebUtils.getRequiredDoubleParameter(context, "paramEmpty"));
  }

  @Test
  void testDoubleParameters() throws RequestBindingException {
    request.addParameter("param", new String[] { "1.5", "2.5", "3" });

    request.addParameter("param2", "1.5");
    request.addParameter("param2", "2");
    request.addParameter("param2", "bogus");

    double[] values = WebUtils.getRequiredDoubleParameters(context, "param");
    assertThat(values).containsExactly(1.5, 2.5, 3);
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            WebUtils.getRequiredDoubleParameters(context, "param2"));
  }

  @Test
  void testBooleanParameter() throws RequestBindingException {
    request.addParameter("param1", "true");
    request.addParameter("param2", "e");
    request.addParameter("param4", "yes");
    request.addParameter("param5", "1");
    request.addParameter("paramEmpty", "");

    assertThat(WebUtils.getBooleanParameter(context, "param1").equals(Boolean.TRUE)).isTrue();
    assertThat(WebUtils.getBooleanParameter(context, "param1", false)).isTrue();
    assertThat(WebUtils.getRequiredBooleanParameter(context, "param1")).isTrue();

    assertThat(WebUtils.getBooleanParameter(context, "param2", true)).isFalse();
    assertThat(WebUtils.getRequiredBooleanParameter(context, "param2")).isFalse();

    assertThat(WebUtils.getBooleanParameter(context, "param3")).isNull();
    assertThat(WebUtils.getBooleanParameter(context, "param3", true)).isTrue();
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            WebUtils.getRequiredBooleanParameter(context, "param3"));

    assertThat(WebUtils.getBooleanParameter(context, "param4", false)).isTrue();
    assertThat(WebUtils.getRequiredBooleanParameter(context, "param4")).isTrue();

    assertThat(WebUtils.getBooleanParameter(context, "param5", false)).isTrue();
    assertThat(WebUtils.getRequiredBooleanParameter(context, "param5")).isTrue();
    assertThat(WebUtils.getRequiredBooleanParameter(context, "paramEmpty")).isFalse();
  }

  @Test
  void testBooleanParameters() throws RequestBindingException {
    request.addParameter("param", new String[] { "true", "yes", "off", "1", "bogus" });

    request.addParameter("param2", "false");
    request.addParameter("param2", "true");
    request.addParameter("param2", "");

    boolean[] array = new boolean[] { true, true, false, true, false };
    boolean[] values = WebUtils.getRequiredBooleanParameters(context, "param");
    assertThat(array).hasSameSizeAs(values);
    for (int i = 0; i < array.length; i++) {
      assertThat(array[i]).isEqualTo(values[i]);
    }

    array = new boolean[] { false, true, false };
    values = WebUtils.getRequiredBooleanParameters(context, "param2");
    assertThat(array).hasSameSizeAs(values);
    for (int i = 0; i < array.length; i++) {
      assertThat(array[i]).isEqualTo(values[i]);
    }
  }

  @Test
  void testStringParameter() throws RequestBindingException {
    request.addParameter("param1", "str");
    request.addParameter("paramEmpty", "");

    assertThat(WebUtils.getStringParameter(context, "param1")).isEqualTo("str");
    assertThat(WebUtils.getStringParameter(context, "param1", "string")).isEqualTo("str");
    assertThat(WebUtils.getRequiredStringParameter(context, "param1")).isEqualTo("str");

    assertThat(WebUtils.getStringParameter(context, "param3")).isNull();
    assertThat(WebUtils.getStringParameter(context, "param3", "string")).isEqualTo("string");
    assertThat(WebUtils.getStringParameter(context, "param3", null)).isNull();
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            WebUtils.getRequiredStringParameter(context, "param3"));

    assertThat(WebUtils.getStringParameter(context, "paramEmpty")).isEmpty();
    assertThat(WebUtils.getRequiredStringParameter(context, "paramEmpty")).isEmpty();
  }

  @Test
  void getBeanWithNameAndClassShouldReturnBeanWhenAvailable() {
    MockHttpContext context = new MockHttpContext();

    assertThat((Object) WebUtils.getBean(context, "testBean", String.class)).isNull();
  }

  @Test
  void getLocaleResolverShouldReturnNullWhenNotAvailable() {
    MockHttpContext context = new MockHttpContext();

    LocaleResolver localeResolver = WebUtils.getLocaleResolver(context);

    assertThat(localeResolver).isNull();
  }

  @Test
  void getTimeZoneShouldReturnNullWhenNoLocaleResolver() {
    MockHttpContext context = new MockHttpContext();

    TimeZone timeZone = WebUtils.getTimeZone(context);

    assertThat(timeZone).isNull();
  }

  @Test
  void getOutputRedirectModelShouldReturnNullWhenNotAvailable() {
    MockHttpContext context = new MockHttpContext();

    RedirectModel redirectModel = WebUtils.getOutputRedirectModel(context);

    assertThat((Object) redirectModel).isNull();
  }

  @Test
  void getRedirectModelManagerShouldReturnNullWhenNotAvailable() {
    MockHttpContext context = new MockHttpContext();

    RedirectModelManager manager = WebUtils.getRedirectModelManager(context);

    assertThat(manager).isNull();
  }

  @Test
  void saveRedirectModelShouldNotThrowExceptionWhenNoRedirectModel() {
    MockHttpContext context = new MockHttpContext();

    assertThatNoException().isThrownBy(() ->
            WebUtils.saveRedirectModel("http://example.com", context));
  }

  @Test
  void saveRedirectModelShouldNotThrowExceptionWhenNoManager() {
    MockHttpContext context = new MockHttpContext();
    context.setAttribute(RedirectModel.OUTPUT_ATTRIBUTE, new RedirectModel());

    assertThatNoException().isThrownBy(() ->
            WebUtils.saveRedirectModel("http://example.com", context));
  }

  @Test
  void getStringParameterShouldReturnNullWhenNotPresent() throws RequestBindingException {
    MockHttpContext context = new MockHttpContext();

    String value = WebUtils.getStringParameter(context, "nonexistent");

    assertThat(value).isNull();
  }

  @Test
  void getStringParameterShouldReturnValueWhenPresent() throws RequestBindingException {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("param", "testValue");

    String value = WebUtils.getStringParameter(context, "param");

    assertThat(value).isEqualTo("testValue");
  }

  @Test
  void getStringParameterWithDefaultShouldReturnDefaultWhenNotPresent() {
    MockHttpContext context = new MockHttpContext();

    String value = WebUtils.getStringParameter(context, "nonexistent", "defaultValue");

    assertThat(value).isEqualTo("defaultValue");
  }

  @Test
  void getStringParameterWithDefaultShouldReturnValueWhenPresent() {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("param", "testValue");

    String value = WebUtils.getStringParameter(context, "param", "defaultValue");

    assertThat(value).isEqualTo("testValue");
  }

  @Test
  void getStringParametersShouldReturnEmptyArrayWhenNotPresent() {
    MockHttpContext context = new MockHttpContext();

    String[] values = WebUtils.getStringParameters(context, "nonexistent");

    assertThat(values).isEmpty();
  }

  @Test
  void getStringParametersShouldReturnValuesWhenPresent() {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("param", "value1", "value2");

    String[] values = WebUtils.getStringParameters(context, "param");

    assertThat(values).containsExactly("value1", "value2");
  }

  @Test
  void getRequiredStringParameterShouldThrowExceptionWhenNotPresent() {
    MockHttpContext context = new MockHttpContext();

    assertThatExceptionOfType(MissingRequestParameterException.class)
            .isThrownBy(() -> WebUtils.getRequiredStringParameter(context, "nonexistent"))
            .withMessage("Required request parameter 'nonexistent' for method parameter type string is not present");
  }

  @Test
  void getRequiredStringParameterShouldReturnValueWhenPresent() throws RequestBindingException {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("param", "testValue");

    String value = WebUtils.getRequiredStringParameter(context, "param");

    assertThat(value).isEqualTo("testValue");
  }

  @Test
  void getRequiredStringParametersShouldThrowExceptionWhenNotPresent() {
    MockHttpContext context = new MockHttpContext();

    assertThatExceptionOfType(MissingRequestParameterException.class)
            .isThrownBy(() -> WebUtils.getRequiredStringParameters(context, "nonexistent"));
  }

  @Test
  void getRequiredStringParametersShouldReturnValuesWhenPresent() throws RequestBindingException {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("param", "value1", "value2");

    String[] values = WebUtils.getRequiredStringParameters(context, "param");

    assertThat(values).containsExactly("value1", "value2");
  }

  @Test
  void getFloatParameterShouldReturnNullWhenNotPresent() throws RequestBindingException {
    MockHttpContext context = new MockHttpContext();

    Float value = WebUtils.getFloatParameter(context, "nonexistent");

    assertThat(value).isNull();
  }

  @Test
  void getFloatParameterShouldReturnValueWhenPresent() throws RequestBindingException {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("param", "3.14");

    Float value = WebUtils.getFloatParameter(context, "param");

    assertThat(value).isEqualTo(3.14f);
  }

  @Test
  void getFloatParameterWithDefaultShouldReturnDefaultWhenNotPresent() {
    MockHttpContext context = new MockHttpContext();

    float value = WebUtils.getFloatParameter(context, "nonexistent", 2.71f);

    assertThat(value).isEqualTo(2.71f);
  }

  @Test
  void getFloatParameterWithDefaultShouldReturnValueWhenPresent() {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("param", "3.14");

    float value = WebUtils.getFloatParameter(context, "param", 2.71f);

    assertThat(value).isEqualTo(3.14f);
  }

  @Test
  void getFloatParametersShouldReturnEmptyArrayWhenNotPresent() {
    MockHttpContext context = new MockHttpContext();

    float[] values = WebUtils.getFloatParameters(context, "nonexistent");

    assertThat(values).isEmpty();
  }

  @Test
  void getFloatParametersShouldReturnValuesWhenPresent() {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("param", "1.1", "2.2", "3.3");

    float[] values = WebUtils.getFloatParameters(context, "param");

    assertThat(values).containsExactly(1.1f, 2.2f, 3.3f);
  }

  @Test
  void getRequiredFloatParameterShouldThrowExceptionWhenNotPresent() {
    MockHttpContext context = new MockHttpContext();

    assertThatExceptionOfType(MissingRequestParameterException.class)
            .isThrownBy(() -> WebUtils.getRequiredFloatParameter(context, "nonexistent"));
  }

  @Test
  void getRequiredFloatParameterShouldThrowExceptionWhenInvalidValue() {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("param", "invalid");

    assertThatExceptionOfType(RequestBindingException.class)
            .isThrownBy(() -> WebUtils.getRequiredFloatParameter(context, "param"));
  }

  @Test
  void getRequiredFloatParameterShouldReturnValueWhenPresent() throws RequestBindingException {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("param", "3.14");

    float value = WebUtils.getRequiredFloatParameter(context, "param");

    assertThat(value).isEqualTo(3.14f);
  }

  @Test
  void getRequiredFloatParametersShouldThrowExceptionWhenNotPresent() {
    MockHttpContext context = new MockHttpContext();

    assertThatExceptionOfType(MissingRequestParameterException.class)
            .isThrownBy(() -> WebUtils.getRequiredFloatParameters(context, "nonexistent"));
  }

  @Test
  void getRequiredFloatParametersShouldThrowExceptionWhenInvalidValue() {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("param", "1.1", "invalid", "3.3");

    assertThatExceptionOfType(RequestBindingException.class)
            .isThrownBy(() -> WebUtils.getRequiredFloatParameters(context, "param"));
  }

  @Test
  void getRequiredFloatParametersShouldReturnValuesWhenPresent() throws RequestBindingException {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("param", "1.1", "2.2", "3.3");

    float[] values = WebUtils.getRequiredFloatParameters(context, "param");

    assertThat(values).containsExactly(1.1f, 2.2f, 3.3f);
  }

  @Test
  void getDoubleParameterShouldReturnNullWhenNotPresent() throws RequestBindingException {
    MockHttpContext context = new MockHttpContext();

    Double value = WebUtils.getDoubleParameter(context, "nonexistent");

    assertThat(value).isNull();
  }

  @Test
  void getDoubleParameterShouldReturnValueWhenPresent() throws RequestBindingException {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("param", "3.14159");

    Double value = WebUtils.getDoubleParameter(context, "param");

    assertThat(value).isEqualTo(3.14159);
  }

  @Test
  void getDoubleParameterWithDefaultShouldReturnDefaultWhenNotPresent() {
    MockHttpContext context = new MockHttpContext();

    double value = WebUtils.getDoubleParameter(context, "nonexistent", 2.71828);

    assertThat(value).isEqualTo(2.71828);
  }

  @Test
  void getDoubleParameterWithDefaultShouldReturnValueWhenPresent() {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("param", "3.14159");

    double value = WebUtils.getDoubleParameter(context, "param", 2.71828);

    assertThat(value).isEqualTo(3.14159);
  }

  @Test
  void getDoubleParametersShouldReturnEmptyArrayWhenNotPresent() {
    MockHttpContext context = new MockHttpContext();

    double[] values = WebUtils.getDoubleParameters(context, "nonexistent");

    assertThat(values).isEmpty();
  }

  @Test
  void getDoubleParametersShouldReturnValuesWhenPresent() {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("param", "1.11", "2.22", "3.33");

    double[] values = WebUtils.getDoubleParameters(context, "param");

    assertThat(values).containsExactly(1.11, 2.22, 3.33);
  }

  @Test
  void getRequiredDoubleParameterShouldThrowExceptionWhenNotPresent() {
    MockHttpContext context = new MockHttpContext();

    assertThatExceptionOfType(MissingRequestParameterException.class)
            .isThrownBy(() -> WebUtils.getRequiredDoubleParameter(context, "nonexistent"));
  }

  @Test
  void getRequiredDoubleParameterShouldThrowExceptionWhenInvalidValue() {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("param", "invalid");

    assertThatExceptionOfType(RequestBindingException.class)
            .isThrownBy(() -> WebUtils.getRequiredDoubleParameter(context, "param"));
  }

  @Test
  void getRequiredDoubleParameterShouldReturnValueWhenPresent() throws RequestBindingException {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("param", "3.14159");

    double value = WebUtils.getRequiredDoubleParameter(context, "param");

    assertThat(value).isEqualTo(3.14159);
  }

  @Test
  void getRequiredDoubleParametersShouldThrowExceptionWhenNotPresent() {
    MockHttpContext context = new MockHttpContext();

    assertThatExceptionOfType(MissingRequestParameterException.class)
            .isThrownBy(() -> WebUtils.getRequiredDoubleParameters(context, "nonexistent"));
  }

  @Test
  void getRequiredDoubleParametersShouldThrowExceptionWhenInvalidValue() {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("param", "1.11", "invalid", "3.33");

    assertThatExceptionOfType(RequestBindingException.class)
            .isThrownBy(() -> WebUtils.getRequiredDoubleParameters(context, "param"));
  }

  @Test
  void getRequiredDoubleParametersShouldReturnValuesWhenPresent() throws RequestBindingException {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("param", "1.11", "2.22", "3.33");

    double[] values = WebUtils.getRequiredDoubleParameters(context, "param");

    assertThat(values).containsExactly(1.11, 2.22, 3.33);
  }

}
