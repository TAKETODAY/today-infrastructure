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

package infra.persistence;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.stream.Stream;

import infra.jdbc.JdbcConnection;
import infra.jdbc.RepositoryManager;
import infra.persistence.platform.MySQLPlatform;

import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/7 20:10
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractRepositoryManagerTests {

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("infra.persistence.AbstractRepositoryManagerTests#data")
  public @interface ParameterizedRepositoryManagerTest {

  }

  public Stream<Arguments> data() {
    return Stream.of(
            arguments(named("H2 tests", DbType.H2), createRepositoryManager(DbType.H2)),
            arguments(named("HyperSQL tests", DbType.HyperSQL), createRepositoryManager(DbType.HyperSQL))
    );
  }

  protected RepositoryManager createRepositoryManager(DbType dbType) {
    RepositoryManager repositoryManager = new RepositoryManager(dbType.url, dbType.user, dbType.pass);
    if (dbType == DbType.HyperSQL) {
      try (JdbcConnection con = repositoryManager.open()) {
        con.createNamedQuery("set database sql syntax MYS true")
                .executeUpdate();
      }
    }

    prepareTestsData(dbType, repositoryManager);
    return repositoryManager;
  }

  protected void prepareTestsData(DbType dbType, RepositoryManager repositoryManager) {
  }

  public enum DbType {
    H2("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", ""),
    HyperSQL("jdbc:hsqldb:mem:testmemdb;sql.syntax_mys=true", "SA", "");

    public final String url;
    public final String user;
    public final String pass;

    DbType(String url, String user, String pass) {
      this.url = url;
      this.user = user;
      this.pass = pass;
    }
  }

  static class HyperSQLPlatform extends MySQLPlatform {

    @Override
    public void selectCountFrom(StringBuilder countSql, String tableName) {
      countSql.append("SELECT COUNT(*) FROM ")
              .append(tableName);
    }
  }

}
