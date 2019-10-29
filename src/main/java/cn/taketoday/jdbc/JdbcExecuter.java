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

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import lombok.Getter;
import lombok.Setter;

/**
 * @author TODAY <br>
 *         2019-08-18 19:20
 */
@Setter
@Getter
public class JdbcExecuter implements JdbcOperations {

    private final QueryOperation queryOperation;
    private final BasicOperation basicOperation;
    private final UpdateOperation updateOperation;

    public JdbcExecuter(DataSource dataSource) {
        final QueryExecuter queryExecuter = new QueryExecuter(dataSource);

        this.queryOperation = queryExecuter;
        this.basicOperation = queryExecuter;

        this.updateOperation = new UpdateExecuter(dataSource);
    }

    public JdbcExecuter(BasicOperation basicOperation, QueryOperation queryOperation, UpdateOperation updateOperation) {
        this.queryOperation = queryOperation;
        this.basicOperation = basicOperation;
        this.updateOperation = updateOperation;
    }

    @Override
    public <T> T execute(ConnectionCallback<T> action) throws SQLException {
        return getBasicOperation().execute(action);
    }

    @Override
    public <T> T execute(StatementCallback<T> action) throws SQLException {
        return getBasicOperation().execute(action);
    }

    @Override
    public void execute(String sql) throws SQLException {
        getBasicOperation().execute(sql);
    }

    @Override
    public <T> T execute(String sql, PreparedStatementCallback<T> action) throws SQLException {
        return getBasicOperation().execute(sql, action);
    }

    @Override
    public <T> T execute(String sql, CallableStatementCallback<T> action) throws SQLException {
        return getBasicOperation().execute(sql, action);
    }

    @Override
    public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object[] args) throws SQLException {
        return getQueryOperation().query(sql, rowMapper, args);
    }

    @Override
    public <T> T query(String sql, ResultSetExtractor<T> rse, Object[] args) throws SQLException {
        return getQueryOperation().query(sql, rse, args);
    }

    @Override
    public void query(String sql, ResultSetHandler rch, Object[] args) throws SQLException {
        getQueryOperation().query(sql, rch, args);
    }

    @Override
    public <T> T query(String sql, Class<T> requiredType, Object[] args) throws SQLException {
        return getQueryOperation().query(sql, requiredType, args);
    }

    @Override
    public <T> List<T> queryList(String sql, Class<T> elementType, Object[] args) throws SQLException {
        return getQueryOperation().queryList(sql, elementType, args);
    }

    @Override
    public List<Map<String, Object>> queryList(String sql, Object[] args) throws SQLException {
        return getQueryOperation().queryList(sql, args);
    }

    @Override
    public Map<String, Object> queryMap(String sql, Object[] args) throws SQLException {
        return getQueryOperation().queryMap(sql, args);
    }

    @Override
    public int update(String sql) throws SQLException {
        return getUpdateOperation().update(sql);
    }

    @Override
    public int update(String sql, Object[] args) throws SQLException {
        return getUpdateOperation().update(sql, args);
    }

    @Override
    public int[] batchUpdate(String... sql) throws SQLException {
        return getUpdateOperation().batchUpdate(sql);
    }

    @Override
    public int[] batchUpdate(String sql, List<Object[]> batchArgs) throws SQLException {
        return getUpdateOperation().batchUpdate(sql, batchArgs);
    }

}
