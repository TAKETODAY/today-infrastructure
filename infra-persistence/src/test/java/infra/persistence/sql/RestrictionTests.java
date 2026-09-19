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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import infra.persistence.platform.Platform;

import static infra.persistence.sql.Restrictions.and;
import static infra.persistence.sql.Restrictions.append;
import static infra.persistence.sql.Restrictions.appendWhereClause;
import static infra.persistence.sql.Restrictions.equal;
import static infra.persistence.sql.Restrictions.forOperator;
import static infra.persistence.sql.Restrictions.greaterEqual;
import static infra.persistence.sql.Restrictions.greaterThan;
import static infra.persistence.sql.Restrictions.isNotNull;
import static infra.persistence.sql.Restrictions.isNull;
import static infra.persistence.sql.Restrictions.lessEqual;
import static infra.persistence.sql.Restrictions.lessThan;
import static infra.persistence.sql.Restrictions.notEqual;
import static infra.persistence.sql.Restrictions.or;
import static infra.persistence.sql.Restrictions.plain;
import static infra.persistence.sql.Restrictions.renderWhereClause;
import static infra.persistence.sql.Restrictions.xor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/5/9 14:55
 */
class RestrictionTests {

  private final Platform platform = Platform.mysql();

  @Test
  void shouldRenderPlainRestriction() {
    StringBuilder sqlBuffer = new StringBuilder();
    Restriction restriction = plain("SELECT * FROM table");

    restriction.render(platform, sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo("SELECT * FROM table");
  }

  @Test
  void shouldRenderEqualRestriction() {
    StringBuilder sqlBuffer = new StringBuilder();
    Restriction restriction = equal("column", "value");

    restriction.render(platform, sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo("column = value");
  }

  @Test
  void shouldRenderQuotedIdentifierForPlatform() {
    Restriction restriction = equal("`column`", "value");

    StringBuilder genericSql = new StringBuilder();
    restriction.render(Platform.generic(), genericSql);
    assertThat(genericSql.toString()).isEqualTo("\"column\" = value");

    StringBuilder mysqlSql = new StringBuilder();
    restriction.render(Platform.mysql(), mysqlSql);
    assertThat(mysqlSql.toString()).isEqualTo("`column` = value");
  }

  @Test
  void shouldRenderNotEqualRestriction() {
    StringBuilder sqlBuffer = new StringBuilder();
    Restriction restriction = notEqual("column", "value");

    restriction.render(platform, sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo("column <> value");
  }

  @Test
  void shouldRenderGreaterThanRestriction() {
    StringBuilder sqlBuffer = new StringBuilder();
    Restriction restriction = greaterThan("column", "value");

    restriction.render(platform, sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo("column > value");
  }

  @Test
  void shouldRenderLessThanRestriction() {
    StringBuilder sqlBuffer = new StringBuilder();
    Restriction restriction = lessThan("column", "value");

    restriction.render(platform, sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo("column < value");
  }

  @Test
  void shouldRenderIsNullRestriction() {
    StringBuilder sqlBuffer = new StringBuilder();
    Restriction restriction = isNull("column");

    restriction.render(platform, sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo("column is null");
  }

  @Test
  void shouldRenderIsNotNullRestriction() {
    StringBuilder sqlBuffer = new StringBuilder();
    Restriction restriction = isNotNull("column");

    restriction.render(platform, sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo("column is not null");
  }

  @Test
  void shouldRenderMultipleRestrictionsWithAnd() {
    StringBuilder sqlBuffer = new StringBuilder();
    Restriction restriction1 = equal("column1", "value1");
    Restriction restriction2 = lessThan("column2", "value2");

    append(platform, List.of(restriction1, restriction2), sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo(" WHERE column1 = value1 AND column2 < value2");
  }

  @Test
  void graterEqual_withColumnOnly_shouldRenderCorrectly() {
    StringBuilder sqlBuffer = new StringBuilder();
    Restriction restriction = greaterEqual("column");

    restriction.render(platform, sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo("column >= ?");
  }

  @Test
  void renderWhereClause_withEmptyRestrictions_shouldReturnNull() {
    List<Restriction> restrictions = Collections.emptyList();
    StringBuilder result = renderWhereClause(platform, restrictions);
    assertThat(result).isNull();
  }

  @Test
  void renderWhereClause_withNullRestrictions_shouldReturnNull() {
    StringBuilder result = renderWhereClause(platform, null);
    assertThat(result).isNull();
  }

  @Test
  void renderWhereClause_withSingleRestriction_shouldRenderWithoutLogicalOperator() {
    List<Restriction> restrictions = List.of(equal("col", "val"));
    StringBuilder sqlBuffer = new StringBuilder();

    appendWhereClause(platform, restrictions, sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo("col = val");
  }

  @Test
  void render_withEmptyRestrictions_shouldNotAppendWhereClause() {
    List<Restriction> restrictions = Collections.emptyList();
    StringBuilder sqlBuffer = new StringBuilder();

    append(platform, restrictions, sqlBuffer);

    assertThat(sqlBuffer).isEmpty();
  }

  @Test
  void render_withNullRestrictions_shouldNotAppendWhereClause() {
    StringBuilder sqlBuffer = new StringBuilder();

    append(platform, null, sqlBuffer);

    assertThat(sqlBuffer).isEmpty();
  }

  @Test
  void and_shouldCombineRestrictionsWithANDOperator() {
    Restriction r1 = equal("col1", "val1");
    Restriction r2 = equal("col2", "val2");
    StringBuilder sqlBuffer = new StringBuilder();

    Restriction combined = and(r1, r2);
    combined.render(platform, sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo("(col1 = val1 AND col2 = val2)");
  }

  @Test
  void or_shouldCombineRestrictionsWithOROperator() {
    Restriction r1 = equal("col1", "val1");
    Restriction r2 = equal("col2", "val2");
    StringBuilder sqlBuffer = new StringBuilder();

    Restriction combined = or(r1, r2);
    combined.render(platform, sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo("(col1 = val1 OR col2 = val2)");
  }

  @Test
  void renderWhereClause_withMultipleRestrictions_shouldRespectLogicalOperators() {
    List<Restriction> restrictions = Arrays.asList(
            equal("col1", "val1"),
            or(
                    equal("col2", "val2"),
                    equal("col3", "val3")
            )
    );
    StringBuilder sqlBuffer = new StringBuilder();

    appendWhereClause(platform, restrictions, sqlBuffer);

    assertThat(sqlBuffer.toString())
            .isEqualTo("col1 = val1 AND (col2 = val2 OR col3 = val3)");
  }

  @Test
  void lessEqual_withColumnOnly_shouldRenderCorrectly() {
    StringBuilder sqlBuffer = new StringBuilder();
    Restriction restriction = lessEqual("column");

    restriction.render(platform, sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo("column <= ?");
  }

  @Test
  void forOperator_shouldRenderCustomOperator() {
    StringBuilder sqlBuffer = new StringBuilder();
    Restriction restriction = forOperator("name", " LIKE ", "'%test%'");

    restriction.render(platform, sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo("name LIKE '%test%'");
  }

  @Test
  void nestedAnd_shouldCombineMultipleRestrictionsCorrectly() {
    StringBuilder sqlBuffer = new StringBuilder();
    Restriction r1 = equal("a", "1");
    Restriction r2 = equal("b", "2");
    Restriction r3 = equal("c", "3");

    Restriction combined = and(and(r1, r2), r3);
    combined.render(platform, sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo("((a = 1 AND b = 2) AND c = 3)");
  }

  @Test
  void nestedOr_shouldCombineMultipleRestrictionsCorrectly() {
    StringBuilder sqlBuffer = new StringBuilder();
    Restriction r1 = equal("a", "1");
    Restriction r2 = equal("b", "2");
    Restriction r3 = equal("c", "3");

    Restriction combined = or(or(r1, r2), r3);
    combined.render(platform, sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo("((a = 1 OR b = 2) OR c = 3)");
  }

  @Test
  void mixedAndOr_shouldRenderWithCorrectPrecedence() {
    StringBuilder sqlBuffer = new StringBuilder();
    Restriction r1 = equal("a", "1");
    Restriction r2 = equal("b", "2");
    Restriction r3 = equal("c", "3");

    Restriction combined = or(and(r1, r2), r3);
    combined.render(platform, sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo("((a = 1 AND b = 2) OR c = 3)");
  }

  @Test
  void render_withMultipleANDRestrictions_shouldRenderCorrectly() {
    StringBuilder sqlBuffer = new StringBuilder();
    List<Restriction> restrictions = Arrays.asList(
            equal("col1", "val1"),
            equal("col2", "val2"),
            equal("col3", "val3")
    );

    append(platform, restrictions, sqlBuffer);

    assertThat(sqlBuffer.toString())
            .isEqualTo(" WHERE col1 = val1 AND col2 = val2 AND col3 = val3");
  }

  @Test
  void renderWhereClause_withComplexNestedLogic_shouldRenderCorrectly() {
    StringBuilder sqlBuffer = new StringBuilder();
    Restriction r1 = equal("a", "1");
    Restriction r2 = equal("b", "2");
    Restriction r3 = equal("c", "3");
    Restriction r4 = equal("d", "4");

    List<Restriction> restrictions = Arrays.asList(
            and(r1, r2),
            or(r3, r4)
    );

    appendWhereClause(platform, restrictions, sqlBuffer);

    assertThat(sqlBuffer.toString())
            .isEqualTo("(a = 1 AND b = 2) AND (c = 3 OR d = 4)");
  }

  @Test
  void andMultiple_shouldCombineAllRestrictionsWithAND() {
    Restriction r1 = equal("a", "1");
    Restriction r2 = equal("b", "2");
    Restriction r3 = equal("c", "3");
    Restriction r4 = equal("d", "4");

    Restriction combined = and(and(and(r1, r2), r3), r4
    );

    StringBuilder sqlBuffer = new StringBuilder();
    combined.render(platform, sqlBuffer);

    assertThat(sqlBuffer.toString())
            .isEqualTo("(((a = 1 AND b = 2) AND c = 3) AND d = 4)");
  }

  @Test
  void orMultiple_shouldCombineAllRestrictionsWithOR() {
    Restriction r1 = equal("a", "1");
    Restriction r2 = equal("b", "2");
    Restriction r3 = equal("c", "3");
    Restriction r4 = equal("d", "4");

    Restriction combined = or(
            or(
                    or(r1, r2),
                    r3),
            r4
    );

    StringBuilder sqlBuffer = new StringBuilder();
    combined.render(platform, sqlBuffer);

    assertThat(sqlBuffer.toString())
            .isEqualTo("(((a = 1 OR b = 2) OR c = 3) OR d = 4)");
  }

  @Test
  void complexLogicalCombination_shouldRenderCorrectly() {
    Restriction r1 = equal("a", "1");
    Restriction r2 = equal("b", "2");
    Restriction r3 = equal("c", "3");
    Restriction r4 = equal("d", "4");

    Restriction combined = and(
            or(r1, r2),
            and(r3, r4)
    );

    StringBuilder sqlBuffer = new StringBuilder();
    combined.render(platform, sqlBuffer);

    assertThat(sqlBuffer.toString())
            .isEqualTo("((a = 1 OR b = 2) AND (c = 3 AND d = 4))");
  }

  @Test
  void appendWhereClause_withOrConnector_shouldJoinEveryRestriction() {
    List<Restriction> restrictions = List.of(
            equal("a", "1"),
            equal("b", "2"));
    StringBuilder sqlBuffer = new StringBuilder();

    appendWhereClause(platform, restrictions, LogicalOperator.OR, sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo("a = 1 OR b = 2");
  }

  @Test
  void xor_shouldRenderOnPlatformWithNativeSupport() {
    Restriction combined = xor(
            equal("a", "1"),
            equal("b", "2"));
    StringBuilder sqlBuffer = new StringBuilder();

    combined.render(Platform.mysql(), sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo("(a = 1 XOR b = 2)");
  }

  @Test
  void xor_shouldFailFastOnPlatformWithoutSupport() {
    Restriction combined = xor(
            equal("a", "1"),
            equal("b", "2"));
    StringBuilder sqlBuffer = new StringBuilder();

    assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> combined.render(Platform.generic(), sqlBuffer));
  }

  @Test
  void appendWhereClause_withXorConnector_shouldJoinEveryRestriction() {
    List<Restriction> restrictions = List.of(
            equal("a", "1"),
            equal("b", "2"));
    StringBuilder sqlBuffer = new StringBuilder();

    appendWhereClause(Platform.mysql(), restrictions, LogicalOperator.XOR, sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo("a = 1 XOR b = 2");
  }

  @Test
  void mixedLogicalOperators_withParentheses_shouldRenderCorrectly() {
    Restriction r1 = equal("a", "1");
    Restriction r2 = equal("b", "2");
    Restriction r3 = equal("c", "3");

    Restriction combined = and(
            r1,
            or(r2, r3)
    );

    StringBuilder sqlBuffer = new StringBuilder();
    combined.render(platform, sqlBuffer);

    assertThat(sqlBuffer.toString())
            .isEqualTo("(a = 1 AND (b = 2 OR c = 3))");
  }

  @Test
  void multipleOperators_withDifferentPrecedence_shouldRenderCorrectly() {
    Restriction r1 = greaterThan("a", "1");
    Restriction r2 = lessEqual("b", "2");
    Restriction r3 = notEqual("c", "3");

    Restriction combined = or(
            and(r1, r2),
            or(r2, r3)
    );

    StringBuilder sqlBuffer = new StringBuilder();
    combined.render(platform, sqlBuffer);

    assertThat(sqlBuffer.toString())
            .isEqualTo("((a > 1 AND b <= 2) OR (b <= 2 OR c <> 3))");
  }

  @Test
  void nullChecks_withComplexLogic_shouldRenderCorrectly() {
    Restriction r1 = isNull("a");
    Restriction r2 = isNotNull("b");
    Restriction r3 = equal("c", "3");

    Restriction combined = and(
            or(r1, r2),
            r3
    );

    StringBuilder sqlBuffer = new StringBuilder();
    combined.render(platform, sqlBuffer);

    assertThat(sqlBuffer.toString())
            .isEqualTo("((a is null OR b is not null) AND c = 3)");
  }

  @Test
  void complexNesting_withMultipleLevels_shouldRenderCorrectly() {
    Restriction r1 = equal("a", "1");
    Restriction r2 = equal("b", "2");
    Restriction r3 = equal("c", "3");
    Restriction r4 = equal("d", "4");

    Restriction combined = or(
            and(
                    or(r1, r2),
                    and(r3, r4)
            ),
            equal("e", "5")
    );

    StringBuilder sqlBuffer = new StringBuilder();
    combined.render(platform, sqlBuffer);

    assertThat(sqlBuffer.toString())
            .isEqualTo("(((a = 1 OR b = 2) AND (c = 3 AND d = 4)) OR e = 5)");
  }

  @Test
  void nestedAndOr_withThreeLevels_shouldRenderCorrectly() {
    Restriction r1 = equal("a", "1");
    Restriction r2 = equal("b", "2");
    Restriction r3 = equal("c", "3");
    Restriction r4 = equal("d", "4");

    Restriction combined = or(
            and(
                    r1,
                    or(r2, r3)
            ),
            r4
    );

    StringBuilder sqlBuffer = new StringBuilder();
    combined.render(platform, sqlBuffer);

    assertThat(sqlBuffer.toString())
            .isEqualTo("((a = 1 AND (b = 2 OR c = 3)) OR d = 4)");
  }

  @Test
  void multiLevelNesting_withMixedOperators_shouldRenderCorrectly() {
    Restriction r1 = equal("col1", "1");
    Restriction r2 = greaterEqual("col2", "2");
    Restriction r3 = lessEqual("col3", "3");
    Restriction r4 = notEqual("col4", "4");
    Restriction r5 = isNull("col5");

    Restriction combined = or(
            and(
                    or(r1, r2),
                    and(r3, r4)
            ),
            r5
    );

    StringBuilder sqlBuffer = new StringBuilder();
    combined.render(platform, sqlBuffer);

    assertThat(sqlBuffer.toString())
            .isEqualTo("(((col1 = 1 OR col2 >= 2) AND (col3 <= 3 AND col4 <> 4)) OR col5 is null)");
  }

  @Test
  void renderWhereClause_withAllComparisonOperators_shouldRenderCorrectly() {
    List<Restriction> restrictions = Arrays.asList(
            equal("a", "1"),
            notEqual("b", "2"),
            greaterThan("c", "3"),
            greaterEqual("d", "4"),
            lessThan("e", "5"),
            lessEqual("f", "6")
    );

    StringBuilder sqlBuffer = new StringBuilder();
    appendWhereClause(platform, restrictions, sqlBuffer);

    assertThat(sqlBuffer.toString())
            .isEqualTo("a = 1 AND b <> 2 AND c > 3 AND d >= 4 AND e < 5 AND f <= 6");
  }

  @Test
  void nestedRestrictions_withParameters_shouldRenderCorrectly() {
    Restriction r1 = equal("col1", "?");
    Restriction r2 = greaterThan("col2");
    Restriction r3 = lessThan("col3");

    Restriction combined = and(
            r1,
            or(r2, r3)
    );

    StringBuilder sqlBuffer = new StringBuilder();
    combined.render(platform, sqlBuffer);

    assertThat(sqlBuffer.toString())
            .isEqualTo("(col1 = ? AND (col2 > ? OR col3 < ?))");
  }

  @Test
  void renderWhereClause_withEmptyStringValues_shouldRenderCorrectly() {
    List<Restriction> restrictions = Arrays.asList(
            equal("col1", ""),
            equal("col2", "")
    );
    StringBuilder sqlBuffer = new StringBuilder();

    appendWhereClause(platform, restrictions, sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo("col1 =  AND col2 = ");
  }

  @Test
  void renderWhereClause_withSpecialCharacters_shouldEscapeCorrectly() {
    List<Restriction> restrictions = List.of(
            equal("col@1", "val#1"),
            equal("col$2", "val%2")
    );
    StringBuilder sqlBuffer = new StringBuilder();

    appendWhereClause(platform, restrictions, sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo("`col@1` = val#1 AND col$2 = val%2");
  }

  @Test
  void renderWhereClause_withZeroLengthStringBuilder_shouldPreserveCapacity() {
    StringBuilder sqlBuffer = new StringBuilder(0);
    List<Restriction> restrictions = List.of(equal("col", "val"));

    appendWhereClause(platform, restrictions, sqlBuffer);

    assertThat(sqlBuffer.toString()).isEqualTo("col = val");
    assertThat(sqlBuffer.capacity()).isGreaterThan(0);
  }

  @Test
  void renderWhereClause_withLargeNumberOfRestrictions_shouldHandleCorrectly() {
    List<Restriction> restrictions = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      restrictions.add(equal("col" + i, String.valueOf(i)));
    }

    StringBuilder sqlBuffer = new StringBuilder();
    appendWhereClause(platform, restrictions, sqlBuffer);

    String result = sqlBuffer.toString();
    assertThat(result).startsWith("col0 = 0");
    assertThat(result).endsWith("col99 = 99");
    assertThat(result.split("AND")).hasSize(100);
  }

  @Test
  void renderWhereClause_withDifferentSqlInjectionAttempts_shouldEscapeCorrectly() {
    List<Restriction> restrictions = Arrays.asList(
            equal("col1", "1' OR '1'='1"),
            equal("col2", "'); DROP TABLE users;--"),
            equal("col3", "\u0000\u0001\u0002")
    );

    StringBuilder sqlBuffer = renderWhereClause(platform, restrictions);

    assertThat(sqlBuffer.toString())
            .isEqualTo("col1 = 1' OR '1'='1 AND col2 = '); DROP TABLE users;-- AND col3 = \u0000\u0001\u0002");
  }

  @Test
  void renderWhereClause_withDuplicateRestrictions_shouldRenderAllDuplicates() {
    Restriction r = equal("col", "val");
    List<Restriction> restrictions = Arrays.asList(r, r, r);

    StringBuilder sqlBuffer = renderWhereClause(platform, restrictions);

    assertThat(sqlBuffer.toString())
            .isEqualTo("col = val AND col = val AND col = val");
  }

  @Test
  void between() {
    StringBuilder sqlBuffer = renderWhereClause(platform, List.of(Restrictions.between("age")));
    assertThat(sqlBuffer.toString())
            .isEqualTo("age BETWEEN ? AND ?");
  }

  @Test
  void notBetween() {
    StringBuilder sqlBuffer = renderWhereClause(platform, List.of(Restrictions.notBetween("age")));
    assertThat(sqlBuffer.toString())
            .isEqualTo("age NOT BETWEEN ? AND ?");
  }

}
