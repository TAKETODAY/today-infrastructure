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

package infra.app.webmvc.test.config;

import org.junit.jupiter.api.Test;

import infra.app.test.context.runner.WebApplicationContextRunner;
import infra.context.annotation.config.AutoConfigurations;
import infra.test.context.FilteredClassLoader;
import infra.test.web.mock.MockMvc;
import infra.test.web.mock.RequestBuilder;
import infra.test.web.mock.assertj.MockMvcTester;
import infra.web.DispatcherHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MockMvcAutoConfiguration}.
 *
 * @author Madhura Bhave
 * @author Brian Clozel
 * @author Stephane Nicoll
 */
class MockMvcAutoConfigurationTests {

  private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(MockMvcAutoConfiguration.class));

  @Test
  void registersDispatcherHandlerFromMockMvc() {
    this.contextRunner.run((context) -> {
      MockMvc mockMvc = context.getBean(MockMvc.class);
      assertThat(context).hasSingleBean(DispatcherHandler.class);
      assertThat(context.getBean(DispatcherHandler.class)).isEqualTo(mockMvc.getDispatcher());
    });
  }

  @Test
  void registersMockMvcTester() {
    this.contextRunner.run((context) -> assertThat(context).hasSingleBean(MockMvcTester.class));
  }

  @Test
  void shouldNotRegisterMockMvcTesterIfAssertJMissing() {
    this.contextRunner.withClassLoader(new FilteredClassLoader(org.assertj.core.api.Assert.class))
            .run((context) -> assertThat(context).doesNotHaveBean(MockMvcTester.class));
  }

  @Test
  void registeredMockMvcTesterDelegatesToConfiguredMockMvc() {
    MockMvc mockMvc = mock(MockMvc.class);
    this.contextRunner.withBean("customMockMvc", MockMvc.class, () -> mockMvc).run((context) -> {
      assertThat(context).hasSingleBean(MockMvc.class).hasSingleBean(MockMvcTester.class);
      MockMvcTester mvc = context.getBean(MockMvcTester.class);
      mvc.get().uri("/dummy").exchange();
      then(mockMvc).should().perform(any(RequestBuilder.class));
    });
  }

}
