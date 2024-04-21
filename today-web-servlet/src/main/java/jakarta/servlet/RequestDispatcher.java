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

package jakarta.servlet;

import java.io.IOException;

/**
 * Defines an object that receives requests from the client and sends them to any resource (such as a servlet, HTML
 * file, or JSP file) on the server. The servlet container creates the <code>RequestDispatcher</code> object, which is
 * used as a wrapper around a server resource located at a particular path or given by a particular name.
 *
 * <p>
 * This interface is intended to wrap servlets, but a servlet container can create <code>RequestDispatcher</code>
 * objects to wrap any type of resource.
 *
 * @author Various
 * @see ServletContext#getRequestDispatcher(String)
 * @see ServletContext#getNamedDispatcher(String)
 * @see ServletRequest#getRequestDispatcher(String)
 */
public interface RequestDispatcher {

  /**
   * The name of the request attribute under which the original request URI is made available to the target of a
   * {@link #forward(ServletRequest, ServletResponse) forward}
   *
   * @since Servlet 3.0
   */
  static final String FORWARD_REQUEST_URI = "jakarta.servlet.forward.request_uri";

  /**
   * The name of the request attribute under which the original context path is made available to the target of a
   * {@link #forward(ServletRequest, ServletResponse) forward}
   *
   * @since Servlet 3.0
   */
  static final String FORWARD_CONTEXT_PATH = "jakarta.servlet.forward.context_path";

  /**
   * The name of the request attribute under which the original {@link jakarta.servlet.http.HttpServletMapping} is made
   * available to the target of a {@link #forward(ServletRequest, ServletResponse) forward}
   *
   * @since Servlet 4.0
   */
  static final String FORWARD_MAPPING = "jakarta.servlet.forward.mapping";

  /**
   * The name of the request attribute under which the original path info is made available to the target of a
   * {@link #forward(ServletRequest, ServletResponse) forward}
   *
   * @since Servlet 3.0
   */
  static final String FORWARD_PATH_INFO = "jakarta.servlet.forward.path_info";

  /**
   * The name of the request attribute under which the original servlet path is made available to the target of a
   * {@link #forward(ServletRequest, ServletResponse) forward}
   *
   * @since Servlet 3.0
   */
  static final String FORWARD_SERVLET_PATH = "jakarta.servlet.forward.servlet_path";

  /**
   * The name of the request attribute under which the original query string is made available to the target of a
   * {@link #forward(ServletRequest, ServletResponse) forward}
   *
   * @since Servlet 3.0
   */
  static final String FORWARD_QUERY_STRING = "jakarta.servlet.forward.query_string";

  /**
   * The name of the request attribute under which the request URI of the target of an
   * {@link #include(ServletRequest, ServletResponse) include} is stored
   *
   * @since Servlet 3.0
   */
  static final String INCLUDE_REQUEST_URI = "jakarta.servlet.include.request_uri";

  /**
   * The name of the request attribute under which the context path of the target of an
   * {@link #include(ServletRequest, ServletResponse) include} is stored
   *
   * @since Servlet 3.0
   */
  static final String INCLUDE_CONTEXT_PATH = "jakarta.servlet.include.context_path";

  /**
   * The name of the request attribute under which the path info of the target of an
   * {@link #include(ServletRequest, ServletResponse) include} is stored
   *
   * @since Servlet 3.0
   */
  static final String INCLUDE_PATH_INFO = "jakarta.servlet.include.path_info";

  /**
   * The name of the request attribute under which the {@link jakarta.servlet.http.HttpServletMapping} of the target of an
   * {@link #include(ServletRequest, ServletResponse) include} is stored
   *
   * @since Servlet 4.0
   */
  static final String INCLUDE_MAPPING = "jakarta.servlet.include.mapping";

  /**
   * The name of the request attribute under which the servlet path of the target of an
   * {@link #include(ServletRequest, ServletResponse) include} is stored
   *
   * @since Servlet 3.0
   */
  static final String INCLUDE_SERVLET_PATH = "jakarta.servlet.include.servlet_path";

