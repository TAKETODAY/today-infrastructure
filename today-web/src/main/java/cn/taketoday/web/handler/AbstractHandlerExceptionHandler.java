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

package cn.taketoday.web.handler;

import java.util.Set;
import java.util.function.Predicate;

import cn.taketoday.core.Ordered;
import cn.taketoday.core.OrderedSupport;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.LogFormatUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.HandlerExceptionHandler;
import cn.taketoday.web.RequestContext;

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

  /** Logger available to subclasses. */
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Nullable
  private Set<?> mappedHandlers;

  @Nullable
  private Class<?>[] mappedHandlerClasses;

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
  @Nullable
  protected Class<?>[] getMappedHandlerClasses() {
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
      if (result != null && result != NONE_RETURN_VALUE) {
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
