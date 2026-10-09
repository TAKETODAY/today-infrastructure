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

package infra.persistence.support;

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
import java.util.function.Consumer;
import java.util.function.Function;

import javax.sql.DataSource;

import infra.beans.factory.support.StandardBeanFactory;
import infra.dao.DataAccessException;
import infra.dao.DataRetrievalFailureException;
import infra.dao.InvalidDataAccessApiUsageException;
import infra.dao.OptimisticLockingFailureException;
import infra.jdbc.DefaultResultSetHandlerFactory;
import infra.jdbc.GeneratedKeysException;
import infra.jdbc.JdbcBeanMetadata;
import infra.jdbc.JdbcConnection;
import infra.jdbc.JdbcUpdateAffectedIncorrectNumberOfRowsException;
import infra.jdbc.PersistenceException;
import infra.jdbc.RepositoryManager;
import infra.jdbc.core.ResultSetExtractor;
import infra.jdbc.datasource.DataSourceUtils;
import infra.jdbc.format.LoggingPreparedStatement;
import infra.jdbc.format.SqlStatementLogger;
import infra.lang.Descriptive;
import infra.logging.LogMessage;
import infra.persistence.DebugDescriptive;
import infra.persistence.DefaultEntityMetadataFactory;
import infra.persistence.EntityIterator;
import infra.persistence.EntityManager;
import infra.persistence.EntityMetadata;
import infra.persistence.EntityMetadataFactory;
import infra.persistence.EntityProperty;
import infra.persistence.IllegalEntityException;
import infra.persistence.NewEntityIndicator;
import infra.persistence.Order;
import infra.persistence.Page;
import infra.persistence.Pageable;
import infra.persistence.PropertyUpdateStrategy;
import infra.persistence.Scroll;
import infra.persistence.ScrollPageable;
import infra.persistence.ScrollPosition;
import infra.persistence.Slice;
import infra.persistence.UpdateStrategySource;
import infra.persistence.VersionIncrementStrategy;
import infra.persistence.annotation.UpdateBy;
import infra.persistence.annotation.Version;
import infra.persistence.event.BatchExecution;
import infra.persistence.event.BatchExecutionListener;
import infra.persistence.event.BatchOperation;
import infra.persistence.event.DefaultEntityEventRegistry;
import infra.persistence.event.EntityEventRegistry;
import infra.persistence.event.EntityOperationPhase;
import infra.persistence.platform.Platform;
import infra.persistence.query.EntityQueryFactories;
import infra.persistence.query.EntityQueryFactory;
import infra.persistence.query.FindByIdQuery;
import infra.persistence.query.NoConditionsQuery;
import infra.persistence.query.QueryCondition;
import infra.persistence.query.QueryStatement;
import infra.persistence.sql.Insert;
import infra.persistence.sql.OrderSpec;
import infra.persistence.sql.Restriction;
import infra.persistence.sql.Restrictions;
import infra.persistence.sql.SimpleSelect;
import infra.persistence.sql.Update;
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

  private final DataSource dataSource;

  private final RepositoryManager repositoryManager;

  private int maxBatchRecords = 0;

  private Platform platform;

  private EntityMetadataFactory entityMetadataFactory = new DefaultEntityMetadataFactory();

  private PropertyUpdateStrategy defaultUpdateStrategy = PropertyUpdateStrategy.noneNull();

  private VersionIncrementStrategy versionIncrementStrategy = new DefaultVersionIncrementStrategy();

  private Pageable defaultPageable = Pageable.of(1, 10);

  private SqlStatementLogger stmtLogger = SqlStatementLogger.sharedInstance;

  private EntityEventRegistry entityEventRegistry = new DefaultEntityEventRegistry();

  private EntityEventMulticaster eventMulticaster = new EntityEventMulticaster(entityEventRegistry);

  private @Nullable TransactionDefinition transactionConfig = TransactionDefinition.withDefaults();

  private EntityQueryFactories entityQueryFactories = new EntityQueryFactories(entityMetadataFactory);

  private IdGeneratorResolver idGeneratorResolver = new IdGeneratorResolver(new StandardBeanFactory());

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
   * This method also initializes a new instance of {@code EntityQueryFactories}
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
    this.entityQueryFactories.setEntityMetadataFactory(entityMetadataFactory);
  }

  /**
   * Return the {@link EntityQueryFactories} managing the {@link EntityQueryFactory}
   * instances used to turn an example object into a {@link QueryStatement} or
   * {@link QueryCondition}.
   *
   * <p>Use it to register custom factories:
   * <pre>{@code
   * entityManager.getEntityQueryFactories().addFactory(myFactory);
   * }</pre>
   *
   * @since 5.0
   */
  public EntityQueryFactories getEntityQueryFactories() {
    return entityQueryFactories;
  }

  /**
   * Set the {@link EntityQueryFactories} used to turn an example object into a
   * {@link QueryStatement} or {@link QueryCondition}. When {@code null}, the default
   * {@link EntityQueryFactories} is restored.
   *
   * @param entityQueryFactories the factories to use, or {@code null} to use the default
   * @since 5.0
   */
  public void setEntityQueryFactories(@Nullable EntityQueryFactories entityQueryFactories) {
    if (entityQueryFactories == null) {
      this.entityQueryFactories = new EntityQueryFactories(entityMetadataFactory);
    }
    else {
      entityQueryFactories.setEntityMetadataFactory(entityMetadataFactory);
      this.entityQueryFactories = entityQueryFactories;
    }
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
   * and {@link BatchExecutionListener batch persist
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
   * Return the resolver for configuring application-generated IDs.
   *
   * @return the ID generator resolver
   * @since 5.0
   */
  public IdGeneratorResolver getIdGeneratorResolver() {
    return idGeneratorResolver;
  }

  /**
   * Configure the resolver used for application-generated IDs.
   * The supplied resolver's lifecycle remains managed by its owner.
   *
   * @param idGeneratorResolver the ID generator resolver
   * @since 5.0
   */
  public void setIdGeneratorResolver(IdGeneratorResolver idGeneratorResolver) {
    Assert.notNull(idGeneratorResolver, "idGeneratorResolver is required");
    this.idGeneratorResolver = idGeneratorResolver;
  }

  // ---------------------------------------------------------------------
  // Implementation of EntityManager
  // ---------------------------------------------------------------------

  @Override
  public int persist(Object entity) throws DataAccessException {
    return persist(entity, defaultUpdateStrategy(entity));
  }

  @Override
  public int persist(Object entity, @Nullable PropertyUpdateStrategy strategy) throws DataAccessException {
    EntityMetadata entityMetadata = entityMetadataFactory.getEntityMetadata(entity.getClass());
    if (strategy == null) {
      strategy = defaultUpdateStrategy(entity);
    }

    List<EntityProperty> properties = null;
    String sql = null;
    Connection con = null;
    PreparedStatement statement = null;
    ResultSet generatedKeys = null;
    EntityOperationPhase phase = EntityOperationPhase.PRE_PROCESSING;
    try {
      strategy = eventMulticaster.onPrePersist(entity, entityMetadata, strategy);
      phase = EntityOperationPhase.PREPARATION;
      idGeneratorResolver.generateId(entity, entityMetadata);
      properties = selectInsertProperties(strategy, entity, entityMetadata);
      sql = insertStatement(entityMetadata, properties);
      if (stmtLogger.isDebugEnabled()) {
        stmtLogger.logStatement(LogMessage.format("Persisting entity: {}", entity), sql);
      }
      con = DataSourceUtils.getConnection(dataSource);
      boolean autoGenerateId = entityMetadata.isAutoGeneratedId();
      statement = prepareStatement(con, sql, autoGenerateId);
      setParameters(entity, properties, statement);
      // execute
      phase = EntityOperationPhase.EXECUTION;
      int updateCount = statement.executeUpdate();
      phase = EntityOperationPhase.POST_PROCESSING;
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

      eventMulticaster.onPostPersist(entity, entityMetadata, properties);
      return updateCount;
    }
    catch (Throwable ex) {
      eventMulticaster.onPersistFailed(entity, entityMetadata, phase, properties, ex);
      throw handleEntityException("Persist entity", entityMetadata, phase, sql, ex);
    }
    finally {
      closeResource(con, statement, generatedKeys);
    }
  }

  @Override
  public int persist(Iterable<?> entities) throws DataAccessException {
    return persist(entities, null);
  }

  @Override
  public int persist(Iterable<?> entities, @Nullable PropertyUpdateStrategy strategy) throws DataAccessException {
    try (var transaction = repositoryManager.beginTransaction(transactionConfig)) {
      var statements = new HashMap<BatchKey, PreparedBatch>(8);
      try {
        for (Object entity : entities) {
          Class<?> entityClass = entity.getClass();
          EntityMetadata entityMetadata = entityMetadataFactory.getEntityMetadata(entityClass);
          var strategyToUse = eventMulticaster.onPrePersist(entity, entityMetadata, strategy != null ? strategy : defaultUpdateStrategy(entity));
          idGeneratorResolver.generateId(entity, entityMetadata);
          var properties = selectInsertProperties(strategyToUse, entity, entityMetadata);
          boolean generatedKeys = entityMetadata.isAutoGeneratedId();
          var key = new BatchKey(entityClass, properties);
          PreparedBatch batch = statements.get(key);
          if (batch == null) {
            String sql = insertStatement(entityMetadata, properties);
            batch = new PreparedBatch(transaction.getNativeConnection(), sql, entityMetadata,
                    properties, generatedKeys, BatchOperation.INSERT);
            statements.put(key, batch);
          }
          batch.addBatchUpdate(entity);
        }

        int updateCount = 0;
        for (PreparedBatch preparedBatch : statements.values()) {
          int count = preparedBatch.explicitExecuteBatch();
          updateCount = computeUpdateRows(updateCount, count);
        }
        closeBatchStatements(statements, null);
        transaction.commit(false);
        return updateCount;
      }
      catch (Throwable ex) {
        throw handleBatchFailure("Batch persist entities", statements, transaction, ex);
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

    EntityOperationPhase phase = EntityOperationPhase.PRE_PROCESSING;

    List<EntityProperty> selectedProperties = null;
    String sql = null;
    Connection con = null;
    PreparedStatement statement = null;
    try {
      strategy = eventMulticaster.onPreUpdate(entity, metadata, strategy);
      phase = EntityOperationPhase.PREPARATION;
      Object oldVersion = incrementVersion(entity, metadata);
      EntityProperty versionProperty = metadata.getVersionProperty();

      Update updateStmt = new Update(metadata.getTableName());
      ArrayList<EntityProperty> properties = new ArrayList<>(4);
      ArrayList<EntityProperty> updateByProperties = new ArrayList<>(2);
      for (EntityProperty property : metadata.getEntityProperties(false)) {
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
      selectedProperties = Collections.unmodifiableList(properties);

      if (updateByProperties.isEmpty()) {
        throw new InvalidDataAccessApiUsageException("Updating an entity, There is no update by properties");
      }

      if (versionProperty != null) {
        updateStmt.addRestriction(versionProperty.getColumnName());
      }

      sql = updateStmt.toStatementString(platform);

      if (stmtLogger.isDebugEnabled()) {
        stmtLogger.logStatement(LogMessage.format("Updating entity using: '{}'", updateByProperties), sql);
      }

      con = DataSourceUtils.getConnection(dataSource);
      statement = prepareStatement(con, sql, false);
      int idx = setParameters(entity, selectedProperties, statement);
      // apply where parameters
      for (EntityProperty updateBy : updateByProperties) {
        updateBy.setTo(statement, idx++, entity);
      }

      if (versionProperty != null) {
        versionProperty.setParameter(statement, idx, oldVersion);
      }

      phase = EntityOperationPhase.EXECUTION;
      int updateCount = statement.executeUpdate();
      if (versionProperty != null && updateCount != 1) {
        throw new OptimisticLockingFailureException(
                "Optimistic locking failure updating entity [%s], expected version: %s, but %d row(s) were updated"
                        .formatted(metadata.getTableName(), oldVersion, updateCount));
      }
      phase = EntityOperationPhase.POST_PROCESSING;
      eventMulticaster.onPostUpdate(entity, metadata, selectedProperties, updateCount);
      return updateCount;
    }
    catch (Throwable ex) {
      eventMulticaster.onUpdateFailed(entity, metadata, phase, selectedProperties, null, ex);
      throw handleEntityException("Update entity using @UpdateBy properties", metadata, phase, sql, ex);
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
    return PropertyUpdateStrategy.notId().and(strategy);
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
    EntityOperationPhase phase = EntityOperationPhase.PRE_PROCESSING;
    List<EntityProperty> properties = null;
    String sql = null;
    Connection con = null;
    PreparedStatement statement = null;
    try {
      strategy = eventMulticaster.onPreUpdate(entity, metadata, strategy);
      phase = EntityOperationPhase.PREPARATION;
      Object oldVersion = incrementVersion(entity, metadata);
      properties = selectUpdateProperties(entity, metadata, strategy);
      sql = updateStatement(metadata, properties, idProperty);

      if (stmtLogger.isDebugEnabled()) {
        stmtLogger.logStatement(LogMessage.format("Updating entity using ID: '{}'", id), sql);
      }

      con = DataSourceUtils.getConnection(dataSource);
      statement = prepareStatement(con, sql, false);
      int idx = setParameters(entity, properties, statement);
      // last one is ID
      idProperty.setParameter(statement, idx, id);

      EntityProperty versionProperty = metadata.getVersionProperty();
      if (versionProperty != null) {
        versionProperty.setParameter(statement, idx + 1, oldVersion);
      }
      phase = EntityOperationPhase.EXECUTION;
      int updateCount = statement.executeUpdate();
      if (versionProperty != null && updateCount != 1) {
        throw new OptimisticLockingFailureException(
                "Optimistic locking failure updating entity [%s] with ID: %s, expected version: %s, but %d row(s) were updated"
                        .formatted(metadata.getTableName(), id, oldVersion, updateCount));
      }
      phase = EntityOperationPhase.POST_PROCESSING;
      eventMulticaster.onPostUpdate(entity, metadata, properties, updateCount);
      return updateCount;
    }
    catch (Throwable ex) {
      eventMulticaster.onUpdateFailed(entity, metadata, phase, properties, id, ex);
      throw handleEntityException("Update entity by ID", metadata, phase, sql, ex);
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
  public int updateById(Iterable<?> entities, @Nullable PropertyUpdateStrategy strategy) {
    Assert.notNull(entities, "Entities are required");
    try (var transaction = repositoryManager.beginTransaction(transactionConfig)) {
      var statements = new HashMap<BatchKey, PreparedBatch>();
      try {
        for (Object entity : entities) {
          EntityMetadata metadata = entityMetadataFactory.getEntityMetadata(entity.getClass());
          EntityProperty idProperty = idProperty(metadata, "Updating an entity, Id property not found");
          Object id = idProperty.getValue(entity);
          if (id == null) {
            throw new InvalidDataAccessApiUsageException("Updating an entity, ID value is required");
          }
          var strategyToUse = eventMulticaster.onPreUpdate(entity, metadata,
                  updateExcludeId(strategy != null ? strategy : defaultUpdateStrategy(entity)));
          Object oldVersion = incrementVersion(entity, metadata);
          var properties = selectUpdateProperties(entity, metadata, strategyToUse);
          var key = new BatchKey(metadata.getEntityClass(), properties);
          PreparedBatch batch = statements.get(key);
          if (batch == null) {
            String sql = updateStatement(metadata, properties, idProperty);
            batch = new PreparedBatch(transaction.getNativeConnection(), sql, metadata,
                    properties, false, BatchOperation.UPDATE);
            statements.put(key, batch);
          }
          batch.addBatchUpdate(entity, idProperty, id, oldVersion);
        }

        int updateCount = 0;
        for (var preparedBatch : statements.values()) {
          int count = preparedBatch.explicitExecuteBatch();
          updateCount = computeUpdateRows(updateCount, count);
        }

        closeBatchStatements(statements, null);
        transaction.commit(false);
        return updateCount;
      }
      catch (Throwable ex) {
        throw handleBatchFailure("Batch update entities", statements, transaction, ex);
      }
    }
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
    Connection con = null;
    PreparedStatement statement = null;
    EntityOperationPhase phase = EntityOperationPhase.PRE_PROCESSING;
    try {
      eventMulticaster.onPreDelete(null, id, metadata);
      phase = EntityOperationPhase.PREPARATION;
      sql.append("DELETE FROM ");
      sql.append(metadata.getTableName().render(platform));
      sql.append(" WHERE ");
      sql.append(idProperty.getColumnName().render(platform));
      sql.append(" = ? ");

      if (stmtLogger.isDebugEnabled()) {
        stmtLogger.logStatement(LogMessage.format("Deleting entity using ID: {}", id), sql);
      }

      con = DataSourceUtils.getConnection(dataSource);
      statement = prepareStatement(con, sql.toString(), false);
      idProperty.setParameter(statement, 1, id);
      phase = EntityOperationPhase.EXECUTION;
      int updateCount = statement.executeUpdate();
      phase = EntityOperationPhase.POST_PROCESSING;
      eventMulticaster.onPostDelete(null, id, metadata, updateCount);
      return updateCount;
    }
    catch (Throwable ex) {
      eventMulticaster.onDeleteFailed(null, metadata, phase, id, ex);
      throw handleEntityException("Delete entity by ID", metadata, phase, sql.toString(), ex);
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

    StringBuilder sql = new StringBuilder();
    Connection con = null;
    PreparedStatement statement = null;
    EntityOperationPhase phase = EntityOperationPhase.PRE_PROCESSING;
    try {
      eventMulticaster.onPreDelete(entityOrExample, id, metadata);
      phase = EntityOperationPhase.PREPARATION;
      QueryCondition conditionStmt = null;
      sql.append("DELETE FROM ");
      sql.append(metadata.getTableName().render(platform));
      if (id != null) {
        // delete by id
        sql.append(" WHERE ");
        sql.append(idProperty.getColumnName().render(platform));
        sql.append(" = ? ");
        if (versionProperty != null && versionValue != null) {
          sql.append("AND ").append(versionProperty.getColumnName().render(platform)).append(" = ? ");
        }
      }
      else {
        conditionStmt = entityQueryFactories.createCondition(entityOrExample);
        conditionStmt.appendWhereClause(platform, metadata, sql);
      }

      if (stmtLogger.isDebugEnabled()) {
        stmtLogger.logStatement(LogMessage.format("Deleting entity: [{}]", entityOrExample), sql);
      }

      con = DataSourceUtils.getConnection(dataSource);
      statement = prepareStatement(con, sql.toString(), false);
      if (id != null) {
        int paramIdx = 1;
        idProperty.setParameter(statement, paramIdx++, id);
        if (versionProperty != null && versionValue != null) {
          versionProperty.setParameter(statement, paramIdx, versionValue);
        }
      }
      else {
        conditionStmt.setParameter(metadata, statement);
      }

      phase = EntityOperationPhase.EXECUTION;
      int updateCount = statement.executeUpdate();
      if (versionProperty != null && versionValue != null && updateCount != 1) {
        throw new OptimisticLockingFailureException(
                "Optimistic locking failure deleting entity [%s] with ID: %s, expected version: %s, but %d row(s) were deleted"
                        .formatted(metadata.getTableName(), id, versionValue, updateCount));
      }
      phase = EntityOperationPhase.POST_PROCESSING;
      eventMulticaster.onPostDelete(entityOrExample, id, metadata, updateCount);
      return updateCount;
    }
    catch (Throwable ex) {
      eventMulticaster.onDeleteFailed(entityOrExample, metadata, phase, id, ex);
      throw handleEntityException(id != null ? "Delete entity by ID" : "Delete entity by example",
              metadata, phase, sql.toString(), ex);
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
  public <T> @Nullable T findFirst(Class<T> entityClass, @Nullable QueryStatement statement) throws DataAccessException {
    return iterate(entityClass, statement).first();
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
  public <T> @Nullable T findUnique(Class<T> entityClass, @Nullable QueryStatement statement) throws DataAccessException {
    return iterate(entityClass, statement).unique();
  }

  @Override
  public <T> List<T> find(Class<T> entityClass) throws DataAccessException {
    return find(entityClass, (QueryStatement) null);
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
  public <T> List<T> find(Class<T> entityClass, @Nullable QueryStatement statement) throws DataAccessException {
    return iterate(entityClass, statement).list();
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
  public <K, T> Map<K, T> find(Class<T> entityClass, @Nullable QueryStatement statement, String mapKey) throws DataAccessException {
    return iterate(entityClass, statement).toMap(mapKey);
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
  public <K, T> Map<K, T> find(Class<T> entityClass, @Nullable QueryStatement statement, Function<T, K> keyMapper) throws DataAccessException {
    return iterate(entityClass, statement).toMap(keyMapper);
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
    return count(entityClass, entityQueryFactories.createCondition(example));
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
    return page(entityClass, entityQueryFactories.createCondition(example), pageable);
  }

  @Override
  public <T> Page<T> page(Class<T> entityClass, @Nullable QueryCondition condition) throws DataAccessException {
    return page(entityClass, condition, Pageable.unwrap(condition));
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
  public <T> void iterate(Class<T> entityClass, @Nullable QueryStatement statement, Consumer<T> entityConsumer) throws DataAccessException {
    iterate(entityClass, statement).consume(entityConsumer);
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
    return iterate(entityClass, entityQueryFactories.createQuery(example));
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
      repositoryManager.releaseConnection(con, dataSource, statement, ex);
      throw translateException(getDescription(handler), statement, ex);
    }
  }

  @Override
  public <T> Number count(Class<T> entityClass, @Nullable QueryCondition condition) throws DataAccessException {
    if (condition == null) {
      condition = NoConditionsQuery.instance;
    }
    EntityMetadata metadata = entityMetadataFactory.getEntityMetadata(entityClass);

    List<Restriction> restrictions = condition.collectRestrictions(metadata);
    Connection con = DataSourceUtils.getConnection(dataSource);
    try {
      return doQueryCount(metadata, condition, restrictions, con);
    }
    finally {
      repositoryManager.releaseConnection(con, dataSource, null, null);
    }
  }

  @Override
  public <T> Page<T> page(Class<T> entityClass, @Nullable QueryCondition condition, @Nullable Pageable pageable) throws DataAccessException {
    if (condition == null) {
      condition = NoConditionsQuery.instance;
    }

    if (pageable == null) {
      pageable = defaultPageable();
    }

    EntityMetadata metadata = entityMetadataFactory.getEntityMetadata(entityClass);
    List<Restriction> restrictions = condition.collectRestrictions(metadata);

    Connection con = DataSourceUtils.getConnection(dataSource);
    String statement = null;
    PreparedStatement stmt = null;
    try {
      Number count = doQueryCount(metadata, condition, restrictions, con);
      if (count.intValue() < 1) {
        // no record
        repositoryManager.releaseConnection(con, dataSource, null, null);
        return new Page<>(pageable, 0, Collections.emptyList());
      }

      statement = new SimpleSelect(metadata.getTableName(),
              Arrays.asList(metadata.getColumnNames(true)), restrictions)
              .pageable(pageable)
              .orderBy(condition.resolveOrderByClause(metadata))
              .toStatementString(platform);

      stmt = prepareStatement(con, statement, false);
      condition.setParameter(metadata, stmt);

      if (stmtLogger.isDebugEnabled()) {
        stmtLogger.logStatement(getDebugLogMessage(condition), statement);
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
        throw translateException(getDescription(condition), statement, (SQLException) ex);
      }
      throw new DataRetrievalFailureException("Unable to retrieve the pageable data ", ex);
    }
  }

  @Override
  public <T> Slice<T> slice(Class<T> entityClass, Object example, @Nullable Pageable pageable) throws DataAccessException {
    return slice(entityClass, entityQueryFactories.createCondition(example), pageable);
  }

  @Override
  public <T> Slice<T> slice(Class<T> entityClass, @Nullable QueryCondition condition, @Nullable Pageable pageable) throws DataAccessException {
    if (condition == null) {
      condition = NoConditionsQuery.instance;
    }
    if (pageable == null) {
      pageable = defaultPageable();
    }
    int pageSize = pageable.pageSize();
    int pageNumber = pageable.pageNumber();
    if (pageNumber < 1 || pageSize < 1 || pageSize == Integer.MAX_VALUE) {
      throw new IllegalArgumentException("Slice page number and size must be positive, and size must allow one extra row");
    }
    if (pageNumber - 1 > Integer.MAX_VALUE / pageSize) {
      throw new IllegalArgumentException("Slice offset exceeds the integer range");
    }

    EntityMetadata metadata = entityMetadataFactory.getEntityMetadata(entityClass);
    String statement = new SimpleSelect(metadata.getTableName(),
            Arrays.asList(metadata.getColumnNames(true)), condition.collectRestrictions(metadata))
            .limit(pageSize + 1)
            .offset(pageable.offset())
            .orderBy(condition.resolveOrderByClause(metadata))
            .toStatementString(platform);

    Connection con = DataSourceUtils.getConnection(dataSource);
    PreparedStatement stmt = null;
    try {
      stmt = prepareStatement(con, statement, false);
      condition.setParameter(metadata, stmt);
      if (stmtLogger.isDebugEnabled()) {
        stmtLogger.logStatement(getDebugLogMessage(condition), statement);
      }

      List<T> rows = new DefaultEntityIterator<T>(con, stmt, entityClass, metadata).list();
      boolean hasNext = rows.size() > pageSize;
      if (hasNext) {
        rows = rows.subList(0, pageSize);
      }
      return new Slice<>(rows, pageNumber, pageSize, hasNext);
    }
    catch (Throwable ex) {
      closeResource(con, stmt);
      if (ex instanceof DataAccessException dae) {
        throw dae;
      }
      if (ex instanceof SQLException sqlException) {
        throw translateException(getDescription(condition), statement, sqlException);
      }
      throw new DataRetrievalFailureException("Unable to retrieve the slice", ex);
    }
  }

  @Override
  public <T> Scroll<T> scroll(Class<T> entityClass, Object example, ScrollPageable pageable) {
    return scroll(entityClass, entityQueryFactories.createCondition(example), pageable);
  }

  @Override
  public <T> Scroll<T> scroll(Class<T> entityClass, @Nullable QueryCondition condition, ScrollPageable pageable) throws DataAccessException {
    Assert.notNull(pageable, "ScrollPageable is required");

    if (condition == null) {
      condition = NoConditionsQuery.instance;
    }

    int pageSize = pageable.pageSize();
    EntityMetadata metadata = entityMetadataFactory.getEntityMetadata(entityClass);
    ScrollPosition examplePosition = condition.scrollPosition(metadata);
    ScrollPosition position = pageable.position();
    if (examplePosition != null) {
      if (position != null) {
        throw new IllegalArgumentException("Scroll position must be supplied by either the example or ScrollPageable, not both");
      }
      position = examplePosition;
    }
    OrderSpec orderSpec = condition.resolveOrderByClause(metadata);
    KeysetOrder keysetOrder = KeysetOrder.resolve(metadata, orderSpec, position);
    List<ScrollPosition.Entry> cursor = position == null || position.isInitial() ? null : position.cursor();
    if (cursor != null) {
      keysetOrder.validateCursor(cursor);
    }

    List<Restriction> restrictions = condition.collectRestrictions(metadata);
    if (cursor != null) {
      restrictions.add(keysetOrder.afterCursor());
    }

    SimpleSelect select = new SimpleSelect(metadata.getTableName(),
            Arrays.asList(metadata.getColumnNames(true)), restrictions)
            .limit(pageSize + 1);
    keysetOrder.applyTo(select);
    String statement = select.toStatementString(platform);
    Connection con = DataSourceUtils.getConnection(dataSource);
    PreparedStatement stmt = null;
    try {
      stmt = prepareStatement(con, statement, false);
      int index = condition.setParameter(metadata, stmt);
      if (cursor != null) {
        keysetOrder.bindCursor(stmt, index, cursor);
      }
      if (stmtLogger.isDebugEnabled()) {
        stmtLogger.logStatement(getDebugLogMessage(condition), statement);
      }
      List<T> rows = new DefaultEntityIterator<T>(con, stmt, entityClass, metadata).list(pageSize + 1);
      for (T row : rows) {
        keysetOrder.validateRow(row);
      }
      boolean hasNext = rows.size() > pageSize;
      if (hasNext) {
        rows = rows.subList(0, pageSize);
      }
      return new ListScroll<>(rows, !hasNext, keysetOrder::positionFrom);
    }
    catch (Throwable ex) {
      closeResource(con, stmt);
      if (ex instanceof DataAccessException dae) {
        throw dae;
      }
      if (ex instanceof SQLException sqlException) {
        throw translateException(getDescription(condition), statement, sqlException);
      }
      throw new DataRetrievalFailureException("Unable to scroll the query result", ex);
    }
  }

  private Number doQueryCount(EntityMetadata metadata, QueryCondition handler, List<Restriction> restrictions, Connection con) throws DataAccessException {
    var tableName = metadata.getTableName();
    StringBuilder countSql = new StringBuilder(restrictions.size() * 10 + 25 + tableName.getText().length());
    platform.selectCountFrom(countSql, tableName);

    Restrictions.append(platform, restrictions, countSql);

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

  private RuntimeException handleEntityException(String operation, EntityMetadata metadata,
          EntityOperationPhase phase, @Nullable String sql, Throwable ex) {
    String task = "%s [entity type: '%s', phase: %s]"
            .formatted(operation, metadata.getEntityClass().getName(), phase);
    if (ex instanceof SQLException sqlException) {
      return translateException(task, sql, sqlException);
    }
    if (ex instanceof RuntimeException runtimeException) {
      return runtimeException;
    }
    if (ex instanceof Error error) {
      throw error;
    }
    throw new PersistenceException(task, ex);
  }

  private DataAccessException handleBatchFailure(String task,
          Map<BatchKey, PreparedBatch> statements, JdbcConnection transaction, Throwable ex) {
    closeBatchStatements(statements, ex);
    rollbackAfterFailure(transaction, ex);
    if (ex instanceof DataAccessException dae) {
      return dae;
    }
    if (ex instanceof SQLException sqlException) {
      return translateException(task, null, sqlException);
    }
    if (ex instanceof Error error) {
      throw error;
    }
    return new PersistenceException(task + " failed", ex);
  }

  /**
   * Select a property list in SQL parameter order after pre-persist processing.
   */
  private List<EntityProperty> selectInsertProperties(PropertyUpdateStrategy strategy, Object entity, EntityMetadata metadata) {
    EntityProperty[] entityProperties = metadata.getEntityProperties(true);
    var properties = new ArrayList<EntityProperty>(entityProperties.length);
    for (EntityProperty property : entityProperties) {
      if (property.isIdProperty() && metadata.isAutoGeneratedId()) {
        continue;
      }
      if ((property.isIdProperty() && metadata.getIdGeneration() != null)
              || strategy.shouldUpdate(entity, property)) {
        properties.add(property);
      }
    }

    return Collections.unmodifiableList(properties);
  }

  /**
   * Render SQL from the same property list used for parameter binding.
   */
  private String insertStatement(EntityMetadata entityMetadata, List<EntityProperty> properties) {
    Insert insert = new Insert(entityMetadata.getTableName());
    for (EntityProperty property : properties) {
      insert.addColumn(property.getColumnName());
    }
    return insert.toStatementString(platform);
  }

  private void closeResource(@Nullable Connection connection, @Nullable Statement stmt) {
    repositoryManager.closeResource(stmt, null, null);
    repositoryManager.releaseConnection(connection, dataSource, null, null);
  }

  private void closeResource(@Nullable Connection connection, @Nullable PreparedStatement statement, @Nullable ResultSet resultSet) {
    closeResource(connection, statement);
    repositoryManager.closeResource(resultSet, null, null);
  }

  private void closeBatchStatements(Map<BatchKey, PreparedBatch> statements, @Nullable Throwable exception) {
    for (PreparedBatch batch : statements.values()) {
      repositoryManager.closeResource(batch.stmt, batch.getStatement(), exception);
    }
    statements.clear();
  }

  private static void rollbackAfterFailure(JdbcConnection transaction, Throwable exception) {
    try {
      var status = transaction.getTransaction();
      if (status != null && !status.isCompleted()) {
        transaction.rollback(false);
      }
    }
    catch (Throwable rollbackFailure) {
      if (rollbackFailure != exception) {
        exception.addSuppressed(rollbackFailure);
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
      return ((Descriptive) handler).getDescription();
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

  private @Nullable Object incrementVersion(Object entity, EntityMetadata metadata) {
    EntityProperty versionProperty = metadata.getVersionProperty();
    Object oldVersion = null;
    if (versionProperty != null) {
      oldVersion = versionProperty.getValue(entity);
      versionProperty.setValue(entity, nextVersion(oldVersion));
    }
    return oldVersion;
  }

  private Object nextVersion(@Nullable Object currentVersion) {
    Assert.notNull(currentVersion, "Entity version not set");
    Object next = versionIncrementStrategy.nextVersion(currentVersion);
    if (next == null) {
      throw new IllegalArgumentException(
              "VersionIncrementStrategy returned null for current version: " + currentVersion);
    }
    return next;
  }

  static int setParameters(Object entity, List<EntityProperty> properties, PreparedStatement statement) throws SQLException {
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

  private String updateStatement(EntityMetadata metadata, List<EntityProperty> properties, EntityProperty idProperty) {
    EntityProperty versionProperty = metadata.getVersionProperty();

    Update updateStmt = new Update(metadata.getTableName());
    for (EntityProperty property : properties) {
      updateStmt.addAssignment(property.getColumnName());
    }
    updateStmt.addRestriction(idProperty.getColumnName());
    if (versionProperty != null) {
      updateStmt.addRestriction(versionProperty.getColumnName());
    }
    return updateStmt.toStatementString(platform);
  }

  private List<EntityProperty> selectUpdateProperties(Object entity, EntityMetadata metadata, PropertyUpdateStrategy strategy) {
    EntityProperty versionProperty = metadata.getVersionProperty();
    var properties = new ArrayList<EntityProperty>();
    for (EntityProperty property : metadata.getEntityProperties(false)) {
      if (property == versionProperty || strategy.shouldUpdate(entity, property)) {
        properties.add(property);
      }
    }

    if (properties.isEmpty()) {
      throw new InvalidDataAccessApiUsageException("Updating an entity, There is no update properties");
    }
    return Collections.unmodifiableList(properties);
  }

  private static int computeUpdateRows(int updateCount, int count) {
    return updateCount == Statement.SUCCESS_NO_INFO
            || count == Statement.SUCCESS_NO_INFO
            ? Statement.SUCCESS_NO_INFO
            : updateCount + count;
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

  private final class PreparedBatch extends BatchExecution {

    private final PreparedStatement stmt;

    private final BatchOperation operation;

    private int affectedRows = 0;

    PreparedBatch(Connection connection, String sql, EntityMetadata entityMetadata,
            List<EntityProperty> properties, boolean autoGenerateId, BatchOperation operation) throws SQLException {
      super(sql, entityMetadata, properties, autoGenerateId);
      this.stmt = prepareStatement(connection, sql, autoGenerateId);
      this.operation = operation;
    }

    @Override
    public BatchOperation getOperation() {
      return operation;
    }

    public void addBatchUpdate(Object entity) throws Throwable {
      entities.add(entity);
      setParameters(entity, properties, stmt);
      stmt.addBatch();
      if (maxBatchRecords > 0 && entities.size() >= maxBatchRecords) {
        executeBatch(stmt, true);
      }
    }

    public void addBatchUpdate(Object entity, EntityProperty idProperty, Object id, @Nullable Object oldVersion) throws Throwable {
      entities.add(entity);
      int index = setParameters(entity, properties, stmt);
      idProperty.setParameter(stmt, index, id);
      EntityProperty versionProperty = entityMetadata.getVersionProperty();
      if (versionProperty != null) {
        versionProperty.setParameter(stmt, index + 1, oldVersion);
      }
      stmt.addBatch();
      if (maxBatchRecords > 0 && entities.size() >= maxBatchRecords) {
        executeBatch(stmt, true);
      }
    }

    public int explicitExecuteBatch() throws Throwable {
      executeBatch(stmt, false);
      return affectedRows;
    }

    private void executeBatch(PreparedStatement statement, boolean implicitExecution) throws Throwable {
      if (entities.isEmpty()) {
        return;
      }
      Throwable exception = null;
      try {
        eventMulticaster.preProcessing(this, implicitExecution);
        int batchSize = entities.size();
        if (stmtLogger.isDebugEnabled()) {
          stmtLogger.logStatement(LogMessage.format("Executing batch size: {}", batchSize), this.statement);
        }
        int[] updateCounts = statement.executeBatch();
        assertUpdateCount(this.statement, updateCounts.length, batchSize);
        int count = computeUpdateStatus(updateCounts);

        if (autoGenerateId) {
          EntityProperty idProperty = entityMetadata.getIdProperty();
          if (idProperty != null) {
            ResultSet generatedKeys = statement.getGeneratedKeys();
            Throwable keyFailure = null;
            try {
              for (Object entity : entities) {
                try {
                  if (!generatedKeys.next()) {
                    throw new GeneratedKeysException("Missing generated key for batch entity");
                  }
                  idProperty.setProperty(entity, generatedKeys, 1);
                }
                catch (SQLException e) {
                  throw new GeneratedKeysException("Cannot get generated keys", e);
                }
              }
            }
            catch (RuntimeException | Error ex) {
              keyFailure = ex;
              throw ex;
            }
            finally {
              repositoryManager.closeResource(generatedKeys, this.statement, keyFailure);
            }
          }
        }

        this.affectedRows = computeUpdateRows(this.affectedRows, count);
        for (int i = 0; i < batchSize; i++) {
          Object entity = entities.get(i);
          if (operation == BatchOperation.UPDATE) {
            eventMulticaster.onPostUpdate(entity, entityMetadata, properties, updateCounts[i]);
          }
          else {
            eventMulticaster.onPostPersist(entity, entityMetadata, properties);
          }
        }
      }
      catch (Throwable e) {
        exception = e;
        throw e;
      }
      finally {
        postProcessing(implicitExecution, exception);
      }
    }

    private int computeUpdateStatus(int[] updateCounts) throws SQLException {
      EntityProperty versionProperty = operation == BatchOperation.UPDATE ? entityMetadata.getVersionProperty() : null;
      int count = 0;
      for (int updateCount : updateCounts) {
        if (updateCount < 0 && updateCount != Statement.SUCCESS_NO_INFO) {
          throw new SQLException((operation == BatchOperation.UPDATE
                  ? "Batch update failed with count " : "Batch execution failed with count ") + updateCount);
        }
        if (versionProperty != null) {
          if (updateCount == Statement.SUCCESS_NO_INFO) {
            throw new InvalidDataAccessApiUsageException(
                    "Cannot verify optimistic locking: JDBC driver returned SUCCESS_NO_INFO. "
                            + "Do not batch-update entities with @Version when exact update counts are unavailable; "
                            + "update them individually instead.");
          }
          if (updateCount != 1) {
            throw new OptimisticLockingFailureException(
                    "Batch update affected an unexpected number of rows: " + updateCount);
          }
        }

        count = computeUpdateRows(count, updateCount);
      }
      return count;
    }

    private void postProcessing(boolean implicitExecution, @Nullable Throwable exception) {
      try {
        eventMulticaster.postProcessing(this, implicitExecution, exception);
      }
      catch (Throwable ex) {
        if (exception == null) {
          throw ex;
        }
        if (exception != ex) {
          exception.addSuppressed(ex);
        }
      }
      finally {
        entities.clear();
      }

    }

    @Override
    public int getAffectedRows() {
      return affectedRows;
    }

  }

  private record BatchKey(Class<?> entityClass, List<EntityProperty> properties) {
  }

  private record KeysetSort(EntityProperty property, Order direction) {
  }

  private record KeysetOrder(List<KeysetSort> keys) {

    static KeysetOrder resolve(EntityMetadata metadata, OrderSpec orderSpec, @Nullable ScrollPosition position) {
      ArrayList<KeysetSort> keys = new ArrayList<>();
      if (!orderSpec.isEmpty()) {
        for (OrderSpec.Part part : orderSpec.parts()) {
          if (!(part instanceof OrderSpec.Item item)) {
            throw new IllegalArgumentException("Keyset pagination requires mapped sort columns, not raw SQL fragments");
          }
          EntityProperty property = metadata.findProperty(item.column());
          if (property == null) {
            throw new IllegalArgumentException("Unknown keyset sort property: " + item.column());
          }
          for (KeysetSort key : keys) {
            if (key.property() == property) {
              throw new IllegalArgumentException("Duplicate keyset sort property: " + item.column());
            }
          }
          keys.add(new KeysetSort(property, item.direction()));
        }
      }
      else if (position != null) {
        for (ScrollPosition.Entry entry : position.cursor()) {
          EntityProperty property = metadata.findProperty(entry.property());
          if (property == null) {
            throw new IllegalArgumentException("Unknown keyset sort property: " + entry.property());
          }
          for (KeysetSort key : keys) {
            if (key.property() == property) {
              throw new IllegalArgumentException("Duplicate keyset sort property: " + entry.property());
            }
          }
          keys.add(new KeysetSort(property, entry.direction()));
        }
      }
      EntityProperty idProperty = metadata.findIdProperty();
      if (idProperty != null && keys.stream().noneMatch(key -> key.property() == idProperty)) {
        keys.add(new KeysetSort(idProperty, keys.isEmpty() ? Order.ASC : keys.get(keys.size() - 1).direction()));
      }
      if (keys.isEmpty()) {
        throw new IllegalArgumentException("Keyset pagination requires an entity ID or explicit unique ordering");
      }
      return new KeysetOrder(Collections.unmodifiableList(keys));
    }

    void validateCursor(List<ScrollPosition.Entry> cursor) {
      if (cursor.size() != keys.size()) {
        throw new IllegalArgumentException("Scroll position does not match the query ordering: cursor must contain every keyset sort property");
      }
      for (int i = 0; i < keys.size(); i++) {
        KeysetSort key = keys.get(i);
        ScrollPosition.Entry entry = cursor.get(i);
        if (!key.property().getName().equals(entry.property())) {
          throw new IllegalArgumentException("Scroll position does not match the query ordering: expected keyset property '%s' at index %d, but got '%s'"
                  .formatted(key.property().getName(), i, entry.property()));
        }
        if (key.direction() != entry.direction()) {
          throw new IllegalArgumentException("Scroll position does not match the query ordering: expected keyset direction '%s' for property '%s', but got '%s'"
                  .formatted(key.direction(), key.property().getName(), entry.direction()));
        }
      }
    }

    Restriction afterCursor() {
      Restriction comparison = null;
      for (int i = keys.size() - 1; i >= 0; i--) {
        KeysetSort key = keys.get(i);
        var column = key.property().getColumnName();
        Restriction next = key.direction() == Order.ASC
                ? Restrictions.greaterThan(column) : Restrictions.lessThan(column);
        comparison = comparison == null ? next : Restrictions.or(next, Restrictions.and(Restrictions.equal(column), comparison));
      }
      return comparison;
    }

    void applyTo(SimpleSelect select) {
      for (KeysetSort key : keys) {
        select.orderBy().orderBy(key.property().getColumnName(), key.direction());
      }
    }

    void bindCursor(PreparedStatement stmt, int index, List<ScrollPosition.Entry> cursor) throws SQLException {
      for (int i = 0; i < keys.size(); i++) {
        EntityProperty property = keys.get(i).property();
        Object value = cursor.get(i).value();
        property.setParameter(stmt, index++, value);
        if (i < keys.size() - 1) {
          property.setParameter(stmt, index++, value);
        }
      }
    }

    void validateRow(Object row) {
      for (KeysetSort key : keys) {
        if (key.property().getValue(row) == null) {
          throw new IllegalArgumentException("Keyset columns must not contain null values");
        }
      }
    }

    ScrollPosition positionFrom(Object row) {
      ArrayList<ScrollPosition.Entry> cursor = new ArrayList<>(keys.size());
      for (KeysetSort key : this.keys) {
        Object value = key.property().getValue(row);
        cursor.add(new ScrollPosition.Entry(key.property().getName(), value, key.direction()));
      }
      return ScrollPosition.keyset(cursor);
    }

  }

  private static final class ListScroll<T> implements Scroll<T> {

    private final List<T> rows;

    private final boolean last;

    private final Function<T, ScrollPosition> positionExtractor;

    private ListScroll(List<T> rows, boolean last, Function<T, ScrollPosition> positionExtractor) {
      this.rows = rows;
      this.last = last;
      this.positionExtractor = positionExtractor;
    }

    @Override
    public List<T> rows() {
      return rows;
    }

    @Override
    public ScrollPosition positionAt(int index) {
      return positionExtractor.apply(rows.get(index));
    }

    @Override
    public boolean isLast() {
      return last;
    }

  }

}
