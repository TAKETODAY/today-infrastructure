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

package infra.aop;

import infra.beans.factory.annotation.Autowired;

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
