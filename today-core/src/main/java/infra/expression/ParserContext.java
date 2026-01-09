/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
