package infra.jdbc.config;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.Test;

import infra.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/5/7 19:32
 */
class Dbcp2JdbcConnectionDetailsBeanPostProcessorTests {

  @Test
  @SuppressWarnings("unchecked")
  void setUsernamePasswordUrlAndDriverClassName() {
    BasicDataSource dataSource = new BasicDataSource();
    dataSource.setUrl("will-be-overwritten");
    dataSource.setUsername("will-be-overwritten");
    dataSource.setPassword("will-be-overwritten");
    dataSource.setDriverClassName("will-be-overwritten");
    new Dbcp2JdbcConnectionDetailsBeanPostProcessor(mock(ObjectProvider.class)).processDataSource(dataSource,
            new TestJdbcConnectionDetails());
    assertThat(dataSource.getUrl()).isEqualTo("jdbc:customdb://customdb.example.com:12345/database-1");
    assertThat(dataSource.getUserName()).isEqualTo("user-1");
    assertThat(dataSource).extracting("password").isEqualTo("password-1");
    assertThat(dataSource.getDriverClassName()).isEqualTo(DatabaseDriver.POSTGRESQL.getDriverClassName());
  }

}