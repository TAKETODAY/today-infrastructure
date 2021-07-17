/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.context;

import java.util.Collection;

import cn.taketoday.context.env.ConfigurableEnvironment;
import cn.taketoday.context.env.StandardEnvironment;
import cn.taketoday.context.factory.AbstractBeanFactory;
import cn.taketoday.context.factory.StandardBeanFactory;

/**
 * Standard {@link ApplicationContext}
 *
 * @author TODAY <br>
 * 2018-09-06 13:47
 */
public class StandardApplicationContext
        extends AbstractApplicationContext implements ConfigurableApplicationContext {

  private StandardBeanFactory beanFactory;

  /**
   * Default Constructor use default {@link StandardEnvironment}
   */
  public StandardApplicationContext() {
    this(new StandardEnvironment());
  }

  /**
   * Construct with given {@link ConfigurableEnvironment}
   *
   * @param env
   *         {@link ConfigurableEnvironment} instance
   */
  public StandardApplicationContext(ConfigurableEnvironment env) {
    super(env);
  }

  /**
   * Set given properties location
   *
   * @param propertiesLocation
   *         a file or a directory to scan
   */
  public StandardApplicationContext(String propertiesLocation) {
    this();
    setPropertiesLocation(propertiesLocation);
  }

  /**
   * Start with given class set
   *
   * @param classes
   *         class set
   */
  public StandardApplicationContext(Collection<Class<?>> classes) {
    this(Constant.BLANK);
    load(classes);
  }

  /**
   * Construct with {@link StandardBeanFactory}
   *
   * @param beanFactory
   *         {@link StandardBeanFactory} instance
   */
  public StandardApplicationContext(StandardBeanFactory beanFactory) {
    this();
    this.beanFactory = beanFactory;
  }

  /**
   * Start context with given properties location and base scan packages
   *
   * @param propertiesLocation
   *         a file or a directory contains
   * @param locations
   *         scan classes from packages
   */
  public StandardApplicationContext(String propertiesLocation, String... locations) {
    this(propertiesLocation);
    load(locations);
  }

  @Override
  public StandardBeanFactory getBeanFactory() {
    final StandardBeanFactory beanFactory = this.beanFactory;
    if (beanFactory == null) {
      return this.beanFactory = createBeanFactory();
    }
    return beanFactory;
  }

  protected StandardBeanFactory createBeanFactory() {
    return new StandardBeanFactory(this);
  }

  @Override
  protected void loadBeanDefinitions(AbstractBeanFactory beanFactory, Collection<Class<?>> candidates) {
    // load beans form scanned classes
    super.loadBeanDefinitions(beanFactory, candidates);
    // @since 2.1.6
    candidates.addAll(this.beanFactory.loadMetaInfoBeans());
    // load beans form beans that annotated Configuration
    this.beanFactory.loadConfigurationBeans();
    this.beanFactory.loadMissingBean(candidates);
  }

}
