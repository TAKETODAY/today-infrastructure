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

package cn.taketoday.web.socket.server.support;

import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.socket.WebSocketHandler;
import cn.taketoday.web.socket.WebSocketSession;
import cn.taketoday.web.socket.server.HandshakeInterceptor;
import jakarta.servlet.http.HttpSession;

/**
 * An interceptor to copy information from the HTTP session to the "handshake
 * attributes" map to be made available via {@link WebSocketSession#getAttributes()}.
 *
 * <p>Copies a subset or all Web session attributes and/or the Web session ID
 * under the key {@link #HTTP_SESSION_ID_ATTR_NAME}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class WebSessionHandshakeInterceptor implements HandshakeInterceptor {

  /**
   * The name of the attribute under which the WEB session id is exposed when
   * {@link #setCopySessionId(boolean) copySessionId} is "true".
   */
  public static final String HTTP_SESSION_ID_ATTR_NAME = "WEB.SESSION.ID";

  private final Collection<String> attributeNames;

  private boolean copyAllAttributes;

  private boolean copySessionId = true;

  private boolean createSession;

  /**
   * Default constructor for copying all HTTP session attributes and the HTTP
   * session id.
   *
   * @see #setCopyAllAttributes
   * @see #setCopySessionId
   */
  public WebSessionHandshakeInterceptor() {
    this.attributeNames = Collections.emptyList();
    this.copyAllAttributes = true;
  }

  /**
   * Constructor for copying specific session attributes and the session id.
   *
   * @param attributeNames session attributes to copy
   * @see #setCopyAllAttributes
   * @see #setCopySessionId
   */
  public WebSessionHandshakeInterceptor(Collection<String> attributeNames) {
    this.attributeNames = Collections.unmodifiableCollection(attributeNames);
    this.copyAllAttributes = false;
  }

  /**
   * Return the configured attribute names to copy (read-only).
   */
  public Collection<String> getAttributeNames() {
    return this.attributeNames;
  }

  /**
   * Whether to copy all attributes from the session. If set to "true",
   * any explicitly configured attribute names are ignored.
   * <p>By default this is set to either "true" or "false" depending on which
   * constructor was used (default or with attribute names respectively).
   *
   * @param copyAllAttributes whether to copy all attributes
   */
  public void setCopyAllAttributes(boolean copyAllAttributes) {
    this.copyAllAttributes = copyAllAttributes;
  }

  /**
   * Whether to copy all session attributes.
   */
  public boolean isCopyAllAttributes() {
    return this.copyAllAttributes;
  }

  /**
   * Whether the HTTP session id should be copied to the handshake attributes
   * under the key {@link #HTTP_SESSION_ID_ATTR_NAME}.
   * <p>By default this is "true".
   *
   * @param copyHttpSessionId whether to copy the HTTP session id.
   */
  public void setCopySessionId(boolean copyHttpSessionId) {
    this.copySessionId = copyHttpSessionId;
  }

  /**
   * Whether to copy the HTTP session id to the handshake attributes.
   */
  public boolean isCopySessionId() {
    return this.copySessionId;
  }

  /**
   * Whether to allow the HTTP session to be created while accessing it.
   * <p>By default set to {@code false}.
   *
   * @see jakarta.servlet.http.HttpServletRequest#getSession(boolean)
   */
  public void setCreateSession(boolean createSession) {
    this.createSession = createSession;
  }

  /**
   * Whether the HTTP session is allowed to be created.
   */
  public boolean isCreateSession() {
    return this.createSession;
  }

  @Override
  public boolean beforeHandshake(RequestContext request,
          WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

    HttpSession session = getSession(request);
    if (session != null) {
      if (isCopySessionId()) {
        attributes.put(HTTP_SESSION_ID_ATTR_NAME, session.getId());
      }
      Enumeration<String> names = session.getAttributeNames();
      while (names.hasMoreElements()) {
        String name = names.nextElement();
        if (isCopyAllAttributes() || getAttributeNames().contains(name)) {
          attributes.put(name, session.getAttribute(name));
        }
      }
    }
    return true;
  }

  @Nullable
  private HttpSession getSession(RequestContext request) {
    return null;
  }

  @Override
  public void afterHandshake(RequestContext request,
          WebSocketHandler wsHandler, @Nullable Exception ex) {
  }

}
