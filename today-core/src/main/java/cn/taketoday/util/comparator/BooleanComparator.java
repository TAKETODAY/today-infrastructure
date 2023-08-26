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

package cn.taketoday.util.comparator;

import java.io.Serializable;
import java.util.Comparator;

import cn.taketoday.lang.Nullable;

/**
 * A {@link Comparator} for {@link Boolean} objects that can sort either
 * {@code true} or {@code false} first.
 *
 * @author Keith Donald
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("serial")
public class BooleanComparator implements Comparator<Boolean>, Serializable {

  /**
   * A shared default instance of this comparator,
   * treating {@code true} lower than {@code false}.
   */
  public static final BooleanComparator TRUE_LOW = new BooleanComparator(true);

  /**
   * A shared default instance of this comparator,
   * treating {@code true} higher than {@code false}.
   */
  public static final BooleanComparator TRUE_HIGH = new BooleanComparator(false);

  private final boolean trueLow;

  /**
   * Create a BooleanComparator that sorts boolean values based on
   * the provided flag.
   * <p>Alternatively, you can use the default shared instances:
   * {@code BooleanComparator.TRUE_LOW} and
   * {@code BooleanComparator.TRUE_HIGH}.
   *
   * @param trueLow whether to treat true as lower or higher than false
   * @see #TRUE_LOW
   * @see #TRUE_HIGH
   */
  public BooleanComparator(boolean trueLow) {
    this.trueLow = trueLow;
  }

  @Override
  public int compare(Boolean left, Boolean right) {
    int multiplier = this.trueLow ? -1 : 1;
    return multiplier * Boolean.compare(left, right);
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return (this == other || (other instanceof BooleanComparator &&
            this.trueLow == ((BooleanComparator) other).trueLow));
  }

  @Override
  public int hashCode() {
    return Boolean.hashCode(this.trueLow);
  }

  @Override
  public String toString() {
    return "BooleanComparator: " + (this.trueLow ? "true low" : "true high");
  }

}
