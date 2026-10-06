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

package infra.persistence.auditing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import infra.beans.NotReadablePropertyException;
import infra.core.annotation.AliasFor;
import infra.jdbc.RepositoryManager;
import infra.jdbc.datasource.DriverManagerDataSource;
import infra.persistence.DefaultEntityMetadataFactory;
import infra.persistence.IllegalEntityException;
import infra.persistence.PropertyUpdateStrategy;
import infra.persistence.EntityMetadata;
import infra.persistence.EntityProperty;
import infra.persistence.annotation.Column;
import infra.persistence.annotation.Table;
import infra.persistence.annotation.UpdateBy;
import infra.persistence.event.PersistEventListener;
import infra.persistence.event.BatchExecutionListener;
import infra.persistence.event.BatchExecution;
import infra.persistence.event.UpdateEventListener;
import infra.persistence.support.DefaultEntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class AuditingEntityListenerTests {

  static final Instant NOW = Instant.parse("2026-10-05T12:00:00Z");

  RepositoryManager repository;

  DefaultEntityManager manager;

  AtomicReference<String> auditor = new AtomicReference<>("creator");

  AuditingEntityListener listener;

  @BeforeEach
  void setup() {
    repository = new RepositoryManager(new DriverManagerDataSource(
            "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
    repository.createNamedQuery("create table audited_entity (id bigint primary key, name varchar(64), "
            + "created_at timestamp with time zone, created_by varchar(64), "
            + "modified_at timestamp with time zone, modified_by varchar(64))").executeUpdate();
    manager = new DefaultEntityManager(repository);
    listener = new AuditingEntityListener(Clock.fixed(NOW, ZoneId.of("Asia/Shanghai")), auditor::get);
    manager.getEntityEventRegistry().addListener(listener);
  }

  @Test
  void insertAndPartialUpdatePersistAuditFieldsDespiteRestrictiveStrategy() {
    AuditedEntity entity = entity(1L);
    manager.persist(entity, (object, property) -> property.isIdProperty());
    assertThat(manager.findById(AuditedEntity.class, 1L).createdBy).isEqualTo("creator");
    assertThat(entity.createdAt).isEqualTo(NOW);
    assertThat(entity.modifiedAt).isEqualTo(NOW);

    auditor.set("editor");
    entity.createdAt = Instant.EPOCH;
    entity.createdBy = "forged";
    entity.name = "updated";
    manager.updateById(entity, (object, property) -> property.getName().equals("name"));

    AuditedEntity loaded = manager.findById(AuditedEntity.class, 1L);
    assertThat(loaded.name).isEqualTo("updated");
    assertThat(loaded.createdAt).isEqualTo(NOW);
    assertThat(loaded.createdBy).isEqualTo("creator");
    assertThat(loaded.modifiedBy).isEqualTo("editor");
  }

  @Test
  void batchInsertAndUpdateByConditionUseAuditRules() {
    manager.setMaxBatchRecords(1);
    manager.persist(List.of(entity(1L), entity(2L)), (object, property) -> property.isIdProperty());
    assertThat(repository.createNamedQuery("select created_by from audited_entity").fetch(String.class))
            .containsExactly("creator", "creator");

    auditor.set("editor");
    ConditionalEntity patch = new ConditionalEntity();
    patch.key = 1L;
    patch.name = "changed";
    patch.createdBy = "forged";
    manager.update(patch, (object, property) -> property.getName().equals("name"));
    assertThat(repository.createNamedQuery("select modified_by from audited_entity").fetch(String.class))
            .containsExactlyInAnyOrder("editor", "creator");
    assertThat(repository.createNamedQuery("select created_by from audited_entity").fetch(String.class))
            .containsExactly("creator", "creator");
  }

  @Test
  void freshCustomizedStrategiesDoNotSplitCompatibleBatches() {
    List<Integer> batchSizes = new ArrayList<>();
    List<Long> persistedIds = new ArrayList<>();
    manager.setMaxBatchRecords(2);
    PropertyUpdateStrategy original = (object, property) -> property.isIdProperty();
    manager.getEntityEventRegistry().addListener(new BatchExecutionListener() {
      @Override
      public void preProcessing(BatchExecution execution, boolean implicitExecution) {
        assertThat(execution.properties).extracting(property -> property.getName())
                .contains("id", "createdAt", "createdBy", "modifiedAt", "modifiedBy");
        if (!execution.entities.isEmpty()) {
          batchSizes.add(execution.entities.size());
        }
      }

      @Override
      public void postProcessing(BatchExecution execution, boolean implicitExecution, @Nullable Throwable exception) {
      }
    });
    manager.getEntityEventRegistry().addListener(new PersistEventListener<AuditedEntity>() {
      @Override
      public PropertyUpdateStrategy onPrePersist(AuditedEntity entity, EntityMetadata metadata,
              PropertyUpdateStrategy strategy) {
        PropertyUpdateStrategy effective = (target, property) -> strategy.shouldUpdate(target, property);
        return effective;
      }

      @Override
      public void onPostPersist(AuditedEntity entity, EntityMetadata metadata, List<EntityProperty> properties) {
        assertThat(properties).extracting(EntityProperty::getName)
                .containsExactly("id", "createdAt", "createdBy", "modifiedAt", "modifiedBy");
        assertThatThrownBy(properties::clear).isInstanceOf(UnsupportedOperationException.class);
        persistedIds.add(entity.id);
      }
    });
    assertThat(manager.persist(List.of(entity(1L), entity(2L), entity(3L)), original)).isEqualTo(3);
    assertThat(batchSizes).containsExactly(2, 1);
    assertThat(persistedIds).containsExactly(1L, 2L, 3L);
    assertThat(manager.count(AuditedEntity.class).intValue()).isEqualTo(3);
  }

  @Test
  void unavailableAuditorDoesNotClearStoredIdentityOnPartialUpdate() {
    manager.persist(entity(1L));
    auditor.set(null);
    AuditedEntity patch = entity(1L);
    manager.updateById(patch, (object, property) -> false);
    assertThat(manager.findById(AuditedEntity.class, 1L).modifiedBy).isEqualTo("creator");
    assertThat(patch.modifiedBy).isNull();
  }

  @Test
  void removingListenerRestoresRegularPropertySelection() {
    manager.persist(entity(1L));
    manager.getEntityEventRegistry().removeListener(listener);
    AuditedEntity patch = entity(1L);
    patch.createdBy = "manual";
    manager.updateById(patch);
    assertThat(manager.findById(AuditedEntity.class, 1L).createdBy).isEqualTo("manual");
  }

  @Test
  void ordinaryListenersCanCustomizeSelectionAndObserveEffectiveStrategy() {
    manager.getEntityEventRegistry().removeListener(listener);
    manager.getEntityEventRegistry().addListener(new PersistEventListener<AuditedEntity>() {
      @Override
      public PropertyUpdateStrategy onPrePersist(AuditedEntity entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
        entity.name = "listener-value";
        return (target, property) -> property.getName().equals("name") || strategy.shouldUpdate(target, property);
      }

      @Override
      public void onPostPersist(AuditedEntity entity, EntityMetadata metadata, List<EntityProperty> properties) {
        assertThat(properties).extracting(EntityProperty::getName).containsExactly("id", "name");
      }
    });
    manager.persist(entity(1L), (object, property) -> property.isIdProperty());
    assertThat(manager.findById(AuditedEntity.class, 1L).name).isEqualTo("listener-value");
    manager.getEntityEventRegistry().addListener(new UpdateEventListener<AuditedEntity>() {
      @Override
      public PropertyUpdateStrategy onPreUpdate(AuditedEntity entity, EntityMetadata metadata,
              PropertyUpdateStrategy strategy) {
        return (target, property) -> property.getName().equals("name");
      }
    });
    AuditedEntity patch = entity(1L);
    patch.name = "custom-update";
    manager.updateById(patch, (object, property) -> false);
    assertThat(manager.findById(AuditedEntity.class, 1L).name).isEqualTo("custom-update");
  }

  @Test
  void allDateTypesUseTheSameInstantAndConfiguredZone() {
    DateEntity entity = new DateEntity();
    AtomicInteger calls = new AtomicInteger();
    Clock clock = mock(Clock.class);
    given(clock.instant()).willReturn(NOW);
    given(clock.getZone()).willReturn(ZoneId.of("Asia/Shanghai"));
    AuditingEntityListener auditing = new AuditingEntityListener(
            clock, () -> {
              calls.incrementAndGet();
              return "user";
            });
    auditing.onPrePersist(entity, new DefaultEntityMetadataFactory().getEntityMetadata(DateEntity.class),
            PropertyUpdateStrategy.noneNull());
    assertThat(entity.instant).isEqualTo(NOW);
    assertThat(entity.local).isEqualTo(LocalDateTime.of(2026, 10, 5, 20, 0));
    assertThat(entity.offset.toInstant()).isEqualTo(NOW);
    assertThat(entity.zoned.toInstant()).isEqualTo(NOW);
    assertThat(entity.zoned.getZone()).isEqualTo(ZoneId.of("Asia/Shanghai"));
    assertThat(calls).hasValue(1);
    verify(clock).instant();
  }

  @Test
  void invalidDateAndAuditorTypesFailBeforeSql() {
    assertThatThrownBy(() -> manager.persist(new InvalidDateEntity()))
            .isInstanceOf(IllegalEntityException.class).hasMessageContaining("Unsupported audit date type");
    manager.getEntityEventRegistry().removeListener(listener);
    AuditingEntityListener auditing = new AuditingEntityListener(Clock.systemUTC(), () -> 42L);
    manager.getEntityEventRegistry().addListener(auditing);
    assertThatThrownBy(() -> manager.persist(entity(1L)))
            .isInstanceOfAny(ClassCastException.class, IllegalArgumentException.class);
    assertThat(manager.count(AuditedEntity.class).intValue()).isZero();
  }

  @Test
  void auditorPropertiesPopulateIdAndNameIndependently() {
    repository.createNamedQuery("create table detailed_audit (id bigint primary key, "
            + "creator_id bigint, creator_name varchar(64), editor_id bigint, editor_name varchar(64))")
            .executeUpdate();
    AtomicReference<Operator> operator = new AtomicReference<>(new Operator(42L, "Alice"));
    AtomicInteger calls = new AtomicInteger();
    manager.getEntityEventRegistry().removeListener(listener);
    manager.getEntityEventRegistry().addListener(new AuditingEntityListener(Clock.systemUTC(), () -> {
      calls.incrementAndGet();
      return operator.get();
    }));
    DetailedAuditEntity entity = new DetailedAuditEntity();
    entity.id = 1L;
    manager.persist(entity, (object, property) -> property.isIdProperty());
    DetailedAuditEntity loaded = manager.findById(DetailedAuditEntity.class, 1L);
    assertThat(loaded.creatorId).isEqualTo(42L);
    assertThat(loaded.creatorName).isEqualTo("Alice");
    assertThat(loaded.editorId).isEqualTo(42L);
    assertThat(loaded.editorName).isEqualTo("Alice");
    assertThat(calls).hasValue(1);

    operator.set(new Operator(43L, "Bob"));
    manager.updateById(entity, (object, property) -> false);
    loaded = manager.findById(DetailedAuditEntity.class, 1L);
    assertThat(loaded.creatorId).isEqualTo(42L);
    assertThat(loaded.creatorName).isEqualTo("Alice");
    assertThat(loaded.editorId).isEqualTo(43L);
    assertThat(loaded.editorName).isEqualTo("Bob");
    assertThat(calls).hasValue(2);
  }

  private record Operator(Long id, String name) {
  }

  @Table("detailed_audit")
  static class DetailedAuditEntity {
    public Long id;
    @CreatedBy("id")
    public Long creatorId;
    @CreatedBy("name")
    public String creatorName;
    @LastModifiedBy("id")
    public Long editorId;
    @LastModifiedBy("name")
    public String editorName;
  }

  @Test
  void conflictingAnnotationsFailBeforeSql() {
    assertThatThrownBy(() -> manager.persist(new ConflictingEntity()))
            .isInstanceOf(IllegalEntityException.class).hasMessageContaining("Conflicting audit annotations");
  }

  @Test
  void auditAnnotationOnIdFailsBeforeSql() {
    assertThatThrownBy(() -> manager.persist(new AuditedIdEntity()))
            .isInstanceOf(IllegalEntityException.class).hasMessageContaining("id");
    assertThat(manager.count(AuditedEntity.class).intValue()).isZero();
  }

  @Test
  void ordinaryEntityRetainsStrategyWithoutResolvingProviders() {
    Clock clock = mock(Clock.class);
    AuditorAware<?> provider = mock(AuditorAware.class);
    AuditingEntityListener auditing = new AuditingEntityListener(clock, provider);
    PlainEntity entity = new PlainEntity();
    EntityMetadata metadata = new DefaultEntityMetadataFactory().getEntityMetadata(PlainEntity.class);
    PropertyUpdateStrategy strategy = PropertyUpdateStrategy.noneNull();
    assertThat(auditing.onPrePersist(entity, metadata, strategy)).isSameAs(strategy);
    assertThat(auditing.onPreUpdate(entity, metadata, strategy)).isSameAs(strategy);
    verifyNoInteractions(clock, provider);
  }

  @Test
  void nestedPathAndComposedAnnotationPreserveNullValues() {
    AtomicReference<Profile> profile = new AtomicReference<>(new Profile("Alice"));
    AuditingEntityListener auditing = new AuditingEntityListener(Clock.systemUTC(),
            () -> new ProfileOperator(profile.get()));
    PathEntity entity = new PathEntity();
    EntityMetadata metadata = new DefaultEntityMetadataFactory().getEntityMetadata(PathEntity.class);
    auditing.onPrePersist(entity, metadata, PropertyUpdateStrategy.noneNull());
    assertThat(entity.creator).isEqualTo("Alice");
    assertThat(entity.editor).isEqualTo("Alice");
    profile.set(new Profile(null));
    auditing.onPrePersist(entity, metadata, PropertyUpdateStrategy.noneNull());
    auditing.onPreUpdate(entity, metadata, PropertyUpdateStrategy.noneNull());
    assertThat(entity.creator).isEqualTo("Alice");
    assertThat(entity.editor).isEqualTo("Alice");
  }

  @Test
  void unreadableAuditorPathFailsBeforeSql() {
    manager.getEntityEventRegistry().removeListener(listener);
    manager.getEntityEventRegistry().addListener(new AuditingEntityListener(Clock.systemUTC(), () -> new Operator(1L, "Alice")));
    assertThatThrownBy(() -> manager.persist(new PathEntity()))
            .isInstanceOf(NotReadablePropertyException.class);
    assertThat(manager.count(AuditedEntity.class).intValue()).isZero();
  }

  @Test
  void writeOnlyAuditorPathFailsBeforeSql() {
    manager.getEntityEventRegistry().removeListener(listener);
    manager.getEntityEventRegistry().addListener(new AuditingEntityListener(Clock.systemUTC(), WriteOnlyOperator::new));
    assertThatThrownBy(() -> manager.persist(new PathEntity()))
            .isInstanceOf(NotReadablePropertyException.class);
    assertThat(manager.count(AuditedEntity.class).intValue()).isZero();
  }

  @Test
  void subsequentListenerCanReplaceAuditSelectionRules() {
    manager.persist(entity(1L));
    manager.getEntityEventRegistry().addListener(new UpdateEventListener<AuditedEntity>() {
      @Override
      public PropertyUpdateStrategy onPreUpdate(AuditedEntity entity, EntityMetadata metadata,
              PropertyUpdateStrategy strategy) {
        return (target, property) -> property.getName().equals("createdBy");
      }
    });
    AuditedEntity patch = entity(1L);
    patch.createdBy = "replacement";
    manager.updateById(patch);
    assertThat(manager.findById(AuditedEntity.class, 1L).createdBy).isEqualTo("replacement");
  }

  static class WriteOnlyOperator {
    public void setProfile(Profile profile) {
    }
  }

  @Test
  void equalMetadataInstancesKeepTheirOwnPropertyRoles() {
    AuditedEntity entity = entity(1L);
    EntityMetadata first = new DefaultEntityMetadataFactory().getEntityMetadata(AuditedEntity.class);
    EntityMetadata second = new DefaultEntityMetadataFactory().getEntityMetadata(AuditedEntity.class);
    assertThat(first).isEqualTo(second).isNotSameAs(second);
    PropertyUpdateStrategy excludeAll = (target, property) -> false;
    listener.onPrePersist(entity, first, excludeAll);
    PropertyUpdateStrategy selected = listener.onPrePersist(entity, second, excludeAll);
    assertThat(second.getEntityProperties(false)).allSatisfy(property ->
            assertThat(selected.shouldUpdate(entity, property)).isEqualTo(!property.getName().equals("name")));
    entity.modifiedBy = null;
    EntityProperty modifiedBy = second.findProperty("modifiedBy");
    assertThat(modifiedBy).isNotNull();
    assertThat(selected.shouldUpdate(entity, modifiedBy)).isFalse();
  }

  @Target(ElementType.FIELD)
  @Retention(RetentionPolicy.RUNTIME)
  @CreatedBy
  @interface Creator {
    @AliasFor(annotation = CreatedBy.class, attribute = "value")
    String value();
  }

  private record Profile(@Nullable String name) {
  }

  private record ProfileOperator(Profile profile) {
  }

  static class PathEntity {
    @Creator("profile.name")
    public String creator;
    @LastModifiedBy("profile.name")
    public String editor;
  }

  static class PlainEntity {
    public Long id;
  }

  static class AuditedIdEntity {
    @CreatedBy
    public Long id;
  }

  private static AuditedEntity entity(Long id) {
    AuditedEntity entity = new AuditedEntity();
    entity.id = id;
    return entity;
  }

  @Table("audited_entity")
  static class AuditedEntity {
    public Long id;
    public String name;
    @CreatedDate
    public Instant createdAt;
    @CreatedBy
    public String createdBy;
    @LastModifiedDate
    public Instant modifiedAt;
    @LastModifiedBy
    public String modifiedBy;
  }

  @Table("audited_entity")
  static class ConditionalEntity {
    @UpdateBy
    @Column(name = "id")
    public Long key;
    public String name;
    @CreatedBy
    public String createdBy;
    @LastModifiedBy
    public String modifiedBy;
  }

  static class DateEntity {
    @CreatedDate
    public Instant instant;
    @LastModifiedDate
    public LocalDateTime local;
    @LastModifiedDate
    public OffsetDateTime offset;
    @LastModifiedDate
    public ZonedDateTime zoned;
    @CreatedBy
    public String creator;
    @LastModifiedBy
    public String editor;
  }

  static class InvalidDateEntity {
    @CreatedDate
    public String date;
  }

  static class ConflictingEntity {
    @CreatedDate
    @LastModifiedDate
    public Instant date;
  }

}
