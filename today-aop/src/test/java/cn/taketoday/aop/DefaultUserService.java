/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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
package cn.taketoday.aop;

import cn.taketoday.beans.factory.annotation.Autowired;

/**
 * @author TODAY <br>
 * 2018-11-11 09:25
 */
public class DefaultUserService implements UserService {

  final private UserDao userDao;

  @Autowired
  public DefaultUserService(UserDao userDao) {
    this.userDao = userDao;
  }

  @TimeAware
  @Logger("登录")
  @Override
  public User login(User user) {
    //		int i = 1 / 0;
    return userDao.login(user);
  }

  @Logger("注册")
  @Override
  public boolean register(User user) {
    return userDao.save(user);
  }

  @Override
  public boolean remove(User user) {

    return true;
  }
}
