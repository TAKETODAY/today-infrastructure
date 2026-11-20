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

package infra.jdbc.object;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Types;

import infra.dao.IncorrectResultSizeDataAccessException;
import infra.dao.TypeMismatchDataAccessException;
import infra.jdbc.IncorrectResultSetColumnCountException;
import infra.jdbc.datasource.embedded.EmbeddedDatabase;
import infra.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import infra.jdbc.datasource.embedded.EmbeddedDatabaseType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SqlFunctionTests {

  private final EmbeddedDatabase database = new EmbeddedDatabaseBuilder()
          .generateUniqueName(true).setType(EmbeddedDatabaseType.H2).build();

  @AfterEach
  void shutDownDatabase() {
    database.shutdown();
  }

  @Test
  void runsWithoutParametersUsingBeanConfiguration() {
    var function = new SqlFunction<Long>();
    function.setDataSource(database);
    function.setSql("SELECT 42");
    function.setResultType(Long.class);
    function.compile();

    assertThat(function.run()).isEqualTo(42);
    assertThat(function.runGeneric()).isEqualTo(42L);
  }

  @Test
  void supportsSingleIntegerAndMultipleParameters() {
    var absolute = new SqlFunction<Number>(database, "SELECT ABS(CAST(? AS INTEGER))", new int[] { Types.INTEGER });
    var sum = new SqlFunction<>(database, "SELECT CAST(? AS INTEGER) + CAST(? AS INTEGER)",
            new int[] { Types.INTEGER, Types.INTEGER }, Long.class);

    assertThat(absolute.run(-3)).isEqualTo(3);
    assertThat(absolute.runGeneric(-4)).isEqualTo(4);
    assertThat(sum.run(12, 30)).isEqualTo(42);
    assertThat(sum.runGeneric(new Object[] { 12, 30 })).isEqualTo(42L);
  }

  @Test
  void returnsTextOnlyThroughGenericApi() {
    var function = new SqlFunction<String>(database, "SELECT UPPER('hello')");

    assertThat(function.runGeneric()).isEqualTo("HELLO");
    assertThatThrownBy(function::run).isInstanceOf(TypeMismatchDataAccessException.class).hasMessageContaining("HELLO");
  }

  @Test
  void rejectsMoreThanOneRowOrColumn() {
    var rows = new SqlFunction<Integer>(database, "VALUES (1), (2)");
    var columns = new SqlFunction<Integer>(database, "SELECT 1, 2");

    assertThatThrownBy(rows::runGeneric).isInstanceOf(IncorrectResultSizeDataAccessException.class);
    assertThatThrownBy(columns::runGeneric).isInstanceOf(IncorrectResultSetColumnCountException.class);
  }

}
