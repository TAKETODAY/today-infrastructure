/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn Copyright
 * © Today & 2017 - 2018 All Rights Reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.jdbc.config;

import com.zaxxer.hikari.HikariDataSource;

import cn.taketoday.context.annotation.Value;
import cn.taketoday.context.factory.DisposableBean;
import cn.taketoday.context.factory.InitializingBean;

/**
 * @author TODAY <br>
 *         2018-08-03 16:20
 */
public class DefaultDataSource extends HikariDataSource implements InitializingBean, DisposableBean {

    @Value("#{jdbc.url}")
    private String jdbcUrl;

    @Value("#{jdbc.userName}")
    private String userName;

    @Value("#{jdbc.passwd}")
    private String password;

    @Value("600000")
    private long idleTimeout;

    @Value("#{jdbc.driver}")
    private String driverClassName;

    @Value("false")
    private boolean isReadOnly;

    @Value("30000")
    private long connectionTimeout;

    @Value("1800000")
    private long maxLifetime;

    @Value("2")
    private int maxPoolSize;

    public DefaultDataSource() {

    }

    @Override
    public String toString() {
        return " {\"jdbcUrl\":\"" + jdbcUrl + "\",\"username\":\"" + userName + "\",\"password\":\"" + password
                + "\",\"idleTimeout\":\"" + idleTimeout + "\",\"driverClassName\":\"" + driverClassName
                + "\",\"isReadOnly\":\"" + isReadOnly + "\",\"connectionTimeout\":\"" + connectionTimeout
                + "\",\"maxLifetime\":\"" + maxLifetime + "\",\"maxPoolSize\":\"" + maxPoolSize + "\"}";
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        setJdbcUrl(jdbcUrl);
        setUsername(userName);
        setPassword(password);
        setReadOnly(isReadOnly);
        setIdleTimeout(idleTimeout);
        setMaxLifetime(maxLifetime);
        setMaximumPoolSize(maxPoolSize);
        setDriverClassName(driverClassName);
        setConnectionTimeout(connectionTimeout);
    }

    @Override
    public void destroy() throws Exception {
        close();
    }

}
