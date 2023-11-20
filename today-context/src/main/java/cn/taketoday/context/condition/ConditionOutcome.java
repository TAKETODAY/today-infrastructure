/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.context.condition;

import java.util.Objects;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Outcome for a condition match, including log message.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ConditionMessage
 * @since 4.0
 */
public class ConditionOutcome {

  private final boolean match;

  private final ConditionMessage message;

  /**
   * Create a new {@link ConditionOutcome} instance. For more consistent messages
   * consider using {@link #ConditionOutcome(boolean, ConditionMessage)}.
   *
   * @param match if the condition is a match
   * @param message the condition message
   */
  public ConditionOutcome(boolean match, String message) {
    this(match, ConditionMessage.of(message));
  }

  /**
   * Create a new {@link ConditionOutcome} instance.
   *
   * @param match if the condition is a match
   * @param message the condition message
   */
  public ConditionOutcome(boolean match, ConditionMessage message) {
    Assert.notNull(message, "ConditionMessage is required");
    this.match = match;
    this.message = message;
  }

  /**
   * Return {@code true} if the outcome was a match.
   *
   * @return {@code true} if the outcome matches
   */
  public boolean isMatch() {
    return this.match;
  }

  /**
   * Return an outcome message or {@code null}.
   *
   * @return the message or {@code null}
   */
  @Nullable
  public String getMessage() {
    return this.message.isEmpty() ? null : this.message.toString();
  }

  /**
   * Return an outcome message or {@code null}.
   *
   * @return the message or {@code null}
   */
  public ConditionMessage getConditionMessage() {
    return this.message;
  }

  /**
   * Return the inverse of the specified condition outcome.
   *
   * @return the inverse of the condition outcome
   */
  public ConditionOutcome inverse() {
    return new ConditionOutcome(!match, message);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object)
      return true;
    if (!(object instanceof ConditionOutcome outcome))
      return false;
    return match == outcome.match
            && Objects.equals(message, outcome.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(match, message);
  }

  @Override
  public String toString() {
    return this.message.toString();
  }

  // Static Factory Methods

  /**
   * Create a new {@link ConditionOutcome} instance for a 'match'.
   *
   * @return the {@link ConditionOutcome}
   */
  public static ConditionOutcome match() {
    return match(ConditionMessage.empty());
  }

  /**
   * Create a new {@link ConditionOutcome} instance for 'match'. For more consistent
   * messages consider using {@link #match(ConditionMessage)}.
   *
   * @param message the message
   * @return the {@link ConditionOutcome}
   */
  public static ConditionOutcome match(String message) {
    return new ConditionOutcome(true, message);
  }

  /**
   * Create a new {@link ConditionOutcome} instance for 'match'.
   *
   * @param message the message
   * @return the {@link ConditionOutcome}
   */
  public static ConditionOutcome match(ConditionMessage message) {
    return new ConditionOutcome(true, message);
  }

  /**
   * Create a new {@link ConditionOutcome} instance for 'no match'. For more consistent
   * messages consider using {@link #noMatch(ConditionMessage)}.
   *
   * @param message the message
   * @return the {@link ConditionOutcome}
   */
  public static ConditionOutcome noMatch(String message) {
    return new ConditionOutcome(false, message);
  }

  /**
   * Create a new {@link ConditionOutcome} instance for 'no match'.
   *
   * @param message the message
   * @return the {@link ConditionOutcome}
   */
  public static ConditionOutcome noMatch(ConditionMessage message) {
    return new ConditionOutcome(false, message);
  }

}
