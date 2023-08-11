/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.test.context.env;

import java.util.Properties;

import cn.taketoday.beans.factory.config.YamlPropertiesFactoryBean;
import cn.taketoday.core.env.PropertiesPropertySource;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.io.EncodedResource;
import cn.taketoday.core.io.PropertySourceFactory;
import cn.taketoday.core.io.Resource;
import cn.taketoday.util.StringUtils;

/**
 * Demo {@link PropertySourceFactory} that provides YAML support.
 *
 * @author Sam Brannen
 * @since 4.0
 */
class YamlPropertySourceFactory implements PropertySourceFactory {

  @Override
  public PropertySource<?> createPropertySource(String name, EncodedResource encodedResource) {
    Resource resource = encodedResource.getResource();
    if (!StringUtils.hasText(name)) {
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
    if (!StringUtils.hasText(name)) {
      name = resource.getClass().getSimpleName() + "@" + System.identityHashCode(resource);
    }
    return name;
  }

}
