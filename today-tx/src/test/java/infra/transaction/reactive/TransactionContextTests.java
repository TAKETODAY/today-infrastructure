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

package infra.transaction.reactive;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/17 22:55
 */
class TransactionContextTests {

  @Test
  void shouldCreateTransactionContextWithDefaultValues() {
    TransactionContext context = new TransactionContext();

    assertThat(context.getParent()).isNull();
    assertThat(context.getResources()).isEmpty();
    assertThat(context.getSynchronizations()).isNull();
    assertThat(context.getCurrentTransactionName()).isNull();
    assertThat(context.isCurrentTransactionReadOnly()).isFalse();
    assertThat(context.getCurrentTransactionIsolationLevel()).isNull();
    assertThat(context.isActualTransactionActive()).isFalse();
  }

  @Test
  void shouldCreateTransactionContextWithParent() {
    TransactionContext parent = new TransactionContext();
    TransactionContext child = new TransactionContext(parent);

    assertThat(child.getParent()).isSameAs(parent);
    assertThat(child.getResources()).isEmpty();
    assertThat(child.getSynchronizations()).isNull();
  }

  @Test
  void shouldGetAndSetResources() {
    TransactionContext context = new TransactionContext();
    Map<Object, Object> resources = context.getResources();

    assertThat(resources).isNotNull();
    assertThat(resources).isEmpty();

    String key = "testKey";
    Object value = new Object();
    resources.put(key, value);

    assertThat(resources).containsEntry(key, value);
  }

  @Test
  void shouldGetAndSetSynchronizations() {
    TransactionContext context = new TransactionContext();
    Set<TransactionSynchronization> synchronizations = new LinkedHashSet<>();

    context.setSynchronizations(synchronizations);
    assertThat(context.getSynchronizations()).isSameAs(synchronizations);

    context.setSynchronizations(null);
    assertThat(context.getSynchronizations()).isNull();
  }

  @Test
  void shouldGetAndSetTransactionName() {
    TransactionContext context = new TransactionContext();
    String transactionName = "testTransaction";

    context.setCurrentTransactionName(transactionName);
    assertThat(context.getCurrentTransactionName()).isEqualTo(transactionName);

    context.setCurrentTransactionName(null);
    assertThat(context.getCurrentTransactionName()).isNull();
  }

  @Test
  void shouldGetAndSetTransactionReadOnlyFlag() {
    TransactionContext context = new TransactionContext();

    context.setCurrentTransactionReadOnly(true);
    assertThat(context.isCurrentTransactionReadOnly()).isTrue();

    context.setCurrentTransactionReadOnly(false);
    assertThat(context.isCurrentTransactionReadOnly()).isFalse();
  }

  @Test
  void shouldGetAndSetTransactionIsolationLevel() {
    TransactionContext context = new TransactionContext();
    Integer isolationLevel = 2; // ISOLATION_READ_COMMITTED

    context.setCurrentTransactionIsolationLevel(isolationLevel);
    assertThat(context.getCurrentTransactionIsolationLevel()).isEqualTo(isolationLevel);

    context.setCurrentTransactionIsolationLevel(null);
    assertThat(context.getCurrentTransactionIsolationLevel()).isNull();
  }

  @Test
  void shouldGetAndSetActualTransactionActiveFlag() {
    TransactionContext context = new TransactionContext();

    context.setActualTransactionActive(true);
    assertThat(context.isActualTransactionActive()).isTrue();

    context.setActualTransactionActive(false);
    assertThat(context.isActualTransactionActive()).isFalse();
  }

  @Test
  void shouldClearTransactionContext() {
    TransactionContext context = new TransactionContext();

    // Set up various transaction states
    context.setSynchronizations(new LinkedHashSet<>());
    context.setCurrentTransactionName("testTransaction");
    context.setCurrentTransactionReadOnly(true);
    context.setCurrentTransactionIsolationLevel(2);
    context.setActualTransactionActive(true);

    // Verify states are set
    assertThat(context.getSynchronizations()).isNotNull();
    assertThat(context.getCurrentTransactionName()).isNotNull();
    assertThat(context.isCurrentTransactionReadOnly()).isTrue();
    assertThat(context.getCurrentTransactionIsolationLevel()).isNotNull();
    assertThat(context.isActualTransactionActive()).isTrue();

    // Clear and verify all states are reset
    context.clear();

    assertThat(context.getSynchronizations()).isNull();
    assertThat(context.getCurrentTransactionName()).isNull();
    assertThat(context.isCurrentTransactionReadOnly()).isFalse();
    assertThat(context.getCurrentTransactionIsolationLevel()).isNull();
    assertThat(context.isActualTransactionActive()).isFalse();
  }

  @Test
  void shouldMaintainSeparateResourcesPerContext() {
    TransactionContext parent = new TransactionContext();
    TransactionContext child = new TransactionContext(parent);

    String parentKey = "parentKey";
    String childKey = "childKey";
    Object parentValue = new Object();
    Object childValue = new Object();

    parent.getResources().put(parentKey, parentValue);
    child.getResources().put(childKey, childValue);

    assertThat(parent.getResources()).containsEntry(parentKey, parentValue);
    assertThat(parent.getResources()).doesNotContainEntry(childKey, childValue);

    assertThat(child.getResources()).containsEntry(childKey, childValue);
    assertThat(child.getResources()).doesNotContainEntry(parentKey, parentValue);
  }

}