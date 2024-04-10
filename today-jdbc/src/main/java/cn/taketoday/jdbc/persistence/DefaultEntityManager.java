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

package cn.taketoday.jdbc.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.sql.DataSource;

import cn.taketoday.core.Pair;
import cn.taketoday.dao.DataAccessException;
import cn.taketoday.dao.DataRetrievalFailureException;
import cn.taketoday.dao.InvalidDataAccessApiUsageException;
import cn.taketoday.jdbc.DefaultResultSetHandlerFactory;
import cn.taketoday.jdbc.GeneratedKeysException;
import cn.taketoday.jdbc.JdbcBeanMetadata;
import cn.taketoday.jdbc.JdbcUpdateAffectedIncorrectNumberOfRowsException;
import cn.taketoday.jdbc.PersistenceException;
import cn.taketoday.jdbc.RepositoryManager;
import cn.taketoday.jdbc.core.ResultSetExtractor;
import cn.taketoday.jdbc.datasource.DataSourceUtils;
import cn.taketoday.jdbc.format.SqlStatementLogger;
import cn.taketoday.jdbc.persistence.dialect.Platform;
import cn.taketoday.jdbc.persistence.sql.Insert;
import cn.taketoday.jdbc.persistence.sql.OrderByClause;
import cn.taketoday.jdbc.persistence.sql.Restriction;
import cn.taketoday.jdbc.persistence.sql.SimpleSelect;
import cn.taketoday.jdbc.persistence.sql.Update;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.LogMessage;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.transaction.TransactionDefinition;
import cn.taketoday.util.CollectionUtils;

/**
 * Default EntityManager implementation
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/10 22:28
 */
public class DefaultEntityManager implements EntityManager {

  private static final Logger logger = LoggerFactory.getLogger(DefaultEntityManager.class);

  private EntityMetadataFactory entityMetadataFactory = new DefaultEntityMetadataFactory();

  private final RepositoryManager repositoryManager;

  private int maxBatchRecords = 0;

  @Nullable
  private ArrayList<BatchPersistListener> batchPersistListeners;

  /**
   * a flag indicating whether auto-generated keys should be returned;
   */
  private boolean autoGenerateId = true;

  private PropertyUpdateStrategy defaultUpdateStrategy = PropertyUpdateStrategy.noneNull();

  private Pageable defaultPageable = Pageable.of(10, 1);

  private Platform platform = Platform.forClasspath();

  private SqlStatementLogger stmtLogger = SqlStatementLogger.sharedInstance;

  @Nullable
  private TransactionDefinition transactionConfig = TransactionDefinition.withDefaults();

  private QueryHandlerFactories handlerFactories = new QueryHandlerFactories(entityMetadataFactory);

  private final DataSource dataSource;

  public DefaultEntityManager(RepositoryManager repositoryManager) {
    this.dataSource = repositoryManager.getDataSource();
    this.repositoryManager = repositoryManager;
  }

  public void setPlatform(@Nullable Platform platform) {
    this.platform = platform == null ? Platform.forClasspath() : platform;
  }

  public void setDefaultUpdateStrategy(PropertyUpdateStrategy defaultUpdateStrategy) {
    Assert.notNull(defaultUpdateStrategy, "defaultUpdateStrategy is required");
    this.defaultUpdateStrategy = defaultUpdateStrategy;
  }

  public void setDefaultPageable(Pageable defaultPageable) {
    Assert.notNull(defaultPageable, "defaultPageable is required");
    this.defaultPageable = defaultPageable;
  }

  public void setEntityMetadataFactory(EntityMetadataFactory entityMetadataFactory) {
    Assert.notNull(entityMetadataFactory, "entityMetadataFactory is required");
    this.entityMetadataFactory = entityMetadataFactory;
    this.handlerFactories = new QueryHandlerFactories(entityMetadataFactory);
  }

