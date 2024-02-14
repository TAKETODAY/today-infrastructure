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

package cn.taketoday.orm.jpa.vendor;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DerbyTenSevenDialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HANAColumnStoreDialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.Informix10Dialect;
import org.hibernate.dialect.MySQL57Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.Oracle12cDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQL95Dialect;
import org.hibernate.dialect.SQLServer2012Dialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;

import java.util.HashMap;
import java.util.Map;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.spi.PersistenceProvider;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.PersistenceUnitTransactionType;

/**
 * {@link cn.taketoday.orm.jpa.JpaVendorAdapter} implementation for Hibernate
 * Compatible with Hibernate ORM 5.5/5.6 as well as 6.0/6.1.
 *
 * <p>Exposes Hibernate's persistence provider and Hibernate's Session as extended
 * EntityManager interface, and adapts {@link AbstractJpaVendorAdapter}'s common
 * configuration settings. Also supports the detection of annotated packages (through
 * {@link cn.taketoday.orm.jpa.persistenceunit.SmartPersistenceUnitInfo#getManagedPackages()}),
 * e.g. containing Hibernate {@link org.hibernate.annotations.FilterDef} annotations,
 * along with Framework-driven entity scanning which requires no {@code persistence.xml}
 * ({@link cn.taketoday.orm.jpa.LocalContainerEntityManagerFactoryBean#setPackagesToScan}).
 *
 * <p><b>A note about {@code HibernateJpaVendorAdapter} vs native Hibernate settings:</b>
 * Some settings on this adapter may conflict with native Hibernate configuration rules
 * or custom Hibernate properties. For example, specify either {@link #setDatabase} or
 * Hibernate's "hibernate.dialect_resolvers" property, not both. Also, be careful about
 * Hibernate's connection release mode: This adapter prefers {@code ON_CLOSE} behavior,
 * aligned with {@link HibernateJpaDialect#setPrepareConnection}, at least for non-JTA
 * scenarios; you may override this through corresponding native Hibernate properties.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see HibernateJpaDialect
 * @since 4.0
 */
public class HibernateJpaVendorAdapter extends AbstractJpaVendorAdapter {

  private static final boolean oldDialectsPresent = ClassUtils.isPresent(
          "org.hibernate.dialect.PostgreSQL95Dialect", HibernateJpaVendorAdapter.class.getClassLoader());

  private final HibernateJpaDialect jpaDialect = new HibernateJpaDialect();

  private final PersistenceProvider persistenceProvider;

  private final Class<? extends EntityManagerFactory> entityManagerFactoryInterface;

  private final Class<? extends EntityManager> entityManagerInterface;

  public HibernateJpaVendorAdapter() {
    this.persistenceProvider = new HibernateJpaPersistenceProvider();
    this.entityManagerFactoryInterface = SessionFactory.class;
    this.entityManagerInterface = Session.class;
  }

  /**
   * Set whether to prepare the underlying JDBC Connection of a transactional
   * Hibernate Session, that is, whether to apply a transaction-specific
   * isolation level and/or the transaction's read-only flag to the underlying
   * JDBC Connection.
   * <p>See {@link HibernateJpaDialect#setPrepareConnection(boolean)} for details.
   * This is just a convenience flag passed through to {@code HibernateJpaDialect}.
   * <p>On Hibernate 5.1+, this flag remains {@code true} by default like against
   * previous Hibernate versions. The vendor adapter manually enforces Hibernate's
   * new connection handling mode {@code DELAYED_ACQUISITION_AND_HOLD} in that case
   * unless a user-specified connection handling mode property indicates otherwise;
   * switch this flag to {@code false} to avoid that interference.
   * <p><b>NOTE: For a persistence unit with transaction type JTA e.g. on WebLogic,
   * the connection release mode will never be altered from its provider default,
   * i.e. not be forced to {@code DELAYED_ACQUISITION_AND_HOLD} by this flag.</b>
   * Alternatively, set Hibernate's "hibernate.connection.handling_mode"
   * property to "DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION" or even
   * "DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT" in such a scenario.
   *
   * @see PersistenceUnitInfo#getTransactionType()
   * @see #getJpaPropertyMap(PersistenceUnitInfo)
   * @see HibernateJpaDialect#beginTransaction
   */
  public void setPrepareConnection(boolean prepareConnection) {
    this.jpaDialect.setPrepareConnection(prepareConnection);
  }

