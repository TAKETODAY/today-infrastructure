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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.orm.mybatis;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import org.apache.ibatis.annotations.Param;

/**
 * @author TODAY <br>
 *         2018-10-10 19:40
 */
public interface DefaultRepository<M, ID extends Serializable> {

    /**
     * Save entity to database
     * 
     * @param model
     *            entity bean
     * @return
     */
    void save(M model);

    /**
     * Save entities to database
     * 
     * @param models
     *            entity beans
     */
    void saveAll(@Param("models") Collection<M> models);

    /**
     * Save entities to database
     * 
     * @param model
     * @return
     */
    void saveSelective(M model);

    /**
     * @param model
     */
    void delete(M model);

    /**
     * 
     * @param model
     */
    void deleteAll(@Param("models") Collection<M> models);

    void deleteAll();

    /**
     * @param id
     */
    void deleteById(ID id);

    /**
     * @param model
     */
    void update(M model);

    /**
     * @param models
     */
    void updateAll(@Param("models") Collection<M> models);

    /**
     * 
     * @return
     */
    int getTotalRecord();

    /**
     * 
     * @param id
     * @return
     */
    M findById(ID id);

    /**
     * @return
     */
    List<M> findAll();

    /**
     * 
     * @param pageNow
     * @param pageSize
     * @return
     */
    List<M> find(@Param("pageNow") int pageNow, @Param("pageSize") int pageSize);

    List<M> findLatest();
}
