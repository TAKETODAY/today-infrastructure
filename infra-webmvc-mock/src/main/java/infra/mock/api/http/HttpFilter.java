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

package infra.mock.api.http;

import java.io.IOException;

import infra.mock.api.FilterChain;
import infra.mock.api.GenericFilter;
import infra.mock.api.MockException;
import infra.mock.api.MockRequest;
import infra.mock.api.MockResponse;

/**
 * <p>
 * Provides an abstract class to be subclassed to create an HTTP filter suitable for a Web site. A subclass of
 * <code>HttpFilter</code> should override
 * {@link #doFilter(HttpMockRequest, HttpMockResponse, FilterChain) }.
 * </p>
 *
 * <p>
 * Filters typically run on multithreaded servers, so be aware that a filter must handle concurrent requests and be
 * careful to synchronize access to shared resources. Shared resources include in-memory data such as instance or class
 * variables and external objects such as files, database connections, and network connections. See the
 * <a href="https://docs.oracle.com/javase/tutorial/essential/concurrency/"> Java Tutorial on Multithreaded
 * Programming</a> for more information on handling multiple threads in a Java program.
 *
 * @author Various
 */
public abstract class HttpFilter extends GenericFilter {

  private static final long serialVersionUID = 7478463438252262094L;

  /**
   * <p>
   * Does nothing, because this is an abstract class.
   * </p>
   */
  public HttpFilter() {
  }

  /**
   * <p>
   * The <code>doFilter</code> method of the Filter is called by the container each time a request/response pair is passed
   * through the chain due to a client request for a resource at the end of the chain. The FilterChain passed in to this
   * method allows the Filter to pass on the request and response to the next entity in the chain. There's no need to
   * override this method.
   * </p>
   *
   * <p>
   * The default implementation inspects the incoming {@code req} and {@code res} objects to determine if they are
   * instances of {@link HttpMockRequest} and {@link HttpMockResponse}, respectively. If not, a
   * {@link MockException} is thrown. Otherwise, the protected
   * {@link #doFilter(HttpMockRequest, HttpMockResponse, FilterChain)}
   * method is called.
   * </p>
   *
   * @param req a {@link MockRequest} object that contains the request the client has made of the filter
   * @param res a {@link MockResponse} object that contains the response the filter sends to the client
   * @param chain the <code>FilterChain</code> for invoking the next filter or the resource
   * @throws IOException if an input or output error is detected when the filter handles the request
   * @throws MockException if the request for the could not be handled or either parameter is not an instance of the
   * respective {@link HttpMockRequest} or {@link HttpMockResponse}.
   */
  @Override
  public void doFilter(MockRequest req, MockResponse res, FilterChain chain)
          throws IOException, MockException {
    if (!(req instanceof HttpMockRequest && res instanceof HttpMockResponse)) {
      throw new MockException("non-HTTP request or response");
    }

    this.doFilter((HttpMockRequest) req, (HttpMockResponse) res, chain);
  }

  /**
   * <p>
   * The <code>doFilter</code> method of the Filter is called by the container each time a request/response pair is passed
   * through the chain due to a client request for a resource at the end of the chain. The FilterChain passed in to this
   * method allows the Filter to pass on the request and response to the next entity in the chain.
   * </p>
   *
   * <p>
   * The default implementation simply calls {@link FilterChain#doFilter}
   * </p>
   *
   * @param req a {@link HttpMockRequest} object that contains the request the client has made of the filter
   * @param res a {@link HttpMockResponse} object that contains the response the filter sends to the client
   * @param chain the <code>FilterChain</code> for invoking the next filter or the resource
   * @throws IOException if an input or output error is detected when the filter handles the request
   * @throws MockException if the request for the could not be handled
   */
  protected void doFilter(HttpMockRequest req, HttpMockResponse res, FilterChain chain)
          throws IOException, MockException {
    chain.doFilter(req, res);
  }

}
