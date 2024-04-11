/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.persistence;

import org.junit.jupiter.api.Test;

import java.util.Objects;

import cn.taketoday.persistence.model.UserModel;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/1/10 14:36
 */
class PropertyUpdateStrategyTests {

  final EntityMetadataFactory metadataFactory = new DefaultEntityMetadataFactory();
  final EntityMetadata entityMetadata = metadataFactory.getEntityMetadata(UserModel.class);

  @Test
  void updateNoneNull() {
    PropertyUpdateStrategy propertyUpdateStrategy = PropertyUpdateStrategy.noneNull();
    UserModel userModel = new UserModel();

    EntityMetadata entityMetadata = metadataFactory.getEntityMetadata(UserModel.class);
    assertThat(propertyUpdateStrategy.shouldUpdate(userModel, entityMetadata.idProperty))
            .isFalse();

    userModel.setId(1);
    assertThat(propertyUpdateStrategy.shouldUpdate(userModel, entityMetadata.idProperty))
            .isTrue();
  }

  @Test
  void always() {
    PropertyUpdateStrategy strategy = PropertyUpdateStrategy.always();
    UserModel userModel = new UserModel();

    assertThat(strategy.shouldUpdate(userModel, entityMetadata.idProperty()))
            .isTrue();

    userModel.setId(1);
    assertThat(strategy.shouldUpdate(userModel, entityMetadata.idProperty()))
            .isTrue();
  }

  @Test
  void and() {
    PropertyUpdateStrategy strategy = PropertyUpdateStrategy.noneNull().and((entity, property) -> !property.isIdProperty);

    UserModel userModel = new UserModel();
    assertThat(strategy.shouldUpdate(userModel, entityMetadata.idProperty()))
            .isFalse();

    userModel.setId(1);
    assertThat(strategy.shouldUpdate(userModel, entityMetadata.idProperty()))
            .isFalse();

    userModel.setName("name");
    assertThat(strategy.shouldUpdate(userModel, Objects.requireNonNull(entityMetadata.findProperty("name"))))
            .isTrue();
  }

  @Test
  void or() {
    PropertyUpdateStrategy strategy = PropertyUpdateStrategy.noneNull().or((entity, property) -> !property.isIdProperty);

    UserModel userModel = new UserModel();
    assertThat(strategy.shouldUpdate(userModel, entityMetadata.idProperty()))
            .isFalse();

    userModel.setId(1);
    assertThat(strategy.shouldUpdate(userModel, entityMetadata.idProperty()))
            .isTrue();

    userModel.setName("name");
    assertThat(strategy.shouldUpdate(userModel, Objects.requireNonNull(entityMetadata.findProperty("name"))))
            .isTrue();
  }

}