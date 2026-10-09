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

package infra.persistence.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import infra.dao.InvalidDataAccessApiUsageException;
import infra.dao.OptimisticLockingFailureException;
import infra.jdbc.RepositoryManager;
import infra.jdbc.datasource.DriverManagerDataSource;
import infra.persistence.EntityMetadata;
import infra.persistence.EntityProperty;
import infra.persistence.PropertyUpdateStrategy;
import infra.persistence.annotation.Table;
import infra.persistence.annotation.Version;
import infra.persistence.auditing.AuditingEntityListener;
import infra.persistence.auditing.LastModifiedBy;
import infra.persistence.event.BatchExecution;
import infra.persistence.event.BatchExecutionListener;
import infra.persistence.event.BatchOperation;
import infra.persistence.event.EntityFailureContext;
import infra.persistence.event.EntityOperationPhase;
import infra.persistence.event.UpdateEventListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

class BatchUpdateTests {

  RepositoryManager repository;
  RecordingManager manager;

  @BeforeEach
  void setup() {
    repository = new RepositoryManager(new DriverManagerDataSource(
            "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
    repository.createNamedQuery("create table batch_item (id bigint primary key, name varchar(64), age integer, "
            + "modified_by varchar(64), version integer)").executeUpdate();
    repository.createNamedQuery("insert into batch_item (id,name,age,version) values (1,'old',10,0),(2,'old',20,0)").executeUpdate();
    manager = new RecordingManager(repository);
  }

  @Test
  void batchListenerDistinguishesSaveAndUpdateOperations() {
    var operations = new ArrayList<BatchOperation>();
    manager.getEntityEventRegistry().addListener(new BatchExecutionListener() {
      @Override
      public void preProcessing(BatchExecution execution, boolean implicitExecution) {
        operations.add(execution.getOperation());
        assertThat(execution.isInsert()).isEqualTo(execution.getOperation() == BatchOperation.INSERT);
        assertThat(execution.isUpdate()).isEqualTo(execution.getOperation() == BatchOperation.UPDATE);
        assertThat(execution.isDelete()).isFalse();
      }

      @Override
      public void postProcessing(BatchExecution execution, boolean implicitExecution, Throwable exception) {
        assertThat(exception).isNull();
        assertThat(execution.getOperation()).isEqualTo(operations.get(operations.size() - 1));
        assertThat(execution.isInsert()).isEqualTo(execution.getOperation() == BatchOperation.INSERT);
        assertThat(execution.isUpdate()).isEqualTo(execution.getOperation() == BatchOperation.UPDATE);
        assertThat(execution.isDelete()).isFalse();
      }
    });

    assertThat(manager.persist(List.of(item(3L, "saved")))).isEqualTo(1);
    assertThat(manager.updateById(List.of(item(3L, "updated")))).isEqualTo(1);
    assertThat(operations).containsExactly(BatchOperation.INSERT, BatchOperation.UPDATE);
    assertThat(manager.findById(Item.class, 3L).name).isEqualTo("updated");
  }

  @Test
  void batchesCompatibleUpdatesAndKeepsCountsInInputOrder() throws SQLException {
    manager.setMaxBatchRecords(2);
    assertThat(manager.updateById(List.of(item(1L, "first"), item(99L, "missing"), item(2L, "second"))))
            .isEqualTo(2);
    assertThat(manager.statements).hasSize(1);
    verify(manager.statements.get(0), times(2)).executeBatch();
    assertThat(manager.findById(Item.class, 1L).name).isEqualTo("first");
    assertThat(manager.findById(Item.class, 2L).name).isEqualTo("second");
  }

  @Test
  void differentColumnsAndRepeatedIdsExecuteInInputOrder() {
    Item age = new Item();
    age.id = 1L;
    age.age = 30;
    assertThat(manager.updateById(List.of(item(1L, "first"), age, item(1L, "last"))))
            .isEqualTo(3);
    assertThat(manager.statements).hasSize(2);
    Item loaded = manager.findById(Item.class, 1L);
    assertThat(loaded.name).isEqualTo("last");
    assertThat(loaded.age).isEqualTo(30);
  }

  @Test
  void auditingAndCustomizedStrategiesRunForEachEntity() {
    manager.getEntityEventRegistry().addListener(new AuditingEntityListener(Clock.systemUTC(), () -> "editor"));
    List<String> events = new ArrayList<>();
    manager.getEntityEventRegistry().addListener(new UpdateEventListener<Item>() {
      @Override
      public PropertyUpdateStrategy onPreUpdate(Item entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
        events.add("pre:" + entity.id);
        return (target, property) -> property.getName().equals("name") || strategy.shouldUpdate(target, property);
      }

      @Override
      public void onPostUpdate(Item entity, EntityMetadata metadata, List<EntityProperty> properties, int count) {
        assertThat(properties).extracting(EntityProperty::getName).contains("name", "modifiedBy").doesNotContain("id");
        assertThatThrownBy(properties::clear).isInstanceOf(UnsupportedOperationException.class);
        events.add("post:" + entity.id + ":" + count);
      }
    });
    assertThat(manager.updateById(List.of(item(1L, "one"), item(2L, "two")), (target, property) -> false))
            .isEqualTo(2);
    assertThat(events).containsExactly("pre:1", "pre:2", "post:1:1", "post:2:1");
    assertThat(manager.findById(Item.class, 1L).modifiedBy).isEqualTo("editor");
    assertThat(manager.findById(Item.class, 2L).name).isEqualTo("two");
  }

  @Test
  void versionConflictRollsBackEarlierBatch() throws SQLException {
    manager.setMaxBatchRecords(1);
    VersionedItem stale = new VersionedItem();
    stale.id = 2L;
    stale.name = "conflict";
    stale.version = 5;
    assertThatThrownBy(() -> manager.updateById(List.of(item(1L, "changed"), stale)))
            .isInstanceOf(OptimisticLockingFailureException.class);
    verify(manager.statements.get(0)).executeBatch();
    verify(manager.statements.get(1)).executeBatch();
    assertThat(manager.findById(Item.class, 1L).name).isEqualTo("old");
    // the in-memory version is restored when the batch fails, so a retry can succeed
    assertThat(stale.version).isEqualTo(5);
  }

  @Test
  void versionedEntitiesUseExactCounts() throws SQLException {
    VersionedItem entity = new VersionedItem();
    entity.id = 1L;
    entity.name = "new";
    entity.version = 0;
    assertThat(manager.updateById(List.of(entity))).isEqualTo(1);
    verify(manager.statements.get(0)).executeBatch();
    assertThat(manager.findById(VersionedItem.class, 1L).version).isEqualTo(1);
  }

  @Test
  void versionConflictDoesNotNotifySuccessfulUpdatesInTheSameBatch() {
    var updated = new ArrayList<Long>();
    var failed = new ArrayList<Long>();
    manager.getEntityEventRegistry().addListener(new UpdateEventListener<VersionedItem>() {
      @Override
      public void onPostUpdate(VersionedItem entity, EntityMetadata metadata, List<EntityProperty> properties, int count) {
        updated.add(entity.id);
      }

      @Override
      public void onUpdateFailed(VersionedItem entity, EntityFailureContext context) {
        assertThat(context.getPhase()).isEqualTo(EntityOperationPhase.EXECUTION);
        assertThat(context.getId()).isEqualTo(entity.id);
        failed.add(entity.id);
      }
    });
    VersionedItem first = new VersionedItem();
    first.id = 1L;
    first.name = "changed";
    first.version = 0;
    VersionedItem stale = new VersionedItem();
    stale.id = 2L;
    stale.name = "conflict";
    stale.version = 5;

    assertThatThrownBy(() -> manager.updateById(List.of(first, stale)))
            .isInstanceOf(OptimisticLockingFailureException.class);
    assertThat(updated).isEmpty();
    assertThat(failed).isEmpty();
    assertThat(manager.findById(VersionedItem.class, 1L).version).isZero();
    assertThat(manager.findById(Item.class, 1L).name).isEqualTo("old");
  }

  @Test
  void preservesUnknownCountsAndRejectsFailedCounts() throws SQLException {
    manager.batchResult = new int[] { Statement.SUCCESS_NO_INFO };
    assertThat(manager.updateById(List.of(item(1L, "new")))).isEqualTo(Statement.SUCCESS_NO_INFO);
    manager.batchResult = new int[] { Statement.EXECUTE_FAILED };
    assertThatThrownBy(() -> manager.updateById(List.of(item(1L, "failed"))))
            .isInstanceOf(infra.dao.DataAccessException.class);
  }

  @Test
  void missingIdRollsBackFlushedUpdatesAndEmptyInputDoesNothing() {
    assertThat(manager.updateById(List.of())).isZero();
    assertThat(manager.statements).isEmpty();
    manager.setMaxBatchRecords(1);
    assertThatThrownBy(() -> manager.updateById(List.of(item(1L, "new"), new Item())))
            .isInstanceOf(InvalidDataAccessApiUsageException.class);
    assertThat(manager.findById(Item.class, 1L).name).isEqualTo("old");
  }

  @Test
  void sqlFailureNotifiesPendingEntitiesAndRollsBack() {
    List<Long> failed = new ArrayList<>();
    manager.getEntityEventRegistry().addListener(new UpdateEventListener<Item>() {
      @Override
      public void onUpdateFailed(Item entity, EntityFailureContext context) {
        assertThat(context.getPhase()).isEqualTo(EntityOperationPhase.EXECUTION);
        failed.add(entity.id);
      }
    });
    assertThatThrownBy(() -> manager.updateById(List.of(item(1L, "new"), item(2L, "x".repeat(100)))))
            .isInstanceOf(infra.dao.DataAccessException.class);
    assertThat(failed).isEmpty();
    assertThat(manager.findById(Item.class, 1L).name).isEqualTo("old");
  }

  @Test
  void laterCallbacksCannotChangeAlreadyBoundParameters() {
    Item first = item(1L, "bound");
    manager.getEntityEventRegistry().addListener(new UpdateEventListener<Item>() {
      @Override
      public PropertyUpdateStrategy onPreUpdate(Item entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
        if (entity.id == 2L) {
          first.name = "mutated";
        }
        return strategy;
      }
    });
    manager.updateById(List.of(first, item(2L, "second")));
    assertThat(manager.findById(Item.class, 1L).name).isEqualTo("bound");
  }

  @Test
  void missingIdDoesNotNotifyEntityFailureListeners() {
    var failed = new ArrayList<Item>();
    var contexts = new ArrayList<EntityFailureContext>();
    manager.getEntityEventRegistry().addListener(new UpdateEventListener<Item>() {
      @Override
      public void onUpdateFailed(Item entity, EntityFailureContext context) {
        failed.add(entity);
        contexts.add(context);
      }
    });
    Item pending = item(1L, "pending");
    Item missingId = new Item();
    assertThatThrownBy(() -> manager.updateById(List.of(pending, missingId)))
            .isInstanceOf(InvalidDataAccessApiUsageException.class);
    assertThat(failed).isEmpty();
    assertThat(contexts).isEmpty();
    assertThat(manager.findById(Item.class, 1L).name).isEqualTo("old");
  }

  @Test
  void executionFailureDoesNotNotifyEntityFailureListenersAcrossPropertyGroups() {
    manager.batchResult = new int[] { Statement.EXECUTE_FAILED };
    var phases = new ArrayList<EntityOperationPhase>();
    var failed = new ArrayList<Item>();
    manager.getEntityEventRegistry().addListener(new UpdateEventListener<Item>() {
      @Override
      public void onUpdateFailed(Item entity, EntityFailureContext context) {
        failed.add(entity);
        phases.add(context.getPhase());
      }
    });
    Item first = item(1L, "first");
    Item second = item(2L, "second");
    second.age = 25;
    assertThatThrownBy(() -> manager.updateById(List.of(first, second)))
            .isInstanceOf(infra.dao.DataAccessException.class);
    assertThat(failed).isEmpty();
    assertThat(phases).isEmpty();
  }

  @Test
  void batchCollectionFailureDoesNotNotifyEntityFailureListeners() {
    IllegalStateException failure = new IllegalStateException("addBatch failed");
    var collectingManager = new DefaultEntityManager(repository) {
      @Override
      protected PreparedStatement prepareStatement(Connection connection, String sql, boolean generatedKeys) throws SQLException {
        PreparedStatement statement = spy(connection.prepareStatement(sql));
        org.mockito.Mockito.doNothing().doThrow(failure).when(statement).addBatch();
        return statement;
      }
    };
    var failed = new ArrayList<Item>();
    var phases = new ArrayList<EntityOperationPhase>();
    collectingManager.getEntityEventRegistry().addListener(new UpdateEventListener<Item>() {
      @Override
      public void onUpdateFailed(Item entity, EntityFailureContext context) {
        failed.add(entity);
        phases.add(context.getPhase());
      }
    });
    Item pending = item(1L, "first");
    Item rejected = item(2L, "second");
    assertThatThrownBy(() -> collectingManager.updateById(List.of(pending, rejected))).isSameAs(failure);
    assertThat(failed).isEmpty();
    assertThat(phases).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void statementCloseFailuresAreReportedWithoutPreventingCommit(boolean insert) throws SQLException {
    var failures = new ArrayList<infra.jdbc.ResourceCloseFailure>();
    repository.setResourceCloseFailureListener(failures::add);
    var closingManager = new CloseFailingManager(repository);
    Item first = item(insert ? 3L : 1L, "first");
    Item second = item(insert ? 4L : 2L, "second");
    second.age = 30;

    assertThat(executeBatch(closingManager, insert, List.of(first, second))).isEqualTo(2);
    assertThat(failures).hasSize(2).allSatisfy(failure -> {
      assertThat(failure.exception()).isSameAs(closingManager.closeFailure);
      assertThat(failure.sql()).isNotNull();
      assertThat(failure.operationFailure()).isNull();
    });
    assertThat(closingManager.statements).hasSize(2);
    for (PreparedStatement statement : closingManager.statements) {
      verify(statement).close();
    }
    assertThat(repository.createNamedQuery("select count(*) from batch_item where name <> 'old'")
            .fetchFirst(Integer.class)).isEqualTo(2);
  }

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void suppressedStatementCloseFailureAllowsCommit(boolean insert) throws SQLException {
    repository.setResourceCloseFailureListener(failure -> { throw new IllegalStateException("observer failed"); });
    var closingManager = new CloseFailingManager(repository);
    Item first = item(insert ? 3L : 1L, "first");
    Item second = item(insert ? 4L : 2L, "second");
    second.age = 30;

    assertThat(executeBatch(closingManager, insert, List.of(first, second))).isEqualTo(2);
    assertThat(closingManager.statements).hasSize(2);
    for (PreparedStatement statement : closingManager.statements) {
      verify(statement).close();
    }
    assertThat(repository.createNamedQuery("select count(*) from batch_item where name <> 'old'")
            .fetchFirst(Integer.class)).isEqualTo(2);
  }

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void statementCloseFailuresDoNotReplaceExecutionFailure(boolean insert) throws SQLException {
    for (boolean observerThrows : new boolean[] { true, false }) {
      var closeFailures = new ArrayList<infra.jdbc.ResourceCloseFailure>();
      repository.setResourceCloseFailureListener(context -> {
        closeFailures.add(context);
        if (observerThrows) {
          throw new IllegalStateException("observer failed");
        }
      });
      var closingManager = new CloseFailingManager(repository);
      var failure = new InvalidDataAccessApiUsageException("execution failed");
      closingManager.getEntityEventRegistry().addListener(new BatchExecutionListener() {
        @Override
        public void preProcessing(BatchExecution execution, boolean implicitExecution) {
          throw failure;
        }

        @Override
        public void postProcessing(BatchExecution execution, boolean implicitExecution, Throwable exception) {
          assertThat(exception).isSameAs(failure);
        }
      });
      Item first = item(insert ? 3L : 1L, "first");
      Item second = item(insert ? 4L : 2L, "second");
      second.age = 30;

      assertThatThrownBy(() -> executeBatch(closingManager, insert, List.of(first, second)))
              .isSameAs(failure)
              .satisfies(ex -> assertThat(ex.getSuppressed())
                      .containsExactly(closingManager.closeFailure, closingManager.closeFailure));
      assertThat(closingManager.statements).hasSize(2);
      assertThat(closeFailures).hasSize(2).allSatisfy(context ->
              assertThat(context.operationFailure()).isSameAs(failure));
      for (PreparedStatement statement : closingManager.statements) {
        verify(statement).close();
      }
    }
  }

  private static int executeBatch(DefaultEntityManager manager, boolean insert, List<Item> entities) {
    return insert ? manager.persist(entities) : manager.updateById(entities);
  }

  static class CloseFailingManager extends DefaultEntityManager {
    final List<PreparedStatement> statements = new ArrayList<>();
    final SQLException closeFailure = new SQLException("close failed");

    CloseFailingManager(RepositoryManager repository) {
      super(repository);
    }

    @Override
    protected PreparedStatement prepareStatement(Connection connection, String sql, boolean generatedKeys) throws SQLException {
      PreparedStatement statement = spy(connection.prepareStatement(sql));
      willAnswer(invocation -> {
        invocation.callRealMethod();
        throw closeFailure;
      }).given(statement).close();
      statements.add(statement);
      return statement;
    }
  }

  private static Item item(Long id, String name) {
    Item entity = new Item();
    entity.id = id;
    entity.name = name;
    return entity;
  }

  @Table("batch_item")
  static class Item {
    public Long id;
    public String name;
    public Integer age;
    @LastModifiedBy
    public String modifiedBy;
  }

  @Table("batch_item")
  static class VersionedItem {
    public Long id;
    public String name;
    @Version
    public Integer version;
  }

  static class RecordingManager extends DefaultEntityManager {
    final List<PreparedStatement> statements = new ArrayList<>();
    int[] batchResult;

    RecordingManager(RepositoryManager repository) {
      super(repository);
    }

    @Override
    protected PreparedStatement prepareStatement(Connection connection, String sql, boolean generatedKeys) throws SQLException {
      PreparedStatement statement = spy(connection.prepareStatement(sql));
      if (batchResult != null && sql.startsWith("UPDATE")) {
        given(statement.executeBatch()).willReturn(batchResult);
      }
      statements.add(statement);
      return statement;
    }
  }
}
