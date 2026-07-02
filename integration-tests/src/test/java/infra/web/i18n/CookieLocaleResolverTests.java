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

import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Configuration;
import infra.core.i18n.LocaleContext;
import infra.core.i18n.SimpleLocaleContext;
import infra.core.i18n.SimpleTimeZoneAwareLocaleContext;
import infra.core.i18n.TimeZoneAwareLocaleContext;
import infra.mock.api.MockException;
import infra.mock.api.http.Cookie;
import infra.mock.web.MockRequest;
import infra.mock.web.MockResponse;
import infra.session.config.EnableSession;
import infra.web.mock.MockRequestContext;
import infra.web.util.WebUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/3 23:52
 */
public class CookieLocaleResolverTests {

  AnnotationConfigApplicationContext webApplicationContext = new AnnotationConfigApplicationContext();

  {
    webApplicationContext.register(SessionLocaleResolverTests.SessionConfig.class);
    webApplicationContext.refresh();
  }

  @EnableSession
  @Configuration
  static class SessionConfig {

  }

  @Test
  public void testResolveLocale() {
    MockRequest request = new MockRequest();
    Cookie cookie = new Cookie("LanguageKoekje", "nl");
    request.setCookies(cookie);
    MockRequestContext requestContext = new MockRequestContext(request);

    CookieLocaleResolver resolver = new CookieLocaleResolver();
    resolver.setCookieName("LanguageKoekje");
    Locale loc = resolver.resolveLocale(requestContext);
    assertThat(loc.getLanguage()).isEqualTo("nl");
  }

  @Test
  public void testResolveLocaleContext() {
    MockRequest request = new MockRequest();
    Cookie cookie = new Cookie("LanguageKoekje", "nl");
    request.setCookies(cookie);
    MockRequestContext requestContext = new MockRequestContext(request);

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
    MockRequest request = new MockRequest();
    Cookie cookie = new Cookie("LanguageKoekje", "nl GMT+1");
    request.setCookies(cookie);
    MockRequestContext requestContext = new MockRequestContext(request);

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
    MockRequest request = new MockRequest();
    Cookie cookie = new Cookie("LanguageKoekje", "++ GMT+1");
    request.setCookies(cookie);
    MockRequestContext requestContext = new MockRequestContext(request);

    CookieLocaleResolver resolver = new CookieLocaleResolver();
    resolver.setCookieName("LanguageKoekje");
    assertThatIllegalStateException()
            .isThrownBy(() -> resolver.resolveLocaleContext(requestContext))
            .withMessageContaining("LanguageKoekje")
            .withMessageContaining("++ GMT+1");
  }

  @Test
  public void testResolveLocaleContextWithInvalidLocaleOnErrorDispatch() {
    MockRequest request = new MockRequest();
    MockRequestContext requestContext = new MockRequestContext(request);

    request.addPreferredLocale(Locale.GERMAN);
    request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, new MockException());
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
    MockRequest request = new MockRequest();
    Cookie cookie = new Cookie("LanguageKoekje", "nl X-MT");
    request.setCookies(cookie);
    MockRequestContext requestContext = new MockRequestContext(request);

