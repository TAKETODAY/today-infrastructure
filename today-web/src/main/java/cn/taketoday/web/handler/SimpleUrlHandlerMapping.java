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

package cn.taketoday.web.handler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import cn.taketoday.beans.BeansException;
import cn.taketoday.core.AntPathMatcher;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.HandlerMapping;

/**
 * Implementation of the {@link HandlerMapping} interface that maps from URLs
 * to request handler beans. Supports both mapping to bean instances and mapping
 * to bean names; the latter is required for non-singleton handlers.
 *
 * <p>The "urlMap" property is suitable for populating the handler map with
 * bean references, e.g. via the map element in XML bean definitions.
 *
 * <p>Mappings to bean names can be set via the "mappings" property, in a form
 * accepted by the {@code java.util.Properties} class, as follows:
 *
 * <pre class="code">
 * /welcome.html=ticketController
 * /show.html=ticketController</pre>
 *
 * <p>The syntax is {@code PATH=HANDLER_BEAN_NAME}. If the path doesn't begin
 * with a slash, one is prepended.
 *
 * <p>Supports direct matches (given "/test" -&gt; registered "/test") and "*"
 * matches (given "/test" -&gt; registered "/t*"). For details on the pattern
 * options, see the {@link cn.taketoday.web.util.pattern.PathPattern} javadoc.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author TODAY 2020/12/23 17:21
 * @see #setMappings
 * @see #setUrlMap
 * @see BeanNameUrlHandlerMapping
 * @since 3.0
 */
public class SimpleUrlHandlerMapping extends AbstractUrlHandlerMapping {

  private final Map<String, Object> urlMap = new LinkedHashMap<>();

  /**
   * Create a {@code SimpleUrlHandlerMapping} with default settings.
   *
   * @since 4.0
   */
  public SimpleUrlHandlerMapping() { }

  /**
   * Create a {@code SimpleUrlHandlerMapping} using the supplied URL map.
   *
   * @param urlMap map with URL paths as keys and handler beans (or handler
   * bean names) as values
   * @see #setUrlMap(Map)
   * @since 4.0
   */
  public SimpleUrlHandlerMapping(Map<String, ?> urlMap) {
    setUrlMap(urlMap);
  }

  /**
   * Create a {@code SimpleUrlHandlerMapping} using the supplied URL map and order.
   *
   * @param urlMap map with URL paths as keys and handler beans (or handler
   * bean names) as values
   * @param order the order value for this {@code SimpleUrlHandlerMapping}
   * @see #setUrlMap(Map)
   * @see #setOrder(int)
   * @since 4.0
   */
  public SimpleUrlHandlerMapping(Map<String, ?> urlMap, int order) {
    setUrlMap(urlMap);
    setOrder(order);
  }

  /**
   * Map URL paths to handler bean names.
   * This is the typical way of configuring this HandlerMapping.
   * <p>Supports direct URL matches and Ant-style pattern matches. For syntax
   * details, see the {@link AntPathMatcher} javadoc.
   *
   * @param mappings properties with URLs as keys and bean names as values
   * @see #setUrlMap
   * @since 4.0
   */
  public void setMappings(Properties mappings) {
    CollectionUtils.mergePropertiesIntoMap(mappings, this.urlMap);
  }

  /**
   * Set a Map with URL paths as keys and handler beans (or handler bean names)
   * as values. Convenient for population with bean references.
   * <p>Supports direct URL matches and Ant-style pattern matches. For syntax
   * details, see the {@link AntPathMatcher} javadoc.
   *
   * @param urlMap map with URLs as keys and beans as values
   * @see #setMappings
   * @since 4.0
   */
  public void setUrlMap(Map<String, ?> urlMap) {
    this.urlMap.putAll(urlMap);
  }

  /**
   * Allow Map access to the URL path mappings, with the option to add or
   * override specific entries.
   * <p>Useful for specifying entries directly, for example via "urlMap[myKey]".
   * This is particularly useful for adding or overriding entries in child
   * bean definitions.
   *
   * @since 4.0
   */
  public Map<String, ?> getUrlMap() {
    return this.urlMap;
  }

  /**
   * Calls the {@link #registerHandlers} method in addition to the
   * superclass's initialization.
   *
   * @since 4.0
   */
  @Override
  public void initApplicationContext() throws BeansException {
    super.initApplicationContext();
    registerHandlers(this.urlMap);
  }

  /**
   * Register all handlers specified in the URL map for the corresponding paths.
   *
   * @param urlMap a Map with URL paths as keys and handler beans or bean names as values
   * @throws BeansException if a handler couldn't be registered
   * @throws IllegalStateException if there is a conflicting handler registered
   */
  protected void registerHandlers(Map<String, Object> urlMap) throws BeansException {
    if (urlMap.isEmpty()) {
      logger.trace("No patterns in {}", formatMappingName());
    }
    else {
      for (Map.Entry<String, Object> entry : urlMap.entrySet()) {
        String url = entry.getKey();
        Object handler = entry.getValue();
        // Prepend with slash if not already present.
        if (!url.startsWith("/")) {
          url = "/" + url;
        }
        // Remove whitespace from handler bean name.
        if (handler instanceof String) {
          handler = ((String) handler).trim();
        }
        registerHandler(url, handler);
      }
      logMappings();
    }
  }

  private void logMappings() {
    if (mappingsLogger.isDebugEnabled()) {
      mappingsLogger.debug("{}  {}", formatMappingName(), getHandlerMap());
    }
    else if (logger.isDebugEnabled()) {
      logger.debug("Patterns {} in {}", getHandlerMap().keySet(), formatMappingName());
    }
  }

}
