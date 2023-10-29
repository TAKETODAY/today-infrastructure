/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.test.context.cache;

import org.junit.jupiter.api.Test;

import java.util.Set;

import cn.taketoday.context.ApplicationContextInitializer;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.test.context.CacheAwareContextLoaderDelegate;
import cn.taketoday.test.context.ContextLoader;
import cn.taketoday.test.context.MergedContextConfiguration;
import cn.taketoday.test.context.support.DelegatingSmartContextLoader;

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