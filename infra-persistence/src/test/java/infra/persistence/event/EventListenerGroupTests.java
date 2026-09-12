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
  }

  @Test
  void baseGroupDoesNotSupportEntityClassLookup() {
    group.addListener(new UserListener());

    assertThatThrownBy(() -> group.listenersFor(UserModel.class))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("Entity-class lookup is not supported");
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
    EntityEventListener<UserModel> replacement = new UserListener();

    group.addListener(new UserListener());
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

  static class UserListener implements EntityEventListener<UserModel> {

  }

}