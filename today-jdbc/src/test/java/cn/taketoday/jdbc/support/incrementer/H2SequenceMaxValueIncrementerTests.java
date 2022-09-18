package cn.taketoday.jdbc.support.incrementer;

import org.h2.engine.Mode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.UUID;

import javax.sql.DataSource;

import cn.taketoday.jdbc.core.JdbcTemplate;
import cn.taketoday.jdbc.datasource.SimpleDriverDataSource;
import cn.taketoday.jdbc.datasource.embedded.EmbeddedDatabase;
import cn.taketoday.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import cn.taketoday.jdbc.datasource.embedded.EmbeddedDatabaseType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/10 9:37
 */
class H2SequenceMaxValueIncrementerTests {

  /**
   * Tests that the incrementer works when using the JDBC connection URL used
   * in the {@code H2EmbeddedDatabaseConfigurer} which is used transparently
   * when using Framework's {@link EmbeddedDatabaseBuilder}.
   *
   * <p>In other words, this tests compatibility with the default H2
   * <em>compatibility mode</em>.
   */
  @Test
  void incrementsSequenceUsingH2EmbeddedDatabaseConfigurer() {
    EmbeddedDatabase database = new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .generateUniqueName(true)
            .addScript("classpath:/cn/taketoday/jdbc/support/incrementer/schema.sql")
            .build();

    assertIncrements(database);

    database.shutdown();
  }

  /**
   * Tests that the incrementer works when using all supported H2 <em>compatibility modes</em>.
   */
  @ParameterizedTest
  @EnumSource(Mode.ModeEnum.class)
  void incrementsSequenceWithExplicitH2CompatibilityMode(Mode.ModeEnum mode) {
    String connectionUrl = String.format("jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false;MODE=%s", UUID.randomUUID(), mode);
    DataSource dataSource = new SimpleDriverDataSource(new org.h2.Driver(), connectionUrl, "sa", "");
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    // language=H2
    jdbcTemplate.execute("CREATE SEQUENCE SEQ");

    assertIncrements(dataSource);

    // language=H2
//    jdbcTemplate.execute("SHUTDOWN");
  }

  private void assertIncrements(DataSource dataSource) {
    // language=H2
    assertThat(new JdbcTemplate(dataSource).queryForObject("values next value for SEQ", int.class)).isEqualTo(1);

    H2SequenceMaxValueIncrementer incrementer = new H2SequenceMaxValueIncrementer(dataSource, "SEQ");
    assertThat(incrementer.nextIntValue()).isEqualTo(2);
    assertThat(incrementer.nextStringValue()).isEqualTo("3");
  }
}
