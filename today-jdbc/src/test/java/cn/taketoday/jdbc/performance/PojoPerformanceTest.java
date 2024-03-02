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

package cn.taketoday.jdbc.performance;

import com.zaxxer.hikari.HikariDataSource;

import org.apache.commons.dbutils.BasicRowProcessor;
import org.apache.commons.dbutils.BeanProcessor;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.ResultQuery;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.sql2o.Sql2o;
import org.sql2o.quirks.NoQuirks;
import org.teasoft.bee.osql.BeeSql;
import org.teasoft.bee.osql.Suid;
import org.teasoft.honey.osql.core.BeeFactory;

import java.beans.PropertyDescriptor;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Primary;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.jdbc.JdbcConnection;
import cn.taketoday.jdbc.NamedQuery;
import cn.taketoday.jdbc.RepositoryManager;
import cn.taketoday.stereotype.Singleton;
import lombok.SneakyThrows;

/**
 * @author aldenquimby@gmail.com
 * <p>
 * TODO: must read 10-100 rows instead 1
 */
@Disabled
public class PojoPerformanceTest {

  private final static String DRIVER_CLASS = "org.h2.Driver";
  private final static String DB_URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=MySQL";
  private final static String DB_USER = "sa";
  private final static String DB_PASSWORD = "";
  private final static String HIBERNATE_DIALECT = "org.hibernate.dialect.H2Dialect";
  private final static SQLDialect JOOQ_DIALECT = SQLDialect.H2;
  //  private final int ITERATIONS = 50000;
  private final int ITERATIONS = 1000;

  private RepositoryManager operations;

  @BeforeEach
  public void setup() {
    Logger.getLogger("org.hibernate").setLevel(Level.OFF);

    operations = new RepositoryManager(DB_URL, DB_USER, DB_PASSWORD);

    createPostTable();

  }

  private void createPostTable() {
    // language=MySQL
    operations.createNamedQuery("DROP TABLE IF EXISTS post").executeUpdate();
    // language=MySQL
    operations.createNamedQuery("\n CREATE TABLE post" +
            "\n (" +
            "\n     id INT NOT NULL IDENTITY PRIMARY KEY" +
            "\n   , text VARCHAR(255)" +
            "\n   , creation_date DATETIME" +
            "\n   , last_change_date DATETIME" +
            "\n   , counter1 INT" +
            "\n   , counter2 INT" +
            "\n   , counter3 INT" +
            "\n   , counter4 INT" +
            "\n   , counter5 INT" +
            "\n   , counter6 INT" +
            "\n   , counter7 INT" +
            "\n   , counter8 INT" +
            "\n   , counter9 INT" +
            "\n )" +
            "\n;").executeUpdate();

    Random r = new Random();

    NamedQuery insQuery = operations.createNamedQuery(
            "insert into post (text, creation_date, last_change_date, counter1, counter2, counter3, counter4, counter5, counter6, counter7, counter8, counter9) values (:text, :creation_date, :last_change_date, :counter1, :counter2, :counter3, :counter4, :counter5, :counter6, :counter7, :counter8, :counter9)");
    for (int idx = 0; idx < ITERATIONS; idx++) {
      insQuery.addParameter("text", "a name " + idx)
              .addParameter("creation_date", new Date(System.currentTimeMillis() + r.nextInt()))
              .addParameter("last_change_date", new Date(System.currentTimeMillis() + r.nextInt()))
              .addParameter("counter1", r.nextDouble() > 0.5 ? r.nextInt() : null)
              .addParameter("counter2", r.nextDouble() > 0.5 ? r.nextInt() : null)
              .addParameter("counter3", r.nextDouble() > 0.5 ? r.nextInt() : null)
              .addParameter("counter4", r.nextDouble() > 0.5 ? r.nextInt() : null)
              .addParameter("counter5", r.nextDouble() > 0.5 ? r.nextInt() : null)
              .addParameter("counter6", r.nextDouble() > 0.5 ? r.nextInt() : null)
              .addParameter("counter7", r.nextDouble() > 0.5 ? r.nextInt() : null)
              .addParameter("counter8", r.nextDouble() > 0.5 ? r.nextInt() : null)
              .addParameter("counter9", r.nextDouble() > 0.5 ? r.nextInt() : null)
              .addToBatch();
    }
    insQuery.executeBatch();
  }

  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  @Test
  public void select() throws SQLException {
    System.out.println("Running " + ITERATIONS + " iterations that load up a Post entity\n");

    PerformanceTestList tests = new PerformanceTestList();

//    tests.add(new HibernateTypicalSelect());

    tests.add(new BeeSelect());
    tests.add(new TODAYTypicalSelect());
    tests.add(new TODAYOptimizedSelect());
    tests.add(new HandCodedSelect());
    tests.add(new Sql2oTypicalSelect());
    tests.add(new Sql2oOptimizedSelect());

    tests.add(new JDBISelect());
    tests.add(new JOOQSelect());
    tests.add(new ApacheDbUtilsTypicalSelect());
    tests.add(new MyBatisSelect());
//    tests.add(new SpringJdbcTemplateSelect());

    System.out.println("Warming up...");
    tests.run(ITERATIONS);
    System.out.println("Done warming up, let's rock and roll!\n");

    tests.run(ITERATIONS);
    tests.printResults("Select");
  }

