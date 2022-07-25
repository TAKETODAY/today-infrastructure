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

package cn.taketoday.beans.factory.xml;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.beans.FatalBeanException;
import cn.taketoday.core.io.PropertiesUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;

/**
 * Default implementation of the {@link NamespaceHandlerResolver} interface.
 * Resolves namespace URIs to implementation classes based on the mappings
 * contained in mapping file.
 *
 * <p>By default, this implementation looks for the mapping file at
 * {@code META-INF/spring.handlers}, but this can be changed using the
 * {@link #DefaultNamespaceHandlerResolver(ClassLoader, String)} constructor.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see NamespaceHandler
 * @see DefaultBeanDefinitionDocumentReader
 * @since 4.0
 */
public class DefaultNamespaceHandlerResolver implements NamespaceHandlerResolver {

  /**
   * The location to look for the mapping files. Can be present in multiple JAR files.
   */
  public static final String DEFAULT_HANDLER_MAPPINGS_LOCATION = "META-INF/spring.handlers";

  /** Logger available to subclasses. */
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  /** ClassLoader to use for NamespaceHandler classes. */
  @Nullable
  private final ClassLoader classLoader;

  /** Resource location to search for. */
  private final String handlerMappingsLocation;

  /** Stores the mappings from namespace URI to NamespaceHandler class name / instance. */
  @Nullable
  private volatile Map<String, Object> handlerMappings;

  /**
   * Create a new {@code DefaultNamespaceHandlerResolver} using the
   * default mapping file location.
   * <p>This constructor will result in the thread context ClassLoader being used
   * to load resources.
   *
   * @see #DEFAULT_HANDLER_MAPPINGS_LOCATION
   */
  public DefaultNamespaceHandlerResolver() {
    this(null, DEFAULT_HANDLER_MAPPINGS_LOCATION);
  }

  /**
   * Create a new {@code DefaultNamespaceHandlerResolver} using the
   * default mapping file location.
   *
   * @param classLoader the {@link ClassLoader} instance used to load mapping resources
   * (may be {@code null}, in which case the thread context ClassLoader will be used)
   * @see #DEFAULT_HANDLER_MAPPINGS_LOCATION
   */
  public DefaultNamespaceHandlerResolver(@Nullable ClassLoader classLoader) {
    this(classLoader, DEFAULT_HANDLER_MAPPINGS_LOCATION);
  }

  /**
   * Create a new {@code DefaultNamespaceHandlerResolver} using the
   * supplied mapping file location.
   *
   * @param classLoader the {@link ClassLoader} instance used to load mapping resources
   * may be {@code null}, in which case the thread context ClassLoader will be used)
   * @param handlerMappingsLocation the mapping file location
   */
  public DefaultNamespaceHandlerResolver(@Nullable ClassLoader classLoader, String handlerMappingsLocation) {
    Assert.notNull(handlerMappingsLocation, "Handler mappings location must not be null");
    this.classLoader = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
    this.handlerMappingsLocation = handlerMappingsLocation;
  }

  /**
   * Locate the {@link NamespaceHandler} for the supplied namespace URI
   * from the configured mappings.
   *
   * @param namespaceUri the relevant namespace URI
   * @return the located {@link NamespaceHandler}, or {@code null} if none found
   */
  @Override
  @Nullable
  public NamespaceHandler resolve(String namespaceUri) {
    Map<String, Object> handlerMappings = getHandlerMappings();
    Object handlerOrClassName = handlerMappings.get(namespaceUri);
    if (handlerOrClassName == null) {
      return null;
    }
    else if (handlerOrClassName instanceof NamespaceHandler) {
      return (NamespaceHandler) handlerOrClassName;
    }
    else {
      String className = (String) handlerOrClassName;
      try {
        Class<NamespaceHandler> handlerClass = ClassUtils.forName(className, this.classLoader);
        if (!NamespaceHandler.class.isAssignableFrom(handlerClass)) {
          throw new FatalBeanException("Class [" + className + "] for namespace [" + namespaceUri +
                  "] does not implement the [" + NamespaceHandler.class.getName() + "] interface");
        }
        NamespaceHandler namespaceHandler = BeanUtils.newInstance(handlerClass);
        namespaceHandler.init();
        handlerMappings.put(namespaceUri, namespaceHandler);
        return namespaceHandler;
      }
      catch (ClassNotFoundException ex) {
        throw new FatalBeanException("Could not find NamespaceHandler class [" + className +
                "] for namespace [" + namespaceUri + "]", ex);
      }
      catch (LinkageError err) {
        throw new FatalBeanException("Unresolvable class definition for NamespaceHandler class [" +
                className + "] for namespace [" + namespaceUri + "]", err);
      }
    }
  }

  /**
   * Load the specified NamespaceHandler mappings lazily.
   */
  private Map<String, Object> getHandlerMappings() {
    Map<String, Object> handlerMappings = this.handlerMappings;
    if (handlerMappings == null) {
      synchronized(this) {
        handlerMappings = this.handlerMappings;
        if (handlerMappings == null) {
          if (logger.isTraceEnabled()) {
            logger.trace("Loading NamespaceHandler mappings from [{}]", handlerMappingsLocation);
          }
          try {
            Properties mappings = PropertiesUtils.loadAllProperties(handlerMappingsLocation, classLoader);
            if (logger.isTraceEnabled()) {
              logger.trace("Loaded NamespaceHandler mappings: {}", mappings);
            }
            handlerMappings = new ConcurrentHashMap<>(mappings.size());
            CollectionUtils.mergePropertiesIntoMap(mappings, handlerMappings);
            this.handlerMappings = handlerMappings;
          }
          catch (IOException ex) {
            throw new IllegalStateException(
                    "Unable to load NamespaceHandler mappings from location [" + this.handlerMappingsLocation + "]", ex);
          }
        }
      }
    }
    return handlerMappings;
  }

  @Override
  public String toString() {
    return "NamespaceHandlerResolver using mappings " + getHandlerMappings();
  }

}
