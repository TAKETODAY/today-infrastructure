package cn.taketoday.jdbc.support.incrementer;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import cn.taketoday.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import cn.taketoday.jdbc.datasource.embedded.EmbeddedDatabaseType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/10 9:37
 */
class H2SequenceMaxValueIncrementerTests {

  @Test
  void testH2SequenceMaxValueIncrementer() {
    DataSource dataSource = new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .addScript("classpath:/cn/taketoday/jdbc/support/incrementer/schema.sql")
            .build();
    H2SequenceMaxValueIncrementer incrementer = new H2SequenceMaxValueIncrementer(dataSource, "SEQ");

    assertThat(incrementer.nextIntValue()).isEqualTo(1);
    assertThat(incrementer.nextStringValue()).isEqualTo("2");
  }

}