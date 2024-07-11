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

package cn.taketoday.persistence.query;

import org.junit.jupiter.api.TestInstance;

import cn.taketoday.jdbc.NamedQuery;
import cn.taketoday.jdbc.RepositoryManager;
import cn.taketoday.persistence.AbstractRepositoryManagerTests;
import cn.taketoday.persistence.DefaultEntityManager;
import cn.taketoday.persistence.model.UserModel;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0 2024/5/24 16:12
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MapperFactoryTests extends AbstractRepositoryManagerTests {

  @Override
  protected void prepareTestsData(DbType dbType, RepositoryManager repositoryManager) {
    try (NamedQuery query = repositoryManager.createNamedQuery("""
            drop table if exists t_user;
            create table t_user
            (
                `id`               int auto_increment primary key,
                `age`              int           default 0    comment 'Age',
                `name`             varchar(255)  default null comment '用户名',
                `avatar`           mediumtext    default null comment '头像',
                `password`         varchar(255)  default null comment '密码',
                `introduce`        varchar(1000) default null comment '介绍',
                `email`            varchar(255)  default null comment 'email',
                `gender`           int           default -1   comment '性别',
                `mobile_phone`     varchar(36)   default null comment '手机号'
            );
            """)) {

      query.executeUpdate();
    }
  }

  @ParameterizedRepositoryManagerTest
  void queryMapper(RepositoryManager repositoryManager) {
    DefaultEntityManager entityManager = new DefaultEntityManager(repositoryManager);
    entityManager.persist(UserModel.male("today", 1));
    MapperFactory mapperFactory = new MapperFactory(repositoryManager);

    UserMapper userMapper = mapperFactory.getMapper(UserMapper.class);

    UserModel byId = userMapper.findById(1);
    System.out.println(byId);
  }

}
