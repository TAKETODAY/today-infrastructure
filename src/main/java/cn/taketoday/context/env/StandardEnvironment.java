/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 * <p>
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.context.env;

import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import cn.taketoday.context.BeanNameCreator;
import cn.taketoday.context.ConcurrentProperties;
import cn.taketoday.context.Constant;
import cn.taketoday.context.factory.BeanDefinitionRegistry;
import cn.taketoday.context.io.Resource;
import cn.taketoday.context.io.ResourceFilter;
import cn.taketoday.context.loader.BeanDefinitionLoader;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.ResourceUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.expression.ExpressionProcessor;

/**
 * Standard implementation of {@link Environment}
 *
 * @author TODAY <br>
 * 2018-11-14 21:23
 */
public class StandardEnvironment implements ConfigurableEnvironment {
  private static final Logger log = LoggerFactory.getLogger(StandardEnvironment.class);

  private final HashSet<String> activeProfiles = new HashSet<>(4);
  private final Properties properties = new ConcurrentProperties();
  private BeanNameCreator beanNameCreator;

  /** resolve beanDefinition which It is marked annotation */
  private BeanDefinitionLoader beanDefinitionLoader;
  /** storage BeanDefinition */
  private BeanDefinitionRegistry beanDefinitionRegistry;

  private String propertiesLocation = Constant.BLANK; // default ""

  private ExpressionProcessor expressionProcessor;

