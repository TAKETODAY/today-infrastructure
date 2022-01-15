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
package cn.taketoday.web.framework;

import java.io.Serializable;
import java.util.function.Supplier;

import cn.taketoday.beans.factory.support.ConfigurableBeanFactory;
import cn.taketoday.context.support.StandardApplicationContext;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.framework.server.WebServer;
import cn.taketoday.web.framework.utils.WebApplicationUtils;
import cn.taketoday.web.session.WebSession;
import cn.taketoday.web.session.WebSessionManager;

/**
 * @author TODAY <br>
 * 2019-11-13 22:07
 */
public class StandardWebServerApplicationContext
        extends StandardApplicationContext implements WebServerApplicationContext {
  private static final Logger log = LoggerFactory.getLogger(StandardWebServerApplicationContext.class);

  private WebServer webServer;

  @Nullable
  private final Class<?> startupClass;

  private String contextPath = Constant.BLANK;

  public StandardWebServerApplicationContext() {
    this.startupClass = null;
  }

  public StandardWebServerApplicationContext(@Nullable Class<?> startupClass) {
    this.startupClass = startupClass;
  }

  public StandardWebServerApplicationContext(@Nullable Class<?> startupClass, String... args) {
    setEnvironment(new StandardWebEnvironment(args));
    this.startupClass = startupClass;
  }

  /**
   * Construct with given {@link ConfigurableEnvironment}
   *
   * @param env {@link ConfigurableEnvironment} instance
   */
  public StandardWebServerApplicationContext(ConfigurableEnvironment env) {
    setEnvironment(env);
    this.startupClass = null;
  }

  @Override
  protected void registerFrameworkComponents(ConfigurableBeanFactory beanFactory) {
    beanFactory.registerDependency(WebSession.class, new WebSessionObjectSupplier());
    beanFactory.registerDependency(RequestContext.class, new RequestContextSupplier());
  }

  @Override
  protected void onRefresh() {
    log.info("Looking For: [{}] Bean.", WebServer.class.getName());

    this.webServer = WebApplicationUtils.obtainWebServer(this);
    super.onRefresh();
  }

  @Override
  public WebServer getWebServer() {
    return webServer;
  }

  @Override
  public Class<?> getStartupClass() {
    return startupClass;
  }

  @Override
  public String getContextPath() {
    final String contextPath = this.contextPath;
    if (contextPath == null) {
      return this.contextPath = Constant.BLANK;
    }
    return contextPath;
  }

  public void setContextPath(String contextPath) {
    this.contextPath = contextPath;
  }

  // @since 3.0
  final class WebSessionObjectSupplier implements Supplier<WebSession>, Serializable {
    WebSessionManager sessionManager;

    private WebSessionManager obtainWebSessionManager() {
      WebSessionManager sessionManager = this.sessionManager;
      if (sessionManager == null) {
        sessionManager = getBean(WebSessionManager.class);
        Assert.state(sessionManager != null, "You must enable web session -> @EnableWebSession");
        this.sessionManager = sessionManager;
      }
      return sessionManager;
    }

    @Override
    public WebSession get() {
      final RequestContext context = RequestContextHolder.currentContext();
      return obtainWebSessionManager().getSession(context);
    }

  }

  final static class RequestContextSupplier implements Supplier<RequestContext>, Serializable {

    @Override
    public RequestContext get() {
      return RequestContextHolder.currentContext();
    }

    @Override
    public String toString() {
      return "Current Request Context";
    }

  }
}
