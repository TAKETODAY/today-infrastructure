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
