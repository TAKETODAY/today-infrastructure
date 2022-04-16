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

package cn.taketoday.framework.web.servlet;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import cn.taketoday.aop.scope.ScopedProxyUtils;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.core.MultiValueMap;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import jakarta.servlet.Filter;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.Servlet;

/**
 * A collection {@link ServletContextInitializer}s obtained from a {@link BeanFactory}.
 * Includes all {@link ServletContextInitializer} beans and also adapts {@link Servlet},
 * {@link Filter} and certain {@link EventListener} beans.
 * <p>
 * Items are sorted so that adapted beans are top ({@link Servlet}, {@link Filter} then
 * {@link EventListener}) and direct {@link ServletContextInitializer} beans are at the
 * end. Further sorting is applied within these groups using the
 * {@link AnnotationAwareOrderComparator}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/20 16:43
 */
public class ServletContextInitializerBeans extends AbstractCollection<ServletContextInitializer> {

  private static final String DISPATCHER_SERVLET_NAME = "dispatcherServlet";

  private static final Logger log = LoggerFactory.getLogger(ServletContextInitializerBeans.class);

  /**
   * Seen bean instances or bean names.
   */
  private final HashSet<Object> seen = new HashSet<>();

  private final MultiValueMap<Class<?>, ServletContextInitializer> initializers;

  private final List<Class<? extends ServletContextInitializer>> initializerTypes;

  private final List<ServletContextInitializer> sortedList;

  @SafeVarargs
  @SuppressWarnings("varargs")
  public ServletContextInitializerBeans(
          BeanFactory beanFactory, Class<? extends ServletContextInitializer>... initializerTypes) {
    this.initializers = MultiValueMap.fromLinkedHashMap();
    this.initializerTypes = initializerTypes.length != 0
                            ? Arrays.asList(initializerTypes)
                            : Collections.singletonList(ServletContextInitializer.class);
    addServletContextInitializerBeans(beanFactory);
    addAdaptableBeans(beanFactory);
    this.sortedList = initializers.values()
            .stream()
            .flatMap((value) -> value.stream().sorted(AnnotationAwareOrderComparator.INSTANCE)).toList();
    logMappings(initializers);
  }

  private void addServletContextInitializerBeans(BeanFactory beanFactory) {
    for (var initializerType : initializerTypes) {
      for (var initializerBean : getOrderedBeansOfType(beanFactory, initializerType)) {
        addServletContextInitializerBean(initializerBean.getKey(), initializerBean.getValue(), beanFactory);
      }
    }
  }

  private void addServletContextInitializerBean(
          String beanName, ServletContextInitializer initializer, BeanFactory beanFactory) {
    if (initializer instanceof ServletRegistrationBean) {
      Servlet source = ((ServletRegistrationBean<?>) initializer).getServlet();
      addServletContextInitializerBean(Servlet.class, beanName, initializer, beanFactory, source);
    }
    else if (initializer instanceof FilterRegistrationBean) {
      Filter source = ((FilterRegistrationBean<?>) initializer).getFilter();
      addServletContextInitializerBean(Filter.class, beanName, initializer, beanFactory, source);
    }
    else if (initializer instanceof DelegatingFilterProxyRegistrationBean) {
      String source = ((DelegatingFilterProxyRegistrationBean) initializer).getTargetBeanName();
      addServletContextInitializerBean(Filter.class, beanName, initializer, beanFactory, source);
    }
    else if (initializer instanceof ServletListenerRegistrationBean) {
      EventListener source = ((ServletListenerRegistrationBean<?>) initializer).getListener();
      addServletContextInitializerBean(EventListener.class, beanName, initializer, beanFactory, source);
    }
    else {
      addServletContextInitializerBean(ServletContextInitializer.class,
              beanName, initializer, beanFactory, initializer);
    }
  }

  private void addServletContextInitializerBean(Class<?> type, String beanName,
          ServletContextInitializer initializer, BeanFactory beanFactory, Object source) {
    initializers.add(type, initializer);
    if (source != null) {
      // Mark the underlying source as seen in case it wraps an existing bean
      seen.add(source);
    }
    if (log.isTraceEnabled()) {
      String resourceDescription = getResourceDescription(beanName, beanFactory);
      int order = getOrder(initializer);
      log.trace("Added existing {} initializer bean '{}'; order={}, resource={}",
              type.getSimpleName(), beanName, order, resourceDescription);
    }
  }

  private String getResourceDescription(String beanName, BeanFactory beanFactory) {
    if (beanFactory instanceof BeanDefinitionRegistry registry) {
      return registry.getBeanDefinition(beanName).getResourceDescription();
    }
    return "unknown";
  }

  @SuppressWarnings("unchecked")
  protected void addAdaptableBeans(BeanFactory beanFactory) {
    MultipartConfigElement multipartConfig = getMultipartConfig(beanFactory);
    addAsRegistrationBean(beanFactory, Servlet.class, new ServletRegistrationBeanAdapter(multipartConfig));
    addAsRegistrationBean(beanFactory, Filter.class, new FilterRegistrationBeanAdapter());
    for (Class<?> listenerType : ServletListenerRegistrationBean.getSupportedTypes()) {
      addAsRegistrationBean(beanFactory, EventListener.class, (Class<EventListener>) listenerType,
              new ServletListenerRegistrationBeanAdapter());
    }
  }

