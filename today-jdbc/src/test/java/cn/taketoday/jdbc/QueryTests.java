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

package cn.taketoday.jdbc;

import org.junit.jupiter.api.TestInstance;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.persistence.AbstractRepositoryManagerTests;
import cn.taketoday.persistence.DefaultEntityManager;
import cn.taketoday.persistence.model.UserModel;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/1/20 22:36
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QueryTests extends AbstractRepositoryManagerTests {

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
  void create(RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);
    createData(entityManager);

    try (JdbcConnection connection = repositoryManager.open()) {

      Query query = connection.createQuery("select * from t_user where id=?")
              .addParameter(1);

      query.setAutoDerivingColumns(true);

      assertThat(query.fetchFirst(UserModel.class))
              .isNotNull()
              .extracting("id").isEqualTo(1);
    }
  }

  @ParameterizedRepositoryManagerTest
  void addParameter(RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);
    createData(entityManager);

    try (JdbcConnection connection = repositoryManager.open()) {
      Query query = connection.createQuery("select * from t_user where id=? and name=?")
              .addParameter(1)
              .addParameter("TODAY");

      query.setAutoDerivingColumns(true);

      assertThat(query.fetchFirst(UserModel.class))
              .isNotNull()
              .extracting("id", "name")
              .containsExactly(1, "TODAY");

    }
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