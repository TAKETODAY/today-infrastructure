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

package infra.jdbc.type;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import infra.core.BridgeMethodResolver;
import infra.core.ResolvableType;
import infra.lang.Assert;
import infra.lang.Enumerable;
import infra.util.ReflectionUtils;

/**
 * TypeHandler for Enumerable
 *
 * @param <V> value type
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/1 22:15
 */
public class EnumerableEnumTypeHandler<V> implements TypeHandler<Enumerable<V>> {

  private final Class<? extends Enumerable<V>> type;

  private final TypeHandler<V> delegate;

  public EnumerableEnumTypeHandler(Class<? extends Enumerable<V>> type, TypeHandlerManager registry) {
    Assert.notNull(type, "Type argument is required");
    this.type = type;
    Class<V> valueType = getValueType(type);
    this.delegate = registry.getTypeHandler(valueType);
  }

  @SuppressWarnings("unchecked")
  static <V> Class<V> getValueType(Class<?> type) {
    ResolvableType resolvableType = ResolvableType.forClass(Enumerable.class, type);
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
  public void setParameter(PreparedStatement ps,
          int parameterIndex, @Nullable Enumerable<V> arg) throws SQLException {
    if (arg == null) {
      delegate.setParameter(ps, parameterIndex, null);
    }
    else {
      delegate.setParameter(ps, parameterIndex, arg.getValue());
    }
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
