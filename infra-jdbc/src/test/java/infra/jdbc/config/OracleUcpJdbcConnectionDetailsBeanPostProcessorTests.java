package infra.jdbc.config;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import infra.beans.factory.ObjectProvider;
import oracle.ucp.jdbc.PoolDataSourceImpl;
import oracle.ucp.util.OpaqueString;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
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
    assertThat(dataSource).extracting("password")
            .extracting((password) -> ((OpaqueString) password).get())
            .isEqualTo("password-1");
    assertThat(dataSource.getConnectionFactoryClassName())
            .isEqualTo(DatabaseDriver.POSTGRESQL.getDriverClassName());
  }

}