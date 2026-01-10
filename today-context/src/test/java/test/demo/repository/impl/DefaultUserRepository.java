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

package test.demo.repository.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import infra.beans.factory.BeanNameAware;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.stereotype.Prototype;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import test.demo.config.User;
import test.demo.repository.UserRepository;

/**
 * @author Today <br>
 * 2018-07-06 17:40:34
 */
@Prototype
//@Repository
public class DefaultUserRepository implements UserRepository, BeanNameAware {

  private static final Logger log = LoggerFactory.getLogger(DefaultUserRepository.class);

  private Map<String, User> users = new HashMap<>();

  public DefaultUserRepository() {

  }

  @PostConstruct
  public void initData() {
    log.info("init data: [{}]", this);
    users.put("666", new User(1, "Harry Yang", 20, "666", "666", "男", new Date()));
    users.put("6666", new User(2, "Harry Yang1", 20, "6666", "6666", "男", new Date()));
    users.put("66666", new User(3, "Harry Yang2", 20, "66666", "66666", "男", new Date()));
    users.put("666666", new User(4, "Harry Yang3", 20, "666666", "666666", "男", new Date()));
  }

  @Override
  public boolean save(User user) {

    users.put(user.getUserId(), user);

    return true;
  }

  @PreDestroy
  public void exit() {
    log.info("destory: [{}]", this);
    users = null;
  }

  @Override
  public User login(User user) {
    if (user == null) {
      return null;
    }
    User user_ = users.get(user.getUserId());

    if (user_ == null) {
      return null;
    }
    if (!user_.getPasswd().equals(user.getPasswd())) {
      return null;
    }
    return user_;
  }

  @Override
  public void setBeanName(String name) {
    log.info("[{}] named: [{}]", this, name);
  }

  @Override
  public User findUser(String id) {
    return users.get(id);
  }

  @Override
  public User removeUser(String id) {
    return users.remove(id);
  }
}
