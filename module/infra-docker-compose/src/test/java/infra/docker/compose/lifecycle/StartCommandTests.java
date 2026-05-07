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

package infra.docker.compose.lifecycle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import infra.app.logging.LogLevel;
import infra.docker.compose.core.DockerCompose;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link StartCommand}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class StartCommandTests {

  private DockerCompose dockerCompose;

  @BeforeEach
  void setUp() {
    this.dockerCompose = mock(DockerCompose.class);
  }

  @Test
  void applyToWhenUp() {
    StartCommand.UP.applyTo(this.dockerCompose, LogLevel.INFO, Collections.emptyList());
    then(this.dockerCompose).should().up(LogLevel.INFO, Collections.emptyList());
  }

  @Test
  void applyToWhenStart() {
    StartCommand.START.applyTo(this.dockerCompose, LogLevel.INFO, Collections.emptyList());
    then(this.dockerCompose).should().start(LogLevel.INFO, Collections.emptyList());
  }

}
