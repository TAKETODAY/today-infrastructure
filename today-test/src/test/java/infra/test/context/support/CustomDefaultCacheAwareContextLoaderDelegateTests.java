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

package infra.test.context.support;

import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;

import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.support.GenericApplicationContext;
import infra.lang.TodayStrategies;
import infra.test.context.CacheAwareContextLoaderDelegate;
import infra.test.context.MergedContextConfiguration;
import infra.test.context.cache.DefaultCacheAwareContextLoaderDelegate;
import infra.test.context.junit.jupiter.JUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Integration tests for configuring a custom default {@link CacheAwareContextLoaderDelegate}
 * via {@link TodayStrategies}.
 *
 * @author sbrannen
 * @since 4.0
 */
class CustomDefaultCacheAwareContextLoaderDelegateTests {

  @Test
  void customDefaultCacheAwareContextLoaderDelegateConfiguredViaTodayStrategies() {
    String key = CacheAwareContextLoaderDelegate.DEFAULT_CACHE_AWARE_CONTEXT_LOADER_DELEGATE_PROPERTY_NAME;

    try {
      TodayStrategies.setProperty(key, AotCacheAwareContextLoaderDelegate.class.getName());

      EngineTestKit.engine("junit-jupiter")//
              .selectors(selectClass(TestCase.class))//
              .execute()//
              .testEvents()//
              .assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));
    }
    finally {
      TodayStrategies.setProperty(key, null);
    }
  }

  @JUnitConfig
  static class TestCase {

    @Test
    void test(@Autowired String foo) {
      // foo will be "bar" unless the AotCacheAwareContextLoaderDelegate is registered.
      assertThat(foo).isEqualTo("AOT");
    }

    @Configuration
    static class Config {

      @Bean
      String foo() {
        return "bar";
      }
    }
  }

  static class AotCacheAwareContextLoaderDelegate extends DefaultCacheAwareContextLoaderDelegate {

    @Override
    protected ApplicationContext loadContextInternal(MergedContextConfiguration mergedContextConfig) {
      GenericApplicationContext applicationContext = new GenericApplicationContext();
      applicationContext.registerBean("foo", String.class, () -> "AOT");
      applicationContext.refresh();
      return applicationContext;
    }
  }

}
