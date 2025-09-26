/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.handler;

import org.jspecify.annotations.Nullable;

import java.io.IOException;

import infra.context.MessageSource;
import infra.context.MessageSourceAware;
import infra.core.annotation.AnnotatedElementUtils;
import infra.core.i18n.LocaleContextHolder;
import infra.util.StringUtils;
import infra.web.DispatcherHandler;
import infra.web.HandlerExceptionHandler;
import infra.web.RequestContext;
import infra.web.ResponseStatusException;
import infra.web.annotation.ResponseStatus;

/**
 * A {@link HandlerExceptionHandler HandlerExceptionHandler} that uses
 * the {@link ResponseStatus @ResponseStatus} annotation to map exceptions
 * to HTTP status codes.
 *
 * <p>This exception resolver is enabled by default in the
 * {@link DispatcherHandler DispatcherHandler}
 * and the MVC Java config and the MVC namespace.
 *
 * <p>this resolver also looks recursively for {@code @ResponseStatus}
 * present on cause exceptions, and  this resolver supports
 * attribute overrides for {@code @ResponseStatus} in custom composed annotations.
 *
 * <p>this resolver also supports {@link ResponseStatusException}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ResponseStatus
 * @see ResponseStatusException
 * @since 4.0 2022/3/2 21:41
 */
public class ResponseStatusExceptionHandler extends AbstractHandlerExceptionHandler implements MessageSourceAware {

  @Nullable
  private MessageSource messageSource;

  @Override
  public void setMessageSource(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  @Nullable
  @Override
  protected Object handleInternal(RequestContext request, @Nullable Object handler, Throwable ex) {
    try {
      if (ex instanceof ResponseStatusException) {
        return resolveResponseStatusException((ResponseStatusException) ex, request, handler);
      }

      ResponseStatus status = AnnotatedElementUtils.findMergedAnnotation(ex.getClass(), ResponseStatus.class);
      if (status != null) {
        return resolveResponseStatus(status, request, handler, ex);
      }

      if (ex.getCause() instanceof Exception) {
        return handleInternal(request, handler, ex.getCause());
      }
    }
    catch (Exception resolveEx) {
      logResultedInException(ex, resolveEx);
    }
    return null;
  }

  /**
   * Template method that handles the {@link ResponseStatus @ResponseStatus} annotation.
   * <p>The default implementation delegates to {@link #applyStatusAndReason}
   * with the status code and reason from the annotation.
   *
   * @param responseStatus the {@code @ResponseStatus} annotation
   * @param request current HTTP request
   * @param handler the executed handler, or {@code null} if none chosen at the
   * time of the exception, e.g. if multipart resolution failed
   * @param ex the exception
   * @return an empty Object, i.e. exception resolved
   */
  protected Object resolveResponseStatus(ResponseStatus responseStatus, RequestContext request,
          @Nullable Object handler, Throwable ex) throws Exception {

    int statusCode = responseStatus.code().value();
    String reason = responseStatus.reason();
    return applyStatusAndReason(statusCode, reason, request);
  }

  /**
   * Template method that handles an {@link ResponseStatusException}.
   * <p>The default implementation applies the headers from
   * {@link ResponseStatusException#getHeaders()} and delegates to
   * {@link #applyStatusAndReason} with the status code and reason from the
   * exception.
   *
   * @param ex the exception
   * @param request current HTTP request
   * @param handler the executed handler, or {@code null} if none chosen at the
   * time of the exception, e.g. if multipart resolution failed
   * @return an empty Object, i.e. exception resolved
   */
  protected Object resolveResponseStatusException(ResponseStatusException ex,
          RequestContext request, @Nullable Object handler) throws Exception {

    request.addHeaders(ex.getHeaders());
    return applyStatusAndReason(ex.getStatusCode().value(), ex.getReason(), request);
  }

  /**
   * Apply the resolved status code and reason to the response.
   * <p>The default implementation sends a response error using
   * {@link RequestContext#sendError(int)} or
   * {@link RequestContext#sendError(int, String)} if there is a reason
   * and then returns an empty ModelAndView.
   *
   * @param statusCode the HTTP status code
   * @param reason the associated reason (may be {@code null} or empty)
   */
  protected Object applyStatusAndReason(int statusCode, @Nullable String reason, RequestContext request) throws IOException {
    if (StringUtils.hasText(reason)) {
      if (messageSource != null) {
        reason = messageSource.getMessage(reason, null, reason, LocaleContextHolder.getLocale());
      }
      request.sendError(statusCode, reason);
    }
    else {
      request.sendError(statusCode);
    }
    return NONE_RETURN_VALUE;
  }

}
