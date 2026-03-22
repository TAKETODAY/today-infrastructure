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
package infra.mariadb4j;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Configuration;
import infra.mariadb4j.config.MariaDB4jAutoConfiguration;
import infra.test.context.ContextConfiguration;
import infra.test.context.TestPropertySource;
import infra.test.context.junit.jupiter.InfraExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests overriding the default configuration of a MariaDB4jSpringService set in a {@link
 * Configuration} via Spring Value properties.
 *
 * @author Michael Vorburger
 */
@ExtendWith(InfraExtension.class)
@ContextConfiguration(classes = MariaDB4jAutoConfiguration.class)
@TestPropertySource(properties = { "mariadb4j.port=5678" })
class MariaDB4jNewDefaultsOverriddenByValueTests {

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
    assertEquals(5678, lifecycle.db.getConfiguration().getPort());
  }

  @AfterEach
  public void tearDown() {
    if (lifecycle.isRunning()) {
      lifecycle.stop();
    }
  }
}
