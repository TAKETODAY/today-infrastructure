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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.orm.mybatis;

import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.context.factory.FactoryBean;
import cn.taketoday.context.factory.InitializingBean;

import javax.sql.DataSource;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;
import org.apache.ibatis.transaction.TransactionFactory;

/**
 * @author Today <br>
 * 
 *         2018-10-13 20:33
 */
@Singleton
public class SessionFactoryBean implements FactoryBean<SqlSessionFactory>, InitializingBean {

    @Autowired
    private Configuration configuration;

    @Autowired
    private TransactionFactory transactionFactory;

    @Autowired
    private DataSource dataSource;

    private SqlSessionFactory sessionFactory;

    @Override
    public SqlSessionFactory getBean() {
        return sessionFactory;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        configuration.setEnvironment(new Environment("TODAY-MYBATIS", transactionFactory, dataSource));
        sessionFactory = new DefaultSqlSessionFactory(configuration);
    }

    @Override
    public Class<SqlSessionFactory> getBeanClass() {
        return SqlSessionFactory.class;
    }

}
