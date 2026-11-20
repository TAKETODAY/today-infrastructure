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

package infra.web.i18n;

import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.TimeZone;

import infra.core.i18n.SimpleTimeZoneAwareLocaleContext;
import infra.core.i18n.TimeZoneAwareLocaleContext;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Configuration;
import infra.web.HttpContext;
import infra.web.mock.MockHttpContext;
import infra.web.mock.MockRequest;
import infra.web.mock.MockResponse;
import infra.session.Session;
import infra.session.config.EnableSession;
import infra.web.DispatcherHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/3 23:52
 */
public class SessionLocaleResolverTests {

  AnnotationConfigApplicationContext webApplicationContext
          = new AnnotationConfigApplicationContext();

  {
    webApplicationContext.register(SessionConfig.class);
    webApplicationContext.refresh();
  }

  @EnableSession
  @Configuration
  static class SessionConfig {

  }

  @Test
  void clearingLocaleContextWithoutSessionDoesNotCreateSession() {
    MockHttpContext context = new MockHttpContext();
    SessionLocaleResolver resolver = new SessionLocaleResolver();

    resolver.setLocale(context, null);
    resolver.setLocaleContext(context, null);

    assertThat(context.getSession(false)).isNull();
    assertThat(resolver.resolveLocale(context)).isEqualTo(Locale.getDefault());
    assertThat(context.getSession(false)).isNull();
  }

  @Test
  void customSessionAttributesAreSetReadLiveAndClearedWithoutRemovingOtherAttributes() {
    MockHttpContext context = new MockHttpContext();
    SessionLocaleResolver resolver = new SessionLocaleResolver();
    resolver.setLocaleAttributeName("language");
    resolver.setTimeZoneAttributeName("zone");
    resolver.setDefaultLocale(Locale.JAPAN);
    TimeZone defaultZone = TimeZone.getTimeZone("UTC");
    TimeZone zone = TimeZone.getTimeZone("Europe/Berlin");
    resolver.setDefaultTimeZone(defaultZone);
    TimeZoneAwareLocaleContext localeContext = (TimeZoneAwareLocaleContext) resolver.resolveLocaleContext(context);
    assertThat(localeContext.getLocale()).isEqualTo(Locale.JAPAN);
    assertThat(localeContext.getTimeZone()).isSameAs(defaultZone);
    assertThat(context.getSession(false)).isNull();

    resolver.setLocaleContext(context, new SimpleTimeZoneAwareLocaleContext(Locale.GERMANY, zone));
    Session session = context.getSession();
    session.setAttribute("other", "keep");
    assertThat(session.getAttribute("language")).isEqualTo(Locale.GERMANY);
    assertThat(session.getAttribute("zone")).isSameAs(zone);
    assertThat(localeContext.getLocale()).isEqualTo(Locale.GERMANY);
    assertThat(localeContext.getTimeZone()).isSameAs(zone);

    resolver.setLocaleContext(context, null);
    assertThat(context.getSession(false)).isSameAs(session);
    assertThat(session.getAttribute("language")).isNull();
    assertThat(session.getAttribute("zone")).isNull();
    assertThat(session.getAttribute("other")).isEqualTo("keep");
    assertThat(localeContext.getLocale()).isEqualTo(Locale.JAPAN);
    assertThat(localeContext.getTimeZone()).isSameAs(defaultZone);
  }

  @Test
  void wrongSessionAttributeTypesAreNotSilentlyIgnored() {
    MockHttpContext context = new MockHttpContext();
    SessionLocaleResolver resolver = new SessionLocaleResolver();
    Session session = context.getSession();
    session.setAttribute(SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME, "invalid");
    assertThatThrownBy(() -> resolver.resolveLocale(context)).isInstanceOf(ClassCastException.class);
    TimeZoneAwareLocaleContext localeContext = (TimeZoneAwareLocaleContext) resolver.resolveLocaleContext(context);
    assertThatThrownBy(localeContext::getLocale).isInstanceOf(ClassCastException.class);
    session.setAttribute(SessionLocaleResolver.TIME_ZONE_SESSION_ATTRIBUTE_NAME, "invalid");
    assertThatThrownBy(localeContext::getTimeZone).isInstanceOf(ClassCastException.class);
  }

  @Test
  public void testResolveLocale() {
    MockRequest request = new MockRequest();
    MockResponse response = new MockResponse();
    HttpContext context = new MockHttpContext(webApplicationContext, request, response, new DispatcherHandler(webApplicationContext));

    context.getSession()
            .setAttribute(SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME, Locale.GERMAN);

    SessionLocaleResolver resolver = new SessionLocaleResolver();
    assertThat(resolver.resolveLocale(context)).isEqualTo(Locale.GERMAN);
  }

  @Test
  public void testSetAndResolveLocale() {
    MockRequest request = new MockRequest();
    MockResponse response = new MockResponse();
    HttpContext context = new MockHttpContext(webApplicationContext, request, response);

    SessionLocaleResolver resolver = new SessionLocaleResolver();
    resolver.setLocale(context, Locale.GERMAN);
    assertThat(resolver.resolveLocale(context)).isEqualTo(Locale.GERMAN);

    Session session = request.getSession();
    request = new MockRequest();
    request.setSession(session);
    resolver = new SessionLocaleResolver();

    assertThat(resolver.resolveLocale(context)).isEqualTo(Locale.GERMAN);
  }

  @Test
  public void testResolveLocaleWithoutSession() throws Exception {
    MockRequest request = new MockRequest();
    request.addPreferredLocale(Locale.TAIWAN);
    HttpContext context = new MockHttpContext(webApplicationContext, request, null);

    SessionLocaleResolver resolver = new SessionLocaleResolver();

    assertThat(resolver.resolveLocale(context)).isEqualTo(request.getLocale());
  }

  @Test
  public void testResolveLocaleWithoutSessionAndDefaultLocale() throws Exception {
    MockRequest request = new MockRequest();
    request.addPreferredLocale(Locale.TAIWAN);
    HttpContext context = new MockHttpContext(webApplicationContext, request, null);

    SessionLocaleResolver resolver = new SessionLocaleResolver();
    resolver.setDefaultLocale(Locale.GERMAN);

    assertThat(resolver.resolveLocale(context)).isEqualTo(Locale.GERMAN);
  }

  @Test
  public void testSetLocaleToNullLocale() throws Exception {
    MockRequest request = new MockRequest();
    MockResponse response = new MockResponse();
    HttpContext context = new MockHttpContext(webApplicationContext, request, response, new DispatcherHandler(webApplicationContext));

    request.addPreferredLocale(Locale.TAIWAN);
    context.getSession().setAttribute(SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME, Locale.GERMAN);

    SessionLocaleResolver resolver = new SessionLocaleResolver();
    resolver.setLocale(context, null);
    Locale locale = (Locale) request.getSession().getAttribute(SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME);
    assertThat(locale).isNull();

    Session session = request.getSession();
    request = new MockRequest();
    request.addPreferredLocale(Locale.TAIWAN);
    request.setSession(session);
    resolver = new SessionLocaleResolver();
    assertThat(resolver.resolveLocale(context)).isEqualTo(Locale.TAIWAN);
  }

}
