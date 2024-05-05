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

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

import cn.taketoday.web.RequestContext;
import cn.taketoday.web.mock.MockRequestContext;
import cn.taketoday.mock.web.HttpMockRequestImpl;

import static java.util.Locale.CANADA;
import static java.util.Locale.ENGLISH;
import static java.util.Locale.GERMAN;
import static java.util.Locale.GERMANY;
import static java.util.Locale.JAPAN;
import static java.util.Locale.JAPANESE;
import static java.util.Locale.KOREA;
import static java.util.Locale.UK;
import static java.util.Locale.US;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/3 23:47
 */
class AcceptHeaderLocaleResolverTests {

  private final AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();

  @Test
  public void resolve() {
    assertThat(this.resolver.resolveLocale(request(CANADA))).isEqualTo(CANADA);
    assertThat(this.resolver.resolveLocale(request(US, CANADA))).isEqualTo(US);
  }

  @Test
  public void resolvePreferredSupported() {
    this.resolver.setSupportedLocales(Collections.singletonList(CANADA));
    assertThat(this.resolver.resolveLocale(request(US, CANADA))).isEqualTo(CANADA);
  }

  @Test
  public void resolvePreferredNotSupported() {
    this.resolver.setSupportedLocales(Collections.singletonList(CANADA));
    assertThat(this.resolver.resolveLocale(request(US, UK))).isEqualTo(US);
  }

  @Test
  public void resolvePreferredAgainstLanguageOnly() {
    this.resolver.setSupportedLocales(Collections.singletonList(ENGLISH));
    assertThat(this.resolver.resolveLocale(request(GERMANY, US, UK))).isEqualTo(ENGLISH);
  }

  @Test
  public void resolvePreferredAgainstCountryIfPossible() {
    this.resolver.setSupportedLocales(Arrays.asList(ENGLISH, UK));
    assertThat(this.resolver.resolveLocale(request(GERMANY, US, UK))).isEqualTo(UK);
  }

  @Test
  public void resolvePreferredAgainstLanguageWithMultipleSupportedLocales() {
    this.resolver.setSupportedLocales(Arrays.asList(GERMAN, US));
    assertThat(this.resolver.resolveLocale(request(GERMANY, US, UK))).isEqualTo(GERMAN);
  }

  @Test
  public void resolvePreferredNotSupportedWithDefault() {
    this.resolver.setSupportedLocales(Arrays.asList(US, JAPAN));
    this.resolver.setDefaultLocale(Locale.JAPAN);
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockRequestContext context = new MockRequestContext(null, request, null);

    request.addHeader("Accept-Language", KOREA.toLanguageTag());
    request.setPreferredLocales(Collections.singletonList(KOREA));
    assertThat(this.resolver.resolveLocale(context)).isEqualTo(Locale.JAPAN);
  }

  @Test
  public void defaultLocale() {
    this.resolver.setDefaultLocale(JAPANESE);
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockRequestContext context = new MockRequestContext(null, request, null);
    assertThat(this.resolver.resolveLocale(context)).isEqualTo(JAPANESE);

    context.requestHeaders().set("Accept-Language", US.toLanguageTag());
    request.setPreferredLocales(Collections.singletonList(US));
    assertThat(this.resolver.resolveLocale(context)).isEqualTo(US);
  }

  private RequestContext request(Locale... locales) {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.setPreferredLocales(Arrays.asList(locales));
    return new MockRequestContext(null, request, null);
  }

}
