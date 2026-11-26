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

import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.TimeZone;

import infra.core.i18n.SimpleLocaleContext;
import infra.core.i18n.TimeZoneAwareLocaleContext;
import infra.web.ServerHttpExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class FixedLocaleResolverTests {

  private final ServerHttpExchange request = mock(ServerHttpExchange.class);

  @Test
  void usesJvmDefaultLocaleWhenCreatedWithoutArguments() {
    FixedLocaleResolver resolver = new FixedLocaleResolver();

    assertThat(resolver.resolveLocale(request)).isEqualTo(Locale.getDefault());
    assertThat(resolver.resolveLocaleContext(request).getLocale()).isEqualTo(Locale.getDefault());
  }

  @Test
  void resolvesConfiguredLocaleWithoutConsultingRequest() {
    FixedLocaleResolver resolver = new FixedLocaleResolver(Locale.JAPAN);

    assertThat(resolver.resolveLocale(request)).isEqualTo(Locale.JAPAN);
    verifyNoInteractions(request);
  }

  @Test
  void fallsBackToJvmDefaultWhenNoLocaleIsConfigured() {
    FixedLocaleResolver resolver = new FixedLocaleResolver(null);

    assertThat(resolver.resolveLocale(request)).isEqualTo(Locale.getDefault());
    verifyNoInteractions(request);
  }

  @Test
  void localeContextExposesConfiguredLocaleAndTimeZone() {
    TimeZone timeZone = TimeZone.getTimeZone("Asia/Tokyo");
    FixedLocaleResolver resolver = new FixedLocaleResolver(Locale.JAPAN, timeZone);

    assertThat(resolver.resolveLocaleContext(request)).isInstanceOfSatisfying(TimeZoneAwareLocaleContext.class, context -> {
      assertThat(context.getLocale()).isEqualTo(Locale.JAPAN);
      assertThat(context.getTimeZone()).isEqualTo(timeZone);
    });
  }

  @Test
  void localeContextHasNoTimeZoneByDefault() {
    FixedLocaleResolver resolver = new FixedLocaleResolver(Locale.JAPAN);

    assertThat(resolver.resolveLocaleContext(request)).isInstanceOfSatisfying(TimeZoneAwareLocaleContext.class,
            context -> assertThat(context.getTimeZone()).isNull());
  }

  @Test
  void rejectsRequestScopedLocaleChanges() {
    FixedLocaleResolver resolver = new FixedLocaleResolver(Locale.JAPAN);

    assertThatThrownBy(() -> resolver.setLocale(request, Locale.FRANCE))
            .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> resolver.setLocaleContext(request, new SimpleLocaleContext(Locale.FRANCE)))
            .isInstanceOf(UnsupportedOperationException.class);
    assertThat(resolver.resolveLocale(request)).isEqualTo(Locale.JAPAN);
  }

}
