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
import java.util.TimeZone;

import infra.core.i18n.LocaleContext;
import infra.core.i18n.SimpleLocaleContext;
import infra.core.i18n.SimpleTimeZoneAwareLocaleContext;
import infra.core.i18n.TimeZoneAwareLocaleContext;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockContextImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.session.DefaultSessionManager;
import infra.session.InMemorySessionRepository;
import infra.session.SecureRandomSessionIdGenerator;
import infra.session.SessionEventDispatcher;
import infra.web.LocaleContextResolver;
import infra.web.LocaleResolver;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/3 23:51
 */
public class LocaleResolverTests {

  @Test
  public void testAcceptHeaderLocaleResolver() {
    doTest(new AcceptHeaderLocaleResolver(), false);
  }

  @Test
  public void testFixedLocaleResolver() {
    doTest(new FixedLocaleResolver(Locale.UK), false);
  }

  @Test
  public void testCookieLocaleResolver() {
    doTest(new CookieLocaleResolver(), true);
  }

  @Test
  public void testSessionLocaleResolver() {
    SessionLocaleResolver localeResolver = new SessionLocaleResolver();
    localeResolver.setSessionManager(new DefaultSessionManager(new InMemorySessionRepository(
            new SessionEventDispatcher(), new SecureRandomSessionIdGenerator()), null));
    doTest(localeResolver, true);
  }

  private void doTest(LocaleResolver localeResolver, boolean shouldSet) {
    // create mocks
    MockContextImpl context = new MockContextImpl();
    HttpMockRequestImpl request = new HttpMockRequestImpl(context);
    request.addPreferredLocale(Locale.UK);
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    MockRequestContext requestContext = new MockRequestContext(request, response);
    // check original locale
    Locale locale = localeResolver.resolveLocale(requestContext);
    assertThat(locale).isEqualTo(Locale.UK);
    // set new locale
    try {
      localeResolver.setLocale(requestContext, Locale.GERMANY);
      assertThat(shouldSet).as("should not be able to set Locale").isTrue();
      // check new locale
      locale = localeResolver.resolveLocale(requestContext);
      assertThat(locale).isEqualTo(Locale.GERMANY);
    }
    catch (UnsupportedOperationException ex) {
      assertThat(shouldSet).as("should be able to set Locale").isFalse();
    }

    // check LocaleContext
    if (localeResolver instanceof LocaleContextResolver localeContextResolver) {
      LocaleContext localeContext = localeContextResolver.resolveLocaleContext(requestContext);
      if (shouldSet) {
        assertThat(localeContext.getLocale()).isEqualTo(Locale.GERMANY);
      }
      else {
        assertThat(localeContext.getLocale()).isEqualTo(Locale.UK);
      }
      boolean condition2 = localeContext instanceof TimeZoneAwareLocaleContext;
      assertThat(condition2).isTrue();
      assertThat(((TimeZoneAwareLocaleContext) localeContext).getTimeZone()).isNull();

      if (localeContextResolver instanceof AbstractLocaleContextResolver) {
        ((AbstractLocaleContextResolver) localeContextResolver).setDefaultTimeZone(TimeZone.getTimeZone("GMT+1"));
        assertThat(TimeZone.getTimeZone("GMT+1")).isEqualTo(((TimeZoneAwareLocaleContext) localeContext).getTimeZone());
      }

      try {
        localeContextResolver.setLocaleContext(requestContext, new SimpleLocaleContext(Locale.US));
        if (!shouldSet) {
          fail("should not be able to set Locale");
        }
        localeContext = localeContextResolver.resolveLocaleContext(requestContext);
        assertThat(localeContext.getLocale()).isEqualTo(Locale.US);
        if (localeContextResolver instanceof AbstractLocaleContextResolver) {
          assertThat(TimeZone.getTimeZone("GMT+1")).isEqualTo(((TimeZoneAwareLocaleContext) localeContext).getTimeZone());
        }
        else {
          assertThat(((TimeZoneAwareLocaleContext) localeContext).getTimeZone()).isNull();
        }

        localeContextResolver.setLocaleContext(requestContext,
                new SimpleTimeZoneAwareLocaleContext(Locale.GERMANY, TimeZone.getTimeZone("GMT+2")));
        localeContext = localeContextResolver.resolveLocaleContext(requestContext);
        assertThat(localeContext.getLocale()).isEqualTo(Locale.GERMANY);
        boolean condition1 = localeContext instanceof TimeZoneAwareLocaleContext;
        assertThat(condition1).isTrue();
        assertThat(TimeZone.getTimeZone("GMT+2")).isEqualTo(((TimeZoneAwareLocaleContext) localeContext).getTimeZone());

        localeContextResolver.setLocaleContext(requestContext,
                new SimpleTimeZoneAwareLocaleContext(null, TimeZone.getTimeZone("GMT+3")));
        localeContext = localeContextResolver.resolveLocaleContext(requestContext);
        assertThat(localeContext.getLocale()).isEqualTo(Locale.UK);
        boolean condition = localeContext instanceof TimeZoneAwareLocaleContext;
        assertThat(condition).isTrue();
        assertThat(TimeZone.getTimeZone("GMT+3")).isEqualTo(((TimeZoneAwareLocaleContext) localeContext).getTimeZone());

        if (localeContextResolver instanceof AbstractLocaleContextResolver) {
          ((AbstractLocaleContextResolver) localeContextResolver).setDefaultLocale(Locale.GERMANY);
          assertThat(localeContext.getLocale()).isEqualTo(Locale.GERMANY);
        }
      }
      catch (UnsupportedOperationException ex) {
        if (shouldSet) {
          fail("should be able to set Locale");
        }
      }
    }
  }

}
