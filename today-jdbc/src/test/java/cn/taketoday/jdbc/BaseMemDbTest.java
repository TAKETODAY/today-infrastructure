/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.jdbc;

import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

/**
 * Created by lars on 01.11.14.
 */
public abstract class BaseMemDbTest {

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

  @Parameterized.Parameters(name = "{index} - {2}")
  public static Collection<Object[]> getData() {
    return Arrays.asList(new Object[][] {
            { DbType.H2, "H2 test" },
            { DbType.HyperSQL, "HyperSQL Test" }
    });
  }

  protected final DbType dbType;
  protected final RepositoryManager repositoryManager;

  public BaseMemDbTest(DbType dbType, String testName) {
    this.dbType = dbType;
    this.repositoryManager = new RepositoryManager(dbType.url, dbType.user, dbType.pass);

    if (dbType == DbType.HyperSQL) {
      try (JdbcConnection con = repositoryManager.open()) {
        con.createNamedQuery("set database sql syntax MYS true").executeUpdate();
      }
    }
  }
}
