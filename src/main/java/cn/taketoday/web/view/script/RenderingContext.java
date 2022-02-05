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

import java.util.Locale;
import java.util.function.Function;

import cn.taketoday.context.ApplicationContext;

/**
 * Context passed to {@link ScriptTemplateView} render function in order to make
 * the application context, the locale, the template loader and the url available on
 * scripting side.
 *
 * @author Sebastien Deleuze
 * @since 4.0
 */
public class RenderingContext {

  private final ApplicationContext applicationContext;

  private final Locale locale;

  private final Function<String, String> templateLoader;

  private final String url;

  /**
   * Create a new {@code RenderingContext}.
   *
   * @param applicationContext the application context
   * @param locale the locale of the rendered template
   * @param templateLoader a function that takes a template path as input and returns
   * the template content as a String
   * @param url the URL of the rendered template
   */
  public RenderingContext(
          ApplicationContext applicationContext, Locale locale,
          Function<String, String> templateLoader, String url) {

    this.url = url;
    this.locale = locale;
    this.templateLoader = templateLoader;
    this.applicationContext = applicationContext;
  }

  /**
   * Return the application context.
   */
  public ApplicationContext getApplicationContext() {
    return this.applicationContext;
  }

  /**
   * Return the locale of the rendered template.
   */
  public Locale getLocale() {
    return this.locale;
  }

  /**
   * Return a function that takes a template path as input and returns the template
   * content as a String.
   */
  public Function<String, String> getTemplateLoader() {
    return this.templateLoader;
  }

  /**
   * Return the URL of the rendered template.
   */
  public String getUrl() {
    return this.url;
  }

}
