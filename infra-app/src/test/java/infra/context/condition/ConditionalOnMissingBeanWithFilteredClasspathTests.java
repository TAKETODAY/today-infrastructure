/*
 * Copyright 2012-present the original author or authors.
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

package infra.context.condition;

import org.junit.jupiter.api.Test;

import infra.app.test.context.runner.ApplicationContextRunner;
import infra.cache.support.CaffeineCacheManager;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link ConditionalOnMissingBean @ConditionalOnMissingBean} with filtered
 * classpath.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
class ConditionalOnMissingBeanWithFilteredClasspathTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withUserConfiguration(OnBeanTypeConfiguration.class);

  @Test
  void testNameOnMissingBeanTypeWithMissingImport() {
    this.contextRunner.run((context) -> assertThat(context).hasBean("foo"));
  }

  @Configuration(proxyBeanMethods = false)
  static class OnBeanTypeConfiguration {

    @Bean
    @ConditionalOnMissingBean(
            type = "infra.context.condition.ConditionalOnMissingBeanWithFilteredClasspathTests.TestCacheManager")
    String foo() {
      return "foo";
    }

  }

  static class TestCacheManager extends CaffeineCacheManager {

  }

}
