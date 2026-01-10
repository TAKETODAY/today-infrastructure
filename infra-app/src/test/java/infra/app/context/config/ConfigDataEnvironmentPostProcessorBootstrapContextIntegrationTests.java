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

package infra.app.context.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import infra.app.Application;
import infra.app.ApplicationType;
import infra.app.BootstrapRegistry;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ConfigDataEnvironmentPostProcessor} when used with a
 * {@link BootstrapRegistry}.
 *
 * @author Phillip Webb
 */
class ConfigDataEnvironmentPostProcessorBootstrapContextIntegrationTests {

  private Application application;

  @BeforeEach
  void setup() {
    this.application = new Application(Config.class);
    this.application.setApplicationType(ApplicationType.NORMAL);
  }

  @Test
  void bootstrapsApplicationContext() {
    String args = "--app.config.import=classpath:application-bootstrap-registry-integration-tests.properties";
    ConfigurableApplicationContext context = application.run(args);
    TestConfigDataBootstrap.LoaderHelper bean = context.getBean(TestConfigDataBootstrap.LoaderHelper.class);
    assertThat(bean).isNotNull();
    assertThat(bean.getBound()).isEqualTo("igotbound");
    assertThat(bean.getProfileBound()).isEqualTo("igotprofilebound");
    assertThat(bean.getLocation().getResolverHelper().getLocation())
            .isEqualTo(ConfigDataLocation.valueOf("testbootstrap:test"));
    context.close();
  }

  @Configuration
  static class Config {

  }

}
