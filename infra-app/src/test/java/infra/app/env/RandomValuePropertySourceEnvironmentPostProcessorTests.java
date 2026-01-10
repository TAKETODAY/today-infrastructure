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

package infra.app.env;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import infra.app.Application;
import infra.app.context.config.ConfigDataEnvironmentPostProcessor;
import infra.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RandomValuePropertySourceEnvironmentPostProcessor}.
 *
 * @author Phillip Webb
 */
class RandomValuePropertySourceEnvironmentPostProcessorTests {

  private final RandomValuePropertySourceEnvironmentPostProcessor postProcessor =
          new RandomValuePropertySourceEnvironmentPostProcessor();

  @Test
  void getOrderIsBeforeConfigData() {
    assertThat(this.postProcessor.getOrder()).isLessThan(ConfigDataEnvironmentPostProcessor.ORDER);
  }

  @Test
  void postProcessEnvironmentAddsPropertySource() {
    MockEnvironment environment = new MockEnvironment();
    this.postProcessor.postProcessEnvironment(environment, mock(Application.class));
    Assertions.assertThat(environment.getProperty("random.string")).isNotNull();
  }

}
