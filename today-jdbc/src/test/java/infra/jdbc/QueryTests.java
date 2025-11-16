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

package infra.jdbc;

import java.util.ArrayList;
import java.util.List;

import infra.persistence.AbstractRepositoryManagerTests;
import infra.persistence.DefaultEntityManager;
import infra.persistence.model.UserModel;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/1/20 22:36
 */
class QueryTests extends AbstractRepositoryManagerTests {

  @Override
  protected void prepareTestsData(DbType dbType, RepositoryManager repositoryManager) {
    repositoryManager.createQuery("drop table if exists t_user").executeUpdate();
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
  void create(DbType dbType, RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);
    createData(entityManager);

    try (JdbcConnection connection = repositoryManager.open()) {

      Query query = connection.createQuery("select * from t_user where `id`=?")
              .addParameter(1);

      query.setAutoDerivingColumns(true);

      assertThat(query.fetchFirst(UserModel.class))
              .isNotNull()
              .extracting("id").isEqualTo(1);
    }
  }

  @ParameterizedRepositoryManagerTest
  void addParameter(DbType dbType, RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);
    createData(entityManager);

    try (JdbcConnection connection = repositoryManager.open()) {
      Query query = connection.createQuery("select * from t_user where `id`=? and `name`=?")
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