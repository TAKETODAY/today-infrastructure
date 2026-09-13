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

package infra.persistence;

import org.jspecify.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
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
import infra.dao.OptimisticLockingFailureException;
import infra.jdbc.DefaultResultSetHandlerFactory;
import infra.jdbc.GeneratedKeysException;
import infra.jdbc.JdbcBeanMetadata;
import infra.jdbc.JdbcUpdateAffectedIncorrectNumberOfRowsException;
import infra.jdbc.PersistenceException;
import infra.jdbc.RepositoryManager;
import infra.jdbc.core.ResultSetExtractor;
import infra.jdbc.datasource.DataSourceUtils;
import infra.jdbc.format.LoggingPreparedStatement;
import infra.jdbc.format.SqlStatementLogger;
import infra.lang.Descriptive;
import infra.logging.LogMessage;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.persistence.annotation.UpdateBy;
import infra.persistence.annotation.Version;
import infra.persistence.event.BatchExecution;
import infra.persistence.event.DefaultEntityEventRegistry;
import infra.persistence.event.EntityEventRegistry;
import infra.persistence.platform.Platform;
import infra.persistence.sql.Insert;
import infra.persistence.sql.OrderByClause;
import infra.persistence.sql.Restriction;
import infra.persistence.sql.SimpleSelect;
import infra.persistence.sql.Update;
import infra.persistence.support.DefaultVersionIncrementStrategy;
import infra.transaction.TransactionDefinition;
import infra.util.Assert;

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
 * - {@linkplain infra.persistence.event.EntityEventListener Entity lifecycle events}
 * for reacting to persist, update and delete operations of specific entity classes.
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

  private final DataSource dataSource;

  private final RepositoryManager repositoryManager;

  private int maxBatchRecords = 0;

  /**
   * a flag indicating whether auto-generated keys should be returned;
   */
  private boolean autoGenerateId = true;

  private Platform platform;

  private EntityMetadataFactory entityMetadataFactory = new DefaultEntityMetadataFactory();

  private PropertyUpdateStrategy defaultUpdateStrategy = PropertyUpdateStrategy.noneNull();

  private VersionIncrementStrategy versionIncrementStrategy = new DefaultVersionIncrementStrategy();

  private Pageable defaultPageable = Pageable.of(10, 1);

  private SqlStatementLogger stmtLogger = SqlStatementLogger.sharedInstance;

  private EntityEventRegistry entityEventRegistry = new DefaultEntityEventRegistry();

  private EntityEventMulticaster eventMulticaster = new EntityEventMulticaster(entityEventRegistry);

  private @Nullable TransactionDefinition transactionConfig = TransactionDefinition.withDefaults();

  private QueryStatementFactories statementFactories = new QueryStatementFactories(entityMetadataFactory);

  public DefaultEntityManager(RepositoryManager repositoryManager) {
    this(repositoryManager, Platform.generic());
  }

  public DefaultEntityManager(RepositoryManager repositoryManager, @Nullable Platform platform) {
    this.dataSource = repositoryManager.getDataSource();
    this.repositoryManager = repositoryManager;
    setPlatform(platform);
  }

  /**
   * Sets the platform for this instance. If the provided platform is {@code null},
   * a default platform will be determined based on the classpath using
   * {@link Platform#generic()}.
   *
   * <p>This method is useful when you want to explicitly define the platform or
   * rely on the default behavior when no specific platform is provided.</p>
   *
   * @param platform the platform to set, or {@code null} to use the default platform
   */
  public void setPlatform(@Nullable Platform platform) {
    this.platform = platform == null ? Platform.generic() : platform;
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
   * Sets the {@link VersionIncrementStrategy} used to compute the next version value
   * for entities with a {@link Version} annotated property during update operations.
   * When set to {@code null}, the {@link DefaultVersionIncrementStrategy default}
   * strategy is used.
   *
   * <pre>{@code
   * // Compose custom strategy with the default as fallback
   * entityManager.setVersionIncrementStrategy(
   *     myCustomStrategy.and(new DefaultVersionIncrementStrategy()));
   * }</pre>
   *
   * @param versionIncrementStrategy the strategy, or {@code null} to use the default
   * @see VersionIncrementStrategy
   * @see DefaultVersionIncrementStrategy
   */
  public void setVersionIncrementStrategy(@Nullable VersionIncrementStrategy versionIncrementStrategy) {
    this.versionIncrementStrategy = versionIncrementStrategy == null
            ? new DefaultVersionIncrementStrategy()
            : versionIncrementStrategy;
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
    this.statementFactories.setEntityMetadataFactory(entityMetadataFactory);
  }

  /**
   * Return the {@link QueryStatementFactories} managing the {@link QueryStatementFactory}
   * instances used to turn an example object into a {@link QueryStatement} or
   * {@link QueryCondition}.
   *
   * <p>Use it to register custom factories:
   * <pre>{@code
   * entityManager.getQueryStatementFactories().addFactory(myFactory);
   * }</pre>
   *
   * @since 5.0
   */
  public QueryStatementFactories getQueryStatementFactories() {
    return statementFactories;
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
   * <p>
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
   * Return the {@link EntityEventRegistry} used to register
   * {@link infra.persistence.event.EntityEventListener entity lifecycle listeners}
   * and {@link infra.persistence.event.BatchPersistListener batch persist
   * listeners}. Listener mutations are picked up by the dispatcher immediately.
   *
   * @since 5.0
   */
  public EntityEventRegistry getEntityEventRegistry() {
    return entityEventRegistry;
  }

  /**
   * Set the {@link EntityEventRegistry} to use. When {@code null}, the default
   * {@link DefaultEntityEventRegistry} is restored.
   *
   * @param entityEventRegistry the registry to use, or {@code null} to use the default
   * @since 5.0
   */
  public void setEntityEventRegistry(@Nullable EntityEventRegistry entityEventRegistry) {
    if (entityEventRegistry == null) {
      this.entityEventRegistry = new DefaultEntityEventRegistry();
    }
    else {
      this.entityEventRegistry = entityEventRegistry;
    }
    this.eventMulticaster = new EntityEventMulticaster(this.entityEventRegistry);
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
    this.statementFactories.addConditionPropertyExtractor(extractor);
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
    this.statementFactories.setConditionPropertyExtractors(extractors);
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

    eventMulticaster.onPrePersist(entity, entityMetadata, strategy);

    var pair = insertStatement(strategy, entity, entityMetadata);

    if (stmtLogger.isDebugEnabled()) {
      stmtLogger.logStatement(LogMessage.format("Persisting entity: {}", entity), pair.first);
    }

    Connection con = DataSourceUtils.getConnection(dataSource);
    PreparedStatement statement = null;
    ResultSet generatedKeys = null;
    try {
      autoGenerateId = autoGenerateId || entityMetadata.isAutoGeneratedId();
      statement = prepareStatement(con, pair.first, autoGenerateId);
      setParameters(entity, pair.second, statement);
      // execute
      int updateCount = statement.executeUpdate();
      if (autoGenerateId) {
        EntityProperty idProperty = entityMetadata.getIdProperty();
        if (idProperty != null) {
          try {
            generatedKeys = statement.getGeneratedKeys();
            if (generatedKeys.next()) {
              idProperty.setProperty(entity, generatedKeys, 1);
            }
          }
          catch (SQLException e) {
            throw new GeneratedKeysException("Cannot get generated keys", e);
          }
        }
      }
      eventMulticaster.onPostPersist(entity, entityMetadata, strategy);
      return updateCount;
    }
    catch (SQLException ex) {
      eventMulticaster.onPersistFailed(entity, entityMetadata, strategy, ex);
      throw translateException("Persisting entity", pair.first, ex);
    }
    catch (RuntimeException | Error ex) {
      eventMulticaster.onPersistFailed(entity, entityMetadata, strategy, ex);
      throw ex;
    }
    finally {
      closeResource(con, statement, generatedKeys);
    }
  }

  @Override
  public int persist(Iterable<?> entities) throws DataAccessException {
    return persist(entities, null, autoGenerateId);
  }

  @Override
  public int persist(Iterable<?> entities, boolean autoGenerateId) throws DataAccessException {
    return persist(entities, null, autoGenerateId);
  }

  @Override
  public int persist(Iterable<?> entities, @Nullable PropertyUpdateStrategy strategy) throws DataAccessException {
    return persist(entities, strategy, autoGenerateId);
  }

  @Override
  public int persist(Iterable<?> entities, @Nullable PropertyUpdateStrategy strategy, boolean autoGenerateId)
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
                    pair.second, autoGenerateId || entityMetadata.isAutoGeneratedId());
            statements.put(entityClass, batch);
          }
          eventMulticaster.onPrePersist(entity, batch.entityMetadata, batch.strategy);
          batch.addBatchUpdate(entity, maxBatchRecords);
        }

        int updateCount = 0;
        for (PreparedBatch preparedBatch : statements.values()) {
          updateCount += preparedBatch.explicitExecuteBatch();
        }
        transaction.commit(false);
        return updateCount;
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
    EntityProperty idProperty = metadata.getIdProperty();
    if (idProperty != null) {
      Object id = idProperty.getValue(entity);
      if (id != null) {
        return doUpdateById(entity, id, idProperty, metadata, updateExcludeId(strategy));
      }
    }

    eventMulticaster.onPreUpdate(entity, metadata, strategy);

    EntityProperty versionProperty = metadata.getVersionProperty();
    Object oldVersion = null;
    if (versionProperty != null) {
      oldVersion = versionProperty.getValue(entity);
      versionProperty.setValue(entity, incrementVersion(oldVersion));
    }

    Update updateStmt = new Update(metadata.getTableName());

    ArrayList<EntityProperty> properties = new ArrayList<>(4);
    ArrayList<EntityProperty> updateByProperties = new ArrayList<>(2);
    for (EntityProperty property : metadata.getEntityProperties(true)) {
      if (property == versionProperty) {
        updateStmt.addAssignment(property.getColumnName());
        properties.add(property);
      }
      else if (property.isPresent(UpdateBy.class)) {
        updateByProperties.add(property);
        updateStmt.addRestriction(property.getColumnName());
      }
      else if (strategy.shouldUpdate(entity, property)) {
        properties.add(property);
        updateStmt.addAssignment(property.getColumnName());
      }
    }

    if (properties.isEmpty()) {
      throw new InvalidDataAccessApiUsageException("Updating an entity, There is no update properties");
    }

    if (updateByProperties.isEmpty()) {
      throw new InvalidDataAccessApiUsageException("Updating an entity, There is no update by properties");
    }

    if (versionProperty != null) {
      updateStmt.addRestriction(versionProperty.getColumnName());
    }

    String sql = updateStmt.toStatementString(platform);

    if (stmtLogger.isDebugEnabled()) {
      stmtLogger.logStatement(LogMessage.format("Updating entity using: '{}'", updateByProperties), sql);
    }

    Connection con = DataSourceUtils.getConnection(dataSource);
    PreparedStatement statement = null;
    try {
      statement = prepareStatement(con, sql, false);
      int idx = setParameters(entity, properties, statement);
      // apply where parameters
      for (EntityProperty updateBy : updateByProperties) {
        updateBy.setTo(statement, idx++, entity);
      }

      if (versionProperty != null) {
        versionProperty.setParameter(statement, idx, oldVersion);
      }

      int updateCount = statement.executeUpdate();
      if (versionProperty != null && updateCount != 1) {
        throw new OptimisticLockingFailureException(
                "Optimistic locking failure updating entity [%s], expected version: %s, but %d row(s) were updated"
                        .formatted(metadata.getTableName(), oldVersion, updateCount));
      }
      eventMulticaster.onPostUpdate(entity, metadata, strategy);
      return updateCount;
    }
    catch (SQLException ex) {
      eventMulticaster.onUpdateFailed(entity, metadata, strategy, ex);
      throw translateException("Updating entity", sql, ex);
    }
    catch (RuntimeException | Error ex) {
      eventMulticaster.onUpdateFailed(entity, metadata, strategy, ex);
      throw ex;
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
    EntityProperty idProperty = idProperty(metadata, "Updating an entity, Id property not found");

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
    return (entity, property) -> !property.isIdProperty() && strategy.shouldUpdate(entity, property);
  }

  @Override
  public int updateById(Object entity, Object id, @Nullable PropertyUpdateStrategy strategy) {
    Assert.notNull(id, "Entity id is required");
    EntityMetadata metadata = entityMetadataFactory.getEntityMetadata(entity.getClass());
    EntityProperty idProperty = idProperty(metadata, "Updating an entity, Id property not found");
    Assert.isTrue(idProperty.getBeanProperty().isInstance(id), "Entity Id matches failed");

    if (strategy == null) {
      strategy = defaultUpdateStrategy(entity);
    }

    return doUpdateById(entity, id, idProperty, metadata, strategy);
  }

  private int doUpdateById(Object entity, Object id, EntityProperty idProperty, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
    eventMulticaster.onPreUpdate(entity, metadata, strategy);

    EntityProperty versionProperty = metadata.getVersionProperty();
    Object oldVersion = null;
    if (versionProperty != null) {
      oldVersion = versionProperty.getValue(entity);
      versionProperty.setValue(entity, incrementVersion(oldVersion));
    }

    Update updateStmt = new Update(metadata.getTableName());
    updateStmt.addRestriction(idProperty.getColumnName());

    ArrayList<EntityProperty> properties = new ArrayList<>();
    for (EntityProperty property : metadata.getEntityProperties(false)) {
      if (property == versionProperty || strategy.shouldUpdate(entity, property)) {
        updateStmt.addAssignment(property.getColumnName());
        properties.add(property);
      }
    }

    if (versionProperty != null) {
      updateStmt.addRestriction(versionProperty.getColumnName());
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
      statement = prepareStatement(con, sql, false);
      int idx = setParameters(entity, properties, statement);
      // last one is ID
      idProperty.setParameter(statement, idx, id);
      if (versionProperty != null) {
        versionProperty.setParameter(statement, idx + 1, oldVersion);
      }
      int updateCount = statement.executeUpdate();
      if (versionProperty != null && updateCount != 1) {
        throw new OptimisticLockingFailureException(
                "Optimistic locking failure updating entity [%s] with ID: %s, expected version: %s, but %d row(s) were updated"
                        .formatted(metadata.getTableName(), id, oldVersion, updateCount));
      }
      eventMulticaster.onPostUpdate(entity, metadata, strategy);
      return updateCount;
    }
    catch (SQLException ex) {
      eventMulticaster.onUpdateFailed(entity, metadata, strategy, ex);
      throw translateException("Updating entity By ID", sql, ex);
    }
    catch (RuntimeException | Error ex) {
      eventMulticaster.onUpdateFailed(entity, metadata, strategy, ex);
      throw ex;
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
    if (strategy == null) {
      strategy = defaultUpdateStrategy(entity);
    }

    eventMulticaster.onPreUpdate(entity, metadata, strategy);
    Update updateStmt = new Update(metadata.getTableName());

    EntityProperty updateBy = null;
    ArrayList<EntityProperty> properties = new ArrayList<>();
    for (EntityProperty property : metadata.getEntityProperties(false)) {
      // columnName or property name
      if (Objects.equals(where, property.getColumnName())
              || Objects.equals(where, property.getBeanProperty().getName())) {
        updateBy = property;
      }
      else if (strategy.shouldUpdate(entity, property)) {
        updateStmt.addAssignment(property.getColumnName());
        properties.add(property);
      }
    }

    if (updateBy == null) {
      throw new InvalidDataAccessApiUsageException("Updating an entity, 'where' property '%s' not found".formatted(where));
    }

    updateStmt.addRestriction(updateBy.getColumnName());

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
      statement = prepareStatement(con, sql, false);
      int idx = setParameters(entity, properties, statement);
      // last one is where
      updateBy.setParameter(statement, idx, updateByValue);
      int updateCount = statement.executeUpdate();
      eventMulticaster.onPostUpdate(entity, metadata, strategy);
      return updateCount;
    }
    catch (SQLException ex) {
      eventMulticaster.onUpdateFailed(entity, metadata, strategy, ex);
      throw translateException("Updating entity By " + where, sql, ex);
    }
    catch (RuntimeException | Error ex) {
      eventMulticaster.onUpdateFailed(entity, metadata, strategy, ex);
      throw ex;
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

    eventMulticaster.onPreDelete(null, id, metadata);

    StringBuilder sql = new StringBuilder();
    sql.append("DELETE FROM ");
    sql.append(metadata.getTableName());
    sql.append(" WHERE `");
    sql.append(idProperty.getColumnName());
    sql.append("` = ? ");

    if (stmtLogger.isDebugEnabled()) {
      stmtLogger.logStatement(LogMessage.format("Deleting entity using ID: {}", id), sql);
    }

    Connection con = DataSourceUtils.getConnection(dataSource);
    PreparedStatement statement = null;
    try {
      statement = prepareStatement(con, sql.toString(), false);
      idProperty.setParameter(statement, 1, id);
      int updateCount = statement.executeUpdate();
      eventMulticaster.onPostDelete(null, id, metadata);
      return updateCount;
    }
    catch (SQLException ex) {
      eventMulticaster.onDeleteFailed(null, id, metadata, ex);
      throw translateException("Deleting entity using ID", sql.toString(), ex);
    }
    catch (RuntimeException | Error ex) {
      eventMulticaster.onDeleteFailed(null, id, metadata, ex);
      throw ex;
    }
    finally {
      closeResource(con, statement);
    }
  }

  @Override
  public int delete(Object entityOrExample) throws DataAccessException {
    EntityMetadata metadata = entityMetadataFactory.getEntityMetadata(entityOrExample.getClass());

    Object id = null;
    EntityProperty idProperty = metadata.getIdProperty();
    if (idProperty != null) {
      id = idProperty.getValue(entityOrExample);
    }

    EntityProperty versionProperty = metadata.getVersionProperty();
    Object versionValue = null;
    if (versionProperty != null) {
      versionValue = versionProperty.getValue(entityOrExample);
    }

    eventMulticaster.onPreDelete(entityOrExample, id, metadata);

    QueryCondition conditionStmt = null;

    StringBuilder sql = new StringBuilder();
    sql.append("DELETE FROM ");
    sql.append(metadata.getTableName());
    if (id != null) {
      // delete by id
      sql.append(" WHERE `");
      sql.append(idProperty.getColumnName());
      sql.append("` = ? ");
      if (versionProperty != null && versionValue != null) {
        sql.append("AND `").append(versionProperty.getColumnName()).append("` = ? ");
      }
    }
    else {
      conditionStmt = statementFactories.createCondition(entityOrExample);
      if (conditionStmt != null) {
        conditionStmt.appendWhereClause(metadata, sql);
      }
    }

    if (stmtLogger.isDebugEnabled()) {
      stmtLogger.logStatement(LogMessage.format("Deleting entity: [{}]", entityOrExample), sql);
    }

    Connection con = DataSourceUtils.getConnection(dataSource);
    PreparedStatement statement = null;
    try {
      statement = prepareStatement(con, sql.toString(), false);
      if (id != null) {
        int paramIdx = 1;
        idProperty.setParameter(statement, paramIdx++, id);
        if (versionProperty != null && versionValue != null) {
          versionProperty.setParameter(statement, paramIdx, versionValue);
        }
      }
      else if (conditionStmt != null) {
        conditionStmt.setParameter(metadata, statement);
      }

      int updateCount = statement.executeUpdate();
      if (versionProperty != null && versionValue != null && updateCount != 1) {
        throw new OptimisticLockingFailureException(
                "Optimistic locking failure deleting entity [%s] with ID: %s, expected version: %s, but %d row(s) were deleted"
                        .formatted(metadata.getTableName(), id, versionValue, updateCount));
      }
      eventMulticaster.onPostDelete(entityOrExample, id, metadata);
      return updateCount;
    }
    catch (SQLException ex) {
      eventMulticaster.onDeleteFailed(entityOrExample, id, metadata, ex);
      throw translateException("Deleting entity", sql.toString(), ex);
    }
    catch (RuntimeException | Error ex) {
      eventMulticaster.onDeleteFailed(entityOrExample, id, metadata, ex);
      throw ex;
    }
    finally {
      closeResource(con, statement);
    }
  }

  @Override
  public void truncate(Class<?> entityClass) throws DataAccessException {
    EntityMetadata metadata = entityMetadataFactory.getEntityMetadata(entityClass);

    String sql = platform.getTruncateTableStatement(metadata.getTableName());
    if (stmtLogger.isDebugEnabled()) {
      stmtLogger.logStatement(LogMessage.format("Truncate table: [{}]", entityClass), sql);
    }

    PreparedStatement statement = null;
    Connection con = DataSourceUtils.getConnection(dataSource);
    try {
      statement = prepareStatement(con, sql, false);
      statement.executeUpdate();
      eventMulticaster.onPostTruncate(entityClass, metadata);
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
  public <T> @Nullable T findById(Class<T> entityClass, Object id) throws DataAccessException {
    return iterate(entityClass, new FindByIdQuery(id)).first();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> @Nullable T findFirst(T entity) throws DataAccessException {
    return findFirst((Class<T>) entity.getClass(), entity);
  }

  @Override
  public <T> @Nullable T findFirst(Class<T> entityClass) throws DataAccessException {
    return findFirst(entityClass, null);
  }

  @Override
  public <T> @Nullable T findFirst(Class<T> entityClass, Object example) throws DataAccessException {
    return iterate(entityClass, example).first();
  }

  @Override
  public <T> @Nullable T findFirst(Class<T> entityClass, @Nullable QueryStatement handler) throws DataAccessException {
    return iterate(entityClass, handler).first();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> @Nullable T findUnique(T example) throws DataAccessException {
    return iterate((Class<T>) example.getClass(), example).unique();
  }

  @Override
  public <T> @Nullable T findUnique(Class<T> entityClass, Object example) throws DataAccessException {
    return iterate(entityClass, example).unique();
  }

  @Override
  public <T> @Nullable T findUnique(Class<T> entityClass, @Nullable QueryStatement handler) throws DataAccessException {
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
    return count(entityClass, statementFactories.createCondition(example));
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
    return page(entityClass, statementFactories.createCondition(example), pageable);
  }

  @Override
  public <T> Page<T> page(Class<T> entityClass, @Nullable QueryCondition handler) throws DataAccessException {
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
    return iterate(entityClass, statementFactories.createQuery(example));
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
      PreparedStatement stmt = prepareStatement(con, statement, false);
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
  public <T> Number count(Class<T> entityClass, @Nullable QueryCondition handler) throws DataAccessException {
    if (handler == null) {
      handler = NoConditionsQuery.instance;
    }
    EntityMetadata metadata = entityMetadataFactory.getEntityMetadata(entityClass);

    List<Restriction> restrictions = handler.collectRestrictions(metadata);
    Connection con = DataSourceUtils.getConnection(dataSource);
    try {
      return doQueryCount(metadata, handler, restrictions, con);
    }
    finally {
      DataSourceUtils.releaseConnection(con, dataSource);
    }
  }

  @Override
  public <T> Page<T> page(Class<T> entityClass, @Nullable QueryCondition handler, @Nullable Pageable pageable) throws DataAccessException {
    if (handler == null) {
      handler = NoConditionsQuery.instance;
    }

    if (pageable == null) {
      pageable = defaultPageable();
    }

    EntityMetadata metadata = entityMetadataFactory.getEntityMetadata(entityClass);
    List<Restriction> restrictions = handler.collectRestrictions(metadata);

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

      statement = new SimpleSelect(Arrays.asList(metadata.getColumnNames(false)), restrictions)
              .setTableName(metadata.getTableName())
              .pageable(pageable)
              .orderBy(handler.resolveOrderByClause(metadata))
              .toStatementString(platform);

      stmt = prepareStatement(con, statement, false);
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

  private Number doQueryCount(EntityMetadata metadata, QueryCondition handler, List<Restriction> restrictions, Connection con) throws DataAccessException {
    StringBuilder countSql = new StringBuilder(restrictions.size() * 10 + 25 + metadata.getTableName().length());
    platform.selectCountFrom(countSql, metadata.getTableName());

    Restriction.append(restrictions, countSql);

    String statement = countSql.toString();
    ResultSet resultSet = null;
    PreparedStatement stmt = null;
    try {
      stmt = prepareStatement(con, statement, false);
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
    PreparedStatement statement = autoGenerateId
            ? connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
            : connection.prepareStatement(sql);
    return LoggingPreparedStatement.wrap(statement, stmtLogger);
  }

  private DataAccessException translateException(String task, @Nullable String sql, SQLException ex) {
    return repositoryManager.translateException(task, sql, ex);
  }

  private Pair<String, ArrayList<EntityProperty>> insertStatement(PropertyUpdateStrategy strategy, Object entity, EntityMetadata entityMetadata) {
    Insert insert = new Insert(entityMetadata.getTableName());
    EntityProperty[] entityProperties = entityMetadata.getEntityProperties(false);
    var properties = new ArrayList<EntityProperty>(entityProperties.length);
    for (EntityProperty property : entityProperties) {
      if (strategy.shouldUpdate(entity, property)) {
        insert.addColumn(property.getColumnName());
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

  String getDescription(Object handler) {
    Descriptive descriptive = null;
    if (handler instanceof Descriptive) {
      descriptive = (Descriptive) handler;
    }
    if (descriptive == null) {
      descriptive = NoConditionsQuery.instance;
    }
    return descriptive.getDescription();
  }

  Object getDebugLogMessage(Object handler) {
    if (handler instanceof DebugDescriptive descriptive) {
      return descriptive.getDebugLogMessage();
    }
    else if (handler instanceof Descriptive) {
      return LogMessage.format(((Descriptive) handler).getDescription());
    }
    return NoConditionsQuery.instance.getDebugLogMessage();
  }

  boolean isNew(Object entity) throws IllegalEntityException {
    if (entity instanceof NewEntityIndicator e) {
      return e.isNew();
    }
    EntityProperty idProperty = entityMetadataFactory.getEntityMetadata(entity.getClass()).getIdProperty();
    if (idProperty != null) {
      return idProperty.getValue(entity) == null;
    }
    return false;
  }

  private static EntityProperty idProperty(EntityMetadata metadata, String error) {
    EntityProperty idProperty = metadata.findIdProperty();
    if (idProperty == null) {
      throw new InvalidDataAccessApiUsageException(error);
    }
    return idProperty;
  }

  //

  private Object incrementVersion(@Nullable Object currentVersion) {
    Assert.notNull(currentVersion, "Entity version not set");
    Object next = versionIncrementStrategy.nextVersion(currentVersion);
    if (next == null) {
      throw new IllegalArgumentException(
              "VersionIncrementStrategy returned null for current version: " + currentVersion);
    }
    return next;
  }

  static int setParameters(Object entity, ArrayList<EntityProperty> properties, PreparedStatement statement) throws SQLException {
    int idx = 1;
    for (EntityProperty property : properties) {
      property.setTo(statement, idx++, entity);
    }
    return idx;
  }

  static void assertUpdateCount(String sql, int actualCount, int expectCount) {
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
    protected @Nullable T readNext(ResultSet resultSet) throws SQLException {
      T entity = handler.extractData(resultSet);
      if (entity != null) {
        eventMulticaster.onPostLoad(entity, entityMetadata);
      }
      return entity;
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

    public final PreparedStatement stmt;

    public final ArrayList<EntityProperty> properties;

    public int currentBatchRecords = 0;

    private int affectedRows = 0;

    PreparedBatch(Connection connection, String sql, PropertyUpdateStrategy strategy,
            EntityMetadata entityMetadata, ArrayList<EntityProperty> properties, boolean autoGenerateId) throws SQLException {
      super(sql, strategy, entityMetadata, autoGenerateId);
      this.properties = properties;
      this.stmt = prepareStatement(connection, sql, autoGenerateId);
    }

    public void addBatchUpdate(Object entity, int maxBatchRecords) throws Throwable {
      entities.add(entity);
      PreparedStatement statement = this.stmt;
      setParameters(entity, properties, statement);
      statement.addBatch();
      if (maxBatchRecords > 0 && ++currentBatchRecords % maxBatchRecords == 0) {
        executeBatch(statement, true);
      }
    }

    public int explicitExecuteBatch() throws Throwable {
      executeBatch(stmt, false);
      closeResource(null, stmt);
      return affectedRows;
    }

    private void executeBatch(PreparedStatement statement, boolean implicitExecution) throws Throwable {
      eventMulticaster.preProcessing(this, implicitExecution);
      if (stmtLogger.isDebugEnabled()) {
        stmtLogger.logStatement(LogMessage.format("Executing batch size: {}", entities.size()), this.statement);
      }
      Throwable exception = null;
      try {
        int batchSize = entities.size();
        int[] updateCounts = statement.executeBatch();
        assertUpdateCount(this.statement, updateCounts.length, batchSize);

        if (autoGenerateId) {
          EntityProperty idProperty = entityMetadata.getIdProperty();
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
        for (Object entity : entities) {
          eventMulticaster.onPostPersist(entity, entityMetadata, strategy);
        }
        this.affectedRows += batchSize;
      }
      catch (Throwable e) {
        exception = e;
        throw e;
      }
      finally {
        eventMulticaster.postProcessing(this, implicitExecution, exception);
        this.currentBatchRecords = 0;
        this.entities.clear();
      }
    }

    @Override
    public int getAffectedRows() {
      return affectedRows;
    }

  }

}
