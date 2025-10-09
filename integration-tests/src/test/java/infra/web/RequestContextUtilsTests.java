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

package infra.web;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.TimeZone;

import infra.mock.web.HttpMockRequestImpl;
import infra.session.Session;
import infra.session.SessionManager;
import infra.web.bind.MissingRequestParameterException;
import infra.web.bind.RequestBindingException;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/22 23:23
 */
@SuppressWarnings("cast")
class RequestContextUtilsTests {

  private final HttpMockRequestImpl request = new HttpMockRequestImpl();

  MockRequestContext context = new MockRequestContext(null, request, null);

  @Test
  void testIntParameter() throws RequestBindingException {
    request.addParameter("param1", "5");
    request.addParameter("param2", "e");
    request.addParameter("paramEmpty", "");

    Assertions.assertThat(RequestContextUtils.getIntParameter(context, "param1")).isEqualTo(5);
    assertThat(RequestContextUtils.getIntParameter(context, "param1", 6)).isEqualTo(5);
    assertThat(RequestContextUtils.getRequiredIntParameter(context, "param1")).isEqualTo(5);

    assertThat(RequestContextUtils.getIntParameter(context, "param2", 6)).isEqualTo(6);
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            RequestContextUtils.getRequiredIntParameter(context, "param2"));

