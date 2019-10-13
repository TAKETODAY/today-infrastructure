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
package cn.taketoday.jdbc;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ConvertUtils;
import cn.taketoday.jdbc.mapping.ColumnMapping;
import cn.taketoday.jdbc.mapping.TableMapping;

/**
 * @author TODAY <br>
 *         2019-08-18 20:12
 */
public class QueryExecuter extends Executer implements QueryOperation {

    public QueryExecuter() {

    }

    public QueryExecuter(final DataSource dataSource) {
        setDataSource(dataSource);
    }

    @Override
    public <T> List<T> query(final String sql, final RowMapper<T> rowMapper, final Object... args) throws SQLException {

        return query(sql, (ResultSet result) -> {

            final List<T> ret = new ArrayList<>();
            int i = 1;
            while (result.next()) {
                ret.add(rowMapper.mapRow(result, i++));
            }
            return ret;
        }, args);
    }

    @Override
    public <T> T query(final String sql, final ResultSetExtractor<T> rse, final Object... args) throws SQLException {

        return execute((ConnectionCallback<T>) (con) -> {

            try (final PreparedStatement statement = con.prepareStatement(sql)) {
                applyStatementSettings(statement, args);

                try (final ResultSet result = statement.executeQuery()) {
                    return rse.extractData(result);
                }
            }
        });
    }

    @Override
    public void query(final String sql, final ResultSetHandler rch, final Object... args) throws SQLException {

        execute((ConnectionCallback<Void>) (Connection con) -> {

            try (final PreparedStatement statement = con.prepareStatement(sql)) {

                applyStatementSettings(statement, args);

                try (final ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        rch.handleResult(result);
                    }
                }
            }
            return null;
        });
    }

    private static final Map<Class<?>, TableMapping> TABLE_MAPPINGS = new HashMap<>();

    @Override
    public <T> T query(final String sql, final Class<T> requiredType, final Object... args) throws SQLException {

        final class ResultSetExtractor0 implements ResultSetExtractor<T> {

            @Override
            public T extractData(ResultSet rs) throws SQLException {

                if (!rs.next()) {
                    return null;
                }

                final T ret = ClassUtils.newInstance(requiredType);
                final ResultSetMetaData metaData = rs.getMetaData();
                final int columnCount = metaData.getColumnCount() + 1;

                final TableMapping table = TABLE_MAPPINGS.computeIfAbsent(requiredType, TableMapping::new);

                for (int i = 1; i < columnCount; i++) {
                    final ColumnMapping propertyMapping = table.get(JdbcUtils.getColumnName(metaData, i));
                    if (propertyMapping != null) {
                        propertyMapping.resolveResult(ret, rs);
                    }
                }
                return ret;
            }
        }

        return

        query(sql, new ResultSetExtractor0(), args);
    }

    @Override
    public <T> List<T> queryList(final String sql, final Class<T> elementType, final Object... args) throws SQLException {

        return query(sql, (ResultSet result) -> {

            final List<T> ret = new ArrayList<>();

            final ResultSetMetaData metaData = result.getMetaData();
            final int columnCount = metaData.getColumnCount() + 1;

            final class ResultSetExtractor0 implements ResultSetExtractor<T> {

                final Map<String, Field> propertyMap = new HashMap<>();
                {
                    for (final Field field : ClassUtils.getFields(elementType)) {
                        propertyMap.put(field.getName().toUpperCase(), ClassUtils.makeAccessible(field));
                    }
                }

                @Override
                public T extractData(ResultSet rs) throws SQLException {

                    final T ret = ClassUtils.newInstance(elementType);

                    for (int i = 1; i < columnCount; i++) {

                        final Field field = propertyMap.get(JdbcUtils.getColumnName(metaData, i).toUpperCase());
                        if (field != null) {
                            final Class<?> type = field.getType();
                            final Object resultSetValue = JdbcUtils.getResultSetValue(rs, i, type);

                            try {

                                field.set(ret, ConvertUtils.convert(resultSetValue, type));
                            }
                            catch (IllegalArgumentException e) {
                                e.printStackTrace();
                            }
                            catch (IllegalAccessException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    return ret;
                }
            }

            final ResultSetExtractor0 extractor0 = new ResultSetExtractor0();

            while (result.next()) {
                ret.add(extractor0.extractData(result));
            }
            return ret;
        }, args);
    }

    @Override
    public List<Map<String, Object>> queryList(final String sql, final Object... args) throws SQLException {

        return query(sql, (ResultSet result) -> {

            final List<Map<String, Object>> ret = new ArrayList<>();
            final ResultSetMetaData metaData = result.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (result.next()) {
                final Map<String, Object> map = new HashMap<>(columnCount);

                for (int i = 1; i <= columnCount; i++) {
                    map.put(JdbcUtils.getColumnName(metaData, i), JdbcUtils.getResultSetValue(result, i));
                }

                ret.add(map);
            }
            return ret;
        }, args);

    }

    @Override
    public Map<String, Object> queryMap(final String sql, final Object... args) throws SQLException {

        return query(sql, (ResultSet result) -> {

            final ResultSetMetaData metaData = result.getMetaData();
            final int all = metaData.getColumnCount();

            final Map<String, Object> ret = new HashMap<>(all);

            for (int i = 1; i <= all; i++) {
                ret.put(JdbcUtils.getColumnName(metaData, i), JdbcUtils.getResultSetValue(result, i));
            }
            return ret;

        }, args);
    }

}
