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

package cn.taketoday.web.view.script;

import cn.taketoday.web.view.AbstractUrlBasedView;
import cn.taketoday.web.view.UrlBasedViewResolver;

/**
 * Convenience subclass of {@link UrlBasedViewResolver} that supports
 * {@link ScriptTemplateView} and custom subclasses of it.
 *
 * <p>The view class for all views created by this resolver can be specified
 * via the {@link #setViewClass(Class)} property.
 *
 * <p><b>Note:</b> When chaining ViewResolvers this resolver will check for the
 * existence of the specified template resources and only return a non-null
 * View object if a template is actually found.
 *
 * @author Sebastien Deleuze
 * @see ScriptTemplateConfigurer
 * @since 4.0
 */
public class ScriptTemplateViewResolver extends UrlBasedViewResolver {

  /**
   * Sets the default {@link #setViewClass view class} to {@link #requiredViewClass}:
   * by default {@link ScriptTemplateView}.
   */
  public ScriptTemplateViewResolver() {
    setViewClass(requiredViewClass());
  }

  /**
   * A convenience constructor that allows for specifying {@link #setPrefix prefix}
   * and {@link #setSuffix suffix} as constructor arguments.
   *
   * @param prefix the prefix that gets prepended to view names when building a URL
   * @param suffix the suffix that gets appended to view names when building a URL
   */
  public ScriptTemplateViewResolver(String prefix, String suffix) {
    this();
    setPrefix(prefix);
    setSuffix(suffix);
  }

  @Override
  protected Class<?> requiredViewClass() {
    return ScriptTemplateView.class;
  }

  @Override
  protected AbstractUrlBasedView instantiateView() {
    return getViewClass() == ScriptTemplateView.class ? new ScriptTemplateView() : super.instantiateView();
  }

}
