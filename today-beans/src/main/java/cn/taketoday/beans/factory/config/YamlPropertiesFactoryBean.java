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

import java.util.Properties;

import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.core.YamlProcessor;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;

/**
 * Factory for {@link java.util.Properties} that reads from a YAML source,
 * exposing a flat structure of String property values.
 *
 * <p>YAML is a nice human-readable format for configuration, and it has some
 * useful hierarchical properties. It's more or less a superset of JSON, so it
 * has a lot of similar features.
 *
 * <p><b>Note: All exposed values are of type {@code String}</b> for access through
 * the common {@link Properties#getProperty} method
 * If this is not desirable, use {@link YamlMapFactoryBean} instead.
 *
 * <p>The Properties created by this factory have nested paths for hierarchical
 * objects, so for instance this YAML
 *
 * <pre class="code">
 * environments:
 *   dev:
 *     url: https://dev.bar.com
 *     name: Developer Setup
 *   prod:
 *     url: https://foo.bar.com
 *     name: My Cool App
 * </pre>
 *
 * is transformed into these properties:
 *
 * <pre class="code">
 * environments.dev.url=https://dev.bar.com
 * environments.dev.name=Developer Setup
 * environments.prod.url=https://foo.bar.com
 * environments.prod.name=My Cool App
 * </pre>
 *
 * Lists are split as property keys with <code>[]</code> dereferencers, for
 * example this YAML:
 *
 * <pre class="code">
 * servers:
 * - dev.bar.com
 * - foo.bar.com
 * </pre>
 *
 * becomes properties like this:
 *
 * <pre class="code">
 * servers[0]=dev.bar.com
 * servers[1]=foo.bar.com
 * </pre>
 *
 * <p>Requires SnakeYAML 2.0 or higher
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/11/30 14:11
 */
public class YamlPropertiesFactoryBean extends YamlProcessor implements FactoryBean<Properties>, InitializingBean {

  private boolean singleton = true;

  @Nullable
  private Properties properties;

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
      this.properties = createProperties();
    }
  }

  @Override
  @Nullable
  public Properties getObject() {
    return (this.properties != null ? this.properties : createProperties());
  }

  @Override
  public Class<?> getObjectType() {
    return Properties.class;
  }

  /**
   * Template method that subclasses may override to construct the object
   * returned by this factory. The default implementation returns a
   * properties with the content of all resources.
   * <p>Invoked lazily the first time {@link #getObject()} is invoked in
   * case of a shared singleton; else, on each {@link #getObject()} call.
   *
   * @return the object returned by this factory
   * @see #process(MatchCallback)
   */
  protected Properties createProperties() {
    Properties result = CollectionUtils.createStringAdaptingProperties();
    process((properties, map) -> result.putAll(properties));
    return result;
  }

}

