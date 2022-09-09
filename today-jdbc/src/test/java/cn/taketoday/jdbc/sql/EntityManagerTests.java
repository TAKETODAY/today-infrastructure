/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.jdbc.sql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.jdbc.Query;
import cn.taketoday.jdbc.RepositoryManager;
import cn.taketoday.jdbc.sql.model.Gender;
import cn.taketoday.jdbc.sql.model.UserModel;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;

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
    // language=MySQL
    try (Query query = repositoryManager.createQuery("""
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
    EntityManager entityManager = new EntityManager(repositoryManager);

    assertThatThrownBy(() ->
            entityManager.persist(new Object()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageStartingWith("Cannot determine ID property");

  }

  @ParameterizedRepositoryManagerTest
  void persist(RepositoryManager repositoryManager) {
    EntityManager entityManager = new EntityManager(repositoryManager);

    UserModel userModel = new UserModel();
    userModel.name = "TODAY";
    userModel.gender = Gender.MALE;
    userModel.age = 10;

    entityManager.persist(userModel, true);

    assertThat(userModel.id).isNotNull();

    // language=MySQL
    try (Query query = repositoryManager.createQuery("SELECT * from t_user where id=:id")) {
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
    EntityManager entityManager = new EntityManager(repositoryManager);
    entityManager.setMaxBatchRecords(10);

    UserModel userModel = UserModel.male("TODAY", 9);

    List<Object> entities = new ArrayList<>();
    entities.add(userModel);

    for (int i = 0; i < 10; i++) {
      entities.add(UserModel.male("TODAY", 10 + i));
    }

    entityManager.persist(entities);

    // language=MySQL
    try (Query query = repositoryManager.createQuery("SELECT * from t_user")) {
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
    @Nullable
    Integer userId;

    @Nullable
    @QueryCondition(expression = "#root.name != null", render = "like %#{name}%")
    String name;

    @Nullable
    @QueryCondition(expression = "#root.name != null", render = "=#{age}")
    Integer age;

    @QueryCondition(expression = "#root.name != null", render = "> #{birthdayBegin}")
    LocalDate birthdayBegin;

    @QueryCondition(expression = "#root.birthdayEnd != null", render = "< #{birthdayEnd}")
    LocalDate birthdayEnd;

  }

  @ParameterizedRepositoryManagerTest
  void findByQuery(RepositoryManager repositoryManager) {
    EntityManager entityManager = new EntityManager(repositoryManager);
    UserForm userForm = new UserForm();
    userForm.age = 10;

    UserModel userModel = UserModel.male("TODAY", 9);

    List<Object> entities = new ArrayList<>();
    entities.add(userModel);

    for (int i = 0; i < 10; i++) {
      entities.add(UserModel.male("TODAY", 10 + i));
    }

    entityManager.persist(entities);

    List<UserModel> list = entityManager.find(UserModel.class, userForm);

    System.out.println(list);

    userForm = new UserForm();
    userForm.name = "TODAY";

    list = entityManager.find(UserModel.class, userForm);
    System.out.println(list);

    assertThat(list).hasSize(entities.size()).isEqualTo(entities);

    entityManager.iterate(UserModel.class, userForm, System.out::println);
  }

}