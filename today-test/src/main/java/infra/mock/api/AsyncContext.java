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

import infra.mock.api.http.HttpMockMapping;
import infra.mock.api.http.HttpMockRequest;

/**
 * Class representing the execution context for an asynchronous operation that was initiated on a ServletRequest.
 *
 * <p>
 * An AsyncContext is created and initialized by a call to {@link MockRequest#startAsync()} or
 * {@link MockRequest#startAsync(MockRequest, MockResponse)}. Repeated invocations of these methods will return
 * the same AsyncContext instance, reinitialized as appropriate.
 *
 * <p>
 * In the event that an asynchronous operation has timed out, the container must run through these steps:
 * <ol>
 * <li>Invoke, at their {@link AsyncListener#onTimeout onTimeout} method, all {@link AsyncListener} instances registered
 * with the ServletRequest on which the asynchronous operation was initiated.</li>
 * <li>If none of the listeners called {@link #complete} or any of the {@link #dispatch} methods, perform an error
 * dispatch with a status code equal to <tt>HttpServletResponse.SC_INTERNAL_SERVER_ERROR</tt>.</li>
 * <li>If no matching error page was found, or the error page did not call {@link #complete} or any of the
 * {@link #dispatch} methods, call {@link #complete}.</li>
 * </ol>
 */
public interface AsyncContext {

  /**
   * The name of the request attribute under which the original request URI is made available to the target of a
   * {@link #dispatch(String)} or {@link #dispatch(MockContext, String)}
   */
  String ASYNC_REQUEST_URI = "infra.mock.api.async.request_uri";

  /**
   * The name of the request attribute under which the original context path is made available to the target of a
   * {@link #dispatch(String)} or {@link #dispatch(MockContext, String)}
   */
  String ASYNC_CONTEXT_PATH = "infra.mock.api.async.context_path";

  /**
   * The name of the request attribute under which the original {@link HttpMockMapping} is made
   * available to the target of a {@link #dispatch(String)} or {@link #dispatch(MockContext, String)}
   */
  String ASYNC_MAPPING = "infra.mock.api.async.mapping";

  /**
   * The name of the request attribute under which the original path info is made available to the target of a
   * {@link #dispatch(String)} or {@link #dispatch(MockContext, String)}
   */
  String ASYNC_PATH_INFO = "infra.mock.api.async.path_info";

  /**
   * The name of the request attribute under which the original query string is made available to the target of a
   * {@link #dispatch(String)} or {@link #dispatch(MockContext, String)}
   */
  String ASYNC_QUERY_STRING = "infra.mock.api.async.query_string";

  /**
   * Gets the request that was used to initialize this AsyncContext by calling {@link MockRequest#startAsync()} or
   * {@link MockRequest#startAsync(MockRequest, MockResponse)}.
   *
   * @return the request that was used to initialize this AsyncContext
   * @throws IllegalStateException if {@link #complete} or any of the {@link #dispatch} methods has been called in the
   * asynchronous cycle
   */
  MockRequest getRequest();

  /**
   * Gets the response that was used to initialize this AsyncContext by calling {@link MockRequest#startAsync()} or
   * {@link MockRequest#startAsync(MockRequest, MockResponse)}.
   *
   * @return the response that was used to initialize this AsyncContext
   * @throws IllegalStateException if {@link #complete} or any of the {@link #dispatch} methods has been called in the
   * asynchronous cycle
   */
  MockResponse getResponse();

  /**
   * Checks if this AsyncContext was initialized with the original or application-wrapped request and response objects.
   *
   * <p>
   * This information may be used by filters invoked in the <i>outbound</i> direction, after a request was put into
   * asynchronous mode, to determine whether any request and/or response wrappers that they added during their
   * <i>inbound</i> invocation need to be preserved for the duration of the asynchronous operation, or may be released.
   *
   * @return true if this AsyncContext was initialized with the original request and response objects by calling
   * {@link MockRequest#startAsync()}, or if it was initialized by calling
   * {@link MockRequest#startAsync(MockRequest, MockResponse)}, and neither the ServletRequest nor
   * ServletResponse arguments carried any application-provided wrappers; false otherwise
   */
  boolean hasOriginalRequestAndResponse();

