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

package infra.dao.support;

import org.junit.jupiter.api.Test;

import infra.beans.factory.BeanInitializationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/12 18:57
 */
class DataAccessObjectSupportTests {

  @Test
  void afterPropertiesSetCallsCheckDaoConfigAndInitDao() {
    TestDaoSupport daoSupport = new TestDaoSupport();

    assertThatNoException().isThrownBy(daoSupport::afterPropertiesSet);
    assertThat(daoSupport.checkDaoConfigCalled).isTrue();
    assertThat(daoSupport.initDaoCalled).isTrue();
  }

  @Test
  void afterPropertiesSetThrowsBeanInitializationExceptionWhenInitDaoFails() {
    TestDaoSupport daoSupport = new TestDaoSupport() {
      @Override
      protected void initDao() throws Exception {
        throw new RuntimeException("Init failed");
      }
    };

    assertThatExceptionOfType(BeanInitializationException.class)
            .isThrownBy(daoSupport::afterPropertiesSet)
            .withMessage("Initialization of DAO failed")
            .havingCause()
            .withMessage("Init failed");
  }

  @Test
  void afterPropertiesSetThrowsBeanInitializationExceptionWhenCheckDaoConfigFails() {
    TestDaoSupport daoSupport = new TestDaoSupport() {
      @Override
      protected void checkDaoConfig() throws IllegalArgumentException {
        throw new IllegalArgumentException("Config check failed");
      }
    };

    assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(daoSupport::afterPropertiesSet)
            .withMessage("Config check failed");
  }

  @Test
  void initDaoCanBeOverridden() throws Exception {
    TestDaoSupport daoSupport = new TestDaoSupport() {
      @Override
      protected void initDao() throws Exception {
        // Custom implementation
        super.initDao();
      }
    };

    daoSupport.initDao();
    // Test passes if no exception is thrown
  }

  @Test
  void checkDaoConfigMustBeImplemented() {
    // This test ensures the abstract method exists
    TestDaoSupport daoSupport = new TestDaoSupport();
    assertThat(daoSupport).isInstanceOf(DataAccessObjectSupport.class);
  }

  private static class TestDaoSupport extends DataAccessObjectSupport {
    boolean checkDaoConfigCalled = false;
    boolean initDaoCalled = false;

    @Override
    protected void checkDaoConfig() throws IllegalArgumentException {
      checkDaoConfigCalled = true;
    }

    @Override
    protected void initDao() throws Exception {
      super.initDao();
      initDaoCalled = true;
    }
  }

}