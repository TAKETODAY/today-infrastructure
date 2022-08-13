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

package cn.taketoday.web.view;

import java.util.Locale;

import cn.taketoday.lang.Nullable;

/**
 * ViewRef contains a view-name and locale
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/13 21:49
 */
public class ViewRef {

  @Nullable
  private final Locale locale;
  private final String viewName;

  public ViewRef(String viewName, @Nullable Locale locale) {
    this.viewName = viewName;
    this.locale = locale;
  }

  @Nullable
  public Locale getLocale() {
    return locale;
  }

  public String getViewName() {
    return viewName;
  }

  public static ViewRef of(String viewName) {
    return new ViewRef(viewName, null);
  }

  public static ViewRef of(String viewName, @Nullable Locale locale) {
    return new ViewRef(viewName, locale);
  }

}