  /**
   * Dispatches the request and response objects of this AsyncContext to the servlet container.
   *
   * <p>
   * If the asynchronous cycle was started with {@link MockRequest#startAsync(MockRequest, MockResponse)}, and
   * the request passed is an instance of HttpServletRequest, then the dispatch is to the URI returned by
   * {@link HttpMockRequest#getRequestURI}. Otherwise, the dispatch is to the URI of the request
   * when it was last dispatched by the container.
   *
   * <p>
   * The following sequence illustrates how this will work:
   *
   * <pre>
   * {@code
   * // REQUEST dispatch to /url/A
   * AsyncContext ac = request.startAsync();
   * ...
   * ac.dispatch(); // ASYNC dispatch to /url/A
   *
   * // REQUEST to /url/A
   * // FORWARD dispatch to /url/B
   * request.getRequestDispatcher("/url/B").forward(request,response);
   * // Start async operation from within the target of the FORWARD
   * // dispatch
   * ac = request.startAsync();
   * ...
   * ac.dispatch(); // ASYNC dispatch to /url/A
   *
   * // REQUEST to /url/A
   * // FORWARD dispatch to /url/B
   * request.getRequestDispatcher("/url/B").forward(request,response);
   * // Start async operation from within the target of the FORWARD
   * // dispatch
   * ac = request.startAsync(request,response);
   * ...
   * ac.dispatch(); // ASYNC dispatch to /url/B
   * }
   * </pre>
   *
   * <p>
   * This method returns immediately after passing the request and response objects to a container managed thread, on
   * which the dispatch operation will be performed. If this method is called before the container-initiated dispatch that
   * called <tt>startAsync</tt> has returned to the container, the dispatch operation will be delayed until after the
   * container-initiated dispatch has returned to the container.
   *
   * <p>
   * The dispatcher type of the request is set to <tt>DispatcherType.ASYNC</tt>. Unlike
   * {@link RequestDispatcher#forward(MockRequest, MockResponse) forward dispatches}, the response buffer and
   * headers will not be reset, and it is legal to dispatch even if the response has already been committed.
   *
   * <p>
   * Control over the request and response is delegated to the dispatch target, and the response will be closed when the
   * dispatch target has completed execution, unless {@link MockRequest#startAsync()} or
   * {@link MockRequest#startAsync(MockRequest, MockResponse)} are called.
   *
   * <p>
   * Any errors or exceptions that may occur during the execution of this method must be caught and handled by the
   * container, as follows:
   * <ol>
   * <li>Invoke, at their {@link AsyncListener#onError onError} method, all {@link AsyncListener} instances registered
   * with the ServletRequest for which this AsyncContext was created, and make the caught <tt>Throwable</tt> available via
   * {@link AsyncEvent#getThrowable}.</li>
   * <li>If none of the listeners called {@link #complete} or any of the {@link #dispatch} methods, perform an error
   * dispatch with a status code equal to <tt>HttpServletResponse.SC_INTERNAL_SERVER_ERROR</tt>, and make the above
   * <tt>Throwable</tt> available as the value of the <tt>RequestDispatcher.ERROR_EXCEPTION</tt> request attribute.</li>
   * <li>If no matching error page was found, or the error page did not call {@link #complete} or any of the
   * {@link #dispatch} methods, call {@link #complete}.</li>
   * </ol>
   *
   * <p>
   * There can be at most one asynchronous dispatch operation per asynchronous cycle, which is started by a call to one of
   * the {@link MockRequest#startAsync} methods. Any attempt to perform an additional asynchronous dispatch operation
   * within the same asynchronous cycle will result in an IllegalStateException. If startAsync is subsequently called on
   * the dispatched request, then any of the dispatch or {@link #complete} methods may be called.
   *
   * @throws IllegalStateException if one of the dispatch methods has been called and the startAsync method has not been
   * called during the resulting dispatch, or if {@link #complete} was called
   * @see MockRequest#getDispatcherType
   */
  void dispatch();

