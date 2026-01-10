/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.cache.interceptor;

import infra.cache.annotation.CacheEvict;
import infra.cache.annotation.CachePut;
import infra.cache.annotation.Cacheable;
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
