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

package infra.test.context.junit4.hybrid;

import infra.beans.factory.support.BeanDefinitionReader;
import infra.beans.factory.xml.XmlBeanDefinitionReader;
import infra.context.annotation.AnnotatedBeanDefinitionReader;
import infra.context.support.GenericApplicationContext;
import infra.lang.Assert;
import infra.test.context.ContextConfigurationAttributes;
import infra.test.context.MergedContextConfiguration;
import infra.test.context.SmartContextLoader;
import infra.test.context.support.AbstractGenericContextLoader;

import static infra.test.context.support.AnnotationConfigContextLoaderUtils.detectDefaultConfigurationClasses;

/**
 * Hybrid {@link SmartContextLoader} that supports path-based and class-based
 * resources simultaneously.
 * <p>This test loader is inspired by Infra.
 * <p>Detects defaults for XML configuration and annotated classes.
 * <p>Beans from XML configuration always override those from annotated classes.
 *
 * @author Sam Brannen
 * @since 4.0
 */
public class HybridContextLoader extends AbstractGenericContextLoader {

  @Override
  protected void validateMergedContextConfiguration(MergedContextConfiguration mergedConfig) {
    Assert.isTrue(mergedConfig.hasClasses() || mergedConfig.hasLocations(), getClass().getSimpleName()
            + " requires either classes or locations");
  }

  @Override
  public void processContextConfiguration(ContextConfigurationAttributes configAttributes) {
    // Detect default XML configuration files:
    super.processContextConfiguration(configAttributes);

    // Detect default configuration classes:
    if (!configAttributes.hasClasses() && isGenerateDefaultLocations()) {
      configAttributes.setClasses(detectDefaultConfigurationClasses(configAttributes.getDeclaringClass()));
    }
  }

  @Override
  protected void loadBeanDefinitions(GenericApplicationContext context, MergedContextConfiguration mergedConfig) {
    // Order doesn't matter: <bean> always wins over @Bean.
    new XmlBeanDefinitionReader(context).loadBeanDefinitions(mergedConfig.getLocations());
    new AnnotatedBeanDefinitionReader(context).register(mergedConfig.getClasses());
  }

  @Override
  protected BeanDefinitionReader createBeanDefinitionReader(GenericApplicationContext context) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + " doesn't support this");
  }

  @Override
  protected String getResourceSuffix() {
    return "-context.xml";
  }

}
