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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import cn.taketoday.beans.factory.ObjectProvider;
import cn.taketoday.beans.factory.annotation.DisableAllDependencyInjection;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.Lazy;
import cn.taketoday.context.annotation.Role;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.context.properties.Props;
import cn.taketoday.lang.Component;
import cn.taketoday.web.view.RedirectModelManager;
import cn.taketoday.web.view.SessionRedirectModelManager;

/**
 * Enable web-session supports, like servlet's http-session
 *
 * @author TODAY 2019-10-03 00:30
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Import(WebSessionConfig.class)
public @interface EnableWebSession {

}

@Configuration(proxyBeanMethods = false)
@DisableAllDependencyInjection
class WebSessionConfig {

  /**
   * default {@link SessionManager} bean
   */
  @Component(SessionManager.BEAN_NAME)
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @ConditionalOnMissingBean(value = SessionManager.class, name = SessionManager.BEAN_NAME)
  DefaultSessionManager webSessionManager(
          SessionIdResolver sessionIdResolver, SessionRepository repository) {
    return new DefaultSessionManager(repository, sessionIdResolver);
  }

  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @ConditionalOnMissingBean(value = SessionManager.class, name = SessionManager.BEAN_NAME)
  SessionEventDispatcher sessionEventDispatcher(ObjectProvider<WebSessionListener> provider) {
    List<WebSessionListener> listeners = provider.orderedStream().toList();
    return new SessionEventDispatcher(listeners);
  }

  /**
   * @since 4.0
   */
  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  WebSessionAttributeParameterResolver webSessionAttributeParameterResolver(
          SessionManager sessionManager, ConfigurableBeanFactory beanFactory) {
    return new WebSessionAttributeParameterResolver(sessionManager, beanFactory);
  }

  /**
   * @since 4.0
   */
  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  WebSessionParameterResolver webSessionParameterResolver(
          SessionManager sessionManager) {
    return new WebSessionParameterResolver(sessionManager);
  }

  /**
   * default {@link SessionRepository} bean
   *
   * @since 3.0
   */
  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @ConditionalOnMissingBean(SessionRepository.class)
  MemSessionRepository memorySessionRepository(
          SessionEventDispatcher eventDispatcher, SessionIdGenerator sessionIdGenerator) {
    return new MemSessionRepository(eventDispatcher, sessionIdGenerator);
  }

  /**
   * @since 4.0
   */
  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @ConditionalOnMissingBean(SessionIdGenerator.class)
  SessionIdGenerator sessionIdGenerator(SessionProperties sessionProperties) {
    SecureRandomSessionIdGenerator generator = new SecureRandomSessionIdGenerator();
    generator.setLength(sessionProperties.getSessionIdLength());
    return generator;
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
  SessionProperties sessionProperties(SessionCookieConfig sessionCookieConfig) {
    return new SessionProperties(sessionCookieConfig);
  }

  /**
   * default {@link SessionIdResolver} bean
   *
   * @since 3.0
   */
  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @ConditionalOnMissingBean(SessionIdResolver.class)
  CookieSessionIdResolver webTokenResolver(SessionCookieConfig config) {
    return new CookieSessionIdResolver(config);
  }

  @Component(RedirectModelManager.BEAN_NAME)
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @ConditionalOnMissingBean(value = RedirectModelManager.class, name = RedirectModelManager.BEAN_NAME)
  SessionRedirectModelManager sessionRedirectModelManager(SessionManager sessionManager) {
    return new SessionRedirectModelManager(sessionManager);
  }

}
