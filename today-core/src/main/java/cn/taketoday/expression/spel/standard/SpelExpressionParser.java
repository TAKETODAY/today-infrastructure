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

package cn.taketoday.expression.spel.standard;

import cn.taketoday.expression.ParseException;
import cn.taketoday.expression.ParserContext;
import cn.taketoday.expression.common.TemplateAwareExpressionParser;
import cn.taketoday.expression.spel.SpelParserConfiguration;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * SpEL parser. Instances are reusable and thread-safe.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 4.0
 */
public class SpelExpressionParser extends TemplateAwareExpressionParser {

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
    Assert.notNull(configuration, "SpelParserConfiguration must not be null");
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