    assertThat(RequestContextUtils.getIntParameter(context, "param3")).isNull();
    assertThat(RequestContextUtils.getIntParameter(context, "param3", 6)).isEqualTo(6);
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            RequestContextUtils.getRequiredIntParameter(context, "param3"));

    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            RequestContextUtils.getRequiredIntParameter(context, "paramEmpty"));
  }

  @Test
  void testIntParameters() throws RequestBindingException {
    request.addParameter("param", "1", "2", "3");

    request.addParameter("param2", "1");
    request.addParameter("param2", "2");
    request.addParameter("param2", "bogus");

    int[] array = new int[] { 1, 2, 3 };
    int[] values = RequestContextUtils.getRequiredIntParameters(context, "param");
    assertThat(values).hasSize(3);
    for (int i = 0; i < array.length; i++) {
      assertThat(array[i]).isEqualTo(values[i]);
    }

    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            RequestContextUtils.getRequiredIntParameters(context, "param2"));
  }

  @Test
  void testLongParameter() throws RequestBindingException {
    request.addParameter("param1", "5");
    request.addParameter("param2", "e");
    request.addParameter("paramEmpty", "");

    assertThat(RequestContextUtils.getLongParameter(context, "param1")).isEqualTo(Long.valueOf(5L));
    assertThat(RequestContextUtils.getLongParameter(context, "param1", 6L)).isEqualTo(5L);
    assertThat(RequestContextUtils.getRequiredIntParameter(context, "param1")).isEqualTo(5L);

    assertThat(RequestContextUtils.getLongParameter(context, "param2", 6L)).isEqualTo(6L);
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            RequestContextUtils.getRequiredLongParameter(context, "param2"));

    assertThat(RequestContextUtils.getLongParameter(context, "param3")).isNull();
    assertThat(RequestContextUtils.getLongParameter(context, "param3", 6L)).isEqualTo(6L);
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            RequestContextUtils.getRequiredLongParameter(context, "param3"));

    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            RequestContextUtils.getRequiredLongParameter(context, "paramEmpty"));
  }

  @Test
  void testLongParameters() throws RequestBindingException {
    request.setParameter("param", new String[] { "1", "2", "3" });

    request.setParameter("param2", "0");
    request.setParameter("param2", "1");
    request.addParameter("param2", "2");
    request.addParameter("param2", "bogus");

    long[] values = RequestContextUtils.getRequiredLongParameters(context, "param");
    assertThat(values).containsExactly(1, 2, 3);

    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            RequestContextUtils.getRequiredLongParameters(context, "param2"));

    context.setParameter("param2", "1", "2");
    values = RequestContextUtils.getRequiredLongParameters(context, "param2");
    assertThat(values).containsExactly(1, 2);

    context.removeParameter("param2");
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            RequestContextUtils.getRequiredLongParameters(context, "param2"));
  }

  @Test
  void testFloatParameter() throws RequestBindingException {
    request.addParameter("param1", "5.5");
    request.addParameter("param2", "e");
    request.addParameter("paramEmpty", "");

    assertThat(RequestContextUtils.getFloatParameter(context, "param1")).isEqualTo(Float.valueOf(5.5f));
    assertThat(RequestContextUtils.getFloatParameter(context, "param1", 6.5f) == 5.5f).isTrue();
    assertThat(RequestContextUtils.getRequiredFloatParameter(context, "param1") == 5.5f).isTrue();

    assertThat(RequestContextUtils.getFloatParameter(context, "param2", 6.5f) == 6.5f).isTrue();
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            RequestContextUtils.getRequiredFloatParameter(context, "param2"));

    assertThat(RequestContextUtils.getFloatParameter(context, "param3")).isNull();
    assertThat(RequestContextUtils.getFloatParameter(context, "param3", 6.5f) == 6.5f).isTrue();
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            RequestContextUtils.getRequiredFloatParameter(context, "param3"));

    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            RequestContextUtils.getRequiredFloatParameter(context, "paramEmpty"));
  }

  @Test
  void testFloatParameters() throws RequestBindingException {
    request.addParameter("param", new String[] { "1.5", "2.5", "3" });

    request.addParameter("param2", "1.5");
    request.addParameter("param2", "2");
    request.addParameter("param2", "bogus");

    float[] values = RequestContextUtils.getRequiredFloatParameters(context, "param");
    assertThat(values).containsExactly(1.5F, 2.5F, 3F);

    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            RequestContextUtils.getRequiredFloatParameters(context, "param2"));
  }

  @Test
  void testDoubleParameter() throws RequestBindingException {
    request.addParameter("param1", "5.5");
    request.addParameter("param2", "e");
    request.addParameter("paramEmpty", "");

    assertThat(RequestContextUtils.getDoubleParameter(context, "param1")).isEqualTo(Double.valueOf(5.5));
    assertThat(RequestContextUtils.getDoubleParameter(context, "param1", 6.5) == 5.5).isTrue();
    assertThat(RequestContextUtils.getRequiredDoubleParameter(context, "param1") == 5.5).isTrue();

    assertThat(RequestContextUtils.getDoubleParameter(context, "param2", 6.5) == 6.5).isTrue();
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            RequestContextUtils.getRequiredDoubleParameter(context, "param2"));

    assertThat(RequestContextUtils.getDoubleParameter(context, "param3")).isNull();
    assertThat(RequestContextUtils.getDoubleParameter(context, "param3", 6.5) == 6.5).isTrue();
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            RequestContextUtils.getRequiredDoubleParameter(context, "param3"));

    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            RequestContextUtils.getRequiredDoubleParameter(context, "paramEmpty"));
  }

  @Test
  void testDoubleParameters() throws RequestBindingException {
    request.addParameter("param", new String[] { "1.5", "2.5", "3" });

    request.addParameter("param2", "1.5");
    request.addParameter("param2", "2");
    request.addParameter("param2", "bogus");

    double[] values = RequestContextUtils.getRequiredDoubleParameters(context, "param");
    assertThat(values).containsExactly(1.5, 2.5, 3);
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            RequestContextUtils.getRequiredDoubleParameters(context, "param2"));
  }

  @Test
  void testBooleanParameter() throws RequestBindingException {
    request.addParameter("param1", "true");
    request.addParameter("param2", "e");
    request.addParameter("param4", "yes");
    request.addParameter("param5", "1");
    request.addParameter("paramEmpty", "");

    assertThat(RequestContextUtils.getBooleanParameter(context, "param1").equals(Boolean.TRUE)).isTrue();
    assertThat(RequestContextUtils.getBooleanParameter(context, "param1", false)).isTrue();
    assertThat(RequestContextUtils.getRequiredBooleanParameter(context, "param1")).isTrue();

    assertThat(RequestContextUtils.getBooleanParameter(context, "param2", true)).isFalse();
    assertThat(RequestContextUtils.getRequiredBooleanParameter(context, "param2")).isFalse();

    assertThat(RequestContextUtils.getBooleanParameter(context, "param3")).isNull();
    assertThat(RequestContextUtils.getBooleanParameter(context, "param3", true)).isTrue();
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            RequestContextUtils.getRequiredBooleanParameter(context, "param3"));

    assertThat(RequestContextUtils.getBooleanParameter(context, "param4", false)).isTrue();
    assertThat(RequestContextUtils.getRequiredBooleanParameter(context, "param4")).isTrue();

    assertThat(RequestContextUtils.getBooleanParameter(context, "param5", false)).isTrue();
    assertThat(RequestContextUtils.getRequiredBooleanParameter(context, "param5")).isTrue();
    assertThat(RequestContextUtils.getRequiredBooleanParameter(context, "paramEmpty")).isFalse();
  }

  @Test
  void testBooleanParameters() throws RequestBindingException {
    request.addParameter("param", new String[] { "true", "yes", "off", "1", "bogus" });

    request.addParameter("param2", "false");
    request.addParameter("param2", "true");
    request.addParameter("param2", "");

    boolean[] array = new boolean[] { true, true, false, true, false };
    boolean[] values = RequestContextUtils.getRequiredBooleanParameters(context, "param");
    assertThat(array).hasSameSizeAs(values);
    for (int i = 0; i < array.length; i++) {
      assertThat(array[i]).isEqualTo(values[i]);
    }

    array = new boolean[] { false, true, false };
    values = RequestContextUtils.getRequiredBooleanParameters(context, "param2");
    assertThat(array).hasSameSizeAs(values);
    for (int i = 0; i < array.length; i++) {
      assertThat(array[i]).isEqualTo(values[i]);
    }
  }

  @Test
  void testStringParameter() throws RequestBindingException {
    request.addParameter("param1", "str");
    request.addParameter("paramEmpty", "");

    assertThat(RequestContextUtils.getStringParameter(context, "param1")).isEqualTo("str");
    assertThat(RequestContextUtils.getStringParameter(context, "param1", "string")).isEqualTo("str");
    assertThat(RequestContextUtils.getRequiredStringParameter(context, "param1")).isEqualTo("str");

    assertThat(RequestContextUtils.getStringParameter(context, "param3")).isNull();
    assertThat(RequestContextUtils.getStringParameter(context, "param3", "string")).isEqualTo("string");
    assertThat(RequestContextUtils.getStringParameter(context, "param3", null)).isNull();
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            RequestContextUtils.getRequiredStringParameter(context, "param3"));

    assertThat(RequestContextUtils.getStringParameter(context, "paramEmpty")).isEmpty();
    assertThat(RequestContextUtils.getRequiredStringParameter(context, "paramEmpty")).isEmpty();
  }

  @Test
  void getBeanWithClassShouldReturnBeanWhenAvailable() {
    // Setup mock bean factory and register a bean
    MockRequestContext context = new MockRequestContext();

    assertThat((Object) RequestContextUtils.getBean(context, String.class)).isNull();
  }

  @Test
  void getBeanWithStringNameShouldReturnBeanWhenAvailable() {
    MockRequestContext context = new MockRequestContext();

    assertThat((Object) RequestContextUtils.getBean(context, "testBean")).isNull();
  }

  @Test
  void getBeanWithNameAndClassShouldReturnBeanWhenAvailable() {
    MockRequestContext context = new MockRequestContext();

    assertThat((Object) RequestContextUtils.getBean(context, "testBean", String.class)).isNull();
  }

  @Test
  void getSessionIdShouldReturnNullWhenNoSession() {
    MockRequestContext context = new MockRequestContext();

    String sessionId = RequestContextUtils.getSessionId(context);

    assertThat(sessionId).isNull();
  }

  @Test
  void getSessionShouldReturnNullWhenNoSessionManager() {
    MockRequestContext context = new MockRequestContext();

    Session session = RequestContextUtils.getSession(context);

    assertThat(session).isNull();
  }

  @Test
  void getRequiredSessionShouldThrowExceptionWhenNoSession() {
    MockRequestContext context = new MockRequestContext();

    assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(() -> RequestContextUtils.getRequiredSession(context))
            .withMessage("Cannot get Session");
  }

  @Test
  void getSessionWithCreateFalseShouldReturnNullWhenNoSessionManager() {
    MockRequestContext context = new MockRequestContext();

    Session session = RequestContextUtils.getSession(context, false);

    assertThat(session).isNull();
  }

  @Test
  void getSessionWithCreateTrueShouldReturnNullWhenNoSessionManager() {
    MockRequestContext context = new MockRequestContext();

    Session session = RequestContextUtils.getSession(context, true);

    assertThat(session).isNull();
  }

  @Test
  void getSessionManagerShouldReturnNullWhenNotAvailable() {
    MockRequestContext context = new MockRequestContext();

    SessionManager sessionManager = RequestContextUtils.getSessionManager(context);

    assertThat(sessionManager).isNull();
  }

  @Test
  void getLocaleResolverShouldReturnNullWhenNotAvailable() {
    MockRequestContext context = new MockRequestContext();

    LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(context);

    assertThat(localeResolver).isNull();
  }

  @Test
  void getTimeZoneShouldReturnNullWhenNoLocaleResolver() {
    MockRequestContext context = new MockRequestContext();

    TimeZone timeZone = RequestContextUtils.getTimeZone(context);

    assertThat(timeZone).isNull();
  }

  @Test
  void getOutputRedirectModelShouldReturnNullWhenNotAvailable() {
    MockRequestContext context = new MockRequestContext();

    RedirectModel redirectModel = RequestContextUtils.getOutputRedirectModel(context);

    assertThat((Object) redirectModel).isNull();
  }

  @Test
  void getRedirectModelManagerShouldReturnNullWhenNotAvailable() {
    MockRequestContext context = new MockRequestContext();

    RedirectModelManager manager = RequestContextUtils.getRedirectModelManager(context);

    assertThat(manager).isNull();
  }

  @Test
  void saveRedirectModelShouldNotThrowExceptionWhenNoRedirectModel() {
    MockRequestContext context = new MockRequestContext();

    assertThatNoException().isThrownBy(() ->
            RequestContextUtils.saveRedirectModel("http://example.com", context));
  }

  @Test
  void saveRedirectModelShouldNotThrowExceptionWhenNoManager() {
    MockRequestContext context = new MockRequestContext();
    context.setAttribute(RedirectModel.OUTPUT_ATTRIBUTE, new RedirectModel());

    assertThatNoException().isThrownBy(() ->
            RequestContextUtils.saveRedirectModel("http://example.com", context));
  }

  @Test
  void getStringParameterShouldReturnNullWhenNotPresent() throws RequestBindingException {
    MockRequestContext context = new MockRequestContext();

    String value = RequestContextUtils.getStringParameter(context, "nonexistent");

    assertThat(value).isNull();
  }

  @Test
  void getStringParameterShouldReturnValueWhenPresent() throws RequestBindingException {
    MockRequestContext context = new MockRequestContext();
    context.setParameter("param", "testValue");

    String value = RequestContextUtils.getStringParameter(context, "param");

    assertThat(value).isEqualTo("testValue");
  }

  @Test
  void getStringParameterWithDefaultShouldReturnDefaultWhenNotPresent() {
    MockRequestContext context = new MockRequestContext();

    String value = RequestContextUtils.getStringParameter(context, "nonexistent", "defaultValue");

    assertThat(value).isEqualTo("defaultValue");
  }

  @Test
  void getStringParameterWithDefaultShouldReturnValueWhenPresent() {
    MockRequestContext context = new MockRequestContext();
    context.setParameter("param", "testValue");

    String value = RequestContextUtils.getStringParameter(context, "param", "defaultValue");

    assertThat(value).isEqualTo("testValue");
  }

  @Test
  void getStringParametersShouldReturnEmptyArrayWhenNotPresent() {
    MockRequestContext context = new MockRequestContext();

    String[] values = RequestContextUtils.getStringParameters(context, "nonexistent");

    assertThat(values).isEmpty();
  }

  @Test
  void getStringParametersShouldReturnValuesWhenPresent() {
    MockRequestContext context = new MockRequestContext();
    context.setParameter("param", "value1", "value2");

    String[] values = RequestContextUtils.getStringParameters(context, "param");

    assertThat(values).containsExactly("value1", "value2");
  }

  @Test
  void getRequiredStringParameterShouldThrowExceptionWhenNotPresent() {
    MockRequestContext context = new MockRequestContext();

    assertThatExceptionOfType(MissingRequestParameterException.class)
            .isThrownBy(() -> RequestContextUtils.getRequiredStringParameter(context, "nonexistent"))
            .withMessage("Required request parameter 'nonexistent' for method parameter type string is not present");
  }

  @Test
  void getRequiredStringParameterShouldReturnValueWhenPresent() throws RequestBindingException {
    MockRequestContext context = new MockRequestContext();
    context.setParameter("param", "testValue");

    String value = RequestContextUtils.getRequiredStringParameter(context, "param");

    assertThat(value).isEqualTo("testValue");
  }

  @Test
  void getRequiredStringParametersShouldThrowExceptionWhenNotPresent() {
    MockRequestContext context = new MockRequestContext();

    assertThatExceptionOfType(MissingRequestParameterException.class)
            .isThrownBy(() -> RequestContextUtils.getRequiredStringParameters(context, "nonexistent"));
  }

  @Test
  void getRequiredStringParametersShouldReturnValuesWhenPresent() throws RequestBindingException {
    MockRequestContext context = new MockRequestContext();
    context.setParameter("param", "value1", "value2");

    String[] values = RequestContextUtils.getRequiredStringParameters(context, "param");

    assertThat(values).containsExactly("value1", "value2");
  }

  @Test
  void getFloatParameterShouldReturnNullWhenNotPresent() throws RequestBindingException {
    MockRequestContext context = new MockRequestContext();

    Float value = RequestContextUtils.getFloatParameter(context, "nonexistent");

    assertThat(value).isNull();
  }

  @Test
  void getFloatParameterShouldReturnValueWhenPresent() throws RequestBindingException {
    MockRequestContext context = new MockRequestContext();
    context.setParameter("param", "3.14");

    Float value = RequestContextUtils.getFloatParameter(context, "param");

    assertThat(value).isEqualTo(3.14f);
  }

  @Test
  void getFloatParameterWithDefaultShouldReturnDefaultWhenNotPresent() {
    MockRequestContext context = new MockRequestContext();

    float value = RequestContextUtils.getFloatParameter(context, "nonexistent", 2.71f);

    assertThat(value).isEqualTo(2.71f);
  }

  @Test
  void getFloatParameterWithDefaultShouldReturnValueWhenPresent() {
    MockRequestContext context = new MockRequestContext();
    context.setParameter("param", "3.14");

    float value = RequestContextUtils.getFloatParameter(context, "param", 2.71f);

    assertThat(value).isEqualTo(3.14f);
  }

  @Test
  void getFloatParametersShouldReturnEmptyArrayWhenNotPresent() {
    MockRequestContext context = new MockRequestContext();

    float[] values = RequestContextUtils.getFloatParameters(context, "nonexistent");

    assertThat(values).isEmpty();
  }

  @Test
  void getFloatParametersShouldReturnValuesWhenPresent() {
    MockRequestContext context = new MockRequestContext();
    context.setParameter("param", "1.1", "2.2", "3.3");

    float[] values = RequestContextUtils.getFloatParameters(context, "param");

    assertThat(values).containsExactly(1.1f, 2.2f, 3.3f);
  }

  @Test
  void getRequiredFloatParameterShouldThrowExceptionWhenNotPresent() {
    MockRequestContext context = new MockRequestContext();

    assertThatExceptionOfType(MissingRequestParameterException.class)
            .isThrownBy(() -> RequestContextUtils.getRequiredFloatParameter(context, "nonexistent"));
  }

  @Test
  void getRequiredFloatParameterShouldThrowExceptionWhenInvalidValue() {
    MockRequestContext context = new MockRequestContext();
    context.setParameter("param", "invalid");

    assertThatExceptionOfType(RequestBindingException.class)
            .isThrownBy(() -> RequestContextUtils.getRequiredFloatParameter(context, "param"));
  }

  @Test
  void getRequiredFloatParameterShouldReturnValueWhenPresent() throws RequestBindingException {
    MockRequestContext context = new MockRequestContext();
    context.setParameter("param", "3.14");

    float value = RequestContextUtils.getRequiredFloatParameter(context, "param");

    assertThat(value).isEqualTo(3.14f);
  }

  @Test
  void getRequiredFloatParametersShouldThrowExceptionWhenNotPresent() {
    MockRequestContext context = new MockRequestContext();

    assertThatExceptionOfType(MissingRequestParameterException.class)
            .isThrownBy(() -> RequestContextUtils.getRequiredFloatParameters(context, "nonexistent"));
  }

  @Test
  void getRequiredFloatParametersShouldThrowExceptionWhenInvalidValue() {
    MockRequestContext context = new MockRequestContext();
    context.setParameter("param", "1.1", "invalid", "3.3");

    assertThatExceptionOfType(RequestBindingException.class)
            .isThrownBy(() -> RequestContextUtils.getRequiredFloatParameters(context, "param"));
  }

  @Test
  void getRequiredFloatParametersShouldReturnValuesWhenPresent() throws RequestBindingException {
    MockRequestContext context = new MockRequestContext();
    context.setParameter("param", "1.1", "2.2", "3.3");

    float[] values = RequestContextUtils.getRequiredFloatParameters(context, "param");

    assertThat(values).containsExactly(1.1f, 2.2f, 3.3f);
  }

  @Test
  void getDoubleParameterShouldReturnNullWhenNotPresent() throws RequestBindingException {
    MockRequestContext context = new MockRequestContext();

    Double value = RequestContextUtils.getDoubleParameter(context, "nonexistent");

    assertThat(value).isNull();
  }

  @Test
  void getDoubleParameterShouldReturnValueWhenPresent() throws RequestBindingException {
    MockRequestContext context = new MockRequestContext();
    context.setParameter("param", "3.14159");

    Double value = RequestContextUtils.getDoubleParameter(context, "param");

    assertThat(value).isEqualTo(3.14159);
  }

  @Test
  void getDoubleParameterWithDefaultShouldReturnDefaultWhenNotPresent() {
    MockRequestContext context = new MockRequestContext();

    double value = RequestContextUtils.getDoubleParameter(context, "nonexistent", 2.71828);

    assertThat(value).isEqualTo(2.71828);
  }

  @Test
  void getDoubleParameterWithDefaultShouldReturnValueWhenPresent() {
    MockRequestContext context = new MockRequestContext();
    context.setParameter("param", "3.14159");

    double value = RequestContextUtils.getDoubleParameter(context, "param", 2.71828);

    assertThat(value).isEqualTo(3.14159);
  }

  @Test
  void getDoubleParametersShouldReturnEmptyArrayWhenNotPresent() {
    MockRequestContext context = new MockRequestContext();

    double[] values = RequestContextUtils.getDoubleParameters(context, "nonexistent");

    assertThat(values).isEmpty();
  }

  @Test
  void getDoubleParametersShouldReturnValuesWhenPresent() {
    MockRequestContext context = new MockRequestContext();
    context.setParameter("param", "1.11", "2.22", "3.33");

    double[] values = RequestContextUtils.getDoubleParameters(context, "param");

    assertThat(values).containsExactly(1.11, 2.22, 3.33);
  }

  @Test
  void getRequiredDoubleParameterShouldThrowExceptionWhenNotPresent() {
    MockRequestContext context = new MockRequestContext();

    assertThatExceptionOfType(MissingRequestParameterException.class)
            .isThrownBy(() -> RequestContextUtils.getRequiredDoubleParameter(context, "nonexistent"));
  }

  @Test
  void getRequiredDoubleParameterShouldThrowExceptionWhenInvalidValue() {
    MockRequestContext context = new MockRequestContext();
    context.setParameter("param", "invalid");

    assertThatExceptionOfType(RequestBindingException.class)
            .isThrownBy(() -> RequestContextUtils.getRequiredDoubleParameter(context, "param"));
  }

  @Test
  void getRequiredDoubleParameterShouldReturnValueWhenPresent() throws RequestBindingException {
    MockRequestContext context = new MockRequestContext();
    context.setParameter("param", "3.14159");

    double value = RequestContextUtils.getRequiredDoubleParameter(context, "param");

    assertThat(value).isEqualTo(3.14159);
  }

  @Test
  void getRequiredDoubleParametersShouldThrowExceptionWhenNotPresent() {
    MockRequestContext context = new MockRequestContext();

    assertThatExceptionOfType(MissingRequestParameterException.class)
            .isThrownBy(() -> RequestContextUtils.getRequiredDoubleParameters(context, "nonexistent"));
  }

  @Test
  void getRequiredDoubleParametersShouldThrowExceptionWhenInvalidValue() {
    MockRequestContext context = new MockRequestContext();
    context.setParameter("param", "1.11", "invalid", "3.33");

    assertThatExceptionOfType(RequestBindingException.class)
            .isThrownBy(() -> RequestContextUtils.getRequiredDoubleParameters(context, "param"));
  }

  @Test
  void getRequiredDoubleParametersShouldReturnValuesWhenPresent() throws RequestBindingException {
    MockRequestContext context = new MockRequestContext();
    context.setParameter("param", "1.11", "2.22", "3.33");

    double[] values = RequestContextUtils.getRequiredDoubleParameters(context, "param");

    assertThat(values).containsExactly(1.11, 2.22, 3.33);
  }

}