  //----------------------------------------
  //          performance tests
  // ---------------------------------------

  // TODO I think we should consider making it a REQUIREMENT for the performance tests
  // that underscore case be mapped to camel case because it is so common.
  // This would allow us to remove the "optimized" sql2o select, which is not really
  // too different from the "typical" one...
  // If not, maybe we should entirely break out the underscore case mapping tests into a different
  // section in the readme...

  final static String SELECT_TYPICAL = "SELECT * FROM post";
  final static String SELECT_OPTIMAL = "SELECT id, text, creation_date as creationDate, last_change_date as lastChangeDate, counter1, counter2, counter3, counter4, counter5, counter6, counter7, counter8, counter9 FROM post";

  /**
   * Considered "optimized" because it uses {@link #SELECT_OPTIMAL} rather than
   * auto-mapping underscore case to camel case.
   */
  class TODAYOptimizedSelect extends PerformanceTestBase {
    private JdbcConnection conn;
    private NamedQuery query;

    @Override
    public void init() {
      conn = operations.open();

      query = conn.createNamedQuery(SELECT_OPTIMAL + " WHERE id = :id");
      query.setAutoDerivingColumns(true);
    }

    @Override
    public void run(int input) {
      query.addParameter("id", input)
              .fetchFirst(Post.class);
    }

    @Override
    public void close() {
      conn.close();
    }
  }

  class TODAYTypicalSelect extends PerformanceTestBase {
    private JdbcConnection conn;
    private NamedQuery query;

    @Override
    public void init() {
      conn = operations.open();

      query = conn.createNamedQuery(SELECT_TYPICAL + " WHERE id = :id")
              .setAutoDerivingColumns(true);
    }

    @Override
    public void run(int input) {
      query.addParameter("id", input)
              .fetchFirst(Post.class);
    }

    @Override
    public void close() {
      conn.close();
    }
  }

  static class BeeSelect extends PerformanceTestBase {
    Suid suid;
    BeeSql beeSql;

    @Override
    public void init() {
      suid = BeeFactory.getHoneyFactory().getSuid();
      beeSql = BeeFactory.getHoneyFactory().getBeeSql();
    }

    @Override
    public void run(int input) {
//      final Post post = new Post();
//      post.id = input;
//      List<Post> list1 = suid.select(post);

      beeSql.select(SELECT_TYPICAL + " WHERE id = " + input);

    }

    @Override
    public void close() {
    }
  }

  static class Sql2oTypicalSelect extends PerformanceTestBase {
    private org.sql2o.Connection conn;
    private org.sql2o.Query query;
    Sql2o sql2o = new Sql2o(DB_URL, DB_USER, DB_PASSWORD, new NoQuirks());

    @Override
    public void init() {
      conn = sql2o.open();
      query = conn.createQuery(SELECT_TYPICAL + " WHERE id = :id")
              .setAutoDeriveColumnNames(true)
      ;
    }

    @Override
    public void run(int input) {
      query.addParameter("id", input)
              .executeAndFetchFirst(Post.class);
    }

    @Override
    public void close() {
      conn.close();
    }
  }

  /**
   * Considered "optimized" because it uses {@link #SELECT_OPTIMAL} rather than
   * auto-mapping underscore case to camel case.
   */
  static class Sql2oOptimizedSelect extends PerformanceTestBase {
    private org.sql2o.Connection conn;
    private org.sql2o.Query query;
    Sql2o sql2o = new Sql2o(DB_URL, DB_USER, DB_PASSWORD, new NoQuirks());

