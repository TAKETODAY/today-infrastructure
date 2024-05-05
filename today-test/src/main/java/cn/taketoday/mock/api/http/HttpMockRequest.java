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

package cn.taketoday.mock.api.http;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

import cn.taketoday.mock.api.MockApi;
import cn.taketoday.mock.api.MockContext;
import cn.taketoday.mock.api.MockRequest;
import cn.taketoday.mock.api.MockResponse;
import cn.taketoday.mock.api.RequestDispatcher;
import cn.taketoday.mock.api.ServletException;
import cn.taketoday.mock.api.annotation.MultipartConfig;

/**
 * Extends the {@link MockRequest} interface to provide request information for HTTP servlets.
 *
 * <p>
 * The servlet container creates an <code>HttpServletRequest</code> object and passes it as an argument to the servlet's
 * service methods (<code>doGet</code>, <code>doPost</code>, etc).
 *
 * @author Various
 */
public interface HttpMockRequest extends MockRequest {

  /**
   * String identifier for Basic authentication. Value "BASIC"
   */
  String BASIC_AUTH = "BASIC";

  /**
   * String identifier for Form authentication. Value "FORM"
   */
  String FORM_AUTH = "FORM";

  /**
   * String identifier for Client Certificate authentication. Value "CLIENT_CERT"
   */
  String CLIENT_CERT_AUTH = "CLIENT_CERT";

  /**
   * String identifier for Digest authentication. Value "DIGEST"
   */
  String DIGEST_AUTH = "DIGEST";

  /**
   * Returns the name of the authentication scheme used to protect the servlet. All servlet containers support basic, form
   * and client certificate authentication, and may additionally support digest authentication. If the servlet is not
   * authenticated <code>null</code> is returned.
   *
   * @return one of the static members BASIC_AUTH, FORM_AUTH, CLIENT_CERT_AUTH, DIGEST_AUTH (suitable for == comparison)
   * or the container-specific string indicating the authentication scheme, or <code>null</code> if the request was not
   * authenticated.
   */
  String getAuthType();

  /**
   * Returns an array containing all of the <code>Cookie</code> objects the client sent with this request. This method
   * returns <code>null</code> if no cookies were sent.
   *
   * @return an array of all the <code>Cookies</code> included with this request, or <code>null</code> if the request has
   * no cookies
   */
  Cookie[] getCookies();

  /**
   * Returns the value of the specified request header as a <code>long</code> value that represents a <code>Date</code>
   * object. Use this method with headers that contain dates, such as <code>If-Modified-Since</code>.
   *
   * <p>
   * The date is returned as the number of milliseconds since January 1, 1970 GMT. The header name is case insensitive.
   *
   * <p>
   * If the request did not have a header of the specified name, this method returns -1. If the header can't be converted
   * to a date, the method throws an <code>IllegalArgumentException</code>.
   *
   * @param name a <code>String</code> specifying the name of the header
   * @return a <code>long</code> value representing the date specified in the header expressed as the number of
   * milliseconds since January 1, 1970 GMT, or -1 if the named header was not included with the request
   * @throws IllegalArgumentException If the header value can't be converted to a date
   */
  long getDateHeader(String name);

  /**
   * Returns the value of the specified request header as a <code>String</code>. If the request did not include a header
   * of the specified name, this method returns <code>null</code>. If there are multiple headers with the same name, this
   * method returns the first head in the request. The header name is case insensitive. You can use this method with any
   * request header.
   *
   * @param name a <code>String</code> specifying the header name
   * @return a <code>String</code> containing the value of the requested header, or <code>null</code> if the request does
   * not have a header of that name
   */
  String getHeader(String name);

  /**
   * Returns all the values of the specified request header as an <code>Enumeration</code> of <code>String</code> objects.
   *
   * <p>
   * Some headers, such as <code>Accept-Language</code> can be sent by clients as several headers each with a different
   * value rather than sending the header as a comma separated list.
   *
   * <p>
   * If the request did not include any headers of the specified name, this method returns an empty
   * <code>Enumeration</code>. The header name is case insensitive. You can use this method with any request header.
   *
   * @param name a <code>String</code> specifying the header name
   * @return an <code>Enumeration</code> containing the values of the requested header. If the request does not have any
   * headers of that name return an empty enumeration. If the container does not allow access to header information,
   * return null
   */
  Enumeration<String> getHeaders(String name);

