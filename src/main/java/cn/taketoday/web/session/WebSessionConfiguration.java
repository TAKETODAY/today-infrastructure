/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.Singleton;

/**
 * @author TODAY <br>
 *         2019-10-03 00:30
 */
public class WebSessionConfiguration {

    @Singleton
    @Import({ WebSessionParameterResolver.class, WebSessionAttributeParameterResolver.class })
    public DefaultWebSessionManager webSessionManager(@Autowired(required = false) TokenResolver tokenResolver,
                                                      @Autowired(required = false) WebSessionStorage sessionStorage) {

        final TokenResolver tokenResolverToUse = tokenResolver == null
                ? new CookieTokenResolver()
                : tokenResolver;

        final WebSessionStorage sessionStorageToUse = sessionStorage == null
                ? new MemWebSessionStorage()
                : sessionStorage;

        return new DefaultWebSessionManager(tokenResolverToUse, sessionStorageToUse);
    }

}
