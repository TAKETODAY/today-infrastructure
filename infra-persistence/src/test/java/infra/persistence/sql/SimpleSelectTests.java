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

package infra.persistence.sql;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import infra.persistence.Order;
import infra.persistence.Pageable;
import infra.persistence.platform.Platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/28 21:16
 */
class SimpleSelectTests {

  private final Platform platform = Platform.generic();

  @Test
  void quotedColumnsAndTableUsePlatformQuoting() {
    Platform custom = new Platform() {

      @Override
      public char openQuote() {
        return '[';
      }

      @Override
      public char closeQuote() {
        return ']';
      }
    };

    assertThat(new SimpleSelect().addColumn("`name`").setTableName("`users`").toStatementString(custom))
            .isEqualTo("SELECT [name] FROM [users]");
  }

  @ParameterizedTest
  @CsvSource({
          ",, '', ''",
          ",0, '', ''",
          "10,, ' FETCH FIRST 10 ROWS ONLY', ' LIMIT 10'",
          "10,0, ' FETCH FIRST 10 ROWS ONLY', ' LIMIT 10'",
          "10,20, ' OFFSET 20 ROWS FETCH FIRST 10 ROWS ONLY', ' LIMIT 10 OFFSET 20'",
          ",20, ' OFFSET 20 ROWS', ' LIMIT 18446744073709551615 OFFSET 20'",
          "0,, ' FETCH FIRST 0 ROWS ONLY', ' LIMIT 0'"
  })
  void paginationUsesPlatformSyntax(@Nullable Integer limit, @Nullable Integer offset,
          String standard, String mysql) {
    SimpleSelect select = new SimpleSelect().setTableName("t_user").addColumn("id")
            .addRestriction("name").orderBy("id").limit(limit).offset(offset);
    String base = "SELECT id FROM t_user WHERE name = ? order by id ASC";

    assertThat(select.toStatementString(platform)).isEqualTo(base + standard);
    assertThat(select.toStatementString(Platform.mysql())).isEqualTo(base + mysql);
  }

  @Test
  void pageableReplacesPaginationAndCanBeCleared() {
    SimpleSelect select = new SimpleSelect().setTableName("t_user").addColumn("id")
            .limit(1).offset(99).pageable(Pageable.of(3, 10));

    assertThat(select.toStatementString(Platform.mysql()))
            .isEqualTo("SELECT id FROM t_user LIMIT 10 OFFSET 20");
    select.limit(null);
    assertThat(select.toStatementString(platform)).isEqualTo("SELECT id FROM t_user OFFSET 20 ROWS");
    select.offset(null);
    assertThat(select.toStatementString(platform)).isEqualTo("SELECT id FROM t_user");
    select.pageable(Pageable.of(2, 10)).clearPagination();
    assertThat(select.toStatementString(Platform.mysql())).isEqualTo("SELECT id FROM t_user");
  }

  @Test
  void invalidPaginationDoesNotChangeExistingState() {
    SimpleSelect select = new SimpleSelect().setTableName("t_user").addColumn("id").limit(10).offset(20);
    Pageable invalid = new Pageable() {
      @Override
      public int pageNumber() {
        return 0;
      }

      @Override
      public int pageSize() {
        return 5;
      }
    };

    assertThatIllegalArgumentException().isThrownBy(() -> select.limit(-1));
    assertThatIllegalArgumentException().isThrownBy(() -> select.offset(-1));
    assertThatIllegalArgumentException().isThrownBy(() -> select.pageable(invalid));
    assertThat(select.toStatementString(Platform.mysql())).isEqualTo("SELECT id FROM t_user LIMIT 10 OFFSET 20");
    for (Platform dialect : new Platform[] { platform, Platform.mysql() }) {
      assertThatIllegalArgumentException()
              .isThrownBy(() -> dialect.appendPagination(new StringBuilder(), -1, null));
      assertThatIllegalArgumentException()
              .isThrownBy(() -> dialect.appendPagination(new StringBuilder(), null, -1));
    }
  }

  @Test
  void simple() {
    SimpleSelect select = new SimpleSelect();
    select.addColumn("name")
            .addColumn("age")
            .addColumns(new String[] { "id", "gender" })
            .setTableName("t_user");

    assertThat(select.toStatementString(platform)).isEqualTo("SELECT name, age, id, gender FROM t_user");
  }

  @Test
  void alias() {
    SimpleSelect select = new SimpleSelect();
    select.addColumn("name")
            .addColumn("age")
            .addColumn("user_id", "id")
            .addColumn("user_id", "id")
            .setTableName("t_user");

    assertThat(select.toStatementString(platform)).isEqualTo("SELECT name, age, user_id AS id FROM t_user");
  }

  @Test
  void where() {
    SimpleSelect select = new SimpleSelect();
    select.addColumn("name")
            .addColumn("age")
            .addColumn("user_id", "id")
            .addColumn("user_id", "id")
            .addWhereToken("id = 1")
            .addRestrictions("name", "gender")
            .addRestriction(Restrictions.notEqual("age", "1"))
            .setTableName("t_user");

    assertThat(select.toStatementString(platform)).isEqualTo(
            "SELECT name, age, user_id AS id FROM t_user WHERE id = 1 AND name = ? AND gender = ? AND age <> 1");
  }

  @Test
  void orderBy() {
    SimpleSelect select = new SimpleSelect();
    select.addColumn("name")
            .addColumn("age")
            .addWhereToken("id = 1")
            .addRestriction("name")
            .setTableName("t_user")
            .orderBy("id", Order.DESC);

    assertThat(select.toStatementString(platform)).isEqualTo(
            "SELECT name, age FROM t_user WHERE id = 1 AND name = ? order by id DESC");
  }

  @Test
  void comment() {
    SimpleSelect select = new SimpleSelect();
    select.addColumn("name")
            .addColumn("age")
            .addWhereToken("id = 1")
            .setTableName("t_user")
            .setComment("find by id")
            .orderBy("id");

    assertThat(select.toStatementString(platform)).isEqualTo(
            "/* find by id */ SELECT name, age FROM t_user WHERE id = 1 order by id ASC");
  }

  @Test
  void orderByBuilderAppendsToExistingSpec() {
    SimpleSelect select = new SimpleSelect();
    select.addColumn("name")
            .setTableName("t_user")
            .orderBy(OrderSpec.asc("a"))
            .orderBy("b");

    assertThat(select.toStatementString(platform)).isEqualTo(
            "SELECT name FROM t_user order by a ASC, b ASC");
  }

  @Test
  void orderByBuilderAppendsAfterRawSpec() {
    SimpleSelect select = new SimpleSelect();
    select.addColumn("name").setTableName("t_user");
    select.orderBy(OrderSpec.plain("x DESC"));
    select.orderBy("b");

    assertThat(select.toStatementString(platform)).isEqualTo(
            "SELECT name FROM t_user order by x DESC, b ASC");
  }

  @Test
  void orderByBuilderReplacedBySpec() {
    SimpleSelect select = new SimpleSelect();
    select.addColumn("name").setTableName("t_user");
    select.orderBy().asc("a");
    select.orderBy(OrderSpec.desc("b"));

    assertThat(select.toStatementString(platform)).isEqualTo(
            "SELECT name FROM t_user order by b DESC");
  }

}
