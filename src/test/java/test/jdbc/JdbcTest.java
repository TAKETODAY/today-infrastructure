/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
package test.jdbc;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.jdbc.JdbcExecutor;
import cn.taketoday.jdbc.utils.JdbcUtils;
import test.jdbc.model.User;

/**
 * @author TODAY <br>
 *         2019-08-19 21:33
 */
@Ignore
public class JdbcTest {

  private static ApplicationContext applicationContext = new StandardApplicationContext("info.properties", "test.jdbc");

  private static JdbcExecutor executer = applicationContext.getBean("h2Executor", JdbcExecutor.class);

  @AfterClass
  public static void destory() {
    applicationContext.close();
  }

  @BeforeClass
  public static void setUp() throws SQLException {

    String sql = "CREATE TABLE `t_user` (\n" + //
            "  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,\n" + //
            "  `name` varchar(100) NOT NULL,\n" + //
            "  `age` int(11) NOT NULL,\n" + //
            "  PRIMARY KEY (`id`)\n" + //
            ");";

    String insert = "insert into `t_user` (`id`,`name`,`age`) values (1, 'Jerry', 12);\n" + //
            "insert into `t_user` (`id`,`name`,`age`) values (2, 'Tom', 12);";

    int update = executer.update(sql);
    System.err.println(update);
    update = executer.update(insert);
    System.err.println(update);
  }

  @Test
  public void testH2() throws SQLException {

    long start = System.currentTimeMillis();
    executer.queryList("select * from t_user", (ResultSet rs, int n) -> {

      final ResultSetMetaData metaData = rs.getMetaData();
      final int columnCount = metaData.getColumnCount() + 1;

      for (int i = 1; i < columnCount; i++) {
        final Object object = rs.getObject(i);
        System.err.println(JdbcUtils.getColumnName(metaData, i) + " == " + object);
      }
      return null;
    });

    final User queryObject = executer.query("select * from t_user", User.class);

    System.err.println(queryObject);

    System.err.println(executer.queryList("select * from t_user", User.class));

    System.err.println("query " + (System.currentTimeMillis() - start) + " ms");
  }

  @Test
  public void testMap() throws SQLException {

    long start = System.currentTimeMillis();
    final List<Map<String, Object>> queryMap = executer.queryList("select * from t_user");

    System.err.println(queryMap);
    System.err.println("Map " + (System.currentTimeMillis() - start) + " ms");
  }

  public static class TEST {

  }

}
