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

package infra.web.context.support;

import org.jspecify.annotations.Nullable;

import java.io.Serial;

import infra.context.ApplicationEvent;

/**
 * Event raised when a request is handled within an ApplicationContext.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see infra.context.ApplicationContext#publishEvent
 * @since 4.0 2022/4/15 13:16
 */
public class RequestHandledEvent extends ApplicationEvent {

  @Serial
  private static final long serialVersionUID = 1L;

  /** Session id that applied to the request, if any. */
  @Nullable
  private final String sessionId;

  /** Request processing time. */
  private final long processingTimeMillis;

  /** Cause of failure, if any. */
  @Nullable
  private final Throwable notHandled;

  /** URL that triggered the request. */
  private final String requestUrl;

  /** IP address that the request came from. */
  private final String clientAddress;

  /** Usually GET or POST. */
  private final String method;

  /** HTTP status code of the response. */
  private final int statusCode;

  /**
   * Create a new RequestHandledEvent.
   *
   * @param source the component that published the event
   * @param requestUrl the URL of the request
   * @param clientAddress the IP address that the request came from
   * @param method the HTTP method of the request (usually GET or POST)
   * @param sessionId the id of the HTTP session, if any
   * @param processingTimeMillis the processing time of the request in milliseconds
   * @param notHandled the cause of failure, if any
   * @param statusCode the HTTP status code of the response
   */
  public RequestHandledEvent(Object source, String requestUrl,
          String clientAddress, String method, @Nullable String sessionId,
          long processingTimeMillis, @Nullable Throwable notHandled, int statusCode) {
    super(source);

    this.method = method;
    this.sessionId = sessionId;
    this.statusCode = statusCode;
    this.requestUrl = requestUrl;
    this.notHandled = notHandled;
    this.clientAddress = clientAddress;
    this.processingTimeMillis = processingTimeMillis;
  }

  /**
   * Return the processing time of the request in milliseconds.
   */
  public long getProcessingTimeMillis() {
    return this.processingTimeMillis;
  }

  /**
   * Return the id of the HTTP session, if any.
   */
  @Nullable
  public String getSessionId() {
    return this.sessionId;
  }

  /**
   * Return whether the request failed.
   */
  public boolean wasFailure() {
    return notHandled != null;
  }

  /**
   * Return the cause of failure, if any.
   */
  @Nullable
  public Throwable getFailureCause() {
    return this.notHandled;
  }

  /**
   * Return the URL of the request.
   */
  public String getRequestUrl() {
    return this.requestUrl;
  }

  /**
   * Return the IP address that the request came from.
   */
  public String getClientAddress() {
    return this.clientAddress;
  }

  /**
   * Return the HTTP method of the request (usually GET or POST).
   */
  public String getMethod() {
    return this.method;
  }

  /**
   * Return the HTTP status code of the response or -1 if the status
   * code is not available.
   */
  public int getStatusCode() {
    return this.statusCode;
  }

  /**
   * Return a short description of this event, only involving
   * the most important context data.
   */
  public String getShortDescription() {
    return "url=[%s]; client=[%s]; session=[%s]; ".formatted(getRequestUrl(), getClientAddress(), sessionId);
  }

  /**
   * Return a full description of this event, involving
   * all available context data.
   */
  public String getDescription() {
    StringBuilder sb = new StringBuilder();
    sb.append("url=[").append(getRequestUrl()).append("]; ");
    sb.append("client=[").append(getClientAddress()).append("]; ");
    sb.append("method=[").append(getMethod()).append("]; ");
    sb.append("session=[").append(sessionId).append("]; ");
    sb.append("time=[").append(processingTimeMillis).append("ms]; ");
    sb.append("status=[");
    if (!wasFailure()) {
      sb.append("OK");
    }
    else {
      sb.append("failed: ").append(this.notHandled);
    }
    sb.append(']');
    return sb.toString();
  }

  @Override
  public String toString() {
    return "RequestHandledEvent: " + getDescription();
  }

}
