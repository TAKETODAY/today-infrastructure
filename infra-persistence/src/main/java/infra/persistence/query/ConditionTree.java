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

package infra.persistence.query;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import infra.persistence.platform.Platform;
import infra.persistence.sql.LogicalOperator;
import infra.util.Assert;

/**
 * A {@code WHERE} expression assembled from ordered {@link Occurrence
 * occurrences} — the root of the condition AST.
 *
 * <p>An occurrence pairs a {@link Condition} with the connector that joins it
 * to the preceding one; the first occurrence's connector is ignored. A child
 * condition is a leaf (for example {@link PropertyCondition}) or an internal
 * node (a {@link ConditionGroup}), so trees nest to any depth.
 *
 * <p>Rendering keeps the sequence unambiguous: when every connector is the
 * same, the terms are rendered flat ({@code a AND b AND c}); when connectors
 * mix, the sequence is folded left-to-right into parenthesized groups
 * ({@code ((a AND b) OR c)}). Placeholders are always bound in render order.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Occurrence
 * @see ConditionGroup
 * @see PropertyCondition
 * @since 5.0
 */
public final class ConditionTree implements Condition {

  private final List<Occurrence> occurrences;

  private ConditionTree(List<Occurrence> occurrences) {
    Assert.notEmpty(occurrences, "ConditionTree requires at least one occurrence");
    this.occurrences = List.copyOf(occurrences);
  }

  /**
   * Create a tree from the given occurrences, in order.
   *
   * @param occurrences the ordered occurrences, at least one
   * @return the tree
   * @throws IllegalArgumentException if {@code occurrences} is empty
   */
  public static ConditionTree of(List<Occurrence> occurrences) {
    Assert.notNull(occurrences, "Occurrences are required");
    return new ConditionTree(new ArrayList<>(occurrences));
  }

  /**
   * Create a single-condition tree.
   *
   * @param condition the only occurrence
   * @return the tree
   */
  public static ConditionTree of(Condition condition) {
    Assert.notNull(condition, "Condition is required");
    return new ConditionTree(Collections.singletonList(new Occurrence(LogicalOperator.AND, condition)));
  }

  /**
   * Create a single-condition tree with an explicit connector.
   *
   * @param connector the connector of the single occurrence, never {@code null}
   * @param condition the only occurrence
   * @return the tree
   */
  public static ConditionTree of(LogicalOperator connector, Condition condition) {
    Assert.notNull(connector, "LogicalOperator is required");
    Assert.notNull(condition, "Condition is required");
    return new ConditionTree(Collections.singletonList(new Occurrence(connector, condition)));
  }

  /**
   * Return a tree with the given condition appended, joined with {@code AND}.
   *
   * @param condition the condition to append
   * @return the new tree
   */
  public ConditionTree and(Condition condition) {
    return add(LogicalOperator.AND, condition);
  }

  /**
   * Return a tree with the given condition appended, joined with {@code OR}.
   *
   * @param condition the condition to append
   * @return the new tree
   */
  public ConditionTree or(Condition condition) {
    return add(LogicalOperator.OR, condition);
  }

  /**
   * Return a tree with the given condition appended, joined with {@code XOR}.
   *
   * @param condition the condition to append
   * @return the new tree
   */
  public ConditionTree xor(Condition condition) {
    return add(LogicalOperator.XOR, condition);
  }

  /**
   * Return a tree with the given condition appended, joined with {@code connector}.
   *
   * @param connector the connector joining the new condition to the preceding one
   * @param condition the condition to append
   * @return the new tree
   */
  public ConditionTree add(LogicalOperator connector, Condition condition) {
    Assert.notNull(connector, "LogicalOperator is required");
    Assert.notNull(condition, "Condition is required");
    List<Occurrence> next = new ArrayList<>(occurrences.size() + 1);
    next.addAll(occurrences);
    next.add(new Occurrence(connector, condition));
    return new ConditionTree(next);
  }

  /**
   * Return the ordered occurrences of this tree.
   *
   * @return an immutable list, never empty
   */
  public List<Occurrence> occurrences() {
    return occurrences;
  }

  @Override
  public void render(Platform platform, StringBuilder sqlBuffer) {
    Occurrence first = occurrences.get(0);
    if (occurrences.size() == 1) {
      first.condition.render(platform, sqlBuffer);
      return;
    }
    boolean allAnd = true;
    for (int i = 1; i < occurrences.size(); i++) {
      if (occurrences.get(i).connector != LogicalOperator.AND) {
        allAnd = false;
        break;
      }
    }
    if (allAnd) {
      first.condition.render(platform, sqlBuffer);
      for (int i = 1; i < occurrences.size(); i++) {
        Occurrence occurrence = occurrences.get(i);
        occurrence.connector.render(platform, sqlBuffer);
        occurrence.condition.render(platform, sqlBuffer);
      }
      return;
    }

    Condition expression = first.condition;
    for (int i = 1; i < occurrences.size(); i++) {
      Occurrence occurrence = occurrences.get(i);
      expression = switch (occurrence.connector) {
        case AND -> ConditionGroup.and(expression, occurrence.condition);
        case OR -> ConditionGroup.or(expression, occurrence.condition);
        case XOR -> ConditionGroup.xor(expression, occurrence.condition);
      };
    }
    expression.render(platform, sqlBuffer);
  }

  @Override
  public int setParameter(PreparedStatement ps, int parameterIndex) throws SQLException {
    for (Occurrence occurrence : occurrences) {
      parameterIndex = occurrence.condition.setParameter(ps, parameterIndex);
    }
    return parameterIndex;
  }

  /**
   * An ordered term of a {@link ConditionTree}: a condition plus the connector
   * that joins it to the preceding term.
   *
   * @param connector the connector to the preceding term, ignored for the first term
   * @param condition the term's condition
   */
  public record Occurrence(LogicalOperator connector, Condition condition) {
  }

}