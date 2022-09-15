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
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import cn.taketoday.dao.DataAccessException;
import cn.taketoday.jdbc.CannotGetJdbcConnectionException;
import cn.taketoday.jdbc.GeneratedKeysException;
import cn.taketoday.jdbc.PersistenceException;
import cn.taketoday.jdbc.RepositoryManager;
import cn.taketoday.jdbc.result.DefaultResultSetHandlerFactory;
import cn.taketoday.jdbc.result.JdbcBeanMetadata;
import cn.taketoday.jdbc.result.ResultSetHandlerIterator;
import cn.taketoday.jdbc.sql.dialect.Dialect;
import cn.taketoday.jdbc.sql.dialect.MySQLDialect;
import cn.taketoday.jdbc.sql.format.SqlStatementLogger;
import cn.taketoday.jdbc.support.JdbcUtils;
import cn.taketoday.jdbc.type.TypeHandler;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.LogMessage;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/10 22:28
 */
public class DefaultEntityManager implements EntityManager {
  private static final Logger log = LoggerFactory.getLogger(DefaultEntityManager.class);

  private SqlStatementLogger stmtLogger = SqlStatementLogger.sharedInstance;

  private EntityMetadataFactory entityMetadataFactory = new DefaultEntityMetadataFactory();

  private final RepositoryManager repositoryManager;

  private int maxBatchRecords = 0;

  private Dialect dialect = new MySQLDialect();

  /**
   * a flag indicating whether auto-generated keys should be returned;
   */
  private boolean returnGeneratedKeys = true;

