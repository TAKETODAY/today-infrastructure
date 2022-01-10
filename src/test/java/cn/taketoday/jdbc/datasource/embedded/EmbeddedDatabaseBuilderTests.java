/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.jdbc.datasource.embedded;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import cn.taketoday.core.io.ClassRelativeResourceLoader;
import cn.taketoday.jdbc.core.JdbcTemplate;
import cn.taketoday.jdbc.datasource.init.CannotReadScriptException;
import cn.taketoday.jdbc.datasource.init.ScriptStatementFailedException;

import static cn.taketoday.jdbc.datasource.embedded.EmbeddedDatabaseType.DERBY;
import static cn.taketoday.jdbc.datasource.embedded.EmbeddedDatabaseType.H2;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Integration tests for {@link EmbeddedDatabaseBuilder}.
 *
 * @author Keith Donald
 * @author Sam Brannen
 */
@Execution(ExecutionMode.SAME_THREAD)
public class EmbeddedDatabaseBuilderTests {

  private final EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder(
          new ClassRelativeResourceLoader(getClass()));

  @Test
  public void addDefaultScripts() throws Exception {
    doTwice(() -> {
      EmbeddedDatabase db = new EmbeddedDatabaseBuilder()//
              .addDefaultScripts()//
              .build();
      assertDatabaseCreatedAndShutdown(db);
    });
  }

  @Test
  public void addScriptWithBogusFileName() {
    assertThatExceptionOfType(CannotReadScriptException.class).isThrownBy(
            new EmbeddedDatabaseBuilder().addScript("bogus.sql")::build);
  }

  @Test
  public void addScript() throws Exception {
    doTwice(() -> {
      EmbeddedDatabase db = builder//
              .addScript("db-schema.sql")//
              .addScript("db-test-data.sql")//
              .build();
      assertDatabaseCreatedAndShutdown(db);
    });
  }

  @Test
  public void addScripts() throws Exception {
    doTwice(() -> {
      EmbeddedDatabase db = builder//
              .addScripts("db-schema.sql", "db-test-data.sql")//
              .build();
      assertDatabaseCreatedAndShutdown(db);
    });
  }

  @Test
  public void addScriptsWithDefaultCommentPrefix() throws Exception {
    doTwice(() -> {
      EmbeddedDatabase db = builder//
              .addScripts("db-schema-comments.sql", "db-test-data.sql")//
              .build();
      assertDatabaseCreatedAndShutdown(db);
    });
  }

  @Test
  public void addScriptsWithCustomCommentPrefix() throws Exception {
    doTwice(() -> {
      EmbeddedDatabase db = builder//
              .addScripts("db-schema-custom-comments.sql", "db-test-data.sql")//
              .setCommentPrefix("~")//
              .build();
      assertDatabaseCreatedAndShutdown(db);
    });
  }

  @Test
  public void addScriptsWithCustomBlockComments() throws Exception {
    doTwice(() -> {
      EmbeddedDatabase db = builder//
              .addScripts("db-schema-block-comments.sql", "db-test-data.sql")//
              .setBlockCommentStartDelimiter("{*")//
              .setBlockCommentEndDelimiter("*}")//
              .build();
      assertDatabaseCreatedAndShutdown(db);
    });
  }

  @Test
  public void setTypeToH2() throws Exception {
    doTwice(() -> {
      EmbeddedDatabase db = builder//
              .setType(H2)//
              .addScripts("db-schema.sql", "db-test-data.sql")//
              .build();
      assertDatabaseCreatedAndShutdown(db);
    });
  }

  @Test
  public void setTypeToDerbyAndIgnoreFailedDrops() throws Exception {
    doTwice(() -> {
      EmbeddedDatabase db = builder//
              .setType(DERBY)//
              .ignoreFailedDrops(true)//
              .addScripts("db-schema-derby-with-drop.sql", "db-test-data.sql").build();
      assertDatabaseCreatedAndShutdown(db);
    });
  }

  @Test
  public void createSameSchemaTwiceWithoutUniqueDbNames() throws Exception {
    EmbeddedDatabase db1 = new EmbeddedDatabaseBuilder(new ClassRelativeResourceLoader(getClass()))
            .addScripts("db-schema-without-dropping.sql").build();
    try {
      assertThatExceptionOfType(ScriptStatementFailedException.class)
              .isThrownBy(() ->
                      new EmbeddedDatabaseBuilder(new ClassRelativeResourceLoader(getClass()))
                              .addScripts("db-schema-without-dropping.sql").build());
    }
    finally {
      db1.shutdown();
    }
  }

  @Test
  public void createSameSchemaTwiceWithGeneratedUniqueDbNames() throws Exception {
    EmbeddedDatabase db1 = new EmbeddedDatabaseBuilder(new ClassRelativeResourceLoader(getClass()))//
            .addScripts("db-schema-without-dropping.sql", "db-test-data.sql")//
            .generateUniqueName(true)//
            .build();

    JdbcTemplate template1 = new JdbcTemplate(db1);
    assertNumRowsInTestTable(template1, 1);
    template1.update("insert into T_TEST (NAME) values ('Sam')");
    assertNumRowsInTestTable(template1, 2);

    EmbeddedDatabase db2 = new EmbeddedDatabaseBuilder(new ClassRelativeResourceLoader(getClass()))//
            .addScripts("db-schema-without-dropping.sql", "db-test-data.sql")//
            .generateUniqueName(true)//
            .build();
    assertDatabaseCreated(db2);

    db1.shutdown();
    db2.shutdown();
  }

  private void doTwice(Runnable test) {
    test.run();
    test.run();
  }

  private void assertNumRowsInTestTable(JdbcTemplate template, int count) {
    assertThat(template.queryForObject("select count(*) from T_TEST", Integer.class).intValue()).isEqualTo(count);
  }

  private void assertDatabaseCreated(EmbeddedDatabase db) {
    assertNumRowsInTestTable(new JdbcTemplate(db), 1);
  }

  private void assertDatabaseCreatedAndShutdown(EmbeddedDatabase db) {
    assertDatabaseCreated(db);
    db.shutdown();
  }

}
