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

import cn.taketoday.lang.Nullable;

/**
 * Servlet-specific subclass of RequestHandledEvent,
 * adding servlet-specific context information.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see cn.taketoday.context.ApplicationContext#publishEvent
 * @since 4.0 2022/4/15 13:17
 */
@SuppressWarnings("serial")
public class ServletRequestHandledEvent extends RequestHandledEvent {

  /** Name of the servlet that handled the request. */
  private final String servletName;

  /**
   * Create a new ServletRequestHandledEvent.
   *
   * @param source the component that published the event
   * @param requestUrl the URL of the request
   * @param clientAddress the IP address that the request came from
   * @param method the HTTP method of the request (usually GET or POST)
   * @param servletName the name of the servlet that handled the request
   * @param sessionId the id of the HTTP session, if any
   * @param userName the name of the user that was associated with the
   * request, if any (usually the UserPrincipal)
   * @param processingTimeMillis the processing time of the request in milliseconds
   */
  public ServletRequestHandledEvent(Object source, String requestUrl,
          String clientAddress, String method, String servletName,
          @Nullable String sessionId, @Nullable String userName, long processingTimeMillis) {
    super(source, requestUrl, clientAddress, method, sessionId, userName, processingTimeMillis);
    this.servletName = servletName;
  }

  /**
   * Create a new ServletRequestHandledEvent.
   *
   * @param source the component that published the event
   * @param requestUrl the URL of the request
   * @param clientAddress the IP address that the request came from
   * @param method the HTTP method of the request (usually GET or POST)
   * @param servletName the name of the servlet that handled the request
   * @param sessionId the id of the HTTP session, if any
   * @param userName the name of the user that was associated with the
   * request, if any (usually the UserPrincipal)
   * @param processingTimeMillis the processing time of the request in milliseconds
   * @param failureCause the cause of failure, if any
   */
  public ServletRequestHandledEvent(Object source, String requestUrl,
          String clientAddress, String method, String servletName, @Nullable String sessionId,
          @Nullable String userName, long processingTimeMillis, @Nullable Throwable failureCause) {
    super(source, requestUrl, clientAddress, method, sessionId, userName, processingTimeMillis, failureCause);
    this.servletName = servletName;
  }

  /**
   * Create a new ServletRequestHandledEvent.
   *
   * @param source the component that published the event
   * @param requestUrl the URL of the request
   * @param clientAddress the IP address that the request came from
   * @param method the HTTP method of the request (usually GET or POST)
   * @param servletName the name of the servlet that handled the request
   * @param sessionId the id of the HTTP session, if any
   * @param userName the name of the user that was associated with the
   * request, if any (usually the UserPrincipal)
   * @param processingTimeMillis the processing time of the request in milliseconds
   * @param failureCause the cause of failure, if any
   * @param statusCode the HTTP status code of the response
   */
  public ServletRequestHandledEvent(Object source, String requestUrl,
          String clientAddress, String method, String servletName, @Nullable String sessionId,
          @Nullable String userName, long processingTimeMillis, @Nullable Throwable failureCause, int statusCode) {
    super(source, requestUrl, clientAddress, method, sessionId, userName, processingTimeMillis, failureCause, statusCode);
    this.servletName = servletName;
  }

  /**
   * Return the name of the servlet that handled the request.
   */
  public String getServletName() {
    return this.servletName;
  }

  @Override
  public String getShortDescription() {
    StringBuilder sb = new StringBuilder();
    sb.append("url=[").append(getRequestUrl()).append("]; ");
    sb.append("client=[").append(getClientAddress()).append("]; ");
    sb.append(super.getShortDescription());
    return sb.toString();
  }

  @Override
  public String getDescription() {
    StringBuilder sb = new StringBuilder();
    sb.append("servlet=[").append(getServletName()).append("]; ");
    sb.append(super.getDescription());
    return sb.toString();
  }

  @Override
  public String toString() {
    return "ServletRequestHandledEvent: " + getDescription();
  }

}
