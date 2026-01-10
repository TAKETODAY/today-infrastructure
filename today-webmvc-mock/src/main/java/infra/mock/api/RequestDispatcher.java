/*
 * Copyright (c) 1997, 2023 Oracle and/or its affiliates and others.
 * All rights reserved.
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.mock.api;

import java.io.IOException;

import infra.mock.api.http.HttpMockMapping;

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
 * @see MockContext#getRequestDispatcher(String)
 * @see MockContext#getNamedDispatcher(String)
 * @see MockRequest#getRequestDispatcher(String)
 */
public interface RequestDispatcher {

  /**
   * The name of the request attribute under which the original request URI is made available to the target of a
   * {@link #forward(MockRequest, MockResponse) forward}
   */
  String FORWARD_REQUEST_URI = "infra.mock.api.forward.request_uri";

  /**
   * The name of the request attribute under which the original context path is made available to the target of a
   * {@link #forward(MockRequest, MockResponse) forward}
   */
  String FORWARD_CONTEXT_PATH = "infra.mock.api.forward.context_path";

  /**
   * The name of the request attribute under which the original {@link HttpMockMapping} is made
   * available to the target of a {@link #forward(MockRequest, MockResponse) forward}
   */
  String FORWARD_MAPPING = "infra.mock.api.forward.mapping";

  /**
   * The name of the request attribute under which the original path info is made available to the target of a
   * {@link #forward(MockRequest, MockResponse) forward}
   */
  String FORWARD_PATH_INFO = "infra.mock.api.forward.path_info";

  /**
   * The name of the request attribute under which the original query string is made available to the target of a
   * {@link #forward(MockRequest, MockResponse) forward}
   */
  String FORWARD_QUERY_STRING = "infra.mock.api.forward.query_string";

  /**
   * The name of the request attribute under which the request URI of the target of an
   * {@link #include(MockRequest, MockResponse) include} is stored
   */
  String INCLUDE_REQUEST_URI = "infra.mock.api.include.request_uri";

  /**
   * The name of the request attribute under which the context path of the target of an
   * {@link #include(MockRequest, MockResponse) include} is stored
   */
  String INCLUDE_CONTEXT_PATH = "infra.mock.api.include.context_path";

  /**
   * The name of the request attribute under which the path info of the target of an
   * {@link #include(MockRequest, MockResponse) include} is stored
   */
  String INCLUDE_PATH_INFO = "infra.mock.api.include.path_info";

  /**
   * The name of the request attribute under which the {@link HttpMockMapping} of the target of an
   * {@link #include(MockRequest, MockResponse) include} is stored
   */
  String INCLUDE_MAPPING = "infra.mock.api.include.mapping";

  /**
   * The name of the request attribute under which the query string of the target of an
   * {@link #include(MockRequest, MockResponse) include} is stored
   */
  String INCLUDE_QUERY_STRING = "infra.mock.api.include.query_string";

  /**
   * The name of the request attribute under which the exception object is propagated during an error dispatch
   */
  String ERROR_EXCEPTION = "infra.mock.api.error.exception";

  /**
   * The name of the request attribute under which the type of the exception object is propagated during an error dispatch
   */
  String ERROR_EXCEPTION_TYPE = "infra.mock.api.error.exception_type";

  /**
   * The name of the request attribute under which the exception message is propagated during an error dispatch
   */
  String ERROR_MESSAGE = "infra.mock.api.error.message";

  /**
   * The name of the request attribute under which the request URI whose processing caused the error is propagated during
   * an error dispatch
   */
  String ERROR_REQUEST_URI = "infra.mock.api.error.request_uri";

  /**
   * The name of the request attribute under which the response status is propagated during an error dispatch
   */
  String ERROR_STATUS_CODE = "infra.mock.api.error.status_code";

  /**
   * Forwards a request from a servlet to another resource (servlet, JSP file, or HTML file) on the server. This method
   * allows one servlet to do preliminary processing of a request and another resource to generate the response.
   *
   * <p>
   * For a <code>RequestDispatcher</code> obtained via <code>getRequestDispatcher()</code>, the
   * <code>MockRequest</code> object has its path elements and parameters adjusted to match the path of the target
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
   * method or be subclasses of the {@link MockRequestWrapper} or {@link MockResponseWrapper} classes that wrap
   * them.
   *
   * <p>
   * This method sets the dispatcher type of the given request to <code>DispatcherType.FORWARD</code>.
   *
   * @param request a {@link MockRequest} object that represents the request the client makes of the servlet
   * @param response a {@link MockResponse} object that represents the response the servlet returns to the client
   * @throws MockException if the target resource throws this exception
   * @throws IOException if the target resource throws this exception
   * @throws IllegalStateException if the response was already committed
   * @see MockRequest#getDispatcherType
   */
  void forward(MockRequest request, MockResponse response) throws MockException, IOException;

  /**
   * Includes the content of a resource (servlet, JSP page, HTML file) in the response. In essence, this method enables
   * programmatic server-side includes.
   *
   * <p>
   * The {@link MockResponse} object has its path elements and parameters remain unchanged from the caller's. The
   * included servlet cannot change the response status code or set headers; any attempt to make a change is ignored.
   *
   * <p>
   * The request and response parameters must be either the same objects as were passed to the calling servlet's service
   * method or be subclasses of the {@link MockRequestWrapper} or {@link MockResponseWrapper} classes that wrap
   * them.
   *
   * <p>
   * This method sets the dispatcher type of the given request to <code>DispatcherType.INCLUDE</code>.
   *
   * @param request a {@link MockRequest} object that contains the client's request
   * @param response a {@link MockResponse} object that contains the servlet's response
   * @throws MockException if the included resource throws this exception
   * @throws IOException if the included resource throws this exception
   * @see MockRequest#getDispatcherType
   */
  void include(MockRequest request, MockResponse response) throws MockException, IOException;
}
