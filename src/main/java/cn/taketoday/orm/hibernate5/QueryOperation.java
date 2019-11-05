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
package cn.taketoday.orm.hibernate5;

import java.io.Serializable;
import java.util.List;

import javax.persistence.PersistenceException;

import org.hibernate.LockMode;

/**
 * @author TODAY <br>
 * 		   2019-11-05 21:12
 */
public interface QueryOperation<T> {

    T get(Serializable id) throws PersistenceException;

    T get(Serializable id, LockMode lockMode) throws PersistenceException;

    T load(Serializable id) throws PersistenceException;

    T load(Serializable id, LockMode lockMode) throws PersistenceException;

    void load(Object entity, Serializable id) throws PersistenceException;

    boolean contains(Object entity) throws PersistenceException;

    // -------------------

    List<T> findAll() throws PersistenceException;

    Long getTotalRecord() throws PersistenceException;

    default Long getTotalRecord(String condition) throws PersistenceException {
        return getTotalRecord(null, condition);
    }

    default Long getTotalRecord(String condition, Object... params) throws PersistenceException {
        return getTotalRecord(params, condition);
    }

    Long getTotalRecord(Object[] params, String condition) throws PersistenceException;

    // ---------------------------
    List<T> find(int pageNow, int pageSize) throws PersistenceException;

    default List<T> find(int pageNow, int pageSize, String condition) throws PersistenceException {
        return find(pageNow, pageSize, null, condition);
    }

    default List<T> find(int pageNow, int pageSize, String condition, Object... params) throws PersistenceException {
        return find(pageNow, pageSize, params, condition);
    }

    List<T> find(int pageNow, int pageSize, Object[] params, String condition) throws PersistenceException;

    default List<T> find(String queryString) throws PersistenceException {
        return find(null, queryString);
    }

    default List<T> find(String queryString, Object... values) throws PersistenceException {
        return find(values, queryString);
    }

    List<T> find(Object[] values, String queryString) throws PersistenceException;

    // 

    default List<T> orderBy(int pageNow, int pageSize, String by) throws PersistenceException {
        return orderBy(pageNow, pageSize, null, by);
    }

    default List<T> orderBy(int pageNow, int pageSize, String by, Object... params) throws PersistenceException {
        return orderBy(pageNow, pageSize, params, by);
    }

    List<T> orderBy(int pageNow, int pageSize, Object[] params, String by) throws PersistenceException;

    default List<String> query(String hql) throws PersistenceException {
        return query(null, hql);
    }

    default List<String> query(String hql, Object... params) throws PersistenceException {
        return query(params, hql);
    }

    List<String> query(Object[] params, String hql) throws PersistenceException;

    default T uniqueResult(String queryString) {
        return uniqueResult(null, queryString);
    }

    default T uniqueResult(String queryString, Object... params) {
        return uniqueCondition(params, queryString);
    }

    T uniqueResult(Object[] params, String queryString);

    default List<T> findCondition(String condition) throws PersistenceException {
        return findCondition(null, condition);
    }

    default List<T> findCondition(String condition, Object... params) throws PersistenceException {
        return findCondition(params, condition);
    }

    List<T> findCondition(Object[] params, String condition) throws PersistenceException;

    default T uniqueCondition(String condition) {
        return uniqueCondition(null, condition);
    }

    default T uniqueCondition(String condition, Object... params) {
        return uniqueCondition(params, condition);
    }

    T uniqueCondition(Object[] params, String condition);

    default <X> X query(String queryString, Class<X> type) throws PersistenceException {
        return query(queryString, null, type);
    }

    default <X> X query(String queryString, Class<X> type, Object... params) throws PersistenceException {
        return query(queryString, params, type);
    }

    <X> X query(String queryString, Object[] params, Class<X> type) throws PersistenceException;

}
