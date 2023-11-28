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

package cn.taketoday.annotation.config.jpa;

import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;

import javax.sql.DataSource;

import cn.taketoday.annotation.config.TestAutoConfigurationPackage;
import cn.taketoday.annotation.config.jdbc.DataSourceAutoConfiguration;
import cn.taketoday.annotation.config.jpa.test.City;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.config.AutoConfigurations;
import cn.taketoday.core.annotation.Order;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;
import cn.taketoday.orm.jpa.vendor.Database;
import cn.taketoday.orm.jpa.vendor.HibernateJpaVendorAdapter;
import cn.taketoday.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Additional tests for {@link HibernateJpaAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 */
class CustomHibernateJpaAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withPropertyValues("datasource.generate-unique-name=true")
          .withUserConfiguration(TestConfiguration.class).withConfiguration(
                  AutoConfigurations.of(DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class));

  @Test
  void namingStrategyDelegatorTakesPrecedence() {
    this.contextRunner.withPropertyValues("spring.jpa.properties.hibernate.ejb.naming_strategy_delegator:"
            + "org.hibernate.cfg.naming.ImprovedNamingStrategyDelegator").run((context) -> {
      JpaProperties jpaProperties = context.getBean(JpaProperties.class);
      HibernateProperties hibernateProperties = context.getBean(HibernateProperties.class);
      Map<String, Object> properties = hibernateProperties
              .determineHibernateProperties(jpaProperties.getProperties(), new HibernateSettings());
      assertThat(properties.get("hibernate.ejb.naming_strategy")).isNull();
    });
  }

  @Test
  void namingStrategyBeansAreUsed() {
    this.contextRunner.withUserConfiguration(NamingStrategyConfiguration.class)
            .withPropertyValues("datasource.url:jdbc:h2:mem:naming-strategy-beans").run((context) -> {
              HibernateJpaConfiguration jpaConfiguration = context.getBean(HibernateJpaConfiguration.class);
              Map<String, Object> hibernateProperties = jpaConfiguration.getVendorProperties();
              assertThat(hibernateProperties.get("hibernate.implicit_naming_strategy"))
                      .isEqualTo(NamingStrategyConfiguration.implicitNamingStrategy);
              assertThat(hibernateProperties.get("hibernate.physical_naming_strategy"))
                      .isEqualTo(NamingStrategyConfiguration.physicalNamingStrategy);
            });
  }

  @Test
  void hibernatePropertiesCustomizersAreAppliedInOrder() {
    this.contextRunner.withUserConfiguration(HibernatePropertiesCustomizerConfiguration.class).run((context) -> {
      HibernateJpaConfiguration jpaConfiguration = context.getBean(HibernateJpaConfiguration.class);
      Map<String, Object> hibernateProperties = jpaConfiguration.getVendorProperties();
      assertThat(hibernateProperties.get("test.counter")).isEqualTo(2);
    });
  }

  @Test
  void defaultDatabaseIsSet() {
    this.contextRunner.withPropertyValues("datasource.url:jdbc:h2:mem:testdb").run((context) -> {
      HibernateJpaVendorAdapter bean = context.getBean(HibernateJpaVendorAdapter.class);
      Database database = (Database) ReflectionTestUtils.getField(bean, "database");
      assertThat(database).isEqualTo(Database.DEFAULT);
    });
  }

  @Configuration(proxyBeanMethods = false)
  @TestAutoConfigurationPackage(City.class)
  static class TestConfiguration {

  }

  @Configuration(proxyBeanMethods = false)
  static class MockDataSourceConfiguration {

    @Bean
    DataSource dataSource() {
      DataSource dataSource = mock(DataSource.class);
      try {
        given(dataSource.getConnection()).willReturn(mock(Connection.class));
        given(dataSource.getConnection().getMetaData()).willReturn(mock(DatabaseMetaData.class));
      }
      catch (SQLException ex) {
        // Do nothing
      }
      return dataSource;
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class NamingStrategyConfiguration {

    static final ImplicitNamingStrategy implicitNamingStrategy = new ImplicitNamingStrategyJpaCompliantImpl();

    static final PhysicalNamingStrategy physicalNamingStrategy = new PhysicalNamingStrategyStandardImpl();

    @Bean
    ImplicitNamingStrategy implicitNamingStrategy() {
      return implicitNamingStrategy;
    }

    @Bean
    PhysicalNamingStrategy physicalNamingStrategy() {
      return physicalNamingStrategy;
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class HibernatePropertiesCustomizerConfiguration {

    @Bean
    @Order(2)
    HibernatePropertiesCustomizer sampleCustomizer() {
      return ((hibernateProperties) -> hibernateProperties.put("test.counter", 2));
    }

    @Bean
    @Order(1)
    HibernatePropertiesCustomizer anotherCustomizer() {
      return ((hibernateProperties) -> hibernateProperties.put("test.counter", 1));
    }

  }

}
