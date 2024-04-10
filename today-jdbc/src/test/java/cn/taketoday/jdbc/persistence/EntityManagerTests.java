/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.jdbc.persistence;

import org.junit.jupiter.api.TestInstance;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.taketoday.dao.IncorrectResultSizeDataAccessException;
import cn.taketoday.dao.InvalidDataAccessApiUsageException;
import cn.taketoday.jdbc.NamedQuery;
import cn.taketoday.jdbc.RepositoryManager;
import cn.taketoday.jdbc.persistence.model.Gender;
import cn.taketoday.jdbc.persistence.model.NoIdModel;
import cn.taketoday.jdbc.persistence.model.UserModel;
import cn.taketoday.lang.Nullable;
import cn.taketoday.test.util.ReflectionTestUtils;
import cn.taketoday.util.CollectionUtils;

import static cn.taketoday.jdbc.persistence.QueryCondition.between;
import static cn.taketoday.jdbc.persistence.QueryCondition.isEqualsTo;
import static cn.taketoday.jdbc.persistence.QueryCondition.isNotNull;
import static cn.taketoday.jdbc.persistence.QueryCondition.nested;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/16 22:48
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EntityManagerTests extends AbstractRepositoryManagerTests {

  @Override
  protected void prepareTestsData(DbType dbType, RepositoryManager repositoryManager) {
    try (NamedQuery query = repositoryManager.createNamedQuery("""
            drop table if exists t_user;
            create table t_user
            (
                `id`               int auto_increment primary key,
                `age`              int           default 0    comment 'Age',
                `name`             varchar(255)  default null comment '用户名',
                `avatar`           mediumtext    default null comment '头像',
                `password`         varchar(255)  default null comment '密码',
                `introduce`        varchar(1000) default null comment '介绍',
                `email`            varchar(255)  default null comment 'email',
                `gender`           int           default -1   comment '性别',
                `mobile_phone`     varchar(36)   default null comment '手机号'
            );
            """)) {

      query.executeUpdate();
    }

  }

  @ParameterizedRepositoryManagerTest
  void exception(RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);

    assertThatThrownBy(() ->
            entityManager.persist(new Object()))
            .isInstanceOf(IllegalEntityException.class)
            .hasMessageStartingWith("Cannot determine properties");

  }

  @ParameterizedRepositoryManagerTest
  void persist(RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);

    UserModel userModel = new UserModel();
    userModel.name = "TODAY";
    userModel.gender = Gender.MALE;
    userModel.age = 10;

    entityManager.persist(userModel, true);

    assertThat(userModel.id).isNotNull();

    try (NamedQuery query = repositoryManager.createNamedQuery("SELECT * from t_user where id=:id")) {
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
  void batchPersist(RepositoryManager repositoryManager) {
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

    String name;

    @Nullable
    Integer age;

    @Where("birthday >= ?")
    LocalDate birthdayBegin;

    @Where("birthday <= ?")
    LocalDate birthdayEnd;

  }

  @ParameterizedRepositoryManagerTest
  void findByExample(RepositoryManager repositoryManager) {
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
  void iterateListOfQueryConditions(RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);
    createData(entityManager);

//    QueryCondition condition =
//            QueryCondition.equalsTo("age", 10)
//                    .and(QueryCondition.of("name", ConditionOperator.LIKE, "T"));

//    QueryCondition condition = QueryCondition.of("name", Operator.SUFFIX_LIKE, "T");
    // entityManager.iterate(UserModel.class, condition, System.out::println);
  }

  @ParameterizedRepositoryManagerTest
  void deleteById(RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);
    createData(entityManager);

    UserModel byId = entityManager.findById(UserModel.class, 1);
    assertThat(byId).isNotNull().extracting("id").isEqualTo(1);

    entityManager.delete(UserModel.class, 1);

    byId = entityManager.findById(UserModel.class, 1);
    assertThat(byId).isNull();
  }

  @ParameterizedRepositoryManagerTest
  void deleteByEntity(RepositoryManager repositoryManager) {
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
  void findUnique(RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);
    createData(entityManager);

    assertThatThrownBy(() ->
            entityManager.findUnique(UserModel.class,
                    isNotNull("name")
                            .or(nested(between("age", 1, 20))))
    )
            .isInstanceOf(IncorrectResultSizeDataAccessException.class);

    UserModel unique = entityManager.findUnique(UserModel.class,
            isNotNull("name")
                    .and(nested(isEqualsTo("age", 9))));

    assertThat(unique).isNotNull()
            .extracting("id").isEqualTo(1);

    assertThat(unique).extracting("name").isEqualTo("TODAY");
    assertThat(unique).extracting("age").isEqualTo(9);

    int deleteRows = entityManager.delete(unique);
    assertThat(deleteRows).isEqualTo(1);

    // null
    unique = entityManager.findUnique(UserModel.class,
            isNotNull("name")
                    .and(nested(isEqualsTo("age", 9))));
    assertThat(unique).isNull();
  }

  @ParameterizedRepositoryManagerTest
  void updateBy(RepositoryManager repositoryManager) {
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
  void updateById(RepositoryManager repositoryManager) {
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
            .hasMessage("Updating an entity, ID property is required");

    assertThatThrownBy(() -> entityManager.updateById(userModel, "errorId"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Entity Id matches failed");

    assertThatThrownBy(() -> entityManager.updateById(new NoIdModel(), "errorId"))
            .isInstanceOf(IllegalEntityException.class)
            .hasMessage("ID property is required");

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
  void findMap(RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);
    createData(entityManager);

    UserModel example = new UserModel();
    example.setName("TODAY");
    assertThat(entityManager.find(UserModel.class, example, "age")).isNotEmpty().hasSize(11);

    assertThat(entityManager.find(UserModel.class, isEqualsTo("name", "TODAY"), "age"))
            .isEqualTo(entityManager.find(UserModel.class, example, "age"));
  }

  @ParameterizedRepositoryManagerTest
  void findMapWithMappingFunction(RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);
    createData(entityManager);

    UserModel example = new UserModel();
    example.setName("TODAY");
    assertThat(entityManager.find(UserModel.class, example, UserModel::getAge)).isNotEmpty().hasSize(11);

    assertThat(entityManager.find(UserModel.class, isEqualsTo("name", "TODAY"), UserModel::getAge))
            .isEqualTo(entityManager.find(example, UserModel::getAge));
  }

  @ParameterizedRepositoryManagerTest
  void findList(RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);
    createData(entityManager);

    UserModel example = new UserModel();
    example.setName("TODAY");
    assertThat(entityManager.find(UserModel.class, example)).isNotEmpty().hasSize(11);

    assertThat(entityManager.find(UserModel.class, isEqualsTo("name", "TODAY")))
            .isEqualTo(entityManager.find(UserModel.class, example))
            .isEqualTo(entityManager.find(example));
  }

  @ParameterizedRepositoryManagerTest
  void findByExampleMap(RepositoryManager repositoryManager) {
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
  void findSortBy(RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);
    createData(entityManager);

    assertThat(entityManager.find(UserModel.class, Map.of("age", Order.DESC)))
            .isEqualTo(entityManager.find(UserModel.class, Map.of("id", Order.DESC)));
  }

  @ParameterizedRepositoryManagerTest
  void batchPersistListener(RepositoryManager repositoryManager) {
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
  void count(RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);

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
  void page(RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);

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
    assertThat(page.isLastPage()).isFalse();
    assertThat(page.isHasNextPage()).isFalse();
    assertThat(page.isHasPrevPage()).isFalse();
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
    assertThat(page.isHasNextPage()).isTrue();
    assertThat(page.isHasPrevPage()).isFalse();
    assertThat(page.getPageNumber()).isEqualTo(1);
    assertThat(page.getLimit()).isEqualTo(10);
    assertThat(page.getNextPage()).isEqualTo(2);
    assertThat(page.getPrevPage()).isEqualTo(1);
    assertThat(page.getTotalPages()).isEqualTo(11);
    assertThat(page.getTotalRows()).isEqualTo(101L);
    assertThat(page.getRows().get(0)).isEqualTo(userModel);

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

}