  /**
   * Returns an enumeration of all the header names this request contains. If the request has no headers, this method
   * returns an empty enumeration.
   *
   * <p>
   * Some servlet containers do not allow servlets to access headers using this method, in which case this method returns
   * <code>null</code>
   *
   * @return an enumeration of all the header names sent with this request; if the request has no headers, an empty
   * enumeration; if the servlet container does not allow servlets to use this method, <code>null</code>
   */
  Enumeration<String> getHeaderNames();

  /**
   * Returns the value of the specified request header as an <code>int</code>. If the request does not have a header of
   * the specified name, this method returns -1. If the header cannot be converted to an integer, this method throws a
   * <code>NumberFormatException</code>.
   *
   * <p>
   * The header name is case insensitive.
   *
   * @param name a <code>String</code> specifying the name of a request header
   * @return an integer expressing the value of the request header or -1 if the request doesn't have a header of this name
   * @throws NumberFormatException If the header value can't be converted to an <code>int</code>
   */
  int getIntHeader(String name);

  /**
   * Return the HttpServletMapping of the request.
   * <p>
   * The mapping returned depends on the current {@link cn.taketoday.mock.api.DispatcherType} as obtained from
   * {@link #getDispatcherType()}:
   * <dl>
   * <dt>{@link cn.taketoday.mock.api.DispatcherType#REQUEST}, {@link cn.taketoday.mock.api.DispatcherType#ASYNC},
   * {@link cn.taketoday.mock.api.DispatcherType#ERROR}</dt>
   * <dd>Return the mapping for the target of the dispatch i.e. the mapping for the current
   * {@link MockApi}.</dd>
   *
   * <dt>{@link cn.taketoday.mock.api.DispatcherType#INCLUDE}</dt>
   * <dd>Return the mapping as prior to the current dispatch. i.e the mapping returned is unchanged by a call to</dd>
   * {@link RequestDispatcher#include(MockRequest, MockResponse)}.
   *
   * <dt>{@link cn.taketoday.mock.api.DispatcherType#FORWARD}</dt>
   * <dd>Return the mapping for the target of the dispatch i.e. the mapping for the current
   * {@link MockApi}, unless the {@link RequestDispatcher} was obtained via
   * {@link MockContext#getNamedDispatcher(String)}, in which case return the mapping as prior to the
   * current dispatch. i.e the mapping returned is changed during a call to
   * {@link RequestDispatcher#forward(MockRequest, MockResponse)} only if the dispatcher is not a
   * named dispatcher.</dd>
   * </dl>
   * </p>
   * <p>
   * For example:
   * <ul>
   * <li>For a sequence Servlet1&nbsp;--include--&gt;&nbsp;Servlet2&nbsp;--include--&gt;&nbsp;Servlet3, a call to this
   * method in Servlet3 will return the mapping for Servlet1.</li>
   * <li>For a sequence Servlet1&nbsp;--async--&gt;&nbsp;Servlet2&nbsp;--named-forward--&gt;&nbsp;Servlet3, a call to this
   * method in Servlet3 will return the mapping for Servlet2.</li>
   * </ul>
   * </p>
   * <p>
   * The returned object is immutable. Servlet 4.0 compliant implementations must override this method.
   * </p>
   *
   * @return An instance of {@code HttpServletMapping} describing the manner in which the current request was invoked.
   * @implSpec The default implementation returns a {@code
   * HttpServletMapping} that returns the empty string for the match value, pattern and servlet name and {@code null} for
   * the match type.
   * @since Servlet 4.0
   */
  default HttpServletMapping getHttpServletMapping() {
    return new HttpServletMapping() {
      @Override
      public String getMatchValue() {
        return "";
      }

      @Override
      public String getPattern() {
        return "";
      }

      @Override
      public String getServletName() {
        return "";
      }

      @Override
      public MappingMatch getMappingMatch() {
        return null;
      }

      @Override
      public String toString() {
        return "MappingImpl{" + "matchValue=" + getMatchValue() + ", pattern=" + getPattern() + ", servletName="
                + getServletName() + ", mappingMatch=" + getMappingMatch() + "} HttpServletRequest {"
                + HttpMockRequest.this + '}';
      }

    };
  }

