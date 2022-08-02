/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.jdbc.type;

import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import cn.taketoday.core.BridgeMethodResolver;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Enumerable;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ReflectionUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/1 22:15
 */
public class EnumerableEnumTypeHandler<V> implements TypeHandler<Enumerable<V>> {

  private final Class<Enumerable<V>> type;

  private final TypeHandler<V> delegate;

  public EnumerableEnumTypeHandler(Class<Enumerable<V>> type, TypeHandlerRegistry registry) {
    Assert.notNull(type, "Type argument is required");
    this.type = type;
    Class<V> valueType = getValueType(type);
    this.delegate = registry.getTypeHandler(valueType);
  }

  @SuppressWarnings("unchecked")
  static <V> Class<V> getValueType(Class<?> type) {
    ResolvableType resolvableType = ResolvableType.fromClass(Enumerable.class, type);
    Class<?> valueType = resolvableType.resolveGeneric();
    if (valueType == null || valueType == Object.class) {
      Method getValue = ReflectionUtils.getMethod(type, "getValue");
      if (getValue != null) {
        Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(getValue);
        valueType = bridgedMethod.getReturnType();
      }
      if (valueType == null || valueType == Object.class) {
        valueType = String.class;// fallback to Enum#name()
      }
    }
    return (Class<V>) valueType;
  }

  @Override
  public void setParameter(PreparedStatement ps, int parameterIndex, Enumerable<V> parameter) throws SQLException {
    delegate.setParameter(ps, parameterIndex, parameter.getValue());
  }

  @Nullable
  @Override
  public Enumerable<V> getResult(ResultSet rs, String columnName) throws SQLException {
    V result = delegate.getResult(rs, columnName);
    return Enumerable.of(type, result);
  }

  @Nullable
  @Override
  public Enumerable<V> getResult(ResultSet rs, int columnIndex) throws SQLException {
    V result = delegate.getResult(rs, columnIndex);
    return Enumerable.of(type, result);
  }

  @Nullable
  @Override
  public Enumerable<V> getResult(CallableStatement cs, int columnIndex) throws SQLException {
    V result = delegate.getResult(cs, columnIndex);
    return Enumerable.of(type, result);
  }

}
