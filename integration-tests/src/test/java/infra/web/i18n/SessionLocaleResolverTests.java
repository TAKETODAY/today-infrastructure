/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.web.i18n;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import infra.context.annotation.Configuration;
import infra.mock.api.http.HttpSession;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.session.config.EnableWebSession;
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

  @EnableWebSession
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
