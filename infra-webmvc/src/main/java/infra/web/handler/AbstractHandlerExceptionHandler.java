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

package infra.web.handler;

import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.function.Predicate;

import infra.core.Ordered;
import infra.core.OrderedSupport;
import infra.http.HttpHeaders;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.LogFormatUtils;
import infra.util.ObjectUtils;
import infra.util.StringUtils;
import infra.web.HandlerExceptionHandler;
import infra.web.RequestContext;
import infra.web.util.DisconnectedClientHelper;

/**
 * Abstract base class for {@link HandlerExceptionHandler} implementations.
 *
 * <p>Supports mapped {@linkplain #setMappedHandlers handlers} and
 * {@linkplain #setMappedHandlerClasses handler classes} that the handler
 * should be applied to and implements the {@link Ordered} interface.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/2 17:59
 */
public abstract class AbstractHandlerExceptionHandler extends OrderedSupport implements HandlerExceptionHandler {

  /**
   * Log category to use for network failure after a client has gone away.
   *
   * @see DisconnectedClientHelper
   */
  protected static final String DISCONNECTED_CLIENT_LOG_CATEGORY = "infra.web.handler.DisconnectedClient";

  protected static final DisconnectedClientHelper disconnectedClientHelper =
          new DisconnectedClientHelper(DISCONNECTED_CLIENT_LOG_CATEGORY);

  /** Logger available to subclasses. */
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Nullable
  private Set<?> mappedHandlers;

  private Class<?> @Nullable [] mappedHandlerClasses;

  @Nullable
  private Logger warnLogger;

  private boolean preventResponseCaching = false;

  @Nullable
  private Predicate<Object> mappedHandlerPredicate;

  /**
   * Specify the set of handlers that this exception resolver should apply to.
   * <p>If no handler predicate, nor handlers, nor handler classes are set,
   * the exception resolver applies to all handlers.
   *
   * @see #setMappedHandlerPredicate(Predicate)
   */
  public void setMappedHandlers(@Nullable Set<?> mappedHandlers) {
    this.mappedHandlers = mappedHandlers;
  }

  /**
   * Specify the set of classes that this exception resolver should apply to.
   * The resolver will only apply to handlers of the specified types; the
   * specified types may be interfaces or superclasses of handlers as well.
   * <p>If no handler predicate, nor handlers, nor handler classes are set,
   * the exception resolver applies to all handlers.
   *
   * @see #setMappedHandlerPredicate(Predicate)
   */
  public void setMappedHandlerClasses(Class<?>... mappedHandlerClasses) {
    this.mappedHandlerClasses = mappedHandlerClasses;
  }

  /**
   * Alternative to {@link #setMappedHandlerClasses(Class[])}.
   */
  public void addMappedHandlerClass(Class<?> mappedHandlerClass) {
    this.mappedHandlerClasses =
            (this.mappedHandlerClasses != null ?
                    ObjectUtils.addObjectToArray(this.mappedHandlerClasses, mappedHandlerClass) :
                    new Class<?>[] { mappedHandlerClass });
  }

  /**
   * Return the {@link #setMappedHandlerClasses(Class[]) configured} mapped
   * handler classes.
   */
  protected Class<?> @Nullable [] getMappedHandlerClasses() {
    return this.mappedHandlerClasses;
  }

  /**
   * Set the log category for warn logging. The name will be passed to the underlying logger
   * implementation through Commons Logging, getting interpreted as a log category according
   * to the logger's configuration. If {@code null} or empty String is passed, warn logging
   * is turned off.
   * <p>By default there is no warn logging although subclasses like
   * {@link SimpleHandlerExceptionHandler}
   * can change that default. Specify this setting to activate warn logging into a specific
   * category. Alternatively, override the {@link #logException} method for custom logging.
   *
   * @see LoggerFactory#getLogger(String)
   * @see java.util.logging.Logger#getLogger(String)
   */
  public void setWarnLogCategory(String loggerName) {
    this.warnLogger = StringUtils.isNotEmpty(loggerName) ? LoggerFactory.getLogger(loggerName) : null;
  }

  /**
   * Specify whether to prevent HTTP response caching for any view resolved
   * by this exception handler.
   * <p>Default is {@code false}. Switch this to {@code true} in order to
   * automatically generate HTTP response headers that suppress response caching.
   */
  public void setPreventResponseCaching(boolean preventResponseCaching) {
    this.preventResponseCaching = preventResponseCaching;
  }

  /**
   * Use a {@code Predicate} to determine which handlers this exception
   * resolver applies to, including when the request was not mapped in which
   * case the handler is {@code null}.
   * <p>If no handler predicate, nor handlers, nor handler classes are set,
   * the exception resolver applies to all handlers.
   */
  public void setMappedHandlerPredicate(Predicate<Object> predicate) {
    this.mappedHandlerPredicate =
            (this.mappedHandlerPredicate != null ? this.mappedHandlerPredicate.and(predicate) : predicate);
  }