  private MultipartConfigElement getMultipartConfig(BeanFactory beanFactory) {
    var beans = getOrderedBeansOfType(beanFactory, MultipartConfigElement.class);
    return beans.isEmpty() ? null : beans.get(0).getValue();
  }

  protected <T> void addAsRegistrationBean(
          BeanFactory beanFactory, Class<T> type, RegistrationBeanAdapter<T> adapter) {
    addAsRegistrationBean(beanFactory, type, type, adapter);
  }

  private <T, B extends T> void addAsRegistrationBean(
          BeanFactory beanFactory, Class<T> type, Class<B> beanType, RegistrationBeanAdapter<T> adapter) {

    var entries = getOrderedBeansOfType(beanFactory, beanType, seen);
    for (Entry<String, B> entry : entries) {
      String beanName = entry.getKey();
      B bean = entry.getValue();
      if (seen.add(bean)) {
        // One that we haven't already seen
        var registration = adapter.createRegistrationBean(beanName, bean, entries.size());
        int order = getOrder(bean);
        registration.setOrder(order);
        initializers.add(type, registration);
        if (log.isTraceEnabled()) {
          log.trace("Created {} initializer for bean '{}'; order={}, resource={}",
                  type.getSimpleName(), beanName, order, getResourceDescription(beanName, beanFactory));
        }
      }
    }
  }

  private int getOrder(Object value) {
    return AnnotationAwareOrderComparator.INSTANCE.getOrder(value);
  }

  private <T> List<Entry<String, T>> getOrderedBeansOfType(BeanFactory beanFactory, Class<T> type) {
    return getOrderedBeansOfType(beanFactory, type, Collections.emptySet());
  }

  private <T> List<Entry<String, T>> getOrderedBeansOfType(
          BeanFactory beanFactory, Class<T> type, Set<?> excludes) {
    Set<String> names = beanFactory.getBeanNamesForType(type, true, false);
    var map = new LinkedHashMap<String, T>();
    for (String name : names) {
      if (!excludes.contains(name) && !ScopedProxyUtils.isScopedTarget(name)) {
        T bean = beanFactory.getBean(name, type);
        if (!excludes.contains(bean)) {
          map.put(name, bean);
        }
      }
    }
    var beans = new ArrayList<>(map.entrySet());
    beans.sort((o1, o2) -> AnnotationAwareOrderComparator.INSTANCE.compare(o1.getValue(), o2.getValue()));
    return beans;
  }

  private void logMappings(MultiValueMap<Class<?>, ServletContextInitializer> initializers) {
    if (log.isDebugEnabled()) {
      logMappings("filters", initializers, Filter.class, FilterRegistrationBean.class);
      logMappings("servlets", initializers, Servlet.class, ServletRegistrationBean.class);
    }
  }

  private void logMappings(String name, MultiValueMap<Class<?>, ServletContextInitializer> initializers,
          Class<?> type, Class<? extends RegistrationBean> registrationType) {
    var registrations = new ArrayList<>();
    registrations.addAll(initializers.getOrDefault(registrationType, Collections.emptyList()));
    registrations.addAll(initializers.getOrDefault(type, Collections.emptyList()));
    String info = registrations.stream()
            .map(Object::toString)
            .collect(Collectors.joining(", "));
    log.debug("Mapping {}: {}", name, info);
  }

  @Override
  public Iterator<ServletContextInitializer> iterator() {
    return this.sortedList.iterator();
  }

  @Override
  public int size() {
    return this.sortedList.size();
  }

  /**
   * Adapter to convert a given Bean type into a {@link RegistrationBean} (and hence a
   * {@link ServletContextInitializer}).
   *
   * @param <T> the type of the Bean to adapt
   */
  @FunctionalInterface
  protected interface RegistrationBeanAdapter<T> {

    RegistrationBean createRegistrationBean(String name, T source, int totalNumberOfSourceBeans);

  }

  /**
   * {@link RegistrationBeanAdapter} for {@link Servlet} beans.
   */
  private record ServletRegistrationBeanAdapter(MultipartConfigElement multipartConfig)
          implements RegistrationBeanAdapter<Servlet> {

    @Override
    public RegistrationBean createRegistrationBean(String name, Servlet source, int totalNumberOfSourceBeans) {
      String url = totalNumberOfSourceBeans != 1 ? "/" + name + "/" : "/";
      if (name.equals(DISPATCHER_SERVLET_NAME)) {
        url = "/"; // always map the main dispatcherServlet to "/"
      }
      var bean = new ServletRegistrationBean<>(source, url);
      bean.setName(name);
      bean.setMultipartConfig(this.multipartConfig);
      return bean;
    }

  }

  /**
   * {@link RegistrationBeanAdapter} for {@link Filter} beans.
   */
  private static class FilterRegistrationBeanAdapter implements RegistrationBeanAdapter<Filter> {

    @Override
    public RegistrationBean createRegistrationBean(String name, Filter source, int totalNumberOfSourceBeans) {
      var bean = new FilterRegistrationBean<>(source);
      bean.setName(name);
      return bean;
    }

  }

  /**
   * {@link RegistrationBeanAdapter} for certain {@link EventListener} beans.
   */
  private static class ServletListenerRegistrationBeanAdapter implements RegistrationBeanAdapter<EventListener> {

    @Override
    public RegistrationBean createRegistrationBean(
            String name, EventListener source, int totalNumberOfSourceBeans) {
      return new ServletListenerRegistrationBean<>(source);
    }

  }

}
