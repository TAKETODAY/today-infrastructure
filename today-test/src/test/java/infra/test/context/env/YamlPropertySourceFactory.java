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

package infra.test.context.env;

import java.util.Properties;

import infra.beans.factory.config.YamlPropertiesFactoryBean;
import infra.core.env.PropertiesPropertySource;
import infra.core.env.PropertySource;
import infra.core.io.EncodedResource;
import infra.core.io.PropertySourceFactory;
import infra.core.io.Resource;
import infra.util.StringUtils;

/**
 * Demo {@link PropertySourceFactory} that provides YAML support.
 *
 * @author Sam Brannen
 * @since 4.0
 */
public class YamlPropertySourceFactory implements PropertySourceFactory {

  @Override
  public PropertySource<?> createPropertySource(String name, EncodedResource encodedResource) {
    Resource resource = encodedResource.getResource();
    if (StringUtils.isBlank(name)) {
      name = getNameForResource(resource);
    }
    YamlPropertiesFactoryBean factoryBean = new YamlPropertiesFactoryBean();
    factoryBean.setResources(resource);
    factoryBean.afterPropertiesSet();
    Properties properties = factoryBean.getObject();
    return new PropertiesPropertySource(name, properties);
  }

  /**
   * Return the description for the given Resource; if the description is
   * empty, return the class name of the resource plus its identity hash code.
   */
  private static String getNameForResource(Resource resource) {
    String name = resource.toString();
    if (StringUtils.isBlank(name)) {
      name = resource.getClass().getSimpleName() + "@" + System.identityHashCode(resource);
    }
    return name;
  }

}
