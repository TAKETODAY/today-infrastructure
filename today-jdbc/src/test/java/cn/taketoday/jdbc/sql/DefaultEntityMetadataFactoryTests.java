/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.jdbc.sql;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import cn.taketoday.beans.BeanProperty;
import cn.taketoday.jdbc.sql.model.UserModel;
import lombok.Data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/18 16:55
 */
class DefaultEntityMetadataFactoryTests {
  DefaultEntityMetadataFactory factory = new DefaultEntityMetadataFactory();

  @Test
  void defaultState() {
    EntityMetadata entityMetadata = factory.createEntityMetadata(UserModel.class);
    assertThat(entityMetadata).isNotNull();
    assertThatThrownBy(() ->
            factory.createEntityMetadata(Object.class))
            .isInstanceOf(IllegalEntityException.class)
            .hasMessageStartingWith("Cannot determine ID property for entity: " + Object.class);
  }

  @Test
  void exception() {
    assertIllegalArgumentException(factory::setPropertyFilter);
    assertIllegalArgumentException(factory::setColumnNameDiscover);
    assertIllegalArgumentException(factory::setIdPropertyDiscover);
    assertIllegalArgumentException(factory::setTableNameGenerator);

    factory.setTableNameGenerator(TableNameGenerator.forTableAnnotation());
    assertThatThrownBy(() ->
            factory.createEntityMetadata(Object.class))
            .isInstanceOf(IllegalEntityException.class)
            .hasMessage("Cannot determine table name for entity: " + Object.class);

    factory.setTableNameGenerator(
            TableNameGenerator.forTableAnnotation()
                    .and(TableNameGenerator.defaultStrategy())
    );

    factory.setColumnNameDiscover(ColumnNameDiscover.forColumnAnnotation());

    assertThatThrownBy(() ->
            factory.createEntityMetadata(UserModel.class))
            .isInstanceOf(IllegalEntityException.class)
            .hasMessageStartingWith("Cannot determine column name for property: UserModel#");

    factory.setPropertyFilter(PropertyFilter.acceptAny());
  }

  @Test
  void multipleId() {
    class MultipleId extends UserModel {
      @Id
      private Long id_;
    }
    assertThatThrownBy(() ->
            factory.createEntityMetadata(MultipleId.class))
            .isInstanceOf(IllegalEntityException.class)
            .hasMessage("Only one Id property supported, entity: " + MultipleId.class);
  }

  @Test
  @Disabled
  void overrideId() {
    @Data
    class OverrideId extends UserModel {
      @Id
      public Integer id;
    }

    EntityMetadata entityMetadata = factory.createEntityMetadata(OverrideId.class);
    assertThat(entityMetadata.idProperty.property)
            .isEqualTo(BeanProperty.valueOf(OverrideId.class, "id"));

  }

  @Test
  void idDiscover() {
    class IdDiscover {
      private Long id;

      private Long id_;
    }

    //default

    EntityMetadata entityMetadata = factory.createEntityMetadata(IdDiscover.class);
    assertThat(entityMetadata.idProperty.property)
            .isEqualTo(BeanProperty.valueOf(IdDiscover.class, "id"));

    factory.setIdPropertyDiscover(IdPropertyDiscover.forPropertyName("id_"));

    entityMetadata = factory.createEntityMetadata(IdDiscover.class);
    assertThat(entityMetadata.idProperty.property)
            .isEqualTo(BeanProperty.valueOf(IdDiscover.class, "id_"));
  }

  @Test
  void getEntityHolder() {
    EntityMetadata entityMetadata = factory.getEntityMetadata(UserModel.class);
    assertThat(factory.entityCache.get(UserModel.class)).isEqualTo(entityMetadata);

  }

  static <T> void assertIllegalArgumentException(Consumer<T> throwingCallable) {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> throwingCallable.accept(null))
            .withMessageEndingWith("is required");
  }
}