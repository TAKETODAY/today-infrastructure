/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.jdbc.core.namedparam;

import java.util.ArrayList;

import cn.taketoday.beans.support.BeanProperty;
import cn.taketoday.beans.support.BeanPropertyAccessor;
import cn.taketoday.core.reflect.PropertyAccessor;
import cn.taketoday.jdbc.core.StatementCreatorUtils;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * {@link SqlParameterSource} implementation that obtains parameter values
 * from bean properties of a given JavaBean object. The names of the bean
 * properties have to match the parameter names.
 *
 * <p>Uses a Framework BeanPropertyAccessor for bean property access underneath.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @see NamedParameterJdbcTemplate
 * @since 4.0
 */
public class BeanPropertySqlParameterSource extends AbstractSqlParameterSource {

  @Nullable
  private String[] propertyNames;

  private final BeanPropertyAccessor accessor;

  /**
   * Create a new BeanPropertySqlParameterSource for the given bean.
   *
   * @param object the bean instance to wrap
   */
  public BeanPropertySqlParameterSource(Object object) {
    this.accessor = BeanPropertyAccessor.ofObject(object);
  }

  @Override
  public boolean hasValue(String paramName) {
    BeanProperty beanProperty = accessor.obtainMetadata().getBeanProperty(paramName);
    return beanProperty != null && beanProperty.isReadable();
  }

  @Override
  @Nullable
  public Object getValue(String paramName) throws IllegalArgumentException {
    return accessor.getProperty(paramName);
  }

  /**
   * Derives a default SQL type from the corresponding property type.
   *
   * @see StatementCreatorUtils#javaTypeToSqlParameterType
   */
  @Override
  public int getSqlType(String paramName) {
    int sqlType = super.getSqlType(paramName);
    if (sqlType != TYPE_UNKNOWN) {
      return sqlType;
    }
    Class<?> propType = accessor.obtainMetadata().getPropertyClass(paramName);
    return StatementCreatorUtils.javaTypeToSqlParameterType(propType);
  }

  @Override
  @NonNull
  public String[] getParameterNames() {
    return getReadablePropertyNames();
  }

  /**
   * Provide access to the property names of the wrapped bean.
   * Uses support provided in the {@link PropertyAccessor} interface.
   *
   * @return an array containing all the known property names
   */
  public String[] getReadablePropertyNames() {
    if (this.propertyNames == null) {
      ArrayList<String> names = new ArrayList<>();
      for (BeanProperty property : accessor.obtainMetadata()) {
        if (property.isReadable()) {
          names.add(property.getName());
        }
      }
      this.propertyNames = StringUtils.toStringArray(names);
    }
    return this.propertyNames;
  }

}
