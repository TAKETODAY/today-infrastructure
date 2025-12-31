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

import org.junit.jupiter.api.Test;

import infra.context.ApplicationEventPublisher;
import infra.web.RequestContext;
import infra.web.RequestContextUtils;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/19 11:39
 */
class RequestHandledEventPublisherTests {

  @Test
  void publishesEventOnRequestCompletion() {
    ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    RequestHandledEventPublisher eventPublisher = new RequestHandledEventPublisher(publisher);
    RequestContext request = mock(RequestContext.class);

    when(request.getRequestURI()).thenReturn("/test");
    when(request.getRemoteAddress()).thenReturn("127.0.0.1");
    when(request.getMethodAsString()).thenReturn("GET");
    when(request.getRequestProcessingTime()).thenReturn(100L);
    when(request.getStatus()).thenReturn(200);

    eventPublisher.requestCompleted(request, null);

    verify(publisher).publishEvent(any(RequestHandledEvent.class));
  }

  @Test
  void publishesEventWithFailureCause() {
    ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    RequestHandledEventPublisher eventPublisher = new RequestHandledEventPublisher(publisher);
    RequestContext request = mock(RequestContext.class);
    RuntimeException failure = new RuntimeException("test");

    when(request.getRequestURI()).thenReturn("/test");
    when(request.getRemoteAddress()).thenReturn("127.0.0.1");
    when(request.getMethodAsString()).thenReturn("POST");
    when(request.getRequestProcessingTime()).thenReturn(200L);
    when(request.getStatus()).thenReturn(500);

    eventPublisher.requestCompleted(request, failure);

    verify(publisher).publishEvent(argThat(event ->
            event instanceof RequestHandledEvent &&
                    ((RequestHandledEvent) event).getFailureCause() == failure));
  }

  @Test
  void nullEventPublisherThrowsException() {
    assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> new RequestHandledEventPublisher(null));
  }

  @Test
  void handlesNullSessionId() {
    ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    RequestHandledEventPublisher eventPublisher = new RequestHandledEventPublisher(publisher);
    RequestContext request = mock(RequestContext.class);

    when(request.getRequestURI()).thenReturn("/test");
    when(request.getRemoteAddress()).thenReturn("127.0.0.1");
    when(request.getMethodAsString()).thenReturn("GET");
    when(request.getRequestProcessingTime()).thenReturn(100L);
    when(request.getStatus()).thenReturn(200);
    when(RequestContextUtils.getSessionId(request)).thenReturn(null);

    eventPublisher.requestCompleted(request, null);

    verify(publisher).publishEvent(argThat(event ->
            event instanceof RequestHandledEvent &&
                    ((RequestHandledEvent) event).getSessionId() == null));
  }
}