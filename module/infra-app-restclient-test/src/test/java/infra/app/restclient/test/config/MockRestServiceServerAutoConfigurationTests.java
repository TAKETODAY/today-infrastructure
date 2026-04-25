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

package infra.app.restclient.test.config;

import org.junit.jupiter.api.Test;

import infra.app.restclient.test.MockServerRestClientCustomizer;
import infra.app.restclient.test.MockServerRestTemplateCustomizer;

import infra.app.test.context.runner.ApplicationContextRunner;
import infra.context.annotation.config.AutoConfigurations;
import infra.test.web.client.MockRestServiceServer;
import infra.test.web.client.RequestExpectationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MockRestServiceServerAutoConfiguration}.
 *
 * @author HuitaePark
 * @author Andy Wilkinson
 */
class MockRestServiceServerAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withPropertyValues("infra.test.restclient.mockrestserviceserver.enabled=true")
          .withConfiguration(AutoConfigurations.of(MockRestServiceServerAutoConfiguration.class));

  @Test
  void registersMockServerRestClientCustomizer() {
    this.contextRunner.run((context) -> assertThat(context).hasSingleBean(MockServerRestClientCustomizer.class));
  }

  @Test
  void registersMockServerRestTemplateCustomizer() {
    this.contextRunner.run((context) -> assertThat(context).hasSingleBean(MockServerRestTemplateCustomizer.class));
  }

  @Test
  void registersMockRestServiceServer() {
    this.contextRunner.run((context) -> assertThat(context).hasSingleBean(MockRestServiceServer.class));
  }

  @Test
  void backsOffWhenUserProvidesMockServerRestClientCustomizer() {
    this.contextRunner.withBean("userMockServerRestClientCustomizer", MockServerRestClientCustomizer.class)
            .run((context) -> assertThat(context).hasSingleBean(MockServerRestClientCustomizer.class)
                    .hasBean("userMockServerRestClientCustomizer"));
  }

  @Test
  void backsOffWhenUserProvidesMockServerRestTemplateCustomizer() {
    this.contextRunner.withBean("userMockServerRestTemplateCustomizer", MockServerRestTemplateCustomizer.class)
            .run((context) -> assertThat(context).hasSingleBean(MockServerRestTemplateCustomizer.class)
                    .hasBean("userMockServerRestTemplateCustomizer"));
  }

  @Test
  void backsOffWhenUserProvidesMockRestServiceServer() {
    this.contextRunner
            .withBean("userMockRestServiceServer", MockRestServiceServer.class, mock(RequestExpectationManager.class))
            .run((context) -> assertThat(context).hasSingleBean(MockRestServiceServer.class)
                    .hasBean("userMockRestServiceServer"));
  }

}
