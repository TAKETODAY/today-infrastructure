/*
 * Copyright 2012-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.jdbc.config;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import infra.beans.factory.ObjectProvider;
import oracle.ucp.config.PoolProperties;
import oracle.ucp.config.PoolPropertiesException;
import oracle.ucp.jdbc.PoolDataSourceImpl;
import oracle.ucp.util.OpaqueString;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link OracleUcpJdbcConnectionDetailsBeanPostProcessor}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/5/7 19:30
 */
class OracleUcpJdbcConnectionDetailsBeanPostProcessorTests {

  @Test
  @SuppressWarnings("unchecked")
  void setUsernamePasswordUrlAndDriverClassName() throws SQLException {
    PoolDataSourceImpl dataSource = new PoolDataSourceImpl();
    dataSource.setURL("will-be-overwritten");
    dataSource.setUser("will-be-overwritten");
    dataSource.setPassword("will-be-overwritten");
    dataSource.setConnectionFactoryClassName("will-be-overwritten");
    new OracleUcpJdbcConnectionDetailsBeanPostProcessor(mock(ObjectProvider.class)).processDataSource(dataSource,
            new TestJdbcConnectionDetails());
    assertThat(dataSource.getURL()).isEqualTo("jdbc:customdb://customdb.example.com:12345/database-1");
    assertThat(dataSource.getUser()).isEqualTo("user-1");
    assertThat(dataSource).extracting("recentPoolProperties")
            .asInstanceOf(InstanceOfAssertFactories.type(PoolProperties.class))
            .extracting(this::getPassword)
            .isEqualTo("password-1");
    assertThat(dataSource.getConnectionFactoryClassName())
            .isEqualTo(DatabaseDriver.POSTGRESQL.getDriverClassName());
  }

  private String getPassword(PoolProperties poolProperties) {
    try {
      return ((OpaqueString) poolProperties.getPoolProperty("password")).get();
    }
    catch (PoolPropertiesException ex) {
      throw new IllegalStateException(ex);
    }
  }

}
