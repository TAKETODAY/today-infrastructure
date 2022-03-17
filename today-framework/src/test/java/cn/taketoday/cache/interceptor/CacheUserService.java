/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.cache.interceptor;

import cn.taketoday.cache.annotation.CacheEvict;
import cn.taketoday.cache.annotation.CachePut;
import cn.taketoday.cache.annotation.Cacheable;
import test.demo.config.User;
import test.demo.repository.impl.DefaultUserRepository;

/**
 * @author TODAY 2021/4/21 21:48
 */
public class CacheUserService {
  DefaultUserRepository userDao = new DefaultUserRepository();

  int accessTime = 0;

  @CachePut(key = "by_id_${user.userId}", condition = "${user.userId.length()==3}")
  public User save(User user) {
    userDao.save(user);
    return user;
  }

  @Cacheable(key = "by_id_${id}")
  public User getUser(String id) {
    accessTime++;
    System.out.println(id);
    return userDao.findUser(id);
  }

  @CacheEvict(key = "by_id_${id}")
  public void evict(String id) {
    userDao.removeUser(id);
  }

  @CacheEvict(key = "by_id_${id}", beforeInvocation = true)
  public void evictBeforeInvocation(String id) {
    userDao.removeUser(id);
  }

  @CacheEvict(key = "by_id_${id}", allEntries = true)
  public void evictAllEntries(String id) {
    userDao.removeUser(id);
  }

  @CacheEvict(key = "by_id_${id}", condition = "${id.length()==3}")
  public void evictConditional(String id) {
    userDao.removeUser(id);
  }

  public int getAccessTime() {
    return accessTime;
  }
}
