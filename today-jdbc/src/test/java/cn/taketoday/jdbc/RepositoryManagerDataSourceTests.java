/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Created with IntelliJ IDEA. User: lars Date: 10/5/12 Time: 10:54 PM To change
 * this template use File | Settings | File Templates.
 */
public class RepositoryManagerDataSourceTests {

  private RepositoryManager manager;

  private String url = "jdbc:h2:mem:test2;DB_CLOSE_DELAY=-1;MODE=MySQL";
  private String user = "sa";
  private String pass = "";

  @BeforeEach
  protected void setUp() throws Exception {
    DataSource ds = JdbcConnectionPool.create(url, user, pass);

    manager = new RepositoryManager(ds);
  }

  @Test
  public void testExecuteAndFetchWithNulls() {
    String sql = "create table testExecWithNullsTbl (" +
            "id int auto_increment primary key, " +
            "text varchar(255), " +
            "aNumber int, " +
            "aLongNumber bigint)";
    manager.createQuery(sql).setName("testExecuteAndFetchWithNulls").executeUpdate();

    manager.runInTransaction((connection, argument) -> {
      NamedQuery insQuery = connection.createQuery(
              "insert into testExecWithNullsTbl (text, aNumber, aLongNumber) values(:text, :number, :lnum)");
      insQuery.addParameter("text", "some text").addParameter("number", 2).addParameter("lnum", 10L).executeUpdate();
      insQuery.addParameter("text", "some text").addParameter("number", (Integer) null).addParameter("lnum", 10L).executeUpdate();
      insQuery.addParameter("text", (String) null).addParameter("number", 21).addParameter("lnum", (Long) null).executeUpdate();
      insQuery.addParameter("text", "some text").addParameter("number", 1221).addParameter("lnum", 10).executeUpdate();
      insQuery.addParameter("text", "some text").addParameter("number", 2311).addParameter("lnum", 12).executeUpdate();
    });

    List<Entity> fetched = manager.createQuery("select * from testExecWithNullsTbl").fetch(Entity.class);

    assertEquals(5, fetched.size());
    assertNull(fetched.get(2).text);
    assertNotNull(fetched.get(3).text);

    assertNull(fetched.get(1).aNumber);
    assertNotNull(fetched.get(2).aNumber);

    assertNull(fetched.get(2).aLongNumber);
    assertNotNull(fetched.get(3).aLongNumber);

  }
}
