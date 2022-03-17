package cn.taketoday.jdbc.issues;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import javax.sql.DataSource;

import cn.taketoday.jdbc.JdbcConnection;
import cn.taketoday.jdbc.JdbcOperations;
import cn.taketoday.jdbc.Query;
import cn.taketoday.jdbc.result.Table;

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

    JdbcOperations sql2o = new JdbcOperations(ds);

    try (JdbcConnection connection = sql2o.open()) {
      int val = connection.createQuery("select 42").fetchScalar(Integer.class);

      assertThat(val).isEqualTo(42);
    }
  }

  /**
   * Ref issue #73
   */
  @Test
  void testUUID() {

    try (JdbcConnection connection = new JdbcOperations(ds).beginTransaction()) {
      connection.createQuery("create table uuidtest(id uuid primary key, val uuid null)").executeUpdate();

      UUID uuid1 = UUID.randomUUID();
      UUID uuid2 = UUID.randomUUID();
      UUID uuid3 = UUID.randomUUID();
      UUID uuid4 = null;

      Query insQuery = connection.createQuery("insert into uuidtest(id, val) values (:id, :val)");
      insQuery.addParameter("id", uuid1).addParameter("val", uuid2).executeUpdate();
      insQuery.addParameter("id", uuid3).addParameter("val", uuid4).executeUpdate();

      Table table = connection.createQuery("select * from uuidtest").fetchTable();

      assertThat((UUID) table.rows().get(0).getObject("id")).isEqualTo(uuid1);
      assertThat((UUID) table.rows().get(0).getObject("val")).isEqualTo(uuid2);
      assertThat((UUID) table.rows().get(1).getObject("id")).isEqualTo(uuid3);
      assertThat(table.rows().get(1).getObject("val")).isNull();

      connection.rollback();
    }

  }
}
