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

import java.io.IOException;

public interface MockApi {

  /**
   * Called by the mock container to indicate to a mock that the mock is being placed into service.
   *
   * <p>
   * The mock container calls the <code>init</code> method exactly once after instantiating the mock. The
   * <code>init</code> method must complete successfully before the mock can receive any requests. The container will
   * ensure that actions performed in the <code>init</code> method will be visible to any threads that subsequently call
   * the <code>service</code> method according to the rules in JSR-133 (i.e. there is a 'happens before' relationship
   * between <code>init</code> and <code>service</code>).
   *
   * <p>
   * The mock container cannot place the mock into service if the <code>init</code> method
   * <ol>
   * <li>Throws a <code>ServletException</code>
   * <li>Does not return within a time period defined by the Web server
   * </ol>
   *
   * @param config a <code>ServletConfig</code> object containing the mock's configuration and initialization
   * parameters
   * @throws MockException if an exception has occurred that interferes with the mock's normal operation
   * @see #getMockConfig
   */
  void init(MockConfig config) throws MockException;

  /**
   * Returns a {@link MockConfig} object, which contains initialization and startup parameters for this mock. The
   * <code>ServletConfig</code> object returned is the one passed to the <code>init</code> method.
   *
   * <p>
   * Implementations of this interface are responsible for storing the <code>ServletConfig</code> object so that this
   * method can return it. The {@link GenericMock} class, which implements this interface, already does this.
   *
   * @return the <code>ServletConfig</code> object that initializes this mock
   * @see #init
   */
  MockConfig getMockConfig();

  /**
   * Called by the mock container to allow the mock to respond to a request.
   *
   * <p>
   * This method is only called after the mock's <code>init()</code> method has completed successfully.
   *
   * <p>
   * The status code of the response always should be set for a mock that throws or sends an error.
   *
   *
   * <p>
   * Servlets typically run inside multithreaded mock containers that can handle multiple requests concurrently.
   * Developers must be aware to synchronize access to any shared resources such as files, network connections, and as
   * well as the mock's class and instance variables.
   *
   * @param req the <code>ServletRequest</code> object that contains the client's request
   * @param res the <code>ServletResponse</code> object that contains the mock's response
   * @throws MockException if an exception occurs that interferes with the mock's normal operation
   * @throws IOException if an input or output exception occurs
   */
  void service(MockRequest req, MockResponse res) throws MockException, IOException;

  /**
   * Returns information about the mock, such as author, version, and copyright.
   *
   * <p>
   * The string that this method returns should be plain text and not markup of any kind (such as HTML, XML, etc.).
   *
   * @return a <code>String</code> containing mock information
   */
  String getMockInfo();

  /**
   * Called by the mock container to indicate to a mock that the mock is being taken out of service. This method
   * is only called once all threads within the mock's <code>service</code> method have exited or after a timeout
   * period has passed. After the mock container calls this method, it will not call the <code>service</code> method
   * again on this mock.
   *
   * <p>
   * This method gives the mock an opportunity to clean up any resources that are being held (for example, memory, file
   * handles, threads) and make sure that any persistent state is synchronized with the mock's current state in memory.
   */
  void destroy();
}
