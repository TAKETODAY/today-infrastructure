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

package infra.test.context.junit.jupiter.nested;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.jdbc.datasource.DataSourceTransactionManager;
import infra.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import infra.test.annotation.Commit;
import infra.test.annotation.Rollback;
import infra.test.context.NestedTestConfiguration;
import infra.test.context.junit.jupiter.InfraExtension;
import infra.test.context.junit.jupiter.JUnitConfig;
import infra.test.context.transaction.TestTransaction;
import infra.test.transaction.TransactionAssert;
import infra.transaction.TransactionManager;
import infra.transaction.annotation.EnableTransactionManagement;
import infra.transaction.annotation.Transactional;

import static infra.test.context.NestedTestConfiguration.EnclosingConfiguration.INHERIT;
import static infra.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;
import static infra.transaction.annotation.Propagation.NOT_SUPPORTED;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify support for {@code @Nested} test classes using
 * {@link Transactional @Transactional} in conjunction with the
 * {@link InfraExtension} in a JUnit Jupiter environment.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitConfig
@Transactional
@Commit
@NestedTestConfiguration(OVERRIDE) // since INHERIT is now the global default
class TransactionalNestedTests {

  @Test
  void transactional(@Autowired DataSource dataSource) {
    TransactionAssert.assertThatTransaction().isActive();
    assertThat(dataSource).isNotNull();
    assertCommit();
  }

  @Nested
  @JUnitConfig(Config.class)
  class ConfigOverriddenByDefaultTests {

    @Test
    void notTransactional(@Autowired DataSource dataSource) {
      TransactionAssert.assertThatTransaction().isNotActive();
      assertThat(dataSource).isNotNull();
    }
  }

  @Nested
  @NestedTestConfiguration(INHERIT)
  class InheritedConfigTests {

    @Test
    void transactional(@Autowired DataSource dataSource) {
      TransactionAssert.assertThatTransaction().isActive();
      assertThat(dataSource).isNotNull();
      assertCommit();
    }

    @Nested
    class DoubleNestedWithImplicitlyInheritedConfigTests {

      @Test
      void transactional(@Autowired DataSource dataSource) {
        TransactionAssert.assertThatTransaction().isActive();
        assertThat(dataSource).isNotNull();
        assertCommit();
      }

      @Nested
      @Rollback
      class TripleNestedWithImplicitlyInheritedConfigTests {

        @Test
        void transactional(@Autowired DataSource dataSource) {
          TransactionAssert.assertThatTransaction().isActive();
          assertThat(dataSource).isNotNull();
          assertRollback();
        }
      }
    }

    @Nested
    @NestedTestConfiguration(OVERRIDE)
    @JUnitConfig(Config.class)
    @Transactional
    @Rollback
    class DoubleNestedWithOverriddenConfigTests {

      @Test
      void transactional(@Autowired DataSource dataSource) {
        TransactionAssert.assertThatTransaction().isActive();
        assertThat(dataSource).isNotNull();
        assertRollback();
      }

      @Nested
      @NestedTestConfiguration(INHERIT)
      @Commit
      class TripleNestedWithInheritedConfigTests {

        @Test
        void transactional(@Autowired DataSource dataSource) {
          TransactionAssert.assertThatTransaction().isActive();
          assertThat(dataSource).isNotNull();
          assertCommit();
        }
      }

      @Nested
      @NestedTestConfiguration(INHERIT)
      class TripleNestedWithInheritedConfigAndTestInterfaceTests implements TestInterface {

        @Test
        void notTransactional(@Autowired DataSource dataSource) {
          TransactionAssert.assertThatTransaction().isNotActive();
          assertThat(dataSource).isNotNull();
        }
      }
    }
  }

  private void assertCommit() {
    assertThat(TestTransaction.isFlaggedForRollback()).as("flagged for commit").isFalse();
  }

  private void assertRollback() {
    assertThat(TestTransaction.isFlaggedForRollback()).as("flagged for rollback").isTrue();
  }

  // -------------------------------------------------------------------------

  @Configuration
  @EnableTransactionManagement
  static class Config {

    @Bean
    TransactionManager transactionManager(DataSource dataSource) {
      return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    DataSource dataSource() {
      return new EmbeddedDatabaseBuilder().generateUniqueName(true).build();
    }
  }

  @Transactional(propagation = NOT_SUPPORTED)
  interface TestInterface {
  }

}
