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

package infra.jdbc.issues;

import java.util.Date;
import java.util.List;

import infra.jdbc.JdbcConnection;
import infra.jdbc.PersistenceException;
import infra.jdbc.RepositoryManager;
import infra.jdbc.Row;
import infra.jdbc.Table;
import infra.jdbc.issues.pojos.Issue1Pojo;
import infra.jdbc.issues.pojos.KeyValueEntity;
import infra.persistence.AbstractRepositoryManagerTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class IssuesTests extends AbstractRepositoryManagerTests {

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
  @ParameterizedRepositoryManagerTest
  void testSetterPriority(DbType dbType, RepositoryManager sql2o) {
    Issue1Pojo pojo = sql2o.createNamedQuery("select 1 val from (values(0))")
            .fetchFirst(Issue1Pojo.class);

    assertEquals(2, pojo.val);
  }

  /**
   * Tests for issue #2 https://github.com/aaberg/sql2o/issues/2
   *
   * Issue: NPE - should instead tell what the problem is
   */
  @ParameterizedRepositoryManagerTest
  void testForFieldDoesNotExistException(DbType dbType, RepositoryManager sql2o) {
    try {
      KeyValueEntity pojo = sql2o.createNamedQuery("select 1 id, 'something' foo from (values(0))").fetchFirst(
              KeyValueEntity.class);
    }
    catch (PersistenceException ex) {
      assertThat(ex).hasMessageContaining("Could not map");
    }
  }

  /**
   * Tests for issue #4 https://github.com/aaberg/sql2o/issues/4
   *
   * NPE when typing wrong column name in row.get(...) Also, column name should
   * not be case sensitive, if sql2o not is in casesensitive property is false.
   */
  @ParameterizedRepositoryManagerTest
  public void testForNpeInRowGet(DbType dbType, RepositoryManager sql2o) {
    sql2o.createNamedQuery("create table issue4table(id integer identity primary key, val varchar(20))").executeUpdate();

    sql2o.createNamedQuery("insert into issue4table (val) values (:val)")
            .addParameter("val", "something").addToBatch()
            .addParameter("val", "something else").addToBatch()
            .addParameter("val", "hello").addToBatch()
            .executeBatch();

    Table table = sql2o.createNamedQuery("select * from issue4table").fetchTable();

    Row row0 = table.rows().get(0);
    String row0Val = row0.getString("vAl");

    assertThat("something").isEqualTo(row0Val);

    Row row1 = table.rows().get(1);
    boolean failed = false;

    try {
      String row1Value = row1.getString("ahsHashah"); // Should fail with an sql2o exception
    }
    catch (PersistenceException ex) {
      failed = true;

      assertThat(ex.getMessage()).startsWith("Column with name 'ahsHashah' does not exist");
    }

    assertThat(failed).isTrue().as("assert that exception occurred");

  }

  public static class Issue5POJO {
    public int id;
    public int val;

    public void setVal(int val) {
      this.val = val;
    }

    public void setId(int id) {
      this.id = id;
    }
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
  @ParameterizedRepositoryManagerTest
  void testForNullToSimpeType(DbType dbType, RepositoryManager sql2o) {
    sql2o.createNamedQuery("create table issue5table(id int identity primary key, val integer)").executeUpdate();

    sql2o.createNamedQuery("insert into issue5table(val) values (:val)")
            .addParameter("val", (Object) null).executeUpdate();

    List<Issue5POJO> list1 = sql2o.createNamedQuery("select * from issue5table")
            .fetch(Issue5POJO.class);

    List<Issue5POJO2> list2 = sql2o.createNamedQuery("select * from issue5table")
            .fetch(Issue5POJO2.class);

    assertThat(list1.size()).isEqualTo(1);
    assertThat(list2.size()).isEqualTo(1);
    assertThat(list1.get(0).val).isEqualTo(0);
    assertThat(list2.get(0).getVal()).isEqualTo(0);
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
  @ParameterizedRepositoryManagerTest
  void testForLabelErrorInHsqlDb(DbType dbType, RepositoryManager sql2o) {
    sql2o.createNamedQuery("create table issue9test (id integer identity primary key, val varchar(50))").executeUpdate();

    String insertSql = "insert into issue9test(val) values (:val)";
    sql2o.createNamedQuery(insertSql).addParameter("val", "something").executeUpdate();
    sql2o.createNamedQuery(insertSql).addParameter("val", "something else").executeUpdate();
    sql2o.createNamedQuery(insertSql).addParameter("val", "something third").executeUpdate();

    List<Issue9Pojo> pojos = sql2o.createNamedQuery("select id, val theVal from issue9Test").fetch(Issue9Pojo.class);

    assertEquals(3, pojos.size());
    assertEquals("something", pojos.get(0).theVal);

  }

  public static enum WhatEverEnum {
    VAL, ANOTHER_VAL;
  }

  @ParameterizedRepositoryManagerTest
  void testForNullPointerExceptionInAddParameterMethod(DbType dbType, RepositoryManager sql2o) {
    sql2o.createNamedQuery("create table issue11test (id integer identity primary key, val varchar(50), adate datetime)")
            .executeUpdate();

    String insertSql = "insert into issue11test (val, adate) values (:val, :date)";
    sql2o.createNamedQuery(insertSql)
            .addParameter("val", WhatEverEnum.VAL)
            .addParameter("date", new Date())
            .executeUpdate();
    Date dtNull = null;
    WhatEverEnum enumNull = null;

    sql2o.createNamedQuery(insertSql).addParameter("val", enumNull)
            .addParameter("date", dtNull).executeUpdate();
  }

  /**
   * Test for issue #132 ( https://github.com/aaberg/sql2o/issues/132 ) Ref change
   * done in pull request #75 Also see comment on google groups
   * https://groups.google.com/forum/#!topic/sql2o/3H4XJIv-i04
   *
   * If a column cannot be mapped to a property, an exception should be thrown.
   * Today it is silently ignored.
   */
  @ParameterizedRepositoryManagerTest
  void testErrorWhenFieldDoesntExist(DbType dbType, RepositoryManager sql2o) {

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
      connection.createNamedQuery(createQuery).executeUpdate();

      String insertSql = "insert into testErrorWhenFieldDoesntExist(id_val, str_val) values (:val1, :val2)";
      connection.createNamedQuery(insertSql)
              .addParameter("val1", 1)
              .addParameter("val2", "test")
              .executeUpdate();

      Exception ex = null;
      try {
        // This is expected to fail to map columns and throw an exception.
        LocalPojo p = connection.createNamedQuery("select * from testErrorWhenFieldDoesntExist")
                .fetchFirst(LocalPojo.class);
      }
      catch (Exception e) {
        ex = e;
      }
      assertNotNull(ex);

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
  @ParameterizedRepositoryManagerTest
  void testIndexOutOfRangeExceptionWithMultipleColumnsWithSameName(DbType dbType, RepositoryManager sql2o) {

    String sql = "select 11 id, 'something' name, 'something else' name from (values(0))";

    ThePojo p;
    Table t;
    try (JdbcConnection connection = sql2o.open()) {
      p = connection.createNamedQuery(sql).fetchFirst(ThePojo.class);

      t = connection.createNamedQuery(sql).fetchTable();
    }

    assertEquals(11, p.id);
    assertEquals("something else", p.name);

    assertEquals(11, (int) t.rows().get(0).getInteger("id"));
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
  @ParameterizedRepositoryManagerTest
  void testIgnoreSqlComments(DbType dbType, RepositoryManager sql2o) {

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
      connection.createNamedQuery(createSql).executeUpdate();

      for (int idx = 0; idx < 100; idx++) {
        int intval = idx % 10;
        connection.createNamedQuery(insertQuery)
                .addParameter("id", idx)
                .addParameter("intval", intval)
                .addParameter("strval", "teststring" + idx)
                .executeUpdate();
      }

      List<TheIgnoreSqlCommentPojo> resultList = connection.createNamedQuery(fetchQuery)
              .addParameter("param", 5)
              .fetch(TheIgnoreSqlCommentPojo.class);

      assertEquals(10, resultList.size());
    }
  }

  static class Pojo {
    public int id;
    public String val1;
  }

  @ParameterizedRepositoryManagerTest
  void testIssue166OneCharacterParameterFail(DbType dbType, RepositoryManager sql2o) {
    try (JdbcConnection connection = sql2o.open()) {
      connection.createNamedQuery("create table testIssue166OneCharacterParameterFail(id integer, val varchar(10))")
              .executeUpdate();

      // This because of the :v parameter.
      connection.createNamedQuery("insert into testIssue166OneCharacterParameterFail(id, val) values(:id, :v)")
              .addParameter("id", 1)
              .addParameter("v", "foobar")
              .executeUpdate();

      int cnt = connection.createNamedQuery("select count(*) from testIssue166OneCharacterParameterFail where id = :p")
              .addParameter("p", 1)
              .scalar(Integer.class);

      assertEquals(1, cnt);
    }
  }

  @ParameterizedRepositoryManagerTest
  void testIssue149NullPointerWhenUsingWrongParameterName(DbType dbType, RepositoryManager sql2o) {

    try (JdbcConnection connection = sql2o.open()) {
      connection.createNamedQuery("create table issue149 (id integer primary key, val varchar(20))").executeUpdate();
      connection.createNamedQuery("insert into issue149(id, val) values (:id, :val)")
              .addParameter("id", 1)
              .addParameter("asdsa", "something") // spell-error in parameter name
              .executeUpdate();

      fail("Expected exception!!");
    }
    catch (PersistenceException ex) {
      // awesome!
    }
    catch (Throwable t) {
      fail("A " + t.getClass().getName() + " was thrown, but An " + PersistenceException.class.getName() + " was expected");
    }
  }
}
