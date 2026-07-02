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

import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Configuration;
import infra.web.mock.MockRequest;
import infra.web.mock.MockResponse;
import infra.session.Session;
import infra.session.config.EnableSession;
import infra.web.DispatcherHandler;
import infra.web.RequestContext;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;

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
  public void testResolveLocale() {
    MockRequest request = new MockRequest();
    MockResponse response = new MockResponse();
    RequestContext context = new MockRequestContext(webApplicationContext, request, response, new DispatcherHandler(webApplicationContext));

    context.getSession()
            .setAttribute(SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME, Locale.GERMAN);

    SessionLocaleResolver resolver = new SessionLocaleResolver();
    assertThat(resolver.resolveLocale(context)).isEqualTo(Locale.GERMAN);
  }

  @Test
  public void testSetAndResolveLocale() {
    MockRequest request = new MockRequest();
    MockResponse response = new MockResponse();
    RequestContext context = new MockRequestContext(webApplicationContext, request, response);

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
    RequestContext context = new MockRequestContext(webApplicationContext, request, null);

    SessionLocaleResolver resolver = new SessionLocaleResolver();

    assertThat(resolver.resolveLocale(context)).isEqualTo(request.getLocale());
  }

  @Test
  public void testResolveLocaleWithoutSessionAndDefaultLocale() throws Exception {
    MockRequest request = new MockRequest();
    request.addPreferredLocale(Locale.TAIWAN);
    RequestContext context = new MockRequestContext(webApplicationContext, request, null);

    SessionLocaleResolver resolver = new SessionLocaleResolver();
    resolver.setDefaultLocale(Locale.GERMAN);

    assertThat(resolver.resolveLocale(context)).isEqualTo(Locale.GERMAN);
  }

  @Test
  public void testSetLocaleToNullLocale() throws Exception {
    MockRequest request = new MockRequest();
    MockResponse response = new MockResponse();
    RequestContext context = new MockRequestContext(webApplicationContext, request, response, new DispatcherHandler(webApplicationContext));

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
