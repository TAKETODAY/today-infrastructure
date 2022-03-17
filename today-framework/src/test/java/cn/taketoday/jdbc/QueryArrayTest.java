package cn.taketoday.jdbc;

import org.junit.Rule;
import org.junit.Test;
import org.zapodot.junit.db.EmbeddedDatabaseRule;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.zapodot.junit.db.EmbeddedDatabaseRule.CompatibilityMode.Oracle;

/**
 * @author zapodot
 */
public class QueryArrayTest {

  private static class Foo {
    private int bar;
  }

  @Rule
  public EmbeddedDatabaseRule databaseRule = EmbeddedDatabaseRule.builder()
          .withMode(Oracle)
          .withInitialSql("CREATE TABLE FOO(BAR int PRIMARY KEY); INSERT INTO FOO VALUES(1); INSERT INTO FOO VALUES(2)")
          .build();

  @Test
  public void arrayTest() throws Exception {
    final JdbcOperations database = new JdbcOperations(databaseRule.getDataSource());
    try (final JdbcConnection connection = database.open();
            final Query query = connection.createQuery("SELECT * FROM FOO WHERE BAR IN (:bars)")) {

      final List<Foo> foos = query.addParameters("bars", 1, 2)
              .fetch(Foo.class);

      assertThat(foos.size()).isEqualTo(2);
    }
  }

  @Test
  public void emptyArrayTest() throws Exception {
    final JdbcOperations database = new JdbcOperations(databaseRule.getDataSource());

    try (final JdbcConnection connection = database.open();
            final Query query = connection.createQuery("SELECT * FROM FOO WHERE BAR IN (:bars)")) {

      final List<Foo> noFoos = query.addParameters("bars", new Integer[] {})
              .fetch(Foo.class);
      assertThat(noFoos.size()).isZero();
    }
  }
}
