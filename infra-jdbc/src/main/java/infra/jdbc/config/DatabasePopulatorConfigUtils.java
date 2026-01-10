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

package infra.jdbc.config;

import org.jspecify.annotations.Nullable;
import org.w3c.dom.Element;

import java.util.List;

import infra.beans.BeanMetadataElement;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.TypedStringValue;
import infra.beans.factory.support.BeanDefinitionBuilder;
import infra.beans.factory.support.ManagedList;
import infra.jdbc.datasource.init.CompositeDatabasePopulator;
import infra.jdbc.datasource.init.ResourceDatabasePopulator;
import infra.util.StringUtils;
import infra.util.xml.DomUtils;

/**
 * Internal utility methods used with JDBC configuration.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/7 22:10
 */
abstract class DatabasePopulatorConfigUtils {

  public static void setDatabasePopulator(Element element, BeanDefinitionBuilder builder) {
    List<Element> scripts = DomUtils.getChildElementsByTagName(element, "script");
    if (!scripts.isEmpty()) {
      builder.addPropertyValue("databasePopulator", createDatabasePopulator(element, scripts, "INIT"));
      builder.addPropertyValue("databaseCleaner", createDatabasePopulator(element, scripts, "DESTROY"));
    }
  }

  @Nullable
  private static BeanDefinition createDatabasePopulator(Element element, List<Element> scripts, String execution) {
    BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(CompositeDatabasePopulator.class);

    boolean ignoreFailedDrops = element.getAttribute("ignore-failures").equals("DROPS");
    boolean continueOnError = element.getAttribute("ignore-failures").equals("ALL");

    ManagedList<BeanMetadataElement> delegates = new ManagedList<>();
    for (Element scriptElement : scripts) {
      String executionAttr = scriptElement.getAttribute("execution");
      if (StringUtils.isBlank(executionAttr)) {
        executionAttr = "INIT";
      }
      if (!execution.equals(executionAttr)) {
        continue;
      }
      BeanDefinitionBuilder delegate = BeanDefinitionBuilder.genericBeanDefinition(ResourceDatabasePopulator.class);
      delegate.addPropertyValue("ignoreFailedDrops", ignoreFailedDrops);
      delegate.addPropertyValue("continueOnError", continueOnError);

      // Use a factory bean for the resources so they can be given an order if a pattern is used
      BeanDefinitionBuilder resourcesFactory = BeanDefinitionBuilder.genericBeanDefinition(SortedResourcesFactoryBean.class);
      resourcesFactory.addConstructorArgValue(new TypedStringValue(scriptElement.getAttribute("location")));
      delegate.addPropertyValue("scripts", resourcesFactory.getBeanDefinition());
      if (StringUtils.isNotEmpty(scriptElement.getAttribute("encoding"))) {
        delegate.addPropertyValue("sqlScriptEncoding", new TypedStringValue(scriptElement.getAttribute("encoding")));
      }
      String separator = getSeparator(element, scriptElement);
      if (separator != null) {
        delegate.addPropertyValue("separator", new TypedStringValue(separator));
      }
      delegates.add(delegate.getBeanDefinition());
    }

    if (delegates.isEmpty()) {
      return null;
    }

    builder.addPropertyValue("populators", delegates);

    return builder.getBeanDefinition();
  }

  @Nullable
  private static String getSeparator(Element element, Element scriptElement) {
    String scriptSeparator = scriptElement.getAttribute("separator");
    if (StringUtils.isNotEmpty(scriptSeparator)) {
      return scriptSeparator;
    }
    String elementSeparator = element.getAttribute("separator");
    if (StringUtils.isNotEmpty(elementSeparator)) {
      return elementSeparator;
    }
    return null;
  }

}