  /**
   * Returns the name of the HTTP method with which this request was made, for example, GET, POST, or PUT.
   *
   * @return a <code>String</code> specifying the name of the method with which this request was made
   */
  String getMethod();

  /**
   * Returns any extra path information associated with the URL the client sent when it made this request. The extra path
   * information follows the servlet path but precedes the query string and will start with a "/" character.
   *
   * <p>
   * This method returns <code>null</code> if there was no extra path information.
   *
   * @return a <code>String</code> specifying extra path information that comes after the servlet path but before the
   * query string in the request URL; or <code>null</code> if the URL does not have any extra path information. The path
   * will be canonicalized as per section 3.5 of the specification. This method will not return any encoded characters
   * unless the container is configured specifically to allow them.
   * @throws IllegalArgumentException In standard configuration, this method will never throw. However, a container may be
   * configured to not reject some suspicious sequences identified by 3.5.2, furthermore the container may be configured
   * to allow such paths to only be accessed via safer methods like {@link #getRequestURI()} and to throw
   * IllegalArgumentException if this method is called for such suspicious paths.
   */
  String getPathInfo();

  /**
   * Returns any extra path information after the servlet name but before the query string, and translates it to a real
   * path.
   *
   * <p>
   * If the URL does not have any extra path information, this method returns <code>null</code> or the servlet container
   * cannot translate the virtual path to a real path for any reason (such as when the web application is executed from an
   * archive).
   *
   * The web container does not decode this string.
   *
   * @return a <code>String</code> specifying the real path, or <code>null</code> if the URL does not have any extra path
   * information
   */
  String getPathTranslated();

  /**
   * Instantiates a new instance of {@link PushBuilder} for issuing server push responses from the current request. This
   * method returns null if the current connection does not support server push, or server push has been disabled by the
   * client via a {@code SETTINGS_ENABLE_PUSH} settings frame value of {@code 0} (zero).
   *
   * @return a {@link PushBuilder} for issuing server push responses from the current request, or null if push is not
   * supported
   * @implSpec The default implementation returns null.
   * @since Servlet 4.0
   */
  default PushBuilder newPushBuilder() {
    return null;
  }

  /**
   * Returns the query string that is contained in the request URL after the path. This method returns <code>null</code>
   * if the URL does not have a query string.
   *
   * @return a <code>String</code> containing the query string or <code>null</code> if the URL contains no query string.
   * The value is not decoded by the container.
   */
  String getQueryString();

  /**
   * Returns the login of the user making this request, if the user has been authenticated, or <code>null</code> if the
   * user has not been authenticated. Whether the user name is sent with each subsequent request depends on the browser
   * and type of authentication.
   *
   * @return a <code>String</code> specifying the login of the user making this request, or <code>null</code> if the user
   * login is not known
   */
  String getRemoteUser();

  /**
   * Returns a boolean indicating whether the authenticated user is included in the specified logical "role". Roles and
   * role membership can be defined using deployment descriptors. If the user has not been authenticated, the method
   * returns <code>false</code>.
   *
   * <p>
   * The role name "*" should never be used as an argument in calling <code>isUserInRole</code>. Any call to
   * <code>isUserInRole</code> with "*" must return false. If the role-name of the security-role to be tested is "**", and
   * the application has NOT declared an application security-role with role-name "**", <code>isUserInRole</code> must
   * only return true if the user has been authenticated; that is, only when {@link #getRemoteUser} and
   * {@link #getUserPrincipal} would both return a non-null value. Otherwise, the container must check the user for
   * membership in the application role.
   *
   * @param role a <code>String</code> specifying the name of the role
   * @return a <code>boolean</code> indicating whether the user making this request belongs to a given role;
   * <code>false</code> if the user has not been authenticated
   */
  boolean isUserInRole(String role);

