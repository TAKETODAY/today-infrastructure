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

package infra.mock.api;

import java.util.Map;

import infra.mock.api.http.Cookie;

/**
 * Class that may be used to configure various properties of cookies used for session tracking purposes.
 *
 * <p>
 * An instance of this class is acquired by a call to {@link MockContext#getSessionCookieConfig}.
 */
public interface SessionCookieConfig {

  /**
   * Sets the name that will be assigned to any session tracking cookies created on behalf of the application represented
   * by the <tt>MockContext</tt> from which this <tt>SessionCookieConfig</tt> was acquired.
   *
   * <p>
   * NOTE: Changing the name of session tracking cookies may break other tiers (for example, a load balancing frontend)
   * that assume the cookie name to be equal to the default <tt>JSESSIONID</tt>, and therefore should only be done
   * cautiously.
   *
   * @param name the cookie name to use
   * @throws IllegalStateException if the <tt>MockContext</tt> from which this <tt>SessionCookieConfig</tt> was
   * acquired has already been initialized
   */
  void setName(String name);

  /**
   * Gets the name that will be assigned to any session tracking cookies created on behalf of the application represented
   * by the <tt>MockContext</tt> from which this <tt>SessionCookieConfig</tt> was acquired.
   *
   * <p>
   * By default, <tt>JSESSIONID</tt> will be used as the cookie name.
   *
   * @return the cookie name set via {@link #setName}, or <tt>null</tt> if {@link #setName} was never called
   * @see Cookie#getName()
   */
  String getName();

  /**
   * Sets the domain name that will be assigned to any session tracking cookies created on behalf of the application
   * represented by the <tt>MockContext</tt> from which this <tt>SessionCookieConfig</tt> was acquired.
   *
   * @param domain the cookie domain to use
   * @throws IllegalStateException if the <tt>MockContext</tt> from which this <tt>SessionCookieConfig</tt> was
   * acquired has already been initialized
   * @see Cookie#setDomain(String)
   */
  void setDomain(String domain);

  /**
   * Gets the domain name that will be assigned to any session tracking cookies created on behalf of the application
   * represented by the <tt>MockContext</tt> from which this <tt>SessionCookieConfig</tt> was acquired.
   *
   * @return the cookie domain set via {@link #setDomain}, or <tt>null</tt> if {@link #setDomain} was never called
   * @see Cookie#getDomain()
   */
  String getDomain();

  /**
   * Sets the path that will be assigned to any session tracking cookies created on behalf of the application represented
   * by the <tt>MockContext</tt> from which this <tt>SessionCookieConfig</tt> was acquired.
   *
   * @param path the cookie path to use
   * @throws IllegalStateException if the <tt>MockContext</tt> from which this <tt>SessionCookieConfig</tt> was
   * acquired has already been initialized
   * @see Cookie#setPath(String)
   */
  void setPath(String path);

  /**
   * Gets the path that will be assigned to any session tracking cookies created on behalf of the application represented
   * by the <tt>MockContext</tt> from which this <tt>SessionCookieConfig</tt> was acquired.
   *
   * <p>
   * By default, the context path of the <tt>MockContext</tt> from which this <tt>SessionCookieConfig</tt> was acquired
   * will be used.
   *
   * @return the cookie path set via {@link #setPath}, or <tt>null</tt> if {@link #setPath} was never called
   * @see Cookie#getPath()
   */
  String getPath();

  /**
   * With the adoption of support for RFC 6265, this method should no longer be used.
   * <p>
   * If called, this method has no effect.
   *
   * @param comment ignore
   * @throws IllegalStateException if the <tt>MockContext</tt> from which this <tt>SessionCookieConfig</tt> was
   * acquired has already been initialized
   * @see Cookie#setComment(String)
   * @see Cookie#getVersion
   * @deprecated This is no longer required with RFC 6265
   */
  @Deprecated(since = "Servlet 6.0", forRemoval = true)
  void setComment(String comment);

  /**
   * With the adoption of support for RFC 6265, this method should no longer be used.
   *
   * @return Always {@code null}
   * @see Cookie#getComment()
   * @deprecated This is no longer required with RFC 6265
   */
  @Deprecated(since = "Servlet 6.0", forRemoval = true)
  String getComment();

  /**
   * Marks or unmarks the session tracking cookies created on behalf of the application represented by the
   * <tt>MockContext</tt> from which this <tt>SessionCookieConfig</tt> was acquired as <i>HttpOnly</i>.
   *
   * <p>
   * A cookie is marked as <tt>HttpOnly</tt> by adding the <tt>HttpOnly</tt> attribute to it. <i>HttpOnly</i> cookies are
   * not supposed to be exposed to client-side scripting code, and may therefore help mitigate certain kinds of cross-site
   * scripting attacks.
   *
   * @param httpOnly true if the session tracking cookies created on behalf of the application represented by the
   * <tt>MockContext</tt> from which this <tt>SessionCookieConfig</tt> was acquired shall be marked as <i>HttpOnly</i>,
   * false otherwise
   * @throws IllegalStateException if the <tt>MockContext</tt> from which this <tt>SessionCookieConfig</tt> was
   * acquired has already been initialized
   * @see Cookie#setHttpOnly(boolean)
   */
  void setHttpOnly(boolean httpOnly);

