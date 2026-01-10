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

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

import infra.mock.web.HttpMockRequestImpl;
import infra.web.RequestContext;
import infra.web.mock.MockRequestContext;

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

    context.requestHeaders().setOrRemove("Accept-Language", US.toLanguageTag());
    request.setPreferredLocales(Collections.singletonList(US));
    assertThat(this.resolver.resolveLocale(context)).isEqualTo(US);
  }

  private RequestContext request(Locale... locales) {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.setPreferredLocales(Arrays.asList(locales));
    return new MockRequestContext(null, request, null);
  }

}
