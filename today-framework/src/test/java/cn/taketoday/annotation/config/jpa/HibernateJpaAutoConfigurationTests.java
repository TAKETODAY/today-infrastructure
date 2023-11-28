/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.annotation.config.jpa;

import com.zaxxer.hikari.HikariDataSource;

import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.transaction.jta.platform.internal.NoJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.sql.DataSource;

import cn.taketoday.annotation.config.TestAutoConfigurationPackage;
import cn.taketoday.annotation.config.jdbc.DataSourceTransactionManagerAutoConfiguration;
import cn.taketoday.annotation.config.jpa.mapping.NonAnnotatedEntity;
import cn.taketoday.annotation.config.jpa.test.City;
import cn.taketoday.annotation.config.transaction.jta.JtaAutoConfiguration;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.context.ApplicationListener;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.config.AutoConfigurations;
import cn.taketoday.context.event.ContextRefreshedEvent;
import cn.taketoday.framework.test.context.assertj.AssertableApplicationContext;
import cn.taketoday.framework.test.context.runner.ContextConsumer;
import cn.taketoday.orm.hibernate5.support.HibernateImplicitNamingStrategy;
import cn.taketoday.orm.hibernate5.support.HibernateJtaPlatform;
import cn.taketoday.orm.jpa.JpaTransactionManager;
import cn.taketoday.orm.jpa.JpaVendorAdapter;
import cn.taketoday.orm.jpa.LocalContainerEntityManagerFactoryBean;
import cn.taketoday.orm.jpa.support.EntityManagerFactoryBuilderCustomizer;
import cn.taketoday.orm.jpa.vendor.HibernateJpaVendorAdapter;
import cn.taketoday.scheduling.concurrent.ThreadPoolTaskExecutor;
import cn.taketoday.transaction.jta.JtaTransactionManager;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;
import jakarta.transaction.Synchronization;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HibernateJpaAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Kazuki Shimizu
 * @author Stephane Nicoll
 * @author Chris Bono
 */
class HibernateJpaAutoConfigurationTests extends AbstractJpaAutoConfigurationTests {

  HibernateJpaAutoConfigurationTests() {
    super(HibernateJpaAutoConfiguration.class);
  }

  @Test
  void hibernateDialectIsNotSetByDefault() {
    contextRunner().run(assertJpaVendorAdapter(
            (adapter) -> assertThat(adapter.getJpaPropertyMap()).doesNotContainKeys("hibernate.dialect")));
  }

  @Test
  void hibernateDialectIsSetWhenDatabaseIsSet() {
    contextRunner().withPropertyValues("jpa.database=H2")
            .run(assertJpaVendorAdapter((adapter) -> assertThat(adapter.getJpaPropertyMap())
                    .contains(entry("hibernate.dialect", H2Dialect.class.getName()))));
  }

  @Test
  void hibernateDialectIsSetWhenDatabasePlatformIsSet() {
    String databasePlatform = TestH2Dialect.class.getName();
    contextRunner().withPropertyValues("jpa.database-platform=" + databasePlatform)
            .run(assertJpaVendorAdapter((adapter) -> assertThat(adapter.getJpaPropertyMap())
                    .contains(entry("hibernate.dialect", databasePlatform))));
  }

  private ContextConsumer<AssertableApplicationContext> assertJpaVendorAdapter(
          Consumer<HibernateJpaVendorAdapter> adapter) {
    return (context) -> {
      assertThat(context).hasSingleBean(JpaVendorAdapter.class);
      assertThat(context).hasSingleBean(HibernateJpaVendorAdapter.class);
      adapter.accept(context.getBean(HibernateJpaVendorAdapter.class));
    };
  }

  @Test
  void jtaDefaultPlatform() {
    contextRunner().withUserConfiguration(JtaTransactionManagerConfiguration.class)
            .run(assertJtaPlatform(HibernateJtaPlatform.class));
  }

