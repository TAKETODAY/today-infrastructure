package cn.taketoday.jdbc.issues;

import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import javax.sql.DataSource;

import cn.taketoday.jdbc.DefaultSession;
import cn.taketoday.jdbc.JdbcConnection;
import cn.taketoday.jdbc.Query;
import cn.taketoday.jdbc.result.Table;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by lars on 05.10.2014.
 */
public class H2Tests {

  DataSource ds;

  String driverClassName;
  String url;
  String user;
  String pass;

  @Before
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
  public void testIssue155() {

    DefaultSession sql2o = new DefaultSession(ds);

    try (JdbcConnection connection = sql2o.open()) {
      int val = connection.createQuery("select 42").executeScalar(Integer.class);

      assertThat(val, is(equalTo(42)));
    }
  }

  /**
   * Ref issue #73
   */
  @Test
  public void testUUID() {

    try (JdbcConnection connection = new DefaultSession(ds).beginTransaction()) {
      connection.createQuery("create table uuidtest(id uuid primary key, val uuid null)").executeUpdate();

      UUID uuid1 = UUID.randomUUID();
      UUID uuid2 = UUID.randomUUID();
      UUID uuid3 = UUID.randomUUID();
      UUID uuid4 = null;

      Query insQuery = connection.createQuery("insert into uuidtest(id, val) values (:id, :val)");
      insQuery.addParameter("id", uuid1).addParameter("val", uuid2).executeUpdate();
      insQuery.addParameter("id", uuid3).addParameter("val", uuid4).executeUpdate();

      Table table = connection.createQuery("select * from uuidtest").fetchTable();

      assertThat((UUID) table.rows().get(0).getObject("id"), is(equalTo(uuid1)));
      assertThat((UUID) table.rows().get(0).getObject("val"), is(equalTo(uuid2)));
      assertThat((UUID) table.rows().get(1).getObject("id"), is(equalTo(uuid3)));
      assertThat(table.rows().get(1).getObject("val"), is(nullValue()));

      connection.rollback();
    }

  }
}
