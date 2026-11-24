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

package infra.test.context.cache;

import org.junit.jupiter.api.Test;

import infra.context.ApplicationContext;
import infra.context.ConfigurableApplicationContext;
import infra.test.context.MergedContextConfiguration;
import infra.test.context.cache.ContextCache.PauseMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ContextCacheEdgeTests {

  @Test
  void nullLoadedContextsAndUnregisteringMissingContextsAreRejected() {
    var cache = new DefaultContextCache(2, PauseMode.ALWAYS);
    var key = key();
    assertThatIllegalStateException().isThrownBy(() -> cache.put(key, config -> null))
            .withMessage("LoadFunction must return a non-null ApplicationContext");
    assertThatIllegalStateException().isThrownBy(() -> cache.unregisterContextUsage(key, getClass()))
            .withMessageContaining("ApplicationContext must not be null");
  }

  @Test
  void nonConfigurableAndAlreadyStoppedContextsAreNotPaused() {
    for (ApplicationContext context : new ApplicationContext[] {
            mock(ApplicationContext.class), mock(ConfigurableApplicationContext.class) }) {
      var cache = new DefaultContextCache(2, PauseMode.ALWAYS);
      var key = key();
      cache.put(key, context);
      cache.registerContextUsage(key, getClass());
      cache.unregisterContextUsage(key, getClass());
      assertThat(cache.getContextUsageCount()).isZero();
      if (context instanceof ConfigurableApplicationContext configurable) {
        verify(configurable, never()).pause();
      }
      assertThat(cache.toString()).contains("size = 1", "maxSize = 2", "failureCount = 0");
      cache.logStatistics();
      cache.clear();
    }
  }

  private MergedContextConfiguration key() {
    return new MergedContextConfiguration(getClass(), null, null, null, null);
  }

}