  @Test
  void jtaCustomPlatform() {
    contextRunner()
            .withPropertyValues(
                    "jpa.properties.hibernate.transaction.jta.platform:" + TestJtaPlatform.class.getName())
            .withConfiguration(AutoConfigurations.of(JtaAutoConfiguration.class))
            .run(assertJtaPlatform(TestJtaPlatform.class));
  }

  @Test
  void jtaNotUsedByTheApplication() {
    contextRunner().run(assertJtaPlatform(NoJtaPlatform.class));
  }

  private ContextConsumer<AssertableApplicationContext> assertJtaPlatform(Class<? extends JtaPlatform> expectedType) {
    return (context) -> {
      SessionFactoryImpl sessionFactory = context.getBean(LocalContainerEntityManagerFactoryBean.class)
              .getNativeEntityManagerFactory().unwrap(SessionFactoryImpl.class);
      assertThat(sessionFactory.getServiceRegistry().getService(JtaPlatform.class)).isInstanceOf(expectedType);
    };
  }

  @Test
  void jtaCustomTransactionManagerUsingProperties() {
    contextRunner().withPropertyValues("transaction.default-timeout:30",
            "transaction.rollback-on-commit-failure:true").run((context) -> {
      JpaTransactionManager transactionManager = context.getBean(JpaTransactionManager.class);
      assertThat(transactionManager.getDefaultTimeout()).isEqualTo(30);
      assertThat(transactionManager.isRollbackOnCommitFailure()).isTrue();
    });
  }

  @Test
  void autoConfigurationBacksOffWithSeveralDataSources() {
    contextRunner()
            .withConfiguration(AutoConfigurations.of(
                    DataSourceTransactionManagerAutoConfiguration.class,
                    JtaAutoConfiguration.class))
            .withUserConfiguration(TestTwoDataSourcesConfiguration.class).run((context) -> {
              assertThat(context).hasNotFailed();
              assertThat(context).doesNotHaveBean(EntityManagerFactory.class);
            });
  }

  @Test
  void providerDisablesAutoCommitIsConfigured() {
    contextRunner().withPropertyValues("datasource.type:" + HikariDataSource.class.getName(),
            "datasource.hikari.auto-commit:false").run((context) -> {
      Map<String, Object> jpaProperties = context.getBean(LocalContainerEntityManagerFactoryBean.class)
              .getJpaPropertyMap();
      assertThat(jpaProperties)
              .contains(entry("hibernate.connection.provider_disables_autocommit", "true"));
    });
  }

  @Test
  void providerDisablesAutoCommitIsNotConfiguredIfAutoCommitIsEnabled() {
    contextRunner().withPropertyValues("datasource.type:" + HikariDataSource.class.getName(),
            "datasource.hikari.auto-commit:true").run((context) -> {
      Map<String, Object> jpaProperties = context.getBean(LocalContainerEntityManagerFactoryBean.class)
              .getJpaPropertyMap();
      assertThat(jpaProperties).doesNotContainKeys("hibernate.connection.provider_disables_autocommit");
    });
  }

  @Test
  void providerDisablesAutoCommitIsNotConfiguredIfPropertyIsSet() {
    contextRunner()
            .withPropertyValues("datasource.type:" + HikariDataSource.class.getName(),
                    "datasource.hikari.auto-commit:false",
                    "jpa.properties.hibernate.connection.provider_disables_autocommit=false")
            .run((context) -> {
              Map<String, Object> jpaProperties = context.getBean(LocalContainerEntityManagerFactoryBean.class)
                      .getJpaPropertyMap();
              assertThat(jpaProperties)
                      .contains(entry("hibernate.connection.provider_disables_autocommit", "false"));
            });
  }

  @Test
  void providerDisablesAutoCommitIsNotConfiguredWithJta() {
    contextRunner().withUserConfiguration(JtaTransactionManagerConfiguration.class)
            .withPropertyValues("datasource.type:" + HikariDataSource.class.getName(),
                    "datasource.hikari.auto-commit:false")
            .run((context) -> {
              Map<String, Object> jpaProperties = context.getBean(LocalContainerEntityManagerFactoryBean.class)
                      .getJpaPropertyMap();
              assertThat(jpaProperties).doesNotContainKeys("hibernate.connection.provider_disables_autocommit");
            });
  }

