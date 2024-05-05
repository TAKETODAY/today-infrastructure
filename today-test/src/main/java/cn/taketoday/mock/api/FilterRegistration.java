/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.mock.api;

import java.util.Collection;
import java.util.EnumSet;

/**
 * Interface through which a {@link Filter} may be further configured.
 *
 * @since Servlet 3.0
 */
public interface FilterRegistration extends Registration {

  /**
   * Adds a filter mapping with the given servlet names and dispatcher types for the Filter represented by this
   * FilterRegistration.
   *
   * <p>
   * Filter mappings are matched in the order in which they were added.
   *
   * <p>
   * Depending on the value of the <tt>isMatchAfter</tt> parameter, the given filter mapping will be considered after or
   * before any <i>declared</i> filter mappings of the MockContext from which this FilterRegistration was obtained.
   *
   * <p>
   * If this method is called multiple times, each successive call adds to the effects of the former.
   *
   * @param dispatcherTypes the dispatcher types of the filter mapping, or null if the default
   * <tt>DispatcherType.REQUEST</tt> is to be used
   * @param isMatchAfter true if the given filter mapping should be matched after any declared filter mappings, and false
   * if it is supposed to be matched before any declared filter mappings of the MockContext from which this
   * FilterRegistration was obtained
   * @param servletNames the servlet names of the filter mapping
   * @throws IllegalArgumentException if <tt>servletNames</tt> is null or empty
   * @throws IllegalStateException if the MockContext from which this FilterRegistration was obtained has already been
   * initialized
   */
  public void addMappingForServletNames(EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter,
          String... servletNames);

  /**
   * Gets the currently available servlet name mappings of the Filter represented by this <code>FilterRegistration</code>.
   *
   * <p>
   * If permitted, any changes to the returned <code>Collection</code> must not affect this
   * <code>FilterRegistration</code>.
   *
   * @return a (possibly empty) <code>Collection</code> of the currently available servlet name mappings of the Filter
   * represented by this <code>FilterRegistration</code>
   */
  public Collection<String> getServletNameMappings();

  /**
   * Adds a filter mapping with the given url patterns and dispatcher types for the Filter represented by this
   * FilterRegistration.
   *
   * <p>
   * Filter mappings are matched in the order in which they were added.
   *
   * <p>
   * Depending on the value of the <tt>isMatchAfter</tt> parameter, the given filter mapping will be considered after or
   * before any <i>declared</i> filter mappings of the MockContext from which this FilterRegistration was obtained.
   *
   * <p>
   * If this method is called multiple times, each successive call adds to the effects of the former.
   *
   * @param dispatcherTypes the dispatcher types of the filter mapping, or null if the default
   * <tt>DispatcherType.REQUEST</tt> is to be used
   * @param isMatchAfter true if the given filter mapping should be matched after any declared filter mappings, and false
   * if it is supposed to be matched before any declared filter mappings of the MockContext from which this
   * FilterRegistration was obtained
   * @param urlPatterns the url patterns of the filter mapping
   * @throws IllegalArgumentException if <tt>urlPatterns</tt> is null or empty
   * @throws IllegalStateException if the MockContext from which this FilterRegistration was obtained has already been
   * initialized
   */
  public void addMappingForUrlPatterns(EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter,
          String... urlPatterns);

  /**
   * Gets the currently available URL pattern mappings of the Filter represented by this <code>FilterRegistration</code>.
   *
   * <p>
   * If permitted, any changes to the returned <code>Collection</code> must not affect this
   * <code>FilterRegistration</code>.
   *
   * @return a (possibly empty) <code>Collection</code> of the currently available URL pattern mappings of the Filter
   * represented by this <code>FilterRegistration</code>
   */
  public Collection<String> getUrlPatternMappings();

  /**
   * Interface through which a {@link Filter} registered via one of the <tt>addFilter</tt> methods on
   * {@link MockContext} may be further configured.
   */
  interface Dynamic extends FilterRegistration, Registration.Dynamic {
  }
}
