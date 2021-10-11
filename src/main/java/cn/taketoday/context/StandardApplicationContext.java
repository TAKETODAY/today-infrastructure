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
import java.util.Collection;
import java.util.List;

import cn.taketoday.beans.factory.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.StandardBeanFactory;
import cn.taketoday.context.loader.BeanDefinitionLoader;
import cn.taketoday.context.loader.ScanningBeanDefinitionReader;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.core.env.ConfigurableEnvironment;

/**
 * Standard {@link ApplicationContext}
 *
 * @author TODAY 2018-09-06 13:47
 */
public class StandardApplicationContext
        extends DefaultApplicationContext implements ConfigurableApplicationContext, BeanDefinitionRegistry, AnnotationConfigRegistry {

  private String propertiesLocation;
  private final ScanningBeanDefinitionReader scanningReader = new ScanningBeanDefinitionReader(this);

  /**
   * Default Constructor
   */
  public StandardApplicationContext() { }

  /**
   * Set given properties location
   *
   * @param propertiesLocation
   *         a file or a di rectory to scan
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
  }

  /**
   * Construct with {@link StandardBeanFactory}
   *
   * @param beanFactory
   *         {@link StandardBeanFactory} instance
   */
  public StandardApplicationContext(StandardBeanFactory beanFactory) {
    super(beanFactory);
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
    scan(locations);
  }

  public void setPropertiesLocation(String propertiesLocation) {
    this.propertiesLocation = propertiesLocation;
  }

  public String getPropertiesLocation() {
    return propertiesLocation;
  }

  @Override
  public void prepareBeanFactory() {
    super.prepareBeanFactory();
    List<BeanDefinitionLoader> strategies = TodayStrategies.getDetector().getStrategies(
            BeanDefinitionLoader.class, this);

    StandardBeanFactory beanFactory = getBeanFactory();
    for (BeanDefinitionLoader loader : strategies) {
      loader.loadBeanDefinitions(this, beanFactory);
    }
  }

  @Override
  protected void initPropertySources() throws IOException {
    super.initPropertySources();
    ConfigurableEnvironment environment = getEnvironment();
    ApplicationPropertySourcesProcessor processor = new ApplicationPropertySourcesProcessor(this);
    processor.setPropertiesLocation(propertiesLocation);
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
  public void importBeans(Class<?>... components) {
    beanDefinitionReader.importBeans(components);
  }

  @Override
  public void scan(String... basePackages) {
    scanningReader.scan(basePackages);
  }

}
