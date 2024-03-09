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

package cn.taketoday.jdbc.datasource.embedded;

/**
 * An {@link EmbeddedDatabaseConfigurer} delegate that can be used to customize
 * the embedded database.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class EmbeddedDatabaseConfigurerDelegate extends AbstractEmbeddedDatabaseConfigurer {

  private final EmbeddedDatabaseConfigurer target;

  public EmbeddedDatabaseConfigurerDelegate(EmbeddedDatabaseConfigurer target) {
    this.target = target;
  }

  @Override
  public void configureConnectionProperties(ConnectionProperties properties, String databaseName) {
    this.target.configureConnectionProperties(properties, databaseName);
  }

}
