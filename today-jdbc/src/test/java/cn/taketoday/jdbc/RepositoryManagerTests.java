/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.jdbc;

import com.google.common.collect.ImmutableList;

import org.hsqldb.jdbc.JDBCDataSource;
import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.joda.time.Period;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;

import cn.taketoday.dao.DataAccessException;
import cn.taketoday.jdbc.pojos.BigDecimalPojo;
import cn.taketoday.jdbc.pojos.ComplexEntity;
import cn.taketoday.jdbc.pojos.EntityWithPrivateFields;
import cn.taketoday.jdbc.pojos.StringConversionPojo;
import cn.taketoday.jdbc.pojos.SuperPojo;
import cn.taketoday.jdbc.result.LazyTable;
import cn.taketoday.jdbc.result.ResultSetIterable;
import cn.taketoday.jdbc.result.Row;
import cn.taketoday.jdbc.result.Table;
import cn.taketoday.jdbc.type.BytesInputStreamTypeHandler;
import cn.taketoday.jdbc.type.Enumerated;
import cn.taketoday.jdbc.type.TypeHandlerRegistry;
import cn.taketoday.jdbc.utils.IOUtils;
import cn.taketoday.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Created by IntelliJ IDEA. User: lars Date: 5/21/11 Time: 9:25 PM Most sql2o
 * tests are in this class.
 */
@RunWith(Parameterized.class)
public class RepositoryManagerTests extends BaseMemDbTest {

  private static final int NUMBER_OF_USERS_IN_THE_TEST = 10000;

  private int insertIntoUsers = 0;

  public RepositoryManagerTests(DbType dbType, String testName) {
    super(dbType, testName);
  }

  //@Test  TODO. commented out. Can't get test to work without an application server.
  public void testCreateSql2oFromJndi() throws Exception {
    System.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.apache.naming.java.javaURLContextFactory");
    System.setProperty(Context.URL_PKG_PREFIXES, "org.apache.naming");

    InitialContext ic = new InitialContext();

    ic.createSubcontext("java:");
    ic.createSubcontext("java:comp");
    ic.createSubcontext("java:comp/env");

    JDBCDataSource datasource = new JDBCDataSource();
    datasource.setUrl(dbType.url);
    datasource.setUser(dbType.user);
    datasource.setPassword(dbType.pass);

    ic.bind("java:comp/env/Sql2o", datasource);

    System.out.println("Datasource initialized.");

    RepositoryManager jndiSql2o = new RepositoryManager("Sql2o");

    assertNotNull(jndiSql2o);
  }

  @Test
  public void testExecuteAndFetch() {
    createAndFillUserTable();

    try (JdbcConnection con = repositoryManager.open()) {

      Date before = new Date();
      List<User> allUsers = con.createNamedQuery("select * from User").fetch(User.class);

      assertNotNull(allUsers);

      Date after = new Date();
      long span = after.getTime() - before.getTime();
      System.out.printf("Fetched %s user: %s ms%n", insertIntoUsers, span);

      // repeat this
      before = new Date();
      allUsers = con.createNamedQuery("select * from User").fetch(User.class);
      after = new Date();
      span = after.getTime() - before.getTime();
      System.out.printf("Again Fetched %s user: %s ms%n", insertIntoUsers, span);

      assertEquals(allUsers.size(), insertIntoUsers);

    }
    deleteUserTable();
  }

  @Test
  public void testExecuteAndFetchWithNulls() {
    String sql = "create table testExecWithNullsTbl (" +
            "id int identity primary key, " +
            "text varchar(255), " +
            "aNumber int, " +
            "aLongNumber bigint)";
    try (JdbcConnection con = repositoryManager.open()) {
      con.createNamedQuery(sql, "testExecuteAndFetchWithNulls").executeUpdate();

      JdbcConnection connection = repositoryManager.beginTransaction();
      NamedQuery insQuery = connection.createNamedQuery(
              "insert into testExecWithNullsTbl (text, aNumber, aLongNumber) values(:text, :number, :lnum)");
      insQuery.addParameter("text", "some text").addParameter("number", 2).addParameter("lnum", 10L).executeUpdate();
      insQuery.addParameter("text", "some text")
              .addNullParameter("number")
              .addParameter("lnum", 10L).executeUpdate();

      insQuery.addParameter("text", (String) null)
              .addParameter("number", 21)
              .addNullParameter("lnum")
              .executeUpdate();

      insQuery.addParameter("text", "some text").addParameter("number", 1221).addParameter("lnum", 10).executeUpdate();
      insQuery.addParameter("text", "some text").addParameter("number", 2311).addParameter("lnum", 12).executeUpdate();
      connection.commit();

      List<Entity> fetched = con.createNamedQuery("select * from testExecWithNullsTbl").fetch(Entity.class);

      assertEquals(5, fetched.size());
      assertNull(fetched.get(2).text);
      assertNotNull(fetched.get(3).text);

      assertNull(fetched.get(1).aNumber);
      assertNotNull(fetched.get(2).aNumber);

      assertNull(fetched.get(2).aLongNumber);
      assertNotNull(fetched.get(3).aLongNumber);
    }
  }

  @Test
  public void testBatch() {
    repositoryManager.createNamedQuery(
            "create table User(\n" +
                    "id int identity primary key,\n" +
                    "name varchar(20),\n" +
                    "email varchar(255),\n" +
                    "text varchar(100))").executeUpdate();

    String insQuery = "insert into User(name, email, text) values (:name, :email, :text)";

    JdbcConnection con = repositoryManager.beginTransaction();
    int[] inserted = con.createNamedQuery(insQuery)
            .addParameter("name", "test")
            .addParameter("email", "test@test.com")
            .addParameter("text", "something exciting")
            .addToBatch()

            .addParameter("name", "test2")
            .addParameter("email", "test2@test.com")
            .addParameter("text", "something exciting too")
            .addToBatch()

            .addParameter("name", "test3")
            .addParameter("email", "test3@test.com")
            .addParameter("text", "blablabla")
            .addToBatch()

            .executeBatch()
            .getLastBatchResult();
    con.commit();

    assertEquals(3, inserted.length);
    for (int i : inserted) {
      assertEquals(1, i);
    }

    deleteUserTable();
  }

  @Test
  public void testExecuteScalar() {
    createAndFillUserTable();

    Object o = repositoryManager.createNamedQuery("select text from User where id = 2").fetchScalar();
    assertEquals(o.getClass(), String.class);

    Object o2 = repositoryManager.createNamedQuery("select 10").fetchScalar();
    assertEquals(o2, 10);

    deleteUserTable();
  }

  @Test
  public void testBatchNoTransaction() {

    repositoryManager.createNamedQuery(
            "create table User(\n" +
                    "id int identity primary key,\n" +
                    "name varchar(20),\n" +
                    "email varchar(255),\n" +
                    "text varchar(100))").executeUpdate();

    String insQuery = "insert into User(name, email, text) values (:name, :email, :text)";

    repositoryManager.createNamedQuery(insQuery)
            .addParameter("name", "test")
            .addParameter("email", "test@test.com")
            .addParameter("text", "something exciting")
            .addToBatch()

            .addParameter("name", "test2")
            .addParameter("email", "test2@test.com")
            .addParameter("text", "something exciting too")
            .addToBatch()

            .addParameter("name", "test3")
            .addParameter("email", "test3@test.com")
            .addParameter("text", "blablabla")
            .addToBatch()

            .executeBatch();

    deleteUserTable();
  }

