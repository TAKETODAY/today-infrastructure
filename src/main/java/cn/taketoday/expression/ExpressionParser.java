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

package cn.taketoday.expression;

/**
 * Parses expression strings into compiled expressions that can be evaluated.
 * Supports parsing templates as well as standard expression strings.
 *
 * @author Keith Donald
 * @author Andy Clement
 * @since 4.0
 */
public interface ExpressionParser {

  /**
   * Parse the expression string and return an Expression object you can use for repeated evaluation.
   * <p>Some examples:
   * <pre class="code">
   *     3 + 4
   *     name.firstName
   * </pre>
   *
   * @param expressionString the raw expression string to parse
   * @return an evaluator for the parsed expression
   * @throws ParseException an exception occurred during parsing
   */
  Expression parseExpression(String expressionString) throws ParseException;

  /**
   * Parse the expression string and return an Expression object you can use for repeated evaluation.
   * <p>Some examples:
   * <pre class="code">
   *     3 + 4
   *     name.firstName
   * </pre>
   *
   * @param expressionString the raw expression string to parse
   * @param context a context for influencing this expression parsing routine (optional)
   * @return an evaluator for the parsed expression
   * @throws ParseException an exception occurred during parsing
   */
  Expression parseExpression(String expressionString, ParserContext context) throws ParseException;

}
