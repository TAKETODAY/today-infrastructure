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

package infra.web.context.support;

import org.jspecify.annotations.Nullable;

import infra.context.ApplicationEvent;
import infra.context.ApplicationEventPublisher;
import infra.lang.Assert;
import infra.web.RequestCompletedListener;
import infra.web.RequestContext;
import infra.web.RequestContextUtils;

/**
 * publish {@link RequestHandledEvent}
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see RequestHandledEvent
 * @since 4.0 2022/5/11 10:44
 */
public class RequestHandledEventPublisher implements RequestCompletedListener {

  protected final ApplicationEventPublisher eventPublisher;

  public RequestHandledEventPublisher(ApplicationEventPublisher eventPublisher) {
    Assert.notNull(eventPublisher, "ApplicationEventPublisher is required");
    this.eventPublisher = eventPublisher;
  }

  @Override
  public void requestCompleted(RequestContext request, @Nullable Throwable notHandled) {
    // Whether we succeeded, publish an event.
    var event = getRequestHandledEvent(request, notHandled);
    eventPublisher.publishEvent(event);
  }

  /**
   * create a {@link RequestHandledEvent} for the given request.
   *
   * @param request request context
   * @param notHandled failure cause
   * @return the event
   */
  protected ApplicationEvent getRequestHandledEvent(RequestContext request, @Nullable Throwable notHandled) {
    return new RequestHandledEvent(this, request.getRequestURI(), request.getRemoteAddress(),
            request.getMethodAsString(), RequestContextUtils.getSessionId(request),
            request.getRequestProcessingTime(), notHandled, request.getStatus());
  }

}
