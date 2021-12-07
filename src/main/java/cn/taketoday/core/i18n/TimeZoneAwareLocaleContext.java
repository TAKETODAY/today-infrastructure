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

package cn.taketoday.core.i18n;

import java.util.TimeZone;

import cn.taketoday.lang.Nullable;

/**
 * Extension of {@link LocaleContext}, adding awareness of the current time zone.
 *
 * <p>Having this variant of LocaleContext set to {@link LocaleContextHolder} means
 * that some TimeZone-aware infrastructure has been configured, even if it may not
 * be able to produce a non-null TimeZone at the moment.
 *
 * @author Juergen Hoeller
 * @author Nicholas Williams
 * @see LocaleContextHolder#getTimeZone()
 * @since 4.0
 */
public interface TimeZoneAwareLocaleContext extends LocaleContext {

  /**
   * Return the current TimeZone, which can be fixed or determined dynamically,
   * depending on the implementation strategy.
   *
   * @return the current TimeZone, or {@code null} if no specific TimeZone associated
   */
  @Nullable
  TimeZone getTimeZone();

}
