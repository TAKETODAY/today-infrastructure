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

package cn.taketoday.session.config;

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
import cn.taketoday.context.annotation.Role;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.session.CookieSessionIdResolver;
import cn.taketoday.session.DefaultSessionManager;
import cn.taketoday.session.InMemorySessionRepository;
import cn.taketoday.session.SecureRandomSessionIdGenerator;
import cn.taketoday.session.SessionEventDispatcher;
import cn.taketoday.session.SessionIdGenerator;
import cn.taketoday.session.SessionIdResolver;
import cn.taketoday.session.SessionManager;
import cn.taketoday.session.SessionMethodArgumentResolver;
import cn.taketoday.session.SessionRepository;
import cn.taketoday.session.WebSessionAttributeParameterResolver;
import cn.taketoday.session.WebSessionListener;
import cn.taketoday.stereotype.Component;
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

@DisableAllDependencyInjection
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SessionProperties.class)
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
  @ConditionalOnMissingBean
  SessionEventDispatcher sessionEventDispatcher(ObjectProvider<WebSessionListener> provider) {
    List<WebSessionListener> listeners = provider.orderedStream().toList();
    return new SessionEventDispatcher(listeners);
  }

  /**
   * @since 4.0
   */
  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  WebSessionAttributeParameterResolver webSessionAttributeMethodArgumentResolver(SessionManager sessionManager, ConfigurableBeanFactory beanFactory) {
    return new WebSessionAttributeParameterResolver(sessionManager, beanFactory);
  }

  /**
   * @since 4.0
   */
  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  SessionMethodArgumentResolver webSessionMethodArgumentResolver(SessionManager sessionManager) {
    return new SessionMethodArgumentResolver(sessionManager);
  }

  /**
   * default {@link SessionRepository} bean
   *
   * @since 3.0
   */
  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @ConditionalOnMissingBean(SessionRepository.class)
  InMemorySessionRepository memorySessionRepository(
          SessionProperties properties,
          SessionEventDispatcher eventDispatcher, SessionIdGenerator idGenerator) {
    var repository = new InMemorySessionRepository(eventDispatcher, idGenerator);
    repository.setMaxSessions(properties.getMaxSessions());
    return repository;
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
   * default {@link SessionIdResolver} bean
   *
   * @since 3.0
   */
  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @ConditionalOnMissingBean(SessionIdResolver.class)
  CookieSessionIdResolver cookieSessionIdResolver(SessionProperties sessionProperties) {
    return new CookieSessionIdResolver(sessionProperties.getCookie());
  }

  @Component(RedirectModelManager.BEAN_NAME)
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @ConditionalOnMissingBean(value = RedirectModelManager.class, name = RedirectModelManager.BEAN_NAME)
  SessionRedirectModelManager sessionRedirectModelManager(SessionManager sessionManager) {
    return new SessionRedirectModelManager(sessionManager);
  }

}