  /**
   * Dispatches the request and response objects of this AsyncContext to the given <tt>path</tt>.
   *
   * <p>
   * The <tt>path</tt> parameter is interpreted in the same way as in {@link MockRequest#getRequestDispatcher(String)},
   * within the scope of the {@link MockContext} from which this AsyncContext was initialized.
   *
   * <p>
   * All path related query methods of the request must reflect the dispatch target, while the original request URI,
   * context path, path info, servlet path, and query string may be recovered from the {@link #ASYNC_REQUEST_URI},
   * {@link #ASYNC_CONTEXT_PATH}, {@link #ASYNC_PATH_INFO}, {@link #ASYNC_SERVLET_PATH}, and {@link #ASYNC_QUERY_STRING}
   * attributes of the request. These attributes will always reflect the original path elements, even under repeated
   * dispatches.
   *
   * <p>
   * There can be at most one asynchronous dispatch operation per asynchronous cycle, which is started by a call to one of
   * the {@link MockRequest#startAsync} methods. Any attempt to perform an additional asynchronous dispatch operation
   * within the same asynchronous cycle will result in an IllegalStateException. If startAsync is subsequently called on
   * the dispatched request, then any of the dispatch or {@link #complete} methods may be called.
   *
   * <p>
   * See {@link #dispatch()} for additional details, including error handling.
   *
   * @param path the path of the dispatch target, scoped to the MockContext from which this AsyncContext was
   * initialized
   * @throws IllegalStateException if one of the dispatch methods has been called and the startAsync method has not been
   * called during the resulting dispatch, or if {@link #complete} was called
   * @see MockRequest#getDispatcherType
   */
  void dispatch(String path);

  /**
   * Dispatches the request and response objects of this AsyncContext to the given <tt>path</tt> scoped to the given
   * <tt>context</tt>.
   *
   * <p>
   * The <tt>path</tt> parameter is interpreted in the same way as in {@link MockRequest#getRequestDispatcher(String)},
   * except that it is scoped to the given <tt>context</tt>.
   *
   * <p>
   * All path related query methods of the request must reflect the dispatch target, while the original request URI,
   * context path, path info, servlet path, and query string may be recovered from the {@link #ASYNC_REQUEST_URI},
   * {@link #ASYNC_CONTEXT_PATH}, {@link #ASYNC_PATH_INFO}, {@link #ASYNC_SERVLET_PATH}, and {@link #ASYNC_QUERY_STRING}
   * attributes of the request. These attributes will always reflect the original path elements, even under repeated
   * dispatches.
   *
   * <p>
   * There can be at most one asynchronous dispatch operation per asynchronous cycle, which is started by a call to one of
   * the {@link MockRequest#startAsync} methods. Any attempt to perform an additional asynchronous dispatch operation
   * within the same asynchronous cycle will result in an IllegalStateException. If startAsync is subsequently called on
   * the dispatched request, then any of the dispatch or {@link #complete} methods may be called.
   *
   * <p>
   * See {@link #dispatch()} for additional details, including error handling.
   *
   * @param context the MockContext of the dispatch target
   * @param path the path of the dispatch target, scoped to the given MockContext
   * @throws IllegalStateException if one of the dispatch methods has been called and the startAsync method has not been
   * called during the resulting dispatch, or if {@link #complete} was called
   * @see MockRequest#getDispatcherType
   */
  void dispatch(MockContext context, String path);

  /**
   * Completes the asynchronous operation that was started on the request that was used to initialze this AsyncContext,
   * closing the response that was used to initialize this AsyncContext.
   *
   * <p>
   * Any listeners of type {@link AsyncListener} that were registered with the ServletRequest for which this AsyncContext
   * was created will be invoked at their {@link AsyncListener#onComplete(AsyncEvent) onComplete} method.
   *
   * <p>
   * It is legal to call this method any time after a call to {@link MockRequest#startAsync()} or
   * {@link MockRequest#startAsync(MockRequest, MockResponse)}, and before a call to one of the <tt>dispatch</tt>
   * methods of this class. If this method is called before the container-initiated dispatch that called
   * <tt>startAsync</tt> has returned to the container, then the call will not take effect (and any invocations of
   * {@link AsyncListener#onComplete(AsyncEvent)} will be delayed) until after the container-initiated dispatch has
   * returned to the container.
   */
  void complete();

  /**
   * Causes the container to dispatch a thread, possibly from a managed thread pool, to run the specified
   * <tt>Runnable</tt>. The container may propagate appropriate contextual information to the <tt>Runnable</tt>.
   *
   * @param run the asynchronous handler
   */
  void start(Runnable run);

