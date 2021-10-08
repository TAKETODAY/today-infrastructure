/*
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.context.loader;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.extensions.compactnotation.CompactConstructor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.EnvironmentPostProcessor;
import cn.taketoday.core.AnnotationAttributes;
import cn.taketoday.core.Assert;
import cn.taketoday.core.Constant;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceFilter;
import cn.taketoday.framework.config.PropertiesSource;
import cn.taketoday.framework.utils.WebApplicationUtils;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ResourceUtils;
import cn.taketoday.util.StringUtils;

/**
 * @author TODAY 2021/10/8 22:47
 * @since 4.0
 */
public class ProEnvironmentPostProcessor implements EnvironmentPostProcessor {

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, ConfigurableApplicationContext context) {

  }

  protected void postLoadingProperties(Set<String> locations) throws IOException {

    // load properties from starter class annotated @PropertiesSource
    if (applicationClass != null) {
      AnnotationAttributes[] attributes =
              AnnotationUtils.getAttributesArray(applicationClass, PropertiesSource.class);
      if (ObjectUtils.isNotEmpty(attributes)) {
        for (AnnotationAttributes attribute : attributes) {
          for (String propertiesLocation : StringUtils.split(attribute.getString(Constant.VALUE))) {
            if (!locations.contains(propertiesLocation)) {
              loadProperties(propertiesLocation);
              locations.add(propertiesLocation);
            }
          }
        }
      }
    }

    // arguments
    getProperties().putAll(WebApplicationUtils.parseCommandLineArguments(arguments));
  }

  static class InitPropertySourcesDelegate {
    static boolean snakeyamlIsPresent = ClassUtils.isPresent("org.yaml.snakeyaml.Yaml");

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
      if (isYamlProperties(propertiesResource.getName())) {
        if (snakeyamlIsPresent) {
          // load yaml files
          loadFromYmal(getProperties(), propertiesResource);
        }
        else {
          log.warn("'org.yaml.snakeyaml.Yaml' does not exist in your classpath, yaml config file will be ignored");
        }
      }
      else {
        // load properties files
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
            return name.endsWith(PROPERTIES_SUFFIX) && !name.startsWith("pom"); // pom.properties
          };
          doLoadFromDirectory(propertiesResource, this.properties, propertiesFileFilter);
        }
        else {
          doLoad(this.properties, propertiesResource);
        }
      }
    }

    /**
     * Is yaml?
     *
     * @param propertiesLocation
     *         location
     */
    private boolean isYamlProperties(String propertiesLocation) {
      return propertiesLocation.endsWith(".yaml") || propertiesLocation.endsWith(".yml");
    }

    private void loadFromYmal(final Properties properties, final Resource yamlResource) throws IOException {
      log.info("Found Yaml Properties Resource: [{}]", yamlResource.getLocation());
      SnakeyamlDelegate.doMapping(properties, yamlResource);
    }

    public void loadProperties(String propertiesLocation) throws IOException {
      Assert.notNull(propertiesLocation, "Properties dir can't be null");
      Resource resource = ResourceUtils.getResource(propertiesLocation);
      loadProperties(resource);
    }

    /**
     * Load properties file with given path
     */
    public void loadProperties() throws IOException {
      // load default properties source : application.yaml or application.properties
      LinkedHashSet<String> locations = new LinkedHashSet<>(8); // loaded locations
      loadDefaultResources(locations);

      if (locations.isEmpty()) {
        // scan class path properties files
        for (final String propertiesLocation : StringUtils.splitAsList(propertiesLocation)) {
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
     * @param locations
     *         loaded file locations
     *
     * @throws IOException
     *         if any io exception occurred when loading properties files
     */
    protected void postLoadingProperties(Set<String> locations) throws IOException { }

    /**
     * load default properties files
     *
     * @param locations
     *         loaded files
     *
     * @throws IOException
     *         If load error
     */
    protected void loadDefaultResources(final Set<String> locations) throws IOException {
      final String[] defaultLocations = new String[] {
              DEFAULT_YML_FILE,
              DEFAULT_YAML_FILE,
              PROPERTIES_SUFFIX
      };

      for (final String location : defaultLocations) {
        final Resource propertiesResource = ResourceUtils.getResource(location);
        if (propertiesResource.exists()) {
          loadProperties(propertiesResource); // loading
          setPropertiesLocation(location);// can override
          locations.add(location);
        }
      }
    }

    /**
     * Replace the properties from current active profiles
     *
     * @param locations
     *         loaded properties locations
     *
     * @throws IOException
     *         When access to the resource if any {@link IOException} occurred
     */
    protected void replaceProperties(Set<String> locations) throws IOException {
      // replace
      final String[] activeProfiles = getActiveProfiles();
      for (final String profile : activeProfiles) {
        log.info("Replace properties by profile: [{}]", profile);

        for (final String location : locations) {
          final StringBuilder builder = new StringBuilder(location);
          builder.insert(builder.indexOf("."), '-' + profile);

          try {
            loadProperties(builder.toString());
          }
          catch (FileNotFoundException ignored) { }
        }
      }
    }

    /**
     * Set active profiles from properties
     */
    protected void refreshActiveProfiles() {
      final String profiles = getProperty(KEY_ACTIVE_PROFILES);

      if (StringUtils.isNotEmpty(profiles)) {
        addActiveProfile(StringUtils.splitAsList(profiles));
        log.info("Refresh active profiles: {}", activeProfiles);
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

    public String getPropertiesLocation() {
      return propertiesLocation;
    }

    static class SnakeyamlDelegate {

      protected static void doMapping(final Properties properties, Resource yamlResource) throws IOException {
        final Map<String, Object> base = new Yaml(new CompactConstructor()).load(yamlResource.getInputStream());
        SnakeyamlDelegate.doMapping(properties, base, null);
      }

      @SuppressWarnings("unchecked")
      protected static void doMapping(final Properties properties, final Map<String, Object> base, final String prefix) {
        for (final Map.Entry<String, Object> entry : base.entrySet()) {
          String key = entry.getKey();
          final Object value = entry.getValue();
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

}
