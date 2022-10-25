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

package cn.taketoday.web.context.support;

import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.context.ApplicationEventPublisher;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestCompletedListener;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextUtils;
import cn.taketoday.web.config.WebMvcProperties;

/**
 * publish {@link RequestHandledEvent}
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see RequestHandledEvent
 * @see WebMvcProperties#isPublishRequestHandledEvents()
 * @since 4.0 2022/5/11 10:44
 */
public class RequestHandledEventPublisher implements RequestCompletedListener {

  protected final ApplicationEventPublisher eventPublisher;

  public RequestHandledEventPublisher(ApplicationEventPublisher eventPublisher) {
    Assert.notNull(eventPublisher, "ApplicationEventPublisher is required");
    this.eventPublisher = eventPublisher;
  }

  @Override
  public void requestCompleted(RequestContext request, @Nullable Throwable failureCause) {
    // Whether we succeeded, publish an event.
    var event = getRequestHandledEvent(request, failureCause);
    eventPublisher.publishEvent(event);
  }

  /**
   * create a {@link RequestHandledEvent} for the given request.
   *
   * @param request request context
   * @param failureCause failure cause
   * @return the event
   */
  protected ApplicationEvent getRequestHandledEvent(
          RequestContext request, @Nullable Throwable failureCause) {
    return new RequestHandledEvent(this, request.getRequestURI(), request.getRemoteAddress(),
            request.getMethodValue(), RequestContextUtils.getSessionId(request), null,
            request.getRequestProcessingTime(), failureCause, request.getStatus());
  }

}