  /**
   * Set a flag indicating whether auto-generated keys should be returned;
   *
   * @param autoGenerateId a flag indicating whether auto-generated keys should be returned;
   */
  public void setAutoGenerateId(boolean autoGenerateId) {
    this.autoGenerateId = autoGenerateId;
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

  public void setStatementLogger(SqlStatementLogger stmtLogger) {
    Assert.notNull(stmtLogger, "SqlStatementLogger is required");
    this.stmtLogger = stmtLogger;
  }

  /**
   * Set transaction config
   *
   * @param definition the TransactionDefinition instance (can be {@code null} for defaults),
   * describing propagation behavior, isolation level, timeout etc.
   */
  public void setTransactionConfig(@Nullable TransactionDefinition definition) {
    this.transactionConfig = definition;
  }

  // ---------------------------------------------------------------------
  // Implementation of EntityManager
  // ---------------------------------------------------------------------

  @Override
  public int persist(Object entity) throws DataAccessException {
    return persist(entity, defaultUpdateStrategy(), autoGenerateId);
  }

  @Override
  public int persist(Object entity, boolean autoGenerateId) throws DataAccessException {
    return persist(entity, defaultUpdateStrategy(), autoGenerateId);
  }

  @Override
  public int persist(Object entity, @Nullable PropertyUpdateStrategy strategy) throws DataAccessException {
    return persist(entity, strategy, autoGenerateId);
  }

  @Override
  public int persist(Object entity, @Nullable PropertyUpdateStrategy strategy, boolean autoGenerateId) throws DataAccessException {
    EntityMetadata entityMetadata = entityMetadataFactory.getEntityMetadata(entity.getClass());
    if (strategy == null) {
      strategy = defaultUpdateStrategy();
    }

    var pair = insertStatement(strategy, entity, entityMetadata);

    if (stmtLogger.isDebugEnabled()) {
      stmtLogger.logStatement(LogMessage.format("Persisting entity: {}", entity), pair.first);
    }

    Connection con = DataSourceUtils.getConnection(dataSource);
    PreparedStatement statement = null;
    ResultSet generatedKeys = null;
    try {
      autoGenerateId = autoGenerateId || entityMetadata.autoGeneratedId;
      statement = prepareStatement(con, pair.first, autoGenerateId);
      setParameters(entity, pair.second, statement);
      // execute
      int updateCount = statement.executeUpdate();
      if (autoGenerateId) {
        if (entityMetadata.idProperty != null) {
          try {
            generatedKeys = statement.getGeneratedKeys();
            if (generatedKeys.next()) {
              entityMetadata.idProperty.setProperty(entity, generatedKeys, 1);
            }
          }
          catch (SQLException e) {
            throw new GeneratedKeysException("Cannot get generated keys", e);
          }
        }
      }
      return updateCount;
    }
    catch (SQLException ex) {
      throw translateException("Persisting entity", pair.first, ex);
    }
    finally {
      closeResource(con, statement, generatedKeys);
    }
  }

  @Override
  public void persist(Iterable<?> entities) throws DataAccessException {
    persist(entities, defaultUpdateStrategy(), autoGenerateId);
  }

  @Override
  public void persist(Iterable<?> entities, boolean autoGenerateId) throws DataAccessException {
    persist(entities, defaultUpdateStrategy(), autoGenerateId);
  }

  @Override
  public void persist(Iterable<?> entities, @Nullable PropertyUpdateStrategy strategy) throws DataAccessException {
    persist(entities, strategy, autoGenerateId);
  }

  @Override
  public void persist(Iterable<?> entities, @Nullable PropertyUpdateStrategy strategy, boolean autoGenerateId)
          throws DataAccessException //
  {
    if (strategy == null) {
      strategy = defaultUpdateStrategy();
    }
    try (var transaction = repositoryManager.beginTransaction(transactionConfig)) {
      int maxBatchRecords = getMaxBatchRecords();
      var statements = new HashMap<Class<?>, PreparedBatch>(8);
      try {
        for (Object entity : entities) {
          Class<?> entityClass = entity.getClass();
          PreparedBatch batch = statements.get(entityClass);
          if (batch == null) {
            EntityMetadata entityMetadata = entityMetadataFactory.getEntityMetadata(entityClass);
            var pair = insertStatement(strategy, entity, entityMetadata);
            batch = new PreparedBatch(transaction.getJdbcConnection(), pair.first, strategy, entityMetadata,
                    pair.second, autoGenerateId || entityMetadata.autoGeneratedId);
            statements.put(entityClass, batch);
          }
          batch.addBatchUpdate(entity, maxBatchRecords);
        }

        for (PreparedBatch preparedBatch : statements.values()) {
          preparedBatch.explicitExecuteBatch();
        }
        transaction.commit(false);
      }
      catch (Throwable ex) {
        transaction.rollback(false);
        if (ex instanceof DataAccessException dae) {
          throw dae;
        }
        if (ex instanceof SQLException se) {
          throw translateException("Batch persist entities Running in transaction", null, se);
        }
        throw new PersistenceException("Batch persist entities failed", ex);
      }
    }
  }

  @Override
  public int updateById(Object entity) {
    return updateById(entity, null);
  }

  @Override
  public int updateById(Object entity, Object id) {
    return updateById(entity, id, null);
  }

  @Override
  public int updateById(Object entity, @Nullable PropertyUpdateStrategy strategy) {
    EntityMetadata metadata = entityMetadataFactory.getEntityMetadata(entity.getClass());
    EntityProperty idProperty = metadata.idProperty();

    Object id = idProperty.getValue(entity);
    if (id == null) {
      throw new InvalidDataAccessApiUsageException("Updating an entity, ID property is required");
    }

    if (strategy == null) {
      strategy = defaultUpdateStrategy();
    }

    Update updateStmt = new Update();
    updateStmt.setTableName(metadata.tableName);
    updateStmt.addRestriction(idProperty.columnName);

    ArrayList<EntityProperty> properties = new ArrayList<>();
    boolean emptyUpdateColumns = true;
    for (EntityProperty property : metadata.entityProperties) {
      if (property.property != idProperty.property
              && strategy.shouldUpdate(entity, property)) {
        updateStmt.addAssignment(property.columnName);
        properties.add(property);
        emptyUpdateColumns = false;
      }
    }

    if (emptyUpdateColumns) {
      throw new InvalidDataAccessApiUsageException("Updating an entity, There is no update properties");
    }

    return updateById(entity, id, idProperty, updateStmt, properties);
  }

  @Override
  public int updateById(Object entity, Object id, @Nullable PropertyUpdateStrategy strategy) {
    Assert.notNull(id, "Entity id is required");
    EntityMetadata metadata = entityMetadataFactory.getEntityMetadata(entity.getClass());
    EntityProperty idProperty = metadata.idProperty();
    Assert.isTrue(idProperty.property.isInstance(id), "Entity Id matches failed");

    Update updateStmt = new Update();
    updateStmt.setTableName(metadata.tableName);
    updateStmt.addRestriction(idProperty.columnName);

    if (strategy == null) {
      strategy = defaultUpdateStrategy();
    }

    ArrayList<EntityProperty> properties = new ArrayList<>();
    for (EntityProperty property : metadata.entityProperties) {
      if (strategy.shouldUpdate(entity, property)) {
        updateStmt.addAssignment(property.columnName);
        properties.add(property);
      }
    }

    return updateById(entity, id, idProperty, updateStmt, properties);
  }

  private int updateById(Object entity, Object id, EntityProperty idProperty, Update updateStmt, ArrayList<EntityProperty> properties) {
    String sql = updateStmt.toStatementString(platform);

    if (stmtLogger.isDebugEnabled()) {
      stmtLogger.logStatement(LogMessage.format("Updating entity using ID: '{}'", id), sql);
    }

    Connection con = DataSourceUtils.getConnection(dataSource);
    PreparedStatement statement = null;
    try {
      statement = con.prepareStatement(sql);
      int idx = setParameters(entity, properties, statement);
      // last one is ID
      idProperty.setParameter(statement, idx, id);
      return statement.executeUpdate();
    }
    catch (SQLException ex) {
      throw translateException("Updating entity By ID", sql, ex);
    }
    finally {
      closeResource(con, statement);
    }
  }

  @Override
  public int updateBy(Object entity, String where) {
    return updateBy(entity, where, null);
  }

  @Override
  public int updateBy(Object entity, String where, @Nullable PropertyUpdateStrategy strategy) {
    EntityMetadata metadata = entityMetadataFactory.getEntityMetadata(entity.getClass());

    Update updateStmt = new Update();
    updateStmt.setTableName(metadata.tableName);

    if (strategy == null) {
      strategy = defaultUpdateStrategy();
    }

    EntityProperty updateBy = null;
    ArrayList<EntityProperty> properties = new ArrayList<>();
    for (EntityProperty property : metadata.entityProperties) {
      // columnName or property name
      if (Objects.equals(where, property.columnName)
              || Objects.equals(where, property.property.getName())) {
        updateBy = property;
      }
      else if (strategy.shouldUpdate(entity, property)) {
        updateStmt.addAssignment(property.columnName);
        properties.add(property);
      }
    }

    if (updateBy == null) {
      throw new InvalidDataAccessApiUsageException("Updating an entity, 'where' property '%s' not found".formatted(where));
    }

    updateStmt.addRestriction(updateBy.columnName);

    Object updateByValue = updateBy.getValue(entity);
    if (updateByValue == null) {
      throw new InvalidDataAccessApiUsageException(
              "Updating an entity, 'where' property value '%s' is required".formatted(where));
    }

    String sql = updateStmt.toStatementString(platform);
    if (stmtLogger.isDebugEnabled()) {
      stmtLogger.logStatement(LogMessage.format("Updating entity using {} : '{}'", where, updateByValue), sql);
    }

    Connection con = DataSourceUtils.getConnection(dataSource);
    PreparedStatement statement = null;
    try {
      statement = con.prepareStatement(sql);
      int idx = setParameters(entity, properties, statement);
      // last one is where
      updateBy.setParameter(statement, idx, updateByValue);
      return statement.executeUpdate();
    }
    catch (SQLException ex) {
      throw translateException("Updating entity By " + where, sql, ex);
    }
    finally {
      closeResource(con, statement);
    }
  }

  @Override
  public int delete(Class<?> entityClass, Object id) {
    EntityMetadata metadata = entityMetadataFactory.getEntityMetadata(entityClass);

    if (metadata.idProperty == null) {
      throw new InvalidDataAccessApiUsageException("Deleting an entity, Id property not found");
    }

    StringBuilder sql = new StringBuilder();
    sql.append("DELETE FROM ");
    sql.append(metadata.tableName);
    sql.append(" WHERE `");
    sql.append(metadata.idProperty.columnName);
    sql.append("` = ? ");

    if (stmtLogger.isDebugEnabled()) {
      stmtLogger.logStatement(LogMessage.format("Deleting entity using ID: {}", id), sql);
    }

    Connection con = DataSourceUtils.getConnection(dataSource);
    PreparedStatement statement = null;
    try {
      statement = con.prepareStatement(sql.toString());
      metadata.idProperty.setParameter(statement, 1, id);
      return statement.executeUpdate();
    }
    catch (SQLException ex) {
      throw translateException("Deleting entity using ID", sql.toString(), ex);
    }
    finally {
      closeResource(con, statement);
    }
  }

  @Override
  public int delete(Object entityOrExample) {
    EntityMetadata metadata = entityMetadataFactory.getEntityMetadata(entityOrExample.getClass());

    Object id = null;
    if (metadata.idProperty != null) {
      id = metadata.idProperty.getValue(entityOrExample);
    }

    ExampleQuery exampleQuery = null;

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
      exampleQuery = new ExampleQuery(entityOrExample, metadata);
      exampleQuery.renderWhereClause(sql);
    }

    if (stmtLogger.isDebugEnabled()) {
      stmtLogger.logStatement(LogMessage.format("Deleting entity: [{}]", entityOrExample), sql);
    }

    Connection con = DataSourceUtils.getConnection(dataSource);
    PreparedStatement statement = null;
    try {
      statement = con.prepareStatement(sql.toString());
      if (id != null) {
        metadata.idProperty.setParameter(statement, 1, id);
      }
      else {
        exampleQuery.setParameter(metadata, statement);
      }

      return statement.executeUpdate();
    }
    catch (SQLException ex) {
      throw translateException("Deleting entity", sql.toString(), ex);
    }
    finally {
      closeResource(con, statement);
    }
  }

