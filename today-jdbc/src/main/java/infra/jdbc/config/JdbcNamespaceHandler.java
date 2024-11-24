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

import infra.beans.factory.xml.NamespaceHandler;
import infra.beans.factory.xml.NamespaceHandlerSupport;

/**
 * {@link NamespaceHandler} for JDBC configuration namespace.
 *
 * @author Oliver Gierke
 * @author Dave Syer
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/7 22:09
 */
public class JdbcNamespaceHandler extends NamespaceHandlerSupport {

  @Override
  public void init() {
    registerBeanDefinitionParser("embedded-database", new EmbeddedDatabaseBeanDefinitionParser());
    registerBeanDefinitionParser("initialize-database", new InitializeDatabaseBeanDefinitionParser());
  }
}
