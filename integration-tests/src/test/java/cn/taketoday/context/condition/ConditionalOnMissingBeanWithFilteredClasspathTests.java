/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.context.condition;

import org.junit.jupiter.api.Test;

import cn.taketoday.cache.support.CaffeineCacheManager;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;

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
            type = "cn.taketoday.context.condition.ConditionalOnMissingBeanWithFilteredClasspathTests.TestCacheManager")
    String foo() {
      return "foo";
    }

  }

  static class TestCacheManager extends CaffeineCacheManager {

  }

}
