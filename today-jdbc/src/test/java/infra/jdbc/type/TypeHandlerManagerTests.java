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

import org.junit.jupiter.api.Test;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import infra.beans.BeanMetadata;
import infra.beans.BeanProperty;
import infra.lang.Nullable;
import infra.persistence.model.UserModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/1/23 14:45
 */
class TypeHandlerManagerTests {

  final TypeHandlerManager manager = new TypeHandlerManager();

  @Test
  void addHandlerResolver() {
    assertThatThrownBy(() -> manager.addHandlerResolver(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("TypeHandlerResolver is required");

    manager.addHandlerResolver(new AnyTypeHandlerResolver());

    BeanMetadata metadata = BeanMetadata.forClass(UserModel.class);
    BeanProperty name = metadata.obtainBeanProperty("name");
    TypeHandler<String> typeHandler = manager.getTypeHandler(name);
    assertThat(typeHandler).isInstanceOf(AnyTypeHandler.class)
            .extracting("type").isEqualTo(String.class);
  }

  @Test
  void setHandlerResolver() {
    manager.setHandlerResolver(new AnyTypeHandlerResolver());

    BeanMetadata metadata = BeanMetadata.forClass(UserModel.class);
    BeanProperty name = metadata.obtainBeanProperty("name");
    TypeHandler<String> typeHandler = manager.getTypeHandler(name);
    assertThat(typeHandler).isInstanceOf(AnyTypeHandler.class)
            .extracting("type").isEqualTo(String.class);

    manager.clear();

    typeHandler = manager.getTypeHandler(name);
    assertThat(typeHandler).isInstanceOf(UnknownTypeHandler.class);
  }

  @Test
  void register() {
    manager.register(new SmartTypeHandler0());

    BeanMetadata metadata = BeanMetadata.forClass(UserModel.class);
    BeanProperty name = metadata.obtainBeanProperty("name");
    TypeHandler<String> typeHandler = manager.getTypeHandler(name);
    assertThat(typeHandler).isInstanceOf(SmartTypeHandler0.class);

  }

  static class AnyTypeHandlerResolver implements TypeHandlerResolver {

    @Nullable
    @Override
    public TypeHandler<?> resolve(BeanProperty property) {
      return new AnyTypeHandler<>(property.getType());
    }
  }

  static class SmartTypeHandler0 implements SmartTypeHandler<Integer> {

    @Override
    public boolean supportsProperty(BeanProperty property) {
      return true;
    }

    @Override
    public void setParameter(PreparedStatement ps, int parameterIndex, @Nullable Integer arg) throws SQLException {

    }

    @Nullable
    @Override
    public Integer getResult(ResultSet rs, String columnName) throws SQLException {
      return 0;
    }

    @Nullable
    @Override
    public Integer getResult(ResultSet rs, int columnIndex) throws SQLException {
      return 0;
    }

    @Nullable
    @Override
    public Integer getResult(CallableStatement cs, int columnIndex) throws SQLException {
      return 0;
    }
  }
}