    @Override
    public void init() {
      conn = sql2o.open();
      query = conn.createQuery(SELECT_OPTIMAL + " WHERE id = :id");
    }

    @Override
    public void run(int input) {
      query.addParameter("id", input)
              .executeAndFetchFirst(Post.class);
    }

    @Override
    public void close() {
      conn.close();
    }
  }

  /**
   * It appears JDBI does not support mapping underscore to camel case.
   */
  static class JDBISelect extends PerformanceTestBase {
    Handle h;
    org.skife.jdbi.v2.Query<Post> q;

    @Override
    public void init() {
      DBI dbi = new DBI(DB_URL, DB_USER, DB_PASSWORD);
      h = dbi.open();
      q = h.createQuery(SELECT_OPTIMAL + " WHERE id = :id").map(Post.class);
    }

    @Override
    public void run(int input) {
      q.bind("id", input).first();
    }

    @Override
    public void close() {
      h.close();
    }
  }

  /**
   * TODO can this be optimized?
   */
  static class JOOQSelect extends PerformanceTestBase {
    ResultQuery q;

    public void init() {
      DSLContext create;
      try {
        create = DSL.using(DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD), JOOQ_DIALECT);
      }
      catch (SQLException e) {
        throw new RuntimeException("Error initializing jOOQ DSLContext", e);
      }

      q = create.select()
              .from("post")
              .where("id = ?", -1) // param needs an initial value, else future calls the bind() will fail.
              .keepStatement(true);
    }

    @Override
    public void run(int input) {
      Record r = q.bind(1, input).fetchOne();
      Post p2 = r.into(Post.class); // TODO this fails for some unknown reason
    }

