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

import java.util.Map;
import java.util.Set;

/**
 * Interface through which a {@link Servlet} or {@link Filter} may be further configured.
 *
 * <p>
 * A Registration object whose {@link #getClassName} method returns null is considered <i>preliminary</i>. Servlets and
 * Filters whose implementation class is container implementation specific may be declared without any
 * <tt>servlet-class</tt> or <tt>filter-class</tt> elements, respectively, and will be represented as preliminary
 * Registration objects. Preliminary registrations must be completed by calling one of the <tt>addServlet</tt> or
 * <tt>addFilter</tt> methods on {@link MockContext}, and passing in the Servlet or Filter name (obtained via
 * {@link #getName}) along with the supporting Servlet or Filter implementation class name, Class object, or instance,
 * respectively. In most cases, preliminary registrations will be completed by an appropriate, container-provided
 * {@link ServletContainerInitializer}.
 *
 * @since Servlet 3.0
 */
public interface Registration {

  /**
   * Gets the name of the Servlet or Filter that is represented by this Registration.
   *
   * @return the name of the Servlet or Filter that is represented by this Registration
   */
  public String getName();

  /**
   * Gets the fully qualified class name of the Servlet or Filter that is represented by this Registration.
   *
   * @return the fully qualified class name of the Servlet or Filter that is represented by this Registration, or null if
   * this Registration is preliminary
   */
  public String getClassName();

  /**
   * Sets the initialization parameter with the given name and value on the Servlet or Filter that is represented by this
   * Registration.
   *
   * @param name the initialization parameter name
   * @param value the initialization parameter value
   * @return true if the update was successful, i.e., an initialization parameter with the given name did not already
   * exist for the Servlet or Filter represented by this Registration, and false otherwise
   * @throws IllegalStateException if the MockContext from which this Registration was obtained has already been
   * initialized
   * @throws IllegalArgumentException if the given name or value is <tt>null</tt>
   */
  public boolean setInitParameter(String name, String value);

  /**
   * Gets the value of the initialization parameter with the given name that will be used to initialize the Servlet or
   * Filter represented by this Registration object.
   *
   * @param name the name of the initialization parameter whose value is requested
   * @return the value of the initialization parameter with the given name, or <tt>null</tt> if no initialization
   * parameter with the given name exists
   */
  public String getInitParameter(String name);

  /**
   * Sets the given initialization parameters on the Servlet or Filter that is represented by this Registration.
   *
   * <p>
   * The given map of initialization parameters is processed <i>by-value</i>, i.e., for each initialization parameter
   * contained in the map, this method calls {@link #setInitParameter(String, String)}. If that method would return false
   * for any of the initialization parameters in the given map, no updates will be performed, and false will be returned.
   * Likewise, if the map contains an initialization parameter with a <tt>null</tt> name or value, no updates will be
   * performed, and an IllegalArgumentException will be thrown.
   *
   * <p>
   * The returned set is not backed by the {@code Registration} object, so changes in the returned set are not reflected
   * in the {@code Registration} object, and vice-versa.
   * </p>
   *
   * @param initParameters the initialization parameters
   * @return the (possibly empty) Set of initialization parameter names that are in conflict
   * @throws IllegalStateException if the MockContext from which this Registration was obtained has already been
   * initialized
   * @throws IllegalArgumentException if the given map contains an initialization parameter with a <tt>null</tt> name or
   * value
   */
  public Set<String> setInitParameters(Map<String, String> initParameters);

  /**
   * Gets an immutable (and possibly empty) Map containing the currently available initialization parameters that will be
   * used to initialize the Servlet or Filter represented by this Registration object.
   *
   * @return Map containing the currently available initialization parameters that will be used to initialize the Servlet
   * or Filter represented by this Registration object
   */
  public Map<String, String> getInitParameters();

  /**
   * Interface through which a {@link Servlet} or {@link Filter} registered via one of the <tt>addServlet</tt> or
   * <tt>addFilter</tt> methods, respectively, on {@link MockContext} may be further configured.
   */
  interface Dynamic extends Registration {

    /**
     * Configures the Servlet or Filter represented by this dynamic Registration as supporting asynchronous operations or
     * not.
     *
     * <p>
     * By default, servlet and filters do not support asynchronous operations.
     *
     * <p>
     * A call to this method overrides any previous setting.
     *
     * @param isAsyncSupported true if the Servlet or Filter represented by this dynamic Registration supports asynchronous
     * operations, false otherwise
     * @throws IllegalStateException if the MockContext from which this dynamic Registration was obtained has already
     * been initialized
     */
    public void setAsyncSupported(boolean isAsyncSupported);
  }
}
