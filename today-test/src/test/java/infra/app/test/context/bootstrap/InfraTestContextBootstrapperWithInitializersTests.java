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

package infra.app.test.context.bootstrap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.context.ApplicationContextInitializer;
import infra.context.ConfigurableApplicationContext;
import infra.app.test.context.InfraTestContextBootstrapper;
import infra.test.context.BootstrapWith;
import infra.test.context.ContextConfiguration;
import infra.test.context.junit.jupiter.InfraExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link InfraTestContextBootstrapper} with
 * {@link ApplicationContextInitializer}.
 *
 * @author Phillip Webb
 */
@ExtendWith(InfraExtension.class)
@BootstrapWith(InfraTestContextBootstrapper.class)
@ContextConfiguration(initializers = InfraTestContextBootstrapperWithInitializersTests.CustomInitializer.class)
class InfraTestContextBootstrapperWithInitializersTests {

  @Autowired
  private ApplicationContext context;

  @Test
  void foundConfiguration() {
    Object bean = this.context.getBean(InfraTestContextBootstrapperExampleConfig.class);
    assertThat(bean).isNotNull();
  }

  // gh-8483

  static class CustomInitializer implements ApplicationContextInitializer {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
    }

  }

}
