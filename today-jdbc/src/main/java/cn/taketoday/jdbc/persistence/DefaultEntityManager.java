/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.jdbc.persistence;

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
import cn.taketoday.jdbc.datasource.DataSourceUtils;
import cn.taketoday.jdbc.result.DefaultResultSetHandlerFactory;
import cn.taketoday.jdbc.result.JdbcBeanMetadata;
import cn.taketoday.jdbc.result.ResultSetHandler;
import cn.taketoday.jdbc.result.ResultSetIterator;
import cn.taketoday.jdbc.support.JdbcAccessor;
import cn.taketoday.jdbc.support.JdbcUtils;
import cn.taketoday.jdbc.type.TypeHandler;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.LogMessage;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;

/**
 * Default EntityManager implementation
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/10 22:28
 */
public class DefaultEntityManager extends JdbcAccessor implements EntityManager {

  private EntityMetadataFactory entityMetadataFactory = new DefaultEntityMetadataFactory();

  private final RepositoryManager repositoryManager;

  private int maxBatchRecords = 0;

  @Nullable
  private ArrayList<BatchPersistListener> batchPersistListeners;

  /**
   * a flag indicating whether auto-generated keys should be returned;
   */
  private boolean returnGeneratedKeys = true;

  private PropertyUpdateStrategy defaultUpdateStrategy = PropertyUpdateStrategy.noneNull();

  public DefaultEntityManager(RepositoryManager repositoryManager) {
    setDataSource(repositoryManager.getDataSource());
    setExceptionTranslator(repositoryManager.getExceptionTranslator());
    this.repositoryManager = repositoryManager;
  }

  public void setDefaultUpdateStrategy(PropertyUpdateStrategy defaultUpdateStrategy) {
    Assert.notNull(defaultUpdateStrategy, "defaultUpdateStrategy is required");
    this.defaultUpdateStrategy = defaultUpdateStrategy;
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
    persist(entity, defaultUpdateStrategy(), returnGeneratedKeys);
  }

  @Override
  public void persist(Object entity, boolean returnGeneratedKeys) throws DataAccessException {
    persist(entity, defaultUpdateStrategy(), returnGeneratedKeys);
  }

  @Override
  public void persist(Object entity, @Nullable PropertyUpdateStrategy strategy) throws DataAccessException {
    persist(entity, strategy, returnGeneratedKeys);
  }

