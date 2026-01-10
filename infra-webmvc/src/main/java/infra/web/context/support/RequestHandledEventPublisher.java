/*
 * Copyright 2017 - 2026 the TODAY authors.
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
