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

package infra.expression.spel.support;

import infra.expression.TypedValue;

/**
 * A {@link TypedValue} for booleans.
 *
 * @author Andy Clement
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class BooleanTypedValue extends TypedValue {

  /**
   * True.
   */
  public static final BooleanTypedValue TRUE = new BooleanTypedValue(true);

  /**
   * False.
   */
  public static final BooleanTypedValue FALSE = new BooleanTypedValue(false);

  private BooleanTypedValue(boolean b) {
    super(b);
  }

  public static BooleanTypedValue forValue(boolean b) {
    return (b ? TRUE : FALSE);
  }

}
