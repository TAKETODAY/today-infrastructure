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

package infra.web.view;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AbstractCachingViewResolverTests {

  private final AbstractCachingViewResolver resolver = new AbstractCachingViewResolver() {
    @Override
    protected View loadView(String viewName, Locale locale) {
      return mock(View.class);
    }
  };

  @Test
  void removesOnlyTheRequestedNameAndLocale() throws Exception {
    View english = resolver.resolveViewName("report", Locale.ENGLISH);
    View french = resolver.resolveViewName("report", Locale.FRENCH);
    View other = resolver.resolveViewName("summary", Locale.ENGLISH);

    assertThat(english).isNotNull().isNotSameAs(french);
    resolver.removeFromCache("report", Locale.ENGLISH);

    assertThat(resolver.resolveViewName("report", Locale.ENGLISH)).isNotNull().isNotSameAs(english);
    assertThat(resolver.resolveViewName("report", Locale.FRENCH)).isSameAs(french);
    assertThat(resolver.resolveViewName("summary", Locale.ENGLISH)).isSameAs(other);
  }

  @Test
  void clearsAllCachedViews() throws Exception {
    View report = resolver.resolveViewName("report", Locale.ENGLISH);
    View summary = resolver.resolveViewName("summary", Locale.ENGLISH);

    resolver.clearCache();

    assertThat(resolver.resolveViewName("report", Locale.ENGLISH)).isNotNull().isNotSameAs(report);
    assertThat(resolver.resolveViewName("summary", Locale.ENGLISH)).isNotNull().isNotSameAs(summary);
  }

  @Test
  void evictsCachedViewWhenCapacityIsExceeded() throws Exception {
    resolver.setCacheLimit(1);
    View report = resolver.resolveViewName("report", Locale.ENGLISH);
    View summary = resolver.resolveViewName("summary", Locale.ENGLISH);

    assertThat(resolver.resolveViewName("summary", Locale.ENGLISH)).isSameAs(summary);
    assertThat(resolver.resolveViewName("report", Locale.ENGLISH)).isNotNull().isNotSameAs(report);
  }

  @Test
  void resolvesASeparateViewEachTimeWhenCachingIsDisabled() throws Exception {
    resolver.setCache(false);
    View report = resolver.resolveViewName("report", Locale.ENGLISH);

    assertThat(resolver.resolveViewName("report", Locale.ENGLISH)).isNotNull().isNotSameAs(report);
  }

}
