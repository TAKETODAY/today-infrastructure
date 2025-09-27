/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.jdbc.core.namedparam;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;

import infra.beans.BeanProperty;
import infra.beans.BeanWrapper;
import infra.beans.NotReadablePropertyException;
import infra.jdbc.core.StatementCreatorUtils;
import infra.reflect.PropertyAccessor;
import infra.util.StringUtils;

/**
 * {@link SqlParameterSource} implementation that obtains parameter values
 * from bean properties of a given JavaBean object. The names of the bean
 * properties have to match the parameter names. Supports components of
 * record classes as well, with accessor methods matching parameter names.
 *
 * <p>Uses Infra {@link BeanWrapper} for bean property access underneath.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see NamedParameterJdbcTemplate
 * @see SimplePropertySqlParameterSource
 * @since 4.0
 */
public class BeanPropertySqlParameterSource extends AbstractSqlParameterSource {

  private String @Nullable []propertyNames;

  private final BeanWrapper beanWrapper;

  /**
   * Create a new BeanPropertySqlParameterSource for the given bean.
   *
   * @param object the bean instance to wrap
   */
  public BeanPropertySqlParameterSource(Object object) {
    this.beanWrapper = BeanWrapper.forBeanPropertyAccess(object);
  }

  @Override
  public boolean hasValue(String paramName) {
    return beanWrapper.isReadableProperty(paramName);
  }

  @Override
  @Nullable
  public Object getValue(String paramName) throws IllegalArgumentException {
    try {
      return beanWrapper.getPropertyValue(paramName);
    }
    catch (NotReadablePropertyException ex) {
      throw new IllegalArgumentException(ex.getMessage());
    }
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
    Class<?> propType = beanWrapper.getPropertyType(paramName);
    return StatementCreatorUtils.javaTypeToSqlParameterType(propType);
  }

  @Override
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
      for (BeanProperty property : beanWrapper.getBeanProperties()) {
        if (property.isReadable()) {
          names.add(property.getName());
        }
      }
      this.propertyNames = StringUtils.toStringArray(names);
    }
    return this.propertyNames;
  }

}
