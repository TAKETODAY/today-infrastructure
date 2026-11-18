/*
 * #%L
 * MariaDB4j
 * %%
 * Copyright (C) 2014 Michael Vorburger
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.mariadb4j;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import infra.beans.factory.annotation.Autowired;
import infra.core.testfixture.net.TestSocketUtils;
import infra.mariadb4j.config.MariaDB4jAutoConfiguration;
import infra.test.context.ContextConfiguration;
import infra.test.context.DynamicPropertyRegistry;
import infra.test.context.DynamicPropertySource;
import infra.test.context.junit.jupiter.InfraExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests overriding the default MariaDB4j port with an environment property.
 *
 * @author Michael Vorburger
 */
@ExtendWith(InfraExtension.class)
@ContextConfiguration(classes = MariaDB4jAutoConfiguration.class)
class MariaDB4jNewDefaultsOverriddenByValueTests {

  private static final int PORT = TestSocketUtils.findAvailableTcpPort();

  @DynamicPropertySource
  static void mariaDbProperties(DynamicPropertyRegistry registry) {
    registry.add("mariadb4j.port", () -> PORT);
    // The macOS temporary directory can make the default Unix socket path too long.
    registry.add("mariadb4j.socket", () -> "/tmp/mariadb4j-" + PORT + ".sock");
  }

  @Autowired
  MariaDB4jLifecycle lifecycle;

  @BeforeEach
  public void setUp() {
    if (!lifecycle.isRunning()) {
      lifecycle.start(); // Only start if not already running
    }
  }

  @Test
  public void testNewDefaults() {
    assertThat(lifecycle.mariadb.getConfiguration().getPort()).isEqualTo(PORT);
  }

  @AfterEach
  public void tearDown() {
    if (lifecycle.isRunning()) {
      lifecycle.stop();
    }
  }
}
