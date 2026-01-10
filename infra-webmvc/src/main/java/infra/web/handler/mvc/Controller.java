/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.handler.mvc;

import infra.context.ApplicationContextAware;
import infra.context.ResourceLoaderAware;
import infra.web.DispatcherHandler;
import infra.web.HandlerMapping;
import infra.web.HttpRequestHandler;
import infra.web.RequestContext;

/**
 * Base Controller interface, representing a component that receives
 * {@code RequestContext} instances just like a {@code HttpServlet}
 * but is able to participate in an MVC workflow. Controllers are
 * comparable to the notion of a Struts {@code Action}.
 *
 * <p>Any implementation of the Controller interface should be a
 * <i>reusable, thread-safe</i> class, capable of handling multiple
 * HTTP requests throughout the lifecycle of an application. To be able to
 * configure a Controller easily, Controller implementations are encouraged
 * to be (and usually are) JavaBeans.
 *
 * <h3><a name="workflow">Workflow</a></h3>
 *
 * <p>After a {@code DispatcherHandler} has received a request and has
 * done its work to resolve locales, themes, and suchlike, it then tries
 * to resolve a Controller, using a  {@link HandlerMapping HandlerMapping}.
 * When a Controller has been found to handle the request, the
 * {@link #handleRequest(RequestContext) handleRequest}
 * method of the located Controller will be invoked; the located Controller
 * is then responsible for handling the actual request and &mdash; if applicable
 * &mdash; returning an appropriate Object result.
 * So actually, this method is the main entry point for the
 * {@link DispatcherHandler DispatcherHandler}
 * which delegates requests to controllers.
 *
 * <p>So basically any <i>direct</i> implementation of the {@code Controller} interface
 * just handles HttpRequests and should return a result, to be further
 * interpreted by the DispatcherHandler. Any additional functionality such as
 * optional validation, form handling, etc. should be obtained through extending
 * {@link AbstractController AbstractController}
 * or one of its subclasses.
 *
 * <h3>Notes on design and testing</h3>
 *
 * <p>The Controller interface is explicitly designed to operate on RequestContext
 * objects, just like an HttpServlet. It does not aim to  decouple itself from
 * the Web API, in contrast to, for example, WebWork, JSF or Tapestry.
 * Instead, the full power of the Web API is available, allowing Controllers to be
 * general-purpose: a Controller is able to not only handle web user interface
 * requests but also to process remoting protocols or to generate reports on demand.
 *
 * <p>Controllers can easily be tested by passing in mock objects for the
 * RequestContext objects as parameters to the
 * {@link #handleRequest(RequestContext) handleRequest}
 * method. As a convenience, Framework ships with a set of Web API mocks
 * that are suitable for testing any kind of web components, but are particularly
 * suitable for testing web controllers. In contrast to a Struts Action,
 * there is no need to mock the ActionServlet or any other infrastructure;
 * mocking RequestContext is sufficient.
 *
 * <p>If Controllers need to be aware of specific environment references, they can
 * choose to implement specific awareness interfaces, just like any other bean in a
 * Framework (web) application context can do, for example:
 * <ul>
 * <li>{@code infra.context.ApplicationContextAware}</li>
 * <li>{@code infra.context.ResourceLoaderAware}</li>
 * </ul>
 *
 * <p>Such environment references can easily be passed in testing environments,
 * through the corresponding setters defined in the respective awareness interfaces.
 * In general, it is recommended to keep the dependencies as minimal as possible:
 * for example, if all you need is resource loading, implement ResourceLoaderAware only.
 * Alternatively, derive from the ApplicationContextSupport base class, which gives
 * you all those references through convenient accessors but requires an
 * ApplicationContext reference on initialization.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see AbstractController
 * @see ApplicationContextAware
 * @see ResourceLoaderAware
 * @since 4.0
 */
@FunctionalInterface
public interface Controller extends HttpRequestHandler {

}
