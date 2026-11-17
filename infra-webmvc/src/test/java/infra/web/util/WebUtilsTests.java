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

package infra.web.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import infra.beans.factory.BeanNotOfRequiredTypeException;
import infra.context.ApplicationContext;
import infra.context.support.StaticApplicationContext;
import infra.core.i18n.SimpleLocaleContext;
import infra.core.i18n.SimpleTimeZoneAwareLocaleContext;
import infra.http.HttpHeaders;
import infra.session.Session;
import infra.util.MultiValueMap;
import infra.web.HttpContext;
import infra.web.LocaleContextResolver;
import infra.web.LocaleResolver;
import infra.web.RedirectModel;
import infra.web.RedirectModelManager;
import infra.web.bind.MissingRequestParameterException;
import infra.web.bind.RequestBindingException;
import infra.web.multipart.Part;
import infra.web.mock.MockHttpContext;
import infra.web.mock.MockRequest;
import infra.web.mock.MockResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

/**
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public class WebUtilsTests {

  @Test
  void intParameter() {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("valid", "5");
    context.setParameter("invalid", "e");
    context.setParameter("empty", "");

    assertThat(WebUtils.getIntParameter(context, "valid")).isEqualTo(5);
    assertThat(WebUtils.getIntParameter(context, "valid", 6)).isEqualTo(5);
    assertThat(WebUtils.getRequiredIntParameter(context, "valid")).isEqualTo(5);
    assertThat(WebUtils.getIntParameter(context, "invalid", 6)).isEqualTo(6);
    assertThat(WebUtils.getIntParameter(context, "missing")).isNull();
    assertThat(WebUtils.getIntParameter(context, "missing", 6)).isEqualTo(6);
    assertThatThrownBy(() -> WebUtils.getRequiredIntParameter(context, "invalid"))
            .isInstanceOf(RequestBindingException.class);
    assertThatThrownBy(() -> WebUtils.getRequiredIntParameter(context, "missing"))
            .isInstanceOf(MissingRequestParameterException.class);
    assertThatThrownBy(() -> WebUtils.getRequiredIntParameter(context, "empty"))
            .isInstanceOf(RequestBindingException.class);
  }

  @Test
  void intParameters() {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("valid", "1", "2", "3");
    context.setParameter("invalid", "1", "2", "bogus");

    assertThat(WebUtils.getRequiredIntParameters(context, "valid")).containsExactly(1, 2, 3);
    assertThatThrownBy(() -> WebUtils.getRequiredIntParameters(context, "invalid"))
            .isInstanceOf(RequestBindingException.class);
  }

  @Test
  void longParameter() {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("valid", "5");
    context.setParameter("invalid", "e");
    context.setParameter("empty", "");

    assertThat(WebUtils.getLongParameter(context, "valid")).isEqualTo(5L);
    assertThat(WebUtils.getLongParameter(context, "valid", 6L)).isEqualTo(5L);
    assertThat(WebUtils.getRequiredLongParameter(context, "valid")).isEqualTo(5L);
    assertThat(WebUtils.getLongParameter(context, "invalid", 6L)).isEqualTo(6L);
    assertThat(WebUtils.getLongParameter(context, "missing")).isNull();
    assertThat(WebUtils.getLongParameter(context, "missing", 6L)).isEqualTo(6L);
    assertThatThrownBy(() -> WebUtils.getRequiredLongParameter(context, "invalid"))
            .isInstanceOf(RequestBindingException.class);
    assertThatThrownBy(() -> WebUtils.getRequiredLongParameter(context, "missing"))
            .isInstanceOf(MissingRequestParameterException.class);
    assertThatThrownBy(() -> WebUtils.getRequiredLongParameter(context, "empty"))
            .isInstanceOf(RequestBindingException.class);
  }

  @Test
  void longParameters() {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("values", "1", "2", "3");
    assertThat(WebUtils.getRequiredLongParameters(context, "values")).containsExactly(1, 2, 3);

    context.setParameter("values", "1", "2", "bogus");
    assertThatThrownBy(() -> WebUtils.getRequiredLongParameters(context, "values"))
            .isInstanceOf(RequestBindingException.class);

    context.setParameter("values", "1", "2");
    assertThat(WebUtils.getRequiredLongParameters(context, "values")).containsExactly(1, 2);
    context.removeParameter("values");
    assertThatThrownBy(() -> WebUtils.getRequiredLongParameters(context, "values"))
            .isInstanceOf(MissingRequestParameterException.class);
  }

  @Test
  void floatParameter() {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("valid", "5.5");
    context.setParameter("invalid", "e");
    context.setParameter("empty", "");

    assertThat(WebUtils.getFloatParameter(context, "valid")).isEqualTo(5.5f);
    assertThat(WebUtils.getFloatParameter(context, "valid", 6.5f)).isEqualTo(5.5f);
    assertThat(WebUtils.getRequiredFloatParameter(context, "valid")).isEqualTo(5.5f);
    assertThat(WebUtils.getFloatParameter(context, "invalid", 6.5f)).isEqualTo(6.5f);
    assertThat(WebUtils.getFloatParameter(context, "missing")).isNull();
    assertThat(WebUtils.getFloatParameter(context, "missing", 6.5f)).isEqualTo(6.5f);
    assertThatThrownBy(() -> WebUtils.getRequiredFloatParameter(context, "invalid"))
            .isInstanceOf(RequestBindingException.class);
    assertThatThrownBy(() -> WebUtils.getRequiredFloatParameter(context, "missing"))
            .isInstanceOf(MissingRequestParameterException.class);
    assertThatThrownBy(() -> WebUtils.getRequiredFloatParameter(context, "empty"))
            .isInstanceOf(RequestBindingException.class);
  }

  @Test
  void floatParameters() {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("valid", "1.5", "2.5", "3");
    context.setParameter("invalid", "1.5", "2", "bogus");

    assertThat(WebUtils.getFloatParameters(context, "valid")).containsExactly(1.5f, 2.5f, 3f);
    assertThat(WebUtils.getRequiredFloatParameters(context, "valid")).containsExactly(1.5f, 2.5f, 3f);
    assertThatThrownBy(() -> WebUtils.getRequiredFloatParameters(context, "invalid"))
            .isInstanceOf(RequestBindingException.class);
    assertThatThrownBy(() -> WebUtils.getRequiredFloatParameters(context, "missing"))
            .isInstanceOf(MissingRequestParameterException.class);
  }

  @Test
  void doubleParameter() {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("valid", "5.5");
    context.setParameter("invalid", "e");
    context.setParameter("empty", "");

    assertThat(WebUtils.getDoubleParameter(context, "valid")).isEqualTo(5.5);
    assertThat(WebUtils.getDoubleParameter(context, "valid", 6.5)).isEqualTo(5.5);
    assertThat(WebUtils.getRequiredDoubleParameter(context, "valid")).isEqualTo(5.5);
    assertThat(WebUtils.getDoubleParameter(context, "invalid", 6.5)).isEqualTo(6.5);
    assertThat(WebUtils.getDoubleParameter(context, "missing")).isNull();
    assertThat(WebUtils.getDoubleParameter(context, "missing", 6.5)).isEqualTo(6.5);
    assertThatThrownBy(() -> WebUtils.getRequiredDoubleParameter(context, "invalid"))
            .isInstanceOf(RequestBindingException.class);
    assertThatThrownBy(() -> WebUtils.getRequiredDoubleParameter(context, "missing"))
            .isInstanceOf(MissingRequestParameterException.class);
    assertThatThrownBy(() -> WebUtils.getRequiredDoubleParameter(context, "empty"))
            .isInstanceOf(RequestBindingException.class);
  }

  @Test
  void doubleParameters() {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("valid", "1.5", "2.5", "3");
    context.setParameter("invalid", "1.5", "2", "bogus");

    assertThat(WebUtils.getDoubleParameters(context, "valid")).containsExactly(1.5, 2.5, 3);
    assertThat(WebUtils.getRequiredDoubleParameters(context, "valid")).containsExactly(1.5, 2.5, 3);
    assertThatThrownBy(() -> WebUtils.getRequiredDoubleParameters(context, "invalid"))
            .isInstanceOf(RequestBindingException.class);
    assertThatThrownBy(() -> WebUtils.getRequiredDoubleParameters(context, "missing"))
            .isInstanceOf(MissingRequestParameterException.class);
  }

  @Test
  void booleanParameterDefaultsOnlyWhenAbsent() {
    MockHttpContext context = new MockHttpContext();
    assertThat(WebUtils.getBooleanParameter(context, "missing")).isNull();
    assertThat(WebUtils.getBooleanParameter(context, "missing", true)).isTrue();
    assertThat(WebUtils.getBooleanParameter(context, "missing", false)).isFalse();
    assertThatThrownBy(() -> WebUtils.getRequiredBooleanParameter(context, "missing"))
            .isInstanceOf(MissingRequestParameterException.class);
    context.setParameter("present", "true");
    assertThat(WebUtils.getBooleanParameter(context, "present", false)).isTrue();
  }

  @Test
  void booleanParametersPreserveOrderAndEmptyValues() {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("flags", "true", "yes", "off", "1", "bogus");
    assertThat(WebUtils.getRequiredBooleanParameters(context, "flags"))
            .containsExactly(true, true, false, true, false);
    assertThat(WebUtils.getBooleanParameters(context, "flags"))
            .containsExactly(true, true, false, true, false);
    context.setParameter("flags", "false", "true", "");
    assertThat(WebUtils.getRequiredBooleanParameters(context, "flags")).containsExactly(false, true, false);
  }

  @Test
  void stringParameter() {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("valid", "str");
    context.setParameter("empty", "");

    assertThat(WebUtils.getStringParameter(context, "valid")).isEqualTo("str");
    assertThat(WebUtils.getStringParameter(context, "valid", "fallback")).isEqualTo("str");
    assertThat(WebUtils.getRequiredStringParameter(context, "valid")).isEqualTo("str");
    assertThat(WebUtils.getStringParameter(context, "missing")).isNull();
    assertThat(WebUtils.getStringParameter(context, "missing", "fallback")).isEqualTo("fallback");
    assertThat(WebUtils.getStringParameter(context, "missing", null)).isNull();
    assertThatThrownBy(() -> WebUtils.getRequiredStringParameter(context, "missing"))
            .isInstanceOf(MissingRequestParameterException.class)
            .hasMessage("Required request parameter 'missing' for method parameter type string is not present");
    assertThat(WebUtils.getStringParameter(context, "empty")).isEmpty();
    assertThat(WebUtils.getRequiredStringParameter(context, "empty")).isEmpty();
  }

  @Test
  void stringParameters() {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("values", "value1", "value2");
    assertThat(WebUtils.getStringParameters(context, "values")).containsExactly("value1", "value2");
    assertThat(WebUtils.getRequiredStringParameters(context, "values")).containsExactly("value1", "value2");
    assertThatThrownBy(() -> WebUtils.getRequiredStringParameters(context, "missing"))
            .isInstanceOf(MissingRequestParameterException.class);
  }

  @Test
  void contextUtilitiesReturnNullWithoutApplicationContextOrRequestAttributes() {
    MockHttpContext context = new MockHttpContext();
    assertThat(WebUtils.getBean(context, "testBean", String.class)).isNull();
    assertThat(WebUtils.getLocaleResolver(context)).isNull();
    assertThat(WebUtils.getTimeZone(context)).isNull();
    assertThat((Object) WebUtils.getOutputRedirectModel(context)).isNull();
    assertThat(WebUtils.getRedirectModelManager(context)).isNull();
    WebUtils.saveRedirectModel("http://example.com", context);
  }

  @Test
  void sessionMutexUsesDedicatedAttributeOrSessionItself() {
    Session session = new MockHttpContext().getSession();
    assertThat(WebUtils.getSessionMutex(session)).isSameAs(session);
    Object mutex = new Object();
    session.setAttribute(WebUtils.SESSION_MUTEX_ATTRIBUTE, mutex);
    assertThat(WebUtils.getSessionMutex(session)).isSameAs(mutex);
  }

  @Test
  void beanLookupUsesNameAliasesAndParentButNotTypeOrRequestAttributes() {
    try (StaticApplicationContext parent = new StaticApplicationContext();
            StaticApplicationContext child = new StaticApplicationContext()) {
      parent.getBeanFactory().registerSingleton("parentBean", "parent");
      child.setParent(parent);
      child.getBeanFactory().registerSingleton("namedBean", "child");
      child.getBeanFactory().registerAlias("namedBean", "alias");
      MockHttpContext context = new MockHttpContext(child);
      context.setAttribute("missing", "attribute");

      assertThat(WebUtils.getBean(context, "namedBean", String.class)).isEqualTo("child");
      assertThat(WebUtils.getBean(context, "alias", String.class)).isEqualTo("child");
      assertThat(WebUtils.getBean(context, "parentBean", String.class)).isEqualTo("parent");
      assertThat(WebUtils.getBean(context, "missing", String.class)).isNull();
      assertThatThrownBy(() -> WebUtils.getBean(context, "namedBean", Integer.class))
              .isInstanceOf(BeanNotOfRequiredTypeException.class);
    }
  }

  @Test
  void beanLookupPropagatesCreationFailures() {
    HttpContext request = mock(HttpContext.class);
    ApplicationContext applicationContext = mock(ApplicationContext.class);
    IllegalStateException failure = new IllegalStateException("creation failed");
    given(request.getApplicationContext()).willReturn(applicationContext);
    given(applicationContext.containsBean("broken")).willReturn(true);
    given(applicationContext.getBean("broken", String.class)).willThrow(failure);

    assertThatThrownBy(() -> WebUtils.getBean(request, "broken", String.class)).isSameAs(failure);
  }

  @Test
  void localeFallsBackToRequestThenSystemDefaultWithoutResolver() {
    HttpContext context = mock(HttpContext.class);
    given(context.getLocale()).willReturn(Locale.JAPAN).willReturn(null);

    assertThat(WebUtils.resolveLocale(context)).isEqualTo(Locale.JAPAN);
    assertThat(WebUtils.resolveLocale(context)).isEqualTo(Locale.getDefault());
    assertThat(WebUtils.getTimeZone(context)).isNull();
  }

  @Test
  void localeResolverIsSelectedByRoleNameAndOverridesRequestLocale() {
    try (StaticApplicationContext applicationContext = new StaticApplicationContext()) {
      LocaleResolver resolver = mock(LocaleResolver.class);
      applicationContext.getBeanFactory().registerSingleton("otherResolver", resolver);
      MockHttpContext context = new MockHttpContext(applicationContext);
      context.setLocale(Locale.JAPAN);
      context.setAttribute(LocaleResolver.BEAN_NAME, resolver);
      assertThat(WebUtils.getLocaleResolver(context)).isNull();
      assertThat(WebUtils.resolveLocale(context)).isEqualTo(Locale.JAPAN);

      applicationContext.getBeanFactory().registerSingleton(LocaleResolver.BEAN_NAME, resolver);
      given(resolver.resolveLocale(context)).willReturn(Locale.GERMANY);
      assertThat(WebUtils.getLocaleResolver(context)).isSameAs(resolver);
      assertThat(WebUtils.resolveLocale(context)).isEqualTo(Locale.GERMANY);
      assertThat(WebUtils.getTimeZone(context)).isNull();
    }
  }

  @Test
  void timeZoneRequiresTimeZoneAwareLocaleContextAndDoesNotInventDefault() {
    try (StaticApplicationContext applicationContext = new StaticApplicationContext()) {
      LocaleContextResolver resolver = mock(LocaleContextResolver.class);
      applicationContext.getBeanFactory().registerSingleton(LocaleResolver.BEAN_NAME, resolver);
      MockHttpContext context = new MockHttpContext(applicationContext);
      TimeZone zone = TimeZone.getTimeZone("Asia/Shanghai");
      given(resolver.resolveLocaleContext(context)).willReturn(new SimpleLocaleContext(Locale.CHINA),
              new SimpleTimeZoneAwareLocaleContext(Locale.CHINA, null),
              new SimpleTimeZoneAwareLocaleContext(Locale.CHINA, zone));

      assertThat(WebUtils.getTimeZone(context)).isNull();
      assertThat(WebUtils.getTimeZone(context)).isNull();
      assertThat(WebUtils.getTimeZone(context)).isSameAs(zone);
    }
  }

  @Test
  void redirectModelIsSavedWithTargetPathAndRepeatedQueryParameters() {
    MockHttpContext context = new MockHttpContext();
    RedirectModel model = new RedirectModel();
    model.put("message", "saved");
    context.setAttribute(RedirectModel.OUTPUT_ATTRIBUTE, model);
    RedirectModelManager manager = mock(RedirectModelManager.class);

    WebUtils.saveRedirectModel("https://example.org/target?a=1&a=2&b=3#fragment", context, manager);

    assertThat((Object) WebUtils.getOutputRedirectModel(context)).isSameAs(model);
    assertThat(model.getTargetRequestPath()).isEqualTo("/target");
    assertThat(model.getTargetRequestParams().get("a")).containsExactly("1", "2");
    assertThat(model.getTargetRequestParams().get("b")).containsExactly("3");
    then(manager).should().saveRedirectModel(context, model);
  }

  @Test
  void redirectModelUsesNamedManagerUnlessExplicitManagerIsProvided() {
    try (StaticApplicationContext applicationContext = new StaticApplicationContext()) {
      RedirectModelManager named = mock(RedirectModelManager.class);
      RedirectModelManager explicit = mock(RedirectModelManager.class);
      applicationContext.getBeanFactory().registerSingleton(RedirectModelManager.BEAN_NAME, named);
      MockHttpContext context = new MockHttpContext(applicationContext);
      RedirectModel model = new RedirectModel();
      model.put("message", "saved");
      context.setAttribute(RedirectModel.OUTPUT_ATTRIBUTE, model);

      assertThat(WebUtils.getRedirectModelManager(context)).isSameAs(named);
      WebUtils.saveRedirectModel("/first", context);
      then(named).should().saveRedirectModel(context, model);
      WebUtils.saveRedirectModel("/second", context, explicit);
      then(explicit).should().saveRedirectModel(context, model);
      then(named).shouldHaveNoMoreInteractions();
    }
  }

  @Test
  void redirectModelSkipsAbsentOrEmptyModelAndMissingManager() {
    MockHttpContext context = new MockHttpContext();
    RedirectModelManager manager = mock(RedirectModelManager.class);
    WebUtils.saveRedirectModel("/target", context, manager);
    RedirectModel model = new RedirectModel();
    context.setAttribute(RedirectModel.OUTPUT_ATTRIBUTE, model);
    WebUtils.saveRedirectModel("/target", context, manager);
    then(manager).shouldHaveNoInteractions();

    model.put("message", "saved");
    WebUtils.saveRedirectModel("/target?a=1", context);
    assertThat(model.getTargetRequestPath()).isNull();
    assertThat(model.getTargetRequestParams()).isEmpty();
  }

  @Test
  void removeSemicolonContentFromEveryPathSegment() {
    assertThat(WebUtils.removeSemicolonContent("")).isEmpty();
    assertThat(WebUtils.removeSemicolonContent("/plain/path")).isEqualTo("/plain/path");
    assertThat(WebUtils.removeSemicolonContent("/a;x=1;y=2/b;z=3/c;")).isEqualTo("/a/b/c");
    assertThat(WebUtils.removeSemicolonContent(";x=1/a;b=2/")).isEqualTo("/a/");
    assertThat(WebUtils.removeSemicolonContent("/a%3Bb/c")).isEqualTo("/a%3Bb/c");
  }

  @ParameterizedTest
  @ValueSource(strings = { "", "invalid", "999999999999999999999999999999999999999999999999999999999999999999" })
  void invalidIntegralParametersThrowOrFallBackWithoutReturningPartialArrays(String value) {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("number", value);
    assertThatThrownBy(() -> WebUtils.getIntParameter(context, "number"))
            .isInstanceOf(RequestBindingException.class).hasCauseInstanceOf(NumberFormatException.class);
    assertThatThrownBy(() -> WebUtils.getLongParameter(context, "number"))
            .isInstanceOf(RequestBindingException.class).hasCauseInstanceOf(NumberFormatException.class);
    assertThat(WebUtils.getIntParameter(context, "number", 42)).isEqualTo(42);
    assertThat(WebUtils.getLongParameter(context, "number", 42L)).isEqualTo(42L);

    context.setParameter("number", "1", value, "3");
    assertThat(WebUtils.getIntParameters(context, "number")).isEmpty();
    assertThat(WebUtils.getLongParameters(context, "number")).isEmpty();
    assertThatThrownBy(() -> WebUtils.getRequiredIntParameters(context, "number"))
            .isInstanceOf(RequestBindingException.class);
    assertThatThrownBy(() -> WebUtils.getRequiredLongParameters(context, "number"))
            .isInstanceOf(RequestBindingException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = { "", "invalid" })
  void invalidFloatingPointParametersThrowOrFallBackWithoutReturningPartialArrays(String value) {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("number", value);
    assertThatThrownBy(() -> WebUtils.getFloatParameter(context, "number"))
            .isInstanceOf(RequestBindingException.class).hasCauseInstanceOf(NumberFormatException.class);
    assertThatThrownBy(() -> WebUtils.getDoubleParameter(context, "number"))
            .isInstanceOf(RequestBindingException.class).hasCauseInstanceOf(NumberFormatException.class);
    assertThat(WebUtils.getFloatParameter(context, "number", 4.2f)).isEqualTo(4.2f);
    assertThat(WebUtils.getDoubleParameter(context, "number", 4.2)).isEqualTo(4.2);
    context.setParameter("number", "1", value, "3");
    assertThat(WebUtils.getFloatParameters(context, "number")).isEmpty();
    assertThat(WebUtils.getDoubleParameters(context, "number")).isEmpty();
  }

  @Test
  void missingParametersReturnEmptyArraysOrThrowTypedMissingParameterException() {
    MockHttpContext context = new MockHttpContext();
    assertThat(WebUtils.getIntParameters(context, "missing")).isEmpty();
    assertThat(WebUtils.getLongParameters(context, "missing")).isEmpty();
    assertThat(WebUtils.getFloatParameters(context, "missing")).isEmpty();
    assertThat(WebUtils.getDoubleParameters(context, "missing")).isEmpty();
    assertThat(WebUtils.getBooleanParameters(context, "missing")).isEmpty();
    assertThat(WebUtils.getStringParameters(context, "missing")).isEmpty();
    assertThatThrownBy(() -> WebUtils.getRequiredIntParameters(context, "missing"))
            .isInstanceOf(MissingRequestParameterException.class).hasMessageContaining("int");
    assertThatThrownBy(() -> WebUtils.getRequiredLongParameters(context, "missing"))
            .isInstanceOf(MissingRequestParameterException.class).hasMessageContaining("long");
    assertThatThrownBy(() -> WebUtils.getRequiredBooleanParameters(context, "missing"))
            .isInstanceOf(MissingRequestParameterException.class).hasMessageContaining("boolean");
  }

  @Test
  void integralParametersSupportFullRangeAndPreserveArrayOrder() {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("int", "-2147483648", "2147483647", "0", "+1");
    context.setParameter("long", "-9223372036854775808", "9223372036854775807", "0", "+1");
    assertThat(WebUtils.getIntParameters(context, "int"))
            .containsExactly(Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 1);
    assertThat(WebUtils.getLongParameters(context, "long"))
            .containsExactly(Long.MIN_VALUE, Long.MAX_VALUE, 0, 1);
    assertThat(WebUtils.getRequiredIntParameter(context, "int")).isEqualTo(Integer.MIN_VALUE);
    assertThat(WebUtils.getRequiredLongParameter(context, "long")).isEqualTo(Long.MIN_VALUE);
  }

  @ParameterizedTest
  @ValueSource(strings = { "true", "TRUE", "on", "ON", "yes", "YeS", "1" })
  void booleanParametersRecognizeAllTrueRepresentations(String value) {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("flag", value);
    assertThat(WebUtils.getBooleanParameter(context, "flag")).isTrue();
    assertThat(WebUtils.getRequiredBooleanParameter(context, "flag")).isTrue();
    assertThat(WebUtils.getBooleanParameters(context, "flag")).containsExactly(true);
  }

  @ParameterizedTest
  @ValueSource(strings = { "false", "off", "no", "0", "", "unknown", " true " })
  void booleanParametersTreatOtherPresentValuesAsFalseRatherThanUsingDefault(String value) {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("flag", value);
    assertThat(WebUtils.getBooleanParameter(context, "flag", true)).isFalse();
    assertThat(WebUtils.getRequiredBooleanParameters(context, "flag")).containsExactly(false);
  }

  @Test
  void stringParametersPreserveEmptyValuesWhitespaceAndDuplicates() {
    MockHttpContext context = new MockHttpContext();
    context.setParameter("text", "", " text ", "text", "text");
    assertThat(WebUtils.getStringParameter(context, "text", "fallback")).isEmpty();
    assertThat(WebUtils.getRequiredStringParameters(context, "text"))
            .containsExactly("", " text ", "text", "text");
  }

  @Test
  void multipartCleanupContinuesAfterFailureAndCleansEveryPart() throws Exception {
    Part failing = mock(Part.class);
    Part second = mock(Part.class);
    Part third = mock(Part.class);
    willThrow(new IllegalStateException("cleanup failed")).given(failing).cleanup();
    MultiValueMap<String, Part> parts = MultiValueMap.forLinkedHashMap();
    parts.add("first", failing);
    parts.add("first", second);
    parts.add("second", third);

    WebUtils.cleanupMultipartRequest(null);
    WebUtils.cleanupMultipartRequest(MultiValueMap.forLinkedHashMap());
    WebUtils.cleanupMultipartRequest(parts);

    then(failing).should().cleanup();
    then(second).should().cleanup();
    then(third).should().cleanup();
  }

  @Test
  public void parseMatrixVariablesString() {
    MultiValueMap<String, String> variables;

    variables = WebUtils.parseMatrixVariables(null);
    assertThat(variables).hasSize(0);

    variables = WebUtils.parseMatrixVariables("year");
    assertThat(variables).hasSize(1);
    assertThat(variables.getFirst("year")).isEqualTo("");

    variables = WebUtils.parseMatrixVariables("year=2012");
    assertThat(variables).hasSize(1);
    assertThat(variables.getFirst("year")).isEqualTo("2012");

    variables = WebUtils.parseMatrixVariables("year=2012;colors=red,blue,green");
    assertThat(variables).hasSize(2);
    assertThat(variables.get("colors")).containsExactly("red", "blue", "green");
    assertThat(variables.getFirst("year")).isEqualTo("2012");

    variables = WebUtils.parseMatrixVariables(";year=2012;colors=red,blue,green;");
    assertThat(variables).hasSize(2);
    assertThat(variables.get("colors")).containsExactly("red", "blue", "green");
    assertThat(variables.getFirst("year")).isEqualTo("2012");

    variables = WebUtils.parseMatrixVariables("colors=red;colors=blue;colors=green");
    assertThat(variables).hasSize(1);
    assertThat(variables.get("colors")).containsExactly("red", "blue", "green");

    variables = WebUtils.parseMatrixVariables("jsessionid=c0o7fszeb1");
    assertThat(variables).isEmpty();

    variables = WebUtils.parseMatrixVariables("a=b;jsessionid=c0o7fszeb1;c=d");
    assertThat(variables).hasSize(2);
    assertThat(variables.get("a")).containsExactly("b");
    assertThat(variables.get("c")).containsExactly("d");

    variables = WebUtils.parseMatrixVariables("a=b;jsessionid=c0o7fszeb1;c=d");
    assertThat(variables).hasSize(2);
    assertThat(variables.get("a")).containsExactly("b");
    assertThat(variables.get("c")).containsExactly("d");
  }

  @Test
  public void isValidOrigin() {
    List<String> allowed = Collections.emptyList();
    assertThat(checkValidOrigin("mydomain1.example", -1, "http://mydomain1.example", allowed)).isTrue();
    assertThat(checkValidOrigin("mydomain1.example", -1, "http://mydomain2.example", allowed)).isFalse();

    allowed = Collections.singletonList("*");
    assertThat(checkValidOrigin("mydomain1.example", -1, "http://mydomain2.example", allowed)).isTrue();

    allowed = Collections.singletonList("http://mydomain1.example");
    assertThat(checkValidOrigin("mydomain2.example", -1, "http://mydomain1.example", allowed)).isTrue();
    assertThat(checkValidOrigin("mydomain2.example", -1, "http://mydomain3.example", allowed)).isFalse();
  }

  @Test
  public void isSameOrigin() {
    assertThat(checkSameOrigin("http", "mydomain1.example", -1, "http://mydomain1.example")).isTrue();
    assertThat(checkSameOrigin("http", "mydomain1.example", -1, "http://mydomain1.example:80")).isTrue();
    assertThat(checkSameOrigin("https", "mydomain1.example", 443, "https://mydomain1.example")).isTrue();
    assertThat(checkSameOrigin("https", "mydomain1.example", 443, "https://mydomain1.example:443")).isTrue();
    assertThat(checkSameOrigin("http", "mydomain1.example", 123, "http://mydomain1.example:123")).isTrue();
    assertThat(checkSameOrigin("ws", "mydomain1.example", -1, "ws://mydomain1.example")).isTrue();
    assertThat(checkSameOrigin("wss", "mydomain1.example", 443, "wss://mydomain1.example")).isTrue();

    assertThat(checkSameOrigin("http", "mydomain1.example", -1, "http://mydomain2.example")).isFalse();
    assertThat(checkSameOrigin("http", "mydomain1.example", -1, "https://mydomain1.example")).isFalse();
    assertThat(checkSameOrigin("http", "mydomain1.example", -1, "invalid-origin")).isFalse();
    assertThat(checkSameOrigin("https", "mydomain1.example", -1, "http://mydomain1.example")).isFalse();

    assertThat(checkSameOrigin("http", "mydomain1.example", -1, "http://mydomain1.example/")).isTrue();
    assertThat(checkSameOrigin("http", "mydomain1.example", -1, "http://mydomain1.example:80/")).isTrue();
    assertThat(checkSameOrigin("http", "mydomain1.example", -1, "http://mydomain1.example/path")).isTrue();
    assertThat(checkSameOrigin("http", "mydomain1.example", -1, "http://mydomain1.example:80/path")).isTrue();
    assertThat(checkSameOrigin("http", "mydomain2.example", -1, "http://mydomain1.example/")).isFalse();
    assertThat(checkSameOrigin("http", "mydomain2.example", -1, "http://mydomain1.example:80/")).isFalse();
    assertThat(checkSameOrigin("http", "mydomain2.example", -1, "http://mydomain1.example/path")).isFalse();
    assertThat(checkSameOrigin("http", "mydomain2.example", -1, "http://mydomain1.example:80/path")).isFalse();

    assertThat(checkSameOrigin("http", "[::1]", -1, "http://[::1]")).isTrue();
    assertThat(checkSameOrigin("http", "[::1]", 8080, "http://[::1]:8080")).isTrue();
    assertThat(checkSameOrigin("http",
            "[2001:0db8:0000:85a3:0000:0000:ac1f:8001]", -1,
            "http://[2001:0db8:0000:85a3:0000:0000:ac1f:8001]")).isTrue();
    assertThat(checkSameOrigin("http",
            "[2001:0db8:0000:85a3:0000:0000:ac1f:8001]", 8080,
            "http://[2001:0db8:0000:85a3:0000:0000:ac1f:8001]:8080")).isTrue();
    assertThat(checkSameOrigin("http", "[::1]", -1, "http://[::1]:8080")).isFalse();
    assertThat(checkSameOrigin("http", "[::1]", 8080,
            "http://[2001:0db8:0000:85a3:0000:0000:ac1f:8001]:8080")).isFalse();
  }

  @Test
  public void isSameOriginWithXForwardedHeaders() throws Exception {
    String server = "mydomain1.example";
    testWithXForwardedHeaders(server, -1, "https", null, -1, "https://mydomain1.example");
    testWithXForwardedHeaders(server, 123, "https", null, -1, "https://mydomain1.example");
    testWithXForwardedHeaders(server, -1, "https", "mydomain2.example", -1, "https://mydomain2.example");
    testWithXForwardedHeaders(server, 123, "https", "mydomain2.example", -1, "https://mydomain2.example");
    testWithXForwardedHeaders(server, -1, "https", "mydomain2.example", 456, "https://mydomain2.example:456");
    testWithXForwardedHeaders(server, 123, "https", "mydomain2.example", 456, "https://mydomain2.example:456");
  }

  @Test
  public void isSameOriginWithForwardedHeader() throws Exception {
    String server = "mydomain1.example";
    testWithForwardedHeader(server, -1, "proto=https", "https://mydomain1.example");
    testWithForwardedHeader(server, 123, "proto=https", "https://mydomain1.example");
    testWithForwardedHeader(server, -1, "proto=https; host=mydomain2.example", "https://mydomain2.example");
    testWithForwardedHeader(server, 123, "proto=https; host=mydomain2.example", "https://mydomain2.example");
    testWithForwardedHeader(server, -1, "proto=https; host=mydomain2.example:456", "https://mydomain2.example:456");
    testWithForwardedHeader(server, 123, "proto=https; host=mydomain2.example:456", "https://mydomain2.example:456");
  }

  @Test
  void getSanitizedPath() {
    String sanitizedPath = WebUtils.getSanitizedPath("/api/users//1");
    assertThat(sanitizedPath).isEqualTo("/api/users/1");
    assertThat(WebUtils.getSanitizedPath(sanitizedPath)).isEqualTo("/api/users/1");
    assertThat(WebUtils.getSanitizedPath("///api////users///")).isEqualTo("/api/users/");
    assertThat(WebUtils.getSanitizedPath("")).isEmpty();
    assertThat(WebUtils.getSanitizedPath("////")).isEqualTo("/");
  }

  private boolean checkValidOrigin(String serverName, int port, String originHeader, List<String> allowed) {
    MockRequest mockRequest = new MockRequest();
    mockRequest.setServerName(serverName);
    if (port != -1) {
      mockRequest.setServerPort(port);
    }
    mockRequest.addHeader(HttpHeaders.ORIGIN, originHeader);
    MockHttpContext context = new MockHttpContext(null, mockRequest, new MockResponse());
    return WebUtils.isValidOrigin(context, allowed);
  }

  private boolean checkSameOrigin(String scheme, String serverName, int port, String originHeader) {
    MockRequest mockRequest = new MockRequest();
    mockRequest.setScheme(scheme);
    mockRequest.setServerName(serverName);
    if (port != -1) {
      mockRequest.setServerPort(port);
    }
    mockRequest.addHeader(HttpHeaders.ORIGIN, originHeader);

    MockHttpContext context = new MockHttpContext(null, mockRequest, new MockResponse());
    return WebUtils.isSameOrigin(context);
  }

  private void testWithXForwardedHeaders(String serverName, int port, String forwardedProto,
          String forwardedHost, int forwardedPort, String originHeader) throws Exception {

    MockRequest request = new MockRequest();
    request.setServerName(serverName);
    if (port != -1) {
      request.setServerPort(port);
    }
    if (forwardedProto != null) {
      request.addHeader("X-Forwarded-Proto", forwardedProto);
    }
    if (forwardedHost != null) {
      request.addHeader("X-Forwarded-Host", forwardedHost);
    }
    if (forwardedPort != -1) {
      request.addHeader("X-Forwarded-Port", String.valueOf(forwardedPort));
    }
    request.addHeader(HttpHeaders.ORIGIN, originHeader);

    MockHttpContext context = new MockHttpContext(request);
    assertThat(WebUtils.isSameOrigin(context))
            .isEqualTo(checkSameOrigin("http", serverName, port, originHeader));
  }

  private void testWithForwardedHeader(String serverName, int port, String forwardedHeader,
          String originHeader) throws Exception {

    MockRequest request = new MockRequest();
    request.setServerName(serverName);
    if (port != -1) {
      request.setServerPort(port);
    }
    request.addHeader("Forwarded", forwardedHeader);
    request.addHeader(HttpHeaders.ORIGIN, originHeader);

    MockHttpContext context = new MockHttpContext(request);
    assertThat(WebUtils.isSameOrigin(context))
            .isEqualTo(checkSameOrigin("http", serverName, port, originHeader));
  }

}
