/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.framework.web.servlet.support;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cn.taketoday.core.Ordered;
import cn.taketoday.framework.web.server.ErrorPage;
import cn.taketoday.framework.web.server.ErrorPageRegistrar;
import cn.taketoday.framework.web.server.ErrorPageRegistry;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.web.servlet.filter.OncePerRequestFilter;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

/**
 * A Servlet {@link Filter} that provides an {@link ErrorPageRegistry} for non-embedded
 * applications (i.e. deployed WAR files). It registers error pages and handles
 * application errors by filtering requests and forwarding to the error pages instead of
 * letting the server handle them. Error pages are a feature of the servlet spec but there
 * is no Java API for registering them in the spec. This filter works around that by
 * accepting error page registrations fromFramework's {@link ErrorPageRegistrar} (any
 * beans of that type in the context will be applied to this server).
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Andy Wilkinson
 * @since 4.0
 */
public class ErrorPageFilter implements Filter, ErrorPageRegistry, Ordered {

  private static final Logger log = LoggerFactory.getLogger(ErrorPageFilter.class);

  private static final Set<Class<?>> CLIENT_ABORT_EXCEPTIONS;

  static {
    Class<Object> loaded = ClassUtils.load("org.apache.catalina.connector.ClientAbortException");
    if (loaded != null) {
      CLIENT_ABORT_EXCEPTIONS = Set.of(loaded);
    }
    else {
      CLIENT_ABORT_EXCEPTIONS = Collections.emptySet();
    }
  }

  private String global;

  private final Map<Integer, String> statuses = new HashMap<>();

  private final Map<Class<?>, String> exceptions = new HashMap<>();

