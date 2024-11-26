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
import infra.jdbc.datasource.embedded.EmbeddedDatabaseFactoryBean;
import infra.jdbc.datasource.init.ResourceDatabasePopulator;
import infra.util.StringUtils;

/**
 * {@link BeanDefinitionParser} that
 * parses an {@code embedded-database} element and creates a {@link BeanDefinition}
 * for an {@link EmbeddedDatabaseFactoryBean}.
 *
 * <p>Picks up nested {@code script} elements and configures a
 * {@link ResourceDatabasePopulator} for each of them.
 *
 * @author Oliver Gierke
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DatabasePopulatorConfigUtils
 * @since 4.0 2022/3/7 22:11
 */
class EmbeddedDatabaseBeanDefinitionParser extends AbstractBeanDefinitionParser {

  /**
   * Constant for the "database-name" attribute.
   */
  static final String DB_NAME_ATTRIBUTE = "database-name";

  /**
   * Constant for the "generate-name" attribute.
   */
  static final String GENERATE_NAME_ATTRIBUTE = "generate-name";

  @Override
  protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
    BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(EmbeddedDatabaseFactoryBean.class);
    setGenerateUniqueDatabaseNameFlag(element, builder);
    setDatabaseName(element, builder);
    setDatabaseType(element, builder);
    DatabasePopulatorConfigUtils.setDatabasePopulator(element, builder);
    builder.getRawBeanDefinition().setSource(parserContext.extractSource(element));
    return builder.getBeanDefinition();
  }

  @Override
  protected boolean shouldGenerateIdAsFallback() {
    return true;
  }

  private void setGenerateUniqueDatabaseNameFlag(Element element, BeanDefinitionBuilder builder) {
    String generateName = element.getAttribute(GENERATE_NAME_ATTRIBUTE);
    if (StringUtils.hasText(generateName)) {
      builder.addPropertyValue("generateUniqueDatabaseName", generateName);
    }
  }

  private void setDatabaseName(Element element, BeanDefinitionBuilder builder) {
    // 1) Check for an explicit database name
    String name = element.getAttribute(DB_NAME_ATTRIBUTE);

    // 2) Fall back to an implicit database name based on the ID
    if (StringUtils.isBlank(name)) {
      name = element.getAttribute(ID_ATTRIBUTE);
    }

    if (StringUtils.hasText(name)) {
      builder.addPropertyValue("databaseName", name);
    }
    // else, let EmbeddedDatabaseFactory use the default "testdb" name
  }

  private void setDatabaseType(Element element, BeanDefinitionBuilder builder) {
    String type = element.getAttribute("type");
    if (StringUtils.hasText(type)) {
      builder.addPropertyValue("databaseType", type);
    }
  }

}
