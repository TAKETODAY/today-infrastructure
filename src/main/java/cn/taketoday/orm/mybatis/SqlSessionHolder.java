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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.orm.mybatis;

import cn.taketoday.transaction.AbstractResourceHolder;

import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;

import lombok.Getter;

/**
 * @author TODAY <br>
 *         2018-10-09 11:24
 */
@Getter
public class SqlSessionHolder extends AbstractResourceHolder {

    private final SqlSession sqlSession;
    private final ExecutorType executorType;

    public SqlSessionHolder(SqlSession sqlSession, ExecutorType executorType) {
        this.sqlSession = sqlSession;
        this.executorType = executorType;
    }

}
