/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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
package cn.taketoday.beans.factory.config;

import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.core.YamlProcessor;
import cn.taketoday.lang.Nullable;

/**
 * Factory for a {@code Map} that reads from a YAML source, preserving the
 * YAML-declared value types and their structure.
 *
 * <p>YAML is a nice human-readable format for configuration, and it has some
 * useful hierarchical properties. It's more or less a superset of JSON, so it
 * has a lot of similar features.
 *
 * <p>If multiple resources are provided the later ones will override entries in
 * the earlier ones hierarchically; that is, all entries with the same nested key
 * of type {@code Map} at any depth are merged. For example:
 *
 * <pre class="code">
 * foo:
 *   bar:
 *    one: two
 * three: four
 * </pre>
 *
 * plus (later in the list)
 *
 * <pre class="code">
 * foo:
 *   bar:
 *    one: 2
 * five: six
 * </pre>
 *
 * results in an effective input of
 *
 * <pre class="code">
 * foo:
 *   bar:
 *    one: 2
 * three: four
 * five: six
 * </pre>
 *
 * Note that the value of "foo" in the first document is not simply replaced
 * with the value in the second, but its nested values are merged.
 *
 * <p>Requires SnakeYAML 2.0 or higher
 *
 * @author Dave Syer
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/11/30 14:11
 */
public class YamlMapFactoryBean extends YamlProcessor implements FactoryBean<Map<String, Object>>, InitializingBean {

  private boolean singleton = true;

  @Nullable
  private Map<String, Object> map;

  /**
   * Set if a singleton should be created, or a new object on each request
   * otherwise. Default is {@code true} (a singleton).
   */
  public void setSingleton(boolean singleton) {
    this.singleton = singleton;
  }

  @Override
  public boolean isSingleton() {
    return this.singleton;
  }

  @Override
  public void afterPropertiesSet() {
    if (isSingleton()) {
      this.map = createMap();
    }
  }

  @Override
  @Nullable
  public Map<String, Object> getObject() {
    return (this.map != null ? this.map : createMap());
  }

  @Override
  public Class<?> getObjectType() {
    return Map.class;
  }

  /**
   * Template method that subclasses may override to construct the object
   * returned by this factory.
   * <p>Invoked lazily the first time {@link #getObject()} is invoked in
   * case of a shared singleton; else, on each {@link #getObject()} call.
   * <p>The default implementation returns the merged {@code Map} instance.
   *
   * @return the object returned by this factory
   * @see #process(MatchCallback)
   */
  protected LinkedHashMap<String, Object> createMap() {
    LinkedHashMap<String, Object> result = new LinkedHashMap<>();
    process((properties, map) -> merge(result, map));
    return result;
  }

}
