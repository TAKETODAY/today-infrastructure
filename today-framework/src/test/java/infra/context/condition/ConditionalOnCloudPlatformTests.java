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

package infra.context.condition;

import org.junit.jupiter.api.Test;

import infra.annotation.ConditionalOnCloudPlatform;
import infra.app.cloud.CloudPlatform;
import infra.app.test.context.runner.ApplicationContextRunner;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnCloudPlatform @ConditionalOnCloudPlatform}.
 */
class ConditionalOnCloudPlatformTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

  @Test
  void outcomeWhenCloudfoundryPlatformNotPresentShouldNotMatch() {
    this.contextRunner.withUserConfiguration(CloudFoundryPlatformConfig.class)
            .run((context) -> assertThat(context).doesNotHaveBean("foo"));
  }

  @Test
  void outcomeWhenCloudfoundryPlatformPresentShouldMatch() {
    this.contextRunner.withUserConfiguration(CloudFoundryPlatformConfig.class)
            .withPropertyValues("VCAP_APPLICATION:---").run((context) -> assertThat(context).hasBean("foo"));
  }

  @Test
  void outcomeWhenCloudfoundryPlatformPresentAndMethodTargetShouldMatch() {
    this.contextRunner.withUserConfiguration(CloudFoundryPlatformOnMethodConfig.class)
            .withPropertyValues("VCAP_APPLICATION:---").run((context) -> assertThat(context).hasBean("foo"));
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnCloudPlatform(CloudPlatform.CLOUD_FOUNDRY)
  static class CloudFoundryPlatformConfig {

    @Bean
    String foo() {
      return "foo";
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class CloudFoundryPlatformOnMethodConfig {

    @Bean
    @ConditionalOnCloudPlatform(CloudPlatform.CLOUD_FOUNDRY)
    String foo() {
      return "foo";
    }

  }

}
