/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.docker.compose.lifecycle;

import org.junit.jupiter.api.Test;

import java.util.concurrent.FutureTask;

import static org.assertj.core.api.Assertions.assertThat;

class DockerComposeSkipCheckTests {

  private final DockerComposeSkipCheck check = new DockerComposeSkipCheck();

  private final DockerComposeProperties.Skip properties = new DockerComposeProperties.Skip();

  @Test
  void skipsWhenCalledFromJUnitByDefault() {
    assertThat(this.check.shouldSkip(getClass().getClassLoader(), this.properties)).isTrue();
  }

  @Test
  void doesNotSkipWhenTestsExplicitlyOptIn() {
    this.properties.setInTests(false);

    assertThat(this.check.shouldSkip(getClass().getClassLoader(), this.properties)).isFalse();
  }

  @Test
  void doesNotSkipIfTestFrameworkIsNotOnTheApplicationClasspath() {
    assertThat(this.check.shouldSkip(ClassLoader.getPlatformClassLoader(), this.properties)).isFalse();
  }

  @Test
  void doesNotSkipJustBecauseJUnitIsOnTheClasspath() throws Exception {
    var task = new FutureTask<>(() -> this.check.shouldSkip(getClass().getClassLoader(), this.properties));
    new Thread(task).start();

    assertThat(task.get()).isFalse();
  }
}
