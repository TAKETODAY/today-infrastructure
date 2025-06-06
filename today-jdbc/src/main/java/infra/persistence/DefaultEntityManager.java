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

package infra.persistence;

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

import infra.core.Pair;
import infra.dao.DataAccessException;
import infra.dao.DataRetrievalFailureException;
import infra.dao.InvalidDataAccessApiUsageException;
import infra.jdbc.DefaultResultSetHandlerFactory;
import infra.jdbc.GeneratedKeysException;
import infra.jdbc.JdbcBeanMetadata;
import infra.jdbc.JdbcUpdateAffectedIncorrectNumberOfRowsException;
import infra.jdbc.PersistenceException;
import infra.jdbc.RepositoryManager;
import infra.jdbc.core.ResultSetExtractor;
import infra.jdbc.datasource.DataSourceUtils;
import infra.jdbc.format.SqlStatementLogger;
import infra.lang.Assert;
import infra.lang.Descriptive;
import infra.lang.Nullable;
import infra.logging.LogMessage;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.persistence.platform.Platform;
import infra.persistence.sql.Insert;
import infra.persistence.sql.OrderByClause;
import infra.persistence.sql.Restriction;
import infra.persistence.sql.SimpleSelect;
import infra.persistence.sql.Update;
import infra.transaction.TransactionDefinition;
import infra.util.CollectionUtils;

/**
 * Default implementation of the EntityManager interface, providing a comprehensive
 * set of operations for managing entities in a data store. This class supports
 * persistence, retrieval, updating, and deletion of entities, as well as advanced
 * querying capabilities such as sorting, pagination, and mapping results to custom
 * structures.
 *
 * <p>
 * The class is highly configurable, allowing customization of behavior through
 * various setters for properties like platform, update strategy, batch processing,
 * and transaction configuration. It also supports event listeners for batch
 * persistence operations and integrates with repositories for entity management.
 *
 * <p>
 * Key Features:
 * - Entity persistence with support for auto-generated IDs and customizable update strategies.
 * - Batch processing with configurable limits for batched commands.
 * - Advanced query capabilities, including sorting, pagination, and result mapping.
 * - Support for conditional queries and dynamic query handlers.
 * - Transaction management with configurable transaction definitions.
 * - Event listeners for monitoring batch persistence operations.
 *
 * <p>
 * This class is designed to be flexible and extensible, making it suitable for a
 * wide range of data access scenarios. It abstracts the underlying data store
 * interactions, providing a consistent API for entity management.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/10 22:28
 */
public class DefaultEntityManager implements EntityManager {

  private static final Logger logger = LoggerFactory.getLogger(DefaultEntityManager.class);

  private EntityMetadataFactory entityMetadataFactory = new DefaultEntityMetadataFactory();

  private final DataSource dataSource;

  private final RepositoryManager repositoryManager;

  @SuppressWarnings("rawtypes")
  private final ArrayList<ConditionPropertyExtractor> propertyExtractors = new ArrayList<>();

  @Nullable
  private ArrayList<BatchPersistListener> batchPersistListeners;

  private int maxBatchRecords = 0;

  /**
   * a flag indicating whether auto-generated keys should be returned;
   */
  private boolean autoGenerateId = true;

  private PropertyUpdateStrategy defaultUpdateStrategy = PropertyUpdateStrategy.noneNull();

  private Pageable defaultPageable = Pageable.of(10, 1);

  private Platform platform;

  private SqlStatementLogger stmtLogger = SqlStatementLogger.sharedInstance;

  @Nullable
  private TransactionDefinition transactionConfig = TransactionDefinition.withDefaults();

  private QueryStatementFactories handlerFactories = new QueryStatementFactories(entityMetadataFactory, propertyExtractors);

  public DefaultEntityManager(RepositoryManager repositoryManager) {
    this(repositoryManager, Platform.forClasspath());
  }

  public DefaultEntityManager(RepositoryManager repositoryManager, Platform platform) {
    this.dataSource = repositoryManager.getDataSource();
    this.repositoryManager = repositoryManager;
    setPlatform(platform);
  }

