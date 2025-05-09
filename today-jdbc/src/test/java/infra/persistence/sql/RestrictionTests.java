/*
 * Copyright 2017 - 2025 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.persistence.sql;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/5/9 14:55
 */
class RestrictionTests {

  @Test
  void shouldRenderPlainRestriction() {
    StringBuilder sqlBuffer = new StringBuilder();
    Restriction restriction = Restriction.plain("SELECT * FROM table");

    restriction.render(sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo("SELECT * FROM table");
  }

  @Test
  void shouldRenderEqualRestriction() {
    StringBuilder sqlBuffer = new StringBuilder();
    Restriction restriction = Restriction.equal("column", "value");

    restriction.render(sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo("`column` = value");
  }

  @Test
  void shouldRenderNotEqualRestriction() {
    StringBuilder sqlBuffer = new StringBuilder();
    Restriction restriction = Restriction.notEqual("column", "value");

    restriction.render(sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo("`column` <> value");
  }

  @Test
  void shouldRenderGreaterThanRestriction() {
    StringBuilder sqlBuffer = new StringBuilder();
    Restriction restriction = Restriction.graterThan("column", "value");

    restriction.render(sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo("`column` > value");
  }

  @Test
  void shouldRenderLessThanRestriction() {
    StringBuilder sqlBuffer = new StringBuilder();
    Restriction restriction = Restriction.lessThan("column", "value");

    restriction.render(sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo("`column` < value");
  }

  @Test
  void shouldRenderIsNullRestriction() {
    StringBuilder sqlBuffer = new StringBuilder();
    Restriction restriction = Restriction.isNull("column");

    restriction.render(sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo("`column` is null");
  }

  @Test
  void shouldRenderIsNotNullRestriction() {
    StringBuilder sqlBuffer = new StringBuilder();
    Restriction restriction = Restriction.isNotNull("column");

    restriction.render(sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo("`column` is not null");
  }

  @Test
  void shouldRenderMultipleRestrictionsWithAnd() {
    StringBuilder sqlBuffer = new StringBuilder();
    Restriction restriction1 = Restriction.equal("column1", "value1");
    Restriction restriction2 = Restriction.lessThan("column2", "value2");

    Restriction.render(List.of(restriction1, restriction2), sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo(" WHERE `column1` = value1 AND `column2` < value2");
  }
}