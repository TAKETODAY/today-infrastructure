/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.persistence;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.stream.Stream;

import cn.taketoday.jdbc.JdbcConnection;
import cn.taketoday.jdbc.RepositoryManager;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/7 20:10
 */
public abstract class AbstractRepositoryManagerTests {

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("cn.taketoday.persistence.AbstractRepositoryManagerTests#data")
  public @interface ParameterizedRepositoryManagerTest {

  }

  public Stream<Named<RepositoryManager>> data() {
    return Stream.of(
            Named.named("H2", createRepositoryManager(DbType.H2))
//            Named.named("HyperSQL", createRepositoryManager(DbType.HyperSQL))
    );
  }

  protected RepositoryManager createRepositoryManager(DbType dbType) {
    RepositoryManager repositoryManager = new RepositoryManager(dbType.url, dbType.user, dbType.pass);
    if (dbType == DbType.HyperSQL) {
      try (JdbcConnection con = repositoryManager.open()) {
        con.createNamedQuery("set database sql syntax MSS true")
                .executeUpdate();
      }
    }

    prepareTestsData(dbType, repositoryManager);
    return repositoryManager;
  }

  protected void prepareTestsData(DbType dbType, RepositoryManager repositoryManager) { }

  public enum DbType {
    H2("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", ""),
    HyperSQL("jdbc:hsqldb:mem:testmemdb", "SA", "");

    public final String url;
    public final String user;
    public final String pass;

    DbType(String url, String user, String pass) {
      this.url = url;
      this.user = user;
      this.pass = pass;
    }
  }

}
