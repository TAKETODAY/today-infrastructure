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

package cn.taketoday.orm.jpa;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import javax.sql.DataSource;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.core.io.Resource;
import cn.taketoday.dao.DataAccessException;
import cn.taketoday.jdbc.core.JdbcTemplate;
import cn.taketoday.jdbc.datasource.init.ResourceDatabasePopulator;
import cn.taketoday.transaction.PlatformTransactionManager;
import cn.taketoday.transaction.TransactionException;
import cn.taketoday.transaction.TransactionStatus;
import cn.taketoday.transaction.support.DefaultTransactionDefinition;
import cn.taketoday.transaction.support.TransactionSynchronizationManager;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public abstract class AbstractEntityManagerFactoryIntegrationTests {

  protected static final String[] ECLIPSELINK_CONFIG_LOCATIONS = new String[] {
          "/cn/taketoday/orm/jpa/eclipselink/eclipselink-manager.xml",
          "/cn/taketoday/orm/jpa/memdb.xml", "/cn/taketoday/orm/jpa/inject.xml"
  };

  private static ConfigurableApplicationContext applicationContext;

  protected EntityManagerFactory entityManagerFactory;

  protected EntityManager sharedEntityManager;

  protected PlatformTransactionManager transactionManager;

  protected DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();

  protected TransactionStatus transactionStatus;

  private boolean complete = false;

  protected JdbcTemplate jdbcTemplate;

  private boolean zappedTables = false;

  @Autowired
  public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
    this.entityManagerFactory = entityManagerFactory;
    this.sharedEntityManager = SharedEntityManagerCreator.createSharedEntityManager(this.entityManagerFactory);
  }

  @Autowired
  public void setTransactionManager(PlatformTransactionManager transactionManager) {
    this.transactionManager = transactionManager;
  }

  @Autowired
  public void setDataSource(DataSource dataSource) {
    this.jdbcTemplate = new JdbcTemplate(dataSource);
  }

  @BeforeEach
  public void setup() {
    if (applicationContext == null) {
      applicationContext = new ClassPathXmlApplicationContext(getConfigLocations());
    }
    applicationContext.getAutowireCapableBeanFactory().autowireBean(this);

    if (this.transactionManager != null && this.transactionDefinition != null) {
      startNewTransaction();
    }
  }

  protected String[] getConfigLocations() {
    return ECLIPSELINK_CONFIG_LOCATIONS;
  }

  @AfterEach
  public void cleanup() {
    if (this.transactionStatus != null && !this.transactionStatus.isCompleted()) {
      endTransaction();
    }

    assertThat(TransactionSynchronizationManager.getResourceMap().isEmpty()).isTrue();
    assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
    assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
  }

  @AfterAll
  public static void closeContext() {
    if (applicationContext != null) {
      applicationContext.close();
      applicationContext = null;
    }
  }

  protected EntityManager createContainerManagedEntityManager() {
    return ExtendedEntityManagerCreator.createContainerManagedEntityManager(this.entityManagerFactory);
  }

  protected void setComplete() {
    if (this.transactionManager == null) {
      throw new IllegalStateException("No transaction manager set");
    }
    if (this.zappedTables) {
      throw new IllegalStateException("Cannot set complete after deleting tables");
    }
    this.complete = true;
  }

  protected void endTransaction() {
    final boolean commit = this.complete;
    if (this.transactionStatus != null) {
      try {
        if (commit) {
          this.transactionManager.commit(this.transactionStatus);
        }
        else {
          this.transactionManager.rollback(this.transactionStatus);
        }
      }
      finally {
        this.transactionStatus = null;
      }
    }
  }

  protected void startNewTransaction() throws TransactionException {
    this.transactionStatus = this.transactionManager.getTransaction(this.transactionDefinition);
  }

  protected void deleteFromTables(String... tableNames) {
    for (String tableName : tableNames) {
      this.jdbcTemplate.update("DELETE FROM " + tableName);
    }
    this.zappedTables = true;
  }

  protected int countRowsInTable(EntityManager em, String tableName) {
    Query query = em.createNativeQuery("SELECT COUNT(0) FROM " + tableName);
    return ((Number) query.getSingleResult()).intValue();
  }

  protected int countRowsInTable(String tableName) {
    return this.jdbcTemplate.queryForObject("SELECT COUNT(0) FROM " + tableName, Integer.class);
  }

  protected void executeSqlScript(String sqlResourcePath) throws DataAccessException {
    Resource resource = applicationContext.getResource(sqlResourcePath);
    new ResourceDatabasePopulator(resource).execute(this.jdbcTemplate.getDataSource());
  }

}