  /**
   * Checks if the session tracking cookies created on behalf of the application represented by the
   * <tt>MockContext</tt> from which this <tt>SessionCookieConfig</tt> was acquired will be marked as <i>HttpOnly</i>.
   *
   * @return true if the session tracking cookies created on behalf of the application represented by the
   * <tt>MockContext</tt> from which this <tt>SessionCookieConfig</tt> was acquired will be marked as <i>HttpOnly</i>,
   * false otherwise
   * @see Cookie#isHttpOnly()
   */
  boolean isHttpOnly();

  /**
   * Marks or unmarks the session tracking cookies created on behalf of the application represented by the
   * <tt>MockContext</tt> from which this <tt>SessionCookieConfig</tt> was acquired as <i>secure</i>.
   *
   * <p>
   * One use case for marking a session tracking cookie as <tt>secure</tt>, even though the request that initiated the
   * session came over HTTP, is to support a topology where the web container is front-ended by an SSL offloading load
   * balancer. In this case, the traffic between the client and the load balancer will be over HTTPS, whereas the traffic
   * between the load balancer and the web container will be over HTTP.
   *
   * @param secure true if the session tracking cookies created on behalf of the application represented by the
   * <tt>MockContext</tt> from which this <tt>SessionCookieConfig</tt> was acquired shall be marked as <i>secure</i>
   * even if the request that initiated the corresponding session is using plain HTTP instead of HTTPS, and false if they
   * shall be marked as <i>secure</i> only if the request that initiated the corresponding session was also secure
   * @throws IllegalStateException if the <tt>MockContext</tt> from which this <tt>SessionCookieConfig</tt> was
   * acquired has already been initialized
   * @see Cookie#setSecure(boolean)
   * @see MockRequest#isSecure()
   */
  void setSecure(boolean secure);

  /**
   * Checks if the session tracking cookies created on behalf of the application represented by the
   * <tt>MockContext</tt> from which this <tt>SessionCookieConfig</tt> was acquired will be marked as <i>secure</i>
   * even if the request that initiated the corresponding session is using plain HTTP instead of HTTPS.
   *
   * @return true if the session tracking cookies created on behalf of the application represented by the
   * <tt>MockContext</tt> from which this <tt>SessionCookieConfig</tt> was acquired will be marked as <i>secure</i>
   * even if the request that initiated the corresponding session is using plain HTTP instead of HTTPS, and false if they
   * will be marked as <i>secure</i> only if the request that initiated the corresponding session was also secure
   * @see Cookie#getSecure()
   * @see MockRequest#isSecure()
   */
  boolean isSecure();

  /**
   * Sets the lifetime (in seconds) for the session tracking cookies created on behalf of the application represented by
   * the <tt>MockContext</tt> from which this <tt>SessionCookieConfig</tt> was acquired.
   *
   * @param maxAge the lifetime (in seconds) of the session tracking cookies created on behalf of the application
   * represented by the <tt>MockContext</tt> from which this <tt>SessionCookieConfig</tt> was acquired.
   * @throws IllegalStateException if the <tt>MockContext</tt> from which this <tt>SessionCookieConfig</tt> was
   * acquired has already been initialized
   * @see Cookie#setMaxAge
   */
  void setMaxAge(int maxAge);

  /**
   * Gets the lifetime (in seconds) of the session tracking cookies created on behalf of the application represented by
   * the <tt>MockContext</tt> from which this <tt>SessionCookieConfig</tt> was acquired.
   *
   * <p>
   * By default, <tt>-1</tt> is returned.
   *
   * @return the lifetime (in seconds) of the session tracking cookies created on behalf of the application represented by
   * the <tt>MockContext</tt> from which this <tt>SessionCookieConfig</tt> was acquired, or <tt>-1</tt> (the default)
   * @see Cookie#getMaxAge
   */
  int getMaxAge();

  /**
   * Sets the value for the given session cookie attribute. When a value is set via this method, the value returned by the
   * attribute specific getter (if any) must be consistent with the value set via this method.
   *
   * @param name Name of attribute to set, case insensitive
   * @param value Value of attribute
   * @throws IllegalStateException if the associated MockContext has already been initialised
   * @throws IllegalArgumentException If the attribute name is null or contains any characters not permitted for use in
   * Cookie names.
   * @throws NumberFormatException If the attribute is known to be numerical but the provided value cannot be parsed to a
   * number.
   */
  void setAttribute(String name, String value);

  /**
   * Obtain the value for a given session cookie attribute. Values returned from this method must be consistent with the
   * values set and returned by the attribute specific getters and setters in this class.
   *
   * @param name Name of attribute to return, case insensitive
   * @return Value of specified attribute
   */
  String getAttribute(String name);

  /**
   * Obtain the Map (keys are case insensitive) of all attributes and values, including those set via the attribute
   * specific setters, (excluding version) for this <tt>SessionCookieConfig</tt>.
   *
   * @return A read-only Map of attributes to values, excluding version.
   */
  Map<String, String> getAttributes();
}