  // -----------------------------------------------------------------------------------------------
  // Query methods
  // -----------------------------------------------------------------------------------------------

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
    return iterate(entityClass, new FindByIdQuery(id)).first();
  }

  @Nullable
  @Override
  @SuppressWarnings("unchecked")
  public <T> T findFirst(T entity) throws DataAccessException {
    return findFirst((Class<T>) entity.getClass(), entity);
  }

  @Nullable
  @Override
  public <T> T findFirst(Class<T> entityClass, Object example) throws DataAccessException {
    return iterate(entityClass, example).first();
  }

  @Override
  public <T> T findFirst(Class<T> entityClass, @Nullable QueryStatement handler) throws DataAccessException {
    return iterate(entityClass, handler).first();
  }

  @Nullable
  @Override
  @SuppressWarnings("unchecked")
  public <T> T findUnique(T example) throws DataAccessException {
    return iterate((Class<T>) example.getClass(), example).unique();
  }

  @Nullable
  @Override
  public <T> T findUnique(Class<T> entityClass, Object example) throws DataAccessException {
    return iterate(entityClass, example).unique();
  }

  @Override
  public <T> T findUnique(Class<T> entityClass, @Nullable QueryStatement handler) throws DataAccessException {
    return iterate(entityClass, handler).unique();
  }

  @Override
  public <T> List<T> find(Class<T> entityClass) throws DataAccessException {
    return find(entityClass, (QueryStatement) null);
  }

  @Override
  public <T> List<T> find(Class<T> entityClass, Map<String, Order> sortKeys) throws DataAccessException {
    Assert.notEmpty(sortKeys, "sortKeys is required");
    return find(entityClass, new NoConditionsOrderByQuery(OrderByClause.forMap(sortKeys)));
  }

  @Override
  public <T> List<T> find(Class<T> entityClass, Pair<String, Order> sortKey) throws DataAccessException {
    Assert.notNull(sortKey, "sortKey is required");
    return find(entityClass, new NoConditionsOrderByQuery(OrderByClause.mutable().orderBy(sortKey)));
  }

  @SafeVarargs
  @Override
  public final <T> List<T> find(Class<T> entityClass, Pair<String, Order>... sortKeys) throws DataAccessException {
    return find(entityClass, new NoConditionsOrderByQuery(OrderByClause.valueOf(sortKeys)));
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> List<T> find(T example) throws DataAccessException {
    return iterate((Class<T>) example.getClass(), example).list();
  }

  @Override
  public <T> List<T> find(Class<T> entityClass, Object example) throws DataAccessException {
    return iterate(entityClass, example).list();
  }

  @Override
  public <T> List<T> find(Class<T> entityClass, @Nullable QueryStatement handler) throws DataAccessException {
    return iterate(entityClass, handler).list();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <K, T> Map<K, T> find(T example, String mapKey) throws DataAccessException {
    return find((Class<T>) example.getClass(), example, mapKey);
  }

  @Override
  public <K, T> Map<K, T> find(Class<T> entityClass, Object example, String mapKey) throws DataAccessException {
    return iterate(entityClass, example).toMap(mapKey);
  }

  @Override
  public <K, T> Map<K, T> find(Class<T> entityClass, @Nullable QueryStatement handler, String mapKey) throws DataAccessException {
    return iterate(entityClass, handler).toMap(mapKey);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <K, T> Map<K, T> find(T example, Function<T, K> keyMapper) throws DataAccessException {
    return find((Class<T>) example.getClass(), example, keyMapper);
  }

  @Override
  public <K, T> Map<K, T> find(Class<T> entityClass, Object example, Function<T, K> keyMapper) throws DataAccessException {
    return iterate(entityClass, example).toMap(keyMapper);
  }

  @Override
  public <K, T> Map<K, T> find(Class<T> entityClass, @Nullable QueryStatement handler, Function<T, K> keyMapper) throws DataAccessException {
    return iterate(entityClass, handler).toMap(keyMapper);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Number count(T example) throws DataAccessException {
    return count((Class<T>) example.getClass(), example);
  }

  @Override
  public <T> Number count(Class<T> entityClass) throws DataAccessException {
    return count(entityClass, null);
  }

  @Override
  public <T> Number count(Class<T> entityClass, Object example) throws DataAccessException {
    return count(entityClass, handlerFactories.createCondition(example));
  }

  @Override
  public <T> Page<T> page(T example) throws DataAccessException {
    return page(example, Pageable.unwrap(example));
  }

  @Override
  public <T> Page<T> page(Class<T> entityClass, @Nullable Pageable pageable) throws DataAccessException {
    return page(entityClass, null, pageable);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Page<T> page(T example, @Nullable Pageable pageable) throws DataAccessException {
    return page((Class<T>) example.getClass(), example, pageable);
  }

  @Override
  public <T> Page<T> page(Class<T> entityClass, Object example) throws DataAccessException {
    return page(entityClass, example, Pageable.unwrap(example));
  }

  @Override
  public <T> Page<T> page(Class<T> entityClass, Object example, @Nullable Pageable pageable) throws DataAccessException {
    return page(entityClass, handlerFactories.createCondition(example), pageable);
  }

  @Override
  public <T> Page<T> page(Class<T> entityClass, @Nullable ConditionStatement handler) throws DataAccessException {
    return page(entityClass, handler, Pageable.unwrap(handler));
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> void iterate(T example, Consumer<T> entityConsumer) throws DataAccessException {
    iterate((Class<T>) example.getClass(), example, entityConsumer);
  }

  @Override
  public <T> void iterate(Class<T> entityClass, Object example, Consumer<T> entityConsumer) throws DataAccessException {
    iterate(entityClass, example).consume(entityConsumer);
  }

  @Override
  public <T> void iterate(Class<T> entityClass, @Nullable QueryStatement handler, Consumer<T> entityConsumer) throws DataAccessException {
    iterate(entityClass, handler).consume(entityConsumer);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> EntityIterator<T> iterate(T example) throws DataAccessException {
    return iterate((Class<T>) example.getClass(), example);
  }

  @Override
  public <T> EntityIterator<T> iterate(Class<T> entityClass, Object example) throws DataAccessException {
    return iterate(entityClass, handlerFactories.createQuery(example));
  }

  @Override
  public <T> EntityIterator<T> iterate(Class<T> entityClass, @Nullable QueryStatement handler) throws DataAccessException {
    if (handler == null) {
      handler = NoConditionsQuery.instance;
    }

    EntityMetadata metadata = entityMetadataFactory.getEntityMetadata(entityClass);
    String statement = handler.render(metadata).toStatementString(platform);

    Connection con = DataSourceUtils.getConnection(dataSource);
    try {
      PreparedStatement stmt = con.prepareStatement(statement);
      handler.setParameter(metadata, stmt);

      if (stmtLogger.isDebugEnabled()) {
        stmtLogger.logStatement(handler.getDebugLogMessage(), statement);
      }

      return new DefaultEntityIterator<>(con, stmt, entityClass, metadata);
    }
    catch (SQLException ex) {
      DataSourceUtils.releaseConnection(con, dataSource);
      throw translateException(handler.getDescription(), statement, ex);
    }
  }

  @Override
  public <T> Number count(Class<T> entityClass, @Nullable ConditionStatement handler) throws DataAccessException {
    if (handler == null) {
      handler = NoConditionsQuery.instance;
    }
    EntityMetadata metadata = entityMetadataFactory.getEntityMetadata(entityClass);

    ArrayList<Restriction> restrictions = new ArrayList<>();
    handler.renderWhereClause(metadata, restrictions);

    Connection con = DataSourceUtils.getConnection(dataSource);
    try {
      return doQueryCount(metadata, handler, restrictions, con);
    }
    finally {
      DataSourceUtils.releaseConnection(con, dataSource);
    }
  }

  @Override
  public <T> Page<T> page(Class<T> entityClass, @Nullable ConditionStatement handler, @Nullable Pageable pageable) throws DataAccessException {
    if (handler == null) {
      handler = NoConditionsQuery.instance;
    }

    if (pageable == null) {
      pageable = defaultPageable();
    }

    ArrayList<Restriction> restrictions = new ArrayList<>();
    EntityMetadata metadata = entityMetadataFactory.getEntityMetadata(entityClass);
    handler.renderWhereClause(metadata, restrictions);

    Connection con = DataSourceUtils.getConnection(dataSource);
    String statement = null;
    PreparedStatement stmt = null;
    try {
      Number count = doQueryCount(metadata, handler, restrictions, con);
      if (count.intValue() < 1) {
        // no record
        DataSourceUtils.releaseConnection(con, dataSource);
        return new Page<>(pageable, 0, Collections.emptyList());
      }

      statement = new SimpleSelect(Arrays.asList(metadata.columnNames), restrictions)
              .setTableName(metadata.tableName)
              .pageable(pageable)
              .orderBy(handler.getOrderByClause(metadata))
              .toStatementString(platform);

      stmt = con.prepareStatement(statement);
      handler.setParameter(metadata, stmt);

      if (stmtLogger.isDebugEnabled()) {
        stmtLogger.logStatement(handler.getDebugLogMessage(), statement);
      }

      return new Page<>(pageable, count,
              new DefaultEntityIterator<T>(con, stmt, entityClass, metadata).list(pageable.pageSize()));
    }
    catch (Throwable ex) {
      closeResource(con, stmt);
      if (ex instanceof SQLException) {
        throw translateException(handler.getDescription(), statement, (SQLException) ex);
      }
      throw new DataRetrievalFailureException("Unable to retrieve the pageable data ", ex);
    }
  }

  private Number doQueryCount(EntityMetadata metadata, ConditionStatement handler, ArrayList<Restriction> restrictions, Connection con) throws DataAccessException {
    StringBuilder countSql = new StringBuilder(restrictions.size() * 10 + 25 + metadata.tableName.length());
    countSql.append("SELECT COUNT(*) FROM `")
            .append(metadata.tableName)
            .append('`');

    Restriction.render(restrictions, countSql);

    String statement = countSql.toString();
    ResultSet resultSet = null;
    PreparedStatement stmt = null;
    try {
      stmt = con.prepareStatement(statement);
      handler.setParameter(metadata, stmt);

      if (stmtLogger.isDebugEnabled()) {
        stmtLogger.logStatement(handler.getDebugLogMessage(), statement);
      }
      resultSet = stmt.executeQuery();
      if (resultSet.next()) {
        return resultSet.getLong(1);
      }
      return 0;
    }
    catch (SQLException ex) {
      DataSourceUtils.releaseConnection(con, dataSource);
      throw translateException(handler.getDescription(), statement, ex);
    }
    finally {
      closeResource(null, stmt, resultSet);
    }
  }

  /**
   * default Pageable
   */
  protected Pageable defaultPageable() {
    return defaultPageable;
  }

  /**
   * default PropertyUpdateStrategy
   */
  protected PropertyUpdateStrategy defaultUpdateStrategy() {
    return defaultUpdateStrategy;
  }

  protected PreparedStatement prepareStatement(Connection connection, String sql, boolean autoGenerateId) throws SQLException {
    if (autoGenerateId) {
      return connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
    }
    return connection.prepareStatement(sql);
  }

  private DataAccessException translateException(String task, @Nullable String sql, SQLException ex) {
    return repositoryManager.translateException(task, sql, ex);
  }

  private Pair<String, ArrayList<EntityProperty>> insertStatement(PropertyUpdateStrategy strategy, Object entity, EntityMetadata entityMetadata) {
    Insert insert = new Insert(entityMetadata.tableName);
    var properties = new ArrayList<EntityProperty>(entityMetadata.entityProperties.length);
    for (EntityProperty property : entityMetadata.entityProperties) {
      if (strategy.shouldUpdate(entity, property)) {
        insert.addColumn(property.columnName);
        properties.add(property);
      }
    }

    return Pair.of(insert.toStatementString(platform), properties);
  }

  private void closeResource(@Nullable Connection connection, @Nullable PreparedStatement statement) {
    try {
      DataSourceUtils.doReleaseConnection(connection, dataSource);
    }
    catch (SQLException e) {
      if (repositoryManager.isCatchResourceCloseErrors()) {
        throw translateException("Closing Connection", null, e);
      }
      else {
        logger.debug("Could not close JDBC Connection", e);
      }
    }

    if (statement != null) {
      try {
        statement.close();
      }
      catch (SQLException e) {
        if (repositoryManager.isCatchResourceCloseErrors()) {
          throw translateException("Closing Statement", null, e);
        }
        else {
          logger.debug("Could not close JDBC Statement", e);
        }
      }
    }
  }

  private void closeResource(@Nullable Connection connection, @Nullable PreparedStatement statement, @Nullable ResultSet resultSet) {
    closeResource(connection, statement);
    if (resultSet != null) {
      try {
        resultSet.close();
      }
      catch (SQLException e) {
        if (repositoryManager.isCatchResourceCloseErrors()) {
          throw translateException("Closing ResultSet", null, e);
        }
        else {
          logger.debug("Could not close JDBC ResultSet", e);
        }
      }
    }
  }

  //

  private static int setParameters(Object entity, ArrayList<EntityProperty> properties, PreparedStatement statement) throws SQLException {
    int idx = 1;
    for (EntityProperty property : properties) {
      property.setTo(statement, idx++, entity);
    }
    return idx;
  }

  private static void assertUpdateCount(String sql, int actualCount, int expectCount) {
    if (actualCount != expectCount) {
      throw new JdbcUpdateAffectedIncorrectNumberOfRowsException(sql, expectCount, actualCount);
    }
  }

  private final class DefaultEntityIterator<T> extends EntityIterator<T> {

    private final Connection connection;

    private final PreparedStatement statement;

    private final ResultSetExtractor<T> handler;

    private DefaultEntityIterator(Connection connection, PreparedStatement statement, Class<?> entityClass, EntityMetadata entityMetadata) throws SQLException {
      super(statement.executeQuery(), entityMetadata);
      this.statement = statement;
      this.connection = connection;
      try {
        var factory = new DefaultResultSetHandlerFactory<T>(new JdbcBeanMetadata(entityClass, repositoryManager.isDefaultCaseSensitive(),
                true, true), repositoryManager, null);
        this.handler = factory.getResultSetHandler(resultSet.getMetaData());
      }
      catch (SQLException e) {
        throw translateException("Get ResultSetHandler", null, e);
      }
    }

    @Override
    protected T readNext(ResultSet resultSet) throws SQLException {
      return handler.extractData(resultSet);
    }

    @Override
    protected RuntimeException handleReadError(SQLException ex) {
      return translateException("Reading Entity", null, ex);
    }

    @Override
    public void close() {
      closeResource(connection, statement, resultSet);
    }

  }

  final class PreparedBatch extends BatchExecution {

    public final PreparedStatement statement;

    public final ArrayList<EntityProperty> properties;

    public int currentBatchRecords = 0;

    PreparedBatch(Connection connection, String sql, PropertyUpdateStrategy strategy,
            EntityMetadata entityMetadata, ArrayList<EntityProperty> properties, boolean autoGenerateId) throws SQLException {
      super(sql, strategy, entityMetadata, autoGenerateId);
      this.properties = properties;
      this.statement = prepareStatement(connection, sql, autoGenerateId);
    }

    public void addBatchUpdate(Object entity, int maxBatchRecords) throws Throwable {
      entities.add(entity);
      PreparedStatement statement = this.statement;
      setParameters(entity, properties, statement);
      statement.addBatch();
      if (maxBatchRecords > 0 && ++currentBatchRecords % maxBatchRecords == 0) {
        executeBatch(statement, true);
      }
    }

    public void explicitExecuteBatch() throws Throwable {
      executeBatch(statement, false);
      closeResource(null, statement);
    }

    private void executeBatch(PreparedStatement statement, boolean implicitExecution) throws Throwable {
      beforeProcessing(implicitExecution);
      if (stmtLogger.isDebugEnabled()) {
        stmtLogger.logStatement(LogMessage.format("Executing batch size: {}", entities.size()), sql);
      }
      Throwable exception = null;
      try {
        int[] updateCounts = statement.executeBatch();
        assertUpdateCount(sql, updateCounts.length, entities.size());

        if (autoGenerateId) {
          EntityProperty idProperty = entityMetadata.idProperty;
          if (idProperty != null) {
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
        }
      }
      catch (Throwable e) {
        exception = e;
        throw e;
      }
      finally {
        afterProcessing(implicitExecution, exception);
        this.currentBatchRecords = 0;
        this.entities.clear();
      }
    }

    private void afterProcessing(boolean implicitExecution, @Nullable Throwable exception) {
      if (CollectionUtils.isNotEmpty(batchPersistListeners)) {
        for (BatchPersistListener listener : batchPersistListeners) {
          listener.afterProcessing(this, implicitExecution, exception);
        }
      }
    }

    private void beforeProcessing(boolean implicitExecution) {
      if (CollectionUtils.isNotEmpty(batchPersistListeners)) {
        for (BatchPersistListener listener : batchPersistListeners) {
          listener.beforeProcessing(this, implicitExecution);
        }
      }
    }

  }

}