  /**
   * Returns a <code>java.security.Principal</code> object containing the name of the current authenticated user. If the
   * user has not been authenticated, the method returns <code>null</code>.
   *
   * @return a <code>java.security.Principal</code> containing the name of the user making this request; <code>null</code>
   * if the user has not been authenticated
   */
  java.security.Principal getUserPrincipal();

  /**
   * Returns the session ID specified by the client. This may not be the same as the ID of the current valid session for
   * this request. If the client did not specify a session ID, this method returns <code>null</code>.
   *
   * @return a <code>String</code> specifying the session ID, or <code>null</code> if the request did not specify a
   * session ID
   * @see #isRequestedSessionIdValid
   */
  String getRequestedSessionId();

  /**
   * Returns the part of this request's URL from the protocol name up to the query string in the first line of the HTTP
   * request. The web container does not decode this String. For example:
   *
   * <table summary="Examples of Returned Values">
   * <tr align=left>
   * <th>First line of HTTP request</th>
   * <th>Returned Value</th>
   * <tr>
   * <td>POST /some/path.html HTTP/1.1
   * <td>
   * <td>/some/path.html
   * <tr>
   * <td>GET http://foo.bar/a.html HTTP/1.0
   * <td>
   * <td>/a.html
   * <tr>
   * <td>HEAD /xyz?a=b HTTP/1.1
   * <td>
   * <td>/xyz
   * </table>
   *
   * @return a <code>String</code> containing the part of the URL from the protocol name up to the query string
   */
  String getRequestURI();

  /**
   * Reconstructs the URL the client used to make the request. The returned URL contains a protocol, server name, port
   * number, and server path, but it does not include query string parameters.
   *
   * <p>
   * If this request has been forwarded using {@link RequestDispatcher#forward}, the server path in the
   * reconstructed URL must reflect the path used to obtain the RequestDispatcher, and not the server path specified by
   * the client.
   *
   * <p>
   * Because this method returns a <code>StringBuffer</code>, not a string, you can modify the URL easily, for example, to
   * append query parameters.
   *
   * <p>
   * This method is useful for creating redirect messages and for reporting errors.
   *
   * @return a <code>StringBuffer</code> object containing the reconstructed URL
   */
  StringBuffer getRequestURL();

  /**
   * Returns the current <code>HttpSession</code> associated with this request or, if there is no current session and
   * <code>create</code> is true, returns a new session.
   *
   * <p>
   * If <code>create</code> is <code>false</code> and the request has no valid <code>HttpSession</code>, this method
   * returns <code>null</code>.
   *
   * <p>
   * To make sure the session is properly maintained, you must call this method before the response is committed. If the
   * container is using cookies to maintain session integrity and is asked to create a new session when the response is
   * committed, an IllegalStateException is thrown.
   *
   * @param create <code>true</code> to create a new session for this request if necessary; <code>false</code> to return
   * <code>null</code> if there's no current session
   * @return the <code>HttpSession</code> associated with this request or <code>null</code> if <code>create</code> is
   * <code>false</code> and the request has no valid session
   * @see #getSession()
   */
  HttpSession getSession(boolean create);

  /**
   * Returns the current session associated with this request, or if the request does not have a session, creates one.
   *
   * @return the <code>HttpSession</code> associated with this request
   * @see #getSession(boolean)
   */
  HttpSession getSession();

  /**
   * Change the session id of the current session associated with this request and return the new session id.
   *
   * @return the new session id
   * @throws IllegalStateException if there is no session associated with the request
   * @since Servlet 3.1
   */
  String changeSessionId();

