/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
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

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.jdbc.JdbcExecuter;
import cn.taketoday.jdbc.JdbcUtils;
import test.jdbc.model.Article;
import test.jdbc.model.User;

/**
 * @author TODAY <br>
 *         2019-08-19 21:33
 */
public class JdbcMySQLTest {

    private static JdbcExecuter executer;
    private static ApplicationContext applicationContext = new StandardApplicationContext("info.properties", "test.jdbc");

    static {
        executer = applicationContext.getBean("mySQLExecuter", JdbcExecuter.class);
    }

    @AfterClass
    public static void destory() {
        applicationContext.close();
    }

    @BeforeClass
    public static void setUp() throws SQLException {

//        int update = executer.update("insert into `t_user` (`name`,`age`) values ('Jerry', 12);");
//        update += executer.update("insert into `t_user` (`name`,`age`) values ('Tom', 12);");
//        System.err.println(update);
    }

    @Test
    public void testQuery() throws SQLException {

        long start = System.currentTimeMillis();
        executer.query("select * from t_user", (ResultSet rs, int n) -> {

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

    public static class TEST {

    }

    
    @Test
    public void testQueryArticle() throws SQLException {

        long start = System.currentTimeMillis();
        executer.query("select * from t_article", (ResultSet rs, int n) -> {

            final ResultSetMetaData metaData = rs.getMetaData();
            final int columnCount = metaData.getColumnCount() + 1;

            for (int i = 1; i < columnCount; i++) {
                final Object object = rs.getObject(i);
                System.err.println(JdbcUtils.getColumnName(metaData, i) + " == " + object);
            }
            return null;
        });

        final Article queryObject = executer.query("select * from t_article", Article.class);

        System.err.println(queryObject);
        System.err.println(executer.queryList("select * from t_article", Article.class));
        System.err.println("query " + (System.currentTimeMillis() - start) + " ms");
    }

    
}
