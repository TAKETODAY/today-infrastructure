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

package infra.jdbc.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/8 21:21
 */
class SqlReturnUpdateCountTests {

  @Test
  void shouldCreateSqlReturnUpdateCountWithName() {
    String name = "updateCount";

    SqlReturnUpdateCount parameter = new SqlReturnUpdateCount(name);

    assertThat(parameter.getName()).isEqualTo(name);
    assertThat(parameter.getSqlType()).isEqualTo(java.sql.Types.INTEGER);
    assertThat(parameter.getScale()).isNull();
    assertThat(parameter.getTypeName()).isNull();
  }

  @Test
  void shouldAlwaysReturnFalseForIsInputValueProvided() {
    String name = "updateCount";

    SqlReturnUpdateCount parameter = new SqlReturnUpdateCount(name);

    assertThat(parameter.isInputValueProvided()).isFalse();
  }

  @Test
  void shouldAlwaysReturnTrueForIsResultsParameter() {
    String name = "updateCount";

    SqlReturnUpdateCount parameter = new SqlReturnUpdateCount(name);

    assertThat(parameter.isResultsParameter()).isTrue();
  }

  @Test
  void shouldInheritFromSqlParameter() {
    String name = "updateCount";

    SqlReturnUpdateCount parameter = new SqlReturnUpdateCount(name);

    assertThat(parameter).isInstanceOf(SqlParameter.class);
  }

  @Test
  void shouldHandleEmptyName() {
    String name = "";

    SqlReturnUpdateCount parameter = new SqlReturnUpdateCount(name);

    assertThat(parameter.getName()).isEqualTo(name);
    assertThat(parameter.getSqlType()).isEqualTo(java.sql.Types.INTEGER);
  }

}