  @Test
  public void testCaseInsensitive() {
    repositoryManager
            .createNamedQuery("create table testCI(id2 int primary key, value2 varchar(20), sometext varchar(20), valwithgetter varchar(20))")
            .executeUpdate();

    NamedQuery query = repositoryManager.createNamedQuery(
            "insert into testCI(id2, value2, sometext, valwithgetter) values(:id, :value, :someText, :valwithgetter)");
    for (int i = 0; i < 20; i++) {
      query.addParameter("id", i).addParameter("value", "some text " + i).addParameter("someText", "whatever " + i).addParameter(
                      "valwithgetter",
                      "spaz" + i)
              .addToBatch();
    }
    query.executeBatch();

    List<CIEntity> ciEntities = repositoryManager.createNamedQuery("select * from testCI").setCaseSensitive(false).fetch(CIEntity.class);

    assertEquals(20, ciEntities.size());

    // test defaultCaseSensitive;
    repositoryManager.setDefaultCaseSensitive(false);
    List<CIEntity> ciEntities2 = repositoryManager.createNamedQuery("select * from testCI").fetch(CIEntity.class);
    assertEquals(20, ciEntities2.size());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetMaxBatchRecords() {
    try (JdbcConnection conn = this.repositoryManager.open()) {
      NamedQuery q = conn.createNamedQuery("select 'test'");
      q.setMaxBatchRecords(20);
      assertEquals(20, q.getMaxBatchRecords());

      q.setMaxBatchRecords(0);
      assertEquals(0, q.getMaxBatchRecords());

      q.setMaxBatchRecords(-1);
    }
  }

  @Test
  public void testBatchWithMaxBatchRecords() {
    try (JdbcConnection connection = repositoryManager.open()) {
      createAndFillUserTable(connection, true, 50);
      genericTestOnUserData(connection);
    }

    //also test with an odd number

    try (JdbcConnection connection = repositoryManager.open()) {
      createAndFillUserTable(connection, true, 29);
      genericTestOnUserData(connection);
    }
  }

  @Test
  public void testExecuteAndFetchResultSet() {
    List<Integer> list = repositoryManager.createNamedQuery(
                    "select 1 val from (values(0)) union select 2 from (values(0)) union select 3 from (values(0))")
            .fetchScalars(Integer.class);

    assertEquals((int) list.get(0), 1);
    assertEquals((int) list.get(1), 2);
    assertEquals((int) list.get(2), 3);
  }

  @Test
  public void testExecuteScalarListWithNulls() throws SQLException {
    List<String> list = repositoryManager.createNamedQuery(
            "select val from ( " +
                    "select 1 ord, null val from (values(0)) union " +
                    "select 2 ord, 'one' from (values(0)) union " +
                    "select 3 ord, null from (values(0)) union " +
                    "select 4 ord, 'two' from (values(0)) " +
                    ") order by ord" // explicit ordering since nulls seem to mess with ordering
    ).fetchScalars(String.class);

    assertEquals(4, list.size());

    assertNull(list.get(0));
    assertEquals(list.get(1), "one");
    assertNull(list.get(2));
    assertEquals(list.get(3), "two");
  }

  @Test
  public void testJodaTime() {

    repositoryManager.createNamedQuery("create table testjoda(id int primary key, joda1 datetime, joda2 datetime)").executeUpdate();

    repositoryManager.createNamedQuery("insert into testjoda(id, joda1, joda2) values(:id, :joda1, :joda2)")
            .addParameter("id", 1)
            .addParameter("joda1", new DateTime()).addParameter("joda2", new DateTime().plusDays(-1))
            .addToBatch()
            .addParameter("id", 2)
            .addParameter("joda1", new DateTime().plusYears(1))
            .addParameter("joda2", new DateTime().plusDays(-2))
            .addToBatch()
            .addParameter("id", 3)
            .addParameter("joda1", new DateTime().plusYears(2))
            .addParameter("joda2", new DateTime().plusDays(-3))
            .addToBatch()
            .executeBatch();

    List<JodaEntity> list = repositoryManager.createNamedQuery("select * from testjoda").fetch(JodaEntity.class);

    assertEquals(3, list.size());
    assertTrue(list.get(0).getJoda2().isBeforeNow());

  }

  @Test
  public void testColumnAnnotation() {
    try (JdbcConnection connection = repositoryManager.open()) {
      connection.createNamedQuery("create table test_column_annotation(id int primary key, text_col varchar(20))").executeUpdate();

      connection.createNamedQuery("insert into test_column_annotation(id, text_col) values(:id, :text)")
              .addParameter("id", 1)
              .addParameter("text", "test1")
              .addToBatch()

              .addParameter("id", 2)
              .addParameter("text", "test2")
              .addToBatch()

              .executeBatch();

      List<ColumnEntity> result = connection.createNamedQuery("select * from test_column_annotation")
              .fetch(ColumnEntity.class);

      assertEquals(2, result.size());
      assertEquals(1, result.get(0).getId());
      assertEquals("test1", result.get(0).getText());
      assertEquals(2, result.get(1).getId());
      assertEquals("test2", result.get(1).getText());
    }
  }

  @Test
  public void testUtilDate() {
    repositoryManager.createNamedQuery("create table testutildate(id int primary key, d1 datetime, d2 timestamp, d3 date)").executeUpdate();

    Date now = new Date();

    repositoryManager.createNamedQuery("insert into testutildate(id, d1, d2, d3) values(:id, :d1, :d2, :d3)")
            .addParameter("id", 1).addParameter("d1", now).addParameter("d2", now).addParameter("d3", now).addToBatch()
            .addParameter("id", 2).addParameter("d1", now).addParameter("d2", now).addParameter("d3", now).addToBatch()
            .addParameter("id", 3).addParameter("d1", now).addParameter("d2", now).addParameter("d3", now).addToBatch()
            .executeBatch();

    List<UtilDateEntity> list = repositoryManager.createNamedQuery("select * from testutildate").fetch(UtilDateEntity.class);

    assertEquals(3, list.size());

    // make sure d1, d2, d3 were properly inserted and selected
    for (UtilDateEntity e : list) {
      assertEquals(now, e.d1);
      assertEquals(now, e.getD2());
      Date dateOnly = new DateTime(now).toDateMidnight().toDate();
      assertEquals(dateOnly, e.getD3());
    }
  }

  @Test
  public void testConversion() {

    String sql = "select cast(1 as smallint) as val1, 2 as val2 from (values(0)) union select cast(3 as smallint) as val1, 4 as val2 from (values(0))";
    List<TypeConvertEntity> entities = repositoryManager.createNamedQuery(sql)
            .fetch(TypeConvertEntity.class);

    assertEquals(2, entities.size());
  }

  @Test
  public void testUpdateNoTransaction() throws SQLException {
    String ddlQuery = "create table testUpdateNoTransaction(id int primary key, value varchar(50))";
    JdbcConnection connection =
            repositoryManager.createNamedQuery(ddlQuery)
                    .executeUpdate()
                    .getConnection();

    assertTrue(connection.getJdbcConnection().isClosed());

    String insQuery = "insert into testUpdateNoTransaction(id, value) values (:id, :value)";
    repositoryManager.createNamedQuery(insQuery)
            .addParameter("id", 1)
            .addParameter("value", "test1")
            .executeUpdate()
            .createNamedQuery(insQuery)
            .addParameter("id", 2)
            .addParameter("value", "val2")
            .executeUpdate();

    assertTrue(connection.getJdbcConnection().isClosed());
  }

  @Test
  public void testNullDate() {
    repositoryManager.createNamedQuery("create table nullDateTest(id integer primary key, somedate datetime)").executeUpdate();

    repositoryManager.createNamedQuery("insert into nullDateTest(id, somedate) values(:id, :date)")
            .addParameter("id", 1)
            .addParameter("date", (Date) null).executeUpdate();

    Date d = (Date) repositoryManager.createNamedQuery("select somedate from nullDateTest where id = 1").fetchScalar();
    assertNull(d);
  }

  @Test
  public void testGetResult() {

    repositoryManager.createNamedQuery("create table get_result_test(id integer primary key, value varchar(20))").executeUpdate();

    String insertSql = "insert into get_result_test(id, value) " +
            "select 1, 'hello' from (values(0)) union " +
            "select 2, 'hello2' from (values(0)) union " +
            "select 3, 'hello3' from (values(0))";

    int result = repositoryManager.createNamedQuery(insertSql).executeUpdate().getAffectedRows();

    assertEquals(3, result);
  }

  @Test
  public void testGetKeys() {

    repositoryManager.createNamedQuery("create table get_keys_test(id integer identity primary key, value varchar(20))").executeUpdate();

    String insertSql = "insert into get_keys_test(value) values(:val)";
//        try{
//            Integer key = (Integer)sql2o.createQuery(insertSql).addParameter("val", "something").executeUpdate().getKey();
//            throw new RuntimeException("Sql2oException expected in code line above");
//        }
//        catch(Sql2oException ex){
//            assertTrue(ex.getMessage().contains("executeUpdate(true)"));
//        }

    Integer key = (Integer) repositoryManager.createNamedQuery(insertSql).addParameter("val", "something").executeUpdate().getFirstKey();

    assertNotNull(key);
    assertTrue(key >= 0);

    String multiInsertSql = "insert into get_keys_test(value) select 'a val' col1 from (values(0)) union select 'another val' col1 from (values(0))";
    Object[] keys = repositoryManager.createNamedQuery(multiInsertSql).executeUpdate().getKeys();

    assertNotNull(keys);

    // return value of auto generated keys is DB dependent.
    // H2 will always just return the last generated identity.
    // HyperSQL returns all generated identities (which is more ideal).
    if (this.dbType == DbType.HyperSQL) {
      assertEquals(2, keys.length);
    }
    else {
      assertTrue(keys.length > 0);
    }
  }

  @Test
  public void testExecuteBatchGetKeys() {
    repositoryManager.createNamedQuery("create table get_keys_test2(id integer identity primary key, value varchar(20))").executeUpdate();

    String insertSql = "insert into get_keys_test2(value) values(:val)";

    List<String> vals = new ArrayList<String>() {
      {
        add("something1");
        add("something2");
        add("something3");
      }
    };

    NamedQuery query = repositoryManager.createNamedQuery(insertSql, true);

    for (String val : vals) {
      query.addParameter("val", val);
      query.addToBatch();
    }

    List<Integer> keys = query.executeBatch().getKeys(Integer.class);

    assertNotNull(keys);
    for (Integer key : keys) {
      assertTrue(key >= 0);
    }

    // return value of auto generated keys is DB dependent.
    // H2 will always just return the last generated identity.
    // HyperSQL returns all generated identities (which is more ideal).
    if (this.dbType == DbType.HyperSQL) {
      assertEquals(keys.size(), vals.size());
    }
    else {
      assertTrue(keys.size() > 0);
    }
  }

  @Test
  public void testRollback() {

    repositoryManager.createNamedQuery("create table test_rollback_table(id integer identity primary key, value varchar(25))").executeUpdate();

    repositoryManager
            //first insert something, and commit it.
            .beginTransaction()
            .createNamedQuery("insert into test_rollback_table(value) values (:val)")
            .addParameter("val", "something")
            .executeUpdate()
            .commit();

    // insert something else, and roll it back.
    repositoryManager.beginTransaction()
            .createNamedQuery("insert into test_rollback_table(value) values (:val)")
            .addParameter("val", "something to rollback")
            .executeUpdate()
            .rollback();

    long rowCount = (Long) repositoryManager.createNamedQuery(
            "select count(*) from test_rollback_table").fetchScalar();

    assertEquals(1, rowCount);
  }

  @Test
  public void testBigDecimals() {

    repositoryManager.createNamedQuery(
                    "create table bigdectesttable (id integer identity primary key, val1 numeric(5,3), val2 integer)")
            .executeUpdate();

    repositoryManager.createNamedQuery("insert into bigdectesttable(val1, val2) values(:val1, :val2)")
            .addParameter("val1", 1.256)
            .addParameter("val2", 4)
            .executeUpdate();

    BigDecimalPojo pojo = repositoryManager.createNamedQuery("select * from bigdectesttable")
            .fetchFirst(BigDecimalPojo.class);

    assertEquals(new BigDecimal("1.256"), pojo.val1);
    assertEquals(BigDecimal.valueOf(4), pojo.val2);
  }

  @Test
  public void testQueryDbMappings() {
    Entity entity = repositoryManager.createNamedQuery(
                    "select 1 as id, 'something' as caption, cast('2011-01-01' as date) as theTime from (values(0))")
            .addColumnMapping("caption", "text")
            .addColumnMapping("theTime", "time")
            .fetchFirst(Entity.class);

    assertEquals(1, entity.id);
    assertEquals("something", entity.text);
    assertEquals(new DateTime(2011, 1, 1, 0, 0, 0, 0).toDate(), entity.time);
  }

  @Test
  public void testGlobalDbMappings() {
    RepositoryManager sql2o1 = new RepositoryManager(dbType.url, dbType.user, dbType.pass);

    Map<String, String> defaultColMaps = new HashMap<>();
    defaultColMaps.put("caption", "text");
    defaultColMaps.put("theTime", "time");

    sql2o1.setDefaultColumnMappings(defaultColMaps);

    Entity entity = sql2o1.createNamedQuery(
                    "select 1 as id, 'something' as caption, cast('2011-01-01' as date) as theTime from (values(0))")
            .fetchFirst(Entity.class);

    assertEquals(1, entity.id);
    assertEquals("something", entity.text);
    assertEquals(new DateTime(2011, 1, 1, 0, 0, 0, 0).toDate(), entity.time);

  }

  @Test
  public void testSetPrivateFields() {
    EntityWithPrivateFields entity = repositoryManager.createNamedQuery(
                    "select 1 id, 'hello' value from (values(0))")
            .fetchFirst(EntityWithPrivateFields.class);

    assertEquals(1, entity.getId());
    assertEquals("hello1", entity.getValue());
  }

  @Test
  public void testFetchTable() {
    repositoryManager.createNamedQuery(
                    "create table tabletest(id integer identity primary key, value varchar(20), value2 decimal(5,1))")
            .executeUpdate();
    repositoryManager.createNamedQuery("insert into tabletest(value,value2) values (:value, :value2)")
            .addParameter("value", "something").addParameter("value2", new BigDecimal("3.4")).addToBatch()
            .addParameter("value", "bla").addParameter("value2", new BigDecimal("5.5")).addToBatch().executeBatch();

    Table table = repositoryManager.createNamedQuery("select * from tabletest order by id").fetchTable();

    assertEquals(3, table.columns().size());
    assertEquals("ID", table.columns().get(0).getName());
    assertEquals("VALUE", table.columns().get(1).getName());
    assertEquals("VALUE2", table.columns().get(2).getName());

    assertEquals(2, table.rows().size());

    Row row0 = table.rows().get(0);
    Row row1 = table.rows().get(1);

    assertTrue(0 <= row0.getInteger("ID"));
    assertEquals("something", row0.getString(1));
    assertEquals(new BigDecimal("3.4"), row0.getBigDecimal("VALUE2"));

    assertTrue(1 <= row1.getInteger(0));
    assertEquals("bla", row1.getString("VALUE"));
    assertEquals(5.5D, row1.getDouble(2), 0.00001);
  }

  @Test
  public void testTable_asList() {
    createAndFillUserTable();

    List<Map<String, Object>> rows;
    try (JdbcConnection con = repositoryManager.open()) {
      Table table = con.createNamedQuery("select * from user").fetchTable();

      rows = table.asList();
    }

    assertEquals(insertIntoUsers, rows.size());

    for (Map<String, Object> row : rows) {
      assertEquals(4, row.size());
      assertTrue(row.containsKey("id"));
      assertTrue(row.containsKey("name"));
      assertTrue(row.containsKey("email"));
      assertTrue(row.containsKey("text"));
    }

    deleteUserTable();
  }

  @Test
  public void testStringConversion() {
    StringConversionPojo pojo = repositoryManager.createNamedQuery(
                    "select '1' val1, '2  ' val2, '' val3, '' val4, null val5 from (values(0))")
            .fetchFirst(StringConversionPojo.class);

    assertEquals((Integer) 1, pojo.val1);
    assertEquals(2L, pojo.val2);
    assertNull(pojo.val3);
    assertEquals(0, pojo.val4);
    assertNull(pojo.val5);
  }

  @Test
  public void testSuperPojo() {
    SuperPojo pojo = repositoryManager.createNamedQuery("select 1 id, 'something' value from (values(0))")
            .fetchFirst(SuperPojo.class);

    assertEquals(1, pojo.getId());
    assertEquals("something1", pojo.getValue());
  }

  @Test
  public void testComplexTypes() {
    ComplexEntity pojo = repositoryManager.createNamedQuery(
                    "select 1 id, 1 \"entity.id\", 'something' \"entity.value\" from (values(0))")
            .setName("testComplexTypes")
            .fetchFirst(ComplexEntity.class);

    assertEquals(1, pojo.id);
    assertEquals(1, pojo.getEntity().getId());
    assertEquals("something1", pojo.getEntity().getValue());
  }

//  @Test
//  public void testMultiResult() {
//    jdbcOperations.createQuery("create table multi1(id integer identity primary key, value varchar(20))").executeUpdate();
//    jdbcOperations.createQuery("create table multi2(id integer identity primary key, value2 varchar(20))").executeUpdate();
//
//    jdbcOperations.createQuery("insert into multi1(value) values (:val)")
//            .addParameter("val", "test1").addToBatch()
//            .addParameter("val", "test2").addToBatch()
//            .executeBatch();
//
//    jdbcOperations.createQuery("insert into multi2(value2) values (:val)")
//            .addParameter("val", "test3").addToBatch()
//            .addParameter("val", "test4").addToBatch()
//            .executeBatch();
//
//    List[] results = jdbcOperations.createQuery("select * from multi1 order by id; select * from multi2 order by id")
//            .executeAndFetchMultiple(Multi1.class, Multi2.class);
//    //List<Multi1> results = jdbcOperations.createQuery("select * from multi1 order by id; select * from multi2 order by id").executeAndFetch(Multi1.class);
//
//    List<Multi1> res1 = results[0];
//    List<Multi2> res2 = results[1];
//
//    assertEquals((Long) 1L, res1.get(0).getId());
//    assertEquals("test2", res1.get(1).getValue());
//
//    assertEquals("test3", res2.get(0).getValue2());
//    assertEquals(4, res2.get(1).getId());
//  }

  @Test
  public void testRunInsideTransaction() {

    repositoryManager.createNamedQuery("create table runinsidetransactiontable(id integer identity primary key, value varchar(50))")
            .executeUpdate();
    boolean failed = false;

    try {
      repositoryManager.runInTransaction((StatementRunnable) (connection, argument) -> {
        connection.createNamedQuery("insert into runinsidetransactiontable(value) values(:value)")
                .addParameter("value", "test")
                .executeUpdate();
        throw new RuntimeException("ouch!");
      });
    }
    catch (PersistenceException ex) {
      failed = true;
    }

    assertTrue(failed);
    long rowCount = (Long) repositoryManager.createNamedQuery("select count(*) from runinsidetransactiontable").fetchScalar();
    assertEquals(0, rowCount);

    repositoryManager.runInTransaction((connection, argument) -> {
      connection.createNamedQuery("insert into runinsidetransactiontable(value) values(:value)")
              .addParameter("value", "test")
              .executeUpdate();
    });

    rowCount = (Long) repositoryManager.createNamedQuery("select count(*) from runinsidetransactiontable").fetchScalar();
    assertEquals(1, rowCount);

    String argument = "argument test";

    repositoryManager.runInTransaction((connection, argument1) -> {
      Integer id = connection.createNamedQuery("insert into runinsidetransactiontable(value) values(:value)")
              .addParameter("value", argument1)
              .executeUpdate()
              .getFirstKey(Integer.class);

      String insertedValue = connection.createNamedQuery("select value from runinsidetransactiontable where id = :id")
              .addParameter("id", id)
              .fetchScalar(String.class);
      assertEquals("argument test", insertedValue);
    }, argument);

    rowCount = (Long) repositoryManager.createNamedQuery("select count(*) from runinsidetransactiontable").fetchScalar();
    assertEquals(2, rowCount);
  }

  @Test
  public void testRunInsideTransactionWithResult() {
    repositoryManager.createNamedQuery("create table testRunInsideTransactionWithResultTable(id integer identity primary key, value varchar(50))")
            .executeUpdate();

  }

  private static class runnerWithResultTester implements ResultStatementRunnable<List<Integer>> {

    public List<Integer> run(JdbcConnection connection, Object argument) throws Throwable {
      String[] vals = (String[]) argument;
      List<Integer> keys = new ArrayList<>();
      for (String val : vals) {
        Integer key = connection.createNamedQuery(
                        "insert into testRunInsideTransactionWithResultTable(value) values(:val)",
                        "runnerWithResultTester")
                .addParameter("val", val)
                .executeUpdate()
                .getFirstKey(Integer.class);
        keys.add(key);
      }

      return keys;
    }
  }

  @Test
  public void testDynamicExecuteScalar() {
    Object origVal = repositoryManager.createNamedQuery("select 1").fetchScalar();
    assertEquals(Integer.class, origVal.getClass());
    assertEquals(1, origVal);

    Long intVal = repositoryManager.createNamedQuery("select 1").fetchScalar(Long.class);
    assertEquals((Long) 1l, intVal);

    Short shortVal = repositoryManager.createNamedQuery("select 2").fetchScalar(Short.class);
    Short expected = 2;
    assertEquals(expected, shortVal);
  }

  @Test
  public void testUpdateWithNulls() {
    repositoryManager.createNamedQuery("create table testUpdateWithNulls_2(id integer identity primary key, value integer)").executeUpdate();

    Integer nullInt = null;

    repositoryManager
            .createNamedQuery("insert into testUpdateWithNulls_2(value) values(:val)")
            .addParameter("val", 2)
            .addToBatch()
            .addParameter("val", nullInt)
            .addToBatch()
            .executeBatch();
  }

  @Test
  public void testExceptionInRunnable() {
    repositoryManager.createNamedQuery("create table testExceptionInRunnable(id integer primary key, value varchar(20))").executeUpdate();

    try {
      repositoryManager.runInTransaction((connection, argument) -> {
        connection.createNamedQuery("insert into testExceptionInRunnable(id, value) values(:id, :val)")
                .addParameter("id", 1)
                .addParameter("val", "something").executeUpdate();

        connection.createNamedQuery("insert into testExceptionInRunnable(id, value) values(:id, :val)")
                .addParameter("id", 1)
                .addParameter("val", "something").executeUpdate();
      });
    }
    catch (Throwable ignored) {

    }

    int c = repositoryManager.createNamedQuery("select count(*) from testExceptionInRunnable").fetchScalar(Integer.class);
    assertEquals(0, c);

    repositoryManager.runInTransaction((connection, argument) -> {
      connection.createNamedQuery("insert into testExceptionInRunnable(id, value) values(:id, :val)")
              .addParameter("id", 1)
              .addParameter("val", "something").executeUpdate();

      try {
        connection.createNamedQuery("insert into testExceptionInRunnable(id, value) values(:id, :val)")
                .addParameter("id", 1)
                .addParameter("val", "something").executeUpdate();
      }
      catch (DataAccessException ignored) {

      }
    });

    c = repositoryManager.createNamedQuery("select count(*) from testExceptionInRunnable").fetchScalar(Integer.class);
    assertEquals(1, c);

  }

  public static enum TestEnum {
    HELLO, WORLD;
  }

  public static class EntityWithEnum {
    public int id;
    public TestEnum val;
    @Enumerated
    public TestEnum val2;
  }

  @Test
  public void testEnums() {
    repositoryManager.createNamedQuery("create table EnumTest(id int identity primary key, enum_val varchar(10), enum_val2 int) ").executeUpdate();

    repositoryManager.createNamedQuery("insert into EnumTest(enum_val, enum_val2) values (:val, :val2)")
            .addParameter("val", TestEnum.HELLO).addParameter("val2", TestEnum.HELLO.ordinal()).addToBatch()
            .addParameter("val", TestEnum.WORLD).addParameter("val2", TestEnum.WORLD.ordinal()).addToBatch().executeBatch();

    TestEnum testEnum = repositoryManager.createNamedQuery("select 'HELLO' from (values(0))")
            .fetchScalar(TestEnum.class);
    assertThat(testEnum).isEqualTo(TestEnum.HELLO);

    TestEnum testEnum2 = repositoryManager.createNamedQuery("select NULL from (values(0))")
            .fetchScalar(TestEnum.class);
    assertThat(testEnum2).isNull();

    TypeHandlerRegistry handlerRegistry = new TypeHandlerRegistry();
    repositoryManager.setTypeHandlerRegistry(handlerRegistry);

    List<EntityWithEnum> list = repositoryManager.createNamedQuery("select id, enum_val val, enum_val2 val2 from EnumTest")
            .fetch(EntityWithEnum.class);

    assertThat(list.get(0).val).isEqualTo(TestEnum.HELLO);
    assertThat(list.get(0).val2).isEqualTo(TestEnum.HELLO);
    assertThat(list.get(1).val).isEqualTo(TestEnum.WORLD);
    assertThat(list.get(1).val2).isEqualTo(TestEnum.WORLD);

  }

  public static class BooleanPOJO {
    public boolean val1;
    public Boolean val2;
  }

  @Test
  public void testBooleanConverter() {
    String sql = "select true as val1, false as val2 from (values(0))";

    BooleanPOJO pojo = repositoryManager.createNamedQuery(sql).fetchFirst(BooleanPOJO.class);
    assertTrue(pojo.val1);
    assertFalse(pojo.val2);

    String sql2 = "select null as val1, null as val2 from (values(0))";
    BooleanPOJO pojo2 = repositoryManager.createNamedQuery(sql2).fetchFirst(BooleanPOJO.class);
    assertFalse(pojo2.val1);
    assertNull(pojo2.val2);

    String sql3 = "select 'false' as val1, 'true' as val2 from (values(0))";
    BooleanPOJO pojo3 = repositoryManager.createNamedQuery(sql3).fetchFirst(BooleanPOJO.class);
    assertFalse(pojo3.val1);
    assertTrue(pojo3.val2);
  }

  public static class BlobPOJO1 {
    public int id;
    public byte[] data;
  }

  public static class BlobPOJO2 {
    public int id;
    public InputStream data;
  }

  @Test
  public void testBlob() throws IOException {
    String createSql = "create table blobtbl2(id int identity primary key, data blob)";
    repositoryManager.createNamedQuery(createSql).executeUpdate();

    String dataString = "test";
    byte[] data = dataString.getBytes();
    String insertSql = "insert into blobtbl2(data) values(:data)";
    repositoryManager.createNamedQuery(insertSql).addParameter("data", data).executeUpdate();

    // select
    String sql = "select id, data from blobtbl2";
    BlobPOJO1 pojo1 = repositoryManager.createNamedQuery(sql)
            .fetchFirst(BlobPOJO1.class);

    TypeHandlerRegistry handlerRegistry = new TypeHandlerRegistry();
    handlerRegistry.register(InputStream.class, new BytesInputStreamTypeHandler());
    repositoryManager.setTypeHandlerRegistry(handlerRegistry);

    BlobPOJO2 pojo2 = repositoryManager.createNamedQuery(sql)
            .fetchFirst(BlobPOJO2.class);

    String pojo1DataString = new String(pojo1.data);
    assertThat(dataString).isEqualTo(pojo1DataString);

    InputStream inputStream = pojo2.data;

    String pojo2DataString = StreamUtils.copyToString(inputStream);
//    byte[] pojo2Data = IOUtils.toByteArray(pojo2.data);
//    String pojo2DataString = new String(pojo2Data);
    assertThat(dataString).isEqualTo(pojo2DataString);
  }

  @Test
  public void testInputStream() throws IOException {
    String createSql = "create table blobtbl(id int identity primary key, data blob)";
    repositoryManager.createNamedQuery(createSql).executeUpdate();

    String dataString = "test";
    byte[] data = dataString.getBytes();

    InputStream inputStream = new ByteArrayInputStream(data);

    String insertSql = "insert into blobtbl(data) values(:data)";
    repositoryManager.createNamedQuery(insertSql).addParameter("data", inputStream).executeUpdate();

    // select
    String sql = "select id, data from blobtbl";
    BlobPOJO1 pojo1 = repositoryManager.createNamedQuery(sql).fetchFirst(BlobPOJO1.class);
    BlobPOJO2 pojo2 = repositoryManager.createNamedQuery(sql).fetchFirst(BlobPOJO2.class);

    String pojo1DataString = new String(pojo1.data);
    assertThat(dataString).isEqualTo(pojo1DataString);

    byte[] pojo2Data = IOUtils.toByteArray(pojo2.data);
    String pojo2DataString = new String(pojo2Data);
    assertThat(dataString).isEqualTo(pojo2DataString);
  }

  @Test
  public void testTimeConverter() {
    String sql = "select current_time as col1 from (values(0))";

    Time sqlTime = repositoryManager.createNamedQuery(sql).fetchScalar(Time.class);

    Period p = new Period(new LocalTime(sqlTime), new LocalTime());

    assertThat(sqlTime).isNotNull();
    assertEquals(0, p.getMinutes());

    Date date = repositoryManager.createNamedQuery(sql)
            .fetchScalar(Date.class);
    assertThat(date).isNotNull();

    LocalTime jodaTime = repositoryManager.createNamedQuery(sql)
            .fetchScalar(LocalTime.class);
    assertTrue(jodaTime.getMillisOfDay() > 0);
    assertThat(jodaTime.getHourOfDay()).isEqualTo(new LocalTime().getHourOfDay());
  }

  public static class BindablePojo {
    String data1;
    private Timestamp data2;
    private Long data3;
    private Float data4;

    public Timestamp getData2() {
      return data2;
    }

    public Long getData3() {
      return data3;
    }

    public Float getData4() {
      return data4;
    }

    public void setData2(Timestamp data2) {
      this.data2 = data2;
    }

    public void setData3(Long data3) {
      this.data3 = data3;
    }

    public void setData4(Float data4) {
      this.data4 = data4;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof BindablePojo other) {
        /*System.out.println(data1 + " == " + other.data1);
                System.out.println(data2 + " == " + other.data2);
                System.out.println(data3 + " == " + other.data3);*/
        return data1.equals(other.data1) && data2.equals(other.data2) && data3.equals(other.data3);
      }
      else
        return false;
    }

  }

