/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.handler.mvc;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.HandlerMapping;
import cn.taketoday.web.HttpRequestHandler;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.view.ModelAndView;

/**
 * Base Controller interface, representing a component that receives
 * {@code HttpServletRequest} and {@code HttpServletResponse}
 * instances just like a {@code HttpServlet} but is able to
 * participate in an MVC workflow. Controllers are comparable to the
 * notion of a Struts {@code Action}.
 *
 * <p>Any implementation of the Controller interface should be a
 * <i>reusable, thread-safe</i> class, capable of handling multiple
 * HTTP requests throughout the lifecycle of an application. To be able to
 * configure a Controller easily, Controller implementations are encouraged
 * to be (and usually are) JavaBeans.
 *
 * <h3><a name="workflow">Workflow</a></h3>
 *
 * <p>After a {@code DispatcherServlet} has received a request and has
 * done its work to resolve locales, themes, and suchlike, it then tries
 * to resolve a Controller, using a
 * {@link HandlerMapping HandlerMapping}.
 * When a Controller has been found to handle the request, the
 * {@link #handleRequest(RequestContext) handleRequest}
 * method of the located Controller will be invoked; the located Controller
 * is then responsible for handling the actual request and &mdash; if applicable
 * &mdash; returning an appropriate
 * {@link cn.taketoday.web.view.ModelAndView ModelAndView}.
 * So actually, this method is the main entry point for the
 * {@link cn.taketoday.web.servlet.DispatcherServlet DispatcherServlet}
 * which delegates requests to controllers.
 *
 * <p>So basically any <i>direct</i> implementation of the {@code Controller} interface
 * just handles HttpServletRequests and should return a ModelAndView, to be further
 * interpreted by the DispatcherServlet. Any additional functionality such as
 * optional validation, form handling, etc. should be obtained through extending
 * {@link AbstractController AbstractController}
 * or one of its subclasses.
 *
 * <h3>Notes on design and testing</h3>
 *
 * <p>The Controller interface is explicitly designed to operate on HttpServletRequest
 * and HttpServletResponse objects, just like an HttpServlet. It does not aim to
 * decouple itself from the Servlet API, in contrast to, for example, WebWork, JSF or Tapestry.
 * Instead, the full power of the Servlet API is available, allowing Controllers to be
 * general-purpose: a Controller is able to not only handle web user interface
 * requests but also to process remoting protocols or to generate reports on demand.
 *
 * <p>Controllers can easily be tested by passing in mock objects for the
 * HttpServletRequest and HttpServletResponse objects as parameters to the
 * {@link #handleRequest(RequestContext) handleRequest}
 * method. As a convenience, Framework ships with a set of Servlet API mocks
 * that are suitable for testing any kind of web components, but are particularly
 * suitable for testing web controllers. In contrast to a Struts Action,
 * there is no need to mock the ActionServlet or any other infrastructure;
 * mocking HttpServletRequest and HttpServletResponse is sufficient.
 *
 * <p>If Controllers need to be aware of specific environment references, they can
 * choose to implement specific awareness interfaces, just like any other bean in a
 * Framework (web) application context can do, for example:
 * <ul>
 * <li>{@code cn.taketoday.context.aware.ApplicationContextAware}</li>
 * <li>{@code cn.taketoday.context.aware.ResourceLoaderAware}</li>
 * <li>{@code cn.taketoday.web.servlet.ServletContextAware}</li>
 * </ul>
 *
 * <p>Such environment references can easily be passed in testing environments,
 * through the corresponding setters defined in the respective awareness interfaces.
 * In general, it is recommended to keep the dependencies as minimal as possible:
 * for example, if all you need is resource loading, implement ResourceLoaderAware only.
 * Alternatively, derive from the WebApplicationObjectSupport base class, which gives
 * you all those references through convenient accessors but requires an
 * ApplicationContext reference on initialization.
 *
 * <p>
 *   like Framework's {@code .servlet.mvc.Controller}
 * </p>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see AbstractController
 * @see cn.taketoday.context.aware.ApplicationContextAware
 * @see cn.taketoday.context.aware.ResourceLoaderAware
 * @see cn.taketoday.web.servlet.ServletContextAware
 */
@FunctionalInterface
public interface Controller extends HttpRequestHandler {

  /**
   * Process the request and return a ModelAndView object which the DispatcherServlet
   * will render. A {@code null} return value is not an error: it indicates that
   * this object completed request processing itself and that there is therefore no
   * ModelAndView to render.
   *
   * @param request current HTTP request context
   * @return a ModelAndView to render, or {@code null} if handled directly
   * @throws Exception in case of errors
   */
  @Nullable
  @Override
  ModelAndView handleRequest(RequestContext request) throws Exception;

}
