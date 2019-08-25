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

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;

import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.context.factory.DisposableBean;
import cn.taketoday.context.factory.InitializingBean;
import cn.taketoday.jdbc.JdbcExecuter;
import cn.taketoday.jdbc.JdbcTransactionManager;
import cn.taketoday.transaction.TransactionDefinition;
import cn.taketoday.transaction.manager.TransactionManager;
import cn.taketoday.transaction.support.TransactionTemplate;
import lombok.Getter;

/**
 * @author TODAY <br>
 *         2019-08-19 22:20
 */
@Getter
@Configuration
@Props(prefix = { "jdbc.", "jdbc.pool." })
public class TestConfiguration extends HikariDataSource implements InitializingBean, DisposableBean {

    private String url;
    private String passwd;
    private String driver;
    private String userName;

    private int minIdle = 0;
    private int maxPoolSize = 15;
    private long idleTimeout = 600000;
    private boolean isReadOnly = false;
    private long maxLifetime = 1800000;
    private long connectionTimeout = 30000;

    @Override
    public void afterPropertiesSet() throws Exception {
        setJdbcUrl(url);
        setPassword(passwd);
        setUsername(userName);
        setMinimumIdle(minIdle);
        setReadOnly(isReadOnly);
        setDriverClassName(driver);
        setIdleTimeout(idleTimeout);
        setMaxLifetime(maxLifetime);
        setMaximumPoolSize(maxPoolSize);
        setConnectionTimeout(connectionTimeout);
    }

    @Override
    public void destroy() throws Exception {
        close();
    }

    @Singleton
    public TransactionManager transactionManager() {

        JdbcTransactionManager mybatisTransactionManager = new JdbcTransactionManager();

        mybatisTransactionManager.setDataSource(this);

        return mybatisTransactionManager;
    }

    @Singleton
    public TransactionTemplate transactionTemplate(TransactionManager transactionManager) {

        final TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
        transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

        return transactionTemplate;
    }

    @Singleton
    public JdbcExecuter jdbcExecuter(DataSource dataSource) {
        return new JdbcExecuter(dataSource);
    }

}
