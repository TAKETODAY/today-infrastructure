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

package infra.expression;

import infra.expression.common.TemplateParserContext;

/**
 * Input provided to an expression parser that can influence an expression
 * parsing/compilation routine.
 *
 * @author Keith Donald
 * @author Andy Clement
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface ParserContext {

  /**
   * Whether the expression being parsed is a template.
   * <p>A template consists of literal text that can be mixed with expressions.
   * Some examples:
   * <pre class="code">
   *     Some literal text
   *     Hello #{name.firstName}!
   *     #{3 + 4}
   * </pre>
   *
   * @return true if the expression is a template, false otherwise
   */
  boolean isTemplate();

  /**
   * For template expressions, returns the prefix that identifies the start of an
   * expression block within a string. For example: "${"
   *
   * @return the prefix that identifies the start of an expression
   */
  String getExpressionPrefix();

  /**
   * For template expressions, return the prefix that identifies the end of an
   * expression block within a string. For example: "}"
   *
   * @return the suffix that identifies the end of an expression
   */
  String getExpressionSuffix();

  /**
   * The default ParserContext implementation that enables template expression
   * parsing mode. The expression prefix is "#{" and the expression suffix is "}".
   *
   * @see #isTemplate()
   */
  ParserContext TEMPLATE_EXPRESSION = new TemplateParserContext();

}
