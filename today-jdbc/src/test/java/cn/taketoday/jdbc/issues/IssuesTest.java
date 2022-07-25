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

package cn.taketoday.jdbc.issues;

import org.hsqldb.jdbcDriver;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import cn.taketoday.jdbc.JdbcConnection;
import cn.taketoday.jdbc.JdbcOperations;
import cn.taketoday.jdbc.PersistenceException;
import cn.taketoday.jdbc.issues.pojos.Issue1Pojo;
import cn.taketoday.jdbc.issues.pojos.KeyValueEntity;
import cn.taketoday.jdbc.result.Row;
import cn.taketoday.jdbc.result.Table;
import lombok.Setter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created by IntelliJ IDEA. User: lars Date: 10/17/11 Time: 9:02 PM This class
 * is to test for reported issues.
 */
@RunWith(Parameterized.class)
public class IssuesTest {

  @Parameterized.Parameters(name = "{index} - {4}")
  public static Collection<Object[]> getData() {
    return Arrays.asList(
            new Object[][] {
                    { null, "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "", "H2 test" },
                    { new jdbcDriver(), "jdbc:hsqldb:mem:testmemdb", "SA", "", "HyperSQL DB test" }
            });
  }

  private JdbcOperations sql2o;
  private String url;
  private String user;
  private String pass;

  public IssuesTest(Driver driverToRegister, String url, String user, String pass, String testName) {
    if (driverToRegister != null) {
      try {
        DriverManager.registerDriver(driverToRegister);
      }
      catch (SQLException e) {
        throw new RuntimeException("could not register driver '" + driverToRegister.getClass().getName() + "'", e);
      }
    }

    this.sql2o = new JdbcOperations(url, user, pass);

    this.url = url;
    this.user = user;
    this.pass = pass;

    if ("HyperSQL DB test".equals(testName)) {
      sql2o.createQuery("set database sql syntax MSS true").executeUpdate();
    }
  }

  /**
   * Tests for issue #1 https://github.com/aaberg/sql2o/issues/1
   *
   * Issue: I have a case where I need to override/modify the value loaded from
   * db. I want to do this in a setter but the current version of sql2o modifies
   * the property directly.
   *
   * Comment: The priority was wrong. Sql2o would try to set the field first, and
   * afterwards the setter. The priority should be the setter first and the field
   * after.
   */
  @Test
  public void testSetterPriority() {
    JdbcOperations sql2o = new JdbcOperations(url, user, pass);
    Issue1Pojo pojo = sql2o.createQuery("select 1 val from (values(0))")
            .fetchFirst(Issue1Pojo.class);

    assertEquals(2, pojo.val);
  }

  /**
   * Tests for issue #2 https://github.com/aaberg/sql2o/issues/2
   *
   * Issue: NPE - should instead tell what the problem is
   */
  @Test
  public void testForFieldDoesNotExistException() {
    JdbcOperations sql2o = new JdbcOperations(url, user, pass);

    try {
      KeyValueEntity pojo = sql2o.createQuery("select 1 id, 'something' foo from (values(0))").fetchFirst(
              KeyValueEntity.class);
    }
    catch (PersistenceException ex) {
      Assert.assertTrue(ex.getMessage().contains("Could not map"));
    }
  }

  /**
   * Tests for issue #4 https://github.com/aaberg/sql2o/issues/4
   *
   * NPE when typing wrong column name in row.get(...) Also, column name should
   * not be case sensitive, if sql2o not is in casesensitive property is false.
   */
  @Test
  public void testForNpeInRowGet() {
    sql2o.createQuery("create table issue4table(id integer identity primary key, val varchar(20))").executeUpdate();

    sql2o.createQuery("insert into issue4table (val) values (:val)")
            .addParameter("val", "something").addToBatch()
            .addParameter("val", "something else").addToBatch()
            .addParameter("val", "hello").addToBatch()
            .executeBatch();

    Table table = sql2o.createQuery("select * from issue4table").fetchTable();

    Row row0 = table.rows().get(0);
    String row0Val = row0.getString("vAl");

    Assert.assertEquals("something", row0Val);

    Row row1 = table.rows().get(1);
    boolean failed = false;

    try {
      String row1Value = row1.getString("ahsHashah"); // Should fail with an sql2o exception
    }
    catch (PersistenceException ex) {
      failed = true;

      Assert.assertTrue(ex.getMessage().startsWith("Column with name 'ahsHashah' does not exist"));
    }

    Assert.assertTrue("assert that exception occurred", failed);

  }

  @Setter
  public static class Issue5POJO {
    public int id;
    public int val;
  }

  public static class Issue5POJO2 {
    public int id;
    public int val;

    public int getVal() {
      return val;
    }

    public void setVal(int val) {
      this.val = val;
    }
  }

  /**
   * Tests for issue #5 https://github.com/aaberg/sql2o/issues/5 crashes if the
   * POJO has a int field where we try to set a null value
   */
  @Test
  public void testForNullToSimpeType() {
    sql2o.createQuery("create table issue5table(id int identity primary key, val integer)").executeUpdate();

    sql2o.createQuery("insert into issue5table(val) values (:val)")
            .addParameter("val", (Object) null).executeUpdate();

    List<Issue5POJO> list1 = sql2o.createQuery("select * from issue5table")
            .fetch(Issue5POJO.class);

    List<Issue5POJO2> list2 = sql2o.createQuery("select * from issue5table")
            .fetch(Issue5POJO2.class);

    Assert.assertEquals(1, list1.size());
    Assert.assertEquals(1, list2.size());
    Assert.assertEquals(0, list1.get(0).val);
    Assert.assertEquals(0, list2.get(0).getVal());
  }

  /**
   * Tests for issue #9 https://github.com/aaberg/sql2o/issues/9 When running a
   * select query with column labels (aliases) in HSQLDB, sql2o is still trying to
   * use column names wheWhen running a select query with column labels (aliases)
   * in HSQLDB, sql2o is still trying to use column names when mapping to java
   * classes. This is caused by a behavior in HSQLDB, that is different from most
   * other databases. the ResultSet.getColumnName() method will still return the
   * real column name, even though a label was used. To get the label with HSQLDB,
   * ResultSet.getColumnLabel().n mapping to java classes. This is caused by a
   * behavior in HSQLDB, that is different from most other databases. the
   * ResultSet.getColumnName() method will still return the real column name, even
   * though a label was used. To get the label with HSQLDB,
   * ResultSet.getColumnLabel().
   */
  @Test
  public void testForLabelErrorInHsqlDb() {
    sql2o.createQuery("create table issue9test (id integer identity primary key, val varchar(50))").executeUpdate();

    String insertSql = "insert into issue9test(val) values (:val)";
    sql2o.createQuery(insertSql).addParameter("val", "something").executeUpdate();
    sql2o.createQuery(insertSql).addParameter("val", "something else").executeUpdate();
    sql2o.createQuery(insertSql).addParameter("val", "something third").executeUpdate();

    List<Issue9Pojo> pojos = sql2o.createQuery("select id, val theVal from issue9Test").fetch(Issue9Pojo.class);

    Assert.assertEquals(3, pojos.size());
    Assert.assertEquals("something", pojos.get(0).theVal);

  }

  public static enum WhatEverEnum {
    VAL, ANOTHER_VAL;
  }

  @Test
  public void testForNullPointerExceptionInAddParameterMethod() {
    sql2o.createQuery("create table issue11test (id integer identity primary key, val varchar(50), adate datetime)")
            .executeUpdate();

    String insertSql = "insert into issue11test (val, adate) values (:val, :date)";
    sql2o.createQuery(insertSql)
            .addParameter("val", WhatEverEnum.VAL)
            .addParameter("date", new DateTime())
            .executeUpdate();
    DateTime dtNull = null;
    WhatEverEnum enumNull = null;

    sql2o.createQuery(insertSql).addParameter("val", enumNull).addParameter("date", dtNull).executeUpdate();
  }

  /**
   * Test for issue #132 ( https://github.com/aaberg/sql2o/issues/132 ) Ref change
   * done in pull request #75 Also see comment on google groups
   * https://groups.google.com/forum/#!topic/sql2o/3H4XJIv-i04
   *
   * If a column cannot be mapped to a property, an exception should be thrown.
   * Today it is silently ignored.
   */
  @Test
  public void testErrorWhenFieldDoesntExist() {

    class LocalPojo {
      private long id;
      private String strVal;

      public long getId() {
        return id;
      }

      public String getStrVal() {
        return strVal;
      }
    }

    String createQuery = "create table testErrorWhenFieldDoesntExist(id_val integer primary key, str_val varchar(100))";

    try (JdbcConnection connection = sql2o.open()) {
      connection.createQuery(createQuery).executeUpdate();

      String insertSql = "insert into testErrorWhenFieldDoesntExist(id_val, str_val) values (:val1, :val2)";
      connection.createQuery(insertSql)
              .addParameter("val1", 1)
              .addParameter("val2", "test")
              .executeUpdate();

      Exception ex = null;
      try {
        // This is expected to fail to map columns and throw an exception.
        LocalPojo p = connection.createQuery("select * from testErrorWhenFieldDoesntExist")
                .fetchFirst(LocalPojo.class);
      }
      catch (Exception e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

    }
  }

  public static class Issue9Pojo {
    public int id;
    public String theVal;
  }

  static class ThePojo {
    public int id;
    public String name;
  }

  /**
   * Test for issue #148 (https://github.com/aaberg/sql2o/issues/148) ##
   * IndexOutOfRange exception When a resultset has multiple columns with the same
   * name, sql2o 1.5.1 will throw an IndexOutOfRange exception when calling
   * executeAndFetchTable() method.
   */
  @Test
  public void testIndexOutOfRangeExceptionWithMultipleColumnsWithSameName() {

    String sql = "select 11 id, 'something' name, 'something else' name from (values(0))";

    ThePojo p;
    Table t;
    try (JdbcConnection connection = sql2o.open()) {
      p = connection.createQuery(sql).fetchFirst(ThePojo.class);

      t = connection.createQuery(sql).fetchTable();
    }

    Assert.assertEquals(11, p.id);
    Assert.assertEquals("something else", p.name);

    Assert.assertEquals(11, (int) t.rows().get(0).getInteger("id"));
    assertEquals("something else", t.rows().get(0).getString("name"));
  }

  static class TheIgnoreSqlCommentPojo {
    public int id;
    public int intval;
    public String strval;
  }

  /**
   * Reproduce issue #142 (https://github.com/aaberg/sql2o/issues/142)
   */
  @Test
  public void testIgnoreSqlComments() {

    String createSql = "create table testIgnoreSqlComments(id integer primary key, intval integer, strval varchar(100))";

    String insertQuery = "insert into testIgnoreSqlComments (id, intval, strval)\n " +
            "-- It's a comment!\n" +
            "values (:id, :intval, :strval);";

    String fetchQuery = "select id, intval, strval\n" +
            "-- a 'comment'\n" +
            "from testIgnoreSqlComments\n" +
            "/* and, it's another type of comment!*/" +
            "where intval = :param";

    try (JdbcConnection connection = sql2o.open()) {
      connection.createQuery(createSql).executeUpdate();

      for (int idx = 0; idx < 100; idx++) {
        int intval = idx % 10;
        connection.createQuery(insertQuery)
                .addParameter("id", idx)
                .addParameter("intval", intval)
                .addParameter("strval", "teststring" + idx)
                .executeUpdate();
      }

      List<TheIgnoreSqlCommentPojo> resultList = connection.createQuery(fetchQuery)
              .addParameter("param", 5)
              .fetch(TheIgnoreSqlCommentPojo.class);

      Assert.assertEquals(10, resultList.size());
    }
  }

  static class Pojo {
    public int id;
    public String val1;
  }

  @Test
  public void testIssue166OneCharacterParameterFail() {
    try (JdbcConnection connection = sql2o.open()) {
      connection.createQuery("create table testIssue166OneCharacterParameterFail(id integer, val varchar(10))")
              .executeUpdate();

      // This because of the :v parameter.
      connection.createQuery("insert into testIssue166OneCharacterParameterFail(id, val) values(:id, :v)")
              .addParameter("id", 1)
              .addParameter("v", "foobar")
              .executeUpdate();

      int cnt = connection.createQuery("select count(*) from testIssue166OneCharacterParameterFail where id = :p")
              .addParameter("p", 1)
              .fetchScalar(Integer.class);

      Assert.assertEquals(1, cnt);
    }
  }

  @Test
  public void testIssue149NullPointerWhenUsingWrongParameterName() {

    try (JdbcConnection connection = sql2o.open()) {
      connection.createQuery("create table issue149 (id integer primary key, val varchar(20))").executeUpdate();
      connection.createQuery("insert into issue149(id, val) values (:id, :val)")
              .addParameter("id", 1)
              .addParameter("asdsa", "something") // spell-error in parameter name
              .executeUpdate();

      Assert.fail("Expected exception!!");
    }
    catch (PersistenceException ex) {
      // awesome!
    }
    catch (Throwable t) {
      Assert.fail("A " + t.getClass().getName() + " was thrown, but An " + PersistenceException.class.getName() + " was expected");
    }
  }
}
