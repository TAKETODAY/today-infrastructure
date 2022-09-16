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

package cn.taketoday.jdbc.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

import javax.sql.DataSource;

import cn.taketoday.dao.DataAccessException;
import cn.taketoday.jdbc.GeneratedKeysException;
import cn.taketoday.jdbc.PersistenceException;
import cn.taketoday.jdbc.RepositoryManager;
import cn.taketoday.jdbc.core.ConnectionCallback;
import cn.taketoday.jdbc.datasource.DataSourceUtils;
import cn.taketoday.jdbc.result.DefaultResultSetHandlerFactory;
import cn.taketoday.jdbc.result.JdbcBeanMetadata;
import cn.taketoday.jdbc.result.ResultSetHandlerIterator;
import cn.taketoday.jdbc.result.ResultSetIterator;
import cn.taketoday.jdbc.sql.dialect.Dialect;
import cn.taketoday.jdbc.sql.dialect.MySQLDialect;
import cn.taketoday.jdbc.support.JdbcAccessor;
import cn.taketoday.jdbc.support.JdbcUtils;
import cn.taketoday.jdbc.type.TypeHandler;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.LogMessage;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/10 22:28
 */
public class DefaultEntityManager extends JdbcAccessor implements EntityManager {

  private EntityMetadataFactory entityMetadataFactory = new DefaultEntityMetadataFactory();

  private final RepositoryManager repositoryManager;

  private int maxBatchRecords = 0;

  // TODO Dialect static factory creation
  private Dialect dialect = new MySQLDialect();

  /**
   * a flag indicating whether auto-generated keys should be returned;
   */
  private boolean returnGeneratedKeys = true;

  public DefaultEntityManager(RepositoryManager repositoryManager) {
    this.repositoryManager = repositoryManager;
    setDataSource(repositoryManager.getDataSource());
    setExceptionTranslator(repositoryManager.getExceptionTranslator());
  }

  public void setDialect(Dialect dialect) {
    Assert.notNull(dialect, "dialect is required");
    this.dialect = dialect;
  }

  public void setEntityHolderFactory(EntityMetadataFactory entityMetadataFactory) {
    Assert.notNull(entityMetadataFactory, "entityMetadataFactory is required");
    this.entityMetadataFactory = entityMetadataFactory;
  }

  /**
   * Set a flag indicating whether auto-generated keys should be returned;
   *
   * @param returnGeneratedKeys a flag indicating whether auto-generated keys should be returned;
   */
  public void setReturnGeneratedKeys(boolean returnGeneratedKeys) {
    this.returnGeneratedKeys = returnGeneratedKeys;
  }

  /**
   * Sets the number of batched commands this Query allows to be added before
   * implicitly calling <code>executeBatch()</code> from
   * <code>addToBatch()</code>. <br/>
   *
   * When set to 0, executeBatch is not called implicitly. This is the default
   * behaviour. <br/>
   *
   * When using this, please take care about calling <code>executeBatch()</code>
   * after finished adding all commands to the batch because commands may remain
   * unexecuted after the last <code>addToBatch()</code> call. Additionally, if
   * fetchGeneratedKeys is set, then previously generated keys will be lost after
   * a batch is executed.
   *
   * @throws IllegalArgumentException Thrown if the value is negative.
   */
  public void setMaxBatchRecords(int maxBatchRecords) {
    Assert.isTrue(maxBatchRecords >= 0, "maxBatchRecords should be a non-negative value");
    this.maxBatchRecords = maxBatchRecords;
  }

  public int getMaxBatchRecords() {
    return this.maxBatchRecords;
  }

  @Override
  public void persist(Object entity) throws DataAccessException {
    persist(entity, returnGeneratedKeys);
  }

  @Override
  public void persist(Object entity, boolean returnGeneratedKeys) throws DataAccessException {
    Class<?> entityClass = entity.getClass();
    EntityMetadata entityMetadata = entityMetadataFactory.getEntityMetadata(entityClass);
    String sql = insert(entityMetadata);

    if (stmtLogger.isDebugEnabled()) {
      stmtLogger.logStatement(
              LogMessage.format("Persist entity: {}, generatedKeys={}", entity, returnGeneratedKeys), sql);
    }

    execute("Persist entity", new ConnectionCallback<Void>() {

      @Nullable
      @Override
      public Void doInConnection(Connection connection) throws SQLException, DataAccessException {
        try (PreparedStatement statement = prepareStatement(connection, sql, returnGeneratedKeys)) {
          setPersistParameter(entity, statement, entityMetadata);
          // execute
          int updateCount = statement.executeUpdate();
          assertUpdateCount(updateCount, 1);
          if (returnGeneratedKeys) {
            try {
              ResultSet generatedKeys = statement.getGeneratedKeys();
              if (generatedKeys.next()) {
                entityMetadata.idProperty.setProperty(entity, generatedKeys, 1);
              }
            }
            catch (SQLException e) {
              throw new GeneratedKeysException("Cannot get generated keys", e);
            }
          }
        }
        return null;
      }
    });
  }

