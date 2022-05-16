/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.i18n;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.framework.web.servlet.context.AnnotationConfigServletWebApplicationContext;
import cn.taketoday.framework.web.session.EnableWebSession;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextUtils;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;
import cn.taketoday.web.testfixture.servlet.MockHttpServletResponse;
import jakarta.servlet.http.HttpSession;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/3 23:52
 */
public class SessionLocaleResolverTests {

  AnnotationConfigServletWebApplicationContext webApplicationContext
          = new AnnotationConfigServletWebApplicationContext(SessionConfig.class);

  @EnableWebSession
  @Configuration
  static class SessionConfig {

  }

  @Test
  public void testResolveLocale() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    RequestContext context = new ServletRequestContext(webApplicationContext, request, response);

    RequestContextUtils.getRequiredSession(context)
            .setAttribute(SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME, Locale.GERMAN);

    SessionLocaleResolver resolver = new SessionLocaleResolver();
    assertThat(resolver.resolveLocale(context)).isEqualTo(Locale.GERMAN);
  }

  @Test
  public void testSetAndResolveLocale() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    RequestContext context = new ServletRequestContext(webApplicationContext, request, response);

    SessionLocaleResolver resolver = new SessionLocaleResolver();
    resolver.setLocale(context, Locale.GERMAN);
    assertThat(resolver.resolveLocale(context)).isEqualTo(Locale.GERMAN);

    HttpSession session = request.getSession();
    request = new MockHttpServletRequest();
    request.setSession(session);
    resolver = new SessionLocaleResolver();

    assertThat(resolver.resolveLocale(context)).isEqualTo(Locale.GERMAN);
  }

  @Test
  public void testResolveLocaleWithoutSession() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addPreferredLocale(Locale.TAIWAN);
    RequestContext context = new ServletRequestContext(webApplicationContext, request, null);

    SessionLocaleResolver resolver = new SessionLocaleResolver();

    assertThat(resolver.resolveLocale(context)).isEqualTo(request.getLocale());
  }

  @Test
  public void testResolveLocaleWithoutSessionAndDefaultLocale() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addPreferredLocale(Locale.TAIWAN);
    RequestContext context = new ServletRequestContext(webApplicationContext, request, null);

    SessionLocaleResolver resolver = new SessionLocaleResolver();
    resolver.setDefaultLocale(Locale.GERMAN);

    assertThat(resolver.resolveLocale(context)).isEqualTo(Locale.GERMAN);
  }

  @Test
  public void testSetLocaleToNullLocale() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    RequestContext context = new ServletRequestContext(webApplicationContext, request, response);

    request.addPreferredLocale(Locale.TAIWAN);
    RequestContextUtils.getRequiredSession(context)
            .setAttribute(SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME, Locale.GERMAN);

    SessionLocaleResolver resolver = new SessionLocaleResolver();
    resolver.setLocale(context, null);
    Locale locale = (Locale) request.getSession().getAttribute(SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME);
    assertThat(locale).isNull();

    HttpSession session = request.getSession();
    request = new MockHttpServletRequest();
    request.addPreferredLocale(Locale.TAIWAN);
    request.setSession(session);
    resolver = new SessionLocaleResolver();
    assertThat(resolver.resolveLocale(context)).isEqualTo(Locale.TAIWAN);
  }

}
