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

package cn.taketoday.expression.common;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import cn.taketoday.expression.Expression;
import cn.taketoday.expression.ExpressionParser;
import cn.taketoday.expression.ParseException;
import cn.taketoday.expression.ParserContext;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * An expression parser that understands templates. It can be subclassed by expression
 * parsers that do not offer first class support for templating.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Andy Clement
 * @since 4.0
 */
public abstract class TemplateAwareExpressionParser implements ExpressionParser {

  @Override
  public Expression parseExpression(String expressionString) throws ParseException {
    return parseExpression(expressionString, null);
  }

  @Override
  public Expression parseExpression(String expressionString, @Nullable ParserContext context) throws ParseException {
    if (context != null && context.isTemplate()) {
      Assert.notNull(expressionString, "'expressionString' is required");
      return parseTemplate(expressionString, context);
    }
    else {
      Assert.hasText(expressionString, "'expressionString' must not be null or blank");
      return doParseExpression(expressionString, context);
    }
  }

  private Expression parseTemplate(String expressionString, ParserContext context) throws ParseException {
    if (expressionString.isEmpty()) {
      return new LiteralExpression("");
    }

    List<Expression> expressions = parseExpressions(expressionString, context);
    if (expressions.size() == 1) {
      return expressions.get(0);
    }
    else {
      return new CompositeStringExpression(expressionString, expressions.toArray(new Expression[0]));
    }
  }

  /**
   * Helper that parses given expression string using the configured parser. The
   * expression string can contain any number of expressions all contained in "${...}"
   * markers. For instance: "foo${expr0}bar${expr1}". The static pieces of text will
   * also be returned as Expressions that just return that static piece of text. As a
   * result, evaluating all returned expressions and concatenating the results produces
   * the complete evaluated string. Unwrapping is only done of the outermost delimiters
   * found, so the string 'hello ${foo${abc}}' would break into the pieces 'hello ' and
   * 'foo${abc}'. This means that expression languages that used ${..} as part of their
   * functionality are supported without any problem. The parsing is aware of the
   * structure of an embedded expression. It assumes that parentheses '(', square
   * brackets '[' and curly brackets '}' must be in pairs within the expression unless
   * they are within a string literal and a string literal starts and terminates with a
   * single quote '.
   *
   * @param expressionString the expression string
   * @return the parsed expressions
   * @throws ParseException when the expressions cannot be parsed
   */
  private List<Expression> parseExpressions(String expressionString, ParserContext context) throws ParseException {
    ArrayList<Expression> expressions = new ArrayList<>();
    String prefix = context.getExpressionPrefix();
    String suffix = context.getExpressionSuffix();
    int startIdx = 0;

    while (startIdx < expressionString.length()) {
      int prefixIndex = expressionString.indexOf(prefix, startIdx);
      if (prefixIndex >= startIdx) {
        // an inner expression was found - this is a composite
        if (prefixIndex > startIdx) {
          expressions.add(new LiteralExpression(expressionString.substring(startIdx, prefixIndex)));
        }
        int afterPrefixIndex = prefixIndex + prefix.length();
        int suffixIndex = skipToCorrectEndSuffix(suffix, expressionString, afterPrefixIndex);
        if (suffixIndex == -1) {
          throw new ParseException(expressionString, prefixIndex,
                  "No ending suffix '" + suffix + "' for expression starting at character " +
                          prefixIndex + ": " + expressionString.substring(prefixIndex));
        }
        if (suffixIndex == afterPrefixIndex) {
          throw new ParseException(expressionString, prefixIndex,
                  "No expression defined within delimiter '" + prefix + suffix +
                          "' at character " + prefixIndex);
        }
        String expr = expressionString.substring(prefixIndex + prefix.length(), suffixIndex);
        expr = expr.trim();
        if (expr.isEmpty()) {
          throw new ParseException(expressionString, prefixIndex,
                  "No expression defined within delimiter '" + prefix + suffix +
                          "' at character " + prefixIndex);
        }
        expressions.add(doParseExpression(expr, context));
        startIdx = suffixIndex + suffix.length();
      }
      else {
        // no more ${expressions} found in string, add rest as static text
        expressions.add(new LiteralExpression(expressionString.substring(startIdx)));
        startIdx = expressionString.length();
      }
    }

    return expressions;
  }

