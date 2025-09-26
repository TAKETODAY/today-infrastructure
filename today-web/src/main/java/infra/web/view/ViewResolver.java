/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.web.view;

import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * Interface to be implemented by objects that can resolve views by name.
 *
 * <p>View state doesn't change during the running of the application,
 * so implementations are free to cache views.
 *
 * <p>Implementations are encouraged to support internationalization,
 * i.e. localized view resolution.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ContentNegotiatingViewResolver
 * @see BeanNameViewResolver
 * @since 4.0 2022/1/29 11:12
 */
public interface ViewResolver {

  /**
   * Resolve the given view by name.
   * <p>Note: To allow for ViewResolver chaining, a ViewResolver should
   * return {@code null} if a view with the given name is not defined in it.
   * However, this is not required: Some ViewResolvers will always attempt
   * to build View objects with the given name, unable to return {@code null}
   * (rather throwing an exception when View creation failed).
   *
   * @param viewName name of the view to resolve
   * @param locale the Locale in which to resolve the view.
   * ViewResolvers that support internationalization should respect this.
   * @return the View object, or {@code null} if not found
   * (optional, to allow for ViewResolver chaining)
   * @throws Exception if the view cannot be resolved
   * (typically in case of problems creating an actual View object)
   */
  @Nullable
  View resolveViewName(String viewName, Locale locale) throws Exception;

}

