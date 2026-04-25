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

package infra.app.resttestclient.config;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.net.URI;

import infra.app.resttestclient.TestRestTemplate;
import infra.app.test.context.runner.WebApplicationContextRunner;
import infra.app.test.http.server.LocalTestWebServer;
import infra.context.annotation.config.AutoConfigurations;
import infra.test.classpath.resources.WithResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TestRestTemplateTestAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
class TestRestTemplateTestAutoConfigurationTests {

  private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(TestRestTemplateTestAutoConfiguration.class));

  @Test
  void shouldFailTotRegisterTestRestTemplateWithoutWebServer() {
    this.contextRunner.run((context) -> assertThat(context).hasFailed()
            .getFailure()
            .hasMessageContaining(" No local test web server available"));
  }

  @Test
  @WithResource(name = "META-INF/today.strategies", content = """
          infra.app.test.http.server.LocalTestWebServer$Provider=\
          infra.app.resttestclient.config.RestTestClientTestAutoConfigurationTests$TestLocalTestWebServerProvider
          """)
  void shouldDefineTestRestTemplateBoundToWebServer() {
    this.contextRunner.run((context) -> {
      assertThat(context).hasSingleBean(TestRestTemplate.class)
              .hasBean("infra.resttestclient.TestRestTemplate");
      TestRestTemplate testRestTemplate = context.getBean(TestRestTemplate.class);
      assertThat(testRestTemplate.getRestTemplate().getUriTemplateHandler().expand("/"))
              .isEqualTo(URI.create("https://localhost:8182/"));
    });

  }

  @SuppressWarnings("unused")
  static class TestLocalTestWebServerProvider implements LocalTestWebServer.Provider {

    @Override
    public @Nullable LocalTestWebServer getLocalTestWebServer() {
      return LocalTestWebServer.of(LocalTestWebServer.Scheme.HTTPS, 8182);
    }

  }

}
