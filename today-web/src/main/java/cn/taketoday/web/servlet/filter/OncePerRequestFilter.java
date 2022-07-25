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

package cn.taketoday.web.servlet.filter;

import java.io.IOException;

import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.context.async.WebAsyncManager;
import cn.taketoday.web.context.async.WebAsyncUtils;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filter base class that aims to guarantee a single execution per request
 * dispatch, on any servlet container. It provides a {@link #doFilterInternal}
 * method with HttpServletRequest and HttpServletResponse arguments.
 *
 * <p>A filter may be invoked as part of a
 * {@link jakarta.servlet.DispatcherType#REQUEST REQUEST} or
 * {@link jakarta.servlet.DispatcherType#ASYNC ASYNC} dispatches that occur in
 * separate threads. A filter can be configured in {@code web.xml} whether it
 * should be involved in async dispatches. However, in some cases servlet
 * containers assume different default configuration. Therefore sub-classes can
 * override the method {@link #shouldNotFilterAsyncDispatch()} to declare
 * statically if they should indeed be invoked, <em>once</em>, during both types
 * of dispatches in order to provide thread initialization, logging, security,
 * and so on. This mechanism complements and does not replace the need to
 * configure a filter in {@code web.xml} with dispatcher types.
 *
 * <p>Yet another dispatch type that also occurs in its own thread is
 * {@link jakarta.servlet.DispatcherType#ERROR ERROR}. Subclasses can override
 * {@link #shouldNotFilterErrorDispatch()} if they wish to declare statically
 * if they should be invoked <em>once</em> during error dispatches.
 *
 * <p>The {@link #getAlreadyFilteredAttributeName} method determines how to
 * identify that a request is already filtered. The default implementation is
 * based on the configured name of the concrete filter instance.
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/20 15:15
 */
public abstract class OncePerRequestFilter extends GenericFilterBean {

  /**
   * Suffix that gets appended to the filter name for the
   * "already filtered" request attribute.
   *
   * @see #getAlreadyFilteredAttributeName
   */
  public static final String ALREADY_FILTERED_SUFFIX = ".FILTERED";

  /**
   * This {@code doFilter} implementation stores a request attribute for
   * "already filtered", proceeding without filtering again if the
   * attribute is already there.
   *
   * @see #getAlreadyFilteredAttributeName
   * @see #shouldNotFilter
   * @see #doFilterInternal
   */
  @Override
  public final void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
          throws ServletException, IOException {

    if (!(request instanceof HttpServletRequest httpRequest)) {
      throw new ServletException("OncePerRequestFilter just supports HTTP requests");
    }

    HttpServletResponse httpResponse = (HttpServletResponse) response;
    String alreadyFilteredAttributeName = getAlreadyFilteredAttributeName();
    boolean hasAlreadyFilteredAttribute = request.getAttribute(alreadyFilteredAttributeName) != null;

    if (skipDispatch(httpRequest) || shouldNotFilter(httpRequest)) {
      // Proceed without invoking this filter...
      filterChain.doFilter(request, response);
    }
    else if (hasAlreadyFilteredAttribute) {
      if (DispatcherType.ERROR.equals(request.getDispatcherType())) {
        doFilterNestedErrorDispatch(httpRequest, httpResponse, filterChain);
        return;
      }

      // Proceed without invoking this filter...
      filterChain.doFilter(request, response);
    }
    else {
      // Do invoke this filter...
      request.setAttribute(alreadyFilteredAttributeName, Boolean.TRUE);
      try {
        doFilterInternal(httpRequest, httpResponse, filterChain);
      }
      finally {
        // Remove the "already filtered" request attribute for this request.
        request.removeAttribute(alreadyFilteredAttributeName);
      }
    }
  }

  private boolean skipDispatch(HttpServletRequest request) {
    if (isAsyncDispatch(request) && shouldNotFilterAsyncDispatch()) {
      return true;
    }
    return request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI) != null
            && shouldNotFilterErrorDispatch();
  }

  /**
   * The dispatcher type {@code jakarta.servlet.DispatcherType.ASYNC} means a
   * filter can be invoked in more than one thread over the course of a single
   * request. This method returns {@code true} if the filter is currently
   * executing within an asynchronous dispatch.
   *
   * @param request the current request
   */
  protected boolean isAsyncDispatch(HttpServletRequest request) {
    return DispatcherType.ASYNC == request.getDispatcherType();
  }

  /**
   * Whether request processing is in asynchronous mode meaning that the
   * response will not be committed after the current thread is exited.
   *
   * @param request the current request
   * @see WebAsyncManager#isConcurrentHandlingStarted()
   */
  protected boolean isAsyncStarted(HttpServletRequest request) {
    RequestContext context = RequestContextHolder.get();
    if (context == null) {
      return request.isAsyncStarted();
    }
    WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(context);
    return asyncManager.isConcurrentHandlingStarted();
  }

  /**
   * Return the name of the request attribute that identifies that a request
   * is already filtered.
   * <p>The default implementation takes the configured name of the concrete filter
   * instance and appends ".FILTERED". If the filter is not fully initialized,
   * it falls back to its class name.
   *
   * @see #getFilterName
   * @see #ALREADY_FILTERED_SUFFIX
   */
  protected String getAlreadyFilteredAttributeName() {
    String name = getFilterName();
    if (name == null) {
      name = getClass().getName();
    }
    return name + ALREADY_FILTERED_SUFFIX;
  }

  /**
   * Can be overridden in subclasses for custom filtering control,
   * returning {@code true} to avoid filtering of the given request.
   * <p>The default implementation always returns {@code false}.
   *
   * @param request current HTTP request
   * @return whether the given request should <i>not</i> be filtered
   * @throws ServletException in case of errors
   */
  protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
    return false;
  }

  /**
   * The dispatcher type {@code jakarta.servlet.DispatcherType.ASYNC} means a
   * filter can be invoked in more than one thread over the course of a single
   * request. Some filters only need to filter the initial thread (e.g. request
   * wrapping) while others may need to be invoked at least once in each
   * additional thread for example for setting up thread locals or to perform
   * final processing at the very end.
   * <p>Note that although a filter can be mapped to handle specific dispatcher
   * types via {@code web.xml} or in Java through the {@code ServletContext},
   * servlet containers may enforce different defaults with respect to
   * dispatcher types. This flag enforces the design intent of the filter.
   * <p>The default return value is "true", which means the filter will not be
   * invoked during subsequent async dispatches. If "false", the filter will
   * be invoked during async dispatches with the same guarantees of being
   * invoked only once during a request within a single thread.
   */
  protected boolean shouldNotFilterAsyncDispatch() {
    return true;
  }

  /**
   * Whether to filter error dispatches such as when the servlet container
   * processes and error mapped in {@code web.xml}. The default return value
   * is "true", which means the filter will not be invoked in case of an error
   * dispatch.
   */
  protected boolean shouldNotFilterErrorDispatch() {
    return true;
  }

  /**
   * Same contract as for {@code doFilter}, but guaranteed to be
   * just invoked once per request within a single request thread.
   * See {@link #shouldNotFilterAsyncDispatch()} for details.
   * <p>Provides HttpServletRequest and HttpServletResponse arguments instead of the
   * default ServletRequest and ServletResponse ones.
   */
  protected abstract void doFilterInternal(
          HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
          throws ServletException, IOException;

  /**
   * Typically an ERROR dispatch happens after the REQUEST dispatch completes,
   * and the filter chain starts anew. On some servers however the ERROR
   * dispatch may be nested within the REQUEST dispatch, e.g. as a result of
   * calling {@code sendError} on the response. In that case we are still in
   * the filter chain, on the same thread, but the request and response have
   * been switched to the original, unwrapped ones.
   * <p>Sub-classes may use this method to filter such nested ERROR dispatches
   * and re-apply wrapping on the request or response. {@code ThreadLocal}
   * context, if any, should still be active as we are still nested within
   * the filter chain.
   */
  protected void doFilterNestedErrorDispatch(
          HttpServletRequest request,
          HttpServletResponse response,
          FilterChain filterChain) throws ServletException, IOException {

    filterChain.doFilter(request, response);
  }

}

