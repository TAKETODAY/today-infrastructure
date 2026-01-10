/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.persistence;

import org.junit.jupiter.api.Test;

import java.util.Objects;

import infra.persistence.model.UserModel;

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

  @Test
  void isId_whenPropertyIsId_shouldReturnTrue() {
    PropertyUpdateStrategy strategy = PropertyUpdateStrategy.isId();
    UserModel userModel = new UserModel();

    assertThat(strategy.shouldUpdate(userModel, entityMetadata.idProperty()))
            .isTrue();
  }

  @Test
  void isId_whenPropertyIsNotId_shouldReturnFalse() {
    PropertyUpdateStrategy strategy = PropertyUpdateStrategy.isId();
    UserModel userModel = new UserModel();

    assertThat(strategy.shouldUpdate(userModel, Objects.requireNonNull(entityMetadata.findProperty("name"))))
            .isFalse();
  }

  @Test
  void never_shouldReturnFalseForAnyEntityAndProperty() {
    PropertyUpdateStrategy strategy = PropertyUpdateStrategy.never();
    UserModel userModel = new UserModel();

    assertThat(strategy.shouldUpdate(userModel, entityMetadata.idProperty()))
            .isFalse();

    userModel.setId(1);
    assertThat(strategy.shouldUpdate(userModel, entityMetadata.idProperty()))
            .isFalse();

    userModel.setName("name");
    assertThat(strategy.shouldUpdate(userModel, Objects.requireNonNull(entityMetadata.findProperty("name"))))
            .isFalse();
  }

  @Test
  void notId_shouldReturnFalseForIdProperty() {
    PropertyUpdateStrategy strategy = PropertyUpdateStrategy.notId();
    UserModel userModel = new UserModel();

    assertThat(strategy.shouldUpdate(userModel, entityMetadata.idProperty()))
            .isFalse();
  }

  @Test
  void notId_shouldReturnTrueForNonIdProperty() {
    PropertyUpdateStrategy strategy = PropertyUpdateStrategy.notId();
    UserModel userModel = new UserModel();

    userModel.setName("name");
    assertThat(strategy.shouldUpdate(userModel, Objects.requireNonNull(entityMetadata.findProperty("name"))))
            .isTrue();
  }

  @Test
  void valueEquals_whenValueMatches_shouldReturnTrue() {
    PropertyUpdateStrategy strategy = PropertyUpdateStrategy.isEqual("expected");
    UserModel userModel = new UserModel();

    userModel.setName("expected");
    assertThat(strategy.shouldUpdate(userModel, Objects.requireNonNull(entityMetadata.findProperty("name"))))
            .isTrue();
  }

  @Test
  void valueEquals_whenValueDoesNotMatch_shouldReturnFalse() {
    PropertyUpdateStrategy strategy = PropertyUpdateStrategy.isEqual("expected");
    UserModel userModel = new UserModel();

    userModel.setName("actual");
    assertThat(strategy.shouldUpdate(userModel, Objects.requireNonNull(entityMetadata.findProperty("name"))))
            .isFalse();
  }

  @Test
  void valueNotEquals_whenValueMatches_shouldReturnFalse() {
    PropertyUpdateStrategy strategy = PropertyUpdateStrategy.isNotEqual("expected");
    UserModel userModel = new UserModel();

    userModel.setName("expected");
    assertThat(strategy.shouldUpdate(userModel, Objects.requireNonNull(entityMetadata.findProperty("name"))))
            .isFalse();
  }

  @Test
  void valueNotEquals_whenValueDoesNotMatch_shouldReturnTrue() {
    PropertyUpdateStrategy strategy = PropertyUpdateStrategy.isNotEqual("expected");
    UserModel userModel = new UserModel();

    userModel.setName("actual");
    assertThat(strategy.shouldUpdate(userModel, Objects.requireNonNull(entityMetadata.findProperty("name"))))
            .isTrue();
  }

}