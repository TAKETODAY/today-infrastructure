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

package infra.jdbc.issues;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import javax.sql.DataSource;

import infra.jdbc.JdbcConnection;
import infra.jdbc.NamedQuery;
import infra.jdbc.RepositoryManager;
import infra.jdbc.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by lars on 05.10.2014.
 */
class H2Tests {

  DataSource ds;

  String driverClassName;
  String url;
  String user;
  String pass;

  @BeforeEach
  public void setUp() throws Exception {
    driverClassName = "org.h2.Driver";
    url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
    user = "sa";
    pass = "";
    org.h2.jdbcx.JdbcDataSource datasource = new org.h2.jdbcx.JdbcDataSource();
    datasource.setURL(url);
    datasource.setUser(user);
    datasource.setPassword(pass);

    ds = datasource;
  }

  @Test
  void testIssue155() {

    RepositoryManager sql2o = new RepositoryManager(ds);

    try (JdbcConnection connection = sql2o.open()) {
      int val = connection.createNamedQuery("select 42").scalar(Integer.class);

      assertThat(val).isEqualTo(42);
    }
  }

  /**
   * Ref issue #73
   */
  @Test
  void testUUID() {

    try (JdbcConnection connection = new RepositoryManager(ds).beginTransaction()) {
      connection.createNamedQuery("create table uuidtest(id uuid primary key, val uuid null)").executeUpdate();

      UUID uuid1 = UUID.randomUUID();
      UUID uuid2 = UUID.randomUUID();
      UUID uuid3 = UUID.randomUUID();
      UUID uuid4 = null;

      NamedQuery insQuery = connection.createNamedQuery("insert into uuidtest(id, val) values (:id, :val)");
      insQuery.addParameter("id", uuid1).addParameter("val", uuid2).executeUpdate();
      insQuery.addParameter("id", uuid3).addParameter("val", uuid4).executeUpdate();

      Table table = connection.createNamedQuery("select * from uuidtest").fetchTable();

      assertThat((UUID) table.rows().get(0).getObject("id")).isEqualTo(uuid1);
      assertThat((UUID) table.rows().get(0).getObject("val")).isEqualTo(uuid2);
      assertThat((UUID) table.rows().get(1).getObject("id")).isEqualTo(uuid3);
      assertThat(table.rows().get(1).getObject("val")).isNull();

      connection.rollback();
    }

  }
}