  @Test
  void customResourceMapping() {
    contextRunner().withClassLoader(new HideDataScriptClassLoader())
            .withPropertyValues("sql.init.data-locations:classpath:/db/non-annotated-data.sql",
                    "jpa.mapping-resources=META-INF/mappings/non-annotated.xml",
                    "jpa.defer-datasource-initialization=true")
            .run((context) -> {
              EntityManager em = context.getBean(EntityManagerFactory.class).createEntityManager();
              EntityTransaction transaction = em.getTransaction();
              transaction.begin();
              Query nativeQuery = em.createNativeQuery("INSERT INTO NON_ANNOTATED (id, item) values (2000, 'Test')");
              nativeQuery.executeUpdate();
              transaction.commit();
              NonAnnotatedEntity found = em.find(NonAnnotatedEntity.class, 2000L);
              assertThat(found).isNotNull();
              assertThat(found.getItem()).isEqualTo("Test");
            });
  }

  @Test
  void physicalNamingStrategyCanBeUsed() {
    contextRunner().withUserConfiguration(TestPhysicalNamingStrategyConfiguration.class).run((context) -> {
      Map<String, Object> hibernateProperties = context.getBean(HibernateJpaConfiguration.class)
              .getVendorProperties();
      assertThat(hibernateProperties).contains(
              entry("hibernate.physical_naming_strategy", context.getBean("testPhysicalNamingStrategy")));
      assertThat(hibernateProperties).doesNotContainKeys("hibernate.ejb.naming_strategy");
    });
  }

  @Test
  void implicitNamingStrategyCanBeUsed() {
    contextRunner().withUserConfiguration(TestImplicitNamingStrategyConfiguration.class).run((context) -> {
      Map<String, Object> hibernateProperties = context.getBean(HibernateJpaConfiguration.class)
              .getVendorProperties();
      assertThat(hibernateProperties).contains(
              entry("hibernate.implicit_naming_strategy", context.getBean("testImplicitNamingStrategy")));
      assertThat(hibernateProperties).doesNotContainKeys("hibernate.ejb.naming_strategy");
    });
  }

  @Test
  void namingStrategyInstancesTakePrecedenceOverNamingStrategyProperties() {
    contextRunner()
            .withUserConfiguration(TestPhysicalNamingStrategyConfiguration.class,
                    TestImplicitNamingStrategyConfiguration.class)
            .withPropertyValues("jpa.hibernate.naming.physical-strategy:com.example.Physical",
                    "jpa.hibernate.naming.implicit-strategy:com.example.Implicit")
            .run((context) -> {
              Map<String, Object> hibernateProperties = context.getBean(HibernateJpaConfiguration.class)
                      .getVendorProperties();
              assertThat(hibernateProperties).contains(
                      entry("hibernate.physical_naming_strategy", context.getBean("testPhysicalNamingStrategy")),
                      entry("hibernate.implicit_naming_strategy", context.getBean("testImplicitNamingStrategy")));
              assertThat(hibernateProperties).doesNotContainKeys("hibernate.ejb.naming_strategy");
            });
  }

  @Test
  void hibernatePropertiesCustomizerTakesPrecedenceOverStrategyInstancesAndNamingStrategyProperties() {
    contextRunner()
            .withUserConfiguration(TestHibernatePropertiesCustomizerConfiguration.class,
                    TestPhysicalNamingStrategyConfiguration.class, TestImplicitNamingStrategyConfiguration.class)
            .withPropertyValues("jpa.hibernate.naming.physical-strategy:com.example.Physical",
                    "jpa.hibernate.naming.implicit-strategy:com.example.Implicit")
            .run((context) -> {
              Map<String, Object> hibernateProperties = context.getBean(HibernateJpaConfiguration.class)
                      .getVendorProperties();
              TestHibernatePropertiesCustomizerConfiguration configuration = context
                      .getBean(TestHibernatePropertiesCustomizerConfiguration.class);
              assertThat(hibernateProperties).contains(
                      entry("hibernate.physical_naming_strategy", configuration.physicalNamingStrategy),
                      entry("hibernate.implicit_naming_strategy", configuration.implicitNamingStrategy));
              assertThat(hibernateProperties).doesNotContainKeys("hibernate.ejb.naming_strategy");
            });
  }

