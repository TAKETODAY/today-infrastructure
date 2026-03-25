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

import java.io.File;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Configuration;
import infra.mariadb4j.config.MariaDB4jAutoConfiguration;
import infra.test.context.ContextConfiguration;
import infra.test.context.TestPropertySource;
import infra.test.context.junit.jupiter.InfraExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests programmatically setting the configuration of a MariaDB4jSpringService via setters in a
 * {@link Configuration}.
 *
 * @author Michael Vorburger
 */
@ExtendWith(InfraExtension.class)
@ContextConfiguration(classes = MariaDB4jAutoConfiguration.class)
@TestPropertySource(properties = {
        "mariadb4j.port=5677",
        "mariadb4j.baseDir=build/MariaDB4jOverrideBySetTests/baseDir",
        "mariadb4j.dataDir=build/MariaDB4jOverrideBySetTests/dataDir",
})
class MariaDB4jOverrideBySetTests {

  @Autowired
  MariaDB4jLifecycle s;

  @BeforeEach
  public void setUp() {
    if (!s.isRunning()) {
      s.start(); // Only start if not already running
    }
  }

  @Test
  public void testOverrideBySet() {
    assertEquals(5677, s.mariadb.getConfiguration().getPort());

    assertEquals(new File("build/MariaDB4jOverrideBySetTests/baseDir"), s.mariadb.getConfiguration().getBaseDir());
    assertEquals(new File("build/MariaDB4jOverrideBySetTests/dataDir"), s.mariadb.getConfiguration().getDataDir());
    assertEquals(new File("build/MariaDB4jOverrideBySetTests/baseDir/libs"), s.mariadb.getConfiguration().getLibDir());
  }

  @AfterEach
  public void tearDown() {
    if (s.isRunning()) {
      s.stop();
    }
  }
}
