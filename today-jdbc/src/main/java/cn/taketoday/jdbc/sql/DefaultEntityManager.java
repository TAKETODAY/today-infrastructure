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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import javax.sql.DataSource;

import cn.taketoday.beans.BeanProperty;
import cn.taketoday.dao.DataAccessException;
import cn.taketoday.dao.IncorrectResultSizeDataAccessException;
import cn.taketoday.jdbc.GeneratedKeysException;
import cn.taketoday.jdbc.JdbcUpdateAffectedIncorrectNumberOfRowsException;
import cn.taketoday.jdbc.PersistenceException;
import cn.taketoday.jdbc.RepositoryManager;
import cn.taketoday.jdbc.core.ConnectionCallback;
import cn.taketoday.jdbc.datasource.DataSourceUtils;
import cn.taketoday.jdbc.entity.BatchPersistListener;
import cn.taketoday.jdbc.result.DefaultResultSetHandlerFactory;
import cn.taketoday.jdbc.result.JdbcBeanMetadata;
import cn.taketoday.jdbc.result.ResultSetHandler;
import cn.taketoday.jdbc.result.ResultSetIterator;
import cn.taketoday.jdbc.sql.dialect.Dialect;
import cn.taketoday.jdbc.sql.dialect.MySQLDialect;
import cn.taketoday.jdbc.support.JdbcAccessor;
import cn.taketoday.jdbc.type.TypeHandler;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.LogMessage;
import cn.taketoday.transaction.support.TransactionOperations;
import cn.taketoday.util.CollectionUtils;

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

  @Nullable
  private ArrayList<BatchPersistListener> batchPersistListeners;

  /**
   * a flag indicating whether auto-generated keys should be returned;
   */
  private boolean returnGeneratedKeys = true;

  private TransactionOperations batchTransactionOperations = TransactionOperations.withoutTransaction();

  public DefaultEntityManager(RepositoryManager repositoryManager) {
    this.repositoryManager = repositoryManager;
    setDataSource(repositoryManager.getDataSource());
    setExceptionTranslator(repositoryManager.getExceptionTranslator());
  }

  public void setDialect(Dialect dialect) {
    Assert.notNull(dialect, "dialect is required");
    this.dialect = dialect;
  }

  public void setEntityMetadataFactory(EntityMetadataFactory entityMetadataFactory) {
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

  public void setBatchTransactionOperations(TransactionOperations batchTransactionOperations) {
    this.batchTransactionOperations = batchTransactionOperations;
  }

  public void addBatchPersistListeners(BatchPersistListener... listeners) {
    if (batchPersistListeners == null) {
      batchPersistListeners = new ArrayList<>();
    }
    CollectionUtils.addAll(batchPersistListeners, listeners);
  }

  public void addBatchPersistListeners(Collection<BatchPersistListener> listeners) {
    if (batchPersistListeners == null) {
      batchPersistListeners = new ArrayList<>();
    }
    batchPersistListeners.addAll(listeners);
  }

  public void setBatchPersistListeners(@Nullable Collection<BatchPersistListener> listeners) {
    if (listeners == null) {
      this.batchPersistListeners = null;
    }
    else {
      if (batchPersistListeners == null) {
        batchPersistListeners = new ArrayList<>();
      }
      else {
        batchPersistListeners.clear();
      }
      batchPersistListeners.addAll(listeners);
    }
  }

  //---------------------------------------------------------------------
  // Implementation of EntityManager
  //---------------------------------------------------------------------

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
      public Void doInConnection(@NonNull Connection connection) throws SQLException, DataAccessException {
        try (PreparedStatement statement = prepareStatement(connection, sql, returnGeneratedKeys)) {
          setPersistParameter(entity, statement, entityMetadata);
          // execute
          int updateCount = statement.executeUpdate();
          assertUpdateCount(sql, updateCount, 1);

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

  private static void assertUpdateCount(String sql, int actualCount, int expectCount) {
    if (actualCount != expectCount) {
      throw new JdbcUpdateAffectedIncorrectNumberOfRowsException(sql, expectCount, actualCount);
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
    repositoryManager.runInTransaction((connection, argument) -> {
      int maxBatchRecords = getMaxBatchRecords();
      var statements = new HashMap<Class<?>, PreparedBatch>();
      try {
        for (Object entity : entities) {
          Class<?> entityClass = entity.getClass();
          PreparedBatch batch = statements.get(entityClass);
          if (batch == null) {
            EntityMetadata entityMetadata = entityMetadataFactory.getEntityMetadata(entityClass);
            String sql = insert(entityMetadata);
            batch = new PreparedBatch(connection.getJdbcConnection(), sql, entityMetadata, returnGeneratedKeys);
            statements.put(entityClass, batch);
          }
          batch.addBatchUpdate(entity, maxBatchRecords);
        }

        for (PreparedBatch preparedBatch : statements.values()) {
          preparedBatch.explicitExecuteBatch(returnGeneratedKeys);
        }
      }
      catch (DataAccessException e) {
        throw e;
      }
      catch (SQLException e) {
        throw translateException("Running in transaction", null, e);
      }
      catch (Throwable ex) {
        throw new PersistenceException("Batch persist entities failed", ex);
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
        fireBatchExecution(true);
      }
    }

    public void explicitExecuteBatch(boolean returnGeneratedKeys) throws SQLException {
      executeBatch(statement, returnGeneratedKeys);
      fireBatchExecution(false);
      try {
        statement.close();
      }
      catch (SQLException ex) {
        if (repositoryManager.isCatchResourceCloseErrors()) {
          throw translateException("Closing statement", sql, ex);
        }
        else {
          logger.error("Closing statement: '{}' failed", statement, ex);
        }
      }
    }

    private void fireBatchExecution(boolean implicitExecution) {
      if (CollectionUtils.isNotEmpty(batchPersistListeners)) {
        for (BatchPersistListener listener : batchPersistListeners) {
          listener.executeBatch(entities, implicitExecution);
        }
      }
    }

    private void executeBatch(PreparedStatement statement, boolean returnGeneratedKeys) throws SQLException {
      if (stmtLogger.isDebugEnabled()) {
        stmtLogger.logStatement(LogMessage.format("Executing batch size: {}", entities.size()), sql);
      }
      int[] updateCounts = statement.executeBatch();
      assertUpdateCount(sql, updateCounts.length, entities.size());
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

  }

  @Override
  public void updateById(Object entity) {
    updateById(entity, PropertyUpdateStrategy.updateNoneNull());
  }

  @Override
  public void updateById(Object entity, PropertyUpdateStrategy strategy) {
    Class<?> entityClass = entity.getClass();
    EntityMetadata metadata = entityMetadataFactory.getEntityMetadata(entityClass);
    EntityProperty idProperty = metadata.idProperty;
    Object id = idProperty.getValue(entity);
    if (id == null) {
      throw new IllegalArgumentException("Update an entity, ID property is required");
    }

    Update updateStmt = new Update();
    updateStmt.setTableName(metadata.tableName);
    updateStmt.addWhereColumn(idProperty.columnName);

    for (EntityProperty property : metadata.entityProperties) {
      if (property.property != idProperty.property
              && strategy.shouldUpdate(entity, property)) {
        updateStmt.addColumn(property.columnName);
      }
    }

    String sql = updateStmt.toStatementString();

    if (stmtLogger.isDebugEnabled()) {
      stmtLogger.logStatement(LogMessage.format("Update entity using ID: '{}'", id), sql);
    }

    DataSource dataSource = obtainDataSource();
    Connection con = DataSourceUtils.getConnection(dataSource);
    try {
      PreparedStatement statement = prepareStatement(con, sql, false);

      int idx = 1;
      for (EntityProperty property : metadata.entityProperties) {
        if (property.property != idProperty.property
                && strategy.shouldUpdate(entity, property)) {
          Object propertyValue = property.getValue(entity);
          property.setParameter(statement, idx++, propertyValue);
        }
      }

      // last one is ID
      idProperty.setParameter(statement, idx, id);
      int updateCount = statement.executeUpdate();
      assertUpdateCount(sql, updateCount, 1);
    }
    catch (SQLException ex) {
      // Release Connection early, to avoid potential connection pool deadlock
      // in the case when the exception translator hasn't been initialized yet.
      DataSourceUtils.releaseConnection(con, dataSource);
      throw translateException("Update entity By ID", sql, ex);
    }

  }

  @Override
  public void updateBy(Object entity, String where, PropertyUpdateStrategy strategy) {
    Class<?> entityClass = entity.getClass();
    EntityMetadata metadata = entityMetadataFactory.getEntityMetadata(entityClass);

    Update updateStmt = new Update();
    updateStmt.setTableName(metadata.tableName);
    updateStmt.addWhereColumn(metadata.idProperty.columnName);

    EntityProperty updateBy = null;
    for (EntityProperty property : metadata.entityProperties) {
      // columnName or property name
      if (Objects.equals(where, property.columnName)
              || Objects.equals(where, property.property.getName())) {
        updateBy = property;
      }
      else if (strategy.shouldUpdate(entity, property)) {
        updateStmt.addColumn(property.columnName);
      }
    }

    if (updateBy == null) {
      throw new IllegalArgumentException("Update an entity, 'where' property '" + where + "' is not found");
    }

    Object updateByValue = updateBy.getValue(entity);
    if (updateByValue == null) {
      throw new IllegalArgumentException("Update an entity, 'where' property '" + where + "' is required");
    }

    String sql = updateStmt.toStatementString();
    if (stmtLogger.isDebugEnabled()) {
      stmtLogger.logStatement(LogMessage.format("Update entity using {} : '{}'", where, updateByValue), sql);
    }

    DataSource dataSource = obtainDataSource();
    Connection con = DataSourceUtils.getConnection(dataSource);
    try {
      PreparedStatement statement = prepareStatement(con, sql, false);

      int idx = 1;
      for (EntityProperty property : metadata.entityProperties) {
        if (strategy.shouldUpdate(entity, property)) {
          Object propertyValue = property.getValue(entity);
          property.setParameter(statement, idx++, propertyValue);
        }
      }

      // last one is where
      updateBy.setParameter(statement, idx, updateByValue);
      int updateCount = statement.executeUpdate();
      assertUpdateCount(sql, updateCount, 1);
    }
    catch (SQLException ex) {
      // Release Connection early, to avoid potential connection pool deadlock
      // in the case when the exception translator hasn't been initialized yet.
      DataSourceUtils.releaseConnection(con, dataSource);
      throw translateException("Update entity By " + where, sql, ex);
    }

  }

  @Override
  public void delete(Class<?> entityClass, Object id) {
    EntityMetadata metadata = entityMetadataFactory.getEntityMetadata(entityClass);
    StringBuilder sql = new StringBuilder();

    sql.append("DELETE FROM ");
    sql.append(metadata.tableName);
    sql.append(" WHERE `");
    sql.append(metadata.idProperty.columnName);
    sql.append("` = ? ");

    if (stmtLogger.isDebugEnabled()) {
      stmtLogger.logStatement(LogMessage.format("Delete entity using ID: {}", id), sql.toString());
    }

    DataSource dataSource = obtainDataSource();
    Connection con = DataSourceUtils.getConnection(dataSource);
    try {
      PreparedStatement statement = prepareStatement(con, sql.toString(), false);
      metadata.idProperty.setParameter(statement, 1, id);
      statement.executeUpdate();
    }
    catch (SQLException ex) {
      // Release Connection early, to avoid potential connection pool deadlock
      // in the case when the exception translator hasn't been initialized yet.
      DataSourceUtils.releaseConnection(con, dataSource);
      throw translateException("Delete entity using ID", sql.toString(), ex);
    }

  }

  @Override
  public int delete(Object entity) {
    Class<?> entityClass = entity.getClass();
    EntityMetadata metadata = entityMetadataFactory.getEntityMetadata(entityClass);
    Object id = metadata.idProperty.getValue(entity);

    StringBuilder sql = new StringBuilder();
    sql.append("DELETE FROM ");
    sql.append(metadata.tableName);
    if (id != null) {
      // delete by id
      sql.append(" WHERE `");
      sql.append(metadata.idProperty.columnName);
      sql.append("` = ? ");
    }
    else {
      sql.append(" WHERE ");
      String and = "";
      for (EntityProperty property : metadata.entityProperties) {
        Object propertyValue = property.getValue(entity);
        if (propertyValue != null) {
          sql.append(and);
          sql.append(" `");
          sql.append(property.columnName);
          sql.append("` = ? ");
          and = " AND";
        }
      }
    }

    if (stmtLogger.isDebugEnabled()) {
      stmtLogger.logStatement(LogMessage.format("Delete entity"), sql.toString());
    }

    DataSource dataSource = obtainDataSource();
    Connection con = DataSourceUtils.getConnection(dataSource);
    try {
      PreparedStatement statement = prepareStatement(con, sql.toString(), false);
      if (id != null) {
        metadata.idProperty.setParameter(statement, 1, id);
      }
      else {
        int idx = 1;
        for (EntityProperty property : metadata.entityProperties) {
          Object propertyValue = property.getValue(entity);
          if (propertyValue != null) {
            property.setParameter(statement, idx++, propertyValue);
          }
        }
      }

      return statement.executeUpdate();
    }
    catch (SQLException ex) {
      // Release Connection early, to avoid potential connection pool deadlock
      // in the case when the exception translator hasn't been initialized yet.
      DataSourceUtils.releaseConnection(con, dataSource);
      throw translateException("Delete entity", sql.toString(), ex);
    }

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

    DataSource dataSource = obtainDataSource();
    Connection con = DataSourceUtils.getConnection(dataSource);
    try {
      PreparedStatement statement = prepareStatement(con, sql.toString(), false);
      metadata.idProperty.setParameter(statement, 1, id);

      ResultSet resultSet = statement.executeQuery();
      var iterator = new EntityIterator<T>(con, resultSet, entityClass);

      return iterator.hasNext() ? iterator.next() : null;
    }
    catch (SQLException ex) {
      // Release Connection early, to avoid potential connection pool deadlock
      // in the case when the exception translator hasn't been initialized yet.
      DataSourceUtils.releaseConnection(con, dataSource);
      throw translateException("Fetch entity By ID", sql.toString(), ex);
    }
  }

  @Nullable
  @Override
  @SuppressWarnings("unchecked")
  public <T> T findFirst(T entity) throws DataAccessException {
    return findFirst((Class<T>) entity.getClass(), entity);
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
  public <T> T findFirst(Class<T> entityClass, @Nullable QueryCondition conditions) throws DataAccessException {
    try (ResultSetIterator<T> iterator = iterate(entityClass, conditions)) {
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
  @SuppressWarnings("unchecked")
  public <T> T findUnique(T entity) throws DataAccessException {
    try (ResultSetIterator<T> iterator = iterate((Class<T>) entity.getClass(), entity)) {
      T returnValue = null;
      while (iterator.hasNext()) {
        if (returnValue != null) {
          throw new IncorrectResultSizeDataAccessException(1);
        }
        returnValue = iterator.next();
      }
      return returnValue;
    }
  }

  @Nullable
  @Override
  public <T> T findUnique(Class<T> entityClass, Object query) throws DataAccessException {
    try (ResultSetIterator<T> iterator = iterate(entityClass, query)) {
      T returnValue = null;
      while (iterator.hasNext()) {
        if (returnValue != null) {
          throw new IncorrectResultSizeDataAccessException(1);
        }
        returnValue = iterator.next();
      }
      return returnValue;
    }
  }

  @Override
  public <T> T findUnique(Class<T> entityClass, @Nullable QueryCondition conditions) throws DataAccessException {
    try (ResultSetIterator<T> iterator = iterate(entityClass, conditions)) {
      T returnValue = null;
      while (iterator.hasNext()) {
        if (returnValue != null) {
          throw new IncorrectResultSizeDataAccessException(1);
        }
        returnValue = iterator.next();
      }
      return returnValue;
    }
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
  @SuppressWarnings("unchecked")
  public <K, T> Map<K, T> find(Class<T> entityClass, Object params, String mapKey) throws DataAccessException {
    LinkedHashMap<K, T> entities = new LinkedHashMap<>();
    try (ResultSetIterator<T> iterator = iterate(entityClass, params)) {
      EntityMetadata entityMetadata = entityMetadataFactory.getEntityMetadata(entityClass);
      BeanProperty beanProperty = entityMetadata.root.obtainBeanProperty(mapKey);
      while (iterator.hasNext()) {
        T entity = iterator.next();
        Object propertyValue = beanProperty.getValue(entity);
        entities.put((K) propertyValue, entity);
      }
    }
    return entities;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <K, T> Map<K, T> find(Class<T> entityClass,
          @Nullable QueryCondition conditions, String mapKey) throws DataAccessException {
    var entities = new LinkedHashMap<K, T>();
    try (ResultSetIterator<T> iterator = iterate(entityClass, conditions)) {
      EntityMetadata entityMetadata = entityMetadataFactory.getEntityMetadata(entityClass);
      BeanProperty beanProperty = entityMetadata.root.obtainBeanProperty(mapKey);
      while (iterator.hasNext()) {
        T entity = iterator.next();
        Object propertyValue = beanProperty.getValue(entity);
        entities.put((K) propertyValue, entity);
      }
    }
    return entities;
  }

  @Override
  public <T> List<T> find(Class<T> entityClass, @Nullable QueryCondition conditions) throws DataAccessException {
    ArrayList<T> entities = new ArrayList<>();
    try (ResultSetIterator<T> iterator = iterate(entityClass, conditions)) {
      while (iterator.hasNext()) {
        entities.add(iterator.next());
      }
    }
    return entities;
  }

  @Override
  public <T> void iterate(Class<T> entityClass,
          Object params, Consumer<T> entityConsumer) throws DataAccessException {
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
      stmtLogger.logStatement(LogMessage.format("Lookup entity using query-model: {}", params), sql.toString());
    }

    DataSource dataSource = obtainDataSource();
    Connection con = DataSourceUtils.getConnection(dataSource);
    try {
      PreparedStatement statement = prepareStatement(con, sql.toString(), false);
      int idx = 1;
      for (Condition condition : conditions) {
        condition.typeHandler.setParameter(statement, idx++, condition.propertyValue);
      }
      ResultSet resultSet = statement.executeQuery();
      return new EntityIterator<>(con, resultSet, entityClass);
    }
    catch (SQLException ex) {
      // Release Connection early, to avoid potential connection pool deadlock
      // in the case when the exception translator hasn't been initialized yet.
      DataSourceUtils.releaseConnection(con, dataSource);
      throw translateException("Iterate entities with query-model", sql.toString(), ex);
    }
  }

  @Override
  public <T> void iterate(Class<T> entityClass,
          @Nullable QueryCondition conditions, Consumer<T> entityConsumer) throws DataAccessException {
    try (ResultSetIterator<T> iterator = iterate(entityClass, conditions)) {
      while (iterator.hasNext()) {
        entityConsumer.accept(iterator.next());
      }
    }
  }

  @Override
  public <T> ResultSetIterator<T> iterate(
          Class<T> entityClass, @Nullable QueryCondition conditions) throws DataAccessException {
    EntityMetadata metadata = entityMetadataFactory.getEntityMetadata(entityClass);

    StringBuilder sql = new StringBuilder();
    sql.append("SELECT * FROM ");
    sql.append(metadata.tableName);

    if (conditions != null) {
      sql.append(" WHERE ");
      // WHERE column_name operator value;
      conditions.render(sql);
    }

    DataSource dataSource = obtainDataSource();
    Connection con = DataSourceUtils.getConnection(dataSource);
    try {
      PreparedStatement statement = prepareStatement(con, sql.toString(), false);
      if (conditions != null) {
        conditions.setParameter(statement);
      }

      if (stmtLogger.isDebugEnabled()) {
        stmtLogger.logStatement("Lookup entities", sql.toString());
      }

      ResultSet resultSet = statement.executeQuery();
      return new EntityIterator<>(con, resultSet, entityClass);
    }
    catch (SQLException ex) {
      // Release Connection early, to avoid potential connection pool deadlock
      // in the case when the exception translator hasn't been initialized yet.
      DataSourceUtils.releaseConnection(con, dataSource);
      throw translateException("Iterate entities with query-conditions", sql.toString(), ex);
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

  final class EntityIterator<T> extends ResultSetIterator<T> {
    private final ResultSetHandler<T> handler;
    private final Connection connection;

    private EntityIterator(Connection connection, ResultSet rs, Class<?> entityClass) {
      super(rs);
      this.connection = connection;
      try {
        var factory = new DefaultResultSetHandlerFactory<T>(
                new JdbcBeanMetadata(entityClass, repositoryManager.isDefaultCaseSensitive(), true, true),
                repositoryManager, null);
        this.handler = factory.getResultSetHandler(rs.getMetaData());
      }
      catch (SQLException e) {
        throw translateException("Get ResultSetHandler", null, e);
      }
    }

    @Override
    protected T readNext(ResultSet resultSet) throws SQLException {
      return handler.handle(resultSet);
    }

    @Override
    protected RuntimeException handleReadError(SQLException ex) {
      return translateException("Read Entity", null, ex);
    }

    @Override
    public void close() {
      try {
        DataSourceUtils.doReleaseConnection(connection, getDataSource());
      }
      catch (SQLException e) {
        if (repositoryManager.isCatchResourceCloseErrors()) {
          throw translateException("Closing ResultSet", null, e);
        }
        else {
          logger.debug("Could not close JDBC Connection", e);
        }
      }

      try {
        resultSet.close();
      }
      catch (SQLException e) {
        if (repositoryManager.isCatchResourceCloseErrors()) {
          throw translateException("Closing ResultSet", null, e);
        }
      }
    }

  }

}
