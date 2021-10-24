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

package cn.taketoday.beans.factory;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import cn.taketoday.beans.DisposableBean;
import cn.taketoday.context.annotation.BeanDefinitionBuilder;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * Default SingletonBeanRegistry implementation
 *
 * @author TODAY 2021/10/1 22:47
 * @since 4.0
 */
public class DefaultSingletonBeanRegistry implements SingletonBeanRegistry {
  private static final Logger log = LoggerFactory.getLogger(DefaultSingletonBeanRegistry.class);

  /** Map of bean instance, keyed by bean name */
  private final HashMap<String, Object> singletons = new HashMap<>(128);

  /** Disposable bean instances: bean name to disposable instance. @since 4.0 */
  private final LinkedHashMap<String, Object> disposableBeans = new LinkedHashMap<>();

  /** Flag that indicates whether we're currently within destroySingletons. @since 4.0 */
  private boolean singletonsCurrentlyInDestruction = false;

  @Override
  public void registerSingleton(final String name, final Object singleton) {
    Assert.notNull(name, "Bean name must not be null");
    Assert.notNull(singleton, "Singleton object must not be null");
    synchronized(singletons) {
      final Object oldBean = singletons.put(name, singleton);
      if (oldBean == null) {
        singletonRegistered(name, singleton);
      }
      else if (oldBean != singleton) {
        singletonAlreadyExist(name, singleton, oldBean);
      }
    }
  }

  protected void singletonRegistered(String name, Object singleton) {
    if (log.isDebugEnabled()) {
      log.debug("Register Singleton: [{}] = [{}]", name, ObjectUtils.toHexString(singleton));
    }
  }

  protected void singletonAlreadyExist(String name, Object singleton, Object existBean) {
    log.info("Refresh Singleton: [{}] = [{}] old bean: [{}] ",
             name, ObjectUtils.toHexString(singleton), ObjectUtils.toHexString(existBean));
  }

  @Override
  public void registerSingleton(Object bean) {
    registerSingleton(createBeanName(bean.getClass()), bean);
  }

  @Override
  public Map<String, Object> getSingletons() {
    return singletons;
  }

  @Override
  public Object getSingleton(String name) {
    return singletons.get(name);
  }

  /**
   * Return the (raw) singleton object registered under the given name,
   * creating and registering a new one if none registered yet.
   *
   * @param beanName the name of the bean
   * @param singletonSupplier the ObjectFactory to lazily create the singleton
   * with, if necessary
   * @return the registered singleton object
   */
  @SuppressWarnings("unchecked")
  public <T> T getSingleton(String beanName, Supplier<T> singletonSupplier) {
    Assert.notNull(beanName, "Bean name must not be null");
    Object singletonObject = singletons.get(beanName);
    if (singletonObject == null) {
      synchronized(singletons) {
        if (this.singletonsCurrentlyInDestruction) {
          throw new BeanCreationNotAllowedException(
                  beanName,
                  "Singleton bean creation not allowed while singletons of this factory" +
                          " are in destruction (Do not request a bean from a BeanFactory " +
                          "in a destroy method implementation!)");
        }
        singletonObject = singletons.get(beanName);
        if (singletonObject == null) {
          log.debug("Creating shared instance of singleton bean '{}'", beanName);
          beforeSingletonCreation(beanName);
          try {
            singletonObject = singletonSupplier.get();
          }
          finally {
            afterSingletonCreation(beanName);
          }
          registerSingleton(beanName, singletonObject);
        }
      }
    }
    return (T) singletonObject;
  }

  /**
   * Callback before singleton creation.
   * <p>The default implementation register the singleton as currently in creation.
   *
   * @param beanName the name of the singleton about to be created
   */
  protected void beforeSingletonCreation(String beanName) {

  }

