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

package cn.taketoday.context.event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EventObject;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import cn.taketoday.aop.proxy.AopProxyUtils;
import cn.taketoday.beans.factory.BeanClassLoaderAware;
import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.support.ConfigurableBeanFactory;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;

/**
 * Abstract implementation of the {@link ApplicationEventMulticaster} interface,
 * providing the basic listener registration facility.
 *
 * <p>Doesn't permit multiple instances of the same listener by default,
 * as it keeps listeners in a linked Set. The collection class used to hold
 * ApplicationListener objects can be overridden through the "collectionClass"
 * bean property.
 *
 * <p>Implementing ApplicationEventMulticaster's actual {@link #multicastEvent} method
 * is left to subclasses. {@link SimpleApplicationEventMulticaster} simply multicasts
 * all events to all registered listeners, invoking them in the calling thread.
 * Alternative implementations could be more sophisticated in those respects.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author Harry Yang
 * @see #getApplicationListeners(Object, ResolvableType)
 * @see SimpleApplicationEventMulticaster
 * @since 4.0
 */
public abstract class AbstractApplicationEventMulticaster
        implements ApplicationEventMulticaster, BeanClassLoaderAware, BeanFactoryAware {

  private final ListenerRetriever retriever = new ListenerRetriever();

  final Map<ListenerCacheKey, CachedListenerRetriever> retrieverCache = new ConcurrentHashMap<>(64);

  @Nullable
  private ClassLoader beanClassLoader;

  @Nullable
  private ConfigurableBeanFactory beanFactory;

  @Override
  public void setBeanClassLoader(ClassLoader classLoader) {
    this.beanClassLoader = classLoader;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    if (!(beanFactory instanceof ConfigurableBeanFactory)) {
      throw new IllegalStateException("Not running in a ConfigurableBeanFactory: " + beanFactory);
    }
    this.beanFactory = (ConfigurableBeanFactory) beanFactory;
    if (this.beanClassLoader == null) {
      this.beanClassLoader = this.beanFactory.getBeanClassLoader();
    }
  }

  private ConfigurableBeanFactory getBeanFactory() {
    if (this.beanFactory == null) {
      throw new IllegalStateException("ApplicationEventMulticaster cannot retrieve listener beans " +
              "because it is not associated with a BeanFactory");
    }
    return this.beanFactory;
  }

  @Override
  public void addApplicationListener(ApplicationListener<?> listener) {
    synchronized(this.retriever) {
      // Explicitly remove target for a proxy, if registered already,
      // in order to avoid double invocations of the same listener.
      Object singletonTarget = AopProxyUtils.getSingletonTarget(listener);
      if (singletonTarget instanceof ApplicationListener) {
        this.retriever.applicationListeners.remove(singletonTarget);
      }
      this.retriever.applicationListeners.add(listener);
      this.retrieverCache.clear();
    }
  }

  @Override
  public void addApplicationListenerBean(String listenerBeanName) {
    synchronized(this.retriever) {
      this.retriever.applicationListenerBeans.add(listenerBeanName);
      this.retrieverCache.clear();
    }
  }

  @Override
  public void removeApplicationListener(ApplicationListener<?> listener) {
    synchronized(this.retriever) {
      this.retriever.applicationListeners.remove(listener);
      this.retrieverCache.clear();
    }
  }

  @Override
  public void removeApplicationListenerBean(String listenerBeanName) {
    synchronized(this.retriever) {
      this.retriever.applicationListenerBeans.remove(listenerBeanName);
      this.retrieverCache.clear();
    }
  }

  @Override
  public void removeApplicationListeners(Predicate<ApplicationListener<?>> predicate) {
    synchronized(this.retriever) {
      this.retriever.applicationListeners.removeIf(predicate);
      this.retrieverCache.clear();
    }
  }

  @Override
  public void removeApplicationListenerBeans(Predicate<String> predicate) {
    synchronized(this.retriever) {
      this.retriever.applicationListenerBeans.removeIf(predicate);
      this.retrieverCache.clear();
    }
  }

  @Override
  public void removeAllListeners() {
    synchronized(this.retriever) {
      this.retriever.applicationListeners.clear();
      this.retriever.applicationListenerBeans.clear();
      this.retrieverCache.clear();
    }
  }

  /**
   * Return a Collection containing all ApplicationListeners.
   *
   * @return a Collection of ApplicationListeners
   * @see cn.taketoday.context.event.ApplicationListener
   */
  protected Collection<ApplicationListener<?>> getApplicationListeners() {
    synchronized(this.retriever) {
      return this.retriever.getApplicationListeners();
    }
  }

  /**
   * Return a Collection of ApplicationListeners matching the given
   * event type. Non-matching listeners get excluded early.
   *
   * @param event the event to be propagated. Allows for excluding
   * non-matching listeners early, based on cached matching information.
   * @param eventType the event type
   * @return a Collection of ApplicationListeners
   * @see cn.taketoday.context.event.ApplicationListener
   */
  protected Collection<ApplicationListener<?>> getApplicationListeners(
          Object event, ResolvableType eventType) {
    Class<?> sourceType = null;
    if (event instanceof EventObject) {
      Object source = ((EventObject) event).getSource();
      sourceType = source != null ? source.getClass() : null;
    }

    ListenerCacheKey cacheKey = new ListenerCacheKey(eventType, sourceType);

    // Potential new retriever to populate
    CachedListenerRetriever newRetriever = null;

    // Quick check for existing entry on ConcurrentHashMap
    CachedListenerRetriever existingRetriever = this.retrieverCache.get(cacheKey);
    if (existingRetriever == null) {
      // Caching a new ListenerRetriever if possible
      if (this.beanClassLoader == null ||
              (ClassUtils.isCacheSafe(event.getClass(), this.beanClassLoader)
                      && (sourceType == null || ClassUtils.isCacheSafe(sourceType, this.beanClassLoader)))) {
        newRetriever = new CachedListenerRetriever();
        existingRetriever = this.retrieverCache.putIfAbsent(cacheKey, newRetriever);
        if (existingRetriever != null) {
          newRetriever = null;  // no need to populate it in retrieveApplicationListeners
        }
      }
    }

    if (existingRetriever != null) {
      Collection<ApplicationListener<?>> result = existingRetriever.getApplicationListeners();
      if (result != null) {
        return result;
      }
      // If result is null, the existing retriever is not fully populated yet by another thread.
      // Proceed like caching wasn't possible for this current local attempt.
    }

    return retrieveApplicationListeners(eventType, sourceType, newRetriever);
  }

  /**
   * Actually retrieve the application listeners for the given event and source type.
   *
   * @param eventType the event type
   * @param sourceType the event source type
   * @param retriever the ListenerRetriever, if supposed to populate one (for caching purposes)
   * @return the pre-filtered list of application listeners for the given event and source type
   */
  private Collection<ApplicationListener<?>> retrieveApplicationListeners(
          ResolvableType eventType, @Nullable Class<?> sourceType, @Nullable CachedListenerRetriever retriever) {

    List<ApplicationListener<?>> allListeners = new ArrayList<>();
    Set<ApplicationListener<?>> filteredListeners = retriever != null ? new LinkedHashSet<>() : null;
    Set<String> filteredListenerBeans = retriever != null ? new LinkedHashSet<>() : null;

    Set<ApplicationListener<?>> listeners;
    Set<String> listenerBeans;
    synchronized(this.retriever) {
      listeners = new LinkedHashSet<>(this.retriever.applicationListeners);
      listenerBeans = new LinkedHashSet<>(this.retriever.applicationListenerBeans);
    }

    // Add programmatically registered listeners, including ones coming
    // from ApplicationListenerDetector (singleton beans and inner beans).
    for (ApplicationListener<?> listener : listeners) {
      if (supportsEvent(listener, eventType, sourceType)) {
        if (retriever != null) {
          filteredListeners.add(listener);
        }
        allListeners.add(listener);
      }
    }

    // Add listeners by bean name, potentially overlapping with programmatically
    // registered listeners above - but here potentially with additional metadata.
    if (!listenerBeans.isEmpty()) {
      ConfigurableBeanFactory beanFactory = getBeanFactory();
      for (String listenerBeanName : listenerBeans) {
        if (supportsEvent(beanFactory, listenerBeanName, eventType)) {
          ApplicationListener<?> listener =
                  beanFactory.getBean(listenerBeanName, ApplicationListener.class);
          if (listener != null
                  && !allListeners.contains(listener)
                  && supportsEvent(listener, eventType, sourceType)) {
            if (retriever != null) {
              if (beanFactory.isSingleton(listenerBeanName)) {
                filteredListeners.add(listener);
              }
              else {
                filteredListenerBeans.add(listenerBeanName);
              }
            }
            allListeners.add(listener);
          }
        }
        else {
          // Remove non-matching listeners that originally came from
          // ApplicationListenerDetector, possibly ruled out by additional
          // BeanDefinition metadata (e.g. factory method generics) above.
          Object listener = beanFactory.getSingleton(listenerBeanName);
          if (retriever != null) {
            filteredListeners.remove(listener);
          }
          allListeners.remove(listener);
        }
      }
    }

    AnnotationAwareOrderComparator.sort(allListeners);
    if (retriever != null) {
      if (filteredListenerBeans.isEmpty()) {
        retriever.applicationListeners = new LinkedHashSet<>(allListeners);
      }
      else {
        retriever.applicationListeners = filteredListeners;
      }
      retriever.applicationListenerBeans = filteredListenerBeans;
    }
    return allListeners;
  }

  /**
   * Filter a bean-defined listener early through checking its generically declared
   * event type before trying to instantiate it.
   * <p>If this method returns {@code true} for a given listener as a first pass,
   * the listener instance will get retrieved and fully evaluated through a
   * {@link #supportsEvent(ApplicationListener, ResolvableType, Class)} call afterwards.
   *
   * @param beanFactory the BeanFactory that contains the listener beans
   * @param listenerBeanName the name of the bean in the BeanFactory
   * @param eventType the event type to check
   * @return whether the given listener should be included in the candidates
   * for the given event type
   * @see #supportsEvent(Class, ResolvableType)
   * @see #supportsEvent(ApplicationListener, ResolvableType, Class)
   */
  private boolean supportsEvent(
          ConfigurableBeanFactory beanFactory, String listenerBeanName, ResolvableType eventType) {

    Class<?> listenerType = beanFactory.getType(listenerBeanName);
    if (listenerType == null
            || GenericApplicationListener.class.isAssignableFrom(listenerType)
            || SmartApplicationListener.class.isAssignableFrom(listenerType)) {
      return true;
    }
    if (!supportsEvent(listenerType, eventType)) {
      return false;
    }

    BeanDefinition definition = beanFactory.getBeanDefinition(listenerBeanName);
    if (definition != null) {
      ResolvableType genericEventType = definition.getResolvableType().as(ApplicationListener.class).getGeneric();
      return genericEventType == ResolvableType.NONE
              || genericEventType.isAssignableFrom(eventType);
    }
    // Ignore - no need to check resolvable type for manually registered singleton
    return true;
  }

  /**
   * Filter a listener early through checking its generically declared event
   * type before trying to instantiate it.
   * <p>If this method returns {@code true} for a given listener as a first pass,
   * the listener instance will get retrieved and fully evaluated through a
   * {@link #supportsEvent(ApplicationListener, ResolvableType, Class)} call afterwards.
   *
   * @param listenerType the listener's type as determined by the BeanFactory
   * @param eventType the event type to check
   * @return whether the given listener should be included in the candidates
   * for the given event type
   */
  protected boolean supportsEvent(Class<?> listenerType, ResolvableType eventType) {
    ResolvableType declaredEventType = GenericApplicationListenerAdapter.resolveDeclaredEventType(listenerType);
    return declaredEventType == null || declaredEventType.isAssignableFrom(eventType);
  }

  /**
   * Determine whether the given listener supports the given event.
   * <p>The default implementation detects the {@link SmartApplicationListener}
   * and {@link GenericApplicationListener} interfaces. In case of a standard
   * {@link ApplicationListener}, a {@link GenericApplicationListenerAdapter}
   * will be used to introspect the generically declared type of the target listener.
   *
   * @param listener the target listener to check
   * @param eventType the event type to check against
   * @param sourceType the source type to check against
   * @return whether the given listener should be included in the candidates
   * for the given event type
   */
  protected boolean supportsEvent(
          ApplicationListener<?> listener, ResolvableType eventType, @Nullable Class<?> sourceType) {
    GenericApplicationListener smartListener
            = listener instanceof GenericApplicationListener
              ? (GenericApplicationListener) listener : new GenericApplicationListenerAdapter(listener);
    return smartListener.supportsEventType(eventType) && smartListener.supportsSourceType(sourceType);
  }

  /**
   * Cache key for ListenerRetrievers, based on event type and source type.
   */
  private record ListenerCacheKey(
          ResolvableType eventType, @Nullable Class<?> sourceType) implements Comparable<ListenerCacheKey> {

    private ListenerCacheKey(ResolvableType eventType, @Nullable Class<?> sourceType) {
      Assert.notNull(eventType, "Event type must not be null");
      this.eventType = eventType;
      this.sourceType = sourceType;
    }

    @Override
    public boolean equals(@Nullable Object other) {
      if (this == other) {
        return true;
      }
      if (!(other instanceof ListenerCacheKey otherKey)) {
        return false;
      }
      return this.eventType.equals(otherKey.eventType)
              && Objects.equals(this.sourceType, otherKey.sourceType);
    }

    @Override
    public int hashCode() {
      return this.eventType.hashCode() * 29 + ObjectUtils.nullSafeHashCode(this.sourceType);
    }

    @Override
    public String toString() {
      return "ListenerCacheKey [eventType = " + this.eventType + ", sourceType = " + this.sourceType + "]";
    }

    @Override
    public int compareTo(ListenerCacheKey other) {
      int result = this.eventType.toString().compareTo(other.eventType.toString());
      if (result == 0) {
        if (this.sourceType == null) {
          return (other.sourceType == null ? 0 : -1);
        }
        if (other.sourceType == null) {
          return 1;
        }
        result = this.sourceType.getName().compareTo(other.sourceType.getName());
      }
      return result;
    }
  }

  /**
   * Helper class that encapsulates a specific set of target listeners,
   * allowing for efficient retrieval of pre-filtered listeners.
   * <p>An instance of this helper gets cached per event type and source type.
   */
  private class CachedListenerRetriever {

    @Nullable
    public volatile Set<String> applicationListenerBeans;

    @Nullable
    public volatile Set<ApplicationListener<?>> applicationListeners;

    @Nullable
    public Collection<ApplicationListener<?>> getApplicationListeners() {
      Set<ApplicationListener<?>> applicationListeners = this.applicationListeners;
      Set<String> applicationListenerBeans = this.applicationListenerBeans;
      if (applicationListeners == null || applicationListenerBeans == null) {
        // Not fully populated yet
        return null;
      }

      ArrayList<ApplicationListener<?>> allListeners = new ArrayList<>(
              applicationListeners.size() + applicationListenerBeans.size());
      allListeners.addAll(applicationListeners);
      if (!applicationListenerBeans.isEmpty()) {
        BeanFactory beanFactory = getBeanFactory();
        for (String listenerBeanName : applicationListenerBeans) {
          ApplicationListener<?> listener = beanFactory.getBean(listenerBeanName, ApplicationListener.class);
          if (listener != null) {
            allListeners.add(listener);
          }
        }
      }
      if (!applicationListenerBeans.isEmpty()) {
        AnnotationAwareOrderComparator.sort(allListeners);
      }
      return allListeners;
    }
  }

  /**
   * Helper class that encapsulates a general set of target listeners.
   */
  private class ListenerRetriever {

    public final LinkedHashSet<String> applicationListenerBeans = new LinkedHashSet<>();
    public final LinkedHashSet<ApplicationListener<?>> applicationListeners = new LinkedHashSet<>();

    public Collection<ApplicationListener<?>> getApplicationListeners() {
      List<ApplicationListener<?>> allListeners = new ArrayList<>(
              this.applicationListeners.size() + this.applicationListenerBeans.size());
      allListeners.addAll(this.applicationListeners);
      if (!this.applicationListenerBeans.isEmpty()) {
        BeanFactory beanFactory = getBeanFactory();
        for (String listenerBeanName : this.applicationListenerBeans) {
          ApplicationListener<?> listener =
                  beanFactory.getBean(listenerBeanName, ApplicationListener.class);
          if (listener != null && !allListeners.contains(listener)) {
            allListeners.add(listener);
          }
        }
      }
      AnnotationAwareOrderComparator.sort(allListeners);
      return allListeners;
    }
  }

}
