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

import cn.taketoday.jdbc.CannotGetJdbcConnectionException;
import cn.taketoday.jdbc.GeneratedKeysException;
import cn.taketoday.jdbc.PersistenceException;
import cn.taketoday.jdbc.RepositoryManager;
import cn.taketoday.jdbc.sql.dialect.MySQLDialect;
import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ExceptionUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/16 22:47
 */
public class EntityManager {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private SqlGenerator sqlGenerator = new SqlGenerator(new MySQLDialect());
  private EntityHolderFactory entityHolderFactory = new DefaultEntityHolderFactory();

  private final RepositoryManager repositoryManager;

  private int maxBatchRecords = 0;

  /**
   * a flag indicating whether auto-generated keys should be returned;
   */
  private boolean returnGeneratedKeys = true;

  public EntityManager(RepositoryManager repositoryManager) {
    this.repositoryManager = repositoryManager;
  }

  public void setEntityHolderFactory(EntityHolderFactory entityHolderFactory) {
    Assert.notNull(entityHolderFactory, "entityHolderFactory is required");
    this.entityHolderFactory = entityHolderFactory;
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

  /**
   * persist an entity to underlying repository
   *
   * @param entity entity instance
   * @throws IllegalArgumentException if the instance is not an entity
   */
  public void persist(Object entity) {
    persist(entity, returnGeneratedKeys);
  }

  /**
   * persist an entity to underlying repository
   *
   * @param entity entity instance
   * @param returnGeneratedKeys a flag indicating whether auto-generated keys should be returned;
   * @see PreparedStatement
   * @see Connection#prepareStatement(String, int)
   */
  public void persist(Object entity, boolean returnGeneratedKeys) {
    Class<?> entityClass = entity.getClass();
    EntityHolder entityHolder = entityHolderFactory.getEntityHolder(entityClass);
    String sql = sqlGenerator.generateInsert(entityHolder);

    if (logger.isDebugEnabled()) {
      logger.debug("Persist entity: {} using SQL: [{}] , generatedKeys={}", entity, sql, returnGeneratedKeys);
    }

    Connection connection = getConnection();
    PreparedStatement statement = prepareStatement(connection, sql, returnGeneratedKeys);

    try {
      setPersistParameter(entity, statement, entityHolder);

      // execute
      int updateCount = statement.executeUpdate();
      assertUpdateCount(updateCount, 1);

      if (returnGeneratedKeys) {
        try {
          ResultSet generatedKeys = statement.getGeneratedKeys();
          if (generatedKeys.next()) {
            entityHolder.idProperty.setProperty(entity, generatedKeys, 1);
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
      throw new PersistenceException("Error preparing statement - " + ex.getMessage(), ex);
    }
  }

  /**
   * persist entities to underlying repository
   *
   * @param entities entities instances
   */
  public void persist(Iterable<Object> entities) {
    persist(entities, returnGeneratedKeys);
  }

  public void persist(Iterable<Object> entities, boolean returnGeneratedKeys) {
    Connection connection = getConnection();
    int maxBatchRecords = getMaxBatchRecords();
    var statements = new HashMap<Class<?>, PreparedBatch>();

    for (Object entity : entities) {
      PreparedBatch batch = statements.computeIfAbsent(entity.getClass(), entityClass -> {
        EntityHolder entityHolder = entityHolderFactory.getEntityHolder(entityClass);
        String sql = sqlGenerator.generateInsert(entityHolder);
        return new PreparedBatch(prepareStatement(connection, sql, returnGeneratedKeys), entityHolder, returnGeneratedKeys);
      });

      batch.addBatchUpdate(entity, maxBatchRecords);
    }

    for (PreparedBatch preparedBatch : statements.values()) {
      preparedBatch.executeBatch(returnGeneratedKeys);
    }

  }

  private static void setPersistParameter(Object entity, PreparedStatement statement, EntityHolder entityHolder) throws SQLException {
    int idx = 1;
    for (EntityProperty property : entityHolder.entityProperties) {
      property.setTo(statement, idx++, entity);
    }
  }

  static class PreparedBatch {
    public int currentBatchRecords = 0;
    public final EntityHolder entityHolder;
    public final PreparedStatement statement;
    final ArrayList<Object> entities = new ArrayList<>();

    public final boolean returnGeneratedKeys;

    PreparedBatch(PreparedStatement statement, EntityHolder entityHolder, boolean returnGeneratedKeys) {
      this.statement = statement;
      this.entityHolder = entityHolder;
      this.returnGeneratedKeys = returnGeneratedKeys;
    }

    public void addBatchUpdate(Object entity, int maxBatchRecords) {
      entities.add(entity);
      PreparedStatement statement = this.statement;

      try {
        setPersistParameter(entity, statement, entityHolder);

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
      try {
        int[] updateCounts = statement.executeBatch();
        assertUpdateCount(updateCounts.length, entities.size());
        if (returnGeneratedKeys) {
          ResultSet generatedKeys = statement.getGeneratedKeys();
          for (Object entity : entities) {
            try {
              if (generatedKeys.next()) {
                entityHolder.idProperty.setProperty(entity, generatedKeys, 1);
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
  }

  /**
   * Merge the state of the given entity into underlying repository
   *
   * @param entity entity instance
   * @throws IllegalArgumentException if instance is not an
   * entity or is a removed entity
   */
  public void merge(Object entity) {

  }

  //

}