  private static void assertUpdateCount(int updateCount, int expectCount) {
    if (updateCount != expectCount) {
      throw new PersistenceException("update count '" + updateCount + "' is not equals to expected count '" + expectCount + "'");
    }
  }

  protected PreparedStatement prepareStatement(Connection connection, String sql, boolean returnGeneratedKeys) throws SQLException {
    if (returnGeneratedKeys) {
      return connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
    }
    return connection.prepareStatement(sql);
  }

  @Override
  public void persist(Iterable<?> entities) throws DataAccessException {
    persist(entities, returnGeneratedKeys);
  }

  @Override
  public void persist(Iterable<?> entities, boolean returnGeneratedKeys) throws DataAccessException {
    execute("Batch persist entities", new ConnectionCallback<Void>() {
      @Nullable
      @Override
      public Void doInConnection(Connection connection) throws SQLException, DataAccessException {
        int maxBatchRecords = getMaxBatchRecords();
        var statements = new HashMap<Class<?>, PreparedBatch>();

        for (Object entity : entities) {
          Class<?> entityClass = entity.getClass();
          PreparedBatch batch = statements.get(entityClass);
          if (batch == null) {
            EntityMetadata entityMetadata = entityMetadataFactory.getEntityMetadata(entityClass);
            String sql = insert(entityMetadata);
            batch = new PreparedBatch(connection, sql, entityMetadata, returnGeneratedKeys);
            statements.put(entityClass, batch);
          }
          batch.addBatchUpdate(entity, maxBatchRecords);
        }

        for (PreparedBatch preparedBatch : statements.values()) {
          preparedBatch.executeBatch(returnGeneratedKeys);
          preparedBatch.closeQuietly();
        }

        return null;
      }
    });

  }

  private static void setPersistParameter(
          Object entity, PreparedStatement statement, EntityMetadata entityMetadata) throws SQLException {
    int idx = 1;
    for (EntityProperty property : entityMetadata.entityProperties) {
      property.setTo(statement, idx++, entity);
    }
  }

  class PreparedBatch {
    public final String sql;
    public int currentBatchRecords = 0;
    public final EntityMetadata entityMetadata;
    public final PreparedStatement statement;
    public final boolean returnGeneratedKeys;
    public final ArrayList<Object> entities = new ArrayList<>();

    PreparedBatch(Connection connection, String sql, EntityMetadata entityMetadata, boolean returnGeneratedKeys) throws SQLException {
      this.sql = sql;
      this.statement = prepareStatement(connection, sql, returnGeneratedKeys);
      this.entityMetadata = entityMetadata;
      this.returnGeneratedKeys = returnGeneratedKeys;
    }

    public void addBatchUpdate(Object entity, int maxBatchRecords) throws SQLException {
      entities.add(entity);
      PreparedStatement statement = this.statement;
      setPersistParameter(entity, statement, entityMetadata);
      statement.addBatch();
      if (maxBatchRecords > 0 && ++currentBatchRecords % maxBatchRecords == 0) {
        executeBatch(statement, returnGeneratedKeys);
      }
    }

    public void executeBatch(boolean returnGeneratedKeys) throws SQLException {
      executeBatch(statement, returnGeneratedKeys);
    }

    public void executeBatch(PreparedStatement statement, boolean returnGeneratedKeys) throws SQLException {
      if (stmtLogger.isDebugEnabled()) {
        stmtLogger.logStatement(LogMessage.format("Executing batch size: {}", entities.size()), sql);
      }
      int[] updateCounts = statement.executeBatch();
      assertUpdateCount(updateCounts.length, entities.size());
      if (returnGeneratedKeys) {
        EntityProperty idProperty = entityMetadata.idProperty;
        ResultSet generatedKeys = statement.getGeneratedKeys();
        for (Object entity : entities) {
          try {
            if (generatedKeys.next()) {
              idProperty.setProperty(entity, generatedKeys, 1);
            }
          }
          catch (SQLException e) {
            throw new GeneratedKeysException("Cannot get generated keys", e);
          }
        }
      }
      this.currentBatchRecords = 0;
      this.entities.clear();
    }

