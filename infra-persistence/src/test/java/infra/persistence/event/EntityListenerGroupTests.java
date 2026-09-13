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

package infra.persistence.event;

import org.junit.jupiter.api.Test;

import java.util.List;

import infra.core.annotation.Order;
import infra.jdbc.model.UserModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class EntityListenerGroupTests {

  private final EntityListenerGroup<EntityEventListener<?>> group = new EntityListenerGroup<>(EntityEventListener.class);

  @Test
  void listenersForFilterByDeclaredGenericType() {
    EntityEventListener<UserModel> user = new UserListener();
    EntityEventListener<Object> all = new EverythingListener();
    group.addListeners(List.of(user, all));

    assertThat(group.listenersFor(UserModel.class)).containsExactly(user, all);
    assertThat(group.listenersFor(Object.class)).containsExactly(all);
  }

  @Test
  void listenersForObserveSupertypeEntities() {
    EntityEventListener<Object> all = new EverythingListener();
    group.addListener(all);

    assertThat(group.listenersFor(UserModel.class)).containsExactly(all);
  }

  @Test
  void listenersForObserveInterfaceImplementors() {
    EntityEventListener<NamedEntity> named = new NamedListener();
    group.addListener(named);

    assertThat(group.listenersFor(IEntity.class)).containsExactly(named);
    assertThat(group.listenersFor(UserModel.class)).isEmpty();
  }

  @Test
  void listenersForOnlyMatchAssignableEntityClasses() {
    EntityEventListener<UserModel> user = new UserListener();
    group.addListener(user);

    assertThat(group.listenersFor(UserModel.class)).containsExactly(user);
    assertThat(group.listenersFor(String.class)).isEmpty();
  }

  @Test
  void specializedListenersResolveEntityTypeThroughBaseContract() {
    UpdateEventListener<UserModel> updating = new UpdatingUserListener();
    DeleteEventListener<UserModel> deleting = new DeletingUserListener();

    group.addListeners(List.of(updating, deleting));

    assertThat(group.listenersFor(UserModel.class)).containsExactlyInAnyOrder(updating, deleting);
    assertThat(group.listenersFor(String.class)).isEmpty();
  }

  @Test
  void listenersForResultIsSortedByOrder() {
    EntityEventListener<UserModel> first = new OrderListeners.First();
    EntityEventListener<UserModel> second = new OrderListeners.Second();
    EntityEventListener<UserModel> third = new OrderListeners.Third();

    group.addListeners(List.of(second, third, first));

    assertThat(group.listenersFor(UserModel.class)).containsExactly(first, second, third);
  }

  @Test
  void matchingCacheIsInvalidatedWhenListenerIsAdded() {
    EntityEventListener<UserModel> user = new UserListener();
    assertThat(group.listenersFor(UserModel.class)).isEmpty();

    group.addListener(user);
    assertThat(group.listenersFor(UserModel.class)).containsExactly(user);
  }

  @Test
  void matchingCacheIsInvalidatedWhenListenerIsRemoved() {
    EntityEventListener<UserModel> first = new UserListener();
    EntityEventListener<UserModel> second = new UserListener();
    group.addListeners(List.of(first, second));
    assertThat(group.listenersFor(UserModel.class)).hasSize(2);

    group.removeListener(first);
    assertThat(group.listenersFor(UserModel.class)).containsExactly(second);
  }

  @Test
  void matchingCacheIsInvalidatedWhenGroupIsCleared() {
    group.addListener(new UserListener());
    assertThat(group.listenersFor(UserModel.class)).isNotEmpty();

    group.clear();
    assertThat(group.listenersFor(UserModel.class)).isEmpty();
  }

  @Test
  void listenersForRejectsNullEntityClass() {
    assertThatThrownBy(() -> group.listenersFor(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Entity class is required");
  }

  interface NamedEntity {

    String getName();
  }

  static class IEntity implements NamedEntity {

    private final String name;

    IEntity(String name) {
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
    }
  }

  static class UserListener implements EntityEventListener<UserModel> {

  }

  static class EverythingListener implements EntityEventListener<Object> {

  }

  static class NamedListener implements EntityEventListener<NamedEntity> {

  }

  static class UpdatingUserListener implements UpdateEventListener<UserModel> {

  }

  static class DeletingUserListener implements DeleteEventListener<UserModel> {

  }

  static class OrderListeners {

    @Order(1)
    static class First implements EntityEventListener<UserModel> {

    }

    @Order(2)
    static class Second implements EntityEventListener<UserModel> {

    }

    @Order(3)
    static class Third implements EntityEventListener<UserModel> {

    }
  }

}