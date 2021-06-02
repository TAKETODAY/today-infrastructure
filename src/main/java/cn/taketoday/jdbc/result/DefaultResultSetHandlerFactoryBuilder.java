/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.jdbc.result;

import java.util.Map;

import cn.taketoday.jdbc.reflection.JdbcBeanMetadata;
import cn.taketoday.jdbc.type.TypeHandlerRegistry;

public class DefaultResultSetHandlerFactoryBuilder implements ResultSetHandlerFactoryBuilder {
  private boolean caseSensitive;
  private boolean autoDeriveColumnNames;
  private boolean throwOnMappingError;
  private final TypeHandlerRegistry registry;
  private Map<String, String> columnMappings;

  public DefaultResultSetHandlerFactoryBuilder(TypeHandlerRegistry registry) {
    this.registry = registry;
  }

  public boolean isCaseSensitive() {
    return caseSensitive;
  }

  public void setCaseSensitive(boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
  }

  public boolean isAutoDeriveColumnNames() {
    return autoDeriveColumnNames;
  }

  public void setAutoDeriveColumnNames(boolean autoDeriveColumnNames) {
    this.autoDeriveColumnNames = autoDeriveColumnNames;
  }

  @Override
  public boolean isThrowOnMappingError() {
    return throwOnMappingError;
  }

  @Override
  public void throwOnMappingError(boolean throwOnMappingError) {
    this.throwOnMappingError = throwOnMappingError;
  }

  public Map<String, String> getColumnMappings() {
    return columnMappings;
  }

  public void setColumnMappings(Map<String, String> columnMappings) {
    this.columnMappings = columnMappings;
  }


  public <T> ResultSetHandlerFactory<T> newFactory(Class<T> clazz) {
    JdbcBeanMetadata pojoMetadata = new JdbcBeanMetadata(clazz, caseSensitive, autoDeriveColumnNames, columnMappings, throwOnMappingError);
    return new DefaultResultSetHandlerFactory<>(pojoMetadata, registry);
  }

}
