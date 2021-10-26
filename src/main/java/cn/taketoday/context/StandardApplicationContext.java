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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.context;

import java.io.IOException;
import java.util.List;

import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.StandardBeanFactory;
import cn.taketoday.context.loader.BeanDefinitionLoader;
import cn.taketoday.context.loader.ConfigurationBeanReader;
import cn.taketoday.context.loader.DefinitionLoadingContext;
import cn.taketoday.context.loader.ScanningBeanDefinitionReader;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.util.StringUtils;

/**
 * Standard {@link ApplicationContext}
 *
 * @author TODAY 2018-09-06 13:47
 */
public class StandardApplicationContext
        extends DefaultApplicationContext implements ConfigurableApplicationContext, BeanDefinitionRegistry, AnnotationConfigRegistry {

  @Nullable
  private String propertiesLocation;

  private DefinitionLoadingContext loadingContext;
  private ScanningBeanDefinitionReader scanningReader;

  /**
   * Default Constructor
   */
  public StandardApplicationContext() { }

  /**
   * Construct with {@link StandardBeanFactory}
   *
   * @param beanFactory {@link StandardBeanFactory} instance
   */
  public StandardApplicationContext(StandardBeanFactory beanFactory) {
    super(beanFactory);
  }

  /**
   * Create a new StandardApplicationContext with the given parent.
   *
   * @param parent the parent application context
   * @see #registerBeanDefinition(String, BeanDefinition)
   * @see #refresh()
   */
  public StandardApplicationContext(@Nullable ApplicationContext parent) {
    setParent(parent);
  }

  /**
   * Create a new DefaultApplicationContext with the given StandardBeanFactory.
   *
   * @param beanFactory the StandardBeanFactory instance to use for this context
   * @param parent the parent application context
   * @see #registerBeanDefinition(String, BeanDefinition)
   * @see #refresh()
   */
  public StandardApplicationContext(StandardBeanFactory beanFactory, ApplicationContext parent) {
    this(beanFactory);
    setParent(parent);
  }

  /**
   * Set given properties location
   *
   * @param propertiesLocation a file or a di rectory to scan
   */
  public StandardApplicationContext(String propertiesLocation) {
    setPropertiesLocation(propertiesLocation);
  }

  /**
   * Start with given class set
   *
   * @param components one or more component classes,
   * e.g. {@link cn.taketoday.lang.Configuration @Configuration} classes
   * @see #refresh()
   * @see #register(Class[])
   */
  public StandardApplicationContext(Class<?>... components) {
    register(components);
    refresh();
  }

  /**
   * Start context with given properties location and base scan packages
   *
   * @param propertiesLocation a file or a directory contains
   * @param basePackages scan classes from packages
   * @see #refresh()
   */
  public StandardApplicationContext(String propertiesLocation, String... basePackages) {
    setPropertiesLocation(propertiesLocation);
    scan(basePackages);
    refresh();
  }

  public void setPropertiesLocation(@Nullable String propertiesLocation) {
    this.propertiesLocation = propertiesLocation;
  }

  @Nullable
  public String getPropertiesLocation() {
    return propertiesLocation;
  }

  //---------------------------------------------------------------------
  // Implementation of AbstractApplicationContext
  //---------------------------------------------------------------------

  @Override
  public void prepareBeanFactory(ConfigurableBeanFactory beanFactory) {
    super.prepareBeanFactory(beanFactory);
    List<BeanDefinitionLoader> strategies = TodayStrategies.getDetector().getStrategies(
            BeanDefinitionLoader.class, beanFactory);

    DefinitionLoadingContext loadingContext = loadingContext();
    for (BeanDefinitionLoader loader : strategies) {
      loader.loadBeanDefinitions(loadingContext);
    }
  }

  @Override
  protected void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) {
    addFactoryPostProcessors(new ConfigurationBeanReader(loadingContext()));
    super.postProcessBeanFactory(beanFactory);
  }

  @Override
  protected void initPropertySources(ConfigurableEnvironment environment) throws IOException {
    super.initPropertySources(environment);

    ApplicationPropertySourcesProcessor processor = new ApplicationPropertySourcesProcessor(this);
    if (StringUtils.isNotEmpty(propertiesLocation)) {
      processor.setPropertiesLocation(propertiesLocation);
    }
    processor.postProcessEnvironment();

    // prepare properties
    TodayStrategies detector = TodayStrategies.getDetector();
    List<EnvironmentPostProcessor> postProcessors = detector.getStrategies(
            EnvironmentPostProcessor.class, getBeanFactory());
    for (EnvironmentPostProcessor postProcessor : postProcessors) {
      postProcessor.postProcessEnvironment(environment, this);
    }
  }

  @Override
  public void register(Class<?>... components) {
    getBeanDefinitionReader().registerBean(components);
  }

  @Override
  public void scan(String... basePackages) {
    scanningReader().scanPackages(basePackages);
  }

  private ScanningBeanDefinitionReader scanningReader() {
    if (scanningReader == null) {
      scanningReader = new ScanningBeanDefinitionReader(loadingContext());
    }
    return scanningReader;
  }

  private DefinitionLoadingContext loadingContext() {
    if (loadingContext == null) {
      loadingContext = new DefinitionLoadingContext(beanFactory, this);
    }
    return loadingContext;
  }
}
