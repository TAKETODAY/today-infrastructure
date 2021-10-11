/**
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

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import cn.taketoday.beans.Lazy;
import cn.taketoday.context.Props;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.web.view.RedirectModelManager;
import cn.taketoday.web.view.SessionRedirectModelManager;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author TODAY 2019-10-03 00:30
 */
@Retention(RUNTIME)
@Target({ TYPE, METHOD })
@Import(WebSessionConfiguration.class)
public @interface EnableWebSession {

}

class WebSessionConfiguration {

  /**
   * default {@link WebSessionManager} bean
   */
  @MissingBean(type = WebSessionManager.class)
  @Import({ WebSessionParameterResolver.class, WebSessionAttributeParameterResolver.class })
  DefaultWebSessionManager webSessionManager(
          TokenResolver tokenResolver, WebSessionStorage sessionStorage) {
    return new DefaultWebSessionManager(tokenResolver, sessionStorage);
  }

  /**
   * default {@link WebSessionStorage} bean
   *
   * @since 3.0
   */
  @MissingBean(type = WebSessionStorage.class)
  MemWebSessionStorage sessionStorage() {
    return new MemWebSessionStorage();
  }

  /**
   * default {@link SessionCookieConfiguration} bean
   *
   * @since 3.0
   */
  @Lazy
  @MissingBean
  @Props(prefix = "server.session.cookie.")
  SessionCookieConfiguration sessionCookieConfiguration() {
    return new SessionCookieConfiguration();
  }

  @Lazy
  @MissingBean
  @Props(prefix = "server.session.")
  SessionConfiguration sessionConfiguration(SessionCookieConfiguration sessionCookieConfig) {
    return new SessionConfiguration(sessionCookieConfig);
  }

  /**
   * default {@link TokenResolver} bean
   *
   * @since 3.0
   */
  @MissingBean(type = TokenResolver.class)
  CookieTokenResolver tokenResolver(SessionCookieConfiguration config) {
    return new CookieTokenResolver(config);
  }

  @MissingBean(type = RedirectModelManager.class)
  SessionRedirectModelManager sessionRedirectModelManager(WebSessionManager sessionManager) {
    return new SessionRedirectModelManager(sessionManager);
  }

}
