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

package infra.persistence;

import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import infra.persistence.platform.Platform;
import infra.persistence.sql.LogicalOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
class ConditionGroupTests {

  private final Platform platform = Platform.generic();

  @Test
  void or_shouldRenderParenthesized() {
    ConditionGroup group = ConditionGroup.or(
            stub("a", 1),
            stub("b", 2),
            stub("c", 3));

    assertThat(render(group)).isEqualTo("(a OR b OR c)");
  }

  @Test
  void and_shouldRenderParenthesized() {
    ConditionGroup group = ConditionGroup.and(
            stub("a", 1),
            stub("b", 2),
            stub("c", 3));

    assertThat(render(group)).isEqualTo("(a AND b AND c)");
  }

  @Test
  void mixedConnectors_shouldRenderParenthesized() {
    ConditionGroup group = ConditionGroup.of(List.of(
            new ConditionTree.Occurrence(LogicalOperator.AND, stub("a", 1)),
            new ConditionTree.Occurrence(LogicalOperator.OR, stub("b", 2)),
            new ConditionTree.Occurrence(LogicalOperator.AND, stub("c", 3))));

    assertThat(render(group)).isEqualTo("((a OR b) AND c)");
  }

  @Test
  void nestedGroup_shouldRenderGrouped() {
    ConditionGroup inner = ConditionGroup.or(stub("b", 2), stub("c", 3));
    ConditionGroup group = ConditionGroup.and(stub("a", 1), inner, stub("d", 4));

    assertThat(render(group)).isEqualTo("(a AND (b OR c) AND d)");
  }

  @Test
  void deepNesting_shouldRenderGroupedToAnyDepth() {
    ConditionGroup innermost = ConditionGroup.and(stub("e", 5), stub("f", 6));
    ConditionGroup middle = ConditionGroup.or(stub("c", 3), stub("d", 4), innermost);
    ConditionGroup outer = ConditionGroup.and(stub("a", 1), stub("b", 2), middle);

    assertThat(render(outer))
            .isEqualTo("(a AND b AND (c OR d OR (e AND f)))");
  }

  @Test
  void mixedConnectorsThroughNesting_shouldRenderCorrectly() {
    // a AND (b OR (c XOR d))
    ConditionGroup inner = ConditionGroup.xor(stub("c", 3), stub("d", 4));
    ConditionGroup nested = ConditionGroup.or(stub("b", 2), inner);
    ConditionGroup group = ConditionGroup.and(stub("a", 1), nested);

    StringBuilder sql = new StringBuilder();
    group.render(Platform.mysql(), sql);

    assertThat(sql.toString())
            .isEqualTo("(a AND (b OR (c XOR d)))");
  }

  @Test
  void singleMemberNestedGroup_shouldRenderWithoutExtraParentheses() {
    ConditionGroup inner = ConditionGroup.or(stub("b", 2));
    ConditionGroup group = ConditionGroup.and(stub("a", 1), inner, stub("c", 3));

    assertThat(render(group)).isEqualTo("(a AND b AND c)");
  }

  @Test
  void mixedConnectorsBind_shouldBindInRenderOrder() throws SQLException {
    List<Integer> bound = new ArrayList<>();
    // a AND (b OR c) and (d XOR e)
    ConditionGroup inner = ConditionGroup.or(stub("b", 2, bound), stub("c", 3, bound));
    ConditionGroup xor = ConditionGroup.xor(stub("d", 4, bound), stub("e", 5, bound));
    ConditionGroup group = ConditionGroup.and(stub("a", 1, bound), inner, xor);

    group.setParameter(null, 1);

    assertThat(bound).containsExactly(1, 2, 3, 4, 5);
  }

  @Test
  void singleMember_shouldRenderWithoutParentheses() {
    ConditionGroup group = ConditionGroup.or(stub("a", 1));

    assertThat(render(group)).isEqualTo("a");
  }

  @Test
  void setParameter_shouldBindInRenderOrder() throws SQLException {
    List<Integer> bound = new ArrayList<>();
    ConditionGroup inner = ConditionGroup.or(stub("b", 2, bound), stub("c", 3, bound));
    ConditionGroup group = ConditionGroup.and(stub("a", 1, bound), inner, stub("d", 4, bound));

    group.setParameter(null, 1);

    assertThat(bound).containsExactly(1, 2, 3, 4);
  }

  @Test
  void of_shouldRejectEmptyMembers() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> ConditionGroup.of(List.of()));
  }

  @Test
  void and_shouldRejectEmptyMembers() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> ConditionGroup.and());
  }

  private String render(ConditionGroup group) {
    StringBuilder sql = new StringBuilder();
    group.render(platform, sql);
    return sql.toString();
  }

  private static StubCondition stub(String sql, int value) {
    return new StubCondition(sql, value, new ArrayList<>());
  }

  private static StubCondition stub(String sql, int value, List<Integer> bound) {
    return new StubCondition(sql, value, bound);
  }

  private static final class StubCondition implements Condition {

    private final String sql;

    private final int value;

    private final List<Integer> bound;

    StubCondition(String sql, int value, List<Integer> bound) {
      this.sql = sql;
      this.value = value;
      this.bound = bound;
    }

    @Override
    public void render(Platform platform, StringBuilder sqlBuffer) {
      sqlBuffer.append(sql);
    }

    @Override
    public int setParameter(PreparedStatement ps, int parameterIndex) throws SQLException {
      bound.add(value);
      return parameterIndex + 1;
    }
  }

}
