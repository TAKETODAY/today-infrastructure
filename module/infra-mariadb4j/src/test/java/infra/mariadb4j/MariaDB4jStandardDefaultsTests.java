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
import infra.core.ApplicationTemp;
import infra.mariadb4j.config.MariaDB4jAutoConfiguration;
import infra.test.context.ContextConfiguration;
import infra.test.context.TestPropertySource;
import infra.test.context.junit.jupiter.InfraExtension;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the default configuration of a MariaDB4jSpringService.
 *
 * @author Michael Vorburger
 */
@ExtendWith(InfraExtension.class)
@ContextConfiguration(classes = MariaDB4jAutoConfiguration.class)
@TestPropertySource(properties = {
        "mariadb4j.socket=/tmp/mariadb.sock",
})
class MariaDB4jStandardDefaultsTests {

  @Autowired
  MariaDB4jLifecycle s;

  @BeforeEach
  public void setUp() {
    if (!s.isRunning()) {
      s.start(); // Only start if not already running
    }
  }

  @Test
  public void testStandardDefaults() {
    assertNotEquals(3306, s.mariadb.getConfiguration().getPort());
    assertTrue(s.mariadb.getConfiguration().getBaseDir().toString().contains(ApplicationTemp.instance.toString()));
    assertTrue(s.mariadb.getConfiguration().getDataDir().toString().contains(ApplicationTemp.instance.toString()));
    assertTrue(s.mariadb.getConfiguration().getTmpDir().toString().contains(ApplicationTemp.instance.toString()));
  }

  @AfterEach
  public void tearDown() {
    if (s.isRunning()) {
      s.stop();
    }
  }
}
