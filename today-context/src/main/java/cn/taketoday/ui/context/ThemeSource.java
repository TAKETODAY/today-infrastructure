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

import cn.taketoday.lang.Nullable;

/**
 * Interface to be implemented by objects that can resolve {@link Theme Themes}.
 * This enables parameterization and internationalization of messages
 * for a given 'theme'.
 *
 * @author Jean-Pierre Pawlak
 * @author Juergen Hoeller
 * @see Theme
 * @since 4.0
 */
public interface ThemeSource {

  /**
   * Return the Theme instance for the given theme name.
   * <p>The returned Theme will resolve theme-specific messages, codes,
   * file paths, etc (e.g. CSS and image files in a web environment).
   *
   * @param themeName the name of the theme
   * @return the corresponding Theme, or {@code null} if none defined.
   * Note that, by convention, a ThemeSource should at least be able to
   * return a default Theme for the default theme name "theme" but may also
   * return default Themes for other theme names.
   */
  @Nullable
  Theme getTheme(String themeName);

}