  /**
   * Check whether this handler is supposed to apply (i.e. if the supplied handler
   * matches any of the configured {@linkplain #setMappedHandlers handlers} or
   * {@linkplain #setMappedHandlerClasses handler classes}), and then delegate
   * to the {@link #handleInternal} template method.
   */
  @Nullable
  @Override
  public Object handleException(RequestContext context, Throwable ex, @Nullable Object handler) throws Exception {
    if (shouldApplyTo(context, handler)) {
      prepareResponse(ex, context);
      Object result = handleInternal(context, handler, ex);
      if (result != null && result != NONE_RETURN_VALUE
              && !disconnectedClientHelper.checkAndLogClientDisconnectedException(ex)) {
        // Print debug message when warn logger is not enabled.
        if (logger.isDebugEnabled() && (warnLogger == null || !warnLogger.isWarnEnabled())) {
          logger.debug(buildLogMessage(ex, context) + " to " + result);
        }
        // Explicitly configured warn logger in logException method.
        logException(ex, context);
      }
      return result;
    }
    else {
      return null;
    }
  }

  /**
   * Check whether this handler is supposed to apply to the given handler.
   * <p>The default implementation checks against the configured
   * {@linkplain #setMappedHandlerPredicate(Predicate) handlerPredicate}
   * {@linkplain #setMappedHandlers handlers} and
   * {@linkplain #setMappedHandlerClasses handler classes}, if any.
   *
   * @param request current HTTP request context
   * @param handler the executed handler, or {@code null} if none chosen
   * at the time of the exception (for example, if multipart resolution failed)
   * @return whether this resolved should proceed with resolving the exception
   * for the given request and handler
   * @see #setMappedHandlers
   * @see #setMappedHandlerClasses
   */
  @SuppressWarnings("NullAway")
  protected boolean shouldApplyTo(RequestContext request, @Nullable Object handler) {
    if (this.mappedHandlerPredicate != null) {
      return this.mappedHandlerPredicate.test(handler);
    }
    if (handler != null) {
      if (this.mappedHandlers != null && this.mappedHandlers.contains(handler)) {
        return true;
      }
      if (this.mappedHandlerClasses != null) {
        for (Class<?> handlerClass : this.mappedHandlerClasses) {
          if (handlerClass.isInstance(handler)) {
            return true;
          }
        }
      }
    }
    return !hasHandlerMappings();
  }

  /**
   * Whether there are any handler mappings registered via
   * {@link #setMappedHandlers(Set)} or {@link #setMappedHandlerClasses(Class[])}.
   */
  protected boolean hasHandlerMappings() {
    return this.mappedHandlers != null
            || this.mappedHandlerClasses != null
            || this.mappedHandlerPredicate != null;
  }

  /**
   * Log the given exception at warn level, provided that warn logging has been
   * activated through the {@link #setWarnLogCategory "warnLogCategory"} property.
   * <p>Calls {@link #buildLogMessage} in order to determine the concrete message to log.
   *
   * @param ex the exception that got thrown during handler execution
   * @param request current HTTP request (useful for obtaining metadata)
   * @see #setWarnLogCategory
   * @see #buildLogMessage
   * @see Logger#warn(Object, Throwable)
   */
  protected void logException(Throwable ex, RequestContext request) {
    if (warnLogger != null && warnLogger.isWarnEnabled()) {
      warnLogger.warn(buildLogMessage(ex, request));
    }
  }

  /**
   * Build a log message for the given exception, occurred during processing the given request.
   *
   * @param ex the exception that got thrown during handler execution
   * @param request current HTTP request (useful for obtaining metadata)
   * @return the log message to use
   */
  protected String buildLogMessage(Throwable ex, RequestContext request) {
    return "Resolved [%s]".formatted(LogFormatUtils.formatValue(ex, -1, true));
  }

  /**
   * record log when an exception occurred in this exception handler
   *
   * @param target Throwable that occurred in request handler
   * @param handlerException Throwable occurred in this exception handler
   */
  protected void logResultedInException(Throwable target, Throwable handlerException) {
    logger.warn("Failure while trying to resolve exception [{}]", target.getClass().getName(), handlerException);
  }

  /**
   * Prepare the response for the exceptional case.
   * <p>The default implementation prevents the response from being cached,
   * if the {@link #setPreventResponseCaching "preventResponseCaching"} property
   * has been set to "true".
   *
   * @param ex the exception that got thrown during handler execution
   * @param response current HTTP response
   * @see #preventCaching
   */
  protected void prepareResponse(Throwable ex, RequestContext response) {
    if (this.preventResponseCaching) {
      preventCaching(response);
    }
  }

  /**
   * Prevents the response from being cached, through setting corresponding
   * HTTP {@code Cache-Control: no-store} header.
   *
   * @param response current HTTP response
   */
  protected void preventCaching(RequestContext response) {
    response.addHeader(HttpHeaders.CACHE_CONTROL, "no-store");
  }

  /**
   * Actually handle the given exception that got thrown during handler execution,
   * returning a {@link Object result} that represents a specific error page if appropriate.
   * <p>May be overridden in subclasses, in order to apply specific exception checks.
   * Note that this template method will be invoked <i>after</i> checking whether this
   * resolved applies ("mappedHandlers" etc), so an implementation may simply proceed
   * with its actual exception handling.
   *
   * @param request current HTTP request context
   * @param handler the executed handler, or {@code null} if none chosen at the time
   * of the exception (for example, if lookup handler failed)
   * @param ex the exception that got thrown during handler execution
   * @return a corresponding {@code Object view} to forward to,
   * or {@code null} for default processing in the resolution chain
   */
  @Nullable
  protected abstract Object handleInternal(RequestContext request, @Nullable Object handler, Throwable ex)
          throws Exception;

}