  @Test
  public void testBindPojo() {
    String createSql = "create table bindtbl(id int identity primary key, data1 varchar(10), data2 timestamp, data3 bigint)";
    repositoryManager.createNamedQuery(createSql).executeUpdate();

    // Anonymous class inherits POJO
    BindablePojo pojo1 = new BindablePojo() {
      {
        // Field access
        data1 = "Foo";
        setData2(new Timestamp(new Date().getTime()));
        setData3(789456123L);
        setData4(4.5f);
      }
    };

    String insertSql = "insert into bindtbl(data1, data2, data3) values(:data1, :data2, :data3)";
    repositoryManager.createNamedQuery(insertSql)
            .bind(pojo1)
            .executeUpdate();

    String selectSql = "select data1, data2, data3 from bindtbl";
    BindablePojo pojo2 = repositoryManager.createNamedQuery(selectSql)
            .fetchFirst(BindablePojo.class);

    assertEquals(pojo1, pojo2);
  }

  @Test
  public void testRowGetObjectWithConverters() {
    String sql = "select 1 col1, '23' col2 from (values(0))";
    Table t = repositoryManager.createNamedQuery(sql).fetchTable();
    Row r = t.rows().get(0);

    String col1AsString = r.getObject("col1", String.class);
    Integer col1AsInteger = r.getObject("col1", Integer.class);
    Long col1AsLong = r.getObject("col1", Long.class);

    assertThat(col1AsString).isEqualTo("1");
    assertThat(col1AsInteger).isEqualTo(1);
    assertThat(col1AsLong).isEqualTo(1L);

    String col2AsString = r.getObject("col2", String.class);
    Integer col2AsInteger = r.getObject("col2", Integer.class);
    Long col2AsLong = r.getObject("col2", Long.class);

    assertThat(col2AsString).isEqualTo("23");
    assertThat(col2AsInteger).isEqualTo(23);
    assertThat(col2AsLong).isEqualTo(23L);
  }

