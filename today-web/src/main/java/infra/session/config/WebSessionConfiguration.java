/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.session.config;

import org.jspecify.annotations.Nullable;

import java.io.File;

import infra.beans.factory.SmartInitializingSingleton;
import infra.beans.factory.annotation.DisableAllDependencyInjection;
import infra.beans.factory.annotation.DisableDependencyInjection;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.support.MergedBeanDefinitionPostProcessor;
import infra.beans.factory.support.RootBeanDefinition;
import infra.context.annotation.Configuration;
import infra.context.annotation.MissingBean;
import infra.context.annotation.Role;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.properties.EnableConfigurationProperties;
import infra.core.ApplicationTemp;
import infra.session.CookieSessionIdResolver;
import infra.session.DefaultSessionManager;
import infra.session.FileSessionPersister;
import infra.session.InMemorySessionRepository;
import infra.session.PersistenceSessionRepository;
import infra.session.SecureRandomSessionIdGenerator;
import infra.session.SessionAttributeListener;
import infra.session.SessionAttributeParameterResolver;
import infra.session.SessionEventDispatcher;
import infra.session.SessionIdGenerator;
import infra.session.SessionIdResolver;
import infra.session.SessionListener;
import infra.session.SessionManager;
import infra.session.SessionMethodArgumentResolver;
import infra.session.SessionPersister;
import infra.session.SessionRedirectModelManager;
import infra.session.SessionRepository;
import infra.stereotype.Component;
import infra.web.RedirectModelManager;
import infra.web.RequestContext;
import infra.web.context.support.SessionScope;

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
  static SessionAttributeParameterResolver webSessionAttributeMethodArgumentResolver(
          SessionManager sessionManager, ConfigurableBeanFactory beanFactory) {
    return new SessionAttributeParameterResolver(sessionManager, beanFactory);
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
    eventDispatcher.addSessionListeners(beanFactory.getBeanProvider(SessionListener.class).orderedList());
    eventDispatcher.addAttributeListeners(beanFactory.getBeanProvider(SessionAttributeListener.class).orderedList());

    if (destructionCallbackRegistered) {
      eventDispatcher.addAttributeListeners(SessionScope.createDestructionCallback());
    }

    SessionRepository sessionRepository = beanFactory.getBean(SessionRepository.class);
    if (sessionRepository instanceof PersistenceSessionRepository repository) {
      eventDispatcher.addSessionListeners(repository.createDestructionCallback());
    }
  }

}
