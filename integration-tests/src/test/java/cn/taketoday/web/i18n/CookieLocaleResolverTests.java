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

package cn.taketoday.web.i18n;

import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.TimeZone;

import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.core.i18n.LocaleContext;
import cn.taketoday.core.i18n.SimpleLocaleContext;
import cn.taketoday.core.i18n.SimpleTimeZoneAwareLocaleContext;
import cn.taketoday.core.i18n.TimeZoneAwareLocaleContext;
import cn.taketoday.mock.api.ServletException;
import cn.taketoday.mock.api.http.Cookie;
import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.session.config.EnableWebSession;
import cn.taketoday.web.mock.ServletRequestContext;
import cn.taketoday.web.mock.support.AnnotationConfigWebApplicationContext;
import cn.taketoday.web.util.WebUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/3 23:52
 */
public class CookieLocaleResolverTests {

  AnnotationConfigWebApplicationContext webApplicationContext = new AnnotationConfigWebApplicationContext();

  {
    webApplicationContext.register(SessionLocaleResolverTests.SessionConfig.class);
    webApplicationContext.refresh();
  }

  @EnableWebSession
  @Configuration
  static class SessionConfig {

  }

  @Test
  public void testResolveLocale() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    Cookie cookie = new Cookie("LanguageKoekje", "nl");
    request.setCookies(cookie);
    ServletRequestContext requestContext = new ServletRequestContext(request, null);

