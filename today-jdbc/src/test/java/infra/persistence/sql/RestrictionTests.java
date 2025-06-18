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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

  @Test
  void logicalAnd_shouldReturnTrue_forPlainRestriction() {
    Restriction plain = Restriction.plain("test");
    assertThat(plain.logicalAnd()).isTrue();
  }

  @Test
  void graterEqual_withColumnOnly_shouldRenderCorrectly() {
    StringBuilder sqlBuffer = new StringBuilder();
    Restriction restriction = Restriction.graterEqual("column");

    restriction.render(sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo("`column` >= ?");
  }

  @Test
  void renderWhereClause_withEmptyRestrictions_shouldReturnNull() {
    List<Restriction> restrictions = Collections.emptyList();
    StringBuilder result = Restriction.renderWhereClause(restrictions);
    assertThat(result).isNull();
  }

  @Test
  void renderWhereClause_withNullRestrictions_shouldReturnNull() {
    StringBuilder result = Restriction.renderWhereClause(null);
    assertThat(result).isNull();
  }

  @Test
  void renderWhereClause_withSingleRestriction_shouldRenderWithoutLogicalOperator() {
    List<Restriction> restrictions = List.of(Restriction.equal("col", "val"));
    StringBuilder sqlBuffer = new StringBuilder();

    Restriction.renderWhereClause(restrictions, sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo("`col` = val");
  }

  @Test
  void render_withEmptyRestrictions_shouldNotAppendWhereClause() {
    List<Restriction> restrictions = Collections.emptyList();
    StringBuilder sqlBuffer = new StringBuilder();

    Restriction.render(restrictions, sqlBuffer);

    assertThat(sqlBuffer).isEmpty();
  }

  @Test
  void render_withNullRestrictions_shouldNotAppendWhereClause() {
    StringBuilder sqlBuffer = new StringBuilder();

    Restriction.render(null, sqlBuffer);

    assertThat(sqlBuffer).isEmpty();
  }

  @Test
  void and_shouldCombineRestrictionsWithANDOperator() {
    Restriction r1 = Restriction.equal("col1", "val1");
    Restriction r2 = Restriction.equal("col2", "val2");
    StringBuilder sqlBuffer = new StringBuilder();

    Restriction combined = Restriction.and(r1, r2);
    combined.render(sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo("(`col1` = val1 AND `col2` = val2)");
  }

  @Test
  void or_shouldCombineRestrictionsWithOROperator() {
    Restriction r1 = Restriction.equal("col1", "val1");
    Restriction r2 = Restriction.equal("col2", "val2");
    StringBuilder sqlBuffer = new StringBuilder();

    Restriction combined = Restriction.or(r1, r2);
    combined.render(sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo("(`col1` = val1 OR `col2` = val2)");
  }

  @Test
  void renderWhereClause_withMultipleRestrictions_shouldRespectLogicalOperators() {
    List<Restriction> restrictions = Arrays.asList(
            Restriction.equal("col1", "val1"),
            Restriction.or(
                    Restriction.equal("col2", "val2"),
                    Restriction.equal("col3", "val3")
            )
    );
    StringBuilder sqlBuffer = new StringBuilder();

    Restriction.renderWhereClause(restrictions, sqlBuffer);

    assertThat(sqlBuffer.toString())
            .isEqualTo("`col1` = val1 AND (`col2` = val2 OR `col3` = val3)");
  }

  @Test
  void lessEqual_withColumnOnly_shouldRenderCorrectly() {
    StringBuilder sqlBuffer = new StringBuilder();
    Restriction restriction = Restriction.lessEqual("column");

    restriction.render(sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo("`column` <= ?");
  }

  @Test
  void forOperator_shouldRenderCustomOperator() {
    StringBuilder sqlBuffer = new StringBuilder();
    Restriction restriction = Restriction.forOperator("name", " LIKE ", "'%test%'");

    restriction.render(sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo("`name` LIKE '%test%'");
  }

  @Test
  void nestedAnd_shouldCombineMultipleRestrictionsCorrectly() {
    StringBuilder sqlBuffer = new StringBuilder();
    Restriction r1 = Restriction.equal("a", "1");
    Restriction r2 = Restriction.equal("b", "2");
    Restriction r3 = Restriction.equal("c", "3");

    Restriction combined = Restriction.and(infra.persistence.sql.Restriction.and(r1, r2), r3);
    combined.render(sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo("((`a` = 1 AND `b` = 2) AND `c` = 3)");
  }

  @Test
  void nestedOr_shouldCombineMultipleRestrictionsCorrectly() {
    StringBuilder sqlBuffer = new StringBuilder();
    Restriction r1 = Restriction.equal("a", "1");
    Restriction r2 = Restriction.equal("b", "2");
    Restriction r3 = Restriction.equal("c", "3");

    Restriction combined = Restriction.or(infra.persistence.sql.Restriction.or(r1, r2), r3);
    combined.render(sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo("((`a` = 1 OR `b` = 2) OR `c` = 3)");
  }

  @Test
  void mixedAndOr_shouldRenderWithCorrectPrecedence() {
    StringBuilder sqlBuffer = new StringBuilder();
    Restriction r1 = Restriction.equal("a", "1");
    Restriction r2 = Restriction.equal("b", "2");
    Restriction r3 = Restriction.equal("c", "3");

    Restriction combined = Restriction.or(infra.persistence.sql.Restriction.and(r1, r2), r3);
    combined.render(sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo("((`a` = 1 AND `b` = 2) OR `c` = 3)");
  }

  @Test
  void render_withMultipleANDRestrictions_shouldRenderCorrectly() {
    StringBuilder sqlBuffer = new StringBuilder();
    List<Restriction> restrictions = Arrays.asList(
            Restriction.equal("col1", "val1"),
            Restriction.equal("col2", "val2"),
            Restriction.equal("col3", "val3")
    );

    Restriction.render(restrictions, sqlBuffer);

    assertThat(sqlBuffer.toString())
            .isEqualTo(" WHERE `col1` = val1 AND `col2` = val2 AND `col3` = val3");
  }

  @Test
  void renderWhereClause_withComplexNestedLogic_shouldRenderCorrectly() {
    StringBuilder sqlBuffer = new StringBuilder();
    Restriction r1 = Restriction.equal("a", "1");
    Restriction r2 = Restriction.equal("b", "2");
    Restriction r3 = Restriction.equal("c", "3");
    Restriction r4 = Restriction.equal("d", "4");

    List<Restriction> restrictions = Arrays.asList(
            Restriction.and(r1, r2),
            Restriction.or(r3, r4)
    );

    Restriction.renderWhereClause(restrictions, sqlBuffer);

    assertThat(sqlBuffer.toString())
            .isEqualTo("(`a` = 1 AND `b` = 2) AND (`c` = 3 OR `d` = 4)");
  }

  @Test
  void andMultiple_shouldCombineAllRestrictionsWithAND() {
    Restriction r1 = Restriction.equal("a", "1");
    Restriction r2 = Restriction.equal("b", "2");
    Restriction r3 = Restriction.equal("c", "3");
    Restriction r4 = Restriction.equal("d", "4");

    Restriction combined = Restriction.and(
            Restriction.and(
                    Restriction.and(r1, r2),
                    r3),
            r4
    );

    StringBuilder sqlBuffer = new StringBuilder();
    combined.render(sqlBuffer);

    assertThat(sqlBuffer.toString())
            .isEqualTo("(((`a` = 1 AND `b` = 2) AND `c` = 3) AND `d` = 4)");
  }

  @Test
  void orMultiple_shouldCombineAllRestrictionsWithOR() {
    Restriction r1 = Restriction.equal("a", "1");
    Restriction r2 = Restriction.equal("b", "2");
    Restriction r3 = Restriction.equal("c", "3");
    Restriction r4 = Restriction.equal("d", "4");

    Restriction combined = Restriction.or(
            Restriction.or(
                    Restriction.or(r1, r2),
                    r3),
            r4
    );

    StringBuilder sqlBuffer = new StringBuilder();
    combined.render(sqlBuffer);

    assertThat(sqlBuffer.toString())
            .isEqualTo("(((`a` = 1 OR `b` = 2) OR `c` = 3) OR `d` = 4)");
  }

  @Test
  void complexLogicalCombination_shouldRenderCorrectly() {
    Restriction r1 = Restriction.equal("a", "1");
    Restriction r2 = Restriction.equal("b", "2");
    Restriction r3 = Restriction.equal("c", "3");
    Restriction r4 = Restriction.equal("d", "4");

    Restriction combined = Restriction.and(
            Restriction.or(r1, r2),
            Restriction.and(r3, r4)
    );

    StringBuilder sqlBuffer = new StringBuilder();
    combined.render(sqlBuffer);

    assertThat(sqlBuffer.toString())
            .isEqualTo("((`a` = 1 OR `b` = 2) AND (`c` = 3 AND `d` = 4))");
  }

  @Test
  void singleRestrictionOr_shouldRenderCorrectly() {
    Restriction restriction = Restriction.or(infra.persistence.sql.Restriction.equal("col", "val"));
    StringBuilder sqlBuffer = new StringBuilder();

    restriction.render(sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo("`col` = val");
    assertThat(restriction.logicalAnd()).isFalse();
  }

  @Test
  void renderWhereClause_withMixedLogicalOperators_shouldRenderCorrectly() {
    List<Restriction> restrictions = Arrays.asList(
            Restriction.equal("a", "1"),
            Restriction.or(infra.persistence.sql.Restriction.equal("b", "2")),
            Restriction.and(
                    Restriction.equal("c", "3"),
                    Restriction.equal("d", "4")
            )
    );

    StringBuilder sqlBuffer = new StringBuilder();
    Restriction.renderWhereClause(restrictions, sqlBuffer);

    assertThat(sqlBuffer.toString())
            .isEqualTo("`a` = 1 OR `b` = 2 AND (`c` = 3 AND `d` = 4)");
  }

  @Test
  void multipleOr_withSingleRestriction_shouldRenderCorrectly() {
    Restriction r1 = Restriction.equal("a", "1");
    Restriction combined = Restriction.or(infra.persistence.sql.Restriction.or(r1));
    StringBuilder sqlBuffer = new StringBuilder();

    combined.render(sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo("`a` = 1");
  }

  @Test
  void mixedLogicalOperators_withParentheses_shouldRenderCorrectly() {
    Restriction r1 = Restriction.equal("a", "1");
    Restriction r2 = Restriction.equal("b", "2");
    Restriction r3 = Restriction.equal("c", "3");

    Restriction combined = Restriction.and(
            r1,
            Restriction.or(r2, r3)
    );

    StringBuilder sqlBuffer = new StringBuilder();
    combined.render(sqlBuffer);

    assertThat(sqlBuffer.toString())
            .isEqualTo("(`a` = 1 AND (`b` = 2 OR `c` = 3))");
  }

  @Test
  void multipleOperators_withDifferentPrecedence_shouldRenderCorrectly() {
    Restriction r1 = Restriction.graterThan("a", "1");
    Restriction r2 = Restriction.lessEqual("b", "2");
    Restriction r3 = Restriction.notEqual("c", "3");

    Restriction combined = Restriction.or(
            Restriction.and(r1, r2),
            Restriction.or(r2, r3)
    );

    StringBuilder sqlBuffer = new StringBuilder();
    combined.render(sqlBuffer);

    assertThat(sqlBuffer.toString())
            .isEqualTo("((`a` > 1 AND `b` <= 2) OR (`b` <= 2 OR `c` <> 3))");
  }

  @Test
  void nullChecks_withComplexLogic_shouldRenderCorrectly() {
    Restriction r1 = Restriction.isNull("a");
    Restriction r2 = Restriction.isNotNull("b");
    Restriction r3 = Restriction.equal("c", "3");

    Restriction combined = Restriction.and(
            Restriction.or(r1, r2),
            r3
    );

    StringBuilder sqlBuffer = new StringBuilder();
    combined.render(sqlBuffer);

    assertThat(sqlBuffer.toString())
            .isEqualTo("((`a` is null OR `b` is not null) AND `c` = 3)");
  }

  @Test
  void complexNesting_withMultipleLevels_shouldRenderCorrectly() {
    Restriction r1 = Restriction.equal("a", "1");
    Restriction r2 = Restriction.equal("b", "2");
    Restriction r3 = Restriction.equal("c", "3");
    Restriction r4 = Restriction.equal("d", "4");

    Restriction combined = Restriction.or(
            Restriction.and(
                    Restriction.or(r1, r2),
                    Restriction.and(r3, r4)
            ),
            Restriction.equal("e", "5")
    );

    StringBuilder sqlBuffer = new StringBuilder();
    combined.render(sqlBuffer);

    assertThat(sqlBuffer.toString())
            .isEqualTo("(((`a` = 1 OR `b` = 2) AND (`c` = 3 AND `d` = 4)) OR `e` = 5)");
  }

  @Test
  void renderWhereClause_withSingleOrRestriction_shouldRenderCorrectlyWithoutParentheses() {
    List<Restriction> restrictions = Arrays.asList(
            Restriction.or(infra.persistence.sql.Restriction.equal("col", "val"))
    );
    StringBuilder sqlBuffer = new StringBuilder();

    Restriction.renderWhereClause(restrictions, sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo("`col` = val");
  }

  @Test
  void nestedAndOr_withThreeLevels_shouldRenderCorrectly() {
    Restriction r1 = Restriction.equal("a", "1");
    Restriction r2 = Restriction.equal("b", "2");
    Restriction r3 = Restriction.equal("c", "3");
    Restriction r4 = Restriction.equal("d", "4");

    Restriction combined = Restriction.or(
            Restriction.and(
                    r1,
                    Restriction.or(r2, r3)
            ),
            r4
    );

    StringBuilder sqlBuffer = new StringBuilder();
    combined.render(sqlBuffer);

    assertThat(sqlBuffer.toString())
            .isEqualTo("((`a` = 1 AND (`b` = 2 OR `c` = 3)) OR `d` = 4)");
  }

  @Test
  void multiLevelNesting_withMixedOperators_shouldRenderCorrectly() {
    Restriction r1 = Restriction.equal("col1", "1");
    Restriction r2 = Restriction.graterEqual("col2", "2");
    Restriction r3 = Restriction.lessEqual("col3", "3");
    Restriction r4 = Restriction.notEqual("col4", "4");
    Restriction r5 = Restriction.isNull("col5");

    Restriction combined = Restriction.or(
            Restriction.and(
                    Restriction.or(r1, r2),
                    Restriction.and(r3, r4)
            ),
            r5
    );

    StringBuilder sqlBuffer = new StringBuilder();
    combined.render(sqlBuffer);

    assertThat(sqlBuffer.toString())
            .isEqualTo("(((`col1` = 1 OR `col2` >= 2) AND (`col3` <= 3 AND `col4` <> 4)) OR `col5` is null)");
  }

  @Test
  void renderWhereClause_withAllComparisonOperators_shouldRenderCorrectly() {
    List<Restriction> restrictions = Arrays.asList(
            Restriction.equal("a", "1"),
            Restriction.notEqual("b", "2"),
            Restriction.graterThan("c", "3"),
            Restriction.graterEqual("d", "4"),
            Restriction.lessThan("e", "5"),
            Restriction.lessEqual("f", "6")
    );

    StringBuilder sqlBuffer = new StringBuilder();
    Restriction.renderWhereClause(restrictions, sqlBuffer);

    assertThat(sqlBuffer.toString())
            .isEqualTo("`a` = 1 AND `b` <> 2 AND `c` > 3 AND `d` >= 4 AND `e` < 5 AND `f` <= 6");
  }

  @Test
  void nestedRestrictions_withParameters_shouldRenderCorrectly() {
    Restriction r1 = Restriction.equal("col1", "?");
    Restriction r2 = Restriction.graterThan("col2");
    Restriction r3 = Restriction.lessThan("col3");

    Restriction combined = Restriction.and(
            r1,
            Restriction.or(r2, r3)
    );

    StringBuilder sqlBuffer = new StringBuilder();
    combined.render(sqlBuffer);

    assertThat(sqlBuffer.toString())
            .isEqualTo("(`col1` = ? AND (`col2` > ? OR `col3` < ?))");
  }

  @Test
  void renderWhereClause_withEmptyStringValues_shouldRenderCorrectly() {
    List<Restriction> restrictions = Arrays.asList(
            Restriction.equal("col1", ""),
            Restriction.equal("col2", "")
    );
    StringBuilder sqlBuffer = new StringBuilder();

    Restriction.renderWhereClause(restrictions, sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo("`col1` =  AND `col2` = ");
  }

  @Test
  void renderWhereClause_withSpecialCharacters_shouldEscapeCorrectly() {
    List<Restriction> restrictions = List.of(
            Restriction.equal("col@1", "val#1"),
            Restriction.equal("col$2", "val%2")
    );
    StringBuilder sqlBuffer = new StringBuilder();

    Restriction.renderWhereClause(restrictions, sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo("`col@1` = val#1 AND `col$2` = val%2");
  }

  @Test
  void renderWhereClause_withZeroLengthStringBuilder_shouldPreserveCapacity() {
    StringBuilder sqlBuffer = new StringBuilder(0);
    List<Restriction> restrictions = List.of(Restriction.equal("col", "val"));

    Restriction.renderWhereClause(restrictions, sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo("`col` = val");
    assertThat(sqlBuffer.capacity()).isGreaterThan(0);
  }

  @Test
  void renderWhereClause_withLargeNumberOfRestrictions_shouldHandleCorrectly() {
    List<Restriction> restrictions = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      restrictions.add(Restriction.equal("col" + i, String.valueOf(i)));
    }

    StringBuilder sqlBuffer = new StringBuilder();
    Restriction.renderWhereClause(restrictions, sqlBuffer);

    String result = sqlBuffer.toString();
    assertThat(result).startsWith("`col0` = 0");
    assertThat(result).endsWith("`col99` = 99");
    assertThat(result.split("AND")).hasSize(100);
  }

  @Test
  void renderWhereClause_withDifferentSqlInjectionAttempts_shouldEscapeCorrectly() {
    List<Restriction> restrictions = Arrays.asList(
            Restriction.equal("col1", "1' OR '1'='1"),
            Restriction.equal("col2", "'); DROP TABLE users;--"),
            Restriction.equal("col3", "\u0000\u0001\u0002")
    );

    StringBuilder sqlBuffer = Restriction.renderWhereClause(restrictions);

    assertThat(sqlBuffer.toString())
            .isEqualTo("`col1` = 1' OR '1'='1 AND `col2` = '); DROP TABLE users;-- AND `col3` = \u0000\u0001\u0002");
  }

  @Test
  void renderWhereClause_withDuplicateRestrictions_shouldRenderAllDuplicates() {
    Restriction r = Restriction.equal("col", "val");
    List<Restriction> restrictions = Arrays.asList(r, r, r);

    StringBuilder sqlBuffer = Restriction.renderWhereClause(restrictions);

    assertThat(sqlBuffer.toString())
            .isEqualTo("`col` = val AND `col` = val AND `col` = val");
  }

  @Test
  void between() {
    StringBuilder sqlBuffer = Restriction.renderWhereClause(List.of(Restriction.between("age")));
    assertThat(sqlBuffer.toString())
            .isEqualTo("`age` BETWEEN ? AND ?");
  }

  @Test
  void notBetween() {
    StringBuilder sqlBuffer = Restriction.renderWhereClause(List.of(Restriction.notBetween("age")));
    assertThat(sqlBuffer.toString())
            .isEqualTo("`age` NOT BETWEEN ? AND ?");
  }

}