  /**
   * Checks whether the requested session ID is still valid.
   *
   * <p>
   * If the client did not specify any session ID, this method returns <code>false</code>.
   *
   * @return <code>true</code> if this request has an id for a valid session in the current session context;
   * <code>false</code> otherwise
   * @see #getRequestedSessionId
   * @see #getSession
   */
  boolean isRequestedSessionIdValid();

  /**
   * <p>
   * Checks whether the requested session ID was conveyed to the server as an HTTP cookie.
   * </p>
   *
   * @return <code>true</code> if the session ID was conveyed to the server an an HTTP cookie; otherwise,
   * <code>false</code>
   * @see #getSession
   */
  boolean isRequestedSessionIdFromCookie();

  /**
   * <p>
   * Checks whether the requested session ID was conveyed to the server as part of the request URL.
   * </p>
   *
   * @return <code>true</code> if the session ID was conveyed to the server as part of a URL; otherwise,
   * <code>false</code>
   * @see #getSession
   */
  boolean isRequestedSessionIdFromURL();

  /**
   * Use the container login mechanism configured for the <code>MockContext</code> to authenticate the user making this
   * request.
   *
   * <p>
   * This method may modify and commit the argument <code>HttpServletResponse</code>.
   *
   * @param response The <code>HttpServletResponse</code> associated with this <code>HttpServletRequest</code>
   * @return <code>true</code> when non-null values were or have been established as the values returned by
   * <code>getUserPrincipal</code>, <code>getRemoteUser</code>, and <code>getAuthType</code>. Return <code>false</code> if
   * authentication is incomplete and the underlying login mechanism has committed, in the response, the message (e.g.,
   * challenge) and HTTP status code to be returned to the user.
   * @throws IOException if an input or output error occurred while reading from this request or writing to the given
   * response
   * @throws IllegalStateException if the login mechanism attempted to modify the response and it was already committed
   * @throws ServletException if the authentication failed and the caller is responsible for handling the error (i.e., the
   * underlying login mechanism did NOT establish the message and HTTP status code to be returned to the user)
   * @since Servlet 3.0
   */
  boolean authenticate(HttpMockResponse response) throws IOException, ServletException;

  /**
   * Validate the provided username and password in the password validation realm used by the web container login
   * mechanism configured for the <code>MockContext</code>.
   *
   * <p>
   * This method returns without throwing a <code>ServletException</code> when the login mechanism configured for the
   * <code>MockContext</code> supports username password validation, and when, at the time of the call to login, the
   * identity of the caller of the request had not been established (i.e, all of <code>getUserPrincipal</code>,
   * <code>getRemoteUser</code>, and <code>getAuthType</code> return null), and when validation of the provided
   * credentials is successful. Otherwise, this method throws a <code>ServletException</code> as described below.
   *
   * <p>
   * When this method returns without throwing an exception, it must have established non-null values as the values
   * returned by <code>getUserPrincipal</code>, <code>getRemoteUser</code>, and <code>getAuthType</code>.
   *
   * @param username The <code>String</code> value corresponding to the login identifier of the user.
   * @param password The password <code>String</code> corresponding to the identified user.
   * @throws ServletException if the configured login mechanism does not support username password authentication, or
   * if a non-null caller identity had already been established (prior to the call to login), or if validation of the
   * provided username and password fails.
   * @since Servlet 3.0
   */
  void login(String username, String password) throws ServletException;

  /**
   * Establish <code>null</code> as the value returned when <code>getUserPrincipal</code>, <code>getRemoteUser</code>, and
   * <code>getAuthType</code> is called on the request.
   *
   * @throws ServletException if logout fails
   * @since Servlet 3.0
   */
  void logout() throws ServletException;