  @Test
  public void testExecuteAndFetchLazy() {
    createAndFillUserTable();

    ResultSetIterable<User> allUsers = repositoryManager.createNamedQuery("select * from User").fetchIterable(User.class);

    // read in batches, because maybe we are bulk exporting and can't fit them all into a list
    int totalSize = 0;
    int batchSize = 500;
    List<User> batch = new ArrayList<>(batchSize);
    for (User u : allUsers) {
      totalSize++;
      if (batch.size() == batchSize) {
        System.out.printf("Read batch of %d users, great!%n", batchSize);
        batch.clear();
      }
      batch.add(u);
    }

    allUsers.close();

    assertEquals(totalSize, insertIntoUsers);
    deleteUserTable();
  }

  @Test
  public void testResultSetIterator_multipleHasNextWorks() {
    createAndFillUserTable();

    ResultSetIterable<User> allUsers = repositoryManager.createNamedQuery("select * from User").fetchIterable(User.class);

    Iterator<User> usersIterator = allUsers.iterator();

    // call hasNext a few times, should have no effect
    usersIterator.hasNext();
    usersIterator.hasNext();
    usersIterator.hasNext();

    int totalSize = 0;
    while (usersIterator.hasNext()) {
      totalSize++;
      usersIterator.next();
    }

    allUsers.close();

    assertEquals(totalSize, insertIntoUsers);
    deleteUserTable();
  }

