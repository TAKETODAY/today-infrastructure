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

package cn.taketoday.jdbc.issues;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import javax.sql.DataSource;

import cn.taketoday.jdbc.JdbcConnection;
import cn.taketoday.jdbc.NamedQuery;
import cn.taketoday.jdbc.RepositoryManager;
import cn.taketoday.jdbc.Table;

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
      int val = connection.createNamedQuery("select 42").fetchScalar(Integer.class);

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
