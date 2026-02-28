/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.view;

import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * ViewRef represents a reference to a view, encapsulating both the view name and an optional locale.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/13 21:49
 */
public class ViewRef {

  private final @Nullable Locale locale;

  private final String viewName;

  /**
   * Constructs a new ViewRef with the specified view name and locale.
   *
   * @param viewName the name of the view
   * @param locale the locale associated with the view, can be null
   */
  public ViewRef(String viewName, @Nullable Locale locale) {
    this.viewName = viewName;
    this.locale = locale;
  }

  /**
   * Returns the locale associated with this view reference.
   *
   * @return the locale, or null if no locale is specified
   */
  @Nullable
  public Locale getLocale() {
    return locale;
  }

  /**
   * Returns the name of the view.
   *
   * @return the view name
   */
  public String getViewName() {
    return viewName;
  }

  /**
   * Creates a ViewRef instance with the given view name and no locale.
   *
   * @param viewName the name of the view
   * @return a new ViewRef instance
   */
  public static ViewRef forViewName(String viewName) {
    return new ViewRef(viewName, null);
  }

  /**
   * Creates a ViewRef instance with the given view name and locale.
   *
   * @param viewName the name of the view
   * @param locale the locale associated with the view, can be null
   * @return a new ViewRef instance
   */
  public static ViewRef forViewName(String viewName, @Nullable Locale locale) {
    return new ViewRef(viewName, locale);
  }

}