  /**
   * Return true if the specified suffix can be found at the supplied position in the
   * supplied expression string.
   *
   * @param expressionString the expression string which may contain the suffix
   * @param pos the start position at which to check for the suffix
   * @param suffix the suffix string
   */
  private boolean isSuffixHere(String expressionString, int pos, String suffix) {
    int suffixPosition = 0;
    for (int i = 0; i < suffix.length() && pos < expressionString.length(); i++) {
      if (expressionString.charAt(pos++) != suffix.charAt(suffixPosition++)) {
        return false;
      }
    }
    // the expressionString ran out before the suffix could entirely be found
    return suffixPosition == suffix.length();
  }

  /**
   * Copes with nesting, for example '${...${...}}' where the correct end for the first
   * ${ is the final }.
   *
   * @param suffix the suffix
   * @param expressionString the expression string
   * @param afterPrefixIndex the most recently found prefix location for which the
   * matching end suffix is being sought
   * @return the position of the correct matching nextSuffix or -1 if none can be found
   */
  private int skipToCorrectEndSuffix(String suffix, String expressionString, int afterPrefixIndex)
          throws ParseException {

    // Chew on the expression text - relying on the rules:
    // brackets must be in pairs: () [] {}
    // string literals are "..." or '...' and these may contain unmatched brackets
    int pos = afterPrefixIndex;
    int maxlen = expressionString.length();
    int nextSuffix = expressionString.indexOf(suffix, afterPrefixIndex);
    if (nextSuffix == -1) {
      return -1; // the suffix is missing
    }
    Deque<Bracket> stack = new ArrayDeque<>();
    while (pos < maxlen) {
      if (isSuffixHere(expressionString, pos, suffix) && stack.isEmpty()) {
        break;
      }
      char ch = expressionString.charAt(pos);
      switch (ch) {
        case '{', '[', '(' -> stack.push(new Bracket(ch, pos));
        case '}', ']', ')' -> {
          if (stack.isEmpty()) {
            throw new ParseException(expressionString, pos, "Found closing '" + ch +
                    "' at position " + pos + " without an opening '" +
                    Bracket.theOpenBracketFor(ch) + "'");
          }
          Bracket p = stack.pop();
          if (!p.compatibleWithCloseBracket(ch)) {
            throw new ParseException(expressionString, pos, "Found closing '" + ch +
                    "' at position " + pos + " but most recent opening is '" + p.bracket +
                    "' at position " + p.pos);
          }
        }
        case '\'', '"' -> {
          // jump to the end of the literal
          int endLiteral = expressionString.indexOf(ch, pos + 1);
          if (endLiteral == -1) {
            throw new ParseException(expressionString, pos,
                    "Found non terminating string literal starting at position " + pos);
          }
          pos = endLiteral;
        }
      }
      pos++;
    }
    if (!stack.isEmpty()) {
      Bracket p = stack.pop();
      throw new ParseException(expressionString, p.pos, "Missing closing '" +
              Bracket.theCloseBracketFor(p.bracket) + "' for '" + p.bracket + "' at position " + p.pos);
    }
    if (!isSuffixHere(expressionString, pos, suffix)) {
      return -1;
    }
    return pos;
  }

  /**
   * Actually parse the expression string and return an Expression object.
   *
   * @param expressionString the raw expression string to parse
   * @param context a context for influencing this expression parsing routine (optional)
   * @return an evaluator for the parsed expression
   * @throws ParseException an exception occurred during parsing
   */
  protected abstract Expression doParseExpression(String expressionString, @Nullable ParserContext context)
          throws ParseException;

  /**
   * This captures a type of bracket and the position in which it occurs in the
   * expression. The positional information is used if an error has to be reported
   * because the related end bracket cannot be found. Bracket is used to describe:
   * square brackets [] round brackets () and curly brackets {}
   */
  private static class Bracket {

    char bracket;

    int pos;

    Bracket(char bracket, int pos) {
      this.bracket = bracket;
      this.pos = pos;
    }

    boolean compatibleWithCloseBracket(char closeBracket) {
      if (this.bracket == '{') {
        return closeBracket == '}';
      }
      else if (this.bracket == '[') {
        return closeBracket == ']';
      }
      return closeBracket == ')';
    }

    static char theOpenBracketFor(char closeBracket) {
      if (closeBracket == '}') {
        return '{';
      }
      else if (closeBracket == ']') {
        return '[';
      }
      return '(';
    }

    static char theCloseBracketFor(char openBracket) {
      if (openBracket == '{') {
        return '}';
      }
      else if (openBracket == '[') {
        return ']';
      }
      return ')';
    }
  }

}