  /**
   * Gets all the {@link Part} components of this request, provided that it is of type <code>multipart/form-data</code>.
   *
   * <p>
   * If this request is of type <code>multipart/form-data</code>, but does not contain any <code>Part</code> components,
   * the returned <code>Collection</code> will be empty.
   *
   * <p>
   * Any changes to the returned <code>Collection</code> must not affect this <code>HttpServletRequest</code>.
   *
   * @return a (possibly empty) <code>Collection</code> of the <code>Part</code> components of this request
   * @throws IOException if an I/O error occurred during the retrieval of the {@link Part} components of this request
   * @throws ServletException if this request is not of type <code>multipart/form-data</code>
   * @throws IllegalStateException if the request body is larger than <code>maxRequestSize</code>, or any
   * <code>Part</code> in the request is larger than <code>maxFileSize</code>, or there is no
   * <code>@MultipartConfig</code> or <code>multipart-config</code> in deployment descriptors
   * @see MultipartConfig#maxFileSize
   * @see MultipartConfig#maxRequestSize
   * @since Servlet 3.0
   */
  Collection<Part> getParts() throws IOException, ServletException;

  /**
   * Gets the {@link Part} with the given name.
   *
   * @param name the name of the requested <code>Part</code>
   * @return The <code>Part</code> with the given name, or <code>null</code> if this request is of type
   * <code>multipart/form-data</code>, but does not contain the requested <code>Part</code>
   * @throws IOException if an I/O error occurred during the retrieval of the requested <code>Part</code>
   * @throws ServletException if this request is not of type <code>multipart/form-data</code>
   * @throws IllegalStateException if the request body is larger than <code>maxRequestSize</code>, or any
   * <code>Part</code> in the request is larger than <code>maxFileSize</code>, or there is no
   * <code>@MultipartConfig</code> or <code>multipart-config</code> in deployment descriptors
   * @see MultipartConfig#maxFileSize
   * @see MultipartConfig#maxRequestSize
   * @since Servlet 3.0
   */
  Part getPart(String name) throws IOException, ServletException;

  /**
   * Creates an instance of <code>HttpUpgradeHandler</code> for a given class and uses it for the http protocol upgrade
   * processing.
   *
   * @param <T> The {@code Class}, which extends {@link HttpUpgradeHandler}, of the {@code handlerClass}.
   * @param handlerClass The <code>HttpUpgradeHandler</code> class used for the upgrade.
   * @return an instance of the <code>HttpUpgradeHandler</code>
   * @throws IOException if an I/O error occurred during the upgrade
   * @throws ServletException if the given <code>handlerClass</code> fails to be instantiated
   * @see HttpUpgradeHandler
   * @see WebConnection
   * @since Servlet 3.1
   */
  <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException;

  /**
   * Get the request trailer fields.
   *
   * <p>
   * The returned map is not backed by the {@code HttpServletRequest} object, so changes in the returned map are not
   * reflected in the {@code HttpServletRequest} object, and vice-versa.
   * </p>
   *
   * <p>
   * {@link #isTrailerFieldsReady()} should be called first to determine if it is safe to call this method without causing
   * an exception.
   * </p>
   *
   * @return A map of trailer fields in which all the keys are in lowercase, regardless of the case they had at the
   * protocol level. If there are no trailer fields, yet {@link #isTrailerFieldsReady} is returning true, the empty map is
   * returned.
   * @throws IllegalStateException if {@link #isTrailerFieldsReady()} is false
   * @implSpec The default implementation returns an empty map.
   * @since Servlet 4.0
   */
  default Map<String, String> getTrailerFields() {
    return Collections.emptyMap();
  }

  /**
   * Return a boolean indicating whether trailer fields are ready to read using {@link #getTrailerFields}.
   *
   * This methods returns true immediately if it is known that there is no trailer in the request, for instance, the
   * underlying protocol (such as HTTP 1.0) does not supports the trailer fields, or the request is not in chunked
   * encoding in HTTP 1.1. And the method also returns true if both of the following conditions are satisfied:
   * <ol type="a">
   * <li>the application has read all the request data and an EOF indication has been returned from the {@link #getReader}
   * or {@link #getInputStream}.
   * <li>all the trailer fields sent by the client have been received. Note that it is possible that the client has sent
   * no trailer fields.
   * </ol>
   *
   * @return a boolean whether trailer fields are ready to read
   * @implSpec The default implementation returns false.
   * @since Servlet 4.0
   */
  default boolean isTrailerFieldsReady() {
    return true;
  }
}
