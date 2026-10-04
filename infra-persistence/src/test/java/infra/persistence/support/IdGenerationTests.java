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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import infra.beans.factory.DisposableBean;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.NoUniqueBeanDefinitionException;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.core.annotation.AliasFor;
import infra.jdbc.RepositoryManager;
import infra.jdbc.datasource.DriverManagerDataSource;
import infra.persistence.DefaultEntityMetadataFactory;
import infra.persistence.EntityMetadata;
import infra.persistence.EntityMetadata.IdGeneration;
import infra.persistence.EntityProperty;
import infra.persistence.GenerationType;
import infra.persistence.IdGenerator;
import infra.persistence.IllegalEntityException;
import infra.persistence.PropertyUpdateStrategy;
import infra.persistence.annotation.GeneratedId;
import infra.persistence.annotation.GeneratedUuid;
import infra.persistence.annotation.Table;
import infra.persistence.event.BatchExecution;
import infra.persistence.event.BatchPersistListener;
import infra.persistence.event.PersistEventListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdGenerationTests {

  RepositoryManager repository;

  DefaultEntityManager manager;

  StandardBeanFactory factory;

  IdGeneratorResolver idGeneratorResolver;

  @BeforeEach
  void setup() {
    repository = new RepositoryManager(new DriverManagerDataSource(
            "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
    repository.createNamedQuery("create table generated_entity (id varchar(64) primary key, name varchar(64))")
            .executeUpdate();
    factory = new StandardBeanFactory();
    factory.registerSingleton("sequence", new AtomicInteger());
    manager = new DefaultEntityManager(repository);
    idGeneratorResolver = new IdGeneratorResolver(factory);
    manager.setIdGeneratorResolver(idGeneratorResolver);
  }

  @Test
  void composedAnnotationCreatesInjectedGeneratorAndPreservesAssignedId() {
    ClassEntity first = new ClassEntity();
    first.name = "first";
    assertThat(manager.saveOrUpdate(first)).isEqualTo(1);
    assertThat(first.id).isEqualTo("id-1");

    ClassEntity assigned = new ClassEntity();
    assigned.id = "assigned";
    assertThat(manager.persist(assigned, PropertyUpdateStrategy.notId())).isEqualTo(1);
    assertThat(assigned.id).isEqualTo("assigned");
    assertThat(factory.getBean(AtomicInteger.class).get()).isEqualTo(1);
    assertThat(repository.createNamedQuery("select id from generated_entity order by id").fetch(String.class))
            .containsExactly("assigned", "id-1");
    idGeneratorResolver.destroy();
    assertThat(factory.getBean(AtomicInteger.class).get()).isEqualTo(-1);
  }

  @Test
  void metaAnnotationAliasSelectsNamedBeanInBatch() {
    factory.registerSingleton("named", (IdGenerator) (object, metadata, id) -> "named-" +
            factory.getBean(AtomicInteger.class).incrementAndGet());
    NamedEntity first = new NamedEntity();
    first.name = "first";
    NamedEntity second = new NamedEntity();
    manager.setMaxBatchRecords(1);
    assertThat(manager.persist(List.of(first, second))).isEqualTo(2);
    assertThat(first.id).isEqualTo("named-1");
    assertThat(second.id).isEqualTo("named-2");
    assertThat(repository.createNamedQuery("select id from generated_entity").fetch(String.class))
            .containsExactlyInAnyOrder("named-1", "named-2");
  }

  @Test
  void uuidIsInsertedEvenWhenPropertyStrategyExcludesId() {
    UuidEntity entity = new UuidEntity();
    manager.persist(entity, PropertyUpdateStrategy.notId());
    assertThat(UUID.fromString(entity.id).version()).isEqualTo(7);
    assertThat(repository.createNamedQuery("select id from generated_entity").fetchFirst(String.class))
            .isEqualTo(entity.id);
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  void configuredUuidGeneratorSupportsStringAndUuidProperties(boolean batch) {
    repository.createNamedQuery("create table uuid_entity (id uuid primary key)").executeUpdate();
    UUID generated = UUID.fromString("12345678-1234-4234-8234-123456789abc");
    AtomicInteger calls = new AtomicInteger();
    idGeneratorResolver.setUuidGenerator(() -> {
      calls.incrementAndGet();
      return generated;
    });
    UuidEntity stringEntity = new UuidEntity();
    UuidPropertyEntity uuidEntity = new UuidPropertyEntity();
    UuidEntity assigned = new UuidEntity();
    assigned.id = "assigned";

    if (batch) {
      manager.persist(List.of(stringEntity, uuidEntity, assigned), PropertyUpdateStrategy.notId());
    }
    else {
      manager.persist(stringEntity, PropertyUpdateStrategy.notId());
      manager.persist(uuidEntity, PropertyUpdateStrategy.notId());
      manager.persist(assigned);
    }

    assertThat(stringEntity.id).isEqualTo(generated.toString());
    assertThat(uuidEntity.id).isSameAs(generated);
    assertThat(calls.get()).isEqualTo(2);
    assertThat(repository.createNamedQuery("select id from generated_entity").fetch(String.class))
            .containsExactlyInAnyOrder(generated.toString(), "assigned");
    assertThat(repository.createNamedQuery("select id from uuid_entity").fetchFirst(UUID.class))
            .isEqualTo(generated);
  }

  @Test
  void nullUuidGeneratorRestoresVersion7() {
    UUID generated = UUID.fromString("12345678-1234-4234-8234-123456789abc");
    idGeneratorResolver.setUuidGenerator(() -> generated);
    idGeneratorResolver.setUuidGenerator(null);
    UuidEntity entity = new UuidEntity();
    manager.persist(entity);
    assertThat(UUID.fromString(entity.id).version()).isEqualTo(7);
  }

  @Test
  void injectedResolverUsesConfiguredUuidGenerator() {
    UUID generated = UUID.fromString("12345678-1234-4234-8234-123456789abc");
    IdGeneratorResolver resolver = new IdGeneratorResolver(factory);
    resolver.setUuidGenerator(() -> generated);
    manager.setIdGeneratorResolver(resolver);

    UuidEntity entity = new UuidEntity();
    manager.persist(entity);
    assertThat(manager).extracting("idGeneratorResolver").isSameAs(resolver);
    assertThat(entity.id).isEqualTo(generated.toString());
  }

  @Test
  void uuidGeneratorFailurePreventsInsert() {
    IllegalStateException failure = new IllegalStateException("UUID generation failed");
    idGeneratorResolver.setUuidGenerator(() -> {
      throw failure;
    });
    assertThatThrownBy(() -> manager.persist(new UuidEntity()))
            .isSameAs(failure);
    assertThat(repository.createNamedQuery("select count(*) from generated_entity").fetchFirst(Integer.class))
            .isZero();
  }

  @Test
  void invalidGeneratorResultsFailBeforeInsert() {
    factory.registerSingleton("named", (IdGenerator) (object, metadata, id) -> 42);
    assertThatThrownBy(() -> manager.persist(new NamedEntity()))
            .isInstanceOf(IllegalEntityException.class).hasMessageContaining("incompatible");
    assertThat(repository.createNamedQuery("select count(*) from generated_entity").fetchFirst(Integer.class))
            .isZero();
  }

  @Test
  void uuidPropertyUsesVersion7AndPreservesExistingValue() {
    repository.createNamedQuery("create table uuid_entity (id uuid primary key)").executeUpdate();
    UuidPropertyEntity first = new UuidPropertyEntity();
    manager.persist(first);
    assertThat(first.id.version()).isEqualTo(7);
    UuidPropertyEntity second = new UuidPropertyEntity();
    UUID assigned = UUID.randomUUID();
    second.id = assigned;
    manager.persist(second);
    assertThat(second.id).isSameAs(assigned);
    assertThat(repository.createNamedQuery("select id from uuid_entity").fetch(UUID.class))
            .containsExactlyInAnyOrder(first.id, assigned);
  }

  @Test
  void resolverDoesNotFallbackForMissingNameOrAmbiguousType() {
    IdGeneratorResolver resolver = new IdGeneratorResolver(factory);
    assertThatThrownBy(() -> resolver.resolve(new IdGeneration(GenerationType.IDENTITY, InjectedGenerator.class, "missing")))
            .isInstanceOf(NoSuchBeanDefinitionException.class);
    factory.registerSingleton("first", new InjectedGenerator(new AtomicInteger()));
    factory.registerSingleton("second", new InjectedGenerator(new AtomicInteger()));
    assertThatThrownBy(() -> resolver.resolve(new IdGeneration(GenerationType.IDENTITY, InjectedGenerator.class, "")))
            .isInstanceOf(NoUniqueBeanDefinitionException.class);
    assertThat(resolver.resolve(new IdGeneration(GenerationType.IDENTITY, InjectedGenerator.class, "first")))
            .isSameAs(factory.getBean("first"));
  }

  @Test
  void existingBeanIsReusedAndNotDestroyedByResolver() {
    AtomicInteger sequence = new AtomicInteger();
    InjectedGenerator generator = new InjectedGenerator(sequence);
    factory.registerSingleton("existing", generator);
    IdGeneratorResolver resolver = new IdGeneratorResolver(factory);
    assertThat(resolver.resolve(new IdGeneration(GenerationType.IDENTITY, InjectedGenerator.class, ""))).isSameAs(generator);
    resolver.destroy();
    assertThat(sequence.get()).isZero();
  }

  @Test
  void prototypeGeneratorsAreReusedWithinResolver() {
    RootBeanDefinition definition = new RootBeanDefinition(InjectedGenerator.class);
    definition.setScope("prototype");
    definition.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
    factory.registerBeanDefinition("prototype", definition);
    IdGeneratorResolver resolver = new IdGeneratorResolver(factory);
    IdGeneration named = new IdGeneration(GenerationType.IDENTITY, InjectedGenerator.class, "prototype");
    IdGeneration typed = new IdGeneration(GenerationType.IDENTITY, InjectedGenerator.class, "");
    assertThat(resolver.resolve(named)).isSameAs(resolver.resolve(named));
    assertThat(resolver.resolve(typed)).isSameAs(resolver.resolve(typed));
    assertThat(new IdGeneratorResolver(factory).resolve(named)).isNotSameAs(resolver.resolve(named));
  }

  @Test
  void prePersistAssignmentTakesPrecedenceOverGeneration() {
    manager.getEntityEventRegistry().addListener(new PersistEventListener<ClassEntity>() {
      @Override
      public void onPrePersist(ClassEntity entity, infra.persistence.EntityMetadata metadata,
              PropertyUpdateStrategy strategy) {
        entity.id = "callback-id";
      }
    });
    ClassEntity entity = new ClassEntity();
    manager.persist(entity);
    assertThat(entity.id).isEqualTo("callback-id");
    assertThat(factory.getBean(AtomicInteger.class).get()).isZero();
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  void generatesIdAfterAllPrePersistCallbacks(boolean batch) {
    List<String> events = new ArrayList<>();
    factory.registerSingleton("named", (IdGenerator) (object, metadata, property) -> {
      NamedEntity entity = (NamedEntity) object;
      assertThat(entity.id).isNull();
      assertThat(entity.name).isEqualTo("prepared-ready");
      events.add("generate");
      return "id-" + entity.name;
    });

    manager.getEntityEventRegistry().addListener(new PersistEventListener<NamedEntity>() {
      @Override
      public void onPrePersist(NamedEntity entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
        assertThat(entity.id).isNull();
        entity.name = "prepared";
        events.add("pre-first");
      }
    });

    manager.getEntityEventRegistry().addListener(new PersistEventListener<NamedEntity>() {
      @Override
      public void onPrePersist(NamedEntity entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
        assertThat(entity.id).isNull();
        entity.name += "-ready";
        events.add("pre-second");
      }

      @Override
      public void onPostPersist(NamedEntity entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
        assertThat(entity.id).isEqualTo("id-prepared-ready");
        events.add("post");
      }
    });

    NamedEntity entity = new NamedEntity();
    int rows = batch ? manager.persist(List.of(entity), PropertyUpdateStrategy.notId())
            : manager.persist(entity, PropertyUpdateStrategy.notId());

    assertThat(rows).isEqualTo(1);
    assertThat(events).containsExactly("pre-first", "pre-second", "generate", "post");
    assertThat(repository.createNamedQuery("select id from generated_entity").fetchFirst(String.class))
            .isEqualTo("id-prepared-ready");
    assertThat(repository.createNamedQuery("select name from generated_entity").fetchFirst(String.class))
            .isEqualTo("prepared-ready");
  }

  @Test
  void batchSelectsColumnsForEachEntityAfterCallbacks() {
    NamedEntity first = new NamedEntity();
    NamedEntity second = new NamedEntity();
    NamedEntity third = new NamedEntity();
    third.name = "third";
    factory.registerSingleton("named", (IdGenerator) (object, metadata, property) ->
            "id-" + factory.getBean(AtomicInteger.class).incrementAndGet());
    manager.getEntityEventRegistry().addListener(new PersistEventListener<NamedEntity>() {
      @Override
      public void onPrePersist(NamedEntity entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
        if (entity == first) {
          entity.name = null;
        }
        else if (entity == second) {
          entity.name = "second";
        }
      }
    });

    assertThat(manager.persist(List.of(first, second, third))).isEqualTo(3);
    assertThat(repository.createNamedQuery("select name from generated_entity where id = 'id-1'")
            .fetchFirst(String.class)).isNull();
    assertThat(repository.createNamedQuery("select name from generated_entity where id = 'id-2'")
            .fetchFirst(String.class)).isEqualTo("second");
    assertThat(repository.createNamedQuery("select name from generated_entity where id = 'id-3'")
            .fetchFirst(String.class)).isEqualTo("third");
  }

  @Test
  void batchDoesNotRequestJdbcKeysForApplicationGeneratedIds() {
    AtomicInteger batches = new AtomicInteger();
    manager.getEntityEventRegistry().addListener(new BatchPersistListener() {
      @Override
      public void preProcessing(BatchExecution execution, boolean implicitExecution) {
        assertThat(execution.autoGenerateId).isFalse();
        batches.incrementAndGet();
      }

      @Override
      public void postProcessing(BatchExecution execution, boolean implicitExecution, @Nullable Throwable exception) {
        assertThat(exception).isNull();
      }
    });

    UuidEntity first = new UuidEntity();
    UuidEntity second = new UuidEntity();
    assertThat(manager.persist(List.of(first, second))).isEqualTo(2);
    assertThat(batches.get()).isEqualTo(1);
    assertThat(repository.createNamedQuery("select id from generated_entity").fetch(String.class))
            .containsExactlyInAnyOrder(first.id, second.id);
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  void callbackAssignedIdIsInsertedWithoutInvokingGenerator(boolean batch) {
    factory.registerSingleton("named", (IdGenerator) (object, metadata, property) -> {
      throw new AssertionError("Generator must not run for a callback-assigned ID");
    });

    manager.getEntityEventRegistry().addListener(new PersistEventListener<NamedEntity>() {
      @Override
      public void onPrePersist(NamedEntity entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
        assertThat(entity.id).isNull();
        entity.id = "callback-assigned";
      }

      @Override
      public void onPostPersist(NamedEntity entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
        assertThat(entity.id).isEqualTo("callback-assigned");
      }
    });

    NamedEntity entity = new NamedEntity();
    int rows = batch ? manager.persist(List.of(entity), PropertyUpdateStrategy.notId())
            : manager.persist(entity, PropertyUpdateStrategy.notId());

    assertThat(rows).isEqualTo(1);
    assertThat(entity.id).isEqualTo("callback-assigned");
    assertThat(repository.createNamedQuery("select id from generated_entity").fetchFirst(String.class))
            .isEqualTo("callback-assigned");
  }

  @Test
  void identityKeysAreReturnedForSingleAndBatchInsert() {
    repository.createNamedQuery("create table identity_entity (id bigint generated by default as identity primary key)")
            .executeUpdate();
    IdentityEntity first = new IdentityEntity();
    manager.persist(first);
    IdentityEntity second = new IdentityEntity();
    IdentityEntity third = new IdentityEntity();
    manager.persist(List.of(second, third));
    assertThat(List.of(first.id, second.id, third.id)).doesNotContainNull().doesNotHaveDuplicates();
  }

  @Test
  void metadataDistinguishesDatabaseGenerationAndComposedGenerators() {
    DefaultEntityMetadataFactory metadataFactory = new DefaultEntityMetadataFactory();
    assertThat(metadataFactory.getEntityMetadata(ClassEntity.class).isAutoGeneratedId()).isFalse();
    assertThat(metadataFactory.getEntityMetadata(NamedEntity.class).getIdGeneration().generatorName()).isEqualTo("named");
    assertThat(metadataFactory.getEntityMetadata(IdentityEntity.class).isAutoGeneratedId()).isTrue();
  }

  @Test
  void uuidIdSupportsGetterAndNestedMetaAnnotation() {
    DefaultEntityMetadataFactory metadataFactory = new DefaultEntityMetadataFactory();
    EntityMetadata metadata = metadataFactory.getEntityMetadata(GetterUuidEntity.class);
    assertThat(metadata.getIdProperty().getName()).isEqualTo("key");
    assertThat(metadata.getIdGeneration().strategy()).isEqualTo(GenerationType.UUID);
    assertThat(metadata.isAutoGeneratedId()).isFalse();
  }

  @Test
  void uuidIdRejectsUnsupportedPropertyType() {
    assertThatThrownBy(() -> new DefaultEntityMetadataFactory().getEntityMetadata(InvalidUuidEntity.class))
            .isInstanceOf(IllegalEntityException.class)
            .hasMessage("UUID generation requires a UUID or String ID property");
  }

  @GeneratedId(generator = InjectedGenerator.class)
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE })
  @interface CustomId {
  }

  @GeneratedId
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.FIELD, ElementType.METHOD })
  @interface NamedId {

    @AliasFor(annotation = GeneratedId.class, attribute = "generatorName")
    String value() default "";
  }

  @Table("generated_entity")
  static class ClassEntity {
    @CustomId
    public String id;
    public String name;
  }

  @Table("generated_entity")
  static class NamedEntity {
    @NamedId("named")
    public String id;
    public String name;
  }

  @Table("generated_entity")
  static class UuidEntity {
    @GeneratedUuid
    public String id;
    public String name;
  }

  @Table("identity_entity")
  static class IdentityEntity {
    @GeneratedId
    public Long id;
  }

  @Table("uuid_entity")
  static class UuidPropertyEntity {
    @GeneratedUuid
    public UUID id;
  }

  @GeneratedUuid
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @interface CustomUuidId {
  }

  static class GetterUuidEntity {
    private UUID key;

    @CustomUuidId
    public UUID getKey() {
      return key;
    }

    public void setKey(UUID key) {
      this.key = key;
    }
  }

  static class InvalidUuidEntity {
    @GeneratedUuid
    public Long id;
  }

  static class InjectedGenerator implements IdGenerator, DisposableBean {

    private final AtomicInteger sequence;

    InjectedGenerator(AtomicInteger sequence) {
      this.sequence = sequence;
    }

    @Override
    public Object generateId(Object entity, EntityMetadata metadata, EntityProperty idProperty) {
      return "id-" + sequence.incrementAndGet();
    }

    @Override
    public void destroy() {
      sequence.set(-1);
    }

  }

}
