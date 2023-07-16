/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.orm.hibernate5;

import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;

import java.util.function.Consumer;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.config.AutowireCapableBeanFactory;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ConcurrentReferenceHashMap;

/**
 * Framework's implementation of Hibernate's {@link BeanContainer} SPI,
 * delegating to a Framework {@link ConfigurableBeanFactory}.
 *
 * <p>Auto-configured by {@link LocalSessionFactoryBean#setBeanFactory},
 * programmatically supported via {@link LocalSessionFactoryBuilder#setBeanContainer},
 * and manually configurable through a "hibernate.resource.beans.container" entry
 * in JPA properties, e.g.:
 *
 * <pre class="code">
 * &lt;bean id="entityManagerFactory" class="cn.taketoday.orm.jpa.LocalContainerEntityManagerFactoryBean"&gt;
 *   ...
 *   &lt;property name="jpaPropertyMap"&gt;
 * 	   &lt;map&gt;
 *       &lt;entry key="hibernate.resource.beans.container"&gt;
 * 	       &lt;bean class="cn.taketoday.orm.hibernate5.HibernateBeanContainer"/&gt;
 * 	     &lt;/entry&gt;
 * 	   &lt;/map&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * Or in Java-based JPA configuration:
 *
 * <pre class="code">
 * LocalContainerEntityManagerFactoryBean emfb = ...
 * emfb.getJpaPropertyMap().put(AvailableSettings.BEAN_CONTAINER, new FrameworkBeanContainer(beanFactory));
 * </pre>
 *
 * Please note that Framework's {@link LocalSessionFactoryBean} is an immediate alternative
 * to {@link cn.taketoday.orm.jpa.LocalContainerEntityManagerFactoryBean} for
 * common JPA purposes: The Hibernate {@code SessionFactory} will natively expose the JPA
 * {@code EntityManagerFactory} interface as well, and Hibernate {@code BeanContainer}
 * integration will be registered out of the box.
 *
 * @author Juergen Hoeller
 * @see LocalSessionFactoryBean#setBeanFactory
 * @see LocalSessionFactoryBuilder#setBeanContainer
 * @see cn.taketoday.orm.jpa.LocalContainerEntityManagerFactoryBean#setJpaPropertyMap
 * @see org.hibernate.cfg.AvailableSettings#BEAN_CONTAINER
 * @since 4.0
 */
public final class HibernateBeanContainer implements BeanContainer {

  private static final Logger log = LoggerFactory.getLogger(HibernateBeanContainer.class);

  private final ConfigurableBeanFactory beanFactory;

  private final ConcurrentReferenceHashMap<Object, HibernateContainedBean<?>> beanCache = new ConcurrentReferenceHashMap<>();

