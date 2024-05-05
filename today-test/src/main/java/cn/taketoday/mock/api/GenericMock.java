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
import java.io.Serial;
import java.util.Enumeration;

import cn.taketoday.mock.api.http.HttpMock;

/**
 * Defines a generic, protocol-independent servlet. To write an HTTP servlet for use on the Web, extend
 * {@link HttpMock} instead.
 *
 * <p>
 * <code>GenericServlet</code> implements the <code>Servlet</code> and <code>ServletConfig</code> interfaces.
 * <code>GenericServlet</code> may be directly extended by a servlet, although it's more common to extend a
 * protocol-specific subclass such as <code>HttpServlet</code>.
 *
 * <p>
 * <code>GenericServlet</code> makes writing servlets easier. It provides simple versions of the lifecycle methods
 * <code>init</code> and <code>destroy</code> and of the methods in the <code>ServletConfig</code> interface.
 * <code>GenericServlet</code> also implements the <code>log</code> method, declared in the <code>MockContext</code>
 * interface.
 *
 * <p>
 * To write a generic servlet, you need only override the abstract <code>service</code> method.
 *
 * @author Various
 */
public abstract class GenericMock implements MockApi, MockConfig, java.io.Serializable {
  @Serial
  private static final long serialVersionUID = -8592279577370996712L;

  private transient MockConfig config;

  /**
   * Does nothing. All of the servlet initialization is done by one of the <code>init</code> methods.
   */
  public GenericMock() {
  }

  /**
   * Called by the servlet container to indicate to a servlet that the servlet is being taken out of service. See
   * {@link MockApi#destroy}.
   */
  @Override
  public void destroy() {
  }

  /**
   * Returns a <code>String</code> containing the value of the named initialization parameter, or <code>null</code> if the
   * parameter does not exist. See {@link MockConfig#getInitParameter}.
   *
   * <p>
   * This method is supplied for convenience. It gets the value of the named parameter from the servlet's
   * <code>ServletConfig</code> object.
   *
   * @param name a <code>String</code> specifying the name of the initialization parameter
   * @return String a <code>String</code> containing the value of the initialization parameter
   */
  @Override
  public String getInitParameter(String name) {
    MockConfig sc = getMockConfig();
    if (sc == null) {
      throw new IllegalStateException("ServletConfig has not been initialized");
    }

    return sc.getInitParameter(name);
  }

  /**
   * Returns the names of the servlet's initialization parameters as an <code>Enumeration</code> of <code>String</code>
   * objects, or an empty <code>Enumeration</code> if the servlet has no initialization parameters. See
   * {@link MockConfig#getInitParameterNames}.
   *
   * <p>
   * This method is supplied for convenience. It gets the parameter names from the servlet's <code>ServletConfig</code>
   * object.
   *
   * @return Enumeration an enumeration of <code>String</code> objects containing the names of the servlet's
   * initialization parameters
   */
  @Override
  public Enumeration<String> getInitParameterNames() {
    MockConfig sc = getMockConfig();
    if (sc == null) {
      throw new IllegalStateException("ServletConfig has not been initialized");
    }

    return sc.getInitParameterNames();
  }

  /**
   * Returns this servlet's {@link MockConfig} object.
   *
   * @return ServletConfig the <code>ServletConfig</code> object that initialized this servlet
   */
  @Override
  public MockConfig getMockConfig() {
    return config;
  }

  /**
   * Returns a reference to the {@link MockContext} in which this servlet is running. See
   * {@link MockConfig#getMockContext}.
   *
   * <p>
   * This method is supplied for convenience. It gets the context from the servlet's <code>ServletConfig</code> object.
   *
   * @return MockContext the <code>MockContext</code> object passed to this servlet by the <code>init</code> method
   */
  @Override
  public MockContext getMockContext() {
    MockConfig sc = getMockConfig();
    if (sc == null) {
      throw new IllegalStateException("ServletConfig has not been initialized");
    }

    return sc.getMockContext();
  }

  /**
   * Returns information about the servlet, such as author, version, and copyright. By default, this method returns an
   * empty string. Override this method to have it return a meaningful value. See {@link MockApi#getMockInfo}.
   *
   * @return String information about this servlet, by default an empty string
   */
  @Override
  public String getMockInfo() {
    return "";
  }

  /**
   * Called by the servlet container to indicate to a servlet that the servlet is being placed into service. See
   * {@link MockApi#init}.
   *
   * <p>
   * This implementation stores the {@link MockConfig} object it receives from the servlet container for later use.
   * When overriding this form of the method, call <code>super.init(config)</code>.
   *
   * @param config the <code>ServletConfig</code> object that contains configuration information for this servlet
   * @throws MockException if an exception occurs that interrupts the servlet's normal operation
   */
  @Override
  public void init(MockConfig config) throws MockException {
    this.config = config;
    this.init();
  }

  /**
   * A convenience method which can be overridden so that there's no need to call <code>super.init(config)</code>.
   *
   * <p>
   * Instead of overriding {@link #init(MockConfig)}, simply override this method and it will be called by
   * <code>GenericServlet.init(ServletConfig config)</code>. The <code>ServletConfig</code> object can still be retrieved
   * via {@link #getMockConfig}.
   *
   * @throws MockException if an exception occurs that interrupts the servlet's normal operation
   */
  public void init() throws MockException {

  }

  /**
   * Writes the specified message to a servlet log file, prepended by the servlet's name. See
   * {@link MockContext#log(String)}.
   *
   * @param msg a <code>String</code> specifying the message to be written to the log file
   */
  public void log(String msg) {
    getMockContext().log(getMockName() + ": " + msg);
  }

  /**
   * Writes an explanatory message and a stack trace for a given <code>Throwable</code> exception to the servlet log file,
   * prepended by the servlet's name. See {@link MockContext#log(String, Throwable)}.
   *
   * @param message a <code>String</code> that describes the error or exception
   * @param t the <code>java.lang.Throwable</code> error or exception
   */
  public void log(String message, Throwable t) {
    getMockContext().log(getMockName() + ": " + message, t);
  }

  /**
   * Called by the servlet container to allow the servlet to respond to a request. See {@link MockApi#service}.
   *
   * <p>
   * This method is declared abstract so subclasses, such as <code>HttpServlet</code>, must override it.
   *
   * @param req the <code>ServletRequest</code> object that contains the client's request
   * @param res the <code>ServletResponse</code> object that will contain the servlet's response
   * @throws MockException if an exception occurs that interferes with the servlet's normal operation occurred
   * @throws IOException if an input or output exception occurs
   */
  @Override
  public abstract void service(MockRequest req, MockResponse res) throws MockException, IOException;

  /**
   * Returns the name of this servlet instance. See {@link MockConfig#getMockName}.
   *
   * @return the name of this servlet instance
   */
  @Override
  public String getMockName() {
    MockConfig sc = getMockConfig();
    if (sc == null) {
      throw new IllegalStateException("ServletConfig has not been initialized");
    }

    return sc.getMockName();
  }
}
