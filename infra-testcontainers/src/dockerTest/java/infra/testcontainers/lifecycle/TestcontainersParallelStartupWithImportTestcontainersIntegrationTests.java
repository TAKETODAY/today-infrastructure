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

package infra.testcontainers.lifecycle;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.postgresql.PostgreSQLContainer;

import infra.app.test.system.CapturedOutput;
import infra.app.test.system.OutputCaptureExtension;
import infra.test.context.TestPropertySource;
import infra.test.context.junit.jupiter.InfraExtension;
import infra.test.testcontainers.DisabledIfDockerUnavailable;
import infra.test.testcontainers.TestImage;
import infra.testcontainers.context.ImportTestcontainers;
import infra.testcontainers.lifecycle.TestcontainersParallelStartupWithImportTestcontainersIntegrationTests.Containers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for parallel startup.
 *
 * @author Phillip Webb
 */
@ExtendWith(InfraExtension.class)
@TestPropertySource(properties = "infra.testcontainers.beans.startup=parallel")
@DisabledIfDockerUnavailable
@ExtendWith({ OutputCaptureExtension.class, ResetStartablesExtension.class })
@ImportTestcontainers(Containers.class)
class TestcontainersParallelStartupWithImportTestcontainersIntegrationTests {

  @Test
  void startsInParallel(CapturedOutput out) {
    assertThat(out).contains("-lifecycle-0").contains("-lifecycle-1").contains("-lifecycle-2");
  }

  static class Containers {

    @Container
    static PostgreSQLContainer container1 = TestImage.container(PostgreSQLContainer.class);

    @Container
    static PostgreSQLContainer container2 = TestImage.container(PostgreSQLContainer.class);

    @Container
    static PostgreSQLContainer container3 = TestImage.container(PostgreSQLContainer.class);

  }

}