  public StandardEnvironment() {
    if (System.getSecurityManager() != null) {
      AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
        properties.putAll(System.getProperties());
        System.setProperties(properties);
        return null;
      });
    }
    else {
      properties.putAll(System.getProperties());
      System.setProperties(properties);
    }
  }

  @Override
  public Properties getProperties() {
    return properties;
  }

  @Override
  public boolean containsProperty(String key) {
    return properties.containsKey(key);
  }

  @Override
  public String getProperty(String key) {
    return properties.getProperty(key);
  }

  @Override
  public String getProperty(String key, String defaultValue) {
    return properties.getProperty(key, defaultValue);
  }

  @Override
  public BeanDefinitionRegistry getBeanDefinitionRegistry() {
    return beanDefinitionRegistry;
  }

  @Override
  public BeanDefinitionLoader getBeanDefinitionLoader() {
    return beanDefinitionLoader;
  }

  @Override
  public String[] getActiveProfiles() {
    return StringUtils.toStringArray(activeProfiles);
  }

  // ---ConfigurableEnvironment

  @Override
  public void setActiveProfiles(String... profiles) {
    Collections.addAll(activeProfiles, profiles);
    log.info("Active profiles: {}", activeProfiles);
  }

  @Override
  public void setProperty(String key, String value) {
    properties.setProperty(key, value);
  }

  @Override
  public void addActiveProfile(String profile) {
    log.info("Add active profile: [{}]", profile);
    activeProfiles.add(profile);
  }

  /**
   * Load properties from {@link Resource}
   *
   * @param propertiesResource
   *         {@link Resource}
   *
   * @throws IOException
   *         When access to the resource if any {@link IOException} occurred
   */
  protected void loadProperties(final Resource propertiesResource) throws IOException {

    if (!propertiesResource.exists()) {
      log.warn("The resource: [{}] you provided that doesn't exist", propertiesResource);
      return;
    }

    if (propertiesResource.isDirectory()) {
      log.debug("Start scanning properties resource.");

      final ResourceFilter propertiesFileFilter = (final Resource file) -> {
        if (file.isDirectory()) {
          return true;
        }
        final String name = file.getName();
        return name.endsWith(Constant.PROPERTIES_SUFFIX) && !name.startsWith("pom"); // pom.properties
      };
      doLoadFromDirectory(propertiesResource, this.properties, propertiesFileFilter);
    }
    else {
      doLoad(this.properties, propertiesResource);
    }
  }

  @Override
  public void loadProperties(String propertiesLocation) throws IOException {
    loadProperties(ResourceUtils.getResource(Objects.requireNonNull(propertiesLocation, "Properties dir can't be null")));
  }

  /**
   * Load properties file with given path
   */
  @Override
  public void loadProperties() throws IOException {

    for (final String propertiesLocation : StringUtils.split(propertiesLocation)) {
      loadProperties(propertiesLocation);
    }

    refreshActiveProfiles();
  }

  /**
   * Set active profiles from properties
   */
  protected void refreshActiveProfiles() {

    final String profiles = getProperty(Constant.KEY_ACTIVE_PROFILES);

    if (StringUtils.isNotEmpty(profiles)) {
      setActiveProfiles(StringUtils.split(profiles));
    }
  }

  /**
   * Do load
   *
   * @param directory
   *         base dir
   * @param properties
   *         properties
   *
   * @throws IOException
   *         if the resource is not available
   */
  public static void doLoadFromDirectory(final Resource directory,
                                         final Properties properties,
                                         final ResourceFilter propertiesFileFilter) throws IOException //
  {
    final Resource[] listResources = directory.list(propertiesFileFilter);
    for (final Resource resource : listResources) {
      if (resource.isDirectory()) { // recursive
        doLoadFromDirectory(resource, properties, propertiesFileFilter);
        continue;
      }
      doLoad(properties, resource);
    }
  }

  /**
   * @param properties
   *         Target properties to store
   * @param resource
   *         Resource to load
   *
   * @throws IOException
   *         if the resource is not available
   */
  public static void doLoad(Properties properties, final Resource resource) throws IOException {
    if (log.isInfoEnabled()) {
      log.info("Found Properties Resource: [{}]", resource.getLocation());
    }

    try (final InputStream inputStream = resource.getInputStream()) {
      properties.load(inputStream);
    }
  }

  @Override
  public ConfigurableEnvironment setBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry) {
    this.beanDefinitionRegistry = beanDefinitionRegistry;
    return this;
  }

  @Override
  public ConfigurableEnvironment setBeanDefinitionLoader(BeanDefinitionLoader beanDefinitionLoader) {
    this.beanDefinitionLoader = beanDefinitionLoader;
    return this;
  }

  @Override
  public boolean acceptsProfiles(String... profiles) {

    final Set<String> activeProfiles = this.activeProfiles;
    for (final String profile : Objects.requireNonNull(profiles)) {
      if (StringUtils.isNotEmpty(profile) && profile.charAt(0) == '!') {
        if (!activeProfiles.contains(profile.substring(1))) {
          return true;
        }
      }
      else if (activeProfiles.contains(profile)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public ConfigurableEnvironment setBeanNameCreator(BeanNameCreator beanNameCreator) {
    this.beanNameCreator = beanNameCreator;
    return this;
  }

  /**
   * Get a bean name creator
   *
   * @return {@link BeanNameCreator}
   */
  @Override
  public BeanNameCreator getBeanNameCreator() {
    final BeanNameCreator ret = this.beanNameCreator;
    if (ret == null) {
      return this.beanNameCreator = createBeanNameCreator();
    }
    return ret;
  }

  /**
   * create {@link BeanNameCreator}
   *
   * @return a default {@link BeanNameCreator}
   */
  protected BeanNameCreator createBeanNameCreator() {
    return new DefaultBeanNameCreator(this);
  }

  @Override
  public ExpressionProcessor getExpressionProcessor() {
    return expressionProcessor;
  }

  @Override
  public ConfigurableEnvironment setExpressionProcessor(ExpressionProcessor expressionProcessor) {
    this.expressionProcessor = expressionProcessor;
    return this;
  }

  @Override
  public ConfigurableEnvironment setPropertiesLocation(String propertiesLocation) {
    this.propertiesLocation = propertiesLocation;
    return this;
  }

  public String getPropertiesLocation() {
    return propertiesLocation;
  }

}
