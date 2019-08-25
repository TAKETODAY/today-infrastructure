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
package cn.taketoday.jdbc.mapping;

import java.lang.reflect.Method;
import java.util.Arrays;

import cn.taketoday.jdbc.SqlType;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Today <br>
 * 
 *         2018-09-11 11:47
 */
@Getter
@Setter
public final class RepositoryMethod {

    private String sql;

    private Method method;

    private SqlType sqlType;

    private Class<?> returnType;

    private Class<?> genericClass;

    private ParameterMapping[] parameterMapping;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n\t\"sql\":\"").append(sql).append("\",\n\t\"method\":\"").append(method).append("\",\n\t\"sqlType\":\"").append(
                sqlType).append("\",\n\t\"returnType\":\"").append(returnType).append("\",\n\t\"genericClass\":\"").append(
                        genericClass).append("\",\n\t\"parameterMapping\":\"").append(Arrays.toString(parameterMapping)).append("\"\n}");
        return builder.toString();
    }
}