  public DefaultEntityManager(RepositoryManager repositoryManager) {
    this.repositoryManager = repositoryManager;
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

  public void setStatementLogger(SqlStatementLogger statementLogger) {
    Assert.notNull(statementLogger, "statementLogger is required");
    this.stmtLogger = statementLogger;
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

    Connection connection = getConnection();

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
    catch (SQLException ex) {
      throw new PersistenceException("Error in executeUpdate, " + ex.getMessage(), ex);
    }
  }

  private static void assertUpdateCount(int updateCount, int expectCount) {
    if (updateCount != expectCount) {
      throw new PersistenceException("update count '" + updateCount + "' is not equals to expected count '" + expectCount + "'");
    }
  }

  protected Connection getConnection() {
    try {
      return repositoryManager.getConnectionSource().getConnection();
    }
    catch (SQLException ex) {
      throw new CannotGetJdbcConnectionException(
              "Could not acquire a connection from connection-source: " + repositoryManager.getConnectionSource(), ex);
    }
  }

  protected PreparedStatement prepareStatement(Connection connection, String sql, boolean returnGeneratedKeys) {
    try {
      if (returnGeneratedKeys) {
        return connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      }
      return connection.prepareStatement(sql);
    }
    catch (SQLException ex) {
      throw new PersistenceException("Error preparing statement '" + sql + "' - " + ex.getMessage(), ex);
    }
  }

  @Override
  public void persist(Iterable<?> entities) throws DataAccessException {
    persist(entities, returnGeneratedKeys);
  }

  @Override
  public void persist(Iterable<?> entities, boolean returnGeneratedKeys) throws DataAccessException {
    Connection connection = getConnection();
    int maxBatchRecords = getMaxBatchRecords();
    var statements = new HashMap<Class<?>, PreparedBatch>();

    for (Object entity : entities) {
      PreparedBatch batch = statements.computeIfAbsent(entity.getClass(), entityClass -> {
        EntityMetadata entityMetadata = entityMetadataFactory.getEntityMetadata(entityClass);
        String sql = insert(entityMetadata);
        return new PreparedBatch(connection, sql, entityMetadata, returnGeneratedKeys);
      });

      batch.addBatchUpdate(entity, maxBatchRecords);
    }

    for (PreparedBatch preparedBatch : statements.values()) {
      preparedBatch.executeBatch(returnGeneratedKeys);
      preparedBatch.closeQuietly();
    }

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

    PreparedBatch(Connection connection, String sql, EntityMetadata entityMetadata, boolean returnGeneratedKeys) {
      this.sql = sql;
      this.statement = prepareStatement(connection, sql, returnGeneratedKeys);
      this.entityMetadata = entityMetadata;
      this.returnGeneratedKeys = returnGeneratedKeys;
    }

    public void addBatchUpdate(Object entity, int maxBatchRecords) {
      entities.add(entity);
      PreparedStatement statement = this.statement;

      try {
        setPersistParameter(entity, statement, entityMetadata);

        statement.addBatch();
        if (maxBatchRecords > 0 && ++currentBatchRecords % maxBatchRecords == 0) {
          executeBatch(statement, returnGeneratedKeys);
        }
      }
      catch (SQLException e) {
        throw new PersistenceException("Error while adding statement to batch", e);
      }
    }

    public void executeBatch(boolean returnGeneratedKeys) {
      executeBatch(statement, returnGeneratedKeys);
    }

    public void executeBatch(PreparedStatement statement, boolean returnGeneratedKeys) {
      if (stmtLogger.isDebugEnabled()) {
        stmtLogger.logStatement(LogMessage.format("Executing batch size: {}", entities.size()), sql);
      }
      try {
        int[] updateCounts = statement.executeBatch();
        assertUpdateCount(updateCounts.length, entities.size());
        if (returnGeneratedKeys) {
          ResultSet generatedKeys = statement.getGeneratedKeys();
          for (Object entity : entities) {
            try {
              if (generatedKeys.next()) {
                entityMetadata.idProperty.setProperty(entity, generatedKeys, 1);
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
      catch (Throwable e) {
        throw new PersistenceException("Error while executing batch operation: " + e.getMessage(), e);
      }
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
    return null;
  }

  @Override
  public <T> T findFirst(T entity) throws DataAccessException {
    return null;
  }

  @Override
  public <T> List<T> findFirst(Class<T> entityClass, Object query) throws DataAccessException {
    return null;
  }

  @Override
  public <T> List<T> find(T entity) throws DataAccessException {
    return null;
  }

  @Override
  public <T> List<T> find(Class<T> entityClass, Object params) throws DataAccessException {
    ArrayList<T> entities = new ArrayList<>();
    Iterator<T> iterator = iterate(entityClass, params);
    while (iterator.hasNext()) {
      entities.add(iterator.next());
    }
    return entities;
  }

  @Override
  public <T> void iterate(Class<T> entityClass, Object params, Consumer<T> entityConsumer) throws DataAccessException {
    Iterator<T> iterator = iterate(entityClass, params);
    while (iterator.hasNext()) {
      entityConsumer.accept(iterator.next());
    }
  }

  @Override
  public <T> Iterator<T> iterate(Class<T> entityClass, Object params) throws DataAccessException {
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

    Connection connection = getConnection();
    PreparedStatement statement = prepareStatement(connection, sql.toString(), false);
    try {
      int idx = 1;
      for (Condition condition : conditions) {
        condition.typeHandler.setParameter(statement, idx++, condition.propertyValue);
      }
    }
    catch (SQLException ex) {
      throw new PersistenceException("Error in setParameter, " + ex.getMessage(), ex);
    }
    try {
      ResultSet resultSet = statement.executeQuery();
      return new ResultSetHandlerIterator<>(resultSet, new DefaultResultSetHandlerFactory<>(
              new JdbcBeanMetadata(entityClass, repositoryManager.isDefaultCaseSensitive(), true, true),
              repositoryManager, null));
    }
    catch (SQLException ex) {
      throw new PersistenceException("Error in fetch entity, " + ex.getMessage(), ex);
    }
  }

  @Override
  public <T> void iterate(Class<T> entityClass, QueryCondition conditions, Consumer<T> entityConsumer) throws DataAccessException {
    Iterator<T> iterator = iterate(entityClass, conditions);
    while (iterator.hasNext()) {
      entityConsumer.accept(iterator.next());
    }
  }

  @Override
  public <T> Iterator<T> iterate(Class<T> entityClass, @Nullable QueryCondition conditions) throws DataAccessException {
    EntityMetadata metadata = entityMetadataFactory.getEntityMetadata(entityClass);

    StringBuilder sql = new StringBuilder();
    sql.append("SELECT * FROM ");
    sql.append(metadata.tableName);

    if (conditions != null) {
      sql.append(" WHERE ");
      // WHERE column_name operator value;
      conditions.render(sql);
    }

    Connection connection = getConnection();
    PreparedStatement statement = prepareStatement(connection, sql.toString(), false);
    try {
      if (conditions != null) {
        conditions.setParameter(statement);
      }
    }
    catch (SQLException ex) {
      throw new PersistenceException("Error in setParameter, " + ex.getMessage(), ex);
    }

    if (stmtLogger.isDebugEnabled()) {
      stmtLogger.logStatement("Lookup entities", sql.toString());
    }

    try {
      ResultSet resultSet = statement.executeQuery();
      return new ResultSetHandlerIterator<>(resultSet, new DefaultResultSetHandlerFactory<>(
              new JdbcBeanMetadata(entityClass, repositoryManager.isDefaultCaseSensitive(), true, true),
              repositoryManager, null));
    }
    catch (SQLException ex) {
      throw new PersistenceException("Error in fetch entity, " + ex.getMessage(), ex);
    }
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

}
