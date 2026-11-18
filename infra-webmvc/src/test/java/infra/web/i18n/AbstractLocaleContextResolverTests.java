/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.i18n;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.TimeZone;

import infra.core.i18n.LocaleContext;
import infra.core.i18n.SimpleLocaleContext;
import infra.web.HttpContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class AbstractLocaleContextResolverTests {

  private final HttpContext request = mock(HttpContext.class);

  private final TestLocaleContextResolver resolver = new TestLocaleContextResolver();

  @Test
  void prefersLocaleContextOverConfiguredDefaultAndRequestLocale() {
    resolver.setLocaleContext(request, new SimpleLocaleContext(Locale.JAPAN));
    resolver.setDefaultLocale(Locale.FRANCE);
    given(request.getLocale()).willReturn(Locale.GERMANY);

    assertThat(resolver.resolveLocale(request)).isEqualTo(Locale.JAPAN);
  }

  @Test
  void prefersConfiguredDefaultOverRequestLocaleWhenContextHasNoLocale() {
    resolver.setDefaultLocale(Locale.FRANCE);
    given(request.getLocale()).willReturn(Locale.GERMANY);

    assertThat(resolver.resolveLocale(request)).isEqualTo(Locale.FRANCE);
  }

  @Test
  void fallsBackToRequestLocaleWhenNoDefaultIsConfigured() {
    given(request.getLocale()).willReturn(Locale.GERMANY);

    assertThat(resolver.resolveLocale(request)).isEqualTo(Locale.GERMANY);
  }

  @Test
  void fallsBackToJvmDefaultWhenNoLocaleIsAvailable() {
    assertThat(resolver.resolveLocale(request)).isEqualTo(Locale.getDefault());
  }

  @Test
  void supportsSettingAndClearingDefaultTimeZone() {
    TimeZone timeZone = TimeZone.getTimeZone("Asia/Tokyo");

    assertThat(resolver.getDefaultTimeZone()).isNull();
    resolver.setDefaultTimeZone(timeZone);
    assertThat(resolver.getDefaultTimeZone()).isEqualTo(timeZone);
    resolver.setDefaultTimeZone(null);
    assertThat(resolver.getDefaultTimeZone()).isNull();
  }

  @Test
  void setLocaleWrapsLocaleAndClearsContextOnNull() {
    resolver.setLocale(request, Locale.JAPAN);
    assertThat(resolver.localeContext).isInstanceOfSatisfying(SimpleLocaleContext.class,
            context -> assertThat(context.getLocale()).isEqualTo(Locale.JAPAN));

    resolver.setLocale(request, null);
    assertThat(resolver.localeContext).isNull();
  }

  private static final class TestLocaleContextResolver extends AbstractLocaleContextResolver {

    private @Nullable LocaleContext localeContext;

    @Override
    public LocaleContext resolveLocaleContext(HttpContext request) {
      return this.localeContext != null ? this.localeContext : new SimpleLocaleContext(null);
    }

    @Override
    public void setLocaleContext(HttpContext request, @Nullable LocaleContext localeContext) {
      this.localeContext = localeContext;
    }
  }

}
