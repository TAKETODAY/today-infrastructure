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
package cn.taketoday.aop;

import java.util.HashMap;
import java.util.Map;

/**
 * @author TODAY <br>
 * 2018-07-06 17:40:34
 */
public class MemUserDao implements UserDao {

  private Map<String, User> users = new HashMap<>();

  public MemUserDao() {
    users.put("666", new User().setPassword("666"));
    users.put("6666", new User().setPassword("6666"));
    users.put("66666", new User().setPassword("66666"));
    users.put("666666", new User().setPassword("666666"));
    users.put("taketoday@foxmail.com", new User().setPassword("130447AD788ACD4E5A06BF83136E78CB"));
  }

  @Override
  public boolean save(User user) {
    users.put(user.getEmail(), user);
    return true;
  }

  @Override
  public User login(User user) {

    User user_ = users.get(user.getEmail());

    if (user_ == null) {
      return null;
    }
    if (!user_.getPassword().equals(user.getPassword())) {
      return null;
    }
    return user_;
  }

}