  /**
   * Sets the platform for this instance. If the provided platform is {@code null},
   * a default platform will be determined based on the classpath using
   * {@link Platform#forClasspath()}.
   *
   * <p>This method is useful when you want to explicitly define the platform or
   * rely on the default behavior when no specific platform is provided.</p>
   *
   * @param platform the platform to set, or {@code null} to use the default platform
   */
  public void setPlatform(@Nullable Platform platform) {
    this.platform = platform == null ? Platform.forClasspath() : platform;
  }

  /**
   * Sets the default update strategy for properties. This method ensures that
   * a non-null {@link PropertyUpdateStrategy} is set as the default strategy.
   * If a null value is passed, an exception will be thrown.
   *
   * <p>Example usage:
   * <pre>{@code
   *   PropertyUpdateStrategy strategy = new CustomUpdateStrategy();
   *   propertyManager.setDefaultUpdateStrategy(strategy);
   *
   *   // Now the default strategy is applied to all relevant property updates
   * }</pre>
   *
   * @param defaultUpdateStrategy the strategy to be used as the default for
   * property updates; must not be null
   * @throws IllegalArgumentException if the provided strategy is null
   */
  public void setDefaultUpdateStrategy(PropertyUpdateStrategy defaultUpdateStrategy) {
    Assert.notNull(defaultUpdateStrategy, "defaultUpdateStrategy is required");
    this.defaultUpdateStrategy = defaultUpdateStrategy;
  }

  /**
   * Sets the default {@link Pageable} to be used when no specific pageable
   * configuration is provided. This method ensures that the given
   * {@code defaultPageable} is not null, throwing an exception if it is.
   *
   * <p>Example usage:
   * <pre>{@code
   *   Pageable defaultPageable = Pageable.of(1, 10);
   *   myService.setDefaultPageable(defaultPageable);
   *
   *   // Now, any subsequent operations in myService will use the above
   *   // default pageable unless explicitly overridden.
   * }</pre>
   *
   * @param defaultPageable the {@link Pageable} instance to set as the default;
   * must not be null
   * @throws IllegalArgumentException if {@code defaultPageable} is null
   */
  public void setDefaultPageable(Pageable defaultPageable) {
    Assert.notNull(defaultPageable, "defaultPageable is required");
    this.defaultPageable = defaultPageable;
  }