  @Override
  public void persist(Object entity, @Nullable PropertyUpdateStrategy strategy, boolean returnGeneratedKeys) throws DataAccessException {
    if (strategy == null) {
      strategy = defaultUpdateStrategy();
    }
    Class<?> entityClass = entity.getClass();
    EntityMetadata entityMetadata = entityMetadataFactory.getEntityMetadata(entityClass);
    String sql = insert(entityMetadata, entity, strategy);

    if (stmtLogger.isDebugEnabled()) {
      stmtLogger.logStatement(LogMessage.format("Persist entity: {}, generatedKeys={}",
              ObjectUtils.toHexString(entity), returnGeneratedKeys), sql);
    }

    DataSource dataSource = obtainDataSource();
    Connection con = DataSourceUtils.getConnection(dataSource);
    try {
      try (PreparedStatement statement = prepareStatement(con, sql, returnGeneratedKeys)) {
        setPersistParameter(entity, statement, strategy, entityMetadata);
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
    }
    catch (SQLException ex) {
      throw translateException("Persist entity", sql, ex);
    }
    finally {
      DataSourceUtils.releaseConnection(con, dataSource);
    }
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
    persist(entities, defaultUpdateStrategy(), returnGeneratedKeys);
  }

  @Override
  public void persist(Iterable<?> entities, boolean returnGeneratedKeys) throws DataAccessException {
    persist(entities, defaultUpdateStrategy(), returnGeneratedKeys);
  }

  @Override
  public void persist(Iterable<?> entities, @Nullable PropertyUpdateStrategy strategy) throws DataAccessException {
    persist(entities, strategy, returnGeneratedKeys);
  }

  @Override
  public void persist(Iterable<?> entities, @Nullable PropertyUpdateStrategy strategy, boolean returnGeneratedKeys)
          throws DataAccessException //
  {
    if (strategy == null) {
      strategy = defaultUpdateStrategy();
    }
    repositoryManager.runInTransaction((connection, arg) -> {
      int maxBatchRecords = getMaxBatchRecords();
      var statements = new HashMap<Class<?>, PreparedBatch>();
      try {
        for (Object entity : entities) {
          Class<?> entityClass = entity.getClass();
          PreparedBatch batch = statements.get(entityClass);
          if (batch == null) {
            EntityMetadata entityMetadata = entityMetadataFactory.getEntityMetadata(entityClass);
            String sql = insert(entityMetadata, entity, arg);
            batch = new PreparedBatch(connection.getJdbcConnection(), sql, arg, entityMetadata, returnGeneratedKeys);
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
    }, strategy);
  }

  private static void setPersistParameter(Object entity, PreparedStatement statement,
          PropertyUpdateStrategy strategy, EntityMetadata entityMetadata) throws SQLException {
    int idx = 1;
    for (EntityProperty property : entityMetadata.entityProperties) {
      if (strategy.shouldUpdate(entity, property)) {
        property.setTo(statement, idx++, entity);
      }
    }
  }

  class PreparedBatch {
    public final String sql;
    public int currentBatchRecords = 0;
    public final EntityMetadata entityMetadata;
    public final PreparedStatement statement;
    public final boolean returnGeneratedKeys;
    public final PropertyUpdateStrategy strategy;
    public final ArrayList<Object> entities = new ArrayList<>();

    PreparedBatch(Connection connection, String sql, PropertyUpdateStrategy strategy,
            EntityMetadata entityMetadata, boolean returnGeneratedKeys) throws SQLException {
      this.sql = sql;
      this.strategy = strategy;
      this.statement = prepareStatement(connection, sql, returnGeneratedKeys);
      this.entityMetadata = entityMetadata;
      this.returnGeneratedKeys = returnGeneratedKeys;
    }

    public void addBatchUpdate(Object entity, int maxBatchRecords) throws SQLException {
      entities.add(entity);
      PreparedStatement statement = this.statement;
      setPersistParameter(entity, statement, strategy, entityMetadata);
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
    updateById(entity, null);
  }

  @Override
  public void updateById(Object entity, @Nullable PropertyUpdateStrategy strategy) {
    if (strategy == null) {
      strategy = defaultUpdateStrategy();
    }
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
    PreparedStatement statement = null;
    try {
      statement = prepareStatement(con, sql, false);
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
      throw translateException("Update entity By ID", sql, ex);
    }
    finally {
      JdbcUtils.closeStatement(statement);
      DataSourceUtils.releaseConnection(con, dataSource);
    }
  }

  @Override
  public void updateBy(Object entity, String where) {
    updateBy(entity, where, null);
  }

  @Override
  public void updateBy(Object entity, String where, @Nullable PropertyUpdateStrategy strategy) {
    if (strategy == null) {
      strategy = defaultUpdateStrategy();
    }

    Class<?> entityClass = entity.getClass();
    EntityMetadata metadata = entityMetadataFactory.getEntityMetadata(entityClass);

    Update updateStmt = new Update();
    updateStmt.setTableName(metadata.tableName);

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
      throw new IllegalArgumentException("Update an entity, 'where' property '" + where + "' not found");
    }

    updateStmt.addWhereColumn(updateBy.columnName);

    Object updateByValue = updateBy.getValue(entity);
    if (updateByValue == null) {
      throw new IllegalArgumentException(
              "Update an entity, 'where' property value '" + where + "' is required");
    }

    String sql = updateStmt.toStatementString();
    if (stmtLogger.isDebugEnabled()) {
      stmtLogger.logStatement(LogMessage.format("Update entity using {} : '{}'", where, updateByValue), sql);
    }

    DataSource dataSource = obtainDataSource();
    Connection con = DataSourceUtils.getConnection(dataSource);
    PreparedStatement statement = null;
    try {
      statement = prepareStatement(con, sql, false);
      int idx = 1;
      for (EntityProperty property : metadata.entityProperties) {
        if ((!Objects.equals(where, property.columnName)
                && !Objects.equals(where, property.property.getName()))
                && strategy.shouldUpdate(entity, property)) {
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
      throw translateException("Update entity By " + where, sql, ex);
    }
    finally {
      JdbcUtils.closeStatement(statement);
      DataSourceUtils.releaseConnection(con, dataSource);
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
    PreparedStatement statement = null;
    try {
      statement = prepareStatement(con, sql.toString(), false);
      metadata.idProperty.setParameter(statement, 1, id);
      statement.executeUpdate();
    }
    catch (SQLException ex) {
      throw translateException("Delete entity using ID", sql.toString(), ex);
    }
    finally {
      JdbcUtils.closeStatement(statement);
      DataSourceUtils.releaseConnection(con, dataSource);
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
    PreparedStatement statement = null;
    try {
      statement = prepareStatement(con, sql.toString(), false);
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
      throw translateException("Delete entity", sql.toString(), ex);
    }
    finally {
      JdbcUtils.closeStatement(statement);
      DataSourceUtils.releaseConnection(con, dataSource);
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

      var iterator = new EntityIterator<T>(con, statement, entityClass);

      return iterator.hasNext() ? iterator.next() : null;
    }
    catch (SQLException ex) {
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
  public <T> List<T> find(Class<T> entityClass) throws DataAccessException {
    return find(entityClass, null);
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
  public <K, T> Map<K, T> find(Class<T> entityClass, @Nullable QueryCondition conditions, String mapKey)
          throws DataAccessException //
  {
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

    if (!columnNamesBuf.isEmpty()) {
      sql.append(columnNamesBuf.substring(2));
    }

    if (stmtLogger.isDebugEnabled()) {
      stmtLogger.logStatement(LogMessage.format("Lookup entity using query-model: {}", params), sql.toString());
    }

    DataSource dataSource = obtainDataSource();
    Connection con = DataSourceUtils.getConnection(dataSource);
    PreparedStatement statement;
    try {
      statement = prepareStatement(con, sql.toString(), false);
      int idx = 1;
      for (Condition condition : conditions) {
        condition.typeHandler.setParameter(statement, idx++, condition.propertyValue);
      }
      return new EntityIterator<>(con, statement, entityClass);
    }
    catch (SQLException ex) {
      DataSourceUtils.releaseConnection(con, dataSource);
      throw translateException("Iterate entities with query-model", sql.toString(), ex);
    }
  }

  @Override
  public <T> void iterate(Class<T> entityClass, @Nullable QueryCondition conditions, Consumer<T> entityConsumer)
          throws DataAccessException //
  {
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

    DataSource dataSource = obtainDataSource();
    Connection con = DataSourceUtils.getConnection(dataSource);
    PreparedStatement statement;
    try {
      statement = prepareStatement(con, sql.toString(), false);
      if (conditions != null) {
        conditions.setParameter(statement);
      }

      if (stmtLogger.isDebugEnabled()) {
        stmtLogger.logStatement("Lookup entities", sql.toString());
      }

      return new EntityIterator<>(con, statement, entityClass);
    }
    catch (SQLException ex) {
      DataSourceUtils.releaseConnection(con, dataSource);
      throw translateException("Iterate entities with query-conditions", sql.toString(), ex);
    }
  }

  /**
   * default PropertyUpdateStrategy
   */
  protected PropertyUpdateStrategy defaultUpdateStrategy() {
    return defaultUpdateStrategy;
  }

  //

  static String insert(EntityMetadata entityMetadata, Object entity, PropertyUpdateStrategy strategy) {
    StringBuilder sql = new StringBuilder();
    sql.append("INSERT INTO ").append(entityMetadata.tableName);

    StringBuilder columnNamesBuf = new StringBuilder();
    StringBuilder placeholderBuf = new StringBuilder();

    for (EntityProperty property : entityMetadata.entityProperties) {
      if (strategy.shouldUpdate(entity, property)) {
        columnNamesBuf.append(", `").append(property.columnName).append('`');
        placeholderBuf.append(", ?");
      }
    }

    if (!columnNamesBuf.isEmpty()) {
      sql.append("(").append(columnNamesBuf.substring(2)).append(")");
      sql.append(" VALUES (").append(placeholderBuf.substring(2)).append(")");
    }
    return sql.toString();
  }

  final class EntityIterator<T> extends ResultSetIterator<T> {
    private final Connection connection;
    private final PreparedStatement statement;
    private final ResultSetHandler<T> handler;

    private EntityIterator(Connection connection,
            PreparedStatement statement, Class<?> entityClass) throws SQLException {
      super(statement.executeQuery());
      this.statement = statement;
      this.connection = connection;
      try {
        var factory = new DefaultResultSetHandlerFactory<T>(
                new JdbcBeanMetadata(entityClass, repositoryManager.isDefaultCaseSensitive(), true, true),
                repositoryManager, null);
        this.handler = factory.getResultSetHandler(resultSet.getMetaData());
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
        statement.close();
      }
      catch (SQLException e) {
        if (repositoryManager.isCatchResourceCloseErrors()) {
          throw translateException("Closing Statement", null, e);
        }
        else {
          logger.trace("Could not close JDBC Statement", e);
        }
      }

      try {
        resultSet.close();
      }
      catch (SQLException e) {
        if (repositoryManager.isCatchResourceCloseErrors()) {
          throw translateException("Closing ResultSet", null, e);
        }
        else {
          logger.trace("Could not close JDBC ResultSet", e);
        }
      }
    }

  }

}