  @Test
  public void testExecuteAndFetch_fallbackToExecuteScalar() {
    createAndFillUserTable();

    // this should NOT fallback to executeScalar
    List<User> users = repositoryManager.createNamedQuery("select name from User").fetch(User.class);

    // only the name should be set
    for (User u : users) {
      assertNotNull(u.name);
    }

    // this SHOULD fallback to executeScalar
    List<String> userNames = repositoryManager.createNamedQuery("select name from User").fetch(String.class);

    assertEquals(users.size(), userNames.size());

    deleteUserTable();
  }

  @Test
  public void testExecuteAndFetchWithAutoclose() throws SQLException {
    createAndFillUserTable();

    JdbcConnection con = repositoryManager.open();

    try (ResultSetIterable<User> userIterable = con.createNamedQuery("select * from User")
            .fetchIterable(User.class)) {

      userIterable.setAutoCloseConnection(true);

      for (User u : userIterable) {
        assertThat(u.getEmail()).isNotNull();
      }
    }

    assertTrue(con.getJdbcConnection().isClosed());

  }

  @Test
  public void testLazyTable() throws SQLException {
    createAndFillUserTable();

    NamedQuery q = repositoryManager.createNamedQuery("select * from User");
    try (LazyTable lt = q.fetchLazyTable()) {
      for (Row r : lt.rows()) {
        String name = r.getString("name");

        assertThat(name).isNotNull();
      }

      // still in autoClosable scope. Expecting connection to be open.
      assertThat(q.getConnection().getJdbcConnection().isClosed()).isFalse();
    }
    // simulate autoClose.
    // simulated autoClosable scope exited. Expecting connection to be closed.
    assertThat(q.getConnection().getJdbcConnection().isClosed()).isTrue();
  }

