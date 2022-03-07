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

package cn.taketoday.jdbc.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.context.support.ClassPathXmlApplicationContext;
import cn.taketoday.jdbc.BadSqlGrammarException;
import cn.taketoday.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Dave Syer
 */
public class InitializeDatabaseIntegrationTests {

  private String enabled;

  private ClassPathXmlApplicationContext context;

  @BeforeEach
  public void init() {
    enabled = System.setProperty("ENABLED", "true");
  }

  @AfterEach
  public void after() {
    if (enabled != null) {
      System.setProperty("ENABLED", enabled);
    }
    else {
      System.clearProperty("ENABLED");
    }
    if (context != null) {
      context.close();
    }
  }

  @Test
  public void testCreateEmbeddedDatabase() throws Exception {
    context = new ClassPathXmlApplicationContext("cn/taketoday/jdbc/config/jdbc-initialize-config.xml");
    assertCorrectSetup(context.getBean("dataSource", DataSource.class));
  }

  @Test
  public void testDisableCreateEmbeddedDatabase() throws Exception {
    System.setProperty("ENABLED", "false");
    context = new ClassPathXmlApplicationContext("cn/taketoday/jdbc/config/jdbc-initialize-config.xml");
    assertThatExceptionOfType(BadSqlGrammarException.class).isThrownBy(() ->
            assertCorrectSetup(context.getBean("dataSource", DataSource.class)));
  }

  @Test
  public void testIgnoreFailedDrops() throws Exception {
    context = new ClassPathXmlApplicationContext("cn/taketoday/jdbc/config/jdbc-initialize-fail-config.xml");
    assertCorrectSetup(context.getBean("dataSource", DataSource.class));
  }

  @Test
  public void testScriptNameWithPattern() throws Exception {
    context = new ClassPathXmlApplicationContext("cn/taketoday/jdbc/config/jdbc-initialize-pattern-config.xml");
    DataSource dataSource = context.getBean("dataSource", DataSource.class);
    assertCorrectSetup(dataSource);
    JdbcTemplate t = new JdbcTemplate(dataSource);
    assertThat(t.queryForObject("select name from T_TEST", String.class)).isEqualTo("Dave");
  }

  @Test
  public void testScriptNameWithPlaceholder() throws Exception {
    context = new ClassPathXmlApplicationContext("cn/taketoday/jdbc/config/jdbc-initialize-placeholder-config.xml");
    DataSource dataSource = context.getBean("dataSource", DataSource.class);
    assertCorrectSetup(dataSource);
  }

  @Test
  public void testScriptNameWithExpressions() throws Exception {
    context = new ClassPathXmlApplicationContext("cn/taketoday/jdbc/config/jdbc-initialize-expression-config.xml");
    DataSource dataSource = context.getBean("dataSource", DataSource.class);
    assertCorrectSetup(dataSource);
  }

  @Test
  public void testCacheInitialization() throws Exception {
    context = new ClassPathXmlApplicationContext("cn/taketoday/jdbc/config/jdbc-initialize-cache-config.xml");
    assertCorrectSetup(context.getBean("dataSource", DataSource.class));
    CacheData cache = context.getBean(CacheData.class);
    assertThat(cache.getCachedData().size()).isEqualTo(1);
  }

  private void assertCorrectSetup(DataSource dataSource) {
    JdbcTemplate jt = new JdbcTemplate(dataSource);
    assertThat(jt.queryForObject("select count(*) from T_TEST", Integer.class).intValue()).isEqualTo(1);
  }

  public static class CacheData implements InitializingBean {

    private JdbcTemplate jdbcTemplate;

    private List<Map<String, Object>> cache;

    public void setDataSource(DataSource dataSource) {
      this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public List<Map<String, Object>> getCachedData() {
      return cache;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
      cache = jdbcTemplate.queryForList("SELECT * FROM T_TEST");
    }
  }

}
