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
