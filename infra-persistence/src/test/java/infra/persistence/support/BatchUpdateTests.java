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
import infra.persistence.event.UpdateEventListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
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
    VersionedItem stale = new VersionedItem();
    stale.id = 2L;
    stale.name = "conflict";
    stale.version = 5;
    assertThatThrownBy(() -> manager.updateById(List.of(item(1L, "changed"), stale)))
            .isInstanceOf(OptimisticLockingFailureException.class);
    verify(manager.statements.get(0)).executeBatch();
    verify(manager.statements.get(1)).executeBatch();
    assertThat(manager.findById(Item.class, 1L).name).isEqualTo("old");
    assertThat(stale.version).isEqualTo(6);
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
      public void onUpdateFailed(VersionedItem entity, EntityMetadata metadata, List<EntityProperty> properties, Throwable exception) {
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
    assertThat(failed).containsExactly(1L, 2L);
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
      public void onUpdateFailed(Item entity, EntityMetadata metadata, List<EntityProperty> properties, Throwable exception) {
        failed.add(entity.id);
      }
    });
    assertThatThrownBy(() -> manager.updateById(List.of(item(1L, "new"), item(2L, "x".repeat(100)))))
            .isInstanceOf(infra.dao.DataAccessException.class);
    assertThat(failed).containsExactly(1L, 2L);
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