  @Test
  public void testTransactionAutoClosable() {

    repositoryManager.createNamedQuery("create table testTransactionAutoClosable(id int primary key, val varchar(20) not null)").executeUpdate();

    JdbcConnection connection = null;
    try {
      connection = repositoryManager.beginTransaction();
      String sql = "insert into testTransactionAutoClosable(id, val) values (:id, :val);";
      connection.createNamedQuery(sql).addParameter("id", 1).addParameter("val", "foo").executeUpdate();
    }
    finally {
      // autoclosing
      connection.close();
    }

    int count = repositoryManager.createNamedQuery("select count(*) from testTransactionAutoClosable").fetchFirst(Integer.class);
    assertThat(count).isEqualTo(0);

    connection = null;
    try {
      connection = repositoryManager.beginTransaction();
      String sql = "insert into testTransactionAutoClosable(id, val) values (:id, :val);";
      connection.createNamedQuery(sql).addParameter("id", 1).addParameter("val", "foo").executeUpdate();

      connection.commit();
    }
    finally {
      // autoclosing
      connection.close();
    }

    count = repositoryManager.createNamedQuery("select count(*) from testTransactionAutoClosable").fetchFirst(Integer.class);
    assertThat(count).isEqualTo(1);

  }

  @Test
  public void testExternalTransactionCommit() {

    try (JdbcConnection connection1 = repositoryManager.open()) {
      connection1.createNamedQuery("create table testExternalTransactionCommit(id int primary key, val varchar(20) not null)")
              .executeUpdate();
    }

    try (JdbcConnection globalConnection = repositoryManager.beginTransaction()) {
      Connection globalTransaction = globalConnection.getJdbcConnection();

      JdbcConnection connection = repositoryManager.beginTransaction(globalTransaction);
      String sql = "insert into testExternalTransactionCommit(id, val) values (:id, :val);";
      connection.createNamedQuery(sql)
              .addParameter("id", 1)
              .addParameter("val", "foo")
              .executeUpdate();
      connection.commit();

      int count = globalConnection.createNamedQuery("select count(*) from testExternalTransactionCommit")
              .fetchFirst(Integer.class);
      assertThat(count).isEqualTo(1);

      JdbcConnection connection3 = repositoryManager.beginTransaction(globalTransaction);
      String sql1 = "insert into testExternalTransactionCommit(id, val) values (:id, :val);";
      connection3.createNamedQuery(sql1)
              .addParameter("id", 2)
              .addParameter("val", "bar")
              .executeUpdate();
      connection3.commit();

      int count1 = globalConnection.createNamedQuery("select count(*) from testExternalTransactionCommit")
              .fetchFirst(Integer.class);
      assertThat(count1).isEqualTo(2);

      globalConnection.commit();
    }

    try (JdbcConnection connection2 = repositoryManager.open()) {
      int count = connection2.createNamedQuery("select count(*) from testExternalTransactionCommit")
              .fetchFirst(Integer.class);

      assertThat(count).isEqualTo(2);
    }

  }

