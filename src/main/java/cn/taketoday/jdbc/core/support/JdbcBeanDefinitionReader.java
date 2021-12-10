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

package cn.taketoday.jdbc.core.support;

import java.util.Properties;

import javax.sql.DataSource;

import cn.taketoday.beans.factory.BeanDefinitionRegistry;
import cn.taketoday.jdbc.core.JdbcTemplate;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Bean definition reader that reads values from a database table,
 * based on a given SQL statement.
 *
 * <p>Expects columns for bean name, property name and value as String.
 * Formats for each are identical to the properties format recognized
 * by PropertiesBeanDefinitionReader.
 *
 * <p><b>NOTE:</b> This is mainly intended as an example for a custom
 * JDBC-based bean definition reader. It does not aim to offer
 * comprehensive functionality.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #loadBeanDefinitions
 * @see cn.taketoday.beans.factory.support.PropertiesBeanDefinitionReader
 * @deprecated as of 5.3, in favor of Spring's common bean definition formats
 * and/or custom reader implementations
 */
@Deprecated
public class JdbcBeanDefinitionReader {

  private final cn.taketoday.beans.factory.support.PropertiesBeanDefinitionReader propReader;

  @Nullable
  private JdbcTemplate jdbcTemplate;

  /**
   * Create a new JdbcBeanDefinitionReader for the given bean factory,
   * using a default PropertiesBeanDefinitionReader underneath.
   * <p>DataSource or JdbcTemplate still need to be set.
   *
   * @see #setDataSource
   * @see #setJdbcTemplate
   */
  public JdbcBeanDefinitionReader(BeanDefinitionRegistry beanFactory) {
    this.propReader = new cn.taketoday.beans.factory.support.PropertiesBeanDefinitionReader(beanFactory);
  }

  /**
   * Create a new JdbcBeanDefinitionReader that delegates to the
   * given PropertiesBeanDefinitionReader underneath.
   * <p>DataSource or JdbcTemplate still need to be set.
   *
   * @see #setDataSource
   * @see #setJdbcTemplate
   */
  public JdbcBeanDefinitionReader(cn.taketoday.beans.factory.support.PropertiesBeanDefinitionReader reader) {
    Assert.notNull(reader, "Bean definition reader must not be null");
    this.propReader = reader;
  }

  /**
   * Set the DataSource to use to obtain database connections.
   * Will implicitly create a new JdbcTemplate with the given DataSource.
   */
  public void setDataSource(DataSource dataSource) {
    this.jdbcTemplate = new JdbcTemplate(dataSource);
  }

  /**
   * Set the JdbcTemplate to be used by this bean factory.
   * Contains settings for DataSource, SQLExceptionTranslator, etc.
   */
  public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
    Assert.notNull(jdbcTemplate, "JdbcTemplate must not be null");
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Load bean definitions from the database via the given SQL string.
   *
   * @param sql the SQL query to use for loading bean definitions.
   * The first three columns must be bean name, property name and value.
   * Any join and any other columns are permitted: e.g.
   * {@code SELECT BEAN_NAME, PROPERTY, VALUE FROM CONFIG WHERE CONFIG.APP_ID = 1}
   * It's also possible to perform a join. Column names are not significant --
   * only the ordering of these first three columns.
   */
  public void loadBeanDefinitions(String sql) {
    Assert.notNull(this.jdbcTemplate, "Not fully configured - specify DataSource or JdbcTemplate");
    final Properties props = new Properties();
    this.jdbcTemplate.query(sql, rs -> {
      String beanName = rs.getString(1);
      String property = rs.getString(2);
      String value = rs.getString(3);
      // Make a properties entry by combining bean name and property.
      props.setProperty(beanName + '.' + property, value);
    });
    this.propReader.registerBeanDefinitions(props);
  }

}
