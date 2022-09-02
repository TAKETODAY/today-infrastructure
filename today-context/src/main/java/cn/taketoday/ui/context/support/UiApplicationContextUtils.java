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

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.ui.context.HierarchicalThemeSource;
import cn.taketoday.ui.context.ThemeSource;

/**
 * Utility class for UI application context implementations.
 * Provides support for a special bean named "themeSource",
 * of type {@link ThemeSource}.
 *
 * @author Jean-Pierre Pawlak
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class UiApplicationContextUtils {

  /**
   * Name of the ThemeSource bean in the factory.
   * If none is supplied, theme resolution is delegated to the parent.
   *
   * @see ThemeSource
   */
  public static final String THEME_SOURCE_BEAN_NAME = "themeSource";

  private static final Logger log = LoggerFactory.getLogger(UiApplicationContextUtils.class);

  /**
   * Initialize the ThemeSource for the given application context,
   * autodetecting a bean with the name "themeSource". If no such
   * bean is found, a default (empty) ThemeSource will be used.
   *
   * @param context current application context
   * @return the initialized theme source (will never be {@code null})
   * @see #THEME_SOURCE_BEAN_NAME
   */
  public static ThemeSource initThemeSource(ApplicationContext context) {
    if (context.containsLocalBean(THEME_SOURCE_BEAN_NAME)) {
      ThemeSource themeSource = context.getBean(THEME_SOURCE_BEAN_NAME, ThemeSource.class);
      // Make ThemeSource aware of parent ThemeSource.
      if (context.getParent() instanceof ThemeSource && themeSource instanceof HierarchicalThemeSource hts) {
        if (hts.getParentThemeSource() == null) {
          // Only set parent context as parent ThemeSource if no parent ThemeSource
          // registered already.
          hts.setParentThemeSource((ThemeSource) context.getParent());
        }
      }
      if (log.isDebugEnabled()) {
        log.debug("Using ThemeSource [{}]", themeSource);
      }
      return themeSource;
    }
    else {
      // Use default ThemeSource to be able to accept getTheme calls, either
      // delegating to parent context's default or to local ResourceBundleThemeSource.
      HierarchicalThemeSource themeSource = null;
      if (context.getParent() instanceof ThemeSource) {
        themeSource = new DelegatingThemeSource();
        themeSource.setParentThemeSource((ThemeSource) context.getParent());
      }
      else {
        themeSource = new ResourceBundleThemeSource();
      }
      if (log.isDebugEnabled()) {
        log.debug("Unable to locate ThemeSource with name '{}': using default [{}]", THEME_SOURCE_BEAN_NAME, themeSource);
      }
      return themeSource;
    }
  }

}
