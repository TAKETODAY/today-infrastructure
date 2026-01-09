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

import org.w3c.dom.Element;

import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.support.AbstractBeanDefinition;
import infra.beans.factory.support.BeanDefinitionBuilder;
import infra.beans.factory.xml.AbstractBeanDefinitionParser;
import infra.beans.factory.xml.BeanDefinitionParser;
import infra.beans.factory.xml.ParserContext;
import infra.jdbc.datasource.init.DataSourceInitializer;
import infra.jdbc.datasource.init.ResourceDatabasePopulator;

/**
 * {@link BeanDefinitionParser} that parses an {@code initialize-database}
 * element and creates a {@link BeanDefinition} of type {@link DataSourceInitializer}. Picks up nested
 * {@code script} elements and configures a {@link ResourceDatabasePopulator} for them.
 *
 * @author Dave Syer
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/7 22:10
 */
class InitializeDatabaseBeanDefinitionParser extends AbstractBeanDefinitionParser {

  @Override
  protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
    BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(DataSourceInitializer.class);
    builder.addPropertyReference("dataSource", element.getAttribute("data-source"));
    builder.addPropertyValue("enabled", element.getAttribute("enabled"));
    DatabasePopulatorConfigUtils.setDatabasePopulator(element, builder);
    builder.getRawBeanDefinition().setSource(parserContext.extractSource(element));
    return builder.getBeanDefinition();
  }

  @Override
  protected boolean shouldGenerateId() {
    return true;
  }

}
