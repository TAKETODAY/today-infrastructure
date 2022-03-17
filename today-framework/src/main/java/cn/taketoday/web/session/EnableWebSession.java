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

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import cn.taketoday.beans.factory.annotation.DisableAllDependencyInjection;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.Lazy;
import cn.taketoday.context.properties.Props;
import cn.taketoday.context.annotation.Role;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.lang.Component;
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
@Import(WebSessionConfig.class)
public @interface EnableWebSession {

}

@Configuration(proxyBeanMethods = false)
@DisableAllDependencyInjection
class WebSessionConfig {

  /**
   * default {@link WebSessionManager} bean
   */
  @Component(WebSessionManager.BEAN_NAME)
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @ConditionalOnMissingBean(WebSessionManager.class)
  DefaultWebSessionManager webSessionManager(
          TokenResolver tokenResolver, WebSessionStorage sessionStorage) {
    return new DefaultWebSessionManager(tokenResolver, sessionStorage);
  }

  /**
   * @since 4.0
   */
  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  WebSessionAttributeParameterResolver webSessionAttributeParameterResolver(
          WebSessionManager webSessionManager, ConfigurableBeanFactory beanFactory) {
    return new WebSessionAttributeParameterResolver(webSessionManager, beanFactory);
  }

  /**
   * @since 4.0
   */
  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  WebSessionParameterResolver webSessionParameterResolver(
          WebSessionManager webSessionManager) {
    return new WebSessionParameterResolver(webSessionManager);
  }

  /**
   * default {@link WebSessionStorage} bean
   *
   * @since 3.0
   */
  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @ConditionalOnMissingBean(WebSessionStorage.class)
  MemWebSessionStorage webSessionStorage() {
    return new MemWebSessionStorage();
  }

  /**
   * default {@link SessionCookieConfig} bean
   *
   * @since 3.0
   */
  @Lazy
  @Component
  @ConditionalOnMissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @Props(prefix = "server.session.cookie")
  SessionCookieConfig webSessionCookieConfig() {
    return new SessionCookieConfig();
  }

  @Lazy
  @Component
  @ConditionalOnMissingBean
  @Props(prefix = "server.session")
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  SessionConfiguration webSessionConfig(SessionCookieConfig sessionCookieConfig) {
    return new SessionConfiguration(sessionCookieConfig);
  }

  /**
   * default {@link TokenResolver} bean
   *
   * @since 3.0
   */
  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @ConditionalOnMissingBean(TokenResolver.class)
  CookieTokenResolver webTokenResolver(SessionCookieConfig config) {
    return new CookieTokenResolver(config);
  }

  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @ConditionalOnMissingBean(RedirectModelManager.class)
  SessionRedirectModelManager sessionRedirectModelManager(WebSessionManager sessionManager) {
    return new SessionRedirectModelManager(sessionManager);
  }

}
