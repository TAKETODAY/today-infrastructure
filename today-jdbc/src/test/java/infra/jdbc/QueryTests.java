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