    public void closeQuietly() {
      JdbcUtils.closeQuietly(statement);
    }
  }

  @Override
  public void update(Object entity) {

  }

  @Override
  public void updateById(Object entity) {

  }

  @Override
  public void delete(Class<?> entityClass, Object id) {

  }

  @Override
  public void delete(Object entity) {

  }

  /**
   * Find by primary key.
   * Search for an entity of the specified class and primary key.
   * If the entity instance is contained in the underlying repository,
   * it is returned from there.
   *
   * @param entityClass entity class
   * @param id primary key
   * @return the found entity instance or null if the entity does
   * not exist
   * @throws IllegalArgumentException if the first argument does
   * not denote an entity type or the second argument is
   * is not a valid type for that entity's primary key or
   * is null
   */
  @Override
  @Nullable
  public <T> T findById(Class<T> entityClass, Object id) throws DataAccessException {
    EntityMetadata metadata = entityMetadataFactory.getEntityMetadata(entityClass);

    StringBuilder sql = new StringBuilder();
    sql.append("SELECT * FROM `");
    sql.append(metadata.tableName);
    sql.append("` WHERE `");
    sql.append(metadata.idColumnName);
    sql.append("`=? LIMIT 1");

    if (stmtLogger.isDebugEnabled()) {
      stmtLogger.logStatement(LogMessage.format("Lookup entity using ID: '{}'", id), sql.toString());
    }

    return execute("Fetch entity", new ConnectionCallback<>() {

      @Nullable
      @Override
      public T doInConnection(Connection connection) throws SQLException, DataAccessException {
        PreparedStatement statement = prepareStatement(connection, sql.toString(), false);
        metadata.idProperty.setParameter(statement, 1, id);

        ResultSet resultSet = statement.executeQuery();
        var iterator = new ResultSetHandlerIterator<T>(
                resultSet, new DefaultResultSetHandlerFactory<>(
                new JdbcBeanMetadata(entityClass, repositoryManager.isDefaultCaseSensitive(), true, true),
                repositoryManager, null)
        );

        return iterator.hasNext() ? iterator.next() : null;
      }
    });

  }

  @Nullable
  @Override
  @SuppressWarnings("unchecked")
  public <T> T findFirst(T entity) throws DataAccessException {
    try (ResultSetIterator<T> iterator = iterate((Class<T>) entity.getClass(), entity)) {
      while (iterator.hasNext()) {
        T returnValue = iterator.next();
        if (returnValue != null) {
          return returnValue;
        }
      }
    }
    return null;
  }