  @Test
  void eventListenerCanBeRegisteredAsBeans() {
    contextRunner().withUserConfiguration(TestInitializedJpaConfiguration.class)
            .withClassLoader(new HideDataScriptClassLoader())
            .withPropertyValues("jpa.show-sql=true", "jpa.hibernate.ddl-auto:create-drop",
                    "sql.init.data-locations:classpath:/city.sql",
                    "jpa.defer-datasource-initialization=true")
            .run((context) -> {
              // See CityListener
              assertThat(context).hasSingleBean(City.class);
              assertThat(context.getBean(City.class).getName()).isEqualTo("Washington");
            });
  }

  @Test
  void hibernatePropertiesCustomizerCanDisableBeanContainer() {
    contextRunner().withUserConfiguration(DisableBeanContainerConfiguration.class)
            .run((context) -> assertThat(context).doesNotHaveBean(City.class));
  }

  @Test
  void vendorPropertiesWithEmbeddedDatabaseAndNoDdlProperty() {
    contextRunner().run(vendorProperties((vendorProperties) -> {
      assertThat(vendorProperties).doesNotContainKeys(AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION);
      assertThat(vendorProperties.get(AvailableSettings.HBM2DDL_AUTO)).isEqualTo("create-drop");
    }));
  }

  @Test
  void vendorPropertiesWhenDdlAutoPropertyIsSet() {
    contextRunner().withPropertyValues("jpa.hibernate.ddl-auto=update")
            .run(vendorProperties((vendorProperties) -> {
              assertThat(vendorProperties).doesNotContainKeys(AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION);
              assertThat(vendorProperties.get(AvailableSettings.HBM2DDL_AUTO)).isEqualTo("update");
            }));
  }

  @Test
  void vendorPropertiesWhenDdlAutoPropertyAndHibernatePropertiesAreSet() {
    contextRunner()
            .withPropertyValues("jpa.hibernate.ddl-auto=update",
                    "jpa.properties.hibernate.hbm2ddl.auto=create-drop")
            .run(vendorProperties((vendorProperties) -> {
              assertThat(vendorProperties).doesNotContainKeys(AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION);
              assertThat(vendorProperties.get(AvailableSettings.HBM2DDL_AUTO)).isEqualTo("create-drop");
            }));
  }

  @Test
  void vendorPropertiesWhenDdlAutoPropertyIsSetToNone() {
    contextRunner().withPropertyValues("jpa.hibernate.ddl-auto=none")
            .run(vendorProperties((vendorProperties) -> assertThat(vendorProperties).doesNotContainKeys(
                    AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, AvailableSettings.HBM2DDL_AUTO)));
  }

  @Test
  void vendorPropertiesWhenJpaDdlActionIsSet() {
    contextRunner()
            .withPropertyValues(
                    "jpa.properties.jakarta.persistence.schema-generation.database.action=create")
            .run(vendorProperties((vendorProperties) -> {
              assertThat(vendorProperties.get(AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION))
                      .isEqualTo("create");
              assertThat(vendorProperties).doesNotContainKeys(AvailableSettings.HBM2DDL_AUTO);
            }));
  }

  @Test
  void vendorPropertiesWhenBothDdlAutoPropertiesAreSet() {
    contextRunner().withPropertyValues(
            "jpa.properties.jakarta.persistence.schema-generation.database.action=create",
            "jpa.hibernate.ddl-auto=create-only").run(vendorProperties((vendorProperties) -> {
      assertThat(vendorProperties.get(AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION))
              .isEqualTo("create");
      assertThat(vendorProperties.get(AvailableSettings.HBM2DDL_AUTO)).isEqualTo("create-only");
    }));
  }

