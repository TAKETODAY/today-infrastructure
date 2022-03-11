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

package cn.taketoday.expression.common;

import cn.taketoday.expression.ParserContext;

/**
 * Configurable {@link ParserContext} implementation for template parsing. Expects the
 * expression prefix and suffix as constructor arguments.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public class TemplateParserContext implements ParserContext {

  private final String expressionPrefix;

  private final String expressionSuffix;

  /**
   * Create a new TemplateParserContext with the default "#{" prefix and "}" suffix.
   */
  public TemplateParserContext() {
    this("#{", "}");
  }

  /**
   * Create a new TemplateParserContext for the given prefix and suffix.
   *
   * @param expressionPrefix the expression prefix to use
   * @param expressionSuffix the expression suffix to use
   */
  public TemplateParserContext(String expressionPrefix, String expressionSuffix) {
    this.expressionPrefix = expressionPrefix;
    this.expressionSuffix = expressionSuffix;
  }

  @Override
  public final boolean isTemplate() {
    return true;
  }

  @Override
  public final String getExpressionPrefix() {
    return this.expressionPrefix;
  }

  @Override
  public final String getExpressionSuffix() {
    return this.expressionSuffix;
  }

}
