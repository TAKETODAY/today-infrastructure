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

package infra.app.test.context;

import org.junit.jupiter.api.Test;

import infra.app.InfraConfiguration;
import infra.app.test.context.example.ExampleConfig;
import infra.app.test.context.example.scan.Example;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/6/27 21:38
 */
class AnnotatedClassFinderTests {

  private final AnnotatedClassFinder finder = new AnnotatedClassFinder(InfraConfiguration.class);

  @Test
  void findFromClassWhenSourceIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> this.finder.findFromClass((Class<?>) null))
            .withMessageContaining("Source is required");
  }

  @Test
  void findFromPackageWhenSourceIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> this.finder.findFromPackage((String) null))
            .withMessageContaining("Source is required");
  }

  @Test
  void findFromPackageWhenNoConfigurationFoundShouldReturnNull() {
    Class<?> config = this.finder.findFromPackage("infra.app");
    assertThat(config).isNull();
  }

  @Test
  void findFromClassWhenConfigurationIsFoundShouldReturnConfiguration() {
    Class<?> config = this.finder.findFromClass(Example.class);
    assertThat(config).isEqualTo(ExampleConfig.class);
  }

  @Test
  void findFromPackageWhenConfigurationIsFoundShouldReturnConfiguration() {
    Class<?> config = this.finder.findFromPackage("infra.app.test.context.example.scan");
    assertThat(config).isEqualTo(ExampleConfig.class);
  }

}