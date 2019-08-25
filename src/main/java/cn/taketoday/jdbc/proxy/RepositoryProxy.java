/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Today & 2017 - 2018 All Rights Reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.jdbc.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;

import javax.sql.DataSource;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.jdbc.mapping.RepositoryMethod;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Today <br>
 * 
 *         2018-09-11 21:19
 */
@Slf4j
public class RepositoryProxy implements InvocationHandler {

    private final Class<?> repository;

    private final Map<Method, RepositoryMethod> methodCache;

    private ApplicationContext applicationContext;

    public RepositoryProxy(Class<?> repository, Map<Method, RepositoryMethod> repositoryMethods, ApplicationContext applicationContext) {
        this.repository = repository;
        this.methodCache = repositoryMethods;
        this.applicationContext = applicationContext;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        switch (method.getName())
        {
            case "toString" :
                return toString();
            case "hashCode" :
                return Objects.hashCode(proxy);
        }
        RepositoryMethod repositoryMethod = methodCache.get(method);

        String sql = repositoryMethod.getSql();

        log.debug("SQL -> {} ", sql);
        DataSource bean = applicationContext.getBean("DefaultDataSource", DataSource.class);
//		QueryImpl<?> query = new QueryImpl(bean.getConnection(), repositoryMethod.getReturnType());
//		query.sql.append(sql);
        return null;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"repository\":\"").append(repository).append("\"}");
        return builder.toString();
    }

}
