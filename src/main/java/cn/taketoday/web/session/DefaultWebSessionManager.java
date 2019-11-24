/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
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

import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.RequestContext;

/**
 * @author TODAY <br>
 *         2019-09-27 19:58
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
        if (sessionStorage == null) {
            this.setSessionStorage(new MemWebSessionStorage());
        }
        else {
            this.setSessionStorage(sessionStorage);
        }
        if (tokenResolver == null) {
            this.setTokenResolver(new CookieTokenResolver());
        }
        else {
            this.setTokenResolver(tokenResolver);
        }
    }

    @Override
    public WebSession createSession() {

        String token = StringUtils.getUUIDString();

        final WebSessionStorage sessionStorage = getSessionStorage();
        while (sessionStorage.contains(token)) {
            token = StringUtils.getUUIDString();
        }

        final DefaultSession ret = new DefaultSession(token);
        sessionStorage.store(token, ret);
        return ret;
    }

    @Override
    public WebSession createSession(RequestContext context) {
        final WebSession ret = createSession();

        getTokenResolver().saveToken(context, ret);
        return ret;
    }

    @Override
    public WebSession getSession(String id) {

        final WebSessionStorage sessionStorage = getSessionStorage();
        WebSession ret = sessionStorage.get(id);

        if (ret == null) {
            sessionStorage.store(id, ret = new DefaultSession(id));
        }

        return ret;
    }

    @Override
    public WebSession getSession(RequestContext context) {

        final String token = getTokenResolver().getToken(context);

        final WebSession ret;
        if (StringUtils.isEmpty(token) || (ret = getSessionStorage().get(token)) == null) {
            return createSession(context);
        }
        return ret;
    }

    // 
    // -------------------------------------------

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
