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

package cn.taketoday.web.context.support;

import java.io.Serial;

import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.lang.Nullable;

/**
 * Event raised when a request is handled within an ApplicationContext.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see cn.taketoday.context.ApplicationContext#publishEvent
 * @since 4.0 2022/4/15 13:16
 */
public class RequestHandledEvent extends ApplicationEvent {

  @Serial
  private static final long serialVersionUID = 1L;

  /** Session id that applied to the request, if any. */
  @Nullable
  private final String sessionId;

  /** Usually the UserPrincipal. */
  @Nullable
  private final String userName;

  /** Request processing time. */
  private final long processingTimeMillis;

  /** Cause of failure, if any. */
  @Nullable
  private Throwable notHandled;

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
   * @param userName the name of the user that was associated with the
   * request, if any (usually the UserPrincipal)
   * @param processingTimeMillis the processing time of the request in milliseconds
   */
  public RequestHandledEvent(Object source, String requestUrl, String clientAddress, String method,
          @Nullable String sessionId, @Nullable String userName, long processingTimeMillis) {
    super(source);
    this.method = method;
    this.statusCode = -1;
    this.userName = userName;
    this.sessionId = sessionId;
    this.requestUrl = requestUrl;
    this.clientAddress = clientAddress;
    this.processingTimeMillis = processingTimeMillis;
  }

  /**
   * Create a new RequestHandledEvent.
   *
   * @param source the component that published the event
   * @param requestUrl the URL of the request
   * @param clientAddress the IP address that the request came from
   * @param method the HTTP method of the request (usually GET or POST)
   * @param sessionId the id of the HTTP session, if any
   * @param userName the name of the user that was associated with the
   * request, if any (usually the UserPrincipal)
   * @param processingTimeMillis the processing time of the request in milliseconds
   * @param notHandled the cause of failure, if any
   */
  public RequestHandledEvent(Object source, String requestUrl, String clientAddress, String method,
          @Nullable String sessionId, @Nullable String userName, long processingTimeMillis, @Nullable Throwable notHandled) {

    super(source);
    this.method = method;
    this.notHandled = notHandled;
    this.statusCode = -1;
    this.userName = userName;
    this.sessionId = sessionId;
    this.requestUrl = requestUrl;
    this.clientAddress = clientAddress;
    this.processingTimeMillis = processingTimeMillis;
  }

  /**
   * Create a new RequestHandledEvent.
   *
   * @param source the component that published the event
   * @param requestUrl the URL of the request
   * @param clientAddress the IP address that the request came from
   * @param method the HTTP method of the request (usually GET or POST)
   * @param sessionId the id of the HTTP session, if any
   * @param userName the name of the user that was associated with the
   * request, if any (usually the UserPrincipal)
   * @param processingTimeMillis the processing time of the request in milliseconds
   * @param notHandled the cause of failure, if any
   * @param statusCode the HTTP status code of the response
   */
  public RequestHandledEvent(Object source, String requestUrl,
          String clientAddress, String method, @Nullable String sessionId,
          @Nullable String userName, long processingTimeMillis, @Nullable Throwable notHandled, int statusCode) {
    super(source);

    this.method = method;
    this.userName = userName;
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
   * Return the name of the user that was associated with the request
   * (usually the UserPrincipal).
   *
   * @see jakarta.servlet.http.HttpServletRequest#getUserPrincipal()
   */
  @Nullable
  public String getUserName() {
    return this.userName;
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
    StringBuilder sb = new StringBuilder();
    sb.append("url=[").append(getRequestUrl()).append("]; ");
    sb.append("client=[").append(getClientAddress()).append("]; ");
    sb.append("session=[").append(sessionId).append("]; ");
    sb.append("user=[").append(userName).append("]; ");
    return sb.toString();
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
    sb.append("user=[").append(userName).append("]; ");
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
