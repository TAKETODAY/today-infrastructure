/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.web.session;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;

/**
 * @author TODAY <br>
 * 2019-09-27 20:24
 */
public interface WebSessionManager {

  /**
   * default bean name
   */
  String BEAN_NAME = "webSessionManager";

  /**
   * create a new session
   */
  WebSession createSession();

  /**
   * create a new session associated with {@link RequestContext}
   */
  WebSession createSession(RequestContext context);

  /**
   * Get a session with given session id
   * <p>
   * If there is not a session,create one.
   * </p>
   */
  WebSession getSession(String id);

  /**
   * Returns the current session associated with this request, or if the request
   * does not have a session, creates one.
   *
   * @param context Current request
   * @return the <code>WebSession</code> associated with this request
   * @see #getSession(RequestContext, boolean)
   */
  WebSession getSession(RequestContext context);

  /**
   * Returns the current <code>WebSession</code> associated with this request or,
   * if there is no current session and <code>create</code> is true, returns a new
   * session.
   *
   * <p>
   * If <code>create</code> is <code>false</code> and the request has no valid
   * <code>WebSession</code>, this method returns <code>null</code>.
   *
   * <p>
   * To make sure the session is properly maintained, you must call this method
   * before the response is committed. If the container is using cookies to
   * maintain session integrity and is asked to create a new session when the
   * response is committed, an IllegalStateException is thrown.
   *
   * @param context Current request
   * @param create <code>true</code> to create a new session for this request if
   * necessary; <code>false</code> to return <code>null</code> if
   * there's no current session
   * @return the <code>WebSession</code> associated with this request or
   * <code>null</code> if <code>create</code> is <code>false</code> and
   * the request has no valid session
   * @see #getSession(RequestContext)
   */
  @Nullable
  WebSession getSession(RequestContext context, boolean create);

}
