/*
 * Copyright 2017 - 2023 the original author or authors.
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
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.jdbc.core.StatementCreatorUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ReflectionUtils;

/**
 * {@link SqlParameterSource} implementation that obtains parameter values
 * from bean properties of a given JavaBean object, from component accessors
 * of a record class, or from raw field access.
 *
 * <p>This is a more flexible variant of {@link BeanPropertySqlParameterSource},
 * with the limitation that it is not able to enumerate its
 * {@link #getParameterNames() parameter names}.
 *
 * <p>In terms of its fallback property discovery algorithm, this class is
 * similar to {@link cn.taketoday.validation.SimpleErrors} which is
 * also just used for property retrieval purposes (rather than binding).
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see NamedParameterJdbcTemplate
 * @see BeanPropertySqlParameterSource
 * @since 4.0
 */
public class SimplePropertySqlParameterSource extends AbstractSqlParameterSource {

  private final Object paramObject;

  private final Map<String, Object> descriptorMap = new HashMap<>();

  /**
   * Create a new SqlParameterSource for the given bean, record or field holder.
   *
   * @param paramObject the bean, record or field holder instance to wrap
   */
  public SimplePropertySqlParameterSource(Object paramObject) {
    Assert.notNull(paramObject, "Parameter object must not be null");
    this.paramObject = paramObject;
  }

  @Override
  public boolean hasValue(String paramName) {
    return (getDescriptor(paramName) != null);
  }

  @Override
  @Nullable
  public Object getValue(String paramName) throws IllegalArgumentException {
    Object desc = getDescriptor(paramName);
    if (desc instanceof PropertyDescriptor pd) {
      ReflectionUtils.makeAccessible(pd.getReadMethod());
      return ReflectionUtils.invokeMethod(pd.getReadMethod(), this.paramObject);
    }
    else if (desc instanceof Field field) {
      ReflectionUtils.makeAccessible(field);
      return ReflectionUtils.getField(field, this.paramObject);
    }
    throw new IllegalArgumentException("Cannot retrieve value for parameter '" + paramName +
            "' - neither a getter method nor a raw field found");
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
    Object desc = getDescriptor(paramName);
    if (desc instanceof PropertyDescriptor pd) {
      return StatementCreatorUtils.javaTypeToSqlParameterType(pd.getPropertyType());
    }
    else if (desc instanceof Field field) {
      return StatementCreatorUtils.javaTypeToSqlParameterType(field.getType());
    }
    return TYPE_UNKNOWN;
  }

  @Nullable
  private Object getDescriptor(String paramName) {
    return this.descriptorMap.computeIfAbsent(paramName, name -> {
      Object pd = BeanUtils.getPropertyDescriptor(this.paramObject.getClass(), name);
      if (pd == null) {
        pd = ReflectionUtils.findField(this.paramObject.getClass(), name);
      }
      return pd;
    });
  }

}
