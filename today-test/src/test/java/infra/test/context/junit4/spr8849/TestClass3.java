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

package infra.test.context.junit4.spr8849;

import org.junit.Test;
import org.junit.runner.RunWith;

import javax.sql.DataSource;

import infra.context.annotation.Configuration;
import infra.context.annotation.ImportResource;
import infra.test.context.ContextConfiguration;
import infra.test.context.junit4.JUnit4ClassRunner;
import jakarta.annotation.Resource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This name of this class intentionally does not end with "Test" or "Tests"
 * since it should only be run as part of the test suite: {@link Spr8849Tests}.
 *
 * @author Sam Brannen
 * @see Spr8849Tests
 * @since 4.0
 */
@RunWith(JUnit4ClassRunner.class)
@ContextConfiguration
public class TestClass3 {

  @Configuration
  @ImportResource("classpath:/infra/test/context/junit4/spr8849/datasource-config-with-auto-generated-db-name.xml")
  static class Config {
  }

  @Resource
  DataSource dataSource;

  @Test
  public void dummyTest() {
    assertThat(dataSource).isNotNull();
  }

}
