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

package cn.taketoday.context;

import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.MapPropertySource;
import cn.taketoday.core.io.DefaultResourceLoader;
import cn.taketoday.core.io.PropertiesUtils;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StringUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.extensions.compactnotation.CompactConstructor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author TODAY 2021/10/8 22:47
 * @since 4.0
 */
public class ApplicationPropertySourcesProcessor {
  private static final Logger log = LoggerFactory.getLogger(ApplicationPropertySourcesProcessor.class);
  static boolean snakeyamlIsPresent = ClassUtils.isPresent("org.yaml.snakeyaml.Yaml");

  private final HashMap<String, Object> properties = new HashMap<>();
  private final MapPropertySource propertySource = new MapPropertySource("application", properties);

  private String propertiesLocation;
  private final ConfigurableEnvironment environment;

  private final ResourceLoader resourceLoader;

  public ApplicationPropertySourcesProcessor(ConfigurableApplicationContext context) {
    this.environment = context.getEnvironment();
    this.resourceLoader = context;
  }

  public ApplicationPropertySourcesProcessor(ConfigurableEnvironment environment) {
    Assert.notNull(environment, "environment must not be null");
    this.environment = environment;
    this.resourceLoader = new DefaultResourceLoader();
  }

  public ApplicationPropertySourcesProcessor(ConfigurableEnvironment environment, ResourceLoader resourceLoader) {
    Assert.notNull(environment, "environment must not be null");
    Assert.notNull(resourceLoader, "resourceLoader must not be null");
    this.environment = environment;
    this.resourceLoader = resourceLoader;
  }

  public void postProcessEnvironment() throws IOException {
    loadProperties();
    environment.getPropertySources().addFirst(propertySource);
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
    log.info("Found Yaml Properties Resource: [{}]", yamlResource.getLocation());
    SnakeyamlDelegate.doMapping(properties, yamlResource);
  }

  public void loadProperties(String propertiesLocation) throws IOException {
    Assert.notNull(propertiesLocation, "Properties dir can't be null");
    Resource resource = resourceLoader.getResource(propertiesLocation);
    loadProperties(resource);
  }

  /**
   * Load properties file with given path
   */
  public void loadProperties() throws IOException {
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

    // refresh active profiles
    refreshActiveProfiles();
    // load
    replaceProperties(locations);
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
    String[] defaultLocations = new String[] {
            Environment.DEFAULT_YML_FILE,
            Environment.DEFAULT_YAML_FILE,
            Environment.PROPERTIES_SUFFIX
    };

    for (String location : defaultLocations) {
      Resource propertiesResource = resourceLoader.getResource(location);
      if (propertiesResource.exists()) {
        loadProperties(propertiesResource); // loading
        locations.add(location);
      }
    }
  }

  /**
   * Replace the properties from current active profiles
   *
   * @param locations loaded properties locations
   * @throws IOException When access to the resource if any {@link IOException} occurred
   */
  protected void replaceProperties(Set<String> locations) throws IOException {
    // replace
    String[] activeProfiles = environment.getActiveProfiles();
    for (String profile : activeProfiles) {
      log.info("Replace properties by profile: [{}]", profile);

      for (String location : locations) {
        StringBuilder builder = new StringBuilder(location);
        builder.insert(builder.indexOf("."), '-' + profile);

        try {
          loadProperties(builder.toString());
        }
        catch (FileNotFoundException ignored) {
        }
      }
    }
  }

  /**
   * Set active profiles from properties
   */
  protected void refreshActiveProfiles() {
    String profiles = environment.getProperty(Environment.KEY_ACTIVE_PROFILES);
    if (StringUtils.isNotEmpty(profiles)) {
      for (String profile : StringUtils.splitAsList(profiles)) {
        environment.addActiveProfile(profile);
      }
    }
  }

  /**
   * @param resource Resource to load
   * @throws IOException if the resource is not available
   */
  private void doLoad(Resource resource) throws IOException {
    if (log.isInfoEnabled()) {
      log.info("Found Properties Resource: [{}]", resource.getLocation());
    }
    PropertiesUtils.fillProperties(properties, resource);
  }

  public void setPropertiesLocation(String propertiesLocation) {
    Assert.hasLength(propertiesLocation, "propertiesLocation must not be null");
    this.propertiesLocation = propertiesLocation;
  }

  public String getPropertiesLocation() {
    return propertiesLocation;
  }

  public ConfigurableEnvironment getEnvironment() {
    return environment;
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
