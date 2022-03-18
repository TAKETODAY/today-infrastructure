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
package cn.taketoday.orm.mybatis.support;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.context.ApplicationContextException;
import cn.taketoday.context.annotation.AnnotationConfigUtils;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.orm.mybatis.AbstractMyBatisTest;
import cn.taketoday.orm.mybatis.SqlSessionFactoryBean;
import cn.taketoday.orm.mybatis.SqlSessionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SqlSessionDaoSupportTest extends AbstractMyBatisTest {
  private SqlSessionDaoSupport sqlSessionDaoSupport;

  private GenericApplicationContext applicationContext;

  @BeforeEach
  void setup() {
    sqlSessionDaoSupport = new MockSqlSessionDao();
  }

  @AfterEach
  void closeConnection() throws SQLException {
    connection.close();
  }

  @Test
  void testWithSqlSessionTemplate() {
    SqlSessionTemplate sessionTemplate = new SqlSessionTemplate(sqlSessionFactory);
    sqlSessionDaoSupport.setSqlSessionTemplate(sessionTemplate);
    sqlSessionDaoSupport.afterPropertiesSet();

    assertThat(sqlSessionDaoSupport.getSqlSession()).as("should store the Template").isEqualTo(sessionTemplate);
  }

  @Test
  void testWithSqlSessionFactory() {
    sqlSessionDaoSupport.setSqlSessionFactory(sqlSessionFactory);
    sqlSessionDaoSupport.afterPropertiesSet();

    assertThat(((SqlSessionTemplate) sqlSessionDaoSupport.getSqlSession()).getSqlSessionFactory())
            .as("should store the Factory").isEqualTo(sqlSessionFactory);
  }

  @Test
  void testWithBothFactoryAndTemplate() {
    SqlSessionTemplate sessionTemplate = new SqlSessionTemplate(sqlSessionFactory);
    sqlSessionDaoSupport.setSqlSessionTemplate(sessionTemplate);
    sqlSessionDaoSupport.setSqlSessionFactory(sqlSessionFactory);
    sqlSessionDaoSupport.afterPropertiesSet();

    assertThat(sqlSessionDaoSupport.getSqlSession()).as("should ignore the Factory").isEqualTo(sessionTemplate);
  }

  @Test
  void testWithNoFactoryOrSession() {
    assertThrows(IllegalArgumentException.class, sqlSessionDaoSupport::afterPropertiesSet);
  }

  @Test
  void testAutowireWithNoFactoryOrSession() {
    setupContext();
    assertThrows(ApplicationContextException.class, this::startContext);
  }

  @Test
  void testAutowireWithTwoFactories() {
    setupContext();

    setupSqlSessionFactory("factory1");
    setupSqlSessionFactory("factory2");

    assertThrows(ApplicationContextException.class, this::startContext);
  }

  private void setupContext() {
    applicationContext = new GenericApplicationContext();

    RootBeanDefinition definition = new RootBeanDefinition();
    definition.setBeanClass(MockSqlSessionDao.class);
    applicationContext.registerBeanDefinition("dao", definition);

    // add support for autowiring fields
    AnnotationConfigUtils.registerAnnotationConfigProcessors(applicationContext);
  }

  private void startContext() {
    applicationContext.refresh();
    applicationContext.start();

    sqlSessionDaoSupport = applicationContext.getBean(MockSqlSessionDao.class);
  }

  private void setupSqlSessionFactory(String name) {
    RootBeanDefinition definition = new RootBeanDefinition();
    definition.setBeanClass(SqlSessionFactoryBean.class);
    definition.getPropertyValues().add("dataSource", dataSource);

    applicationContext.registerBeanDefinition(name, definition);
  }

  private static final class MockSqlSessionDao extends SqlSessionDaoSupport {
  }
}
