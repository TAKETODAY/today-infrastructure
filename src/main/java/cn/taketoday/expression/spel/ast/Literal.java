/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.expression.spel.ast;

import cn.taketoday.expression.TypedValue;
import cn.taketoday.expression.spel.ExpressionState;
import cn.taketoday.expression.spel.InternalParseException;
import cn.taketoday.expression.spel.SpelEvaluationException;
import cn.taketoday.expression.spel.SpelMessage;
import cn.taketoday.expression.spel.SpelParseException;
import cn.taketoday.lang.Nullable;

/**
 * Common superclass for nodes representing literals (boolean, string, number, etc).
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 */
public abstract class Literal extends SpelNodeImpl {

  @Nullable
  private final String originalValue;

  public Literal(@Nullable String originalValue, int startPos, int endPos) {
    super(startPos, endPos);
    this.originalValue = originalValue;
  }

  @Nullable
  public final String getOriginalValue() {
    return this.originalValue;
  }

  @Override
  public final TypedValue getValueInternal(ExpressionState state) throws SpelEvaluationException {
    return getLiteralValue();
  }

  @Override
  public String toString() {
    return String.valueOf(getLiteralValue().getValue());
  }

  @Override
  public String toStringAST() {
    return toString();
  }

  public abstract TypedValue getLiteralValue();

  /**
   * Process the string form of a number, using the specified base if supplied
   * and return an appropriate literal to hold it. Any suffix to indicate a
   * long will be taken into account (either 'l' or 'L' is supported).
   *
   * @param numberToken the token holding the number as its payload (eg. 1234 or 0xCAFE)
   * @param radix the base of number
   * @return a subtype of Literal that can represent it
   */
  public static Literal getIntLiteral(String numberToken, int startPos, int endPos, int radix) {
    try {
      int value = Integer.parseInt(numberToken, radix);
      return new IntLiteral(numberToken, startPos, endPos, value);
    }
    catch (NumberFormatException ex) {
      throw new InternalParseException(new SpelParseException(startPos, ex, SpelMessage.NOT_AN_INTEGER, numberToken));
    }
  }

  public static Literal getLongLiteral(String numberToken, int startPos, int endPos, int radix) {
    try {
      long value = Long.parseLong(numberToken, radix);
      return new LongLiteral(numberToken, startPos, endPos, value);
    }
    catch (NumberFormatException ex) {
      throw new InternalParseException(new SpelParseException(startPos, ex, SpelMessage.NOT_A_LONG, numberToken));
    }
  }

  public static Literal getRealLiteral(String numberToken, int startPos, int endPos, boolean isFloat) {
    try {
      if (isFloat) {
        float value = Float.parseFloat(numberToken);
        return new FloatLiteral(numberToken, startPos, endPos, value);
      }
      else {
        double value = Double.parseDouble(numberToken);
        return new RealLiteral(numberToken, startPos, endPos, value);
      }
    }
    catch (NumberFormatException ex) {
      throw new InternalParseException(new SpelParseException(startPos, ex, SpelMessage.NOT_A_REAL, numberToken));
    }
  }

}