  @Test
  @Ignore
  public void testExternalTransactionRollback() {

    try (JdbcConnection connection1 = repositoryManager.open()) {
      connection1.createNamedQuery("create table testExternalTransactionRollback(id int primary key, val varchar(20) not null)")
              .executeUpdate();
    }

    try (JdbcConnection globalConnection = repositoryManager.beginTransaction(Connection.TRANSACTION_SERIALIZABLE)) {
      Connection globalTransaction = globalConnection.getJdbcConnection();

      JdbcConnection connection = repositoryManager.beginTransaction(globalTransaction);
      String sql = "insert into testExternalTransactionRollback(id, val) values (:id, :val);";
      connection.createNamedQuery(sql).addParameter("id", 1).addParameter("val", "foo").executeUpdate();
      connection.commit();

      JdbcConnection connection2 = repositoryManager.open(globalTransaction);
      int count = connection2.createNamedQuery("select count(*) from testExternalTransactionRollback")
              .fetchFirst(Integer.class);
      assertThat(count).isEqualTo(1);

      JdbcConnection connection3 = repositoryManager.beginTransaction(globalTransaction);
      String sql2 = "insert into testExternalTransactionRollback(id, val) values (:id, :val);";
      connection3.createNamedQuery(sql2)
              .addParameter("id", 2)
              .addParameter("val", "bar")
              .executeUpdate();
      connection3.commit();

      JdbcConnection connection4 = repositoryManager.open(globalTransaction);
      int count1 = connection4.createNamedQuery("select count(*) from testExternalTransactionRollback")
              .fetchFirst(Integer.class);
      assertThat(count1).isEqualTo(2);
      globalConnection.rollback();
    }

    try (JdbcConnection connection2 = repositoryManager.open()) {
      int count = connection2.createNamedQuery("select count(*) from testExternalTransactionRollback")
              .fetchFirst(Integer.class);

      assertThat(count).isEqualTo(0);
    }

  }

  @Test
  public void testOpenConnection() throws SQLException {

    JdbcConnection connection = repositoryManager.open();

    createAndFillUserTable(connection);

    assertThat(connection.getJdbcConnection().isClosed()).isFalse();

    List<User> users = connection.createNamedQuery("select * from User").fetch(User.class);

    assertThat(users.size()).isEqualTo(NUMBER_OF_USERS_IN_THE_TEST);
    assertThat(connection.getJdbcConnection().isClosed()).isFalse();

    connection.close();

    assertThat(connection.getJdbcConnection().isClosed()).isTrue();
  }

  @Test
  public void testWithConnection() {

    createAndFillUserTable();

    String insertsql = "insert into User(name, email, text) values (:name, :email, :text)";

    repositoryManager.withConnection((connection, argument) -> {

      connection.createNamedQuery(insertsql)
              .addParameter("name", "Sql2o")
              .addParameter("email", "sql2o@sql2o.org")
              .addParameter("text", "bla bla")
              .executeUpdate();

      connection.createNamedQuery(insertsql)
              .addParameter("name", "Sql2o2")
              .addParameter("email", "sql2o@sql2o.org")
              .addParameter("text", "bla bla")
              .executeUpdate();

      connection.createNamedQuery(insertsql)
              .addParameter("name", "Sql2o3")
              .addParameter("email", "sql2o@sql2o.org")
              .addParameter("text", "bla bla")
              .executeUpdate();

    });

    List<User> users = repositoryManager.withConnection((connection, argument) -> {
      return repositoryManager.createNamedQuery("select * from User").fetch(User.class);
    });

    assertThat(users.size()).isEqualTo(10003);

    try {
      repositoryManager.withConnection((StatementRunnable) (connection, argument) -> {
        connection.createNamedQuery(insertsql)
                .addParameter("name", "Sql2o")
                .addParameter("email", "sql2o@sql2o.org")
                .addParameter("text", "bla bla")
                .executeUpdate();

        throw new RuntimeException("whaa!");
      });
    }
    catch (Exception e) {
      // ignore. expected
    }

    List<User> users2 = repositoryManager.createNamedQuery("select * from User").fetch(User.class);

    // expect that that the last insert was committed, as this should not be run in a transaction.
    assertThat(users2.size()).isEqualTo(10004);
  }

  static class LocalPojo {
    private long idVal;
    private String anotherVeryExcitingValue;

    public long getIdVal() {
      return idVal;
    }

    public String getAnotherVeryExcitingValue() {
      return anotherVeryExcitingValue;
    }

    public void setAnotherVeryExcitingValue(String anotherVeryExcitingValue) {
      this.anotherVeryExcitingValue = anotherVeryExcitingValue;
    }

  }

