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

package cn.taketoday.orm.jpa.vendor;

import java.util.Collections;
import java.util.Map;

import cn.taketoday.lang.Nullable;
import cn.taketoday.orm.jpa.JpaDialect;
import cn.taketoday.orm.jpa.JpaVendorAdapter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.spi.PersistenceUnitInfo;

/**
 * Abstract {@link JpaVendorAdapter} implementation that defines common properties,
 * to be translated into vendor-specific JPA properties by concrete subclasses.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @since 4.0
 */
public abstract class AbstractJpaVendorAdapter implements JpaVendorAdapter {

  private Database database = Database.DEFAULT;

  @Nullable
  private String databasePlatform;

  private boolean generateDdl = false;

  private boolean showSql = false;

  /**
   * Specify the target database to operate on, as a value of the {@code Database} enum:
   * DB2, DERBY, H2, HANA, HSQL, INFORMIX, MYSQL, ORACLE, POSTGRESQL, SQL_SERVER, SYBASE
   * <p><b>NOTE:</b> This setting will override your JPA provider's default algorithm.
   * Custom vendor properties may still fine-tune the database dialect. However,
   * there may nevertheless be conflicts: For example, specify either this setting
   * or Hibernate's "hibernate.dialect_resolvers" property, not both.
   */
  public void setDatabase(Database database) {
    this.database = database;
  }

  /**
   * Return the target database to operate on.
   */
  protected Database getDatabase() {
    return this.database;
  }

  /**
   * Specify the name of the target database to operate on.
   * The supported values are vendor-dependent platform identifiers.
   */
  public void setDatabasePlatform(@Nullable String databasePlatform) {
    this.databasePlatform = databasePlatform;
  }

  /**
   * Return the name of the target database to operate on.
   */
  @Nullable
  protected String getDatabasePlatform() {
    return this.databasePlatform;
  }

  /**
   * Set whether to generate DDL after the EntityManagerFactory has been initialized,
   * creating/updating all relevant tables.
   * <p>Note that the exact semantics of this flag depend on the underlying
   * persistence provider. For any more advanced needs, specify the appropriate
   * vendor-specific settings as "jpaProperties".
   * <p><b>NOTE: Do not set this flag to 'true' while also setting JPA's
   * {@code jakarta.persistence.schema-generation.database.action} property.</b>
   * These two schema generation mechanisms - standard JPA versus provider-native -
   * are mutually exclusive, e.g. with Hibernate 5.
   *
   * @see cn.taketoday.orm.jpa.AbstractEntityManagerFactoryBean#setJpaProperties
   */
  public void setGenerateDdl(boolean generateDdl) {
    this.generateDdl = generateDdl;
  }

  /**
   * Return whether to generate DDL after the EntityManagerFactory has been initialized
   * creating/updating all relevant tables.
   */
  protected boolean isGenerateDdl() {
    return this.generateDdl;
  }

  /**
   * Set whether to show SQL in the log (or in the console).
   * <p>For more specific logging configuration, specify the appropriate
   * vendor-specific settings as "jpaProperties".
   *
   * @see cn.taketoday.orm.jpa.AbstractEntityManagerFactoryBean#setJpaProperties
   */
  public void setShowSql(boolean showSql) {
    this.showSql = showSql;
  }

  /**
   * Return whether to show SQL in the log (or in the console).
   */
  protected boolean isShowSql() {
    return this.showSql;
  }

  @Override
  @Nullable
  public String getPersistenceProviderRootPackage() {
    return null;
  }

  @Override
  public Map<String, ?> getJpaPropertyMap(PersistenceUnitInfo pui) {
    return getJpaPropertyMap();
  }

  @Override
  public Map<String, ?> getJpaPropertyMap() {
    return Collections.emptyMap();
  }

  @Override
  @Nullable
  public JpaDialect getJpaDialect() {
    return null;
  }

  @Override
  public Class<? extends EntityManagerFactory> getEntityManagerFactoryInterface() {
    return EntityManagerFactory.class;
  }

  @Override
  public Class<? extends EntityManager> getEntityManagerInterface() {
    return EntityManager.class;
  }

  @Override
  public void postProcessEntityManagerFactory(EntityManagerFactory emf) {
  }

  @Override
  public void postProcessEntityManager(EntityManager em) {
  }

}
