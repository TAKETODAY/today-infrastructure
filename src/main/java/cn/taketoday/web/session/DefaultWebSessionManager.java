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

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Autowired;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;

/**
 * @author TODAY <br>
 * 2019-09-27 19:58
 */
public class DefaultWebSessionManager implements WebSessionManager {

  private TokenResolver tokenResolver;
  private WebSessionStorage sessionStorage;

  public DefaultWebSessionManager(@Autowired(required = false) TokenResolver tokenResolver) {
    this(tokenResolver, new MemWebSessionStorage());
  }

  public DefaultWebSessionManager(@Autowired(required = false) WebSessionStorage sessionStorage) {
    this(new CookieTokenResolver(), sessionStorage);
  }

  public DefaultWebSessionManager(
          @Autowired(required = false) TokenResolver tokenResolver,
          @Autowired(required = false) WebSessionStorage sessionStorage) //
  {
    if (tokenResolver == null) {
      tokenResolver = new CookieTokenResolver();
    }

    if (sessionStorage == null) {
      sessionStorage = new MemWebSessionStorage();
    }

    setTokenResolver(tokenResolver);
    setSessionStorage(sessionStorage);
  }

  @Override
  public WebSession createSession() {
    String token = generateToken();
    final WebSessionStorage sessionStorage = obtainSessionStorage();
    while (sessionStorage.contains(token)) {
      token = generateToken();
    }

    final WebSession ret = createSessionInternal(token, sessionStorage);
    sessionStorage.store(token, ret);
    return ret;
  }

  /**
   * default is create a {@link DefaultSession},
   * subclasses can override this to customize {@link WebSession} implementation
   *
   * @param token
   *         session ID
   * @param sessionStorage
   *         {@link WebSessionStorage}
   */
  protected WebSession createSessionInternal(final String token, final WebSessionStorage sessionStorage) {
    return new DefaultSession(token, sessionStorage);
  }

  /**
   * create a new session id
   */
  protected String generateToken() {
    return StringUtils.getUUIDString();
  }

  @Override
  public WebSession createSession(final RequestContext context) {
    final WebSession ret = createSession();
    obtainTokenResolver().saveToken(context, ret);
    return ret;
  }

  @Override
  public WebSession getSession(final String id) {
    final WebSessionStorage sessionStorage = obtainSessionStorage();
    WebSession ret = sessionStorage.get(id);
    if (ret == null) {
      ret = createSessionInternal(id, sessionStorage);
      sessionStorage.store(id, ret);
    }
    return ret;
  }

  @Override
  public WebSession getSession(final RequestContext context) {
    return getSession(context, true);
  }

  @Override
  public WebSession getSession(final RequestContext context, final boolean create) {
    final String token = obtainTokenResolver().getToken(context);

    WebSession ret = null;
    if ((StringUtils.isEmpty(token) || (ret = obtainSessionStorage().get(token)) == null) && create) {
      return createSession(context);
    }
    return ret;
  }

  //
  // -------------------------------------------

  private WebSessionStorage obtainSessionStorage() {
    final WebSessionStorage ret = getSessionStorage();
    Assert.state(ret != null, "No WebSessionStorage.");
    return ret;
  }

  private TokenResolver obtainTokenResolver() {
    final TokenResolver ret = getTokenResolver();
    Assert.state(ret != null, "No TokenResolver.");
    return ret;
  }

  public TokenResolver getTokenResolver() {
    return tokenResolver;
  }

  public WebSessionStorage getSessionStorage() {
    return sessionStorage;
  }

  public void setSessionStorage(WebSessionStorage sessionStorage) {
    this.sessionStorage = sessionStorage;
  }

  public void setTokenResolver(TokenResolver tokenResolver) {
    this.tokenResolver = tokenResolver;
  }
}