  @Test
  public void testAutoDeriveColumnNames() {
    String createTableSql = "create table testAutoDeriveColumnNames (id_val integer primary key, another_very_exciting_value varchar(20))";
    String insertSql = "insert into testAutoDeriveColumnNames values (:id, :val)";
    String selectSql = "select * from testAutoDeriveColumnNames";

    try (JdbcConnection con = repositoryManager.open()) {
      con.createNamedQuery(createTableSql).executeUpdate();
      con.createNamedQuery(insertSql).addParameter("id", 1).addParameter("val", "test1").executeUpdate();

      Exception ex = null;
      try {
        // expected to fail, as autoDeriveColumnNames are not set
        con.createNamedQuery(selectSql)
                .setAutoDerivingColumns(false)
                .fetchFirst(LocalPojo.class);
      }
      catch (Exception e) {
        ex = e;
      }

      assertNotNull(ex);

      LocalPojo p = con.createNamedQuery(selectSql)
              .setAutoDerivingColumns(true)
              .fetchFirst(LocalPojo.class);

      assertNotNull(p);
      assertEquals(1, p.getIdVal());
      assertEquals("test1", p.getAnotherVeryExcitingValue());

    }
  }

  @Test
  public void testClob() {
    try (JdbcConnection connection = repositoryManager.open()) {
      connection.createNamedQuery("create table testClob(id integer primary key, val clob)")
              .executeUpdate();

      connection.createNamedQuery("insert into testClob (id, val) values (:id, :val)")
              .addParameter("id", 1)
              .addParameter("val", "something")
              .executeUpdate();

      String val = connection.createNamedQuery("select val from testClob where id = :id")
              .addParameter("id", 1)
              .fetchScalar(String.class);

      assertThat(val).isEqualTo("something");
    }

  }

  @Test
  public void testBindInIteration() {
    try (JdbcConnection connection = repositoryManager.open()) {
      createAndFillUserTable(connection, true);
      genericTestOnUserData(connection);
    }

  }

  @Test
  public void testArrayParameter() {
    createAndFillUserTable();

    try (JdbcConnection connection = repositoryManager.open()) {
      List<User> result = connection
              .createNamedQuery("select * from user where id in(:ids)")
              .addParameters("ids", 1, 2, 3)
              .fetch(User.class);

      assertEquals(3, result.size());
    }

    try (JdbcConnection connection = repositoryManager.open()) {
      List<User> result = connection
              .createNamedQuery("select * from user where" +
                      " email like :email" +
                      " and id in(:ids)" +
                      " and text = :text")
              .addParameter("email", "%email.com")
              .addParameters("ids", 1, 2, 3)
              .addParameter("text", "some text")
              .fetch(User.class);

      assertEquals(3, result.size());
    }

    try (JdbcConnection connection = repositoryManager.open()) {
      List<User> result = connection
              .createNamedQuery("select * from user where" +
                      " email like :email" +
                      " and id in(:ids)" +
                      " and text = :text")
              .addParameter("email", "%email.com")
              .addParameter("ids", ImmutableList.of())
              .addParameter("text", "some text")
              .fetch(User.class);

      assertEquals(0, result.size());
    }

    try (JdbcConnection connection = repositoryManager.open()) {
      List<User> result = connection
              .createNamedQuery("select * from user where" +
                      " email like :email" +
                      " and id in(:ids)" +
                      " and text = :text")
              .addParameter("email", "%email.com")
              .addParameter("ids", ImmutableList.of(1))
              .addParameter("text", "some text")
              .fetch(User.class);

      assertEquals(1, result.size());
    }

    try (JdbcConnection connection = repositoryManager.open()) {
      List<User> result = connection
              .createNamedQuery("select * from user where" +
                      " email like :email" +
                      " and text = :text" +
                      " and id in(:ids)" +
                      " and text = :text" +
                      " and id in(:ids)")
              .addParameter("email", "%email.com")
              .addParameters("ids", 1, 2, 3)
              .addParameter("text", "some text")
              .fetch(User.class);

      assertEquals(3, result.size());
    }

    try (JdbcConnection connection = repositoryManager.open()) {
      connection.createNamedQuery("insert into user (id, text_col) values(:id, :text)")
              .addParameters("id", 1, 2, 3).addParameter("text", "test1").addToBatch();
      fail("Batch with array parameter is not supported");
    }
    catch (PersistenceException e) {
      // as expected
    }

    try (JdbcConnection connection = repositoryManager.open()) {
      List<User> result = connection
              .createNamedQuery("select * from user where id in(:ids)")
              .addParameter("ids", new int[]
                      { 1, 2, 3 })
              .fetch(User.class);

      assertEquals(3, result.size());
    }

    try (JdbcConnection connection = repositoryManager.open()) {
      List<User> result = connection
              .createNamedQuery("select * from user where id in(:ids)")
              .addParameter("ids", (Object) ImmutableList.of(1, 2, 3))
              .fetch(User.class);

      assertEquals(3, result.size());
    }
  }

  @Test
  public void testExecuteUpdate() {
    try (JdbcConnection connection = repositoryManager.open()) {
      connection.createNamedQuery("create table test_(id int identity primary key,  val int)")
              .executeUpdate();

      UpdateResult<Integer> updateResult = connection.createNamedQuery("insert into test_ (val) values (:val)")
              .addParameter("val", 2)
              .executeUpdate();

      Integer id = updateResult.getFirstKey();
      assertThat(id).isNotNull();

    }

  }

  /************** Helper stuff ******************/

  private void createAndFillUserTable() {
    JdbcConnection connection = repositoryManager.open();

    createAndFillUserTable(connection);

    connection.close();
  }

  private void createAndFillUserTable(JdbcConnection connection) {
    createAndFillUserTable(connection, false);
  }

  private void createAndFillUserTable(JdbcConnection connection, boolean useBind) {
    createAndFillUserTable(connection, useBind, 0);
  }

  private void createAndFillUserTable(JdbcConnection connection, boolean useBind, int maxBatchRecords) {

    try {
      connection.createNamedQuery("drop table User").executeUpdate();
    }
    catch (Exception e) {
      // if it fails, its because the User table doesn't exists. Just ignore this.
    }

    int rowCount = NUMBER_OF_USERS_IN_THE_TEST;
    connection.createNamedQuery(
            """
                    create table User(
                    id int identity primary key,
                    name varchar(20),
                    email varchar(255),
                    text varchar(100))""").executeUpdate();

    NamedQuery insQuery = connection.createNamedQuery("insert into User(name, email, text) values (:name, :email, :text)");
    insQuery.setMaxBatchRecords(maxBatchRecords);
    UserInserter inserter = UserInserterFactory.buildUserInserter(useBind);

    Date before = new Date();
    for (int idx = 0; idx < rowCount; idx++) {
      inserter.insertUser(insQuery, idx);
    }

        /*
         This check is required because the HSQL jdbc implementation
         throws an exception if executeBatch is called without anything added to the batch.
        */
    if (insQuery.isExplicitExecuteBatchRequired()) {
      insQuery.executeBatch();
    }
    Date after = new Date();
    Long span = after.getTime() - before.getTime();

    System.out.printf("inserted %d rows into User table. Time used: %s ms%n", rowCount, span);

    insertIntoUsers += rowCount;

  }

  private void genericTestOnUserData(JdbcConnection connection) {
    List<User> users = connection.createNamedQuery("select * from User order by id").fetch(User.class);

    assertThat(users.size()).isEqualTo(NUMBER_OF_USERS_IN_THE_TEST);

    for (int idx = 0; idx < NUMBER_OF_USERS_IN_THE_TEST; idx++) {
      User user = users.get(idx);
      assertThat("a name " + idx).isEqualTo(user.name);
      assertThat(String.format("test%s@email.com", idx)).isEqualTo(user.getEmail());
    }
  }

  private void deleteUserTable() {
    repositoryManager.createNamedQuery("drop table User").executeUpdate();
    insertIntoUsers = 0;
  }
}
