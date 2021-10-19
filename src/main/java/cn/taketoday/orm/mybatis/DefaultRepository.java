/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

import org.apache.ibatis.annotations.Param;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * @author TODAY 2018-10-10 19:40
 */
public interface DefaultRepository<M, Id extends Serializable> {

  /**
   * Save entity to database
   *
   * @param model entity bean
   */
  void save(M model);

  /**
   * Save entities to database
   *
   * @param models entity beans
   */
  void saveAll(@Param("models") Collection<M> models);

  /**
   * Save entities to database
   *
   * @param model
   */
  void saveSelective(M model);

  /**
   * @param model
   */
  void delete(M model);

  /**
   * @param models
   */
  void deleteAll(@Param("models") Collection<M> models);

  void deleteAll();

  /**
   * @param id
   */
  void deleteById(Id id);

  /**
   * @param model
   */
  void update(M model);

  /**
   * @param models
   */
  void updateAll(@Param("models") Collection<M> models);

  int getTotalRecord();

  M findById(Id id);

  List<M> findAll();

  List<M> find(@Param("pageNow") int pageNow, @Param("pageSize") int pageSize);

  List<M> findLatest();
}
