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

package cn.taketoday.ui.context.support;

import cn.taketoday.lang.Nullable;
import cn.taketoday.ui.context.HierarchicalThemeSource;
import cn.taketoday.ui.context.Theme;
import cn.taketoday.ui.context.ThemeSource;

/**
 * Empty ThemeSource that delegates all calls to the parent ThemeSource.
 * If no parent is available, it simply won't resolve any theme.
 *
 * <p>Used as placeholder by UiApplicationContextUtils, if a context doesn't
 * define its own ThemeSource. Not intended for direct use in applications.
 *
 * @author Juergen Hoeller
 * @see UiApplicationContextUtils
 * @since 4.0
 */
public class DelegatingThemeSource implements HierarchicalThemeSource {

  @Nullable
  private ThemeSource parentThemeSource;

  @Override
  public void setParentThemeSource(@Nullable ThemeSource parentThemeSource) {
    this.parentThemeSource = parentThemeSource;
  }

  @Override
  @Nullable
  public ThemeSource getParentThemeSource() {
    return this.parentThemeSource;
  }

  @Override
  @Nullable
  public Theme getTheme(String themeName) {
    if (this.parentThemeSource != null) {
      return this.parentThemeSource.getTheme(themeName);
    }
    else {
      return null;
    }
  }

}
