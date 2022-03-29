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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.context.support;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.extensions.compactnotation.CompactConstructor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.EnvironmentCapable;
import cn.taketoday.core.env.MapPropertySource;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.core.io.DefaultResourceLoader;
import cn.taketoday.core.io.PropertiesUtils;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StringUtils;

/**
 * load properties into 'application' MapPropertySource
 *
 * @author TODAY 2021/10/8 22:47
 * @since 4.0
 */
@Deprecated
public class ApplicationPropertySourcesProcessor {
  private static final Logger log = LoggerFactory.getLogger(ApplicationPropertySourcesProcessor.class);
  static boolean snakeyamlIsPresent = ClassUtils.isPresent("org.yaml.snakeyaml.Yaml");

  private final HashMap<String, Object> properties = new HashMap<>();
  private final MapPropertySource propertySource = new MapPropertySource("application", properties);

  @Nullable
  private String propertiesLocation;

  private final ResourceLoader resourceLoader;

  private String[] defaultLocations = new String[] {
          Environment.DEFAULT_YML_FILE,
          Environment.DEFAULT_YAML_FILE,
          Environment.DEFAULT_PROPERTIES_FILE
  };

  public ApplicationPropertySourcesProcessor(ConfigurableApplicationContext context) {
    this.resourceLoader = context;
  }

  public ApplicationPropertySourcesProcessor() {
    this.resourceLoader = new DefaultResourceLoader();
  }

  public ApplicationPropertySourcesProcessor(ResourceLoader resourceLoader) {
    Assert.notNull(resourceLoader, "resourceLoader must not be null");
    this.resourceLoader = resourceLoader;
  }

  public void setDefaultLocations(String... defaultLocations) {
    this.defaultLocations = defaultLocations;
  }

  public void postProcessEnvironment() throws IOException {
    if (resourceLoader instanceof EnvironmentCapable environmentCapable
            && environmentCapable.getEnvironment() instanceof ConfigurableEnvironment environment) {
      postProcessEnvironment(environment);
    }
  }

  public void postProcessEnvironment(ConfigurableEnvironment environment) throws IOException {
    environment.getPropertySources().addBefore(
            StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, propertySource);
    loadProperties(environment);
  }

  /**
   * Load properties from {@link Resource}
   *
   * @param propertiesResource {@link Resource}
   * @throws IOException When access to the resource if any {@link IOException} occurred
   */
  protected void loadProperties(Resource propertiesResource) throws IOException {
    if (isYamlProperties(propertiesResource.getName())) {
      if (snakeyamlIsPresent) {
        // load yaml files
        loadFromYmal(propertiesResource);
      }
      else {
        log.warn("'org.yaml.snakeyaml.Yaml' does not exist in your classpath, yaml config file will be ignored");
      }
    }
    else {
      // load properties files
      if (propertiesResource.exists()) {
        doLoad(propertiesResource);
      }
      else {
        log.warn("The resource: [{}] you provided that doesn't exist", propertiesResource);
      }
    }
  }

  /**
   * Is yaml?
   *
   * @param propertiesLocation location
   */
  private boolean isYamlProperties(String propertiesLocation) {
    return propertiesLocation.endsWith(".yaml") || propertiesLocation.endsWith(".yml");
  }

  private void loadFromYmal(Resource yamlResource) throws IOException {
    log.debug("Found Yaml properties resource: [{}]", yamlResource);
    SnakeyamlDelegate.doMapping(properties, yamlResource);
  }

  public void loadProperties(String propertiesLocation) throws IOException {
    Assert.notNull(propertiesLocation, "propertiesLocation is required");
    Resource resource = resourceLoader.getResource(propertiesLocation);
    loadProperties(resource);
  }

  /**
   * Load properties file with given path
   *
   * @param environment environment
   */
  private void loadProperties(ConfigurableEnvironment environment) throws IOException {
    // load default properties source : application.yaml or application.properties
    LinkedHashSet<String> locations = new LinkedHashSet<>(8); // loaded locations
    loadDefaultResources(locations);

    if (StringUtils.hasText(propertiesLocation)) {
      for (String propertiesLocation : StringUtils.splitAsList(propertiesLocation)) {
        loadProperties(propertiesLocation);
      }
    }
    // load other files
    postLoadingProperties(locations);

    // load
    replaceProperties(environment, locations);
  }

  /**
   * subclasses load other files
   *
   * @param locations loaded file locations
   * @throws IOException if any io exception occurred when loading properties files
   */
  protected void postLoadingProperties(Set<String> locations) throws IOException {
    // load properties from starter class annotated @PropertiesSource
  }

  /**
   * load default properties files
   *
   * @param locations loaded files
   * @throws IOException If load error
   */
  protected void loadDefaultResources(Set<String> locations) throws IOException {
    if (defaultLocations != null) {
      for (String location : defaultLocations) {
        Resource propertiesResource = resourceLoader.getResource(location);
        if (propertiesResource.exists()) {
          loadProperties(propertiesResource); // loading
          locations.add(location);
        }
      }
    }
  }

  /**
   * Replace the properties from current active profiles
   *
   * @param environment environment
   * @param locations loaded properties locations
   * @throws IOException When access to the resource if any {@link IOException} occurred
   */
  protected void replaceProperties(Environment environment, Set<String> locations) throws IOException {
    // replace
    String[] activeProfiles = environment.getActiveProfiles();
    for (String profile : activeProfiles) {
      log.debug("Replace properties by profile: [{}]", profile);

      for (String location : locations) {
        StringBuilder builder = new StringBuilder(location);
        builder.insert(builder.indexOf("."), '-' + profile);

        try {
          loadProperties(builder.toString());
        }
        catch (FileNotFoundException ignored) { }
      }
    }
  }

  /**
   * @param resource Resource to load
   * @throws IOException if the resource is not available
   */
  private void doLoad(Resource resource) throws IOException {
    if (log.isDebugEnabled()) {
      log.debug("Found properties resource: [{}]", resource);
    }
    PropertiesUtils.fillProperties(properties, resource);
  }

  public void setPropertiesLocation(@Nullable String propertiesLocation) {
    this.propertiesLocation = propertiesLocation;
  }

  @Nullable
  public String getPropertiesLocation() {
    return propertiesLocation;
  }

  public MapPropertySource getPropertySource() {
    return propertySource;
  }

  public HashMap<String, Object> getSource() {
    return properties;
  }

  static class SnakeyamlDelegate {

    protected static void doMapping(Map<String, Object> properties, Resource yamlResource) throws IOException {
      Map<String, Object> base = new Yaml(new CompactConstructor()).load(yamlResource.getInputStream());
      SnakeyamlDelegate.doMapping(properties, base, null);
    }

    @SuppressWarnings("unchecked")
    protected static void doMapping(Map<String, Object> properties, Map<String, Object> base, String prefix) {
      for (Map.Entry<String, Object> entry : base.entrySet()) {
        String key = entry.getKey();
        Object value = entry.getValue();
        key = prefix == null ? key : (prefix + '.' + key);
        if (value instanceof Map) {
          doMapping(properties, (Map<String, Object>) value, key);
        }
        else {
          properties.put(key, value);
        }
      }
    }
  }

}
