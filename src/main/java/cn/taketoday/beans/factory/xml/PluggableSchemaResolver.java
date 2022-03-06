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

package cn.taketoday.beans.factory.xml;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.PropertiesUtils;
import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.CollectionUtils;

/**
 * {@link EntityResolver} implementation that attempts to resolve schema URLs into
 * local {@link ClassPathResource classpath resources} using a set of mappings files.
 *
 * <p>By default, this class will look for mapping files in the classpath using the
 * pattern: {@code META-INF/spring.schemas} allowing for multiple files to exist on
 * the classpath at any one time.
 *
 * <p>The format of {@code META-INF/spring.schemas} is a properties file where each line
 * should be of the form {@code systemId=schema-location} where {@code schema-location}
 * should also be a schema file in the classpath. Since {@code systemId} is commonly a
 * URL, one must be careful to escape any ':' characters which are treated as delimiters
 * in properties files.
 *
 * <p>The pattern for the mapping files can be overridden using the
 * {@link #PluggableSchemaResolver(ClassLoader, String)} constructor.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 4.0
 */
public class PluggableSchemaResolver implements EntityResolver {

  /**
   * The location of the file that defines schema mappings.
   * Can be present in multiple JAR files.
   */
  public static final String DEFAULT_SCHEMA_MAPPINGS_LOCATION = "META-INF/spring.schemas";

  private static final Logger logger = LoggerFactory.getLogger(PluggableSchemaResolver.class);

  @Nullable
  private final ClassLoader classLoader;

  private final String schemaMappingsLocation;

  /** Stores the mapping of schema URL &rarr; local schema path. */
  @Nullable
  private volatile Map<String, String> schemaMappings;

  /**
   * Loads the schema URL &rarr; schema file location mappings using the default
   * mapping file pattern "META-INF/spring.schemas".
   *
   * @param classLoader the ClassLoader to use for loading
   * (can be {@code null}) to use the default ClassLoader)
   * @see PropertiesUtils#loadAllProperties(String, ClassLoader)
   */
  public PluggableSchemaResolver(@Nullable ClassLoader classLoader) {
    this.classLoader = classLoader;
    this.schemaMappingsLocation = DEFAULT_SCHEMA_MAPPINGS_LOCATION;
  }

  /**
   * Loads the schema URL &rarr; schema file location mappings using the given
   * mapping file pattern.
   *
   * @param classLoader the ClassLoader to use for loading
   * (can be {@code null}) to use the default ClassLoader)
   * @param schemaMappingsLocation the location of the file that defines schema mappings
   * (must not be empty)
   * @see PropertiesUtils#loadAllProperties(String, ClassLoader)
   */
  public PluggableSchemaResolver(@Nullable ClassLoader classLoader, String schemaMappingsLocation) {
    Assert.hasText(schemaMappingsLocation, "'schemaMappingsLocation' must not be empty");
    this.classLoader = classLoader;
    this.schemaMappingsLocation = schemaMappingsLocation;
  }

  @Override
  @Nullable
  public InputSource resolveEntity(@Nullable String publicId, @Nullable String systemId) throws IOException {
    if (logger.isTraceEnabled()) {
      logger.trace("Trying to resolve XML entity with public id [{}] and system id [{}]", publicId, systemId);
    }

    if (systemId != null) {
      String resourceLocation = getSchemaMappings().get(systemId);
      if (resourceLocation == null && systemId.startsWith("https:")) {
        // Retrieve canonical http schema mapping even for https declaration
        resourceLocation = getSchemaMappings().get("http:" + systemId.substring(6));
      }
      if (resourceLocation != null) {
        Resource resource = new ClassPathResource(resourceLocation, classLoader);
        try {
          InputSource source = new InputSource(resource.getInputStream());
          source.setPublicId(publicId);
          source.setSystemId(systemId);
          if (logger.isTraceEnabled()) {
            logger.trace("Found XML schema [{}] in classpath: {}", systemId, resourceLocation);
          }
          return source;
        }
        catch (FileNotFoundException ex) {
          if (logger.isDebugEnabled()) {
            logger.debug("Could not find XML schema [{}]: {}", systemId, resource, ex);
          }
        }
      }
    }

    // Fall back to the parser's default behavior.
    return null;
  }

  /**
   * Load the specified schema mappings lazily.
   */
  private Map<String, String> getSchemaMappings() {
    Map<String, String> schemaMappings = this.schemaMappings;
    if (schemaMappings == null) {
      synchronized(this) {
        schemaMappings = this.schemaMappings;
        if (schemaMappings == null) {
          if (logger.isTraceEnabled()) {
            logger.trace("Loading schema mappings from [{}]", schemaMappingsLocation);
          }
          try {
            Properties mappings =
                    PropertiesUtils.loadAllProperties(schemaMappingsLocation, classLoader);
            if (logger.isTraceEnabled()) {
              logger.trace("Loaded schema mappings: {}", mappings);
            }
            schemaMappings = new ConcurrentHashMap<>(mappings.size());
            CollectionUtils.mergePropertiesIntoMap(mappings, schemaMappings);
            this.schemaMappings = schemaMappings;
          }
          catch (IOException ex) {
            throw new IllegalStateException(
                    "Unable to load schema mappings from location [" + schemaMappingsLocation + "]", ex);
          }
        }
      }
    }
    return schemaMappings;
  }

  @Override
  public String toString() {
    return "EntityResolver using schema mappings " + getSchemaMappings();
  }

}