  /**
   * Callback after singleton creation.
   * <p>The default implementation marks the singleton as not in creation anymore.
   *
   * @param beanName the name of the singleton that has been created
   */
  protected void afterSingletonCreation(String beanName) {

  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getSingleton(final Class<T> requiredType) {
    final String maybe = createBeanName(requiredType);
    final Object singleton = getSingleton(maybe);
    if (!requiredType.isInstance(singleton)) {
      synchronized(singletons) {
        for (final Object value : singletons.values()) {
          if (requiredType.isInstance(value)) {
            return (T) value;
          }
        }
      }
    }
    return (T) singleton;
  }

  /**
   * default is use {@link ClassUtils#getShortName(Class)}
   *
   * <p>
   * sub-classes can overriding this method to provide a strategy to create bean name
   * </p>
   *
   * @param type type
   * @return bean name
   * @see ClassUtils#getShortName(Class)
   */
  protected String createBeanName(Class<?> type) {
    return BeanDefinitionBuilder.defaultBeanName(type);
  }

  @Override
  public void removeSingleton(String name) {
    synchronized(singletons) {
      singletons.remove(name);
    }
  }

  @Override
  public boolean containsSingleton(String name) {
    return singletons.containsKey(name);
  }

  @Override
  public int getSingletonCount() {
    synchronized(singletons) {
      return singletons.size();
    }
  }

  @Override
  public Set<String> getSingletonNames() {
    return singletons.keySet();
  }

  /**
   * Removes all the mappings from this map. The map will be empty after this call returns.
   *
   * @since 4.0
   */
  protected void clearSingletonCache() {
    synchronized(singletons) {
      singletons.clear();
      singletonsCurrentlyInDestruction = false;
    }
  }

  /**
   * Destroy the given bean. Delegates to {@code destroyBean}
   * if a corresponding disposable bean instance is found.
   *
   * @param beanName the name of the bean
   * @see #destroyBean
   * @since 4.0
   */
  public void destroySingleton(String beanName) {
    // Remove a registered singleton of the given name, if any.
    removeSingleton(beanName);

    // Destroy the corresponding DisposableBean instance.
    DisposableBean disposableBean;
    synchronized(this.disposableBeans) {
      disposableBean = (DisposableBean) this.disposableBeans.remove(beanName);
    }
    destroyBean(beanName, disposableBean);
  }

  /**
   * Destroy the given bean. Must destroy beans that depend on the given
   * bean before the bean itself. Should not throw any exceptions.
   *
   * @param beanName the name of the bean
   * @param bean the bean instance to destroy
   * @since 4.0
   */
  protected void destroyBean(String beanName, @Nullable DisposableBean bean) {
    // Actually destroy the bean now...
    if (bean != null) {
      try {
        bean.destroy();
      }
      catch (Throwable ex) {
        log.warn("Destruction of bean with name '{}' threw an exception", beanName, ex);
      }
    }
  }

  /**
   * @since 4.0
   */
  public void destroySingletons() {
    if (log.isTraceEnabled()) {
      log.trace("Destroying singletons in {}", this);
    }
    synchronized(this.singletons) {
      this.singletonsCurrentlyInDestruction = true;
    }

    String[] disposableBeanNames;
    synchronized(this.disposableBeans) {
      disposableBeanNames = StringUtils.toStringArray(this.disposableBeans.keySet());
    }
    for (int i = disposableBeanNames.length - 1; i >= 0; i--) {
      destroySingleton(disposableBeanNames[i]);
    }

    clearSingletonCache();
  }

  /**
   * Add the given bean to the list of disposable beans in this registry.
   * <p>Disposable beans usually correspond to registered singletons,
   * matching the bean name but potentially being a different instance
   * (for example, a DisposableBean adapter for a singleton that does not
   * naturally implement Spring's DisposableBean interface).
   *
   * @param beanName the name of the bean
   * @param bean the bean instance
   * @since 4.0
   */
  public void registerDisposableBean(String beanName, DisposableBean bean) {
    synchronized(this.disposableBeans) {
      this.disposableBeans.put(beanName, bean);
    }
  }

}
