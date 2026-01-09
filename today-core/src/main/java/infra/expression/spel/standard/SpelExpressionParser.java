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

package infra.expression.spel.standard;

import org.jspecify.annotations.Nullable;

import infra.expression.ParseException;
import infra.expression.ParserContext;
import infra.expression.common.TemplateAwareExpressionParser;
import infra.expression.spel.SpelParserConfiguration;
import infra.lang.Assert;

/**
 * SpEL parser. Instances are reusable and thread-safe.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class SpelExpressionParser extends TemplateAwareExpressionParser {

  public static final SpelExpressionParser INSTANCE = new SpelExpressionParser();

  private final SpelParserConfiguration configuration;

  /**
   * Create a parser with default settings.
   */
  public SpelExpressionParser() {
    this.configuration = new SpelParserConfiguration();
  }

  /**
   * Create a parser with the specified configuration.
   *
   * @param configuration custom configuration options
   */
  public SpelExpressionParser(SpelParserConfiguration configuration) {
    Assert.notNull(configuration, "SpelParserConfiguration is required");
    this.configuration = configuration;
  }

  public SpelExpression parseRaw(String expressionString) throws ParseException {
    Assert.hasText(expressionString, "'expressionString' must not be null or blank");
    return doParseExpression(expressionString, null);
  }

  @Override
  protected SpelExpression doParseExpression(String expressionString, @Nullable ParserContext context) throws ParseException {
    return new InternalSpelExpressionParser(this.configuration).doParseExpression(expressionString, context);
  }

}
