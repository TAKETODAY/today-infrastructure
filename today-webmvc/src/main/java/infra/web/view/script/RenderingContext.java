/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.view.script;

import java.util.Locale;
import java.util.function.Function;

import infra.context.ApplicationContext;

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
