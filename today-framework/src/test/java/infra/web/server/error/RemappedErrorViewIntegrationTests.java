/*
 * Copyright 2017 - 2026 the original author or authors.
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

package infra.web.server.error;

import org.junit.jupiter.api.Test;

import infra.annotation.config.context.PropertyPlaceholderAutoConfiguration;
import infra.annotation.config.http.HttpMessageConvertersAutoConfiguration;
import infra.annotation.config.task.TaskExecutionAutoConfiguration;
import infra.annotation.config.web.ErrorMvcAutoConfiguration;
import infra.annotation.config.web.RandomPortWebServerConfig;
import infra.annotation.config.web.WebMvcAutoConfiguration;
import infra.app.test.context.InfraTest;
import infra.app.test.context.InfraTest.WebEnvironment;
import infra.app.test.web.client.TestRestTemplate;
import infra.app.test.web.server.LocalServerPort;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.stereotype.Controller;
import infra.test.annotation.DirtiesContext;
import infra.web.annotation.RequestMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for remapped error pages.
 *
 * @author Dave Syer
 */
@InfraTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "server.error.path:/infra/error")
@DirtiesContext
class RemappedErrorViewIntegrationTests {

  @LocalServerPort
  private int port;

  private final TestRestTemplate template = new TestRestTemplate();

  @Test
  void directAccessToErrorPage() {
    String content = this.template.getForObject("http://localhost:" + this.port + "/infra/error", String.class);
    assertThat(content).contains("error");
    assertThat(content).contains("999");
  }

  @Configuration(proxyBeanMethods = false)
  @Import({ RandomPortWebServerConfig.class, TaskExecutionAutoConfiguration.class,
          PropertyPlaceholderAutoConfiguration.class, WebMvcAutoConfiguration.class,
          HttpMessageConvertersAutoConfiguration.class, ErrorMvcAutoConfiguration.class })
  @Controller
  static class TestConfiguration {

    @RequestMapping("/")
    String home() {
      throw new RuntimeException("Planned!");
    }

  }

}