  /**
   * Registers the given {@link AsyncListener} with the most recent asynchronous cycle that was started by a call to one
   * of the {@link MockRequest#startAsync} methods.
   *
   * <p>
   * The given AsyncListener will receive an {@link AsyncEvent} when the asynchronous cycle completes successfully, times
   * out, results in an error, or a new asynchronous cycle is being initiated via one of the
   * {@link MockRequest#startAsync} methods.
   *
   * <p>
   * AsyncListener instances will be notified in the order in which they were added.
   *
   * <p>
   * If {@link MockRequest#startAsync(MockRequest, MockResponse)} or {@link MockRequest#startAsync} is called,
   * the exact same request and response objects are available from the {@link AsyncEvent} when the {@link AsyncListener}
   * is notified.
   *
   * @param listener the AsyncListener to be registered
   * @throws IllegalStateException if this method is called after the container-initiated dispatch, during which one of
   * the {@link MockRequest#startAsync} methods was called, has returned to the container
   */
  void addListener(AsyncListener listener);

  /**
   * Registers the given {@link AsyncListener} with the most recent asynchronous cycle that was started by a call to one
   * of the {@link MockRequest#startAsync} methods.
   *
   * <p>
   * The given AsyncListener will receive an {@link AsyncEvent} when the asynchronous cycle completes successfully, times
   * out, results in an error, or a new asynchronous cycle is being initiated via one of the
   * {@link MockRequest#startAsync} methods.
   *
   * <p>
   * AsyncListener instances will be notified in the order in which they were added.
   *
   * <p>
   * The given ServletRequest and ServletResponse objects will be made available to the given AsyncListener via the
   * {@link AsyncEvent#getSuppliedRequest getSuppliedRequest} and {@link AsyncEvent#getSuppliedResponse
   * getSuppliedResponse} methods, respectively, of the {@link AsyncEvent} delivered to it. These objects should not be
   * read from or written to, respectively, at the time the AsyncEvent is delivered, because additional wrapping may have
   * occurred since the given AsyncListener was registered, but may be used in order to release any resources associated
   * with them.
   *
   * @param listener the AsyncListener to be registered
   * @param mockRequest the ServletRequest that will be included in the AsyncEvent
   * @param mockResponse the ServletResponse that will be included in the AsyncEvent
   * @throws IllegalStateException if this method is called after the container-initiated dispatch, during which one of
   * the {@link MockRequest#startAsync} methods was called, has returned to the container
   */
  void addListener(AsyncListener listener, MockRequest mockRequest, MockResponse mockResponse);

  /**
   * Instantiates the given {@link AsyncListener} class.
   *
   * <p>
   * The returned AsyncListener instance may be further customized before it is registered with this AsyncContext via a
   * call to one of the <code>addListener</code> methods.
   *
   * <p>
   * The given AsyncListener class must define a zero argument constructor, which is used to instantiate it.
   *
   * <p>
   * This method supports resource injection if the given <tt>clazz</tt> represents a Managed Bean. See the Jakarta EE
   * platform and CDI specifications for additional details about Managed Beans and resource injection.
   *
   * <p>
   * This method supports any annotations applicable to AsyncListener.
   *
   * @param <T> the class of the object to instantiate
   * @param clazz the AsyncListener class to instantiate
   * @return the new AsyncListener instance
   * @throws MockException if the given <tt>clazz</tt> fails to be instantiated
   */
  <T extends AsyncListener> T createListener(Class<T> clazz) throws MockException;

  /**
   * Sets the timeout (in milliseconds) for this AsyncContext.
   *
   * <p>
   * The timeout applies to this AsyncContext once the container-initiated dispatch during which one of the
   * {@link MockRequest#startAsync} methods was called has returned to the container.
   *
   * <p>
   * The timeout will expire if neither the {@link #complete} method nor any of the dispatch methods are called. A timeout
   * value of zero or less indicates no timeout.
   *
   * <p>
   * If {@link #setTimeout} is not called, then the container's default timeout, which is available via a call to
   * {@link #getTimeout}, will apply.
   *
   * <p>
   * The default value is <code>30000</code> ms.
   *
   * @param timeout the timeout in milliseconds
   * @throws IllegalStateException if this method is called after the container-initiated dispatch, during which one of
   * the {@link MockRequest#startAsync} methods was called, has returned to the container
   */
  void setTimeout(long timeout);

  /**
   * Gets the timeout (in milliseconds) for this AsyncContext.
   *
   * <p>
   * This method returns the container's default timeout for asynchronous operations, or the timeout value passed to the
   * most recent invocation of {@link #setTimeout}.
   *
   * <p>
   * A timeout value of zero or less indicates no timeout.
   *
   * @return the timeout in milliseconds
   */
  long getTimeout();

}
