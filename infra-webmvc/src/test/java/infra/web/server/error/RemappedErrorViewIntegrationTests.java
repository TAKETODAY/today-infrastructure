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

package infra.web.server.error;

import org.junit.jupiter.api.Test;

import infra.app.config.context.PropertyPlaceholderAutoConfiguration;
import infra.app.config.task.TaskExecutionAutoConfiguration;
import infra.app.rest.client.TestRestTemplate;
import infra.app.test.context.InfraTest;
import infra.app.test.context.InfraTest.WebEnvironment;
import infra.app.test.web.server.LocalServerPort;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.http.converter.HttpMessageConverters.ServerBuilder;
import infra.http.converter.config.HttpMessageConvertersAutoConfiguration;
import infra.http.converter.json.JacksonJsonHttpMessageConverter;
import infra.stereotype.Controller;
import infra.test.annotation.DirtiesContext;
import infra.web.annotation.RequestMapping;
import infra.web.config.annotation.WebMvcConfigurer;
import infra.web.server.netty.RandomPortWebServerConfig;
import infra.web.config.ErrorMvcAutoConfiguration;
import infra.web.config.WebMvcAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for remapped error pages.
 *
 * @author Dave Syer
 */
@InfraTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "error.path:/infra/error")
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
  static class TestConfiguration implements WebMvcConfigurer {

    @RequestMapping("/")
    String home() {
      throw new RuntimeException("Planned!");
    }

    @Override
    public void configureMessageConverters(ServerBuilder builder) {
      builder.disableDefaults()
              .addCustomConverter(new JacksonJsonHttpMessageConverter());
    }
  }

}
