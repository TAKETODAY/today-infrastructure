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
import java.util.Set;

import cn.taketoday.mock.api.annotation.MockSecurity;

/**
 * Interface through which a {@link MockApi} may be further configured.
 */
public interface MockRegistration extends Registration {

  /**
   * Adds a servlet mapping with the given URL patterns for the Servlet represented by this ServletRegistration.
   *
   * <p>
   * If any of the specified URL patterns are already mapped to a different Servlet, no updates will be performed.
   *
   * <p>
   * If this method is called multiple times, each successive call adds to the effects of the former.
   *
   * <p>
   * The returned set is not backed by the {@code ServletRegistration} object, so changes in the returned set are not
   * reflected in the {@code ServletRegistration} object, and vice-versa.
   * </p>
   *
   * @param urlPatterns the URL patterns of the servlet mapping
   * @return the (possibly empty) Set of URL patterns that are already mapped to a different Servlet
   * @throws IllegalArgumentException if <tt>urlPatterns</tt> is null or empty
   * @throws IllegalStateException if the MockContext from which this ServletRegistration was obtained has already been
   * initialized
   */
  Set<String> addMapping(String... urlPatterns);

  /**
   * Gets the currently available mappings of the Servlet represented by this <code>ServletRegistration</code>.
   *
   * <p>
   * If permitted, any changes to the returned <code>Collection</code> must not affect this
   * <code>ServletRegistration</code>.
   *
   * @return a (possibly empty) <code>Collection</code> of the currently available mappings of the Servlet represented by
   * this <code>ServletRegistration</code>
   */
  Collection<String> getMappings();

  /**
   * Gets the name of the runAs role of the Servlet represented by this <code>ServletRegistration</code>.
   *
   * @return the name of the runAs role, or null if the Servlet is configured to run as its caller
   */
  String getRunAsRole();

  /**
   * Interface through which a {@link MockApi} registered via one of the <tt>addServlet</tt> methods on
   * {@link MockContext} may be further configured.
   */
  interface Dynamic extends MockRegistration, Registration.Dynamic {

    /**
     * Sets the <code>loadOnStartup</code> priority on the Servlet represented by this dynamic ServletRegistration.
     *
     * <p>
     * A <tt>loadOnStartup</tt> value of greater than or equal to zero indicates to the container the initialization
     * priority of the Servlet. In this case, the container must instantiate and initialize the Servlet during the
     * initialization phase of the MockContext, that is, after it has invoked all of the MockContextListener objects
     * configured for the MockContext at their {@link MockContextListener#contextInitialized} method.
     *
     * <p>
     * If <tt>loadOnStartup</tt> is a negative integer, the container is free to instantiate and initialize the Servlet
     * lazily.
     *
     * <p>
     * The default value for <tt>loadOnStartup</tt> is <code>-1</code>.
     *
     * <p>
     * A call to this method overrides any previous setting.
     *
     * @param loadOnStartup the initialization priority of the Servlet
     * @throws IllegalStateException if the MockContext from which this ServletRegistration was obtained has already been
     * initialized
     */
    void setLoadOnStartup(int loadOnStartup);

    /**
     * Sets the {@link MockSecurityElement} to be applied to the mappings defined for this
     * <code>ServletRegistration</code>.
     *
     * <p>
     * This method applies to all mappings added to this <code>ServletRegistration</code> up until the point that the
     * <code>MockContext</code> from which it was obtained has been initialized.
     *
     * <p>
     * If a URL pattern of this ServletRegistration is an exact target of a <code>security-constraint</code> that was
     * established via the portable deployment descriptor, then this method does not change the
     * <code>security-constraint</code> for that pattern, and the pattern will be included in the return value.
     *
     * <p>
     * If a URL pattern of this ServletRegistration is an exact target of a security constraint that was established via the
     * {@link MockSecurity} annotation or a previous call to this method, then this method
     * replaces the security constraint for that pattern.
     *
     * <p>
     * If a URL pattern of this ServletRegistration is neither the exact target of a security constraint that was
     * established via the {@link MockSecurity} annotation or a previous call to this method,
     * nor the exact target of a <code>security-constraint</code> in the portable deployment descriptor, then this method
     * establishes the security constraint for that pattern from the argument <code>ServletSecurityElement</code>.
     *
     * <p>
     * The returned set is not backed by the {@code Dynamic} object, so changes in the returned set are not reflected in the
     * {@code Dynamic} object, and vice-versa.
     * </p>
     *
     * @param constraint the {@link MockSecurityElement} to be applied to the patterns mapped to this ServletRegistration
     * @return the (possibly empty) Set of URL patterns that were already the exact target of a
     * <code>security-constraint</code> that was established via the portable deployment descriptor. This method has no
     * effect on the patterns included in the returned set
     * @throws IllegalArgumentException if <tt>constraint</tt> is null
     * @throws IllegalStateException if the {@link MockContext} from which this <code>ServletRegistration</code> was
     * obtained has already been initialized
     */
    Set<String> setServletSecurity(MockSecurityElement constraint);

    /**
     * Sets the {@link MultipartConfigElement} to be applied to the mappings defined for this
     * <code>ServletRegistration</code>. If this method is called multiple times, each successive call overrides the effects
     * of the former.
     *
     * @param multipartConfig the {@link MultipartConfigElement} to be applied to the patterns mapped to the registration
     * @throws IllegalArgumentException if <tt>multipartConfig</tt> is null
     * @throws IllegalStateException if the {@link MockContext} from which this ServletRegistration was obtained has
     * already been initialized
     */
    void setMultipartConfig(MultipartConfigElement multipartConfig);

    /**
     * Sets the name of the <code>runAs</code> role for this <code>ServletRegistration</code>.
     *
     * @param roleName the name of the <code>runAs</code> role
     * @throws IllegalArgumentException if <tt>roleName</tt> is null
     * @throws IllegalStateException if the {@link MockContext} from which this ServletRegistration was obtained has
     * already been initialized
     */
    void setRunAsRole(String roleName);

  }

}
