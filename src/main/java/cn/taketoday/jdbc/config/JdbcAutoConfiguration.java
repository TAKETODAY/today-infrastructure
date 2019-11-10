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

import static cn.taketoday.jdbc.mapping.result.DelegatingResultResolver.delegate;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.Ordered;
import cn.taketoday.context.annotation.Order;
import cn.taketoday.context.event.LoadingMissingBeanEvent;
import cn.taketoday.context.listener.ApplicationListener;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.OrderUtils;
import cn.taketoday.jdbc.mapping.ColumnMapping;
import cn.taketoday.jdbc.mapping.result.ResultResolver;

/**
 * @author TODAY <br>
 *         2019-08-24 12:01
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class JdbcAutoConfiguration implements ApplicationListener<LoadingMissingBeanEvent> {

    @Override
    public void onApplicationEvent(LoadingMissingBeanEvent event) {

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

        // Byte[] byte[] int long float double short byte boolean BigDecimal, BigInteger
        // ------------------------------------------------------------------------------

        resultResolvers.add(delegate(p -> p.is(byte[].class), (rs, i) -> rs.getBytes(i)));
        resultResolvers.add(delegate(p -> p.is(BigDecimal.class), (rs, i) -> rs.getBigDecimal(i)));
        resultResolvers.add(delegate(p -> p.is(int.class) || p.is(Integer.class), (rs, i) -> rs.getInt(i)));
        resultResolvers.add(delegate(p -> p.is(byte.class) || p.is(Byte.class), (rs, i) -> rs.getByte(i)));
        resultResolvers.add(delegate(p -> p.is(long.class) || p.is(Long.class), (rs, i) -> rs.getLong(i)));
        resultResolvers.add(delegate(p -> p.is(short.class) || p.is(Short.class), (rs, i) -> rs.getShort(i)));
        resultResolvers.add(delegate(p -> p.is(float.class) || p.is(Float.class), (rs, i) -> rs.getFloat(i)));
        resultResolvers.add(delegate(p -> p.is(double.class) || p.is(Double.class), (rs, i) -> rs.getDouble(i)));
        resultResolvers.add(delegate(p -> p.is(boolean.class) || p.is(Boolean.class), (rs, i) -> rs.getBoolean(i)));
        resultResolvers.add(delegate(p -> p.is(char.class) || p.is(Character.class), (rs, i) -> {
            final String v = rs.getString(i);
            return v == null ? null : Character.valueOf(v.charAt(0));
        }));
        resultResolvers.add(delegate(p -> p.is(BigInteger.class), (rs, i) -> {
            final BigDecimal b = rs.getBigDecimal(i);
            return b == null ? null : b.toBigInteger();
        }));
        resultResolvers.add(delegate(p -> p.is(Byte[].class), (rs, i) -> {
            final byte[] bytes = rs.getBytes(i);
            if (bytes == null) {
                return null;
            }
            final Byte[] ret = new Byte[bytes.length];
            for (int j = 0; j < bytes.length; j++) {
                ret[j] = bytes[j];
            }
            return ret;
        }));
        // String
        // -------------------------------------

        resultResolvers.add(delegate(p -> p.is(Clob.class), (rs, i) -> rs.getClob(i)));
        resultResolvers.add(delegate(p -> p.is(String.class), (rs, i) -> rs.getString(i)));
        resultResolvers.add(delegate(p -> p.is(StringBuffer.class), (rs, i) -> new StringBuffer(rs.getString(i))));
        resultResolvers.add(delegate(p -> p.is(StringBuilder.class), (rs, i) -> new StringBuilder(rs.getString(i))));

        // SQL API
        // -------------------------------------
        resultResolvers.add(delegate(p -> p.is(Blob.class), (rs, i) -> rs.getBlob(i)));
        resultResolvers.add(delegate(p -> p.is(Time.class), (rs, i) -> rs.getTime(i)));
        resultResolvers.add(delegate(p -> p.is(Timestamp.class), (rs, i) -> rs.getTimestamp(i)));
        resultResolvers.add(delegate(p -> p.is(Date.class) || p.is(java.sql.Date.class), (rs, i) -> rs.getDate(i)));

        resultResolvers.add(delegate(p -> p.is(InputStream.class), (rs, i) -> {
            final Blob b = rs.getBlob(i);
            return b == null ? null : b.getBinaryStream();
        }));
        resultResolvers.add(delegate(p -> p.is(Reader.class), (rs, i) -> {
            final Clob c = rs.getClob(i);
            return c == null ? null : c.getCharacterStream();
        }));

        // jdk 1.8 Date and time API
        // -------------------------------------
        resultResolvers.add(delegate(p -> p.is(Instant.class), (rs, i) -> {
            final Timestamp tp = rs.getTimestamp(i);
            return tp == null ? null : tp.toInstant();
        }));
        resultResolvers.add(delegate(p -> p.is(LocalDateTime.class), (rs, i) -> {
            final Timestamp tp = rs.getTimestamp(i);
            return tp == null ? null : tp.toLocalDateTime();
        }));
        resultResolvers.add(delegate(p -> p.is(LocalDate.class), (rs, i) -> {
            final java.sql.Date d = rs.getDate(i);
            return d == null ? null : d.toLocalDate();
        }));
        resultResolvers.add(delegate(p -> p.is(LocalTime.class), (rs, i) -> {
            final Time t = rs.getTime(i);
            return t == null ? null : t.toLocalTime();
        }));
        resultResolvers.add(delegate(p -> p.is(OffsetDateTime.class), (rs, i) -> {
            final Timestamp tp = rs.getTimestamp(i);
            return tp == null ? null : OffsetDateTime.ofInstant(tp.toInstant(), ZoneId.systemDefault());
        }));
        resultResolvers.add(delegate(p -> p.is(OffsetTime.class), (rs, i) -> {
            final Time t = rs.getTime(i);
            return t == null ? null : t.toLocalTime().atOffset(OffsetTime.now().getOffset());
        }));
        resultResolvers.add(delegate(p -> p.is(ZonedDateTime.class), (rs, i) -> {
            final Timestamp tp = rs.getTimestamp(i);
            return tp == null ? null : ZonedDateTime.ofInstant(tp.toInstant(), ZoneId.systemDefault());
        }));
        resultResolvers.add(delegate(p -> p.is(Year.class), (rs, i) -> {
            final int year = rs.getInt(i);
            return year == 0 ? null : Year.of(year);
        }));
        resultResolvers.add(delegate(p -> p.is(Month.class), (rs, i) -> {
            final int month = rs.getInt(i);
            return month == 0 ? null : Month.of(month);
        }));
        resultResolvers.add(delegate(p -> p.is(YearMonth.class), (rs, i) -> {
            final String value = rs.getString(i);
            return value == null ? null : YearMonth.parse(value);
        }));

        // TODO Enums

        // User
        // -------------------------------------

        jdbcConfiguration.configureResultResolver(resultResolvers);
        OrderUtils.reversedSort(resultResolvers);

        ColumnMapping.addResolver(resultResolvers);
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
