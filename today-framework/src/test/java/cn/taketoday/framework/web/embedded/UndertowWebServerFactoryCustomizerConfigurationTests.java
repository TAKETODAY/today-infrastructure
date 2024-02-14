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

package cn.taketoday.framework.web.embedded;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import cn.taketoday.annotation.config.web.embedded.EmbeddedWebServerFactoryCustomizerAutoConfiguration;
import cn.taketoday.annotation.config.web.embedded.EmbeddedWebServerFactoryCustomizerAutoConfiguration.UndertowWebServerFactoryCustomizerConfiguration;
import cn.taketoday.context.annotation.config.AutoConfigurations;
import cn.taketoday.core.task.VirtualThreadTaskExecutor;
import cn.taketoday.framework.test.context.runner.WebApplicationContextRunner;
import cn.taketoday.framework.web.embedded.undertow.UndertowDeploymentInfoCustomizer;
import cn.taketoday.framework.web.servlet.context.AnnotationConfigServletWebApplicationContext;
import io.undertow.servlet.api.DeploymentInfo;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link UndertowWebServerFactoryCustomizerConfiguration}.
 *
 * @author Moritz Halbritter
 */
class UndertowWebServerFactoryCustomizerConfigurationTests {

  private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner(
          AnnotationConfigServletWebApplicationContext::new)
          .withConfiguration(AutoConfigurations.of(EmbeddedWebServerFactoryCustomizerAutoConfiguration.class));

  @EnabledForJreRange(min = JRE.JAVA_21)
  @Test
  void shouldUseVirtualThreadsIfEnabled() {
    this.contextRunner.withPropertyValues("infra.threads.virtual.enabled=true").run((context) -> {
      assertThat(context).hasSingleBean(UndertowDeploymentInfoCustomizer.class);
      assertThat(context).hasBean("virtualThreadsUndertowDeploymentInfoCustomizer");
      UndertowDeploymentInfoCustomizer customizer = context.getBean(UndertowDeploymentInfoCustomizer.class);
      DeploymentInfo deploymentInfo = new DeploymentInfo();
      customizer.customize(deploymentInfo);
      assertThat(deploymentInfo.getExecutor()).isInstanceOf(VirtualThreadTaskExecutor.class);
    });
  }

}
