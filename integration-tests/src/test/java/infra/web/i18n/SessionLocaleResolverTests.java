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

import infra.context.annotation.Configuration;
import infra.mock.api.http.HttpSession;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.session.config.EnableSession;
import infra.web.RequestContext;
import infra.web.RequestContextUtils;
import infra.web.mock.MockRequestContext;
import infra.web.mock.support.AnnotationConfigWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/3 23:52
 */
public class SessionLocaleResolverTests {

  AnnotationConfigWebApplicationContext webApplicationContext
          = new AnnotationConfigWebApplicationContext();

  {
    webApplicationContext.register(SessionConfig.class);
    webApplicationContext.refresh();
  }

  @EnableSession
  @Configuration
  static class SessionConfig {

  }

  @Test
  public void testResolveLocale() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    RequestContext context = new MockRequestContext(webApplicationContext, request, response);

    RequestContextUtils.getRequiredSession(context)
            .setAttribute(SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME, Locale.GERMAN);

    SessionLocaleResolver resolver = new SessionLocaleResolver();
    assertThat(resolver.resolveLocale(context)).isEqualTo(Locale.GERMAN);
  }

  @Test
  public void testSetAndResolveLocale() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    RequestContext context = new MockRequestContext(webApplicationContext, request, response);

    SessionLocaleResolver resolver = new SessionLocaleResolver();
    resolver.setLocale(context, Locale.GERMAN);
    assertThat(resolver.resolveLocale(context)).isEqualTo(Locale.GERMAN);

    HttpSession session = request.getSession();
    request = new HttpMockRequestImpl();
    request.setSession(session);
    resolver = new SessionLocaleResolver();

    assertThat(resolver.resolveLocale(context)).isEqualTo(Locale.GERMAN);
  }

  @Test
  public void testResolveLocaleWithoutSession() throws Exception {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addPreferredLocale(Locale.TAIWAN);
    RequestContext context = new MockRequestContext(webApplicationContext, request, null);

    SessionLocaleResolver resolver = new SessionLocaleResolver();

    assertThat(resolver.resolveLocale(context)).isEqualTo(request.getLocale());
  }

  @Test
  public void testResolveLocaleWithoutSessionAndDefaultLocale() throws Exception {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addPreferredLocale(Locale.TAIWAN);
    RequestContext context = new MockRequestContext(webApplicationContext, request, null);

    SessionLocaleResolver resolver = new SessionLocaleResolver();
    resolver.setDefaultLocale(Locale.GERMAN);

    assertThat(resolver.resolveLocale(context)).isEqualTo(Locale.GERMAN);
  }

  @Test
  public void testSetLocaleToNullLocale() throws Exception {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    RequestContext context = new MockRequestContext(webApplicationContext, request, response);

    request.addPreferredLocale(Locale.TAIWAN);
    RequestContextUtils.getRequiredSession(context)
            .setAttribute(SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME, Locale.GERMAN);

    SessionLocaleResolver resolver = new SessionLocaleResolver();
    resolver.setLocale(context, null);
    Locale locale = (Locale) request.getSession().getAttribute(SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME);
    assertThat(locale).isNull();

    HttpSession session = request.getSession();
    request = new HttpMockRequestImpl();
    request.addPreferredLocale(Locale.TAIWAN);
    request.setSession(session);
    resolver = new SessionLocaleResolver();
    assertThat(resolver.resolveLocale(context)).isEqualTo(Locale.TAIWAN);
  }

}
