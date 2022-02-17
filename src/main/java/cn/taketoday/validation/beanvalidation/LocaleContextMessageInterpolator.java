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

package cn.taketoday.validation.beanvalidation;

import java.util.Locale;

import cn.taketoday.core.i18n.LocaleContextHolder;
import cn.taketoday.lang.Assert;
import jakarta.validation.MessageInterpolator;

/**
 * Delegates to a target {@link MessageInterpolator} implementation but enforces Framework's
 * managed Locale. Typically used to wrap the validation provider's default interpolator.
 *
 * @author Juergen Hoeller
 * @see cn.taketoday.core.i18n.LocaleContextHolder#getLocale()
 * @since 4.0
 */
public class LocaleContextMessageInterpolator implements MessageInterpolator {

  private final MessageInterpolator targetInterpolator;

  /**
   * Create a new LocaleContextMessageInterpolator, wrapping the given target interpolator.
   *
   * @param targetInterpolator the target MessageInterpolator to wrap
   */
  public LocaleContextMessageInterpolator(MessageInterpolator targetInterpolator) {
    Assert.notNull(targetInterpolator, "Target MessageInterpolator must not be null");
    this.targetInterpolator = targetInterpolator;
  }

  @Override
  public String interpolate(String message, Context context) {
    return this.targetInterpolator.interpolate(message, context, LocaleContextHolder.getLocale());
  }

  @Override
  public String interpolate(String message, Context context, Locale locale) {
    return this.targetInterpolator.interpolate(message, context, locale);
  }

}
