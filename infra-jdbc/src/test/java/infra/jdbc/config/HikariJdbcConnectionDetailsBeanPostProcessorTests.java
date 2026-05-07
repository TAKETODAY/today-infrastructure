package infra.jdbc.config;

import com.zaxxer.hikari.HikariDataSource;

import org.junit.jupiter.api.Test;

import infra.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/5/7 19:31
 */
class HikariJdbcConnectionDetailsBeanPostProcessorTests {

  @Test
  @SuppressWarnings("unchecked")
  void setUsernamePasswordAndUrl() {
    HikariDataSource dataSource = new HikariDataSource();
    dataSource.setJdbcUrl("will-be-overwritten");
    dataSource.setUsername("will-be-overwritten");
    dataSource.setPassword("will-be-overwritten");
    dataSource.setDriverClassName(DatabaseDriver.H2.getDriverClassName());
    new HikariJdbcConnectionDetailsBeanPostProcessor(mock(ObjectProvider.class)).processDataSource(dataSource,
            new TestJdbcConnectionDetails());
    assertThat(dataSource.getJdbcUrl()).isEqualTo("jdbc:customdb://customdb.example.com:12345/database-1");
    assertThat(dataSource.getUsername()).isEqualTo("user-1");
    assertThat(dataSource.getPassword()).isEqualTo("password-1");
    assertThat(dataSource.getDriverClassName()).isEqualTo(DatabaseDriver.POSTGRESQL.getDriverClassName());
  }

  @Test
  @SuppressWarnings("unchecked")
  void toleratesConnectionDetailsWithNullDriverClassName() {
    HikariDataSource dataSource = new HikariDataSource();
    dataSource.setDriverClassName(DatabaseDriver.H2.getDriverClassName());
    JdbcConnectionDetails connectionDetails = mock(JdbcConnectionDetails.class);
    new HikariJdbcConnectionDetailsBeanPostProcessor(mock(ObjectProvider.class)).processDataSource(dataSource,
            connectionDetails);
    assertThat(dataSource.getDriverClassName()).isEqualTo(DatabaseDriver.H2.getDriverClassName());
  }

}