/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import cn.taketoday.expression.EvaluationException;
import cn.taketoday.expression.spel.ExpressionState;
import cn.taketoday.expression.spel.SpelEvaluationException;
import cn.taketoday.expression.spel.SpelMessage;
import cn.taketoday.expression.spel.support.BooleanTypedValue;

/**
 * Implements the matches operator. Matches takes two operands:
 * The first is a String and the second is a Java regex.
 * It will return {@code true} when {@link #getValue} is called
 * if the first operand matches the regex.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class OperatorMatches extends Operator {

  private static final int PATTERN_ACCESS_THRESHOLD = 1000000;

  /**
   * Maximum number of characters permitted in a regular expression.
   */
  private static final int MAX_REGEX_LENGTH = 1000;

  private final ConcurrentMap<String, Pattern> patternCache;

  /**
   * Create a new {@link OperatorMatches} instance with a shared pattern cache.
   */
  public OperatorMatches(ConcurrentMap<String, Pattern> patternCache, int startPos, int endPos, SpelNodeImpl... operands) {
    super("matches", startPos, endPos, operands);
    this.patternCache = patternCache;
  }

  /**
   * Check the first operand matches the regex specified as the second operand.
   *
   * @param state the expression state
   * @return {@code true} if the first operand matches the regex specified as the
   * second operand, otherwise {@code false}
   * @throws EvaluationException if there is a problem evaluating the expression
   * (e.g. the regex is invalid)
   */
  @Override
  public BooleanTypedValue getValueInternal(ExpressionState state) throws EvaluationException {
    SpelNodeImpl leftOp = getLeftOperand();
    SpelNodeImpl rightOp = getRightOperand();

    String input = leftOp.getValue(state, String.class);
    if (input == null) {
      throw new SpelEvaluationException(leftOp.getStartPosition(),
              SpelMessage.INVALID_FIRST_OPERAND_FOR_MATCHES_OPERATOR, (Object) null);
    }

    Object right = rightOp.getValue(state);
    if (!(right instanceof String regex)) {
      throw new SpelEvaluationException(rightOp.getStartPosition(),
              SpelMessage.INVALID_SECOND_OPERAND_FOR_MATCHES_OPERATOR, right);
    }

    try {
      Pattern pattern = this.patternCache.get(regex);
      if (pattern == null) {
        checkRegexLength(regex);
        pattern = Pattern.compile(regex);
        this.patternCache.putIfAbsent(regex, pattern);
      }
      Matcher matcher = pattern.matcher(new MatcherInput(input, new AccessCount()));
      return BooleanTypedValue.forValue(matcher.matches());
    }
    catch (PatternSyntaxException ex) {
      throw new SpelEvaluationException(
              rightOp.getStartPosition(), ex, SpelMessage.INVALID_PATTERN, right);
    }
    catch (IllegalStateException ex) {
      throw new SpelEvaluationException(
              rightOp.getStartPosition(), ex, SpelMessage.FLAWED_PATTERN, right);
    }
  }

  private void checkRegexLength(String regex) {
    if (regex.length() > MAX_REGEX_LENGTH) {
      throw new SpelEvaluationException(getStartPosition(),
              SpelMessage.MAX_REGEX_LENGTH_EXCEEDED, MAX_REGEX_LENGTH);
    }
  }

  private static class AccessCount {

    private int count;

    public void check() throws IllegalStateException {
      if (this.count++ > PATTERN_ACCESS_THRESHOLD) {
        throw new IllegalStateException("Pattern access threshold exceeded");
      }
    }
  }

  private static class MatcherInput implements CharSequence {

    private final CharSequence value;

    private final AccessCount access;

    public MatcherInput(CharSequence value, AccessCount access) {
      this.value = value;
      this.access = access;
    }

    @Override
    public char charAt(int index) {
      this.access.check();
      return this.value.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
      return new MatcherInput(this.value.subSequence(start, end), this.access);
    }

    @Override
    public int length() {
      return this.value.length();
    }

    @Override
    public String toString() {
      return this.value.toString();
    }
  }

}
