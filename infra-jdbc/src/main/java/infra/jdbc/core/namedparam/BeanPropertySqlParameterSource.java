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

  private String @Nullable [] propertyNames;

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
  public String @Nullable [] getParameterNames() {
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
