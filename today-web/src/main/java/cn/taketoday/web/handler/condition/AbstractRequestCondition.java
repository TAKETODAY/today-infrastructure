/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.web.handler.condition;

import java.util.Collection;
import java.util.StringJoiner;

import cn.taketoday.lang.Nullable;

/**
 * A base class for {@link RequestCondition} types providing implementations of
 * {@link #equals(Object)}, {@link #hashCode()}, and {@link #toString()}.
 *
 * @param <T> the type of objects that this RequestCondition can be combined
 * with and compared to
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class AbstractRequestCondition<T extends AbstractRequestCondition<T>> implements RequestCondition<T> {

  /**
   * Indicates whether this condition is empty, i.e. whether or not it
   * contains any discrete items.
   *
   * @return {@code true} if empty; {@code false} otherwise
   */
  public boolean isEmpty() {
    return getContent().isEmpty();
  }

  /**
   * Return the discrete items a request condition is composed of.
   * <p>For example URL patterns, HTTP request methods, param expressions, etc.
   *
   * @return a collection of objects (never {@code null})
   */
  protected abstract Collection<?> getContent();

  /**
   * The notation to use when printing discrete items of content.
   * <p>For example {@code " || "} for URL patterns or {@code " && "}
   * for param expressions.
   */
  protected abstract String getToStringInfix();

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    return getContent().equals(((AbstractRequestCondition<?>) other).getContent());
  }

  @Override
  public int hashCode() {
    return getContent().hashCode();
  }

  @Override
  public String toString() {
    String infix = getToStringInfix();
    StringJoiner joiner = new StringJoiner(infix, "[", "]");
    for (Object expression : getContent()) {
      joiner.add(expression.toString());
    }
    return joiner.toString();
  }

}
