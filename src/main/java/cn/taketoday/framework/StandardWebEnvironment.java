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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.framework;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.extensions.compactnotation.CompactConstructor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import cn.taketoday.context.StandardEnvironment;
import cn.taketoday.core.AnnotationAttributes;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.utils.AnnotationUtils;
import cn.taketoday.core.utils.ClassUtils;
import cn.taketoday.core.utils.ObjectUtils;
import cn.taketoday.core.utils.ResourceUtils;
import cn.taketoday.core.utils.StringUtils;
import cn.taketoday.framework.annotation.PropertiesSource;
import cn.taketoday.framework.utils.ApplicationUtils;
import cn.taketoday.logger.Logger;
import cn.taketoday.logger.LoggerFactory;

/**
 * @author TODAY <br>
 * 2019-06-17 22:34
 */
public class StandardWebEnvironment extends StandardEnvironment {
  static boolean snakeyamlIsPresent = ClassUtils.isPresent("org.yaml.snakeyaml.Yaml");

  private static final Logger log = LoggerFactory.getLogger(StandardWebEnvironment.class);

  private final String[] arguments;
  private final Class<?> applicationClass;

  public StandardWebEnvironment() {
    this(null);
  }

  public StandardWebEnvironment(Class<?> applicationClass, String... arguments) {
    this.arguments = arguments;
    this.applicationClass = applicationClass;
  }

  @Override
  public void loadProperties() throws IOException {
    // load default properties source : application.yaml or application.properties
    LinkedHashSet<String> locations = new LinkedHashSet<>(8);
    loadDefaultResources(locations);

    if (locations.isEmpty()) {
      super.loadProperties(Constant.BLANK); // scan class path properties files
    }

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
    getProperties().putAll(ApplicationUtils.parseCommandArguments(arguments));

    refreshActiveProfiles();
    replaceProperties(locations);
  }

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
            Constant.DEFAULT_YML_FILE,
            Constant.DEFAULT_YAML_FILE,
            Constant.DEFAULT_PROPERTIES_FILE
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
   * Is yaml?
   *
   * @param propertiesLocation
   *         location
   */
  protected boolean isYamlProperties(String propertiesLocation) {
    return propertiesLocation.endsWith(".yaml") || propertiesLocation.endsWith(".yml");
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

      for (final String location : locations) {
        final StringBuilder builder = new StringBuilder(location);
        builder.insert(builder.indexOf("."), '-' + profile);

        try {
          super.loadProperties(builder.toString());
        }
        catch (FileNotFoundException ignored) { }
      }
    }
  }

  @Override
  protected void loadProperties(Resource resource) throws IOException {
    if (isYamlProperties(resource.getName())) {
      if (snakeyamlIsPresent) {
        loadFromYmal(getProperties(), resource);
      }
      else {
        log.warn("'org.yaml.snakeyaml.Yaml' does not exist in your classpath, yaml config file will be ignored");
      }
    }
    else {
      super.loadProperties(resource);
    }
  }

  protected void loadFromYmal(final Properties properties, final Resource yamlResource) throws IOException {
    log.info("Found Yaml Properties Resource: [{}]", yamlResource.getLocation());
    SnakeyamlDelegate.doMapping(properties, yamlResource);
  }

  static class SnakeyamlDelegate {

    protected static void doMapping(final Properties properties, Resource yamlResource) throws IOException {
      final Map<String, Object> base = new Yaml(new CompactConstructor()).load(yamlResource.getInputStream());
      SnakeyamlDelegate.doMapping(properties, base, null);
    }

    @SuppressWarnings("unchecked")
    protected static void doMapping(final Properties properties, final Map<String, Object> base, final String prefix) {
      for (final Entry<String, Object> entry : base.entrySet()) {
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