  /**
   * Instantiate a new FrameworkBeanContainer for the given bean factory.
   *
   * @param beanFactory the Framework bean factory to delegate to
   */
  public HibernateBeanContainer(ConfigurableBeanFactory beanFactory) {
    Assert.notNull(beanFactory, "ConfigurableBeanFactory is required");
    this.beanFactory = beanFactory;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <B> ContainedBean<B> getBean(Class<B> beanType,
          LifecycleOptions lifecycleOptions, BeanInstanceProducer fallbackProducer) {
    HibernateContainedBean<?> bean;
    if (lifecycleOptions.canUseCachedReferences()) {
      bean = this.beanCache.get(beanType);
      if (bean == null) {
        bean = createBean(beanType, lifecycleOptions, fallbackProducer);
        this.beanCache.put(beanType, bean);
      }
    }
    else {
      bean = createBean(beanType, lifecycleOptions, fallbackProducer);
    }
    return (HibernateContainedBean<B>) bean;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <B> ContainedBean<B> getBean(String name, Class<B> beanType,
          LifecycleOptions lifecycleOptions, BeanInstanceProducer fallbackProducer) {

    HibernateContainedBean<?> bean;
    if (lifecycleOptions.canUseCachedReferences()) {
      bean = this.beanCache.get(name);
      if (bean == null) {
        bean = createBean(name, beanType, lifecycleOptions, fallbackProducer);
        this.beanCache.put(name, bean);
      }
    }
    else {
      bean = createBean(name, beanType, lifecycleOptions, fallbackProducer);
    }
    return (HibernateContainedBean<B>) bean;
  }

  @Override
  public void stop() {
    this.beanCache.values().forEach(HibernateContainedBean::destroyIfNecessary);
    this.beanCache.clear();
  }

  private HibernateContainedBean<?> createBean(
          Class<?> beanType, LifecycleOptions lifecycleOptions, BeanInstanceProducer fallbackProducer) {

    try {
      if (lifecycleOptions.useJpaCompliantCreation()) {
        return new HibernateContainedBean<>(
                this.beanFactory.createBean(beanType, AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR),
                this.beanFactory::destroyBean);
      }
      else {
        return new HibernateContainedBean<>(beanFactory.getBean(beanType));
      }
    }
    catch (BeansException ex) {
      log.debug("Falling back to Hibernate's default producer after bean creation failure for {}: {}",
              beanType, ex.toString());
      try {
        return new HibernateContainedBean<>(fallbackProducer.produceBeanInstance(beanType));
      }
      catch (RuntimeException ex2) {
        if (ex instanceof BeanCreationException) {
          log.debug("Fallback producer failed for {}: {}", beanType, ex2.toString());
          // Rethrow original Framework exception from first attempt.
          throw ex;
        }
        else {
          // Throw fallback producer exception since original was probably NoSuchBeanDefinitionException.
          throw ex2;
        }
      }
    }
  }

  private HibernateContainedBean<?> createBean(String name, Class<?> beanType,
          LifecycleOptions lifecycleOptions, BeanInstanceProducer fallbackProducer) {
    try {
      if (lifecycleOptions.useJpaCompliantCreation()) {
        Object bean = beanFactory.autowire(beanType, AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR);
        beanFactory.autowireBeanProperties(bean, AutowireCapableBeanFactory.AUTOWIRE_NO);
        beanFactory.applyBeanPropertyValues(bean, name);
        bean = beanFactory.initializeBean(bean, name);
        return new HibernateContainedBean<>(bean, beanInstance -> beanFactory.destroyBean(name, beanInstance));
      }
      else {
        return new HibernateContainedBean<>(beanFactory.getBean(name, beanType));
      }
    }
    catch (BeansException ex) {
      log.debug("Falling back to Hibernate's default producer after bean creation failure for {} with name '{}': {}",
              name, beanType, ex.toString());
      try {
        return new HibernateContainedBean<>(fallbackProducer.produceBeanInstance(name, beanType));
      }
      catch (RuntimeException ex2) {
        if (ex instanceof BeanCreationException) {
          log.debug("Fallback producer failed for {} with name '{}}': {}", beanType, name, ex2.toString());
          // Rethrow original Framework exception from first attempt.
          throw ex;
        }
        else {
          // Throw fallback producer exception since original was probably NoSuchBeanDefinitionException.
          throw ex2;
        }
      }
    }
  }

  private static final class HibernateContainedBean<B> implements ContainedBean<B> {

    private final B beanInstance;

    @Nullable
    private Consumer<B> destructionCallback;

    public HibernateContainedBean(B beanInstance) {
      this.beanInstance = beanInstance;
    }

    public HibernateContainedBean(B beanInstance, Consumer<B> destructionCallback) {
      this.beanInstance = beanInstance;
      this.destructionCallback = destructionCallback;
    }

    @Override
    public B getBeanInstance() {
      return this.beanInstance;
    }

    public void destroyIfNecessary() {
      if (this.destructionCallback != null) {
        this.destructionCallback.accept(this.beanInstance);
      }
    }
  }

}