  private ContextConsumer<AssertableApplicationContext> vendorProperties(
          Consumer<Map<String, Object>> vendorProperties) {
    return (context) -> vendorProperties
            .accept(context.getBean(HibernateJpaConfiguration.class).getVendorProperties());
  }

  @Test
  void withSyncBootstrappingAnApplicationListenerThatUsesJpaDoesNotTriggerABeanCurrentlyInCreationException() {
    contextRunner().withUserConfiguration(JpaUsingApplicationListenerConfiguration.class).run((context) -> {
      assertThat(context).hasNotFailed();
      JpaUsingApplicationListenerConfiguration.EventCapturingApplicationListener listener = context.getBean(JpaUsingApplicationListenerConfiguration.EventCapturingApplicationListener.class);
      assertThat(listener.events).hasSize(1);
      assertThat(listener.events).hasOnlyElementsOfType(ContextRefreshedEvent.class);
    });
  }

  @Test
  void withAsyncBootstrappingAnApplicationListenerThatUsesJpaDoesNotTriggerABeanCurrentlyInCreationException() {
    contextRunner().withUserConfiguration(AsyncBootstrappingConfiguration.class,
            JpaUsingApplicationListenerConfiguration.class).run((context) -> {
      assertThat(context).hasNotFailed();
      JpaUsingApplicationListenerConfiguration.EventCapturingApplicationListener listener = context
              .getBean(JpaUsingApplicationListenerConfiguration.EventCapturingApplicationListener.class);
      assertThat(listener.events).hasSize(1);
      assertThat(listener.events).hasOnlyElementsOfType(ContextRefreshedEvent.class);
      // createEntityManager requires Hibernate bootstrapping to be complete
      assertThatNoException()
              .isThrownBy(() -> context.getBean(EntityManagerFactory.class).createEntityManager());
    });
  }

  @Test
  void whenLocalContainerEntityManagerFactoryBeanHasNoJpaVendorAdapterAutoConfigurationSucceeds() {
    contextRunner()
            .withUserConfiguration(
                    TestConfigurationWithLocalContainerEntityManagerFactoryBeanWithNoJpaVendorAdapter.class)
            .run((context) -> {
              EntityManagerFactory factoryBean = context.getBean(EntityManagerFactory.class);
              Map<String, Object> map = factoryBean.getProperties();
              assertThat(map.get("configured")).isEqualTo("manually");
            });
  }

  @Configuration(proxyBeanMethods = false)
  @TestAutoConfigurationPackage(City.class)
//  @DependsOnDatabaseInitialization
  static class TestInitializedJpaConfiguration {

    private boolean called;

