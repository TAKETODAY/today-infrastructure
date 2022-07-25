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

package cn.taketoday.jmx.export.naming;

import java.io.IOException;
import java.util.Properties;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.core.io.PropertiesUtils;
import cn.taketoday.core.io.Resource;
import cn.taketoday.jmx.export.MBeanExporter;
import cn.taketoday.jmx.support.ObjectNameManager;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.CollectionUtils;

/**
 * {@code ObjectNamingStrategy} implementation that builds
 * {@code ObjectName} instances from the key used in the
 * "beans" map passed to {@code MBeanExporter}.
 *
 * <p>Can also check object name mappings, given as {@code Properties}
 * or as {@code mappingLocations} of properties files. The key used
 * to look up is the key used in {@code MBeanExporter}'s "beans" map.
 * If no mapping is found for a given key, the key itself is used to
 * build an {@code ObjectName}.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see #setMappings
 * @see #setMappingLocation
 * @see #setMappingLocations
 * @see MBeanExporter#setBeans
 * @since 4.0
 */
public class KeyNamingStrategy implements ObjectNamingStrategy, InitializingBean {

  /**
   * {@code Log} instance for this class.
   */
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Stores the mappings of bean key to {@code ObjectName}.
   */
  @Nullable
  private Properties mappings;

  /**
   * Stores the {@code Resource}s containing properties that should be loaded
   * into the final merged set of {@code Properties} used for {@code ObjectName}
   * resolution.
   */
  @Nullable
  private Resource[] mappingLocations;

  /**
   * Stores the result of merging the {@code mappings} {@code Properties}
   * with the properties stored in the resources defined by {@code mappingLocations}.
   */
  @Nullable
  private Properties mergedMappings;

  /**
   * Set local properties, containing object name mappings, e.g. via
   * the "props" tag in XML bean definitions. These can be considered
   * defaults, to be overridden by properties loaded from files.
   */
  public void setMappings(Properties mappings) {
    this.mappings = mappings;
  }

  /**
   * Set a location of a properties file to be loaded,
   * containing object name mappings.
   */
  public void setMappingLocation(Resource location) {
    this.mappingLocations = new Resource[] { location };
  }

  /**
   * Set location of properties files to be loaded,
   * containing object name mappings.
   */
  public void setMappingLocations(Resource... mappingLocations) {
    this.mappingLocations = mappingLocations;
  }

  /**
   * Merges the {@code Properties} configured in the {@code mappings} and
   * {@code mappingLocations} into the final {@code Properties} instance
   * used for {@code ObjectName} resolution.
   */
  @Override
  public void afterPropertiesSet() throws IOException {
    this.mergedMappings = new Properties();
    CollectionUtils.mergePropertiesIntoMap(this.mappings, this.mergedMappings);

    if (this.mappingLocations != null) {
      boolean debugEnabled = logger.isDebugEnabled();
      for (Resource location : this.mappingLocations) {
        if (debugEnabled) {
          logger.debug("Loading JMX object name mappings file from {}", location);
        }
        PropertiesUtils.fillProperties(this.mergedMappings, location);
      }
    }
  }

  /**
   * Attempts to retrieve the {@code ObjectName} via the given key, trying to
   * find a mapped value in the mappings first.
   */
  @Override
  public ObjectName getObjectName(Object managedBean, @Nullable String beanKey) throws MalformedObjectNameException {
    Assert.notNull(beanKey, "KeyNamingStrategy requires bean key");
    String objectName = null;
    if (this.mergedMappings != null) {
      objectName = this.mergedMappings.getProperty(beanKey);
    }
    if (objectName == null) {
      objectName = beanKey;
    }
    return ObjectNameManager.getInstance(objectName);
  }

}
