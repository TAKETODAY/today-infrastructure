/*
 * Copyright 2017 - 2025 the original author or authors.
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
