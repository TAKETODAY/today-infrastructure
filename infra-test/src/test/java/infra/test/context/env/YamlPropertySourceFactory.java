/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
