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
package test.demo.repository.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.beans.factory.BeanNameAware;
import cn.taketoday.lang.Prototype;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
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
