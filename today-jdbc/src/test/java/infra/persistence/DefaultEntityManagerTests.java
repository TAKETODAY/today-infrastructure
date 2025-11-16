/*
 * Copyright 2017 - 2025 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.persistence;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.sql.DataSource;

import infra.beans.BeanProperty;
import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.MergedAnnotations;
import infra.dao.IncorrectResultSizeDataAccessException;
import infra.dao.InvalidDataAccessApiUsageException;
import infra.jdbc.JdbcUpdateAffectedIncorrectNumberOfRowsException;
import infra.jdbc.NamedQuery;
import infra.jdbc.Query;
import infra.jdbc.RepositoryManager;
import infra.jdbc.format.SqlStatementLogger;
import infra.jdbc.type.BasicTypeHandler;
import infra.jdbc.type.SmartTypeHandler;
import infra.jdbc.type.TypeHandler;
import infra.jdbc.type.WrappedTypeHandler;
import infra.lang.Descriptive;
import infra.persistence.model.Gender;
import infra.persistence.model.NoIdModel;
import infra.persistence.model.UserModel;
import infra.persistence.platform.Platform;
import infra.persistence.sql.Restriction;
import infra.test.util.ReflectionTestUtils;
import infra.transaction.TransactionDefinition;
import infra.util.CollectionUtils;

import static infra.persistence.PropertyUpdateStrategy.noneNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/16 22:48
 */
class DefaultEntityManagerTests extends AbstractRepositoryManagerTests {

  @Override
  protected void prepareTestsData(DbType dbType, RepositoryManager repositoryManager) {
    repositoryManager.createNamedQuery("drop table if exists t_user").executeUpdate();

    try (NamedQuery query = repositoryManager.createNamedQuery("""
            create table t_user
            (
                `id`               int auto_increment primary key,
                `age`              int           default 0    ,
                `name`             varchar(255)  default null ,
                `avatar`           varchar(255)  default null ,
                `password`         varchar(255)  default null ,
                `introduce`        varchar(1000) default null ,
                `email`            varchar(255)  default null ,
                `gender`           int           default -1   ,
                `mobile_phone`     varchar(36)   default null
            );
            """)) {

      query.executeUpdate();
    }

  }

  @ParameterizedRepositoryManagerTest
  void exception(DbType dbType, RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);

    assertThatThrownBy(() ->
            entityManager.persist(new Object()))
            .isInstanceOf(IllegalEntityException.class)
            .hasMessageStartingWith("Cannot determine properties");

  }

  @ParameterizedRepositoryManagerTest
  void persist(DbType dbType, RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);

    UserModel userModel = new UserModel();
    userModel.name = "TODAY";
    userModel.gender = Gender.MALE;
    userModel.age = 10;

    entityManager.persist(userModel, true);

    assertThat(userModel.id).isNotNull();

    try (NamedQuery query = repositoryManager.createNamedQuery("SELECT * from t_user where `id`=:id")) {
      query.addParameter("id", userModel.id);
      query.setAutoDerivingColumns(true);

      List<UserModel> userModels = query.fetch(UserModel.class);
      assertThat(userModels).hasSize(1);
      UserModel userModelInDB = CollectionUtils.firstElement(userModels);
      assertThat(userModelInDB).isNotNull();
      assertThat(userModelInDB.age).isEqualTo(userModel.age);
      assertThat(userModelInDB.name).isEqualTo(userModel.name);
      assertThat(userModelInDB.gender).isEqualTo(userModel.gender);
    }
  }

  @ParameterizedRepositoryManagerTest
  void batchPersist(DbType dbType, RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);
    entityManager.setMaxBatchRecords(10);

    UserModel userModel = UserModel.male("TODAY", 9);

    List<Object> entities = new ArrayList<>();
    entities.add(userModel);

    for (int i = 0; i < 10; i++) {
      entities.add(UserModel.male("TODAY", 10 + i));
    }

    entityManager.persist(entities);

    try (NamedQuery query = repositoryManager.createNamedQuery("SELECT * from t_user")) {
      query.setAutoDerivingColumns(true);

      List<UserModel> userModels = query.fetch(UserModel.class);
      assertThat(userModels).hasSize(11).isEqualTo(entities);

      UserModel userModelInDB = CollectionUtils.firstElement(userModels);
      assertThat(userModelInDB).isNotNull();
      assertThat(userModelInDB.age).isEqualTo(userModel.age);
      assertThat(userModelInDB.name).isEqualTo(userModel.name);
      assertThat(userModelInDB.gender).isEqualTo(userModel.gender);
    }

  }

  // find

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.METHOD, ElementType.FIELD })
  public @interface HasText {

  }

  static class UserForm {

    @Id
    @Column("id")
    Integer userId;

    @Where("`name` = ?")
    String name;

    @Nullable
    @Where(operator = "=")
    Integer age;

    @Where("`birthday` >= ?")
    LocalDate birthdayBegin;

    @Where("`birthday` <= ?")
    LocalDate birthdayEnd;

  }

  @ParameterizedRepositoryManagerTest
  void findByExample(DbType dbType, RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);

    UserModel userModel = UserModel.male("TODAY", 9);

    List<Object> entities = new ArrayList<>();
    entities.add(userModel);

    for (int i = 0; i < 10; i++) {
      entities.add(UserModel.male("TODAY", 10 + i));
    }

    entityManager.persist(entities);

    UserForm userForm = new UserForm();
    userForm.name = "TODAY";

    List<UserModel> list = entityManager.find(UserModel.class, userForm);

    assertThat(list).hasSize(entities.size()).isEqualTo(entities);

    // entityManager.iterate(UserModel.class, userForm, System.out::println);
  }

  @ParameterizedRepositoryManagerTest
  void iterateListOfQueryConditions(DbType dbType, RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);
    createData(entityManager);

//    QueryCondition condition =
//            QueryCondition.equalsTo("age", 10)
//                    .and(QueryCondition.of("name", ConditionOperator.LIKE, "T"));