    CookieLocaleResolver resolver = new CookieLocaleResolver();
    resolver.setCookieName("LanguageKoekje");
    assertThatIllegalStateException()
            .isThrownBy(() -> resolver.resolveLocaleContext(requestContext))
            .withMessageContaining("LanguageKoekje")
            .withMessageContaining("nl X-MT");
  }

  @Test
  public void testResolveLocaleContextWithInvalidTimeZoneOnErrorDispatch() {
    MockRequest request = new MockRequest();
    request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, new MockException());
    Cookie cookie = new Cookie("LanguageKoekje", "nl X-MT");
    request.setCookies(cookie);
    MockRequestContext requestContext = new MockRequestContext(request);

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
    MockRequest request = new MockRequest();
    MockResponse response = new MockResponse();
    MockRequestContext requestContext = new MockRequestContext(request, response);

    CookieLocaleResolver resolver = new CookieLocaleResolver();
    resolver.setLocale(requestContext, new Locale("nl", ""));

    Cookie cookie = response.getCookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
    assertThat(cookie).isNotNull();
    assertThat(cookie.getName()).isEqualTo(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
    assertThat(cookie.getDomain()).isNull();
    assertThat(cookie.getPath()).isEqualTo(CookieLocaleResolver.DEFAULT_COOKIE_PATH);
    assertThat(cookie.getSecure()).isFalse();

    request = new MockRequest();
    request.setCookies(cookie);

    resolver = new CookieLocaleResolver();
    Locale loc = resolver.resolveLocale(requestContext);
    assertThat(loc.getLanguage()).isEqualTo("nl");
  }

  @Test
  public void testSetAndResolveLocaleContext() {
    MockRequest request = new MockRequest();
    MockResponse response = new MockResponse();
    MockRequestContext requestContext = new MockRequestContext(request, response);

    CookieLocaleResolver resolver = new CookieLocaleResolver();
    resolver.setLocaleContext(requestContext, new SimpleLocaleContext(new Locale("nl", "")));

    Cookie cookie = response.getCookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
    request = new MockRequest();
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
    MockRequest request = new MockRequest();
    MockResponse response = new MockResponse();
    MockRequestContext requestContext = new MockRequestContext(request, response);

    CookieLocaleResolver resolver = new CookieLocaleResolver();
    resolver.setLocaleContext(requestContext,
            new SimpleTimeZoneAwareLocaleContext(new Locale("nl", ""), TimeZone.getTimeZone("GMT+1")));

    Cookie cookie = response.getCookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
    request = new MockRequest();
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
    MockRequest request = new MockRequest();
    MockResponse response = new MockResponse();
    MockRequestContext requestContext = new MockRequestContext(request, response);

    CookieLocaleResolver resolver = new CookieLocaleResolver();
    resolver.setLocaleContext(requestContext,
            new SimpleTimeZoneAwareLocaleContext(null, TimeZone.getTimeZone("GMT+1")));

    Cookie cookie = response.getCookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
    request = new MockRequest();
    request.addPreferredLocale(Locale.GERMANY);
    request.setCookies(cookie);

    requestContext = new MockRequestContext(request, response);

    resolver = new CookieLocaleResolver();
    LocaleContext loc = resolver.resolveLocaleContext(requestContext);
    assertThat(loc.getLocale()).isEqualTo(Locale.GERMANY);
    boolean condition = loc instanceof TimeZoneAwareLocaleContext;
    assertThat(condition).isTrue();
    assertThat(((TimeZoneAwareLocaleContext) loc).getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+1"));
  }

  @Test
  public void testSetAndResolveLocaleWithCountry() {
    MockRequest request = new MockRequest();
    MockResponse response = new MockResponse();
    MockRequestContext requestContext = new MockRequestContext(request, response);

    CookieLocaleResolver resolver = new CookieLocaleResolver();
    resolver.setLocale(requestContext, new Locale("de", "AT"));

    Cookie cookie = response.getCookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
    assertThat(cookie).isNotNull();
    assertThat(cookie.getName()).isEqualTo(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
    assertThat(cookie.getDomain()).isNull();
    assertThat(cookie.getPath()).isEqualTo(CookieLocaleResolver.DEFAULT_COOKIE_PATH);
    assertThat(cookie.getSecure()).isFalse();
    assertThat(cookie.getValue()).isEqualTo("de-AT");

    request = new MockRequest();
    request.setCookies(cookie);

    resolver = new CookieLocaleResolver();
    Locale loc = resolver.resolveLocale(requestContext);
    assertThat(loc.getLanguage()).isEqualTo("de");
    assertThat(loc.getCountry()).isEqualTo("AT");
  }

  @Test
  public void testSetAndResolveLocaleWithCountryAsLegacyJava() {
    MockRequest request = new MockRequest();
    MockResponse response = new MockResponse();
    MockRequestContext requestContext = new MockRequestContext(request, response);

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

    request = new MockRequest();
    request.setCookies(cookie);

    resolver = new CookieLocaleResolver();
    Locale loc = resolver.resolveLocale(requestContext);
    assertThat(loc.getLanguage()).isEqualTo("de");
    assertThat(loc.getCountry()).isEqualTo("AT");
  }

  @Test
  public void testCustomCookie() {
    MockRequest request = new MockRequest();
    MockResponse response = new MockResponse();
    MockRequestContext requestContext = new MockRequestContext(request, response);

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

    request = new MockRequest();
    request.setCookies(cookie);

    resolver = new CookieLocaleResolver();
    resolver.setCookieName("LanguageKoek");
    Locale loc = resolver.resolveLocale(requestContext);
    assertThat(loc.getLanguage()).isEqualTo("nl");
  }

  @Test
  public void testResolveLocaleWithoutCookie() {
    MockRequest request = new MockRequest();
    request.addPreferredLocale(Locale.TAIWAN);
    MockRequestContext requestContext = new MockRequestContext(request);

    CookieLocaleResolver resolver = new CookieLocaleResolver();

    Locale loc = resolver.resolveLocale(requestContext);
    assertThat(loc).isEqualTo(request.getLocale());
  }

  @Test
  public void testResolveLocaleContextWithoutCookie() {
    MockRequest request = new MockRequest();
    request.addPreferredLocale(Locale.TAIWAN);
    MockRequestContext requestContext = new MockRequestContext(request);

    CookieLocaleResolver resolver = new CookieLocaleResolver();

    LocaleContext loc = resolver.resolveLocaleContext(requestContext);
    assertThat(loc.getLocale()).isEqualTo(request.getLocale());
    boolean condition = loc instanceof TimeZoneAwareLocaleContext;
    assertThat(condition).isTrue();
    assertThat(((TimeZoneAwareLocaleContext) loc).getTimeZone()).isNull();
  }

  @Test
  public void testResolveLocaleWithoutCookieAndDefaultLocale() {
    MockRequest request = new MockRequest();
    request.addPreferredLocale(Locale.TAIWAN);
    MockRequestContext requestContext = new MockRequestContext(request);

    CookieLocaleResolver resolver = new CookieLocaleResolver();
    resolver.setDefaultLocale(Locale.GERMAN);

    Locale loc = resolver.resolveLocale(requestContext);
    assertThat(loc).isEqualTo(Locale.GERMAN);
  }

  @Test
  public void testResolveLocaleContextWithoutCookieAndDefaultLocale() {
    MockRequest request = new MockRequest();
    request.addPreferredLocale(Locale.TAIWAN);
    MockRequestContext requestContext = new MockRequestContext(request);

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
    MockRequest request = new MockRequest();
    request.addPreferredLocale(Locale.TAIWAN);
    Cookie cookie = new Cookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME, "");
    request.setCookies(cookie);
    MockRequestContext requestContext = new MockRequestContext(request);

    CookieLocaleResolver resolver = new CookieLocaleResolver();

    Locale loc = resolver.resolveLocale(requestContext);
    assertThat(loc).isEqualTo(request.getLocale());
  }

  @Test
  public void testResolveLocaleContextWithCookieWithoutLocale() {
    MockRequest request = new MockRequest();
    request.addPreferredLocale(Locale.TAIWAN);
    Cookie cookie = new Cookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME, "");
    request.setCookies(cookie);
    MockRequestContext requestContext = new MockRequestContext(request);

    CookieLocaleResolver resolver = new CookieLocaleResolver();

    LocaleContext loc = resolver.resolveLocaleContext(requestContext);
    assertThat(loc.getLocale()).isEqualTo(request.getLocale());
    boolean condition = loc instanceof TimeZoneAwareLocaleContext;
    assertThat(condition).isTrue();
    assertThat(((TimeZoneAwareLocaleContext) loc).getTimeZone()).isNull();
  }

  @Test
  public void testSetLocaleToNull() {
    MockRequest request = new MockRequest();
    MockResponse response = new MockResponse();

    MockRequestContext requestContext = new MockRequestContext(request, response);

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
    MockRequest request = new MockRequest();
    request.addPreferredLocale(Locale.TAIWAN);
    MockResponse response = new MockResponse();

    MockRequestContext requestContext = new MockRequestContext(request, response);

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
    MockRequest request = new MockRequest();
    request.addPreferredLocale(Locale.TAIWAN);
    Cookie cookie = new Cookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME, Locale.UK.toString());
    request.setCookies(cookie);
    MockResponse response = new MockResponse();
    MockRequestContext requestContext = new MockRequestContext(request, response);

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
    MockRequest request = new MockRequest();
    MockResponse response = new MockResponse();

    MockRequestContext requestContext = new MockRequestContext(request, response);

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