  @Override
  public PersistenceProvider getPersistenceProvider() {
    return this.persistenceProvider;
  }

  @Override
  public String getPersistenceProviderRootPackage() {
    return "org.hibernate";
  }

  @Override
  public Map<String, Object> getJpaPropertyMap(PersistenceUnitInfo pui) {
    return buildJpaPropertyMap(this.jpaDialect.prepareConnection &&
            pui.getTransactionType() != PersistenceUnitTransactionType.JTA);
  }

  @Override
  public Map<String, Object> getJpaPropertyMap() {
    return buildJpaPropertyMap(this.jpaDialect.prepareConnection);
  }

  private Map<String, Object> buildJpaPropertyMap(boolean connectionReleaseOnClose) {
    Map<String, Object> jpaProperties = new HashMap<>();

    if (getDatabasePlatform() != null) {
      jpaProperties.put(AvailableSettings.DIALECT, getDatabasePlatform());
    }
    else {
      Class<?> databaseDialectClass = determineDatabaseDialectClass(getDatabase());
      if (databaseDialectClass != null) {
        jpaProperties.put(AvailableSettings.DIALECT, databaseDialectClass.getName());
      }
    }

    if (isGenerateDdl()) {
      jpaProperties.put(AvailableSettings.HBM2DDL_AUTO, "update");
    }
    if (isShowSql()) {
      jpaProperties.put(AvailableSettings.SHOW_SQL, "true");
    }

    if (connectionReleaseOnClose) {
      jpaProperties.put(AvailableSettings.CONNECTION_HANDLING,
              PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_HOLD);
    }

    // For HibernateBeanContainer to be called on Hibernate 6.2
    jpaProperties.put("hibernate.cdi.extensions", "true");

    return jpaProperties;
  }

  /**
   * Determine the Hibernate database dialect class for the given target database.
   *
   * @param database the target database
   * @return the Hibernate database dialect class, or {@code null} if none found
   */
  @Nullable
  @SuppressWarnings("deprecation")  // for DerbyDialect and PostgreSQLDialect on Hibernate 6.2
  protected Class<?> determineDatabaseDialectClass(Database database) {
    if (oldDialectsPresent) {  // Hibernate <6.2
      return switch (database) {
        case DB2 -> DB2Dialect.class;
        case DERBY -> DerbyTenSevenDialect.class;
        case H2 -> H2Dialect.class;
        case HANA -> HANAColumnStoreDialect.class;
        case HSQL -> HSQLDialect.class;
        case INFORMIX -> Informix10Dialect.class;
        case MYSQL -> MySQL57Dialect.class;
        case ORACLE -> Oracle12cDialect.class;
        case POSTGRESQL -> PostgreSQL95Dialect.class;
        case SQL_SERVER -> SQLServer2012Dialect.class;
        case SYBASE -> SybaseDialect.class;
        default -> null;
      };
    }
    else {  // Hibernate 6.2+ aligned
      return switch (database) {
        case DB2 -> DB2Dialect.class;
        case DERBY -> org.hibernate.dialect.DerbyDialect.class;
        case H2 -> H2Dialect.class;
        case HANA -> HANAColumnStoreDialect.class;
        case HSQL -> HSQLDialect.class;
        case MYSQL -> MySQLDialect.class;
        case ORACLE -> OracleDialect.class;
        case POSTGRESQL -> org.hibernate.dialect.PostgreSQLDialect.class;
        case SQL_SERVER -> SQLServerDialect.class;
        case SYBASE -> SybaseDialect.class;
        default -> null;
      };
    }
  }

  @Override
  public HibernateJpaDialect getJpaDialect() {
    return this.jpaDialect;
  }

  @Override
  public Class<? extends EntityManagerFactory> getEntityManagerFactoryInterface() {
    return this.entityManagerFactoryInterface;
  }

  @Override
  public Class<? extends EntityManager> getEntityManagerInterface() {
    return this.entityManagerInterface;
  }

}
