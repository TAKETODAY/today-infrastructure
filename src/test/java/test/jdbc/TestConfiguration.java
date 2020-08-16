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

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Primary;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.jdbc.JdbcExecutor;
import cn.taketoday.jdbc.annotation.EnableJdbcDataAccess;
import cn.taketoday.transaction.DataSourceTransactionManager;
import cn.taketoday.transaction.TransactionDefinition;
import cn.taketoday.transaction.TransactionManager;
import cn.taketoday.transaction.TransactionTemplate;

/**
 * @author TODAY <br>
 *         2019-08-19 22:20
 */
@Configuration
@EnableJdbcDataAccess
public class TestConfiguration {

    @Primary
    @Singleton(destroyMethods = "close")
    public DataSource h2DataSource(@Props(prefix = "jdbc.h2.") HikariConfig config) {
        return new HikariDataSource(config);
    }

    @Singleton(destroyMethods = "close")
    public DataSource mySQLDataSource(@Props(prefix = "jdbc.MySQL.") HikariConfig config) {
        return new HikariDataSource(config);
    }

    @Singleton
    public TransactionManager transactionManager(@Autowired("mySQLDataSource") DataSource dataSource) {

        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager();

        transactionManager.setDataSource(dataSource);

        return transactionManager;
    }

    @Singleton
    public TransactionTemplate transactionTemplate(TransactionManager transactionManager) {

        final TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
        transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

        return transactionTemplate;
    }

    @Singleton
    public JdbcExecutor h2Executor(@Autowired("h2DataSource") DataSource dataSource) {
        return new JdbcExecutor(dataSource);
    }

    @Singleton
    public JdbcExecutor mySQLExecutor(@Autowired("mySQLDataSource") DataSource dataSource) {
        return new JdbcExecutor(dataSource);
    }

}
