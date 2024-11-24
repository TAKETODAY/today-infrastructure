/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.orm.mybatis.auto;

import infra.beans.factory.annotation.Autowired;
import infra.orm.mybatis.SqlSessionTemplate;
import infra.orm.mybatis.auto.domain.City;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/5 23:12
 */
public class CityMapperImpl {

  @Autowired
  private SqlSessionTemplate sqlSessionTemplate;

  public City findById(long id) {
    return this.sqlSessionTemplate.selectOne("selectCityById", id);
  }

}