  /**
   * The name of the request attribute under which the query string of the target of an
   * {@link #include(ServletRequest, ServletResponse) include} is stored
   *
   * @since Servlet 3.0
   */
  static final String INCLUDE_QUERY_STRING = "jakarta.servlet.include.query_string";

  /**
   * The name of the request attribute under which the exception object is propagated during an error dispatch
   *
   * @since Servlet 3.0
   */
  public static final String ERROR_EXCEPTION = "jakarta.servlet.error.exception";

  /**
   * The name of the request attribute under which the type of the exception object is propagated during an error dispatch
   *
   * @since Servlet 3.0
   */
  public static final String ERROR_EXCEPTION_TYPE = "jakarta.servlet.error.exception_type";

  /**
   * The name of the request attribute under which the exception message is propagated during an error dispatch
   *
   * @since Servlet 3.0
   */
  public static final String ERROR_MESSAGE = "jakarta.servlet.error.message";

  /**
   * The name of the request attribute under which the request URI whose processing caused the error is propagated during
   * an error dispatch
   *
   * @since Servlet 3.0
   */
  public static final String ERROR_REQUEST_URI = "jakarta.servlet.error.request_uri";

  /**
   * The name of the request attribute under which the name of the servlet in which the error occurred is propagated
   * during an error dispatch
   *
   * @since Servlet 3.0
   */
  public static final String ERROR_SERVLET_NAME = "jakarta.servlet.error.servlet_name";

  /**
   * The name of the request attribute under which the response status is propagated during an error dispatch
   *
   * @since Servlet 3.0
   */
  public static final String ERROR_STATUS_CODE = "jakarta.servlet.error.status_code";

  /**
   * Forwards a request from a servlet to another resource (servlet, JSP file, or HTML file) on the server. This method
   * allows one servlet to do preliminary processing of a request and another resource to generate the response.
   *
   * <p>
   * For a <code>RequestDispatcher</code> obtained via <code>getRequestDispatcher()</code>, the
   * <code>ServletRequest</code> object has its path elements and parameters adjusted to match the path of the target
   * resource.
   *
   * <p>
   * <code>forward</code> should be called before the response has been committed to the client (before response body
   * output has been flushed). If the response already has been committed, this method throws an
   * <code>IllegalStateException</code>. Uncommitted output in the response buffer is automatically cleared before the
   * forward.
   *
   * <p>
   * The request and response parameters must be either the same objects as were passed to the calling servlet's service
   * method or be subclasses of the {@link ServletRequestWrapper} or {@link ServletResponseWrapper} classes that wrap
   * them.
   *
   * <p>
   * This method sets the dispatcher type of the given request to <code>DispatcherType.FORWARD</code>.
   *
   * @param request a {@link ServletRequest} object that represents the request the client makes of the servlet
   * @param response a {@link ServletResponse} object that represents the response the servlet returns to the client
   * @throws ServletException if the target resource throws this exception
   * @throws IOException if the target resource throws this exception
   * @throws IllegalStateException if the response was already committed
   * @see ServletRequest#getDispatcherType
   */
  public void forward(ServletRequest request, ServletResponse response) throws ServletException, IOException;

  /**
   * Includes the content of a resource (servlet, JSP page, HTML file) in the response. In essence, this method enables
   * programmatic server-side includes.
   *
   * <p>
   * The {@link ServletResponse} object has its path elements and parameters remain unchanged from the caller's. The
   * included servlet cannot change the response status code or set headers; any attempt to make a change is ignored.
   *
   * <p>
   * The request and response parameters must be either the same objects as were passed to the calling servlet's service
   * method or be subclasses of the {@link ServletRequestWrapper} or {@link ServletResponseWrapper} classes that wrap
   * them.
   *
   * <p>
   * This method sets the dispatcher type of the given request to <code>DispatcherType.INCLUDE</code>.
   *
   * @param request a {@link ServletRequest} object that contains the client's request
   * @param response a {@link ServletResponse} object that contains the servlet's response
   * @throws ServletException if the included resource throws this exception
   * @throws IOException if the included resource throws this exception
   * @see ServletRequest#getDispatcherType
   */
  public void include(ServletRequest request, ServletResponse response) throws ServletException, IOException;
}
