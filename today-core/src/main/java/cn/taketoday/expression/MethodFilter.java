/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.expression;

import java.lang.reflect.Method;
import java.util.List;

/**
 * MethodFilter instances allow SpEL users to fine tune the behaviour of the method
 * resolution process. Method resolution (which translates from a method name in an
 * expression to a real method to invoke) will normally retrieve candidate methods for
 * invocation via a simple call to 'Class.getMethods()' and will choose the first one that
 * is suitable for the input parameters. By registering a MethodFilter the user can
 * receive a callback and change the methods that will be considered suitable.
 *
 * @author Andy Clement
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@FunctionalInterface
public interface MethodFilter {

  /**
   * Called by the method resolver to allow the SpEL user to organize the list of
   * candidate methods that may be invoked. The filter can remove methods that should
   * not be considered candidates and it may sort the results. The resolver will then
   * search through the methods as returned from the filter when looking for a suitable
   * candidate to invoke.
   *
   * @param methods the full list of methods the resolver was going to choose from
   * @return a possible subset of input methods that may be sorted by order of relevance
   */
  List<Method> filter(List<Method> methods);

}