  /**
   * Sets the {@code EntityMetadataFactory} to be used for creating entity metadata.
   * This method also initializes a new instance of {@code QueryStatementFactories}
   * using the provided {@code EntityMetadataFactory} and existing property extractors.
   *
   * <p>If the provided {@code EntityMetadataFactory} is {@code null}, an
   * {@code IllegalArgumentException} will be thrown.</p>
   *
   * @param entityMetadataFactory the {@code EntityMetadataFactory} to set; must not be null
   */
  public void setEntityMetadataFactory(EntityMetadataFactory entityMetadataFactory) {
    Assert.notNull(entityMetadataFactory, "EntityMetadataFactory is required");
    this.entityMetadataFactory = entityMetadataFactory;
    this.handlerFactories = new QueryStatementFactories(entityMetadataFactory, propertyExtractors);
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

  /**
   * Returns the maximum number of records allowed in a batch.
   *
   * This method retrieves the value of the {@code maxBatchRecords} property,
   * which defines the upper limit of records that can be processed in a single
   * batch operation. This is useful for configuring batch processing limits
   * in applications that handle large datasets.
   *
   * @return the maximum number of records allowed in a batch
   */
  public int getMaxBatchRecords() {
    return this.maxBatchRecords;
  }

  /**
   * Adds one or more batch persist listeners to the internal list of listeners.
   * If no listeners have been registered yet, this method initializes the listener list.
   * The method ensures that all provided listeners are added to the existing collection.
   *
   * <p>Example usage:
   * <pre>{@code
   * BatchPersistListener listener1 = entities -> {
   *   // Handle batch persist event for listener1
   * };
   * BatchPersistListener listener2 = entities -> {
   *   // Handle batch persist event for listener2
   * };
   *
   * someObject.addBatchPersistListeners(listener1, listener2);
   * }</pre>
   *
   * @param listeners a variable number of {@link BatchPersistListener} instances
   * to be added to the listener list. If null or empty,
   * this method has no effect.
   */
  public void addBatchPersistListeners(BatchPersistListener... listeners) {
    if (batchPersistListeners == null) {
      batchPersistListeners = new ArrayList<>();
    }
    CollectionUtils.addAll(batchPersistListeners, listeners);
  }

  /**
   * Adds a collection of batch persist listeners to the current list of listeners.
   * If no listeners are currently registered, this method initializes a new list
   * before adding the provided listeners.
   *
   * <p>Example usage:
   * <pre>{@code
   *   List<BatchPersistListener> listeners = new ArrayList<>();
   *   listeners.add(new MyBatchPersistListener());
   *   listeners.add(new AnotherBatchPersistListener());
   *
   *   manager.addBatchPersistListeners(listeners);
   * }</pre>
   *
   * @param listeners a collection of {@link BatchPersistListener} objects to be added
   * to the internal list of batch persist listeners. Must not be null.
   */
  public void addBatchPersistListeners(Collection<BatchPersistListener> listeners) {
    if (batchPersistListeners == null) {
      batchPersistListeners = new ArrayList<>();
    }
    batchPersistListeners.addAll(listeners);
  }

  /**
   * Sets the collection of batch persist listeners for this object.
   * If the provided collection is null, any existing listeners will be cleared.
   * Otherwise, the current listener list will be replaced with the contents
   * of the provided collection.
   *
   * <p>Example usage:
   * <pre>{@code
   *   List<BatchPersistListener> listeners = new ArrayList<>();
   *   listeners.add(new MyBatchPersistListener());
   *
   *   myObject.setBatchPersistListeners(listeners);
   * }</pre>
   *
   * <p>This method ensures that the internal listener list is properly
   * initialized or cleared before adding new listeners, preventing potential
   * memory leaks or unintended behavior.
   *
   * @param listeners a collection of {@link BatchPersistListener} objects to set,
   * or null to clear all existing listeners
   */
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

  /**
   * Sets the SQL statement logger for this component.
   * This method ensures that the provided logger is not null,
   * throwing an exception if the requirement is not met.
   *
   * <p>Example usage:
   * <pre>{@code
   * SqlStatementLogger logger = new SqlStatementLogger(...);
   * component.setStatementLogger(logger);
   * }</pre>
   *
   * @param stmtLogger the SQL statement logger to be set
   */
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

  /**
   * Adds a custom {@link ConditionPropertyExtractor} to the internal list of property extractors.
   * This method is used to register an extractor that can be utilized for extracting condition
   * properties during processing. The provided extractor must not be null.
   *
   * <p>Example usage:
   * <pre>{@code
   * ConditionPropertyExtractor<MyCondition> extractor = condition -> {
   *   // Implement logic to extract properties from the condition
   *   return Collections.singletonMap("key", "value");
   * };
   *
   * processor.addConditionPropertyExtractor(extractor);
   * }</pre>
   *
   * @param extractor the {@link ConditionPropertyExtractor} to be added; must not be null
   * @throws IllegalArgumentException if the provided extractor is null
   */
  @SuppressWarnings("rawtypes")
  public void addConditionPropertyExtractor(ConditionPropertyExtractor extractor) {
    Assert.notNull(extractor, "ConditionPropertyExtractor is required");
    this.propertyExtractors.add(extractor);
  }

  /**
   * Sets the list of condition property extractors to be used for extracting
   * properties from conditions. If the provided list is {@code null}, the current
   * list of extractors will be cleared.
   *
   * <p>This method is useful when you want to customize or replace the existing
   * set of property extractors with a new set. For example, you can define your
   * own extractors to handle specific types of conditions.</p>
   *
   * <p>Example usage:</p>
   *
   * <pre>{@code
   *   List<ConditionPropertyExtractor> customExtractors = Arrays.asList(
   *     new CustomExtractor1(),
   *     new CustomExtractor2()
   *   );
   *
   *   processor.setConditionPropertyExtractors(customExtractors);
   * }</pre>
   *
   * @param extractors the list of {@link ConditionPropertyExtractor} instances to set,
   * or {@code null} to clear the current list
   */
  @SuppressWarnings("rawtypes")
  public void setConditionPropertyExtractors(@Nullable List<ConditionPropertyExtractor> extractors) {
    propertyExtractors.clear();
    if (extractors != null) {
      this.propertyExtractors.addAll(extractors);
    }
  }

  // ---------------------------------------------------------------------
  // Implementation of EntityManager
  // ---------------------------------------------------------------------

  @Override
  public int persist(Object entity) throws DataAccessException {
    return persist(entity, defaultUpdateStrategy(entity), autoGenerateId);
  }

  @Override
  public int persist(Object entity, boolean autoGenerateId) throws DataAccessException {
    return persist(entity, defaultUpdateStrategy(entity), autoGenerateId);
  }

  /**
   * Persists the given entity to the data store using the specified property update strategy.
   * If the strategy is not provided, a default strategy may be used. The method delegates
   * the persistence operation to an overloaded method, passing the entity, strategy, and
   * auto-generation flag for the identifier.
   *
   * @param entity the entity to be persisted; must not be null
   * @param strategy the strategy to apply for updating properties during persistence;
   * can be null if no specific strategy is required
   * @return the number of records affected by the persistence operation
   * @throws DataAccessException if an error occurs while accessing the data store
   */
  @Override
  public int persist(Object entity, @Nullable PropertyUpdateStrategy strategy) throws DataAccessException {
    return persist(entity, strategy, autoGenerateId);
  }

  @Override
  public int persist(Object entity, @Nullable PropertyUpdateStrategy strategy, boolean autoGenerateId) throws DataAccessException {
    EntityMetadata entityMetadata = entityMetadataFactory.getEntityMetadata(entity.getClass());
    if (strategy == null) {
      strategy = defaultUpdateStrategy(entity);
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
    persist(entities, null, autoGenerateId);
  }

  @Override
  public void persist(Iterable<?> entities, boolean autoGenerateId) throws DataAccessException {
    persist(entities, null, autoGenerateId);
  }

  @Override
  public void persist(Iterable<?> entities, @Nullable PropertyUpdateStrategy strategy) throws DataAccessException {
    persist(entities, strategy, autoGenerateId);
  }

  @Override
  public void persist(Iterable<?> entities, @Nullable PropertyUpdateStrategy strategy, boolean autoGenerateId)
          throws DataAccessException //
  {
    try (var transaction = repositoryManager.beginTransaction(transactionConfig)) {
      int maxBatchRecords = getMaxBatchRecords();
      var statements = new HashMap<Class<?>, PreparedBatch>(8);
      try {
        for (Object entity : entities) {
          Class<?> entityClass = entity.getClass();
          PreparedBatch batch = statements.get(entityClass);
          if (batch == null) {
            EntityMetadata entityMetadata = entityMetadataFactory.getEntityMetadata(entityClass);
            PropertyUpdateStrategy strategyToUse = strategy;
            if (strategyToUse == null) {
              strategyToUse = defaultUpdateStrategy(entity);
            }
            var pair = insertStatement(strategyToUse, entity, entityMetadata);
            batch = new PreparedBatch(transaction.getJdbcConnection(), pair.first, strategyToUse, entityMetadata,
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
  public int update(Object entity) throws DataAccessException {
    return update(entity, null);
  }

  @Override
  public int update(Object entity, @Nullable PropertyUpdateStrategy strategy) throws DataAccessException {
    if (strategy == null) {
      strategy = defaultUpdateStrategy(entity);
    }
    EntityMetadata metadata = entityMetadataFactory.getEntityMetadata(entity.getClass());
    EntityProperty idProperty = metadata.idProperty;
    if (idProperty != null) {
      Object id = idProperty.getValue(entity);
      if (id != null) {
        return doUpdateById(entity, id, idProperty, metadata, updateExcludeId(strategy));
      }
    }

    Update updateStmt = new Update(metadata.tableName);

    ArrayList<EntityProperty> properties = new ArrayList<>(4);
    ArrayList<EntityProperty> updateByProperties = new ArrayList<>(2);
    for (EntityProperty property : metadata.entityPropertiesExcludeId) {
      if (property.isPresent(UpdateBy.class)) {
        updateByProperties.add(property);
        updateStmt.addRestriction(property.columnName);
      }
      else if (strategy.shouldUpdate(entity, property)) {
        properties.add(property);
        updateStmt.addAssignment(property.columnName);
      }
    }

    if (properties.isEmpty()) {
      throw new InvalidDataAccessApiUsageException("Updating an entity, There is no update properties");
    }

    if (updateByProperties.isEmpty()) {
      throw new InvalidDataAccessApiUsageException("Updating an entity, There is no update by properties");
    }

    String sql = updateStmt.toStatementString(platform);

    if (stmtLogger.isDebugEnabled()) {
      stmtLogger.logStatement(LogMessage.format("Updating entity using: '{}'", updateByProperties), sql);
    }

    Connection con = DataSourceUtils.getConnection(dataSource);
    PreparedStatement statement = null;
    try {
      statement = con.prepareStatement(sql);
      int idx = setParameters(entity, properties, statement);
      // apply where parameters
      for (EntityProperty updateBy : updateByProperties) {
        updateBy.setTo(statement, idx++, entity);
      }
      return statement.executeUpdate();
    }
    catch (SQLException ex) {
      throw translateException("Updating entity", sql, ex);
    }
    finally {
      closeResource(con, statement);
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
      throw new InvalidDataAccessApiUsageException("Updating an entity, ID value is required");
    }

    if (strategy == null) {
      strategy = defaultUpdateStrategy(entity);
    }

    return doUpdateById(entity, id, idProperty, metadata, updateExcludeId(strategy));
  }

  /**
   * returns a new chain which exclude ID
   *
   * @return returns a new Strategy
   */
  private static PropertyUpdateStrategy updateExcludeId(PropertyUpdateStrategy strategy) {
    return (entity, property) -> !property.isIdProperty && strategy.shouldUpdate(entity, property);
  }

  @Override
  public int updateById(Object entity, Object id, @Nullable PropertyUpdateStrategy strategy) {
    Assert.notNull(id, "Entity id is required");
    EntityMetadata metadata = entityMetadataFactory.getEntityMetadata(entity.getClass());
    EntityProperty idProperty = idProperty(metadata, "Updating an entity, Id property not found");
    Assert.isTrue(idProperty.property.isInstance(id), "Entity Id matches failed");

    if (strategy == null) {
      strategy = defaultUpdateStrategy(entity);
    }

    return doUpdateById(entity, id, idProperty, metadata, strategy);
  }

  private int doUpdateById(Object entity, Object id, EntityProperty idProperty, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
    Update updateStmt = new Update(metadata.tableName);
    updateStmt.addRestriction(idProperty.columnName);

    ArrayList<EntityProperty> properties = new ArrayList<>();
    for (EntityProperty property : metadata.entityProperties) {
      if (strategy.shouldUpdate(entity, property)) {
        updateStmt.addAssignment(property.columnName);
        properties.add(property);
      }
    }

    if (properties.isEmpty()) {
      throw new InvalidDataAccessApiUsageException("Updating an entity, There is no update properties");
    }

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

    Update updateStmt = new Update(metadata.tableName);

    if (strategy == null) {
      strategy = defaultUpdateStrategy(entity);
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
  public int saveOrUpdate(Object entity) throws DataAccessException {
    return saveOrUpdate(entity, null);
  }

  @Override
  public int saveOrUpdate(Object entity, @Nullable PropertyUpdateStrategy strategy) throws DataAccessException {
    if (isNew(entity)) {
      return persist(entity, strategy);
    }
    return update(entity, strategy);
  }

  @Override
  public int delete(Class<?> entityClass, Object id) throws DataAccessException {
    EntityMetadata metadata = entityMetadataFactory.getEntityMetadata(entityClass);

    EntityProperty idProperty = idProperty(metadata, "Deleting an entity, Id property not found");

    StringBuilder sql = new StringBuilder();
    sql.append("DELETE FROM ");
    sql.append(metadata.tableName);
    sql.append(" WHERE `");
    sql.append(idProperty.columnName);
    sql.append("` = ? ");

    if (stmtLogger.isDebugEnabled()) {
      stmtLogger.logStatement(LogMessage.format("Deleting entity using ID: {}", id), sql);
    }

    Connection con = DataSourceUtils.getConnection(dataSource);
    PreparedStatement statement = null;
    try {
      statement = con.prepareStatement(sql.toString());
      idProperty.setParameter(statement, 1, id);
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
  public int delete(Object entityOrExample) throws DataAccessException {
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
      exampleQuery = new ExampleQuery(entityOrExample, metadata, propertyExtractors);
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

  @Override
  public void truncate(Class<?> entityClass) throws DataAccessException {
    EntityMetadata metadata = entityMetadataFactory.getEntityMetadata(entityClass);

    String sql = platform.getTruncateTableStatement(metadata.tableName);
    if (stmtLogger.isDebugEnabled()) {
      stmtLogger.logStatement(LogMessage.format("Truncate table: [{}]", entityClass), sql);
    }

    Statement statement = null;
    Connection con = DataSourceUtils.getConnection(dataSource);
    try {
      statement = con.createStatement();
      statement.executeUpdate(sql);
    }
    catch (SQLException ex) {
      throw translateException("Truncate table", sql, ex);
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
  public <K, T> Map<K, T> find(Class<T> entityClass, Function<T, K> keyMapper) throws DataAccessException {
    return find(entityClass, null, keyMapper);
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
  public <T> EntityIterator<T> iterate(Class<T> entityClass) throws DataAccessException {
    return iterate(entityClass, (QueryStatement) null);
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
        stmtLogger.logStatement(getDebugLogMessage(handler), statement);
      }

      return new DefaultEntityIterator<>(con, stmt, entityClass, metadata);
    }
    catch (SQLException ex) {
      DataSourceUtils.releaseConnection(con, dataSource);
      throw translateException(getDescription(handler), statement, ex);
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
        stmtLogger.logStatement(getDebugLogMessage(handler), statement);
      }

      return new Page<>(pageable, count,
              new DefaultEntityIterator<T>(con, stmt, entityClass, metadata).list(pageable.pageSize()));
    }
    catch (Throwable ex) {
      closeResource(con, stmt);
      if (ex instanceof DataAccessException dae) {
        throw dae;
      }
      if (ex instanceof SQLException) {
        throw translateException(getDescription(handler), statement, (SQLException) ex);
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
        stmtLogger.logStatement(getDebugLogMessage(handler), statement);
      }
      resultSet = stmt.executeQuery();
      if (resultSet.next()) {
        return resultSet.getLong(1);
      }
      return 0;
    }
    catch (SQLException ex) {
      DataSourceUtils.releaseConnection(con, dataSource);
      throw translateException(getDescription(handler), statement, ex);
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
   * get default PropertyUpdateStrategy
   */
  protected PropertyUpdateStrategy defaultUpdateStrategy(Object entity) {
    if (entity instanceof UpdateStrategySource source) {
      return source.updateStrategy();
    }
    if (entity instanceof PropertyUpdateStrategy strategy) {
      return strategy;
    }
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

  private void closeResource(@Nullable Connection connection, @Nullable Statement stmt) {
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

    if (stmt != null) {
      try {
        stmt.close();
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

  private String getDescription(Object handler) {
    Descriptive descriptive = null;
    if (handler instanceof Descriptive) {
      descriptive = (Descriptive) handler;
    }
    if (descriptive == null) {
      descriptive = NoConditionsQuery.instance;
    }
    return descriptive.getDescription();
  }

  private Object getDebugLogMessage(Object handler) {
    if (handler instanceof DebugDescriptive descriptive) {
      return descriptive.getDebugLogMessage();
    }
    else if (handler instanceof Descriptive) {
      return LogMessage.format(((Descriptive) handler).getDescription());
    }
    return NoConditionsQuery.instance.getDebugLogMessage();
  }

  private boolean isNew(Object entity) throws IllegalEntityException {
    if (entity instanceof NewEntityIndicator e) {
      return e.isNew();
    }
    EntityProperty idProperty = entityMetadataFactory.getEntityMetadata(entity.getClass()).idProperty;
    if (idProperty != null) {
      return idProperty.getValue(entity) == null;
    }
    return false;
  }

  private static EntityProperty idProperty(EntityMetadata metadata, String error) {
    EntityProperty idProperty = metadata.idProperty;
    if (idProperty == null) {
      idProperty = metadata.refIdProperty;
      if (idProperty == null) {
        throw new InvalidDataAccessApiUsageException(error);
      }
    }
    return idProperty;
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
