/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.session.config;

import java.io.File;

import cn.taketoday.beans.factory.SmartInitializingSingleton;
import cn.taketoday.beans.factory.annotation.DisableAllDependencyInjection;
import cn.taketoday.beans.factory.annotation.DisableDependencyInjection;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.support.MergedBeanDefinitionPostProcessor;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.annotation.Role;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.core.ApplicationTemp;
import cn.taketoday.lang.Nullable;
import cn.taketoday.session.CookieSessionIdResolver;
import cn.taketoday.session.DefaultSessionManager;
import cn.taketoday.session.FileSessionPersister;
import cn.taketoday.session.InMemorySessionRepository;
import cn.taketoday.session.PersistenceSessionRepository;
import cn.taketoday.session.SecureRandomSessionIdGenerator;
import cn.taketoday.session.SessionEventDispatcher;
import cn.taketoday.session.SessionIdGenerator;
import cn.taketoday.session.SessionIdResolver;
import cn.taketoday.session.SessionManager;
import cn.taketoday.session.SessionMethodArgumentResolver;
import cn.taketoday.session.SessionPersister;
import cn.taketoday.session.SessionRedirectModelManager;
import cn.taketoday.session.SessionRepository;
import cn.taketoday.session.WebSessionAttributeListener;
import cn.taketoday.session.WebSessionAttributeParameterResolver;
import cn.taketoday.session.WebSessionListener;
import cn.taketoday.stereotype.Component;
import cn.taketoday.web.RedirectModelManager;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.context.support.SessionScope;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/30 22:54
 */
@DisableDependencyInjection
@DisableAllDependencyInjection
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SessionProperties.class)
class WebSessionConfiguration implements MergedBeanDefinitionPostProcessor, SmartInitializingSingleton {

  volatile boolean destructionCallbackRegistered;

  /**
   * @param beanDefinition the merged bean definition for the bean
   * @param bean the actual type of the managed bean instance
   * @param beanName the name of the bean
   * @since 4.0
   */
  @Override
  public synchronized void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> bean, String beanName) {
    // register SessionScope automatically
    if (!destructionCallbackRegistered
            && RequestContext.SCOPE_SESSION.equals(beanDefinition.getScope())) {
      destructionCallbackRegistered = true;
    }
  }

  /**
   * default {@link SessionManager} bean
   */
  @Component(SessionManager.BEAN_NAME)
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @ConditionalOnMissingBean(value = SessionManager.class, name = SessionManager.BEAN_NAME)
  static DefaultSessionManager webSessionManager(SessionIdResolver sessionIdResolver, SessionRepository repository) {
    return new DefaultSessionManager(repository, sessionIdResolver);
  }

  @MissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  static SessionEventDispatcher sessionEventDispatcher() {
    return new SessionEventDispatcher();
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
   * <p>
   * Enable session persistent when there is a 'sessionPersister' bean
   * or {@link SessionProperties#isPersistent()} is enabled
   *
   * @since 3.0
   */
  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @ConditionalOnMissingBean(SessionRepository.class)
  static SessionRepository sessionRepository(SessionProperties properties,
          SessionEventDispatcher eventDispatcher, SessionIdGenerator idGenerator,
          @Nullable SessionPersister sessionPersister, @Nullable ApplicationTemp applicationTemp) {
    var repository = new InMemorySessionRepository(eventDispatcher, idGenerator);
    repository.setMaxSessions(properties.getMaxSessions());
    repository.setSessionMaxIdleTime(properties.getTimeout());

    if (properties.isPersistent() || sessionPersister != null) {
      if (sessionPersister == null) {
        var filePersister = new FileSessionPersister(repository);
        File validDirectory = properties.getValidStoreDir(applicationTemp);
        filePersister.setDirectory(validDirectory);
        filePersister.setApplicationTemp(applicationTemp);
        sessionPersister = filePersister;
      }
      return new PersistenceSessionRepository(sessionPersister, repository);
    }
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
    generator.setSessionIdLength(sessionProperties.getSessionIdLength());
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
    return new CookieSessionIdResolver(sessionProperties.cookie);
  }

  @Component(RedirectModelManager.BEAN_NAME)
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @ConditionalOnMissingBean(value = RedirectModelManager.class, name = RedirectModelManager.BEAN_NAME)
  static SessionRedirectModelManager sessionRedirectModelManager(SessionManager sessionManager) {
    return new SessionRedirectModelManager(sessionManager);
  }

  @Override
  public void afterSingletonsInstantiated(ConfigurableBeanFactory beanFactory) {
    SessionEventDispatcher eventDispatcher = beanFactory.getBean(SessionEventDispatcher.class);
    eventDispatcher.addSessionListeners(beanFactory.getBeanProvider(WebSessionListener.class).orderedList());
    eventDispatcher.addAttributeListeners(beanFactory.getBeanProvider(WebSessionAttributeListener.class).orderedList());

    if (destructionCallbackRegistered) {
      eventDispatcher.addAttributeListeners(SessionScope.createDestructionCallback());
    }

    SessionRepository sessionRepository = beanFactory.getBean(SessionRepository.class);
    if (sessionRepository instanceof PersistenceSessionRepository repository) {
      eventDispatcher.addSessionListeners(repository.createDestructionCallback());
    }
  }

}
