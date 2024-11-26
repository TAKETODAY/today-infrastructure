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

package infra.orm.mybatis.annotation;

import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

import infra.context.ResourceLoaderAware;
import infra.context.properties.ConfigurationProperties;
import infra.context.properties.NestedConfigurationProperty;
import infra.core.io.PathMatchingPatternResourceLoader;
import infra.core.io.PatternResourceLoader;
import infra.core.io.Resource;
import infra.core.io.ResourceLoader;
import infra.lang.Nullable;
import infra.orm.mybatis.SqlSessionTemplate;
import infra.util.ObjectUtils;

/**
 * Configuration properties for MyBatis.
 *
 * @author Eddú Meléndez
 * @author Kazuki Shimizu
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/1 02:19
 */
@ConfigurationProperties("mybatis")
public class MybatisProperties implements ResourceLoaderAware {

  /**
   * Location of MyBatis xml config file.
   */
  @Nullable
  private String configLocation;

  /**
   * Locations of MyBatis mapper files.
   */
  private String[] mapperLocations;

  /**
   * Packages to search type aliases. (Package delimiters are ",; \t\n")
   */
  private String typeAliasesPackage;

  /**
   * The super class for filtering type alias. If this not specifies, the MyBatis deal as type alias all classes that
   * searched from typeAliasesPackage.
   */
  private Class<?> typeAliasesSuperType;

  /**
   * Packages to search for type handlers. (Package delimiters are ",; \t\n")
   */
  private String typeHandlersPackage;

  /**
   * Indicates whether perform presence check of the MyBatis xml config file.
   */
  private boolean checkConfigLocation = false;

  /**
   * Execution mode for {@link SqlSessionTemplate}.
   */
  private ExecutorType executorType;

  /**
   * The default scripting language driver class.
   */
  @Nullable
  private Class<? extends LanguageDriver> defaultScriptingLanguageDriver;

  /**
   * Externalized properties for MyBatis configuration.
   */
  private Properties configurationProperties;

  /**
   * A Configuration object for customize default settings. If {@link #configLocation} is specified, this property is
   * not used.
   */
  @Nullable
  @NestedConfigurationProperty
  private Configuration configuration;

  @Nullable
  public String getConfigLocation() {
    return this.configLocation;
  }

  public void setConfigLocation(@Nullable String configLocation) {
    this.configLocation = configLocation;
  }

  public String[] getMapperLocations() {
    return this.mapperLocations;
  }

  public void setMapperLocations(String[] mapperLocations) {
    this.mapperLocations = mapperLocations;
  }

  public String getTypeHandlersPackage() {
    return this.typeHandlersPackage;
  }

  public void setTypeHandlersPackage(String typeHandlersPackage) {
    this.typeHandlersPackage = typeHandlersPackage;
  }

  public String getTypeAliasesPackage() {
    return this.typeAliasesPackage;
  }

  public void setTypeAliasesPackage(String typeAliasesPackage) {
    this.typeAliasesPackage = typeAliasesPackage;
  }

  public Class<?> getTypeAliasesSuperType() {
    return typeAliasesSuperType;
  }

  public void setTypeAliasesSuperType(Class<?> typeAliasesSuperType) {
    this.typeAliasesSuperType = typeAliasesSuperType;
  }

  public boolean isCheckConfigLocation() {
    return this.checkConfigLocation;
  }

  public void setCheckConfigLocation(boolean checkConfigLocation) {
    this.checkConfigLocation = checkConfigLocation;
  }

  public ExecutorType getExecutorType() {
    return this.executorType;
  }

  public void setExecutorType(ExecutorType executorType) {
    this.executorType = executorType;
  }

  @Nullable
  public Class<? extends LanguageDriver> getDefaultScriptingLanguageDriver() {
    return defaultScriptingLanguageDriver;
  }

  public void setDefaultScriptingLanguageDriver(@Nullable Class<? extends LanguageDriver> defaultScriptingLanguageDriver) {
    this.defaultScriptingLanguageDriver = defaultScriptingLanguageDriver;
  }

  public Properties getConfigurationProperties() {
    return configurationProperties;
  }

  public void setConfigurationProperties(Properties configurationProperties) {
    this.configurationProperties = configurationProperties;
  }

  @Nullable
  public Configuration getConfiguration() {
    return configuration;
  }

  public void setConfiguration(@Nullable Configuration configuration) {
    this.configuration = configuration;
  }

  public Resource[] resolveMapperLocations() {
    if (ObjectUtils.isEmpty(mapperLocations)) {
      return Resource.EMPTY_ARRAY;
    }
    LinkedHashSet<Resource> resources = new LinkedHashSet<>();
    for (String location : mapperLocations) {
      resources.addAll(getResources(location));
    }
    return resources.toArray(Resource.EMPTY_ARRAY);
  }

  private Set<Resource> getResources(String location) {
    try {
      if (resourceLoader instanceof PatternResourceLoader patternResourceLoader) {
        return patternResourceLoader.getResources(location);
      }
      return new PathMatchingPatternResourceLoader().getResources(location);
    }
    catch (IOException e) {
      return Collections.emptySet();
    }
  }

  private ResourceLoader resourceLoader;

  @Override
  public void setResourceLoader(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

}
