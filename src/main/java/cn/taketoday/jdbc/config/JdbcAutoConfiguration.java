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
package cn.taketoday.jdbc.config;

import static cn.taketoday.jdbc.mapping.result.DelegatingResultResolver.createDelegate;

import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import org.slf4j.LoggerFactory;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.Ordered;
import cn.taketoday.context.annotation.Order;
import cn.taketoday.context.event.ContextStartedEvent;
import cn.taketoday.context.listener.ApplicationListener;
import cn.taketoday.context.utils.OrderUtils;
import cn.taketoday.jdbc.mapping.result.ResultResolver;

/**
 * @author TODAY <br>
 *         2019-08-24 12:01
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class JdbcAutoConfiguration implements ApplicationListener<ContextStartedEvent> {

    @Override
    public void onApplicationEvent(ContextStartedEvent event) {

        LoggerFactory.getLogger(getClass()).info("Preparing TODAY Jdbc Environment");

        final ApplicationContext applicationContext = event.getApplicationContext();

        final JdbcConfiguration jdbcConfiguration = getJdbcConfiguration(applicationContext);

        configureResultResolver(applicationContext, jdbcConfiguration);
    }

    protected JdbcConfiguration getJdbcConfiguration(ApplicationContext applicationContext) {
        return new CompositeJdbcConfiguration(applicationContext.getBeans(JdbcConfiguration.class));
    }

    protected void configureResultResolver(ApplicationContext applicationContext, JdbcConfiguration jdbcConfiguration) {

        final List<ResultResolver> resultResolvers = applicationContext.getBeans(ResultResolver.class);

        // byte[] int long float double short byte boolean BigDecimal
        // -------------------------------------------------------

        resultResolvers.add(createDelegate(p -> p.is(byte[].class), (rs, i) -> rs.getBytes(i)));
        resultResolvers.add(createDelegate(p -> p.is(int.class) || p.is(Integer.class), (rs, i) -> rs.getInt(i)));
        resultResolvers.add(createDelegate(p -> p.is(byte.class) || p.is(Byte.class), (rs, i) -> rs.getByte(i)));
        resultResolvers.add(createDelegate(p -> p.is(long.class) || p.is(Long.class), (rs, i) -> rs.getLong(i)));
        resultResolvers.add(createDelegate(p -> p.is(short.class) || p.is(Short.class), (rs, i) -> rs.getShort(i)));
        resultResolvers.add(createDelegate(p -> p.is(float.class) || p.is(Float.class), (rs, i) -> rs.getFloat(i)));
        resultResolvers.add(createDelegate(p -> p.is(double.class) || p.is(Double.class), (rs, i) -> rs.getDouble(i)));
        resultResolvers.add(createDelegate(p -> p.is(boolean.class) || p.is(Boolean.class), (rs, i) -> rs.getBoolean(i)));
        resultResolvers.add(createDelegate(p -> p.is(BigDecimal.class), (rs, i) -> rs.getBigDecimal(i)));

        // String
        // -------------------------------------

        resultResolvers.add(createDelegate(p -> p.is(Clob.class), (rs, i) -> rs.getClob(i)));
        resultResolvers.add(createDelegate(p -> p.is(String.class), (rs, i) -> rs.getString(i)));
        resultResolvers.add(createDelegate(p -> p.is(StringBuffer.class), (rs, i) -> new StringBuffer(rs.getString(i))));
        resultResolvers.add(createDelegate(p -> p.is(StringBuilder.class), (rs, i) -> new StringBuilder(rs.getString(i))));

        // SQL API
        // -------------------------------------
        resultResolvers.add(createDelegate(p -> p.is(Blob.class), (rs, i) -> rs.getBlob(i)));
        resultResolvers.add(createDelegate(p -> p.is(Time.class), (rs, i) -> rs.getTime(i)));
        resultResolvers.add(createDelegate(p -> p.is(Timestamp.class), (rs, i) -> rs.getTimestamp(i)));
        resultResolvers.add(createDelegate(p -> p.is(Date.class) || p.is(java.sql.Date.class), (rs, i) -> rs.getDate(i)));

        // -------------------------------------
        jdbcConfiguration.configureResultResolver(resultResolvers);
        OrderUtils.reversedSort(resultResolvers);
    }

    /**
     * All {@link JdbcConfiguration} beans
     * 
     * @author TODAY <br>
     *         2019-08-24 13:27
     */
    protected class CompositeJdbcConfiguration implements JdbcConfiguration {

        private final List<JdbcConfiguration> jdbcConfigurations;

        public CompositeJdbcConfiguration(List<JdbcConfiguration> jdbcConfigurations) {
            OrderUtils.reversedSort(jdbcConfigurations);
            this.jdbcConfigurations = jdbcConfigurations;
        }

        protected List<JdbcConfiguration> getJdbcConfigurations() {
            return jdbcConfigurations;
        }

        @Override
        public void configureResultResolver(List<ResultResolver> resolvers) {
            for (final JdbcConfiguration configuration : getJdbcConfigurations()) {
                configuration.configureResultResolver(resolvers);
            }
        }
    }

}
