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
class EventListenerGroupTests {

  private final EventListenerGroup<EntityEventListener<?>> group = new EventListenerGroup<>();

  @Test
  void emptyGroupReportsNoListeners() {
    assertThat(group.isEmpty()).isTrue();
    assertThat(group.size()).isZero();
    assertThat(group.asList()).isEmpty();
    assertThat(group).isEmpty();
    assertThat(group.entityListeners(UserModel.class)).isEmpty();
  }

  @Test
  void listenersCanBeAddedIndividuallyAndInBulk() {
    EntityEventListener<UserModel> first = new UserListener();
    EntityEventListener<UserModel> second = new UserListener();

    group.addListener(first);
    group.addListener(second);
    assertThat(group.size()).isEqualTo(2);
    assertThat(group.asList()).containsExactly(first, second);
    assertThat(group).containsExactly(first, second);

    group.addListeners(List.of(first, second));
    assertThat(group.size()).isEqualTo(4);
  }

  @Test
  void addListenersIgnoresNullCollection() {
    group.addListeners(null);
    assertThat(group.isEmpty()).isTrue();
  }

  @Test
  void setListenersReplacesAllListeners() {
    EntityEventListener<UserModel> first = new UserListener();
    EntityEventListener<UserModel> second = new UserListener();
    EntityEventListener<UserModel> replacement = new UserListener();

    group.addListener(first);
    group.setListeners(List.of(replacement));

    assertThat(group.asList()).containsExactly(replacement);
  }

  @Test
  void setListenersWithNullClearsEverything() {
    group.addListener(new UserListener());
    group.setListeners(null);
    assertThat(group.isEmpty()).isTrue();
  }

  @Test
  void listenersCanBeRemovedIndividually() {
    EntityEventListener<UserModel> first = new UserListener();
    EntityEventListener<UserModel> second = new UserListener();

    group.addListeners(List.of(first, second));
    group.removeListener(first);

    assertThat(group.asList()).containsExactly(second);
  }

  @Test
  void removeListenersRemovesAllGivenListeners() {
    EntityEventListener<UserModel> first = new UserListener();
    EntityEventListener<UserModel> second = new UserListener();
    EntityEventListener<UserModel> third = new UserListener();

    group.addListeners(List.of(first, second, third));
    group.removeListeners(List.of(first, third));

    assertThat(group.asList()).containsExactly(second);
  }

  @Test
  void clearRemovesAllListeners() {
    group.addListeners(List.of(new UserListener(), new UserListener()));
    group.clear();

    assertThat(group.isEmpty()).isTrue();
    assertThat(group.size()).isZero();
  }

  @Test
  void matchingListenersFilterByDeclaredGenericType() {
    EntityEventListener<UserModel> user = new UserListener();
    EntityEventListener<Object> all = new EverythingListener();
    group.addListeners(List.of(user, all));

    assertThat(group.entityListeners(UserModel.class)).containsExactly(user, all);
    assertThat(group.entityListeners(Object.class)).containsExactly(all);
  }

  @Test
  void matchingListenersObserveSupertypeEntities() {
    EntityEventListener<Object> all = new EverythingListener();
    group.addListener(all);

    assertThat(group.entityListeners(UserModel.class)).containsExactly(all);
  }

  @Test
  void matchingListenersObserveInterfaceImplementors() {
    EntityEventListener<NamedEntity> named = new NamedListener();
    group.addListener(named);

    assertThat(group.entityListeners(IEntity.class)).containsExactly(named);
    assertThat(group.entityListeners(UserModel.class)).isEmpty();
  }

  @Test
  void matchingListenersOnlyMatchesAssignableEntityClasses() {
    EntityEventListener<UserModel> user = new UserListener();
    group.addListener(user);

    assertThat(group.entityListeners(UserModel.class)).containsExactly(user);
    assertThat(group.entityListeners(String.class)).isEmpty();
  }

  @Test
  void specializedListenersResolveEntityTypeThroughBaseContract() {
    UpdatingEventListener<UserModel> updating = new UpdatingUserListener();
    DeletingEventListener<UserModel> deleting = new DeletingUserListener();

    group.addListeners(List.of(updating, deleting));

    assertThat(group.entityListeners(UserModel.class)).containsExactlyInAnyOrder(updating, deleting);
    assertThat(group.entityListeners(String.class)).isEmpty();
  }

  @Test
  void matchingResultIsSortedByOrder() {
    EntityEventListener<UserModel> first = new OrderListeners.First();
    EntityEventListener<UserModel> second = new OrderListeners.Second();
    EntityEventListener<UserModel> third = new OrderListeners.Third();

    group.addListeners(List.of(second, third, first));

    assertThat(group.entityListeners(UserModel.class)).containsExactly(first, second, third);
  }

  @Test
  void matchingCacheIsInvalidatedWhenListenerIsAdded() {
    EntityEventListener<UserModel> user = new UserListener();
    assertThat(group.entityListeners(UserModel.class)).isEmpty();

    group.addListener(user);
    assertThat(group.entityListeners(UserModel.class)).containsExactly(user);
  }

  @Test
  void matchingCacheIsInvalidatedWhenListenerIsRemoved() {
    EntityEventListener<UserModel> first = new UserListener();
    EntityEventListener<UserModel> second = new UserListener();
    group.addListeners(List.of(first, second));
    assertThat(group.entityListeners(UserModel.class)).hasSize(2);

    group.removeListener(first);
    assertThat(group.entityListeners(UserModel.class)).containsExactly(second);
  }

  @Test
  void matchingCacheIsInvalidatedWhenGroupIsCleared() {
    group.addListener(new UserListener());
    assertThat(group.entityListeners(UserModel.class)).isNotEmpty();

    group.clear();
    assertThat(group.entityListeners(UserModel.class)).isEmpty();
  }

  @Test
  void addListenerRejectsNull() {
    assertThatThrownBy(() -> group.addListener(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Listener is required");
  }

  @Test
  void removeListenerRejectsNull() {
    assertThatThrownBy(() -> group.removeListener(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Listener is required");
  }

  @Test
  void matchingListenersRejectsNullEntityClass() {
    assertThatThrownBy(() -> group.entityListeners(null))
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

  static class UpdatingUserListener implements UpdatingEventListener<UserModel> {

  }

  static class DeletingUserListener implements DeletingEventListener<UserModel> {

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