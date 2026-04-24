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

package infra.app.test.config.override;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import infra.app.test.config.ExampleTestConfig;
import infra.app.test.config.OverrideAutoConfiguration;
import infra.app.test.context.InfraTestContextBootstrapper;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.context.annotation.config.ImportAutoConfiguration;
import infra.context.properties.ConfigurationPropertiesBindingPostProcessor;
import infra.test.context.BootstrapWith;
import infra.test.context.junit.jupiter.InfraExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration tests for {@link OverrideAutoConfiguration @OverrideAutoConfiguration} when
 * {@code enabled} is {@code false}.
 *
 * @author Phillip Webb
 */
@ExtendWith(InfraExtension.class)
@OverrideAutoConfiguration(enabled = false)
@BootstrapWith(InfraTestContextBootstrapper.class)
@ImportAutoConfiguration(ExampleTestConfig.class)
class OverrideAutoConfigurationEnabledFalseIntegrationTests {

  @Autowired
  private ApplicationContext context;

  @Test
  void disabledAutoConfiguration() {
    ApplicationContext context = this.context;
    assertThat(context.getBean(ExampleTestConfig.class)).isNotNull();
    assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
            .isThrownBy(() -> context.getBean(ConfigurationPropertiesBindingPostProcessor.class));
  }

}
