/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.context.annotation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.io.PropertySourceDescriptor;
import cn.taketoday.core.io.PropertySourceFactory;
import cn.taketoday.core.io.PropertySourceProcessor;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.StringUtils;

/**
 * Registry of {@link PropertySource} processed on configuration classes.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see PropertySourceDescriptor
 * @since 4.0
 */
class PropertySourceRegistry {

  private final PropertySourceProcessor propertySourceProcessor;

  private final List<PropertySourceDescriptor> descriptors;

  public PropertySourceRegistry(PropertySourceProcessor propertySourceProcessor) {
    this.propertySourceProcessor = propertySourceProcessor;
    this.descriptors = new ArrayList<>();
  }

  public List<PropertySourceDescriptor> getDescriptors() {
    return Collections.unmodifiableList(this.descriptors);
  }

  /**
   * Process the given <code>@PropertySource</code> annotation metadata.
   *
   * @param propertySource metadata for the <code>@PropertySource</code> annotation found
   * @throws IOException if loading a property source failed
   */
  void processPropertySource(MergedAnnotation<PropertySource> propertySource) throws IOException {
    String name = propertySource.getString("name");
    if (StringUtils.isEmpty(name)) {
      name = null;
    }
    String encoding = propertySource.getString("encoding");
    if (StringUtils.isEmpty(encoding)) {
      encoding = null;
    }
    String[] locations = propertySource.getStringArray("value");
    Assert.isTrue(locations.length > 0, "At least one @PropertySource(value) location is required");
    boolean ignoreResourceNotFound = propertySource.getBoolean("ignoreResourceNotFound");

    Class<? extends PropertySourceFactory> factoryClass = propertySource.getClass("factory");
    Class<? extends PropertySourceFactory> factorClassToUse =
            (factoryClass != PropertySourceFactory.class ? factoryClass : null);
    PropertySourceDescriptor descriptor = new PropertySourceDescriptor(Arrays.asList(locations), ignoreResourceNotFound, name,
            factorClassToUse, encoding);
    this.propertySourceProcessor.processPropertySource(descriptor);
    this.descriptors.add(descriptor);
  }

}