    @Autowired
    void validateDataSourceIsInitialized(EntityManagerFactory entityManagerFactory) {
      // Inject the entity manager to validate it is initialized at the injection
      // point
      EntityManager entityManager = entityManagerFactory.createEntityManager();
      EntityTransaction transaction = entityManager.getTransaction();
      transaction.begin();

      Query nativeQuery = entityManager.createNativeQuery("INSERT INTO CITY (ID, NAME, STATE, COUNTRY, MAP) values (2000, 'Washington', 'DC', 'US', 'Google')");
      nativeQuery.executeUpdate();

      transaction.commit();
      City city = entityManager.find(City.class, 2000L);
      assertThat(city).isNotNull();
      assertThat(city.getName()).isEqualTo("Washington");
      this.called = true;
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class TestImplicitNamingStrategyConfiguration {

    @Bean
    ImplicitNamingStrategy testImplicitNamingStrategy() {
      return new HibernateImplicitNamingStrategy();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class TestPhysicalNamingStrategyConfiguration {

    @Bean
    PhysicalNamingStrategy testPhysicalNamingStrategy() {
      return new CamelCaseToUnderscoresNamingStrategy();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class TestHibernatePropertiesCustomizerConfiguration {

    private final PhysicalNamingStrategy physicalNamingStrategy = new CamelCaseToUnderscoresNamingStrategy();

    private final ImplicitNamingStrategy implicitNamingStrategy = new HibernateImplicitNamingStrategy();

    @Bean
    HibernatePropertiesCustomizer testHibernatePropertiesCustomizer() {
      return (hibernateProperties) -> {
        hibernateProperties.put("hibernate.physical_naming_strategy", this.physicalNamingStrategy);
        hibernateProperties.put("hibernate.implicit_naming_strategy", this.implicitNamingStrategy);
      };
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class DisableBeanContainerConfiguration {

    @Bean
    HibernatePropertiesCustomizer disableBeanContainerHibernatePropertiesCustomizer() {
      return (hibernateProperties) -> hibernateProperties.remove(AvailableSettings.BEAN_CONTAINER);
    }

  }

  @SuppressWarnings("serial")
  public static class TestJtaPlatform implements JtaPlatform {

    @Override
    public TransactionManager retrieveTransactionManager() {
      return mock(TransactionManager.class);
    }

    @Override
    public UserTransaction retrieveUserTransaction() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Object getTransactionIdentifier(Transaction transaction) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean canRegisterSynchronization() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void registerSynchronization(Synchronization synchronization) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int getCurrentStatus() {
      throw new UnsupportedOperationException();
    }

  }

  static class HideDataScriptClassLoader extends URLClassLoader {

    private static final List<String> HIDDEN_RESOURCES = Arrays.asList("schema-all.sql", "schema.sql");

    HideDataScriptClassLoader() {
      super(new URL[0], HideDataScriptClassLoader.class.getClassLoader());
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
      if (HIDDEN_RESOURCES.contains(name)) {
        return Collections.emptyEnumeration();
      }
      return super.getResources(name);
    }

  }

  @cn.taketoday.context.annotation.Configuration(proxyBeanMethods = false)
  static class JpaUsingApplicationListenerConfiguration {

    @Bean
    EventCapturingApplicationListener jpaUsingApplicationListener(EntityManagerFactory emf) {
      return new EventCapturingApplicationListener();
    }

    static class EventCapturingApplicationListener implements ApplicationListener<ApplicationEvent> {

      private final List<ApplicationEvent> events = new ArrayList<>();

      @Override
      public void onApplicationEvent(ApplicationEvent event) {
        this.events.add(event);
      }

    }

  }

  @Configuration(proxyBeanMethods = false)
  static class AsyncBootstrappingConfiguration {

    @Bean
    ThreadPoolTaskExecutor ThreadPoolTaskExecutor() {
      return new ThreadPoolTaskExecutor();
    }

    @Bean
    EntityManagerFactoryBuilderCustomizer asyncBootstrappingCustomizer(ThreadPoolTaskExecutor executor) {
      return (builder) -> builder.setBootstrapExecutor(executor);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class TestConfigurationWithLocalContainerEntityManagerFactoryBeanWithNoJpaVendorAdapter
          extends TestConfiguration {

    @Bean
    LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
      LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
      factoryBean.setDataSource(dataSource);
      factoryBean.setPersistenceUnitName("manually-configured");
      factoryBean.setPersistenceProviderClass(HibernatePersistenceProvider.class);
      Map<String, Object> properties = new HashMap<>();
      properties.put("configured", "manually");
      properties.put("hibernate.transaction.jta.platform", NoJtaPlatform.INSTANCE);
      factoryBean.setJpaPropertyMap(properties);
      return factoryBean;
    }

  }

  public static class TestH2Dialect extends H2Dialect {

  }

  @Configuration(proxyBeanMethods = false)
  static class JtaTransactionManagerConfiguration {

    @Bean
    JtaTransactionManager jtaTransactionManager() {
      JtaTransactionManager jtaTransactionManager = new JtaTransactionManager();
      jtaTransactionManager.setUserTransaction(mock(UserTransaction.class));
      jtaTransactionManager.setTransactionManager(mock(TransactionManager.class));
      return jtaTransactionManager;
    }

  }

}
