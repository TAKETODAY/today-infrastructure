/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.web.validation;

import java.util.Locale;

import javax.validation.MessageInterpolator;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ExpressionEvaluator;

/**
 * @author TODAY 2019-07-21 20:17
 * @since 3.0
 */
public class ContextMessageInterpolator implements MessageInterpolator {
  private final ExpressionEvaluator expressionEvaluator;

  public ContextMessageInterpolator(ApplicationContext context) {
    this.expressionEvaluator = new ExpressionEvaluator(context);
  }

  /**
   * EL processing
   * <p>
   * use {@link ExpressionEvaluator#evaluate(String, Class)}
   * </p>
   */
  @Override
  public String interpolate(String messageTemplate, MessageInterpolator.Context context) {
    return expressionEvaluator.evaluate(messageTemplate, String.class);
  }

  /**
   * EL processing
   * <p>
   * use {@link ExpressionEvaluator#evaluate(String, Class)}
   * </p>
   */
  @Override
  public String interpolate(String messageTemplate, Context context, Locale locale) {
    return expressionEvaluator.evaluate(messageTemplate, String.class);
  }

}