//    QueryCondition condition = QueryCondition.of("name", Operator.SUFFIX_LIKE, "T");
    // entityManager.iterate(UserModel.class, condition, System.out::println);
  }

  @ParameterizedRepositoryManagerTest
  void deleteById(DbType dbType, RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);
    createData(entityManager);

    UserModel byId = entityManager.findById(UserModel.class, 1);
    assertThat(byId).isNotNull().extracting("id").isEqualTo(1);

    entityManager.delete(UserModel.class, 1);

    byId = entityManager.findById(UserModel.class, 1);
    assertThat(byId).isNull();
  }

  @ParameterizedRepositoryManagerTest
  void deleteByEntity(DbType dbType, RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);
    createData(entityManager);

    UserModel byId = entityManager.findById(UserModel.class, 1);
    assertThat(byId).isNotNull().extracting("id").isEqualTo(1);

    UserModel userModel = UserModel.forId(1);
    entityManager.delete(userModel);

    byId = entityManager.findById(UserModel.class, 1);
    assertThat(byId).isNull();

    for (int i = 1; i <= 10; i++) {
      byId = entityManager.findById(UserModel.class, i + 1);
      UserModel today = UserModel.male("TODAY", 10 + i - 1);
      today.id = i + 1;
      assertThat(byId).isEqualTo(today);
    }

    //

    userModel = new UserModel();
    userModel.age = 9;
    userModel.name = "TODAY";
    userModel.gender = Gender.MALE;

    int deleteRows = entityManager.delete(userModel);
    assertThat(deleteRows).isZero();

    userModel.age = null;
    userModel.name = "TODAY";
    userModel.gender = Gender.MALE;

    deleteRows = entityManager.delete(userModel);
    assertThat(deleteRows).isEqualTo(10);
  }

  @ParameterizedRepositoryManagerTest
  void findUnique(DbType dbType, RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);
    createData(entityManager);

    assertThatThrownBy(() ->
            entityManager.findUnique(UserModel.class, QueryBuilder.of()
                    .add(Restriction.isNotNull("name"))
                    .add(Restriction.or(Restriction.between("age")), 1, 20)))
            .isInstanceOf(IncorrectResultSizeDataAccessException.class);

    UserModel unique = entityManager.findUnique(UserModel.class,
            QueryBuilder.of(Restriction.isNotNull("name"))
                    .add(Restriction.equal("age"), 9));

    assertThat(unique).isNotNull()
            .extracting("id").isEqualTo(1);

    assertThat(unique).extracting("name").isEqualTo("TODAY");
    assertThat(unique).extracting("age").isEqualTo(9);

    int deleteRows = entityManager.delete(unique);
    assertThat(deleteRows).isEqualTo(1);

    // null
    unique = entityManager.findUnique(UserModel.class,
            QueryBuilder.of(Restriction.isNotNull("name"))
                    .add(Restriction.equal("age"), 9));
    assertThat(unique).isNull();
  }

  @ParameterizedRepositoryManagerTest
  void updateBy(DbType dbType, RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);
    createData(entityManager);

    UserModel userModel = UserModel.forId(1);
    String name = "TEST-UPDATE";
    userModel.setName(name);
    entityManager.updateBy(userModel, "id");

    UserModel model = entityManager.findById(UserModel.class, 1);
    assertThat(model).isNotNull();
    assertThat(model.getName()).isEqualTo(name);

    // throw

    assertThatThrownBy(() -> entityManager.updateBy(userModel, "id_"))
            .isInstanceOf(InvalidDataAccessApiUsageException.class)
            .hasMessage("Updating an entity, 'where' property 'id_' not found");

    userModel.setId(null);
    assertThatThrownBy(() -> entityManager.updateBy(userModel, "id"))
            .isInstanceOf(InvalidDataAccessApiUsageException.class)
            .hasMessageStartingWith("Updating an entity, 'where' property value 'id' is required");
  }

  @ParameterizedRepositoryManagerTest
  void updateById(DbType dbType, RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);
    createData(entityManager);

    UserModel userModel = entityManager.findById(UserModel.class, 1);
    assertThat(userModel).isNotNull();

    String name = "TEST-UPDATE";
    userModel.setName(name);

    assertThat(entityManager.updateById(userModel, 1)).isEqualTo(1);

    UserModel model = entityManager.findById(UserModel.class, 1);
    assertThat(model).isNotNull();
    assertThat(model.getName()).isEqualTo(name);
    assertThat(userModel.getAge()).isEqualTo(model.getAge());
    assertThat(userModel.getAvatar()).isEqualTo(model.getAvatar());
    assertThat(userModel.getGender()).isEqualTo(model.getGender());

    // throw

    userModel.setId(null);

    assertThatThrownBy(() -> entityManager.updateById(userModel, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Entity id is required");

    assertThatThrownBy(() -> entityManager.updateById(userModel))
            .isInstanceOf(InvalidDataAccessApiUsageException.class)
            .hasMessage("Updating an entity, ID value is required");

    assertThatThrownBy(() -> entityManager.updateById(userModel, "errorId"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Entity Id matches failed");

    assertThatThrownBy(() -> entityManager.updateById(new NoIdModel(), "errorId"))
            .isInstanceOf(InvalidDataAccessApiUsageException.class)
            .hasMessage("Updating an entity, Id property not found");

    UserModel update = UserModel.forId(1);

    assertThatThrownBy(() -> entityManager.updateById(update, (entity, property) -> false))
            .isInstanceOf(InvalidDataAccessApiUsageException.class)
            .hasMessage("Updating an entity, There is no update properties");

    assertThat(entityManager.updateById(update, (entity, property) -> property.property.getName().equals("age")))
            .isEqualTo(1);

    UserModel newVal = entityManager.findById(UserModel.class, 1);
    assertThat(newVal).isNotNull();
    assertThat(newVal.getAge()).isNull();

  }

  @ParameterizedRepositoryManagerTest
  void findMap(DbType dbType, RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);
    createData(entityManager);

    UserModel example = new UserModel();
    example.setName("TODAY");
    assertThat(entityManager.find(UserModel.class, example, "age")).isNotEmpty().hasSize(11);

    assertThat(entityManager.find(UserModel.class,
            QueryBuilder.of(Restriction.equal("name"), "TODAY"), "age"))
            .isEqualTo(entityManager.find(UserModel.class, example, "age"));
  }

  @ParameterizedRepositoryManagerTest
  void findMapWithMappingFunction(DbType dbType, RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);
    createData(entityManager);

    UserModel example = new UserModel();
    example.setName("TODAY");
    assertThat(entityManager.find(UserModel.class, example, UserModel::getAge)).isNotEmpty().hasSize(11);

    assertThat(entityManager.find(UserModel.class, QueryBuilder.of(Restriction.equal("name"), "TODAY"), UserModel::getAge))
            .isEqualTo(entityManager.find(example, UserModel::getAge));
  }

  @ParameterizedRepositoryManagerTest
  void findList(DbType dbType, RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);
    createData(entityManager);

    UserModel example = new UserModel();
    example.setName("TODAY");
    assertThat(entityManager.find(UserModel.class, example)).isNotEmpty().hasSize(11);

    assertThat(entityManager.find(UserModel.class,
            QueryBuilder.of(Restriction.equal("name"), "TODAY")))
            .isEqualTo(entityManager.find(UserModel.class, example))
            .isEqualTo(entityManager.find(example));
  }

  @ParameterizedRepositoryManagerTest
  void findByExampleMap(DbType dbType, RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);
    createData(entityManager);

    UserModel example = new UserModel();
    example.setName("TODAY");
    assertThat(entityManager.find(UserModel.class, example)).isNotEmpty().hasSize(11);

    assertThat(entityManager.find(UserModel.class, Map.of("name", "TODAY")))
            .isEqualTo(entityManager.find(UserModel.class, example))
            .isEqualTo(entityManager.find(example));
  }

  @ParameterizedRepositoryManagerTest
  void findSortBy(DbType dbType, RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);
    createData(entityManager);

    assertThat(entityManager.find(UserModel.class, Map.of("age", Order.DESC)))
            .isEqualTo(entityManager.find(UserModel.class, Map.of("id", Order.DESC)));
  }

  @ParameterizedRepositoryManagerTest
  void batchPersistListener(DbType dbType, RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);
    entityManager.setMaxBatchRecords(120);
    EntityMetadataFactory entityMetadataFactory = ReflectionTestUtils.getField(entityManager, "entityMetadataFactory");
    assertThat(entityMetadataFactory).isNotNull();

    entityManager.addBatchPersistListeners((execution, implicitExecution, e) -> {
      assertThat(implicitExecution).isFalse();
      assertThat(execution.entityMetadata).isEqualTo(entityMetadataFactory.getEntityMetadata(UserModel.class));
      assertThat(execution.entities).hasSize(11);
      assertThat(execution.autoGenerateId).isTrue();
      assertThat(e).isNull();
    });

    createData(entityManager);
  }

  @ParameterizedRepositoryManagerTest
  void count(DbType dbType, RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);
    if (dbType == DbType.HyperSQL) {
      entityManager.setPlatform(new HyperSQLPlatform());
    }
    UserModel userModel = UserModel.male("TODAY", 9);
    List<Object> entities = new ArrayList<>();
    entities.add(userModel);

    for (int i = 0; i < 100; i++) {
      entities.add(UserModel.male("TODAY", 10 + i));
    }
    entityManager.persist(entities);

    assertThat(entityManager.count(new UserModel())).isEqualTo(101L);
    assertThat(entityManager.count(UserModel.class)).isEqualTo(101L);
    assertThat(entityManager.count(UserModel.class, null)).isEqualTo(101L);
    UserForm userForm = new UserForm();
    userForm.age = 10;
    userForm.name = "TODAY";
    assertThat(entityManager.count(UserModel.class, userForm)).isEqualTo(1L);

  }

  @ParameterizedRepositoryManagerTest
  void page(DbType dbType, RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);
    if (dbType == DbType.HyperSQL) {
      entityManager.setPlatform(new HyperSQLPlatform());
    }
    UserModel userModel = UserModel.male("TODAY", 9);

    List<Object> entities = new ArrayList<>();
    entities.add(userModel);

    for (int i = 0; i < 100; i++) {
      entities.add(UserModel.male("TODAY", 10 + i));
    }

    entityManager.persist(entities);

    UserForm userForm = new UserForm();
    userForm.age = 10;
    userForm.name = "TODAY";

    Page<UserModel> page = entityManager.page(UserModel.class, userForm, Pageable.of(1, 10));
    assertThat(page.getRows()).hasSize(1);
    assertThat(page.isFirstPage()).isTrue();
    assertThat(page.isLastPage()).isTrue();
    assertThat(page.hasNextPage()).isFalse();
    assertThat(page.hasPrevPage()).isFalse();
    assertThat(page.getPageNumber()).isEqualTo(1);
    assertThat(page.getLimit()).isEqualTo(10);
    assertThat(page.getNextPage()).isEqualTo(1);
    assertThat(page.getPrevPage()).isEqualTo(1);
    assertThat(page.getTotalPages()).isEqualTo(1);
    assertThat(page.getTotalRows()).isEqualTo(1L);

    //
    page = entityManager.page(UserModel.class, Pageable.of(1, 10));
    assertThat(page.getRows()).hasSize(10);
    assertThat(page.isFirstPage()).isTrue();
    assertThat(page.isLastPage()).isFalse();
    assertThat(page.hasNextPage()).isTrue();
    assertThat(page.hasPrevPage()).isFalse();
    assertThat(page.getPageNumber()).isEqualTo(1);
    assertThat(page.getLimit()).isEqualTo(10);
    assertThat(page.getNextPage()).isEqualTo(2);
    assertThat(page.getPrevPage()).isEqualTo(1);
    assertThat(page.getTotalPages()).isEqualTo(11);
    assertThat(page.getTotalRows()).isEqualTo(101L);
    assertThat(page.getRows().get(0)).isEqualTo(userModel);

  }

  // update

  @ParameterizedRepositoryManagerTest
  void update(DbType dbType, RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);
    createData(entityManager);

    UserModel userModel = entityManager.findById(UserModel.class, 1);
    assertThat(userModel).isNotNull();

    String name = "TEST-UPDATE";
    userModel.setName(name);

    assertThat(entityManager.update(new UserName(1, name))).isEqualTo(1);

    UserModel model = entityManager.findById(UserModel.class, 1);
    assertThat(model).isNotNull();
    assertThat(model.getName()).isEqualTo(name);
    assertThat(userModel.getAge()).isEqualTo(model.getAge());
    assertThat(userModel.getAvatar()).isEqualTo(model.getAvatar());
    assertThat(userModel.getGender()).isEqualTo(model.getGender());

    assertThat(entityManager.update(new UserAge(name, 10))).isEqualTo(1);

    model = entityManager.findUnique(UserModel.class, new UserName(null, name));

    assertThat(model).isNotNull();
    assertThat(model.getName()).isEqualTo(name);
    assertThat(model.getAge()).isEqualTo(10);

    // throw

    assertThatThrownBy(() -> entityManager.update(new UserFailed()))
            .isInstanceOf(InvalidDataAccessApiUsageException.class)
            .hasMessage("Updating an entity, There is no update properties");

    assertThatThrownBy(() -> entityManager.update(new UserFailed(), PropertyUpdateStrategy.always()))
            .isInstanceOf(InvalidDataAccessApiUsageException.class)
            .hasMessage("Updating an entity, There is no update by properties");
  }

  @ParameterizedRepositoryManagerTest
  void truncate(DbType dbType, RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);
    if (dbType == DbType.HyperSQL) {
      entityManager.setPlatform(new HyperSQLPlatform());
    }

    createData(entityManager);

    assertThat(entityManager.count(UserModel.class).intValue()).isEqualTo(11);
    assertThat(entityManager.findById(UserModel.class, 1)).isNotNull();

    entityManager.truncate(UserModel.class);

    assertThat(entityManager.count(UserModel.class).intValue()).isEqualTo(0);
    assertThat(entityManager.findById(UserModel.class, 1)).isNull();
  }

  @ParameterizedRepositoryManagerTest
  void addConditionPropertyExtractor(DbType dbType, RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);
    repositoryManager.createQuery("drop table if exists t_option").executeUpdate();
    try (Query query = repositoryManager.createQuery("""
            create table t_option (
                `name`  varchar(255) default null,
                `value` varchar(255) default null
            );
            """)) {

      query.executeUpdate();
    }

    assertThatThrownBy(() -> entityManager.addConditionPropertyExtractor(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("ConditionPropertyExtractor is required");

    entityManager.addConditionPropertyExtractor(new Base64ValueExtractor());
    repositoryManager.getTypeHandlerManager().register(new Base64ValueHandler());

    Option entity = Option.of("k", "v");
    entityManager.persist(entity);

    Option unique = entityManager.findUnique(Option.of("k", "v"));
    assertThat(unique).isEqualTo(entity);

    assertThat(entityManager.findUnique(Option.of("k", null))).isEqualTo(entity);
    assertThat(entityManager.findUnique(Option.of("k1", null))).isNull();

    assertThatThrownBy(() -> entityManager.find(OptionError.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Not writable entity property its value is required");
  }

  @ParameterizedRepositoryManagerTest
  void setConditionPropertyExtractors(DbType dbType, RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);
    repositoryManager.createQuery("drop table if exists t_option").executeUpdate();
    try (NamedQuery query = repositoryManager.createNamedQuery("""
            create table t_option (
                `name`  varchar(255) default null,
                `value` varchar(255) default null
            );
            """)) {

      query.executeUpdate();
    }

    repositoryManager.getTypeHandlerManager().register(new Base64ValueHandler());

    Option entity = Option.of("k", "v");
    entityManager.persist(entity);

    assertThat(entityManager.findUnique(Option.of("k", null))).isNull();

    entityManager.setConditionPropertyExtractors(List.of(new Base64ValueExtractor()));

    assertThat(entityManager.findUnique(Option.of("k", null))).isEqualTo(entity);
    assertThat(entityManager.findUnique(Option.of("k1", null))).isNull();
  }

  @ParameterizedRepositoryManagerTest
  void saveOrUpdate(DbType dbType, RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);

    UserModel model = UserModel.male("TODAY", 2);
    entityManager.saveOrUpdate(model);

    List<UserModel> userModels = entityManager.find(model);
    assertThat(userModels).contains(model).hasSize(1);
  }

  @Test
  void shouldCreateEntityManagerWithRepositoryManager() {
    RepositoryManager repositoryManager = mock(RepositoryManager.class);
    DataSource dataSource = mock(DataSource.class);
    when(repositoryManager.getDataSource()).thenReturn(dataSource);

    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);

    assertThat(entityManager).isNotNull();
  }

  @Test
  void shouldSetPlatform() {
    RepositoryManager repositoryManager = mock(RepositoryManager.class);
    DataSource dataSource = mock(DataSource.class);
    when(repositoryManager.getDataSource()).thenReturn(dataSource);

    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);

    Platform customPlatform = mock(Platform.class);
    entityManager.setPlatform(customPlatform);

    // Should accept non-null platform
    assertThatCode(() -> entityManager.setPlatform(customPlatform)).doesNotThrowAnyException();

    // Should handle null platform by using default
    assertThatCode(() -> entityManager.setPlatform(null)).doesNotThrowAnyException();
  }

  @Test
  void shouldSetDefaultUpdateStrategy() {
    RepositoryManager repositoryManager = mock(RepositoryManager.class);
    DataSource dataSource = mock(DataSource.class);
    when(repositoryManager.getDataSource()).thenReturn(dataSource);

    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);

    PropertyUpdateStrategy strategy = PropertyUpdateStrategy.always();
    entityManager.setDefaultUpdateStrategy(strategy);

    // Should accept non-null strategy
    assertThatCode(() -> entityManager.setDefaultUpdateStrategy(strategy)).doesNotThrowAnyException();

    // Should reject null strategy
    assertThatThrownBy(() -> entityManager.setDefaultUpdateStrategy(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("defaultUpdateStrategy is required");
  }

  @Test
  void shouldSetDefaultPageable() {
    RepositoryManager repositoryManager = mock(RepositoryManager.class);
    DataSource dataSource = mock(DataSource.class);
    when(repositoryManager.getDataSource()).thenReturn(dataSource);

    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);

    Pageable pageable = Pageable.of(1, 10);
    entityManager.setDefaultPageable(pageable);

    // Should accept non-null pageable
    assertThatCode(() -> entityManager.setDefaultPageable(pageable)).doesNotThrowAnyException();

    // Should reject null pageable
    assertThatThrownBy(() -> entityManager.setDefaultPageable(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("defaultPageable is required");
  }

  @Test
  void shouldSetEntityMetadataFactory() {
    RepositoryManager repositoryManager = mock(RepositoryManager.class);
    DataSource dataSource = mock(DataSource.class);
    when(repositoryManager.getDataSource()).thenReturn(dataSource);

    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);

    EntityMetadataFactory factory = mock(EntityMetadataFactory.class);
    entityManager.setEntityMetadataFactory(factory);

    // Should accept non-null factory
    assertThatCode(() -> entityManager.setEntityMetadataFactory(factory)).doesNotThrowAnyException();

    // Should reject null factory
    assertThatThrownBy(() -> entityManager.setEntityMetadataFactory(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("EntityMetadataFactory is required");
  }

  @Test
  void shouldSetAutoGenerateId() {
    RepositoryManager repositoryManager = mock(RepositoryManager.class);
    DataSource dataSource = mock(DataSource.class);
    when(repositoryManager.getDataSource()).thenReturn(dataSource);

    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);

    // Should accept both true and false values
    assertThatCode(() -> entityManager.setAutoGenerateId(true)).doesNotThrowAnyException();
    assertThatCode(() -> entityManager.setAutoGenerateId(false)).doesNotThrowAnyException();
  }

  @Test
  void shouldSetMaxBatchRecords() {
    RepositoryManager repositoryManager = mock(RepositoryManager.class);
    DataSource dataSource = mock(DataSource.class);
    when(repositoryManager.getDataSource()).thenReturn(dataSource);

    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);

    // Should accept non-negative values
    assertThatCode(() -> entityManager.setMaxBatchRecords(0)).doesNotThrowAnyException();
    assertThatCode(() -> entityManager.setMaxBatchRecords(100)).doesNotThrowAnyException();

    // Should reject negative values
    assertThatThrownBy(() -> entityManager.setMaxBatchRecords(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("maxBatchRecords should be a non-negative value");
  }

  @Test
  void shouldGetMaxBatchRecords() {
    RepositoryManager repositoryManager = mock(RepositoryManager.class);
    DataSource dataSource = mock(DataSource.class);
    when(repositoryManager.getDataSource()).thenReturn(dataSource);

    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);

    entityManager.setMaxBatchRecords(50);
    assertThat(entityManager.getMaxBatchRecords()).isEqualTo(50);

    entityManager.setMaxBatchRecords(0);
    assertThat(entityManager.getMaxBatchRecords()).isEqualTo(0);
  }

  @Test
  void shouldAddBatchPersistListeners() {
    RepositoryManager repositoryManager = mock(RepositoryManager.class);
    DataSource dataSource = mock(DataSource.class);
    when(repositoryManager.getDataSource()).thenReturn(dataSource);

    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);

    BatchPersistListener listener1 = mock(BatchPersistListener.class);
    BatchPersistListener listener2 = mock(BatchPersistListener.class);

    // Should accept varargs listeners
    assertThatCode(() -> entityManager.addBatchPersistListeners(listener1, listener2)).doesNotThrowAnyException();

    // Should accept single listener
    assertThatCode(() -> entityManager.addBatchPersistListeners(listener1)).doesNotThrowAnyException();
    assertThatCode(() -> entityManager.addBatchPersistListeners(List.of(listener1))).doesNotThrowAnyException();
  }

  @Test
  void shouldSetBatchPersistListeners() {
    RepositoryManager repositoryManager = mock(RepositoryManager.class);
    DataSource dataSource = mock(DataSource.class);
    when(repositoryManager.getDataSource()).thenReturn(dataSource);

    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);

    List<BatchPersistListener> listeners = new ArrayList<>();
    listeners.add(mock(BatchPersistListener.class));
    listeners.add(mock(BatchPersistListener.class));

    // Should accept non-null collection
    assertThatCode(() -> entityManager.setBatchPersistListeners(listeners)).doesNotThrowAnyException();

    // Should accept null (clears listeners)
    assertThatCode(() -> entityManager.setBatchPersistListeners(null)).doesNotThrowAnyException();
  }

  @Test
  void shouldSetStatementLogger() {
    RepositoryManager repositoryManager = mock(RepositoryManager.class);
    DataSource dataSource = mock(DataSource.class);
    when(repositoryManager.getDataSource()).thenReturn(dataSource);

    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);

    SqlStatementLogger logger = mock(SqlStatementLogger.class);
    entityManager.setStatementLogger(logger);

    // Should accept non-null logger
    assertThatCode(() -> entityManager.setStatementLogger(logger)).doesNotThrowAnyException();

    // Should reject null logger
    assertThatThrownBy(() -> entityManager.setStatementLogger(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("SqlStatementLogger is required");
  }

  @Test
  void shouldSetTransactionConfig() {
    RepositoryManager repositoryManager = mock(RepositoryManager.class);
    DataSource dataSource = mock(DataSource.class);
    when(repositoryManager.getDataSource()).thenReturn(dataSource);

    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);

    TransactionDefinition definition = mock(TransactionDefinition.class);

    // Should accept non-null definition
    assertThatCode(() -> entityManager.setTransactionConfig(definition)).doesNotThrowAnyException();

    // Should accept null (defaults)
    assertThatCode(() -> entityManager.setTransactionConfig(null)).doesNotThrowAnyException();
  }

  @Test
  void shouldAddConditionPropertyExtractor() {
    RepositoryManager repositoryManager = mock(RepositoryManager.class);
    DataSource dataSource = mock(DataSource.class);
    when(repositoryManager.getDataSource()).thenReturn(dataSource);

    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);

    ConditionPropertyExtractor extractor = mock(ConditionPropertyExtractor.class);

    // Should accept non-null extractor
    assertThatCode(() -> entityManager.addConditionPropertyExtractor(extractor)).doesNotThrowAnyException();

    // Should reject null extractor
    assertThatThrownBy(() -> entityManager.addConditionPropertyExtractor(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("ConditionPropertyExtractor is required");
  }

  @Test
  void shouldSetConditionPropertyExtractors() {
    RepositoryManager repositoryManager = mock(RepositoryManager.class);
    DataSource dataSource = mock(DataSource.class);
    when(repositoryManager.getDataSource()).thenReturn(dataSource);

    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);

    List<ConditionPropertyExtractor> extractors = new ArrayList<>();
    extractors.add(mock(ConditionPropertyExtractor.class));
    extractors.add(mock(ConditionPropertyExtractor.class));

    // Should accept non-null list
    assertThatCode(() -> entityManager.setConditionPropertyExtractors(extractors)).doesNotThrowAnyException();

    // Should accept null (clears extractors)
    assertThatCode(() -> entityManager.setConditionPropertyExtractors(null)).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleDefaultPageable() {
    RepositoryManager repositoryManager = mock(RepositoryManager.class);
    DataSource dataSource = mock(DataSource.class);
    when(repositoryManager.getDataSource()).thenReturn(dataSource);

    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);

    Pageable defaultPageable = entityManager.defaultPageable();
    assertThat(defaultPageable).isNotNull();
  }

  @Test
  void shouldHandleDefaultUpdateStrategy() {
    RepositoryManager repositoryManager = mock(RepositoryManager.class);
    DataSource dataSource = mock(DataSource.class);
    when(repositoryManager.getDataSource()).thenReturn(dataSource);

    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);

    Object entity = new Object();
    PropertyUpdateStrategy strategy = entityManager.defaultUpdateStrategy(entity);
    assertThat(strategy).isNotNull();

    // Test with UpdateStrategySource
    UpdateStrategySource source = () -> PropertyUpdateStrategy.always();
    PropertyUpdateStrategy sourceStrategy = entityManager.defaultUpdateStrategy(source);
    assertThat(sourceStrategy).isEqualTo(PropertyUpdateStrategy.always());

    // Test with PropertyUpdateStrategy
    PropertyUpdateStrategy directStrategy = PropertyUpdateStrategy.always();
    PropertyUpdateStrategy resultStrategy = entityManager.defaultUpdateStrategy(directStrategy);
    assertThat(resultStrategy).isEqualTo(directStrategy);
  }

  @Test
  void shouldAssertUpdateCountSuccessfully() {
    String sql = "UPDATE test SET col = ?";

    // Should not throw exception when actual count matches expected count
    assertThatCode(() -> DefaultEntityManager.assertUpdateCount(sql, 5, 5)).doesNotThrowAnyException();
  }

  @Test
  void shouldAssertUpdateCountFailure() {
    String sql = "UPDATE test SET col = ?";

    // Should throw exception when actual count doesn't match expected count
    assertThatThrownBy(() -> DefaultEntityManager.assertUpdateCount(sql, 3, 5))
            .isInstanceOf(JdbcUpdateAffectedIncorrectNumberOfRowsException.class);
  }

  @Test
  void shouldPrepareStatementWithAutoGenerateId() throws SQLException {
    Connection connection = mock(Connection.class);
    String sql = "INSERT INTO test VALUES (?)";
    PreparedStatement statement = mock(PreparedStatement.class);

    when(connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)).thenReturn(statement);

    RepositoryManager repositoryManager = mock(RepositoryManager.class);
    DataSource dataSource = mock(DataSource.class);
    when(repositoryManager.getDataSource()).thenReturn(dataSource);

    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);

    PreparedStatement result = entityManager.prepareStatement(connection, sql, true);
    assertThat(result).isEqualTo(statement);
    verify(connection).prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
  }

  @Test
  void shouldPrepareStatementWithoutAutoGenerateId() throws SQLException {
    Connection connection = mock(Connection.class);
    String sql = "INSERT INTO test VALUES (?)";
    PreparedStatement statement = mock(PreparedStatement.class);

    when(connection.prepareStatement(sql)).thenReturn(statement);

    RepositoryManager repositoryManager = mock(RepositoryManager.class);
    DataSource dataSource = mock(DataSource.class);
    when(repositoryManager.getDataSource()).thenReturn(dataSource);

    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);

    PreparedStatement result = entityManager.prepareStatement(connection, sql, false);
    assertThat(result).isEqualTo(statement);
    verify(connection).prepareStatement(sql);
  }

  @Test
  void shouldHandleEntityPropertySetValue() throws SQLException {
    // Create a simple test entity
    class TestEntity {
      String name;
    }

    TestEntity entity = new TestEntity();
    entity.name = "test";

    // Mock BeanProperty
    BeanProperty beanProperty = mock(BeanProperty.class);
    when(beanProperty.getValue(entity)).thenReturn("testValue");

    // Mock TypeHandler
    TypeHandler<Object> typeHandler = mock(TypeHandler.class);

    // Create EntityProperty
    EntityProperty entityProperty = new EntityProperty(beanProperty, "test_column", typeHandler, false);

    // Mock PreparedStatement
    PreparedStatement ps = mock(PreparedStatement.class);

    // Test setTo method
    assertThatCode(() -> entityProperty.setTo(ps, 1, entity)).doesNotThrowAnyException();
    verify(beanProperty).getValue(entity);
    verify(typeHandler).setParameter(ps, 1, "testValue");

    // Test setParameter method
    assertThatCode(() -> entityProperty.setParameter(ps, 2, "directValue")).doesNotThrowAnyException();
    verify(typeHandler).setParameter(ps, 2, "directValue");
  }

  @Test
  void shouldHandleEntityPropertyGetValue() {
    // Create a simple test entity
    class TestEntity {
      String name = "testName";
    }

    TestEntity entity = new TestEntity();

    // Mock BeanProperty
    BeanProperty beanProperty = mock(BeanProperty.class);
    when(beanProperty.getValue(entity)).thenReturn("testName");

    // Mock TypeHandler
    @SuppressWarnings("unchecked")
    TypeHandler<Object> typeHandler = mock(TypeHandler.class);

    // Create EntityProperty
    EntityProperty entityProperty = new EntityProperty(beanProperty, "test_column", typeHandler, false);

    // Test getValue method
    Object value = entityProperty.getValue(entity);
    assertThat(value).isEqualTo("testName");
    verify(beanProperty).getValue(entity);
  }

  @Test
  void shouldHandleEntityPropertySetValueToEntity() throws SQLException {
    // Create a simple test entity
    class TestEntity {
      String name;
    }

    TestEntity entity = new TestEntity();

    // Mock BeanProperty
    BeanProperty beanProperty = mock(BeanProperty.class);

    // Mock TypeHandler
    @SuppressWarnings("unchecked")
    TypeHandler<Object> typeHandler = mock(TypeHandler.class);
    when(typeHandler.getResult(any(ResultSet.class), anyInt())).thenReturn("resultValue");

    // Create EntityProperty
    EntityProperty entityProperty = new EntityProperty(beanProperty, "test_column", typeHandler, false);

    // Mock ResultSet
    ResultSet rs = mock(ResultSet.class);

    // Test setProperty method
    assertThatCode(() -> entityProperty.setProperty(entity, rs, 1)).doesNotThrowAnyException();
    verify(typeHandler).getResult(rs, 1);
    verify(beanProperty).setDirectly(entity, "resultValue");
  }

  @Test
  void shouldHandleEntityPropertyAnnotations() {
    // Mock BeanProperty
    BeanProperty beanProperty = mock(BeanProperty.class);
    MergedAnnotations mergedAnnotations = mock(MergedAnnotations.class);
    when(beanProperty.mergedAnnotations()).thenReturn(mergedAnnotations);

    // Mock TypeHandler
    @SuppressWarnings("unchecked")
    TypeHandler<Object> typeHandler = mock(TypeHandler.class);

    // Create EntityProperty
    EntityProperty entityProperty = new EntityProperty(beanProperty, "test_column", typeHandler, false);

    // Test getAnnotations method
    MergedAnnotations result = entityProperty.getAnnotations();
    assertThat(result).isEqualTo(mergedAnnotations);

    // Test getAnnotation method
    Class<Id> annotationType = Id.class;
    MergedAnnotation<Id> mergedAnnotation = mock(MergedAnnotation.class);
    when(mergedAnnotations.get(annotationType)).thenReturn(mergedAnnotation);

    MergedAnnotation<Id> annotationResult = entityProperty.getAnnotation(annotationType);
    assertThat(annotationResult).isEqualTo(mergedAnnotation);

    // Test isPresent method
    when(mergedAnnotations.isPresent(annotationType)).thenReturn(true);
    boolean present = entityProperty.isPresent(annotationType);
    assertThat(present).isTrue();
  }

  @Test
  void shouldHandleEntityPropertyWithIdProperty() {
    // Mock BeanProperty
    BeanProperty beanProperty = mock(BeanProperty.class);
    when(beanProperty.getName()).thenReturn("id");

    // Mock TypeHandler
    @SuppressWarnings("unchecked")
    TypeHandler<Object> typeHandler = mock(TypeHandler.class);

    // Create EntityProperty with isIdProperty = true
    EntityProperty entityProperty = new EntityProperty(beanProperty, "id_column", typeHandler, true);

    assertThat(entityProperty.isIdProperty).isTrue();
    assertThat(entityProperty.columnName).isEqualTo("id_column");
    assertThat(entityProperty.property).isEqualTo(beanProperty);
  }

  @Test
  void shouldHandleEntityPropertyWithoutIdProperty() {
    // Mock BeanProperty
    BeanProperty beanProperty = mock(BeanProperty.class);
    when(beanProperty.getName()).thenReturn("name");

    // Mock TypeHandler
    @SuppressWarnings("unchecked")
    TypeHandler<Object> typeHandler = mock(TypeHandler.class);

    // Create EntityProperty with isIdProperty = false
    EntityProperty entityProperty = new EntityProperty(beanProperty, "name_column", typeHandler, false);

    assertThat(entityProperty.isIdProperty).isFalse();
    assertThat(entityProperty.columnName).isEqualTo("name_column");
    assertThat(entityProperty.property).isEqualTo(beanProperty);
  }

  @Test
  void shouldHandleIsNewWithNewEntityIndicator() {
    RepositoryManager repositoryManager = mock(RepositoryManager.class);
    DataSource dataSource = mock(DataSource.class);
    when(repositoryManager.getDataSource()).thenReturn(dataSource);

    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);

    // Mock NewEntityIndicator
    NewEntityIndicator entity = mock(NewEntityIndicator.class);
    when(entity.isNew()).thenReturn(true);

    assertThat(entityManager.isNew(entity)).isTrue();

    when(entity.isNew()).thenReturn(false);
    assertThat(entityManager.isNew(entity)).isFalse();
  }

  @Test
  void shouldSetParametersCorrectly() throws SQLException {
    Object entity = new Object();

    // Mock EntityProperties
    EntityProperty prop1 = mock(EntityProperty.class);
    EntityProperty prop2 = mock(EntityProperty.class);

    ArrayList<EntityProperty> properties = new ArrayList<>();
    properties.add(prop1);
    properties.add(prop2);

    PreparedStatement statement = mock(PreparedStatement.class);

    DefaultEntityManager.setParameters(entity, properties, statement);

    verify(prop1).setTo(statement, 1, entity);
    verify(prop2).setTo(statement, 2, entity);
  }

  @Test
  void shouldGetDescriptionFromDescriptiveObject() {
    Descriptive descriptive = mock(Descriptive.class);
    when(descriptive.getDescription()).thenReturn("Test description");

    RepositoryManager repositoryManager = mock(RepositoryManager.class);
    DataSource dataSource = mock(DataSource.class);
    when(repositoryManager.getDataSource()).thenReturn(dataSource);

    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);

    String description = entityManager.getDescription(descriptive);

    assertThat(description).isEqualTo("Test description");
  }

  @Test
  void shouldGetDescriptionFromNonDescriptiveObject() {
    Object nonDescriptive = new Object();

    RepositoryManager repositoryManager = mock(RepositoryManager.class);
    DataSource dataSource = mock(DataSource.class);
    when(repositoryManager.getDataSource()).thenReturn(dataSource);

    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);

    String description = entityManager.getDescription(nonDescriptive);

    assertThat(description).isEqualTo("Query entities without conditions");
  }

  @Test
  void shouldGetDebugLogMessageFromDebugDescriptive() {
    DebugDescriptive debugDescriptive = mock(DebugDescriptive.class);
    Object logMessage = new Object();
    when(debugDescriptive.getDebugLogMessage()).thenReturn(logMessage);

    RepositoryManager repositoryManager = mock(RepositoryManager.class);
    DataSource dataSource = mock(DataSource.class);
    when(repositoryManager.getDataSource()).thenReturn(dataSource);

    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);

    Object result = entityManager.getDebugLogMessage(debugDescriptive);

    assertThat(result).isEqualTo(logMessage);
  }

  @Test
  void shouldGetDebugLogMessageFromDescriptive() {
    Descriptive descriptive = mock(Descriptive.class);
    when(descriptive.getDescription()).thenReturn("Test description");

    RepositoryManager repositoryManager = mock(RepositoryManager.class);
    DataSource dataSource = mock(DataSource.class);
    when(repositoryManager.getDataSource()).thenReturn(dataSource);

    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);

    Object result = entityManager.getDebugLogMessage(descriptive);

    assertThat(result.toString()).contains("Test description");
  }

  @Test
  void shouldGetDebugLogMessageFromNonDescriptive() {
    Object nonDescriptive = new Object();

    RepositoryManager repositoryManager = mock(RepositoryManager.class);
    DataSource dataSource = mock(DataSource.class);
    when(repositoryManager.getDataSource()).thenReturn(dataSource);

    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);

    Object result = entityManager.getDebugLogMessage(nonDescriptive);

    assertThat(result.toString()).contains("Query entities without conditions");
  }

  public static void createData(DefaultEntityManager entityManager) {
    UserModel userModel = UserModel.male("TODAY", 9);

    List<Object> entities = new ArrayList<>();
    entities.add(userModel);

    for (int i = 0; i < 10; i++) {
      entities.add(UserModel.male("TODAY", 10 + i));
    }

    entityManager.persist(entities);
  }

  @EntityRef(UserModel.class)
  static class UserName implements UpdateStrategySource {

    @Nullable
    final Integer id;

    final String name;

    UserName(@Nullable Integer id, String name) {
      this.id = id;
      this.name = name;
    }

    @Override
    public PropertyUpdateStrategy updateStrategy() {
      return PropertyUpdateStrategy.noneNull();
    }

  }

  @EntityRef(UserModel.class)
  static class UserAge implements PropertyUpdateStrategy {

    @UpdateBy
    final String name;

    final int age;

    UserAge(String name, int age) {
      this.name = name;
      this.age = age;
    }

    @Override
    public boolean shouldUpdate(Object entity, EntityProperty property) {
      return noneNull().shouldUpdate(entity, property);
    }
  }

  @EntityRef(UserModel.class)
  static class UserFailed {
    String name;
  }

  @Table("t_option")
  static class Option {

    public final Base64Value name = new Base64Value();

    public final Base64Value value = new Base64Value();

    static Option of(@Nullable String name, @Nullable String value) {
      Option option = new Option();
      option.name.set(name);
      option.value.set(value);
      return option;
    }

    @Override
    public boolean equals(Object param) {
      if (this == param)
        return true;
      if (!(param instanceof Option option))
        return false;
      return Objects.equals(name, option.name)
              && Objects.equals(value, option.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, value);
    }

  }

  @Table("t_option")
  static class OptionError {

    @Nullable
    public final Base64Value value = null;

  }

  static class Base64Value implements Serializable {

    @Nullable
    private String value;

    Base64Value() {

    }

    Base64Value(@Nullable String value) {
      this.value = value;
    }

    public void set(@Nullable String value) {
      this.value = value;
    }

    @Nullable
    public String get() {
      return value;
    }

    @Override
    public boolean equals(Object param) {
      if (this == param)
        return true;
      if (!(param instanceof Base64Value that))
        return false;
      return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(value);
    }

  }

  static class Base64ValueExtractor implements ConditionPropertyExtractor<String> {

    @Nullable
    @Override
    public Object extract(Object entityOrExample, EntityProperty property, @Nullable Object value) {
      if (value instanceof Base64Value base64Value) {
        return base64Value.value;
      }
      return value;
    }

    @Override
    public Object wrap(String extracted) {
      return new Base64Value(extracted);
    }

  }

  static class Base64ValueHandler extends BasicTypeHandler<Base64Value>
          implements SmartTypeHandler<Base64Value>, WrappedTypeHandler<Base64Value> {

    @Override
    public boolean supportsProperty(BeanProperty property) {
      return property.getTypeDescriptor().is(Base64Value.class);
    }

    @Override
    public void applyResult(Base64Value value, ResultSet rs, int columnIndex) throws SQLException {
      value.set(rs.getString(columnIndex));
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int parameterIndex, Base64Value arg) throws SQLException {
      ps.setString(parameterIndex, arg.value);
    }

  }
}


