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

import cn.taketoday.mock.api.FilterChain;
import cn.taketoday.mock.api.GenericFilter;
import cn.taketoday.mock.api.ServletException;
import cn.taketoday.mock.api.MockRequest;
import cn.taketoday.mock.api.MockResponse;

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
 * @since Servlet 4.0
 */
public abstract class HttpFilter extends GenericFilter {

  private static final long serialVersionUID = 7478463438252262094L;

  /**
   * <p>
   * Does nothing, because this is an abstract class.
   * </p>
   *
   * @since Servlet 4.0
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
   * {@link ServletException} is thrown. Otherwise, the protected
   * {@link #doFilter(HttpMockRequest, HttpMockResponse, FilterChain)}
   * method is called.
   * </p>
   *
   * @param req a {@link MockRequest} object that contains the request the client has made of the filter
   * @param res a {@link MockResponse} object that contains the response the filter sends to the client
   * @param chain the <code>FilterChain</code> for invoking the next filter or the resource
   * @throws IOException if an input or output error is detected when the filter handles the request
   * @throws ServletException if the request for the could not be handled or either parameter is not an instance of the
   * respective {@link HttpMockRequest} or {@link HttpMockResponse}.
   * @since Servlet 4.0
   */
  @Override
  public void doFilter(MockRequest req, MockResponse res, FilterChain chain)
          throws IOException, ServletException {
    if (!(req instanceof HttpMockRequest && res instanceof HttpMockResponse)) {
      throw new ServletException("non-HTTP request or response");
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
   * @throws ServletException if the request for the could not be handled
   * @since Servlet 4.0
   */
  protected void doFilter(HttpMockRequest req, HttpMockResponse res, FilterChain chain)
          throws IOException, ServletException {
    chain.doFilter(req, res);
  }

}
