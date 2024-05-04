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

package cn.taketoday.web.socket.server.standard;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.stereotype.Component;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.servlet.ContextLoader;
import cn.taketoday.web.servlet.WebApplicationContext;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.server.ServerEndpointConfig.Configurator;

/**
 * A {@link Configurator} for initializing
 * {@link ServerEndpoint}-annotated classes through Infra.
 *
 * <p>
 * <pre>{@code
 * @ServerEndpoint(value = "/echo", configurator = InfraConfigurator.class)
 * public class EchoEndpoint {
 *     // ...
 * }
 * }</pre>
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class InfraConfigurator extends Configurator {

  private static final String NO_VALUE = ObjectUtils.identityToString(new Object());

  private static final Logger logger = LoggerFactory.getLogger(InfraConfigurator.class);

  private static final Map<String, Map<Class<?>, String>> cache =
          new ConcurrentHashMap<>();

  @Override
  public <T> T getEndpointInstance(Class<T> endpointClass) {
    WebApplicationContext wac = ContextLoader.getCurrentWebApplicationContext();
    if (wac == null) {
      String message = "Failed to find the root WebApplicationContext. Was ContextLoaderListener not used?";
      logger.error(message);
      throw new IllegalStateException(message);
    }

    String beanName = ClassUtils.getShortNameAsProperty(endpointClass);
    if (wac.containsBean(beanName)) {
      T endpoint = wac.getBean(beanName, endpointClass);
      if (logger.isTraceEnabled()) {
        logger.trace("Using @ServerEndpoint singleton {}", endpoint);
      }
      return endpoint;
    }

    Component ann = AnnotationUtils.findAnnotation(endpointClass, Component.class);
    if (ann != null) {
      String[] names = ann.value();
      String name = CollectionUtils.firstElement(names);
      if (StringUtils.hasText(name) && wac.containsBean(name)) {
        T endpoint = wac.getBean(name, endpointClass);
        if (logger.isTraceEnabled()) {
          logger.trace("Using @ServerEndpoint singleton {}", endpoint);
        }
        return endpoint;
      }
    }

    beanName = getBeanNameByType(wac, endpointClass);
    if (beanName != null) {
      return wac.getBean(beanName, endpointClass);
    }

    if (logger.isTraceEnabled()) {
      logger.trace("Creating new @ServerEndpoint instance of type {}", endpointClass);
    }
    return wac.getAutowireCapableBeanFactory().createBean(endpointClass);
  }

  @Nullable
  private String getBeanNameByType(WebApplicationContext wac, Class<?> endpointClass) {
    String wacId = wac.getId();

    Map<Class<?>, String> beanNamesByType = cache.get(wacId);
    if (beanNamesByType == null) {
      beanNamesByType = new ConcurrentHashMap<>();
      cache.put(wacId, beanNamesByType);
    }

    if (!beanNamesByType.containsKey(endpointClass)) {
      Set<String> names = wac.getBeanNamesForType(endpointClass);
      if (names.size() == 1) {
        beanNamesByType.put(endpointClass, CollectionUtils.firstElement(names));
      }
      else {
        beanNamesByType.put(endpointClass, NO_VALUE);
        if (names.size() > 1) {
          throw new IllegalStateException("Found multiple @ServerEndpoint's of type [%s]: bean names %s"
                  .formatted(endpointClass.getName(), names));
        }
      }
    }

    String beanName = beanNamesByType.get(endpointClass);
    return NO_VALUE.equals(beanName) ? null : beanName;
  }

}
