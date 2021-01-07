package cn.taketoday.jdbc;

import org.junit.Rule;
import org.junit.Test;
import org.zapodot.junit.db.EmbeddedDatabaseRule;

import java.util.List;

import cn.taketoday.jdbc.DefaultSession;
import cn.taketoday.jdbc.JdbcConnection;
import cn.taketoday.jdbc.Query;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.zapodot.junit.db.EmbeddedDatabaseRule.CompatibilityMode.Oracle;

/**
 * @author zapodot
 */
public class QueryArrayTest {

  private static class Foo {
    public int bar;
  }

  @Rule
  public EmbeddedDatabaseRule databaseRule = EmbeddedDatabaseRule.builder()
          .withMode(Oracle)
          .withInitialSql("CREATE TABLE FOO(BAR int PRIMARY KEY); INSERT INTO FOO VALUES(1); INSERT INTO FOO VALUES(2)")
          .build();

  @Test
  public void arrayTest() throws Exception {
    final DefaultSession database = new DefaultSession(databaseRule.getDataSource());
    try (final JdbcConnection connection = database.open();
            final Query query = connection.createQuery("SELECT * FROM FOO WHERE BAR IN (:bars)")) {
      final List<Foo> foos = query.addParameters("bars", 1, 2).executeAndFetch(Foo.class);
      assertThat(foos.size(), equalTo(2));

    }
  }

  @Test
  public void emptyArrayTest() throws Exception {
    final DefaultSession database = new DefaultSession(databaseRule.getDataSource());

    try (final JdbcConnection connection = database.open();
            final Query query = connection.createQuery("SELECT * FROM FOO WHERE BAR IN (:bars)")) {

      final List<Foo> noFoos = query.addParameters("bars", new Integer[] {}).executeAndFetch(Foo.class);
      assertThat(noFoos.size(), equalTo(0));
    }
  }
}
