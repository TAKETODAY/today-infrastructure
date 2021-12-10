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

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.beans.BeanWrapper;
import cn.taketoday.beans.NotReadablePropertyException;
import cn.taketoday.beans.PropertyAccessor;
import cn.taketoday.beans.PropertyAccessorFactory;
import cn.taketoday.jdbc.core.StatementCreatorUtils;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * {@link SqlParameterSource} implementation that obtains parameter values
 * from bean properties of a given JavaBean object. The names of the bean
 * properties have to match the parameter names.
 *
 * <p>Uses a Spring BeanWrapper for bean property access underneath.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @see NamedParameterJdbcTemplate
 * @see cn.taketoday.beans.BeanWrapper
 * @since 2.0
 */
public class BeanPropertySqlParameterSource extends AbstractSqlParameterSource {

  private final BeanWrapper beanWrapper;

  @Nullable
  private String[] propertyNames;

  /**
   * Create a new BeanPropertySqlParameterSource for the given bean.
   *
   * @param object the bean instance to wrap
   */
  public BeanPropertySqlParameterSource(Object object) {
    this.beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(object);
  }

  @Override
  public boolean hasValue(String paramName) {
    return this.beanWrapper.isReadableProperty(paramName);
  }

  @Override
  @Nullable
  public Object getValue(String paramName) throws IllegalArgumentException {
    try {
      return this.beanWrapper.getPropertyValue(paramName);
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
    Class<?> propType = this.beanWrapper.getPropertyType(paramName);
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
      List<String> names = new ArrayList<>();
      PropertyDescriptor[] props = this.beanWrapper.getPropertyDescriptors();
      for (PropertyDescriptor pd : props) {
        if (this.beanWrapper.isReadableProperty(pd.getName())) {
          names.add(pd.getName());
        }
      }
      this.propertyNames = StringUtils.toStringArray(names);
    }
    return this.propertyNames;
  }

}
