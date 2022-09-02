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

package cn.taketoday.ui.context;

import cn.taketoday.context.MessageSource;

/**
 * A Theme can resolve theme-specific messages, codes, file paths, etc.
 * (e&#46;g&#46; CSS and image files in a web environment).
 * The exposed {@link MessageSource} supports
 * theme-specific parameterization and internationalization.
 *
 * @author Juergen Hoeller
 * @see ThemeSource
 * @since 4.0
 */
public interface Theme {

  /**
   * Return the name of the theme.
   *
   * @return the name of the theme (never {@code null})
   */
  String getName();

  /**
   * Return the specific MessageSource that resolves messages
   * with respect to this theme.
   *
   * @return the theme-specific MessageSource (never {@code null})
   */
  MessageSource getMessageSource();

}
