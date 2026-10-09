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

import org.apache.commons.dbutils.BasicRowProcessor;
import org.apache.commons.dbutils.BeanProcessor;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.ResultQuery;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.Blackhole;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.sql2o.Sql2o;
import org.sql2o.quirks.NoQuirks;
import org.teasoft.bee.osql.PreparedSql;
import org.teasoft.bee.osql.Suid;
import org.teasoft.honey.osql.core.BeeFactory;
import org.teasoft.honey.osql.core.HoneyConfig;
import org.teasoft.honey.osql.core.HoneyContext;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

/**
 * Compares single-row queries and entity mapping with a connection per worker.
 * Each invocation creates and closes its statement; result caches are disabled.
 * Results describe the dependency versions declared in {@code infra-jdbc/build.gradle},
 * including legacy JDBI 2 and jOOQ 3.3, rather than current framework releases.
 * Query construction, parameter binding, execution and mapping are measured together.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(3)
@State(Scope.Thread)
public class SelectBenchmark {

  private static final String DRIVER_CLASS = "org.h2.Driver";
  private static final String DB_URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=MySQL";
  private static final String DB_USER = "sa";
  private static final String DB_PASSWORD = "";
  private static final SQLDialect JOOQ_DIALECT = SQLDialect.H2;

  static final int ROW_COUNT = 1000;

  private RepositoryManager operations;

  private PerformanceTestBase test;

  /** Shared, read-only database fixture; query resources belong to each worker. */
  @State(Scope.Benchmark)
  public static class DatabaseState {

    private final Post[] expected = new Post[ROW_COUNT];

    /** Create deterministic rows once for all workers in this trial. */
    @Setup
    public void setup(BenchmarkParams params) throws Exception {
      Class.forName(DRIVER_CLASS);
      if (params.getBenchmark().endsWith(".beeSelect")) {
        HoneyConfig config = HoneyConfig.getHoneyConfig();
        config.setDbName("H2");
        config.setDriverName(DRIVER_CLASS);
        config.setUrl(DB_URL);
        config.setUsername(DB_USER);
        config.setPassword(DB_PASSWORD);
        config.showSQL = false;
        HoneyContext.updateConfig(Map.of("cache_nocache", true));
      }
      var fixture = new SelectBenchmark();
      fixture.operations = new RepositoryManager(DB_URL, DB_USER, DB_PASSWORD);
      try {
        fixture.createPostTable(expected);
      }
      catch (Exception | Error ex) {
        try {
          close();
        }
        catch (Exception closeFailure) {
          ex.addSuppressed(closeFailure);
        }
        throw ex;
      }
    }

    /** Release the in-memory database after worker resources have closed. */
    @TearDown
    public void close() throws SQLException {
      try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
              Statement statement = connection.createStatement()) {
        statement.execute("SHUTDOWN");
      }
    }
  }

  /** Initialize and validate only the implementation selected by JMH. */
  @Setup
  public void setup(DatabaseState database, BenchmarkParams params) throws Exception {
    operations = new RepositoryManager(DB_URL, DB_USER, DB_PASSWORD);
    String benchmark = params.getBenchmark();
    test = switch (benchmark.substring(benchmark.lastIndexOf('.') + 1)) {
      case "beeSelect" -> new BeeSelect();
      case "todayTypicalSelect" -> new TODAYTypicalSelect();
      case "todayOptimizedSelect" -> new TODAYOptimizedSelect();
      case "todayPositionalSelect" -> new TODAYPositionalSelect(SELECT_TYPICAL_POSITIONAL);
      case "todayPositionalAliasedSelect" -> new TODAYPositionalSelect(SELECT_OPTIMAL_POSITIONAL);
      case "handCodedSelect" -> new HandCodedSelect();
      case "sql2oTypicalSelect" -> new Sql2oTypicalSelect();
      case "sql2oOptimizedSelect" -> new Sql2oOptimizedSelect();
      case "jdbiSelect" -> new JDBISelect();
      case "jooqSelect" -> new JOOQSelect();
      case "apacheDbUtilsTypicalSelect" -> new ApacheDbUtilsTypicalSelect();
      case "myBatisSelect" -> new MyBatisSelect();
      default -> throw new IllegalArgumentException("Unknown benchmark: " + benchmark);
    };
    try {
      test.initialize();
      for (int id = 1; id <= ROW_COUNT; id++) {
        validate(database.expected[id - 1], test.run(id), test.getName(), id);
      }
    }
    catch (Exception | Error ex) {
      try {
        close();
      }
      catch (Exception closeFailure) {
        ex.addSuppressed(closeFailure);
      }
      throw ex;
    }
  }

  private static void validate(Post expected, Object result, String name, int id) throws Exception {
    if (!(result instanceof Post actual) || actual.getId() != id) {
      throw new IllegalStateException(name + " returned an invalid row for id " + id);
    }
    for (PropertyDescriptor property : Introspector.getBeanInfo(Post.class, Object.class).getPropertyDescriptors()) {
      Object expectedValue = property.getReadMethod().invoke(expected);
      Object actualValue = property.getReadMethod().invoke(actual);
      boolean equal = expectedValue instanceof Date expectedDate && actualValue instanceof Date actualDate
              ? expectedDate.getTime() == actualDate.getTime() : Objects.equals(expectedValue, actualValue);
      if (!equal) {
        throw new IllegalStateException(name + " incorrectly mapped " + property.getName() + " for id " + id
                + ": expected " + expectedValue + ", actual " + actualValue);
      }
    }
  }

  /** Close the selected implementation, including after partial initialization. */
  @TearDown
  public void close() throws Exception {
    PerformanceTestBase current = test;
    test = null;
    if (current != null) {
      current.close();
    }
  }

  private void createPostTable(Post[] expected) throws Exception {
    try (JdbcConnection connection = operations.open()) {
      connection.createNamedQuery("DROP TABLE IF EXISTS post").executeUpdate();
      connection.createNamedQuery("\n CREATE TABLE post" +
              "\n (" +
              "\n     id INT NOT NULL AUTO_INCREMENT PRIMARY KEY" +
              "\n   , text VARCHAR(255)" +
              "\n   , creation_date DATETIME(3)" +
              "\n   , last_change_date DATETIME(3)" +
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

      Random r = new Random(42);

      NamedQuery insQuery = connection.createNamedQuery(
              "insert into post (text, creation_date, last_change_date, counter1, counter2, counter3, counter4, counter5, counter6, counter7, counter8, counter9) " +
                      "values (:text, :creation_date, :last_change_date, :counter1, :counter2, :counter3, :counter4, :counter5, :counter6, :counter7, :counter8, :counter9)");
      for (int idx = 0; idx < ROW_COUNT; idx++) {
        Post post = new Post();
        post.setId(idx + 1);
        post.setText("a name " + idx);
        post.setCreationDate(new Date(1_700_000_000_000L + r.nextInt()));
        post.setLastChangeDate(new Date(1_700_000_000_000L + r.nextInt()));
        insQuery.addParameter("text", post.getText())
                .addParameter("creation_date", new Timestamp(post.getCreationDate().getTime()))
                .addParameter("last_change_date", new Timestamp(post.getLastChangeDate().getTime()));
        for (int counter = 1; counter <= 9; counter++) {
          Integer value = r.nextBoolean() ? r.nextInt() : null;
          Post.class.getMethod("setCounter" + counter, Integer.class).invoke(post, value);
          insQuery.addParameter("counter" + counter, value);
        }
        expected[idx] = post;
        insQuery.addToBatch();
      }
      insQuery.executeBatch();
    }
  }

  @Benchmark
  public void beeSelect(Blackhole bh) throws SQLException {
    bh.consume(test.run());
  }

  @Benchmark
  public void todayTypicalSelect(Blackhole bh) throws SQLException {
    bh.consume(test.run());
  }

  @Benchmark
  public void todayOptimizedSelect(Blackhole bh) throws SQLException {
    bh.consume(test.run());
  }

  /** Query and map one row using a positional parameter and ordinary column names. */
  @Benchmark
  public void todayPositionalSelect(Blackhole bh) throws SQLException {
    bh.consume(test.run());
  }

  /** Query and map one row using a positional parameter and property-name aliases. */
  @Benchmark
  public void todayPositionalAliasedSelect(Blackhole bh) throws SQLException {
    bh.consume(test.run());
  }

  @Benchmark
  public void handCodedSelect(Blackhole bh) throws SQLException {
    bh.consume(test.run());
  }

  @Benchmark
  public void sql2oTypicalSelect(Blackhole bh) throws SQLException {
    bh.consume(test.run());
  }

  @Benchmark
  public void sql2oOptimizedSelect(Blackhole bh) throws SQLException {
    bh.consume(test.run());
  }

  @Benchmark
  public void jdbiSelect(Blackhole bh) throws SQLException {
    bh.consume(test.run());
  }

  @Benchmark
  public void jooqSelect(Blackhole bh) throws SQLException {
    bh.consume(test.run());
  }

  @Benchmark
  public void apacheDbUtilsTypicalSelect(Blackhole bh) throws SQLException {
    bh.consume(test.run());
  }

  @Benchmark
  public void myBatisSelect(Blackhole bh) throws SQLException {
    bh.consume(test.run());
  }

  //----------------------------------------
  //          performance tests
  // ---------------------------------------

  static final String SELECT_TYPICAL = "SELECT * FROM post";
  static final String SELECT_OPTIMAL = "SELECT id, text, creation_date as creationDate, last_change_date as lastChangeDate, counter1, counter2, counter3, counter4, counter5, counter6, counter7, counter8, counter9 FROM post";

  private static final String SELECT_TYPICAL_NAMED = SELECT_TYPICAL + " WHERE id = :id";
  private static final String SELECT_OPTIMAL_NAMED = SELECT_OPTIMAL + " WHERE id = :id";
  private static final String SELECT_TYPICAL_POSITIONAL = SELECT_TYPICAL + " WHERE id = ?";
  private static final String SELECT_OPTIMAL_POSITIONAL = SELECT_OPTIMAL + " WHERE id = ?";
  private static final String SELECT_MYBATIS = SELECT_TYPICAL + " WHERE id = #{id}";

  /**
   * Uses {@link #SELECT_OPTIMAL} with property-name aliases and default mapping settings.
   */
  class TODAYOptimizedSelect extends PerformanceTestBase {
    private JdbcConnection conn;

    @Override
    public void init() {
      conn = operations.open();
    }

    @Override
    public Object run(int input) {
      try (NamedQuery query = conn.createNamedQuery(SELECT_OPTIMAL_NAMED)) {
        return query.addParameter("id", input)
                .fetchFirst(Post.class);
      }
    }

    @Override
    public void close() {
      if (conn != null) {
        conn.close();
        conn = null;
      }
    }
  }

  class TODAYPositionalSelect extends PerformanceTestBase {
    private final String sql;
    private JdbcConnection conn;

    TODAYPositionalSelect(String sql) {
      this.sql = sql;
    }

    @Override
    public void init() {
      conn = operations.open();
    }

    @Override
    public Object run(int input) {
      try (Query query = conn.createQuery(sql)) {
        return query.addParameter(input).fetchFirst(Post.class);
      }
    }

    @Override
    public void close() {
      if (conn != null) {
        conn.close();
        conn = null;
      }
    }
  }

  class TODAYTypicalSelect extends PerformanceTestBase {
    private JdbcConnection conn;

    @Override
    public void init() {
      conn = operations.open();
    }

    @Override
    public Object run(int input) {
      try (NamedQuery query = conn.createNamedQuery(SELECT_TYPICAL_NAMED)) {
        return query.addParameter("id", input)
                .fetchFirst(Post.class);
      }
    }

    @Override
    public void close() {
      if (conn != null) {
        conn.close();
        conn = null;
      }
    }
  }

  static class BeeSelect extends PerformanceTestBase {
    Suid suid;
    PreparedSql query;

    @Override
    public void init() {
      suid = BeeFactory.getHoneyFactory().getSuid();
      query = BeeFactory.getHoneyFactory().getPreparedSql();
      suid.beginSameConnection();
    }

    @Override
    public Object run(int input) {
      List<Post> rows = query.select(SELECT_TYPICAL_POSITIONAL, new Post(), new Object[] { input });
      return rows.isEmpty() ? null : rows.get(0);
    }

    @Override
    public void close() {
      if (suid != null) {
        suid.endSameConnection();
        suid = null;
      }
    }
  }

  static class Sql2oTypicalSelect extends PerformanceTestBase {
    private org.sql2o.Connection conn;
    Sql2o sql2o = new Sql2o(DB_URL, DB_USER, DB_PASSWORD, new NoQuirks());

    @Override
    public void init() {
      conn = sql2o.open();
    }

    @Override
    public Object run(int input) {
      try (org.sql2o.Query query = conn.createQuery(SELECT_TYPICAL_NAMED)) {
        return query.setAutoDeriveColumnNames(true).addParameter("id", input)
                .executeAndFetchFirst(Post.class);
      }
    }

    @Override
    public void close() {
      if (conn != null) {
        conn.close();
        conn = null;
      }
    }
  }

  /**
   * Considered "optimized" because it uses {@link #SELECT_OPTIMAL} rather than
   * auto-mapping underscore case to camel case.
   */
  static class Sql2oOptimizedSelect extends PerformanceTestBase {
    private org.sql2o.Connection conn;
    Sql2o sql2o = new Sql2o(DB_URL, DB_USER, DB_PASSWORD, new NoQuirks());

    @Override
    public void init() {
      conn = sql2o.open();
    }

    @Override
    public Object run(int input) {
      try (org.sql2o.Query query = conn.createQuery(SELECT_OPTIMAL_NAMED)) {
        return query.addParameter("id", input)
                .executeAndFetchFirst(Post.class);
      }
    }

    @Override
    public void close() {
      if (conn != null) {
        conn.close();
        conn = null;
      }
    }
  }

  /** Uses SQL aliases for the legacy JDBI bean mapper. */
  static class JDBISelect extends PerformanceTestBase {
    Handle h;

    @Override
    public void init() {
      DBI dbi = new DBI(DB_URL, DB_USER, DB_PASSWORD);
      h = dbi.open();
    }

    @Override
    public Object run(int input) {
      return h.createQuery(SELECT_OPTIMAL_NAMED).map(Post.class).bind("id", input).first();
    }

    @Override
    public void close() {
      if (h != null) {
        h.close();
        h = null;
      }
    }
  }

  /** Creates a query model on each invocation, like the other implementations. */
  static class JOOQSelect extends PerformanceTestBase {
    DSLContext create;
    Connection conn;

    public void init() throws Exception {
      conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
      create = DSL.using(conn, JOOQ_DIALECT);
    }

    @Override
    public Object run(int input) {
      ResultQuery<?> query = create.resultQuery(SELECT_TYPICAL_POSITIONAL, input).keepStatement(false);
      try {
        Record r = query.fetchOne();
        return r == null ? null : r.into(Post.class);
      }
      finally {
        query.close();
      }
    }

    @Override
    public void close() throws SQLException {
      if (conn != null) {
        conn.close();
        conn = null;
      }
    }
  }

  static class HandCodedSelect extends PerformanceTestBase {
    private Connection conn = null;

    @Override
    public void init() throws SQLException {
      conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    private Integer getNullableInt(ResultSet rs, int columnIndex) throws SQLException {
      Object obj = rs.getObject(columnIndex);
      return obj == null ? null : (Integer) obj;
    }

    @Override
    public Object run(int input) throws SQLException {
      try (PreparedStatement stmt = conn.prepareStatement(SELECT_TYPICAL_POSITIONAL)) {
        stmt.setInt(1, input);
        try (ResultSet rs = stmt.executeQuery()) {
          if (rs.next()) {
            Post p = new Post();
            p.setId(rs.getInt(1));
            p.setText(rs.getString(2));
            p.setCreationDate(rs.getTimestamp(3));
            p.setLastChangeDate(rs.getTimestamp(4));
            p.setCounter1(getNullableInt(rs, 5));
            p.setCounter2(getNullableInt(rs, 6));
            p.setCounter3(getNullableInt(rs, 7));
            p.setCounter4(getNullableInt(rs, 8));
            p.setCounter5(getNullableInt(rs, 9));
            p.setCounter6(getNullableInt(rs, 10));
            p.setCounter7(getNullableInt(rs, 11));
            p.setCounter8(getNullableInt(rs, 12));
            p.setCounter9(getNullableInt(rs, 13));
            return p;
          }
        }
      }
      return null;
    }

    public void close() throws SQLException {
      if (conn != null) {
        conn.close();
      }
    }
  }

  static class ApacheDbUtilsTypicalSelect extends PerformanceTestBase {
    private QueryRunner runner;
    private org.apache.commons.dbutils.ResultSetHandler<Post> rsHandler;
    private Connection conn;

    /**
     * This class handles mapping "first_name" column to "firstName" property. It
     * looks worse than it is, most is copied from
     * {@link org.apache.commons.dbutils.BeanProcessor} and many people complain
     * online that this isn't built in to Apache DbUtils yet.
     */
    static class IgnoreUnderscoreBeanProcessor extends BeanProcessor {
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
    public void init() throws SQLException {
      runner = new QueryRunner();
      rsHandler = new BeanHandler<>(Post.class, new BasicRowProcessor(new IgnoreUnderscoreBeanProcessor()));
      conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    @Override
    public Object run(int input) throws SQLException {
      return runner.query(conn, SELECT_TYPICAL_POSITIONAL, rsHandler, input);
    }

    @Override
    public void close() throws Exception {
      DbUtils.close(conn);
    }
  }

  /** Uses an annotated SQL mapper with statement-scoped local caching. */
  static class MyBatisSelect extends PerformanceTestBase {
    private SqlSession session;
    private MyBatisPostMapper mapper;

    @Override
    public void init() {
      TransactionFactory transactionFactory = new JdbcTransactionFactory();
      DataSource dataSource = new UnpooledDataSource(DRIVER_CLASS, DB_URL, DB_USER, DB_PASSWORD);

      Environment environment = new Environment("development", transactionFactory, dataSource);
      org.apache.ibatis.session.Configuration config = new org.apache.ibatis.session.Configuration(environment);
      config.setLocalCacheScope(LocalCacheScope.STATEMENT);
      config.setCacheEnabled(false);
      config.addMapper(MyBatisPostMapper.class);
      SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(config);
      session = sqlSessionFactory.openSession(true);
      mapper = session.getMapper(MyBatisPostMapper.class);
    }

    @Override
    public Object run(int input) {
      return mapper.selectPost(input);
    }

    @Override
    public void close() {
      mapper = null;
      if (session != null) {
        session.close();
        session = null;
      }
    }
  }

  /**
   * Mapper interface required for MyBatis performance test
   */
  interface MyBatisPostMapper {

    @Select(SELECT_MYBATIS)
    @Results({ @Result(property = "creationDate", column = "creation_date"), @Result(property = "lastChangeDate",
            column = "last_change_date")
    })
    Post selectPost(int id);
  }

}
