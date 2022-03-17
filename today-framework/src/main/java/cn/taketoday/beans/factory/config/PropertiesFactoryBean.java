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

package cn.taketoday.beans.factory.config;

import java.io.IOException;
import java.util.Properties;

import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.core.io.PropertiesLoaderSupport;
import cn.taketoday.lang.Nullable;

/**
 * Allows for making a properties file from a classpath location available
 * as Properties instance in a bean factory. Can be used to populate
 * any bean property of type Properties via a bean reference.
 *
 * <p>Supports loading from a properties file and/or setting local properties
 * on this FactoryBean. The created Properties instance will be merged from
 * loaded and local values. If neither a location nor local properties are set,
 * an exception will be thrown on initialization.
 *
 * <p>Can create a singleton or a new object on each request.
 * Default is a singleton.
 *
 * @author Juergen Hoeller
 * @see #setLocation
 * @see #setProperties
 * @see #setLocalOverride
 * @see java.util.Properties
 */
public class PropertiesFactoryBean
        extends PropertiesLoaderSupport implements FactoryBean<Properties>, InitializingBean {

  private boolean singleton = true;

  @Nullable
  private Properties singletonInstance;

  /**
   * Set whether a shared 'singleton' Properties instance should be
   * created, or rather a new Properties instance on each request.
   * <p>Default is "true" (a shared singleton).
   */
  public final void setSingleton(boolean singleton) {
    this.singleton = singleton;
  }

  @Override
  public final boolean isSingleton() {
    return this.singleton;
  }

  @Override
  public final void afterPropertiesSet() throws IOException {
    if (this.singleton) {
      this.singletonInstance = createProperties();
    }
  }

  @Override
  @Nullable
  public final Properties getObject() throws IOException {
    if (this.singleton) {
      return this.singletonInstance;
    }
    else {
      return createProperties();
    }
  }

  @Override
  public Class<Properties> getObjectType() {
    return Properties.class;
  }

  /**
   * Template method that subclasses may override to construct the object
   * returned by this factory. The default implementation returns the
   * plain merged Properties instance.
   * <p>Invoked on initialization of this FactoryBean in case of a
   * shared singleton; else, on each {@link #getObject()} call.
   *
   * @return the object returned by this factory
   * @throws IOException if an exception occurred during properties loading
   * @see #mergeProperties()
   */
  protected Properties createProperties() throws IOException {
    return mergeProperties();
  }

}
