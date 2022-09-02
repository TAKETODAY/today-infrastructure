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
 * Sub-interface of ThemeSource to be implemented by objects that
 * can resolve theme messages hierarchically.
 *
 * @author Jean-Pierre Pawlak
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Juergen Hoeller
 * @since 4.0
 */
public interface HierarchicalThemeSource extends ThemeSource {

  /**
   * Set the parent that will be used to try to resolve theme messages
   * that this object can't resolve.
   *
   * @param parent the parent ThemeSource that will be used to
   * resolve messages that this object can't resolve.
   * May be {@code null}, in which case no further resolution is possible.
   */
  void setParentThemeSource(@Nullable ThemeSource parent);

  /**
   * Return the parent of this ThemeSource, or {@code null} if none.
   */
  @Nullable
  ThemeSource getParentThemeSource();

}
