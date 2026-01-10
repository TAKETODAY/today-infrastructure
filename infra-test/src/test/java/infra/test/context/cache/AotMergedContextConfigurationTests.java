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

package infra.test.context.cache;

import org.junit.jupiter.api.Test;

import java.util.Set;

import infra.context.ApplicationContextInitializer;
import infra.context.ConfigurableApplicationContext;
import infra.test.context.CacheAwareContextLoaderDelegate;
import infra.test.context.ContextLoader;
import infra.test.context.MergedContextConfiguration;
import infra.test.context.support.DelegatingSmartContextLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/10/29 17:43
 */
class AotMergedContextConfigurationTests {

  private final CacheAwareContextLoaderDelegate delegate =
          new DefaultCacheAwareContextLoaderDelegate(mock());

  private final ContextLoader contextLoader = new DelegatingSmartContextLoader();

  private final MergedContextConfiguration mergedConfig = new MergedContextConfiguration(getClass(), null, null,
          Set.of(DemoApplicationContextInitializer.class), null, contextLoader);

  private final AotMergedContextConfiguration aotMergedConfig1 = new AotMergedContextConfiguration(getClass(),
          DemoApplicationContextInitializer.class, mergedConfig, delegate);

  private final AotMergedContextConfiguration aotMergedConfig2 = new AotMergedContextConfiguration(getClass(),
          DemoApplicationContextInitializer.class, mergedConfig, delegate);

  @Test
  void testEquals() {
    assertThat(aotMergedConfig1).isEqualTo(aotMergedConfig1);
    assertThat(aotMergedConfig1).isEqualTo(aotMergedConfig2);

    assertThat(mergedConfig).isNotEqualTo(aotMergedConfig1);
    assertThat(aotMergedConfig1).isNotEqualTo(mergedConfig);
  }

  @Test
  void testHashCode() {
    assertThat(aotMergedConfig1).hasSameHashCodeAs(aotMergedConfig2);

    assertThat(aotMergedConfig1).doesNotHaveSameHashCodeAs(mergedConfig);
  }

  static class DemoApplicationContextInitializer implements ApplicationContextInitializer {
    
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {

    }
  }

}