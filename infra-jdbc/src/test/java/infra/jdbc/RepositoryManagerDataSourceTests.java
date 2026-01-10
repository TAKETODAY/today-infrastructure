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

package infra.jdbc;

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
    manager.createNamedQuery(sql).setName("testExecuteAndFetchWithNulls").executeUpdate();

    manager.runInTransaction((connection, argument) -> {
      NamedQuery insQuery = connection.createNamedQuery(
              "insert into testExecWithNullsTbl (text, aNumber, aLongNumber) values(:text, :number, :lnum)");
      insQuery.addParameter("text", "some text").addParameter("number", 2).addParameter("lnum", 10L).executeUpdate();
      insQuery.addParameter("text", "some text").addParameter("number", (Integer) null).addParameter("lnum", 10L).executeUpdate();
      insQuery.addParameter("text", (String) null).addParameter("number", 21).addParameter("lnum", (Long) null).executeUpdate();
      insQuery.addParameter("text", "some text").addParameter("number", 1221).addParameter("lnum", 10).executeUpdate();
      insQuery.addParameter("text", "some text").addParameter("number", 2311).addParameter("lnum", 12).executeUpdate();
    });

    List<Entity> fetched = manager.createNamedQuery("select * from testExecWithNullsTbl").fetch(Entity.class);

    assertEquals(5, fetched.size());
    assertNull(fetched.get(2).text);
    assertNotNull(fetched.get(3).text);

    assertNull(fetched.get(1).aNumber);
    assertNotNull(fetched.get(2).aNumber);

    assertNull(fetched.get(2).aLongNumber);
    assertNotNull(fetched.get(3).aLongNumber);

  }
}
