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
