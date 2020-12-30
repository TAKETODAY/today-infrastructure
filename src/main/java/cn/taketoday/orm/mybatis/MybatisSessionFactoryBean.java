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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.orm.mybatis;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;
import org.apache.ibatis.transaction.TransactionFactory;

import javax.sql.DataSource;

import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.factory.FactoryBean;
import cn.taketoday.context.factory.InitializingBean;

/**
 * @author TODAY <br>
 *         2018-10-13 20:33
 */
public class MybatisSessionFactoryBean implements FactoryBean<SqlSessionFactory>, InitializingBean {

  @Autowired
  private DataSource dataSource;

  @Autowired
  private Configuration configuration;

  private SqlSessionFactory sessionFactory;
  private TransactionFactory transactionFactory;

  public MybatisSessionFactoryBean() {

  }

  public MybatisSessionFactoryBean(DataSource dataSource) {
    this.setDataSource(dataSource);
  }

  @Override
  public SqlSessionFactory getBean() {
    return sessionFactory;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    configuration.setEnvironment(new Environment("TODAY-MYBATIS", getTransactionFactory(), getDataSource()));
    sessionFactory = new DefaultSqlSessionFactory(configuration);
  }

  @Override
  public Class<SqlSessionFactory> getBeanClass() {
    return SqlSessionFactory.class;
  }

  public DataSource getDataSource() {
    if (dataSource == null) {
      throw new ConfigurationException("dataSource must not be null");
    }
    return dataSource;
  }

  public TransactionFactory getTransactionFactory() {
    if (transactionFactory == null) {
      transactionFactory = new DefaultTransactionFactory();
    }
    return transactionFactory;

  }

  public MybatisSessionFactoryBean setTransactionFactory(TransactionFactory transactionFactory) {
    this.transactionFactory = transactionFactory;
    return this;
  }

  public MybatisSessionFactoryBean setDataSource(DataSource dataSource) {
    this.dataSource = dataSource;
    return this;
  }

}
