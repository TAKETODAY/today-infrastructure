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

package infra.web.i18n;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Locale;

import infra.context.support.StaticApplicationContext;
import infra.http.HttpMethod;
import infra.web.ServerHttpExchange;
import infra.web.LocaleResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class LocaleChangeInterceptorTests {

  private final LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();

  private final LocaleResolver resolver = mock(LocaleResolver.class);

  private final Object handler = new Object();

  @ParameterizedTest
  @CsvSource({ "GET, true", "POST, true", "HEAD, false" })
  void onlyConfiguredMethodsChangeLocale(HttpMethod method, boolean expectedChange) {
    interceptor.setLocaleResolver(resolver);
    interceptor.setHttpMethods(HttpMethod.GET, HttpMethod.POST);
    ServerHttpExchange context = localeChangeRequest(method, "fr");

    assertThat(interceptor.preProcessing(context, handler)).isTrue();
    if (expectedChange) {
      verify(resolver).setLocale(context, Locale.FRENCH);
    }
    else {
      verifyNoInteractions(resolver);
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void nullOrEmptyConfigurationAllowsAllMethods(boolean nullConfiguration) {
    interceptor.setLocaleResolver(resolver);
    interceptor.setHttpMethods(nullConfiguration ? null : new HttpMethod[0]);
    ServerHttpExchange context = localeChangeRequest(HttpMethod.PATCH, "fr");

    assertThat(interceptor.preProcessing(context, handler)).isTrue();
    verify(resolver).setLocale(context, Locale.FRENCH);
  }

  @Test
  void missingLocaleParameterLeavesLocaleUnchanged() {
    interceptor.setLocaleResolver(resolver);
    ServerHttpExchange context = mock(ServerHttpExchange.class);

    assertThat(interceptor.preProcessing(context, handler)).isTrue();
    verifyNoInteractions(resolver);
  }

  @ParameterizedTest
  @ValueSource(strings = { "fr_CA", "fr-CA" })
  void customParameterAcceptsLegacyAndBcp47LocaleFormats(String value) {
    interceptor.setLocaleResolver(resolver);
    interceptor.setParamName("lang");
    ServerHttpExchange context = mock(ServerHttpExchange.class);
    given(context.getMethod()).willReturn(HttpMethod.GET);
    given(context.getParameter("lang")).willReturn(value);
    given(context.getParameter(LocaleChangeInterceptor.DEFAULT_PARAM_NAME)).willReturn("en");

    assertThat(interceptor.preProcessing(context, handler)).isTrue();
    verify(resolver).setLocale(context, Locale.CANADA_FRENCH);
  }

  @Test
  void findsLocaleResolverInRequestApplicationContext() {
    try (StaticApplicationContext applicationContext = new StaticApplicationContext()) {
      applicationContext.getBeanFactory().registerSingleton(LocaleResolver.BEAN_NAME, resolver);
      ServerHttpExchange context = localeChangeRequest(HttpMethod.GET, "fr");
      given(context.getApplicationContext()).willReturn(applicationContext);

      assertThat(interceptor.preProcessing(context, handler)).isTrue();
      verify(resolver).setLocale(context, Locale.FRENCH);
    }
  }

  @Test
  void throwsWhenLocaleChangeIsRequestedWithoutResolver() {
    ServerHttpExchange context = localeChangeRequest(HttpMethod.GET, "fr");

    assertThatIllegalStateException().isThrownBy(() -> interceptor.preProcessing(context, handler))
            .withMessage("No LocaleResolver found");
  }

  @Test
  void rejectsInvalidLocaleByDefault() {
    interceptor.setLocaleResolver(resolver);
    ServerHttpExchange context = localeChangeRequest(HttpMethod.GET, "fr!");

    assertThatIllegalArgumentException().isThrownBy(() -> interceptor.preProcessing(context, handler));
    verifyNoInteractions(resolver);
  }

  @Test
  void ignoresInvalidLocaleWhenConfigured() {
    interceptor.setLocaleResolver(resolver);
    interceptor.setIgnoreInvalidLocale(true);
    ServerHttpExchange context = localeChangeRequest(HttpMethod.GET, "fr!");

    assertThat(interceptor.preProcessing(context, handler)).isTrue();
    verifyNoInteractions(resolver);
  }

  private ServerHttpExchange localeChangeRequest(HttpMethod method, String locale) {
    ServerHttpExchange context = mock(ServerHttpExchange.class);
    given(context.getParameter(LocaleChangeInterceptor.DEFAULT_PARAM_NAME)).willReturn(locale);
    given(context.getMethod()).willReturn(method);
    return context;
  }

}
