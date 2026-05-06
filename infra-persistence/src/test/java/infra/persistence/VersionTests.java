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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.Timestamp;
import java.time.Instant;

import javax.sql.DataSource;

import infra.dao.OptimisticLockingFailureException;
import infra.jdbc.AbstractRepositoryManagerTests;
import infra.jdbc.NamedQuery;
import infra.jdbc.RepositoryManager;
import infra.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link Version} annotation and optimistic locking functionality
 * in {@link DefaultEntityManager}, {@link EntityMetadata}, and
 * {@link DefaultEntityMetadataFactory}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class VersionTests extends AbstractRepositoryManagerTests {

  // -------------------------------------------------------------------------
  // Test entity classes
  // -------------------------------------------------------------------------

  @Table("t_versioned")
  static class VersionedModel {
    @Id
    @GeneratedId
    public Integer id;

    public String name;

    @Version
    public Integer version;
  }

  @Table("t_versioned")
  static class VersionedLongModel {
    @Id
    @GeneratedId
    public Integer id;

    public String name;

    @Version
    public Long version;
  }

  @Table("t_versioned")
  static class VersionedShortModel {
    @Id
    @GeneratedId
    public Integer id;

    public String name;

    @Version
    public Short version;
  }

  @Table("t_versioned")
  static class VersionedTimestampModel {
    @Id
    @GeneratedId
    public Integer id;

    public String name;

    @Version
    public Timestamp version;
  }

  @Table("t_versioned")
  static class VersionedInstantModel {
    @Id
    @GeneratedId
    public Integer id;

    public String name;

    @Version
    public Instant version;
  }

  @Table("t_versioned")
  static class VersionedOnGetterModel {
    private Integer id;
    private String name;
    private Long version;

    @Id
    @GeneratedId
    public Integer getId() {
      return id;
    }

    public void setId(Integer id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    @Version
    public Long getVersion() {
      return version;
    }

    public void setVersion(Long version) {
      this.version = version;
    }
  }

  @Table("t_versioned")
  static class MetaVersionedModel {
    @Id
    @GeneratedId
    public Integer id;

    public String name;

    @MyVersion
    public Integer version;
  }

  @Table("t_versioned")
  static class MultiVersionModel {
    @Id
    @GeneratedId
    public Integer id;

    @Version
    public Integer version1;

    @Version
    public Integer version2;
  }

  // -------------------------------------------------------------------------
  // Meta-annotation for testing composed annotation support
  // -------------------------------------------------------------------------

  @Version
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.FIELD, ElementType.METHOD })
  @interface MyVersion {
  }

  // -------------------------------------------------------------------------
  // Table preparation
  // -------------------------------------------------------------------------

  @Override
  protected void prepareTestsData(DbType dbType, RepositoryManager repositoryManager) {
    repositoryManager.createNamedQuery("drop table if exists t_versioned").executeUpdate();

    try (NamedQuery query = repositoryManager.createNamedQuery("""
            create table t_versioned
            (
                `id`      int auto_increment primary key,
                `name`    varchar(255)  default null,
                `version` bigint        default 0
            );
            """)) {
      query.executeUpdate();
    }
  }

  // -------------------------------------------------------------------------
  // EntityMetadata: versionProperty detection
  // -------------------------------------------------------------------------

  @Test
  void entityMetadata_detectsVersionPropertyOnField() {
    DefaultEntityMetadataFactory factory = new DefaultEntityMetadataFactory();
    EntityMetadata metadata = factory.getEntityMetadata(VersionedModel.class);

    assertThat(metadata.versionProperty).isNotNull();
    assertThat(metadata.versionProperty.columnName).isEqualTo("version");
  }

  @Test
  void entityMetadata_detectsVersionPropertyOnGetter() {
    DefaultEntityMetadataFactory factory = new DefaultEntityMetadataFactory();
    EntityMetadata metadata = factory.getEntityMetadata(VersionedOnGetterModel.class);

    assertThat(metadata.versionProperty).isNotNull();
    assertThat(metadata.versionProperty.columnName).isEqualTo("version");
  }

  @Test
  void entityMetadata_detectsMetaAnnotatedVersion() {
    DefaultEntityMetadataFactory factory = new DefaultEntityMetadataFactory();
    EntityMetadata metadata = factory.getEntityMetadata(MetaVersionedModel.class);

    assertThat(metadata.versionProperty).isNotNull();
    assertThat(metadata.versionProperty.columnName).isEqualTo("version");
    assertThat(metadata.versionProperty.isPresent(Version.class)).isTrue();
    assertThat(metadata.versionProperty.isPresent(MyVersion.class)).isTrue();
  }

  @Test
  void entityMetadata_rejectsMultipleVersionAnnotations() {
    DefaultEntityMetadataFactory factory = new DefaultEntityMetadataFactory();

    assertThatThrownBy(() -> factory.getEntityMetadata(MultiVersionModel.class))
            .isInstanceOf(IllegalEntityException.class)
            .hasMessageStartingWith("Only one Version property supported");
  }

  @Test
  void entityMetadata_versionPropertyInEntityProperties() {
    DefaultEntityMetadataFactory factory = new DefaultEntityMetadataFactory();
    EntityMetadata metadata = factory.getEntityMetadata(VersionedModel.class);

    assertThat(metadata.entityProperties)
            .extracting(p -> p.columnName)
            .contains("version");
  }

  @Test
  void entityMetadata_versionPropertyExcludedFromIdExcludeList() {
    DefaultEntityMetadataFactory factory = new DefaultEntityMetadataFactory();
    EntityMetadata metadata = factory.getEntityMetadata(VersionedModel.class);

    // version is not the ID, so it should be in entityPropertiesExcludeId
    assertThat(metadata.entityPropertiesExcludeId)
            .extracting(p -> p.columnName)
            .contains("version");
  }

  // -------------------------------------------------------------------------
  // Integration tests (H2 and HyperSQL)
  // -------------------------------------------------------------------------

  @ParameterizedRepositoryManagerTest
  void persist_withVersion_initialValue(DbType dbType, RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);

    VersionedModel model = new VersionedModel();
    model.name = "test-entity";
    model.version = 0;

    entityManager.persist(model, true);

    assertThat(model.id).isNotNull();
    assertThat(model.version).isEqualTo(0);

    // verify via entityManager
    VersionedModel found = entityManager.findById(VersionedModel.class, model.id);
    assertThat(found).isNotNull();
    assertThat(found.version).isEqualTo(0);
    assertThat(found.name).isEqualTo("test-entity");
  }

  @ParameterizedRepositoryManagerTest
  void persist_withVersion_includesVersionColumn(DbType dbType, RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);

    VersionedModel model = new VersionedModel();
    model.name = "test-entity";
    model.version = 5;

    entityManager.persist(model, true);

    assertThat(model.id).isNotNull();
    assertThat(model.version).isEqualTo(5);

    VersionedModel found = entityManager.findById(VersionedModel.class, model.id);
    assertThat(found).isNotNull();
    assertThat(found.version).isEqualTo(5);
  }

  @ParameterizedRepositoryManagerTest
  void updateById_withVersion_incrementsVersion(DbType dbType, RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);

    VersionedModel model = new VersionedModel();
    model.name = "update-test";
    model.version = 0;
    entityManager.persist(model, true);

    int initialVersion = model.version;

    model.name = "updated-name";
    int updatedRows = entityManager.updateById(model);

    assertThat(updatedRows).isEqualTo(1);
    assertThat(model.version).isEqualTo(initialVersion + 1);

    // verify via entityManager
    VersionedModel found = entityManager.findById(VersionedModel.class, model.id);
    assertThat(found).isNotNull();
    assertThat(found.version).isEqualTo(1);

    // update again
    model.name = "updated-again";
    entityManager.updateById(model);
    assertThat(model.version).isEqualTo(2);

    found = entityManager.findById(VersionedModel.class, model.id);
    assertThat(found).isNotNull();
    assertThat(found.version).isEqualTo(2);
  }

  @ParameterizedRepositoryManagerTest
  void updateById_withVersion_short(DbType dbType, RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);

    VersionedShortModel model = new VersionedShortModel();
    model.name = "short-version";
    model.version = (short) 0;
    entityManager.persist(model, true);

    model.name = "short-updated";
    entityManager.updateById(model);

    assertThat(model.version).isEqualTo((short) 1);
  }

  @ParameterizedRepositoryManagerTest
  void updateById_withVersion_defaultNull_incrementedTo1(DbType dbType, RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);

    // Persist with explicit version = 0, then update — version must always be present
    // for optimistic locking to work correctly.
    VersionedModel model = new VersionedModel();
    model.name = "versioned";
    model.version = 0;

    entityManager.persist(model, true);
    assertThat(model.id).isNotNull();
    assertThat(model.version).isEqualTo(0);

    model.name = "updated";
    entityManager.updateById(model);

    // version should be incremented from 0 to 1
    assertThat(model.version).isEqualTo(1);
  }

  // -------------------------------------------------------------------------
  // Meta-annotation integration test
  // -------------------------------------------------------------------------

  @ParameterizedRepositoryManagerTest
  void metaAnnotatedVersion_worksOnUpdate(DbType dbType, RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);

    MetaVersionedModel model = new MetaVersionedModel();
    model.name = "meta-version";
    model.version = 0;
    entityManager.persist(model, true);

    model.name = "meta-updated";
    entityManager.updateById(model);

    assertThat(model.version).isEqualTo(1);
  }

  @ParameterizedRepositoryManagerTest
  void metaAnnotatedVersion_detectedByMetadata(DbType dbType, RepositoryManager repositoryManager) {
    DefaultEntityMetadataFactory factory = new DefaultEntityMetadataFactory();

    EntityMetadata metadata = factory.getEntityMetadata(MetaVersionedModel.class);
    assertThat(metadata.versionProperty).isNotNull();
    assertThat(metadata.versionProperty.isPresent(Version.class)).isTrue();
  }

  @ParameterizedRepositoryManagerTest
  void metaAnnotatedVersion_conflict_detected(DbType dbType, RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);

    MetaVersionedModel model = new MetaVersionedModel();
    model.name = "meta-conflict";
    model.version = 0;
    entityManager.persist(model, true);

    // simulate concurrent modification via a second entityManager
    DefaultEntityManager entityManager2 = new DefaultEntityManager(repositoryManager);
    MetaVersionedModel copy = entityManager2.findById(MetaVersionedModel.class, model.id);
    copy.name = "modified-by-other";
    entityManager2.updateById(copy);

    assertThatThrownBy(() -> entityManager.updateById(model))
            .isInstanceOf(OptimisticLockingFailureException.class);
  }

  // -------------------------------------------------------------------------
  // Annotation target tests
  // -------------------------------------------------------------------------

  @Test
  void version_annotation_targetsFieldAndMethod() {
    @Table("t_versioned")
    class FieldVersion {
      @Id
      public Integer id;
      @Version
      public Integer version;
    }

    @Table("t_versioned")
    class MethodVersion {
      private Integer id;
      private Integer version;

      @Id
      public Integer getId() { return id; }

      public void setId(Integer id) { this.id = id; }

      @Version
      public Integer getVersion() { return version; }

      public void setVersion(Integer version) { this.version = version; }
    }

    DefaultEntityMetadataFactory factory = new DefaultEntityMetadataFactory();

    assertThatNoException().isThrownBy(() -> factory.getEntityMetadata(FieldVersion.class));
    assertThatNoException().isThrownBy(() -> factory.getEntityMetadata(MethodVersion.class));

    EntityMetadata fieldMeta = factory.getEntityMetadata(FieldVersion.class);
    EntityMetadata methodMeta = factory.getEntityMetadata(MethodVersion.class);

    assertThat(fieldMeta.versionProperty).isNotNull();
    assertThat(methodMeta.versionProperty).isNotNull();
  }

  // -------------------------------------------------------------------------
  // Custom VersionIncrementStrategy on DefaultEntityManager
  // -------------------------------------------------------------------------

  @Test
  void entityManager_setVersionIncrementStrategy() {
    VersionIncrementStrategy customStrategy = currentVersion -> 99;
    RepositoryManager repositoryManager = mock(RepositoryManager.class);
    DataSource dataSource = mock(DataSource.class);
    when(repositoryManager.getDataSource()).thenReturn(dataSource);

    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);
    entityManager.setVersionIncrementStrategy(customStrategy);

    // verify via reflection that the field was set
    Object fieldValue = ReflectionTestUtils.getField(entityManager, "versionIncrementStrategy");
    assertThat(fieldValue).isSameAs(customStrategy);
  }

}
