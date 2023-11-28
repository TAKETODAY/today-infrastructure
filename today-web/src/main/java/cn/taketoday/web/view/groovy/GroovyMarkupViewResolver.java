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

package cn.taketoday.web.view.groovy;

import java.util.Locale;

import cn.taketoday.web.view.AbstractTemplateViewResolver;
import cn.taketoday.web.view.AbstractUrlBasedView;

/**
 * Convenience subclass of {@link AbstractTemplateViewResolver} that supports
 * {@link GroovyMarkupView} (i.e. Groovy XML/XHTML markup templates) and
 * custom subclasses of it.
 *
 * <p>The view class for all views created by this resolver can be specified
 * via {@link #setViewClass(Class)}.
 *
 * <p><b>Note:</b> When chaining ViewResolvers this resolver will check for the
 * existence of the specified template resources and only return a non-null
 * {@code View} object if a template is actually found.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see GroovyMarkupConfigurer
 * @since 4.0
 */
public class GroovyMarkupViewResolver extends AbstractTemplateViewResolver {

  /**
   * Sets the default {@link #setViewClass view class} to {@link #requiredViewClass}:
   * by default {@link GroovyMarkupView}.
   */
  public GroovyMarkupViewResolver() {
    setViewClass(requiredViewClass());
  }

  /**
   * A convenience constructor that allows for specifying the {@link #setPrefix prefix}
   * and {@link #setSuffix suffix} as constructor arguments.
   *
   * @param prefix the prefix that gets prepended to view names when building a URL
   * @param suffix the suffix that gets appended to view names when building a URL
   */
  public GroovyMarkupViewResolver(String prefix, String suffix) {
    this();
    setPrefix(prefix);
    setSuffix(suffix);
  }

  @Override
  protected Class<?> requiredViewClass() {
    return GroovyMarkupView.class;
  }

  @Override
  protected AbstractUrlBasedView instantiateView() {
    return getViewClass() == GroovyMarkupView.class ? new GroovyMarkupView() : super.instantiateView();
  }

  /**
   * This resolver supports i18n, so cache keys should contain the locale.
   */
  @Override
  protected Object getCacheKey(String viewName, Locale locale) {
    return viewName + '_' + locale;
  }

}
