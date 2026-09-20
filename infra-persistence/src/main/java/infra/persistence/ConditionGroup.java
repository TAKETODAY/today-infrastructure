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

import org.jspecify.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import infra.persistence.platform.Platform;
import infra.persistence.sql.LogicalOperator;
import infra.util.Assert;

/**
 * A parenthesized group of ordered {@link Condition conditions}.
 *
 * <p>Inside the group, members are joined exactly like top-level conditions:
 * the second member onward connects to the preceding one with its own
 * {@link ConditionTree.Occurrence#connector() connector}, defaulting to
 * {@code AND}; {@code OR} and {@code XOR} members are mixed freely, which is
 * what makes a group of more than two members expressive.
 *
 * <p>Groups nest: a group is itself a {@link Condition}, so members may be
 * individual conditions or other groups. The group always renders within
 * parentheses, except for a single member which renders without them.
 *
 * <p>Rendering and parameter binding both walk the occurrences in the same
 * order, so the placeholders a group emits are bound in exactly the order they
 * appear.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Condition
 * @see LogicalOperator
 * @see ConditionTree.Occurrence
 * @since 5.0
 */
public final class ConditionGroup implements Condition {

  private final List<ConditionTree.Occurrence> occurrences;

  private ConditionGroup(List<ConditionTree.Occurrence> occurrences) {
    Assert.notEmpty(occurrences, "ConditionGroup requires at least one member");
    this.occurrences = List.copyOf(occurrences);
  }

  /**
   * Create a group from the given ordered occurrences.
   *
   * @param occurrences the ordered occurrences, at least one
   * @return the group
   * @throws IllegalArgumentException if {@code occurrences} is empty
   */
  public static ConditionGroup of(List<ConditionTree.Occurrence> occurrences) {
    Assert.notNull(occurrences, "Occurrences are required");
    return new ConditionGroup(new ArrayList<>(occurrences));
  }

  /**
   * Create a group joining every member with {@code AND}.
   *
   * @param members the members, at least one
   * @return the group
   */
  public static ConditionGroup and(Condition... members) {
    return of(LogicalOperator.AND, members);
  }

  /**
   * Create a group joining every member with {@code OR}.
   *
   * @param members the members, at least one
   * @return the group
   */
  public static ConditionGroup or(Condition... members) {
    return of(LogicalOperator.OR, members);
  }

  /**
   * Create a group joining every member with {@code XOR}.
   *
   * @param members the members, at least one
   * @return the group
   */
  public static ConditionGroup xor(Condition... members) {
    return of(LogicalOperator.XOR, members);
  }

  private static ConditionGroup of(LogicalOperator connector, Condition... members) {
    Assert.notNull(connector, "LogicalOperator is required");
    Assert.notNull(members, "Conditions are required");
    List<ConditionTree.Occurrence> occurrences = new ArrayList<>(members.length);
    for (Condition member : members) {
      occurrences.add(new ConditionTree.Occurrence(connector, member));
    }
    return new ConditionGroup(occurrences);
  }

  /**
   * Return the ordered occurrences of this group.
   *
   * @return an immutable list, never empty
   */
  public List<ConditionTree.Occurrence> occurrences() {
    return occurrences;
  }

  @Override
  public void render(Platform platform, StringBuilder sqlBuffer) {
    if (occurrences.size() == 1) {
      occurrences.get(0).condition().render(platform, sqlBuffer);
      return;
    }
    LogicalOperator shared = occurrences.get(1).connector();
    boolean same = true;
    for (int i = 1; i < occurrences.size(); i++) {
      if (occurrences.get(i).connector() != shared) {
        same = false;
        break;
      }
    }
    if (same) {
      sqlBuffer.append('(');
      occurrences.get(0).condition().render(platform, sqlBuffer);
      for (int i = 1; i < occurrences.size(); i++) {
        ConditionTree.Occurrence occurrence = occurrences.get(i);
        occurrence.connector().render(platform, sqlBuffer);
        occurrence.condition().render(platform, sqlBuffer);
      }
      sqlBuffer.append(')');
      return;
    }
    Condition expression = occurrences.get(0).condition();
    for (int i = 1; i < occurrences.size(); i++) {
      ConditionTree.Occurrence occurrence = occurrences.get(i);
      expression = switch (occurrence.connector()) {
        case AND -> ConditionGroup.and(expression, occurrence.condition());
        case OR -> ConditionGroup.or(expression, occurrence.condition());
        case XOR -> ConditionGroup.xor(expression, occurrence.condition());
      };
    }
    expression.render(platform, sqlBuffer);
  }

  @Override
  public int setParameter(PreparedStatement ps, int parameterIndex) throws SQLException {
    for (ConditionTree.Occurrence occurrence : occurrences) {
      parameterIndex = occurrence.condition().setParameter(ps, parameterIndex);
    }
    return parameterIndex;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    return other instanceof ConditionGroup that && occurrences.equals(that.occurrences);
  }

  @Override
  public int hashCode() {
    return occurrences.hashCode();
  }

  @Override
  public String toString() {
    return "ConditionGroup" + occurrences;
  }

}