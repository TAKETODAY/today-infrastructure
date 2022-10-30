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

import cn.taketoday.beans.factory.ObjectProvider;
import cn.taketoday.beans.factory.annotation.DisableAllDependencyInjection;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.support.MergedBeanDefinitionPostProcessor;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.MissingBean;
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
import cn.taketoday.session.WebSessionAttributeListener;
import cn.taketoday.session.WebSessionAttributeParameterResolver;
import cn.taketoday.session.WebSessionListener;
import cn.taketoday.stereotype.Component;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.context.support.SessionScope;
import cn.taketoday.web.view.RedirectModelManager;
import cn.taketoday.web.view.SessionRedirectModelManager;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/30 22:54
 */
@DisableAllDependencyInjection
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SessionProperties.class)
class WebSessionConfiguration implements MergedBeanDefinitionPostProcessor {

  volatile boolean destructionCallbackRegistered;

  final ObjectProvider<SessionEventDispatcher> eventDispatcher;

  WebSessionConfiguration(ObjectProvider<SessionEventDispatcher> eventDispatcher) {
    this.eventDispatcher = eventDispatcher;
  }

  /**
   * @param beanDefinition the merged bean definition for the bean
   * @param bean the actual type of the managed bean instance
   * @param beanName the name of the bean
   * @since 4.0
   */
  @Override
  public synchronized void postProcessMergedBeanDefinition(
          RootBeanDefinition beanDefinition, Object bean, String beanName) {

    // register SessionScope automatically
    if (!destructionCallbackRegistered
            && RequestContext.SCOPE_SESSION.equals(beanDefinition.getScope())) {
      SessionEventDispatcher dispatcher = eventDispatcher.getIfAvailable();
      if (dispatcher != null) {
        dispatcher.addAttributeListeners(SessionScope.createDestructionCallback());
      }
      destructionCallbackRegistered = true;
    }
  }

  /**
   * default {@link SessionManager} bean
   */
  @Component(SessionManager.BEAN_NAME)
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @ConditionalOnMissingBean(value = SessionManager.class, name = SessionManager.BEAN_NAME)
  static DefaultSessionManager webSessionManager(
          SessionIdResolver sessionIdResolver, SessionRepository repository) {
    return new DefaultSessionManager(repository, sessionIdResolver);
  }

  @MissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  static SessionEventDispatcher sessionEventDispatcher(
          ObjectProvider<WebSessionListener> webSessionListeners,
          ObjectProvider<WebSessionAttributeListener> webSessionAttributeListeners) {

    SessionEventDispatcher eventDispatcher = new SessionEventDispatcher();
    eventDispatcher.addSessionListeners(webSessionListeners.orderedStream().toList());
    eventDispatcher.addAttributeListeners(webSessionAttributeListeners.stream().toList());
    return eventDispatcher;
  }

  /**
   * @since 4.0
   */
  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  static WebSessionAttributeParameterResolver webSessionAttributeMethodArgumentResolver(
          SessionManager sessionManager, ConfigurableBeanFactory beanFactory) {
    return new WebSessionAttributeParameterResolver(sessionManager, beanFactory);
  }

  /**
   * @since 4.0
   */
  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  static SessionMethodArgumentResolver webSessionMethodArgumentResolver(SessionManager sessionManager) {
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
  static InMemorySessionRepository memorySessionRepository(SessionProperties properties,
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
  static SessionIdGenerator sessionIdGenerator(SessionProperties sessionProperties) {
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
  static CookieSessionIdResolver cookieSessionIdResolver(SessionProperties sessionProperties) {
    return new CookieSessionIdResolver(sessionProperties.getCookie());
  }

  @Component(RedirectModelManager.BEAN_NAME)
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @ConditionalOnMissingBean(value = RedirectModelManager.class, name = RedirectModelManager.BEAN_NAME)
  static SessionRedirectModelManager sessionRedirectModelManager(SessionManager sessionManager) {
    return new SessionRedirectModelManager(sessionManager);
  }

}
