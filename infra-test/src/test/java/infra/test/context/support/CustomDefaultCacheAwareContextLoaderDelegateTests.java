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