    CookieLocaleResolver resolver = new CookieLocaleResolver();
    resolver.setCookieName("LanguageKoekje");
    Locale loc = resolver.resolveLocale(requestContext);
    assertThat(loc.getLanguage()).isEqualTo("nl");
  }

  @Test
  public void testResolveLocaleContext() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    Cookie cookie = new Cookie("LanguageKoekje", "nl");
    request.setCookies(cookie);
    ServletRequestContext requestContext = new ServletRequestContext(request, null);

    CookieLocaleResolver resolver = new CookieLocaleResolver();
    resolver.setCookieName("LanguageKoekje");
    LocaleContext loc = resolver.resolveLocaleContext(requestContext);
    assertThat(loc.getLocale().getLanguage()).isEqualTo("nl");
    boolean condition = loc instanceof TimeZoneAwareLocaleContext;
    assertThat(condition).isTrue();
    assertThat(((TimeZoneAwareLocaleContext) loc).getTimeZone()).isNull();
  }

  @Test
  public void testResolveLocaleContextWithTimeZone() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    Cookie cookie = new Cookie("LanguageKoekje", "nl GMT+1");
    request.setCookies(cookie);
    ServletRequestContext requestContext = new ServletRequestContext(request, null);

    CookieLocaleResolver resolver = new CookieLocaleResolver();
    resolver.setCookieName("LanguageKoekje");
    LocaleContext loc = resolver.resolveLocaleContext(requestContext);
    assertThat(loc.getLocale().getLanguage()).isEqualTo("nl");
    boolean condition = loc instanceof TimeZoneAwareLocaleContext;
    assertThat(condition).isTrue();
    assertThat(((TimeZoneAwareLocaleContext) loc).getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+1"));
  }

  @Test
  public void testResolveLocaleContextWithInvalidLocale() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    Cookie cookie = new Cookie("LanguageKoekje", "++ GMT+1");
    request.setCookies(cookie);
    ServletRequestContext requestContext = new ServletRequestContext(request, null);

    CookieLocaleResolver resolver = new CookieLocaleResolver();
    resolver.setCookieName("LanguageKoekje");
    assertThatIllegalStateException()
            .isThrownBy(() -> resolver.resolveLocaleContext(requestContext))
            .withMessageContaining("LanguageKoekje")
            .withMessageContaining("++ GMT+1");
  }

  @Test
  public void testResolveLocaleContextWithInvalidLocaleOnErrorDispatch() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    ServletRequestContext requestContext = new ServletRequestContext(request, null);

    request.addPreferredLocale(Locale.GERMAN);
    request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, new ServletException());
    Cookie cookie = new Cookie("LanguageKoekje", "++ GMT+1");
    request.setCookies(cookie);

    CookieLocaleResolver resolver = new CookieLocaleResolver();
    resolver.setDefaultTimeZone(TimeZone.getTimeZone("GMT+2"));
    resolver.setCookieName("LanguageKoekje");
    LocaleContext loc = resolver.resolveLocaleContext(requestContext);
    assertThat(loc.getLocale()).isEqualTo(Locale.GERMAN);
    boolean condition = loc instanceof TimeZoneAwareLocaleContext;
    assertThat(condition).isTrue();
    assertThat(((TimeZoneAwareLocaleContext) loc).getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+2"));
  }

  @Test
  public void testResolveLocaleContextWithInvalidTimeZone() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    Cookie cookie = new Cookie("LanguageKoekje", "nl X-MT");
    request.setCookies(cookie);
    ServletRequestContext requestContext = new ServletRequestContext(request, null);

    CookieLocaleResolver resolver = new CookieLocaleResolver();
    resolver.setCookieName("LanguageKoekje");
    assertThatIllegalStateException()
            .isThrownBy(() -> resolver.resolveLocaleContext(requestContext))
            .withMessageContaining("LanguageKoekje")
            .withMessageContaining("nl X-MT");
  }

  @Test
  public void testResolveLocaleContextWithInvalidTimeZoneOnErrorDispatch() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, new ServletException());
    Cookie cookie = new Cookie("LanguageKoekje", "nl X-MT");
    request.setCookies(cookie);
    ServletRequestContext requestContext = new ServletRequestContext(request, null);

    CookieLocaleResolver resolver = new CookieLocaleResolver();
    resolver.setDefaultTimeZone(TimeZone.getTimeZone("GMT+2"));
    resolver.setCookieName("LanguageKoekje");
    LocaleContext loc = resolver.resolveLocaleContext(requestContext);
    assertThat(loc.getLocale().getLanguage()).isEqualTo("nl");
    boolean condition = loc instanceof TimeZoneAwareLocaleContext;
    assertThat(condition).isTrue();
    assertThat(((TimeZoneAwareLocaleContext) loc).getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+2"));
  }

  @Test
  public void testSetAndResolveLocale() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockHttpServletResponse response = new MockHttpServletResponse();
    ServletRequestContext requestContext = new ServletRequestContext(request, response);

    CookieLocaleResolver resolver = new CookieLocaleResolver();
    resolver.setLocale(requestContext, new Locale("nl", ""));

    Cookie cookie = response.getCookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
    assertThat(cookie).isNotNull();
    assertThat(cookie.getName()).isEqualTo(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
    assertThat(cookie.getDomain()).isNull();
    assertThat(cookie.getPath()).isEqualTo(CookieLocaleResolver.DEFAULT_COOKIE_PATH);
    assertThat(cookie.getSecure()).isFalse();

    request = new HttpMockRequestImpl();
    request.setCookies(cookie);

    resolver = new CookieLocaleResolver();
    Locale loc = resolver.resolveLocale(requestContext);
    assertThat(loc.getLanguage()).isEqualTo("nl");
  }

  @Test
  public void testSetAndResolveLocaleContext() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockHttpServletResponse response = new MockHttpServletResponse();
    ServletRequestContext requestContext = new ServletRequestContext(request, response);

    CookieLocaleResolver resolver = new CookieLocaleResolver();
    resolver.setLocaleContext(requestContext, new SimpleLocaleContext(new Locale("nl", "")));

    Cookie cookie = response.getCookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
    request = new HttpMockRequestImpl();
    request.setCookies(cookie);

    resolver = new CookieLocaleResolver();
    LocaleContext loc = resolver.resolveLocaleContext(requestContext);
    assertThat(loc.getLocale().getLanguage()).isEqualTo("nl");
    boolean condition = loc instanceof TimeZoneAwareLocaleContext;
    assertThat(condition).isTrue();
    assertThat(((TimeZoneAwareLocaleContext) loc).getTimeZone()).isNull();
  }

  @Test
  public void testSetAndResolveLocaleContextWithTimeZone() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockHttpServletResponse response = new MockHttpServletResponse();
    ServletRequestContext requestContext = new ServletRequestContext(request, response);

    CookieLocaleResolver resolver = new CookieLocaleResolver();
    resolver.setLocaleContext(requestContext,
            new SimpleTimeZoneAwareLocaleContext(new Locale("nl", ""), TimeZone.getTimeZone("GMT+1")));

    Cookie cookie = response.getCookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
    request = new HttpMockRequestImpl();
    request.setCookies(cookie);

    resolver = new CookieLocaleResolver();
    LocaleContext loc = resolver.resolveLocaleContext(requestContext);
    assertThat(loc.getLocale().getLanguage()).isEqualTo("nl");
    boolean condition = loc instanceof TimeZoneAwareLocaleContext;
    assertThat(condition).isTrue();
    assertThat(((TimeZoneAwareLocaleContext) loc).getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+1"));
  }

  @Test
  public void testSetAndResolveLocaleContextWithTimeZoneOnly() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockHttpServletResponse response = new MockHttpServletResponse();
    ServletRequestContext requestContext = new ServletRequestContext(request, response);

    CookieLocaleResolver resolver = new CookieLocaleResolver();
    resolver.setLocaleContext(requestContext,
            new SimpleTimeZoneAwareLocaleContext(null, TimeZone.getTimeZone("GMT+1")));

    Cookie cookie = response.getCookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
    request = new HttpMockRequestImpl();
    request.addPreferredLocale(Locale.GERMANY);
    request.setCookies(cookie);

    requestContext = new ServletRequestContext(request, response);

    resolver = new CookieLocaleResolver();
    LocaleContext loc = resolver.resolveLocaleContext(requestContext);
    assertThat(loc.getLocale()).isEqualTo(Locale.GERMANY);
    boolean condition = loc instanceof TimeZoneAwareLocaleContext;
    assertThat(condition).isTrue();
    assertThat(((TimeZoneAwareLocaleContext) loc).getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+1"));
  }

  @Test
  public void testSetAndResolveLocaleWithCountry() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockHttpServletResponse response = new MockHttpServletResponse();
    ServletRequestContext requestContext = new ServletRequestContext(request, response);

    CookieLocaleResolver resolver = new CookieLocaleResolver();
    resolver.setLocale(requestContext, new Locale("de", "AT"));

    Cookie cookie = response.getCookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
    assertThat(cookie).isNotNull();
    assertThat(cookie.getName()).isEqualTo(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
    assertThat(cookie.getDomain()).isNull();
    assertThat(cookie.getPath()).isEqualTo(CookieLocaleResolver.DEFAULT_COOKIE_PATH);
    assertThat(cookie.getSecure()).isFalse();
    assertThat(cookie.getValue()).isEqualTo("de-AT");

    request = new HttpMockRequestImpl();
    request.setCookies(cookie);

    resolver = new CookieLocaleResolver();
    Locale loc = resolver.resolveLocale(requestContext);
    assertThat(loc.getLanguage()).isEqualTo("de");
    assertThat(loc.getCountry()).isEqualTo("AT");
  }

  @Test
  public void testSetAndResolveLocaleWithCountryAsLegacyJava() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockHttpServletResponse response = new MockHttpServletResponse();
    ServletRequestContext requestContext = new ServletRequestContext(request, response);

    CookieLocaleResolver resolver = new CookieLocaleResolver();
    resolver.setLanguageTagCompliant(false);
    resolver.setLocale(requestContext, new Locale("de", "AT"));

    Cookie cookie = response.getCookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
    assertThat(cookie).isNotNull();
    assertThat(cookie.getName()).isEqualTo(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
    assertThat(cookie.getDomain()).isNull();
    assertThat(cookie.getPath()).isEqualTo(CookieLocaleResolver.DEFAULT_COOKIE_PATH);
    assertThat(cookie.getSecure()).isFalse();
    assertThat(cookie.getValue()).isEqualTo("de_AT");

    request = new HttpMockRequestImpl();
    request.setCookies(cookie);

    resolver = new CookieLocaleResolver();
    Locale loc = resolver.resolveLocale(requestContext);
    assertThat(loc.getLanguage()).isEqualTo("de");
    assertThat(loc.getCountry()).isEqualTo("AT");
  }

  @Test
  public void testCustomCookie() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockHttpServletResponse response = new MockHttpServletResponse();
    ServletRequestContext requestContext = new ServletRequestContext(request, response);

    CookieLocaleResolver resolver = new CookieLocaleResolver();
    resolver.setCookieName("LanguageKoek");
    resolver.setCookieDomain(".springframework.org");
    resolver.setCookiePath("/mypath");
    resolver.setCookieMaxAge(10000);
    resolver.setCookieSecure(true);
    resolver.setLocale(requestContext, new Locale("nl", ""));

    Cookie cookie = response.getCookie("LanguageKoek");
    assertThat(cookie).isNotNull();
    assertThat(cookie.getName()).isEqualTo("LanguageKoek");
    assertThat(cookie.getDomain()).isEqualTo(".springframework.org");
    assertThat(cookie.getPath()).isEqualTo("/mypath");
    assertThat(cookie.getMaxAge()).isEqualTo(10000);
    assertThat(cookie.getSecure()).isTrue();

    request = new HttpMockRequestImpl();
    request.setCookies(cookie);

    resolver = new CookieLocaleResolver();
    resolver.setCookieName("LanguageKoek");
    Locale loc = resolver.resolveLocale(requestContext);
    assertThat(loc.getLanguage()).isEqualTo("nl");
  }

  @Test
  public void testResolveLocaleWithoutCookie() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addPreferredLocale(Locale.TAIWAN);
    ServletRequestContext requestContext = new ServletRequestContext(request, null);

    CookieLocaleResolver resolver = new CookieLocaleResolver();

    Locale loc = resolver.resolveLocale(requestContext);
    assertThat(loc).isEqualTo(request.getLocale());
  }

  @Test
  public void testResolveLocaleContextWithoutCookie() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addPreferredLocale(Locale.TAIWAN);
    ServletRequestContext requestContext = new ServletRequestContext(request, null);

    CookieLocaleResolver resolver = new CookieLocaleResolver();

    LocaleContext loc = resolver.resolveLocaleContext(requestContext);
    assertThat(loc.getLocale()).isEqualTo(request.getLocale());
    boolean condition = loc instanceof TimeZoneAwareLocaleContext;
    assertThat(condition).isTrue();
    assertThat(((TimeZoneAwareLocaleContext) loc).getTimeZone()).isNull();
  }

  @Test
  public void testResolveLocaleWithoutCookieAndDefaultLocale() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addPreferredLocale(Locale.TAIWAN);
    ServletRequestContext requestContext = new ServletRequestContext(request, null);

    CookieLocaleResolver resolver = new CookieLocaleResolver();
    resolver.setDefaultLocale(Locale.GERMAN);

    Locale loc = resolver.resolveLocale(requestContext);
    assertThat(loc).isEqualTo(Locale.GERMAN);
  }

  @Test
  public void testResolveLocaleContextWithoutCookieAndDefaultLocale() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addPreferredLocale(Locale.TAIWAN);
    ServletRequestContext requestContext = new ServletRequestContext(request, null);

    CookieLocaleResolver resolver = new CookieLocaleResolver();
    resolver.setDefaultLocale(Locale.GERMAN);
    resolver.setDefaultTimeZone(TimeZone.getTimeZone("GMT+1"));

    LocaleContext loc = resolver.resolveLocaleContext(requestContext);
    assertThat(loc.getLocale()).isEqualTo(Locale.GERMAN);
    boolean condition = loc instanceof TimeZoneAwareLocaleContext;
    assertThat(condition).isTrue();
    assertThat(((TimeZoneAwareLocaleContext) loc).getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+1"));
  }

  @Test
  public void testResolveLocaleWithCookieWithoutLocale() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addPreferredLocale(Locale.TAIWAN);
    Cookie cookie = new Cookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME, "");
    request.setCookies(cookie);
    ServletRequestContext requestContext = new ServletRequestContext(request, null);

    CookieLocaleResolver resolver = new CookieLocaleResolver();

    Locale loc = resolver.resolveLocale(requestContext);
    assertThat(loc).isEqualTo(request.getLocale());
  }

  @Test
  public void testResolveLocaleContextWithCookieWithoutLocale() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addPreferredLocale(Locale.TAIWAN);
    Cookie cookie = new Cookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME, "");
    request.setCookies(cookie);
    ServletRequestContext requestContext = new ServletRequestContext(request, null);

    CookieLocaleResolver resolver = new CookieLocaleResolver();

    LocaleContext loc = resolver.resolveLocaleContext(requestContext);
    assertThat(loc.getLocale()).isEqualTo(request.getLocale());
    boolean condition = loc instanceof TimeZoneAwareLocaleContext;
    assertThat(condition).isTrue();
    assertThat(((TimeZoneAwareLocaleContext) loc).getTimeZone()).isNull();
  }

  @Test
  public void testSetLocaleToNull() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockHttpServletResponse response = new MockHttpServletResponse();

    ServletRequestContext requestContext = new ServletRequestContext(request, response);

    request.addPreferredLocale(Locale.TAIWAN);
    Cookie cookie = new Cookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME, Locale.UK.toString());
    request.setCookies(cookie);

    CookieLocaleResolver resolver = new CookieLocaleResolver();
    resolver.setLocale(requestContext, null);
    Locale locale = (Locale) request.getAttribute(CookieLocaleResolver.LOCALE_REQUEST_ATTRIBUTE_NAME);
    assertThat(locale).isEqualTo(Locale.TAIWAN);

    Cookie[] cookies = response.getCookies();
    assertThat(cookies.length).isEqualTo(1);
    Cookie localeCookie = cookies[0];
    assertThat(localeCookie.getName()).isEqualTo(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
    assertThat(localeCookie.getValue()).isEqualTo("");
  }

  @Test
  public void testSetLocaleContextToNull() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addPreferredLocale(Locale.TAIWAN);
    MockHttpServletResponse response = new MockHttpServletResponse();

    ServletRequestContext requestContext = new ServletRequestContext(request, response);

    Cookie cookie = new Cookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME, Locale.UK.toString());
    request.setCookies(cookie);

    CookieLocaleResolver resolver = new CookieLocaleResolver();
    resolver.setLocaleContext(requestContext, null);
    Locale locale = (Locale) request.getAttribute(CookieLocaleResolver.LOCALE_REQUEST_ATTRIBUTE_NAME);
    assertThat(locale).isEqualTo(Locale.TAIWAN);
    TimeZone timeZone = (TimeZone) request.getAttribute(CookieLocaleResolver.TIME_ZONE_REQUEST_ATTRIBUTE_NAME);
    assertThat(timeZone).isNull();

    Cookie[] cookies = response.getCookies();
    assertThat(cookies.length).isEqualTo(1);
    Cookie localeCookie = cookies[0];
    assertThat(localeCookie.getName()).isEqualTo(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
    assertThat(localeCookie.getValue()).isEqualTo("");
  }

  @Test
  public void testSetLocaleToNullWithDefault() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addPreferredLocale(Locale.TAIWAN);
    Cookie cookie = new Cookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME, Locale.UK.toString());
    request.setCookies(cookie);
    MockHttpServletResponse response = new MockHttpServletResponse();
    ServletRequestContext requestContext = new ServletRequestContext(request, response);

    CookieLocaleResolver resolver = new CookieLocaleResolver();
    resolver.setDefaultLocale(Locale.CANADA_FRENCH);
    resolver.setLocale(requestContext, null);
    Locale locale = (Locale) request.getAttribute(CookieLocaleResolver.LOCALE_REQUEST_ATTRIBUTE_NAME);
    assertThat(locale).isEqualTo(Locale.CANADA_FRENCH);

    Cookie[] cookies = response.getCookies();
    assertThat(cookies.length).isEqualTo(1);
    Cookie localeCookie = cookies[0];
    assertThat(localeCookie.getName()).isEqualTo(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
    assertThat(localeCookie.getValue()).isEqualTo("");
  }

  @Test
  public void testSetLocaleContextToNullWithDefault() {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockHttpServletResponse response = new MockHttpServletResponse();

    ServletRequestContext requestContext = new ServletRequestContext(request, response);

    request.addPreferredLocale(Locale.TAIWAN);
    Cookie cookie = new Cookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME, Locale.UK.toString());
    request.setCookies(cookie);

    CookieLocaleResolver resolver = new CookieLocaleResolver();
    resolver.setDefaultLocale(Locale.CANADA_FRENCH);
    resolver.setDefaultTimeZone(TimeZone.getTimeZone("GMT+1"));
    resolver.setLocaleContext(requestContext, null);
    Locale locale = (Locale) request.getAttribute(CookieLocaleResolver.LOCALE_REQUEST_ATTRIBUTE_NAME);
    assertThat(locale).isEqualTo(Locale.CANADA_FRENCH);
    TimeZone timeZone = (TimeZone) request.getAttribute(CookieLocaleResolver.TIME_ZONE_REQUEST_ATTRIBUTE_NAME);
    assertThat(timeZone).isEqualTo(TimeZone.getTimeZone("GMT+1"));

    Cookie[] cookies = response.getCookies();
    assertThat(cookies.length).isEqualTo(1);
    Cookie localeCookie = cookies[0];
    assertThat(localeCookie.getName()).isEqualTo(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
    assertThat(localeCookie.getValue()).isEqualTo("");
  }

}