  @Nullable
  @Override
  public <T> T findFirst(Class<T> entityClass, Object query) throws DataAccessException {
    try (ResultSetIterator<T> iterator = iterate(entityClass, query)) {
      while (iterator.hasNext()) {
        T returnValue = iterator.next();
        if (returnValue != null) {
          return returnValue;
        }
      }
    }
    return null;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> List<T> find(T entity) throws DataAccessException {
    ArrayList<T> entities = new ArrayList<>();
    try (ResultSetIterator<T> iterator = iterate((Class<T>) entity.getClass(), entity)) {
      while (iterator.hasNext()) {
        entities.add(iterator.next());
      }
    }
    return entities;
  }

  @Override
  public <T> List<T> find(Class<T> entityClass, Object params) throws DataAccessException {
    ArrayList<T> entities = new ArrayList<>();
    try (ResultSetIterator<T> iterator = iterate(entityClass, params)) {
      while (iterator.hasNext()) {
        entities.add(iterator.next());
      }
    }
    return entities;
  }

  @Override
  public <T> void iterate(Class<T> entityClass, Object params, Consumer<T> entityConsumer) throws DataAccessException {
    try (ResultSetIterator<T> iterator = iterate(entityClass, params)) {
      while (iterator.hasNext()) {
        entityConsumer.accept(iterator.next());
      }
    }
  }

  @Override
  public <T> ResultSetIterator<T> iterate(Class<T> entityClass, Object params) throws DataAccessException {
    EntityMetadata metadata = entityMetadataFactory.getEntityMetadata(entityClass);
    EntityMetadata queryMetadata = entityMetadataFactory.getEntityMetadata(params.getClass());

    StringBuilder sql = new StringBuilder();
    sql.append("SELECT * FROM ");
    sql.append(metadata.tableName);
    sql.append(" WHERE ");

    StringBuilder columnNamesBuf = new StringBuilder();

    class Condition {
      final Object propertyValue;
      final TypeHandler<Object> typeHandler;

      Condition(TypeHandler<Object> typeHandler, Object propertyValue) {
        this.typeHandler = typeHandler;
        this.propertyValue = propertyValue;
      }
    }

    List<Condition> conditions = new ArrayList<>();
    for (EntityProperty entityProperty : queryMetadata.entityProperties) {
      Object propertyValue = entityProperty.getValue(params);
      if (propertyValue != null) {
        // TODO 当前只实现了判断null条件，可以扩展出去让用户做选择
        columnNamesBuf.append(", `")
                .append(entityProperty.columnName)
                .append('`')
                .append(" = ?");
        // and

        conditions.add(new Condition(entityProperty.typeHandler, propertyValue));
      }
    }

    if (columnNamesBuf.length() > 0) {
      sql.append(columnNamesBuf.substring(2));
    }

    if (stmtLogger.isDebugEnabled()) {
      stmtLogger.logStatement(LogMessage.format("Lookup entity using queryObject: {}", params), sql.toString());
    }

    return execute("Iterate entities with query-model", connection -> {
      PreparedStatement statement = prepareStatement(connection, sql.toString(), false);
      int idx = 1;
      for (Condition condition : conditions) {
        condition.typeHandler.setParameter(statement, idx++, condition.propertyValue);
      }
      ResultSet resultSet = statement.executeQuery();
      return new ResultSetHandlerIterator<>(resultSet, new DefaultResultSetHandlerFactory<>(
              new JdbcBeanMetadata(entityClass, repositoryManager.isDefaultCaseSensitive(), true, true),
              repositoryManager, null));
    });
  }

  @Override
  public <T> void iterate(Class<T> entityClass, QueryCondition conditions, Consumer<T> entityConsumer) throws DataAccessException {
    try (ResultSetIterator<T> iterator = iterate(entityClass, conditions)) {
      while (iterator.hasNext()) {
        entityConsumer.accept(iterator.next());
      }
    }
  }

  @Override
  public <T> ResultSetIterator<T> iterate(Class<T> entityClass, @Nullable QueryCondition conditions) throws DataAccessException {
    EntityMetadata metadata = entityMetadataFactory.getEntityMetadata(entityClass);

    StringBuilder sql = new StringBuilder();
    sql.append("SELECT * FROM ");
    sql.append(metadata.tableName);

    if (conditions != null) {
      sql.append(" WHERE ");
      // WHERE column_name operator value;
      conditions.render(sql);
    }

    return execute("Iterate entities with query-conditions", connection -> {
      PreparedStatement statement = prepareStatement(connection, sql.toString(), false);
      if (conditions != null) {
        conditions.setParameter(statement);
      }

      if (stmtLogger.isDebugEnabled()) {
        stmtLogger.logStatement("Lookup entities", sql.toString());
      }

      ResultSet resultSet = statement.executeQuery();
      return new ResultSetHandlerIterator<>(resultSet, new DefaultResultSetHandlerFactory<>(
              new JdbcBeanMetadata(entityClass, repositoryManager.isDefaultCaseSensitive(), true, true),
              repositoryManager, null));
    });
  }

  //
  static String insert(EntityMetadata entityMetadata) {
    StringBuilder sql = new StringBuilder();
    sql.append("INSERT INTO ").append(entityMetadata.tableName);

    StringBuilder columnNamesBuf = new StringBuilder();
    StringBuilder placeholderBuf = new StringBuilder();

    for (String columName : entityMetadata.columnNames) {
      columnNamesBuf.append(", `").append(columName).append('`');
      placeholderBuf.append(", ?");
    }

    if (columnNamesBuf.length() > 0) {
      sql.append("(").append(columnNamesBuf.substring(2)).append(")");
      sql.append(" VALUES (").append(placeholderBuf.substring(2)).append(")");
    }
    return sql.toString();
  }

  @Nullable
  public <T> T execute(String task, ConnectionCallback<T> action) throws DataAccessException {
    DataSource dataSource = obtainDataSource();
    Connection con = DataSourceUtils.getConnection(dataSource);
    try {
      return action.doInConnection(con);
    }
    catch (SQLException ex) {
      // Release Connection early, to avoid potential connection pool deadlock
      // in the case when the exception translator hasn't been initialized yet.
      String sql = getSql(action);
      DataSourceUtils.releaseConnection(con, dataSource);
      con = null;
      throw translateException(task, sql, ex);
    }
    finally {
      DataSourceUtils.releaseConnection(con, dataSource);
    }
  }

}