  private final OncePerRequestFilter delegate = new OncePerRequestFilter() {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
      ErrorPageFilter.this.doFilter(request, response, chain);
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
      return false;
    }

  };

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    this.delegate.init(filterConfig);
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
          throws IOException, ServletException {
    this.delegate.doFilter(request, response, chain);
  }

  private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
          throws IOException, ServletException {
    ErrorWrapperResponse wrapped = new ErrorWrapperResponse(response);
    try {
      chain.doFilter(request, wrapped);
      if (wrapped.hasErrorToSend()) {
        handleErrorStatus(request, response, wrapped.getStatus(), wrapped.getMessage());
        response.flushBuffer();
      }
      else if (!request.isAsyncStarted() && !response.isCommitted()) {
        response.flushBuffer();
      }
    }
    catch (Throwable ex) {
      Throwable exceptionToHandle = ex;
      if (ex instanceof ServletException servletEx) {
        Throwable rootCause = servletEx.getRootCause();
        if (rootCause != null) {
          exceptionToHandle = rootCause;
        }
      }
      handleException(request, response, wrapped, exceptionToHandle);
      response.flushBuffer();
    }
  }

  private void handleErrorStatus(HttpServletRequest request, HttpServletResponse response,
          int status, String message) throws ServletException, IOException {
    if (response.isCommitted()) {
      handleCommittedResponse(request, null);
      return;
    }
    String errorPath = getErrorPath(this.statuses, status);
    if (errorPath == null) {
      response.sendError(status, message);
      return;
    }
    response.setStatus(status);
    setErrorAttributes(request, status, message);
    request.getRequestDispatcher(errorPath).forward(request, response);
  }

  private void handleException(HttpServletRequest request, HttpServletResponse response,
          ErrorWrapperResponse wrapped, Throwable ex) throws IOException, ServletException {
    Class<?> type = ex.getClass();
    String errorPath = getErrorPath(type);
    if (errorPath == null) {
      rethrow(ex);
      return;
    }
    if (response.isCommitted()) {
      handleCommittedResponse(request, ex);
      return;
    }
    forwardToErrorPage(errorPath, request, wrapped, ex);
  }

  private void forwardToErrorPage(String path, HttpServletRequest request, HttpServletResponse response, Throwable ex)
          throws ServletException, IOException {

    if (log.isErrorEnabled()) {
      log.error("Forwarding to error page from request {} due to exception [{}]",
              getDescription(request), ex.getMessage(), ex);
    }

    setErrorAttributes(request, 500, ex.getMessage());
    request.setAttribute(RequestDispatcher.ERROR_EXCEPTION, ex);
    request.setAttribute(RequestDispatcher.ERROR_EXCEPTION_TYPE, ex.getClass());
    response.reset();
    response.setStatus(500);
    request.getRequestDispatcher(path).forward(request, response);
    request.removeAttribute(RequestDispatcher.ERROR_EXCEPTION);
    request.removeAttribute(RequestDispatcher.ERROR_EXCEPTION_TYPE);
  }

  /**
   * Return the description for the given request. By default this method will return a
   * description based on the request {@code servletPath} and {@code pathInfo}.
   *
   * @param request the source request
   * @return the description
   */
  protected String getDescription(HttpServletRequest request) {
    String pathInfo = (request.getPathInfo() != null) ? request.getPathInfo() : "";
    return "[" + request.getServletPath() + pathInfo + "]";
  }

  private void handleCommittedResponse(HttpServletRequest request, Throwable ex) {
    if (isClientAbortException(ex)) {
      return;
    }
    String message = ("Cannot forward to error page for request %s"
            + " as the response has already been"
            + " committed. As a result, the response may have the wrong status"
            + " code. If your application is running on WebSphere Application"
            + " Server you may be able to resolve this problem by setting"
            + " com.ibm.ws.webcontainer.invokeFlushAfterService to false").formatted(getDescription(request));
    if (ex == null) {
      log.error(message);
    }
    else {
      // User might see the error page without all the data here but throwing the
      // exception isn't going to help anyone (we'll log it to be on the safe side)
      log.error(message, ex);
    }
  }

  private boolean isClientAbortException(Throwable ex) {
    if (ex == null) {
      return false;
    }
    for (Class<?> candidate : CLIENT_ABORT_EXCEPTIONS) {
      if (candidate.isInstance(ex)) {
        return true;
      }
    }
    return isClientAbortException(ex.getCause());
  }

  private String getErrorPath(Map<Integer, String> map, Integer status) {
    if (map.containsKey(status)) {
      return map.get(status);
    }
    return this.global;
  }

  private String getErrorPath(Class<?> type) {
    while (type != Object.class) {
      String path = this.exceptions.get(type);
      if (path != null) {
        return path;
      }
      type = type.getSuperclass();
    }
    return this.global;
  }

  private void setErrorAttributes(HttpServletRequest request, int status, String message) {
    request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, status);
    request.setAttribute(RequestDispatcher.ERROR_MESSAGE, message);
    request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, request.getRequestURI());
  }

  private void rethrow(Throwable ex) throws IOException, ServletException {
    if (ex instanceof RuntimeException) {
      throw (RuntimeException) ex;
    }
    if (ex instanceof Error) {
      throw (Error) ex;
    }
    if (ex instanceof IOException) {
      throw (IOException) ex;
    }
    if (ex instanceof ServletException) {
      throw (ServletException) ex;
    }
    throw new IllegalStateException(ex);
  }

  @Override
  public void addErrorPages(ErrorPage... errorPages) {
    for (ErrorPage errorPage : errorPages) {
      if (errorPage.isGlobal()) {
        this.global = errorPage.getPath();
      }
      else if (errorPage.getStatus() != null) {
        this.statuses.put(errorPage.getStatus().value(), errorPage.getPath());
      }
      else {
        this.exceptions.put(errorPage.getException(), errorPage.getPath());
      }
    }
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 1;
  }

  private static class ErrorWrapperResponse extends HttpServletResponseWrapper {

    private int status;

    private String message;

    private boolean hasErrorToSend = false;

    ErrorWrapperResponse(HttpServletResponse response) {
      super(response);
    }

    @Override
    public void sendError(int status) throws IOException {
      sendError(status, null);
    }

    @Override
    public void sendError(int status, String message) throws IOException {
      this.status = status;
      this.message = message;
      this.hasErrorToSend = true;
      // Do not call super because the container may prevent us from handling the
      // error ourselves
    }

    @Override
    public int getStatus() {
      if (this.hasErrorToSend) {
        return this.status;
      }
      // If there was no error we need to trust the wrapped response
      return super.getStatus();
    }

    @Override
    public void flushBuffer() throws IOException {
      sendErrorIfNecessary();
      super.flushBuffer();
    }

    private void sendErrorIfNecessary() throws IOException {
      if (this.hasErrorToSend && !isCommitted()) {
        ((HttpServletResponse) getResponse()).sendError(this.status, this.message);
      }
    }

    String getMessage() {
      return this.message;
    }

    boolean hasErrorToSend() {
      return this.hasErrorToSend;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
      sendErrorIfNecessary();
      return super.getWriter();
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
      sendErrorIfNecessary();
      return super.getOutputStream();
    }

  }

}
