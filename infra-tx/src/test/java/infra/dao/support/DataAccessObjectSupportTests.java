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