    @Override
    public void close() {
      q.close();
    }
  }

  class HandCodedSelect extends PerformanceTestBase {
    private Connection conn = null;
    private PreparedStatement stmt = null;

    @Override
    public void init() {
      try {
        conn = operations.open().getJdbcConnection();
        stmt = conn.prepareStatement(SELECT_TYPICAL + " WHERE id = ?");
      }
      catch (SQLException se) {
        throw new RuntimeException("error when executing query", se);
      }
    }

    private Integer getNullableInt(ResultSet rs, String colName) throws SQLException {
      Object obj = rs.getObject(colName);
      return obj == null ? null : (Integer) obj;
    }

    @Override
    public void run(int input) {
      ResultSet rs = null;

      try {
        stmt.setInt(1, input);

        rs = stmt.executeQuery();

        while (rs.next()) {
          Post p = new Post();
          p.setId(rs.getInt("id"));
          p.setText(rs.getString("text"));
          p.setCreationDate(rs.getDate("creation_date"));
          p.setLastChangeDate(rs.getDate("last_change_date"));
          p.setCounter1(getNullableInt(rs, "counter1"));
          p.setCounter2(getNullableInt(rs, "counter2"));
          p.setCounter3(getNullableInt(rs, "counter3"));
          p.setCounter4(getNullableInt(rs, "counter4"));
          p.setCounter5(getNullableInt(rs, "counter5"));
          p.setCounter6(getNullableInt(rs, "counter6"));
          p.setCounter7(getNullableInt(rs, "counter7"));
          p.setCounter8(getNullableInt(rs, "counter8"));
          p.setCounter9(getNullableInt(rs, "counter9"));
        }
      }
      catch (SQLException e) {
        throw new RuntimeException("error when executing query", e);
      }
      finally {
        if (rs != null) {
          try {
            rs.close();
          }
          catch (SQLException e) {
            e.printStackTrace();
          }
        }
      }
    }

    public void close() {
      if (stmt != null) {
        try {
          stmt.close();
        }
        catch (SQLException e) {
          e.printStackTrace();
        }
      }
      if (conn != null) {
        try {
          conn.close();
        }
        catch (SQLException e) {
          e.printStackTrace();
        }
      }
    }
  }

  @Configuration
  static class DataSourceConfig {

    @Primary
    @Singleton(destroyMethod = "close")
    public DataSource h2DataSource() {
      final HikariDataSource hikariDataSource = new HikariDataSource();
      hikariDataSource.setPassword(DB_PASSWORD);
      hikariDataSource.setUsername(DB_USER);
      hikariDataSource.setDriverClassName(DRIVER_CLASS);
      hikariDataSource.setJdbcUrl(DB_URL);
      return hikariDataSource;
    }
  }

  static class HibernateTypicalSelect extends PerformanceTestBase {
    private Session session;
    private AnnotationConfigApplicationContext context;

    @SneakyThrows
    @Override
    public void init() {
      Logger.getLogger("org.hibernate").setLevel(Level.OFF);
      context = new AnnotationConfigApplicationContext();

      context.scan("cn.taketoday.jdbc.performance");

      SessionFactory sessionFactory = context.getBean(SessionFactory.class);
      session = sessionFactory.openSession();
    }

    @Override
    public void run(int input) {
      session.get(Post.class, input);
    }

    @Override
    public void close() {
      session.close();
      if (context != null) {
        context.close();
      }
    }
  }

  class ApacheDbUtilsTypicalSelect extends PerformanceTestBase {
    private QueryRunner runner;
    private org.apache.commons.dbutils.ResultSetHandler<Post> rsHandler;
    private Connection conn;

    /**
     * This class handles mapping "first_name" column to "firstName" property. It
     * looks worse than it is, most is copied from
     * {@link org.apache.commons.dbutils.BeanProcessor} and many people complain
     * online that this isn't built in to Apache DbUtils yet.
     */
    class IgnoreUnderscoreBeanProcessor extends BeanProcessor {
      @Override
      protected int[] mapColumnsToProperties(ResultSetMetaData md, PropertyDescriptor[] props) throws SQLException {
        int cols = md.getColumnCount();
        int[] columnToProperty = new int[cols + 1];
        Arrays.fill(columnToProperty, BeanProcessor.PROPERTY_NOT_FOUND);

        for (int col = 1; col <= cols; col++) {
          String columnName = md.getColumnLabel(col);
          if (null == columnName || 0 == columnName.length()) {
            columnName = md.getColumnName(col);
          }
          String noUnderscoreColName = columnName.replace("_", ""); // this is the addition from BeanProcessor
          for (int i = 0; i < props.length; i++) {
            if (noUnderscoreColName.equalsIgnoreCase(props[i].getName())) {
              columnToProperty[col] = i;
              break;
            }
          }
        }

        return columnToProperty;
      }
    }

    @Override
    public void init() {
      runner = new QueryRunner();
      rsHandler = new BeanHandler<>(Post.class, new BasicRowProcessor(new IgnoreUnderscoreBeanProcessor()));
      conn = operations.open().getJdbcConnection();
    }

    @Override
    public void run(int input) {
      try {
        runner.query(conn, SELECT_TYPICAL + " WHERE id = ?", rsHandler, input);
      }
      catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void close() {
      try {
        DbUtils.close(conn);
      }
      catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * It appears executing raw SQL is not possible with MyBatis. Therefore
   * "typical" = "optimized", there is no difference.
   */
  static class MyBatisSelect extends PerformanceTestBase {
    private SqlSession session;

    @Override
    public void init() {
      TransactionFactory transactionFactory = new JdbcTransactionFactory();
      final DataSource dataSource = new DataSourceConfig().h2DataSource();

      Environment environment = new Environment("development", transactionFactory, dataSource);
      org.apache.ibatis.session.Configuration config = new org.apache.ibatis.session.Configuration(environment);
      config.addMapper(MyBatisPostMapper.class);
      SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(config);
      session = sqlSessionFactory.openSession();
    }

    @Override
    public void run(int input) {
      session.getMapper(MyBatisPostMapper.class).selectPost(input);
    }

    @Override
    public void close() {
      session.close();
    }
  }

  /**
   * Mapper interface required for MyBatis performance test
   */
  interface MyBatisPostMapper {
    @Select(SELECT_TYPICAL + " WHERE id = #{id}")
    @Results({ @Result(property = "creationDate", column = "creation_date"), @Result(property = "lastChangeDate",
                                                                                     column = "last_change_date")
    })
    Post selectPost(int id);
  }

//  static class SpringJdbcTemplateSelect extends PerformanceTestBase {
//    private NamedParameterJdbcTemplate jdbcTemplate;
//
//    @Override
//    public void init() {
//      final DataSource dataSource = new DataSourceConfig().h2DataSource();
//      jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
//    }
//
//    @Override
//    public void run(int input) {
//      jdbcTemplate.queryForObject(SELECT_TYPICAL + " WHERE id = :id",
//                                  Collections.singletonMap("id", input),
//                                  new BeanPropertyRowMapper<Post>(Post.class));
//    }
//
//    @Override
//    public void close() {}
//  }

  // --
}
