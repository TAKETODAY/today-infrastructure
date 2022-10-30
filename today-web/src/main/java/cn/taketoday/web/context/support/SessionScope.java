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

import java.util.Objects;
import java.util.function.Supplier;

import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.config.Scope;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.session.SessionManager;
import cn.taketoday.session.WebSession;
import cn.taketoday.session.WebSessionAttributeListener;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.RequestContextUtils;
import cn.taketoday.web.util.WebUtils;

/**
 * Session-backed {@link Scope} implementation.
 *
 * <p>Relies on a thread-bound {@link RequestContext} instance
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see SessionManager
 * @since 4.0 2022/2/21 11:40
 */
public class SessionScope extends AbstractRequestContextScope<WebSession> {

  private final ConfigurableBeanFactory beanFactory;

  @Nullable
  private SessionManager sessionManager;

  private boolean managerLoaded;

  /**
   * Constant identifying the {@link String} prefixed to the name of a
   * destruction callback when it is stored in a {@link WebSession}.
   */
  public static final String DESTRUCTION_CALLBACK_NAME_PREFIX =
          SessionScope.class.getName() + ".DESTRUCTION_CALLBACK.";

  public SessionScope(ConfigurableBeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  @Override
  public String getConversationId() {
    RequestContext context = RequestContextHolder.getRequired();
    WebSession session = getSession(context, false);
    if (session != null) {
      return session.getId();
    }
    return null;
  }

  @Override
  public Object get(String name, Supplier<?> objectFactory) {
    RequestContext context = RequestContextHolder.getRequired();
    WebSession session = getSession(context);
    Object sessionMutex = WebUtils.getSessionMutex(session);
    synchronized(sessionMutex) {
      return doGetBean(session, name, objectFactory);
    }
  }

  @Override
  @Nullable
  public Object remove(String name) {
    RequestContext context = RequestContextHolder.getRequired();
    WebSession session = getSession(context);
    if (session != null) {
      Object sessionMutex = WebUtils.getSessionMutex(session);
      synchronized(sessionMutex) {
        return remove(session, name);
      }
    }
    return null;
  }

  /**
   * Returns the current session associated with this request, or if the request
   * does not have a session, creates one.
   *
   * @param context Current request
   * @return the <code>WebSession</code> associated with this request
   * @see #getSession(RequestContext, boolean)
   */
  private WebSession getSession(RequestContext context) {
    return getSession(context, true);
  }

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
   * @param request Current request
   * @param create <code>true</code> to create a new session for this request if
   * necessary; <code>false</code> to return <code>null</code> if
   * there's no current session
   * @return the <code>WebSession</code> associated with this request or
   * <code>null</code> if <code>create</code> is <code>false</code> and
   * the request has no valid session
   * @see #getSession(RequestContext)
   */
  private WebSession getSession(RequestContext request, boolean create) {
    SessionManager sessionManager = this.sessionManager;
    if (sessionManager == null) {
      Assert.state(!managerLoaded, "No SessionManager in context");
      this.managerLoaded = true;
      sessionManager = BeanFactoryUtils.find(
              beanFactory, SessionManager.BEAN_NAME, SessionManager.class);
      if (sessionManager == null) {
        sessionManager = BeanFactoryUtils.find(beanFactory, SessionManager.class);
        if (sessionManager == null) {
          sessionManager = RequestContextUtils.getSessionManager(request);
          if (sessionManager == null) {
            throw new IllegalStateException("No SessionManager in context");
          }
        }
      }
      this.sessionManager = sessionManager;
    }
    return sessionManager.getSession(request, create);
  }

  @Override
  protected void setAttribute(WebSession context, String beanName, Object scopedObject) {
    context.setAttribute(beanName, scopedObject);
  }

  @Override
  protected Object getAttribute(String beanName, WebSession context) {
    return context.getAttribute(beanName);
  }

  @Override
  protected void removeAttribute(WebSession context, String name) {
    context.removeAttribute(name);
  }

  @Nullable
  @Override
  public Object resolveContextualObject(String key) {
    if (RequestContext.SCOPE_REQUEST.equals(key)) {
      return RequestContextHolder.get();
    }
    else if (RequestContext.SCOPE_SESSION.equals(key)) {
      RequestContext context = RequestContextHolder.get();
      if (context != null) {
        return getSession(context, false);
      }
    }
    return null;
  }

  @Override
  public void registerDestructionCallback(String name, Runnable callback) {
    RequestContext context = RequestContextHolder.getRequired();
    WebSession session = getSession(context);
    session.setAttribute(getDestructionCallbackName(name),
            new DestructionCallbackBindingListener(callback));
  }

  private static String getDestructionCallbackName(String name) {
    return DESTRUCTION_CALLBACK_NAME_PREFIX + name;
  }

  public static WebSessionAttributeListener createDestructionCallback() {
    return new DestructionCallback();
  }

  static final class DestructionCallback implements WebSessionAttributeListener {

    @Override
    public void attributeRemoved(WebSession session, String attributeName, @Nullable Object value) {
      // notify DestructionCallbackBindingListener
      String destructionName = getDestructionCallbackName(attributeName);
      if (!Objects.equals(destructionName, attributeName)) {
        session.removeAttribute(destructionName);
      }
    }
  }
}
