/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Today & 2017 - 2018 All Rights Reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.test.dao.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.context.annotation.Repository;
import cn.taketoday.test.dao.UserDao;
import cn.taketoday.test.domain.User;

/**
 * 
 * @author Today <br>
 *         2018-07-06 17:40:34
 */
@Repository
public final class UserDaoImpl implements UserDao {

	private Map<String, User> users = new HashMap<>();

	public UserDaoImpl() {
		users.put("666", new User(1, "杨海健", 20, "666", "666", "男", new Date()));
		users.put("6666", new User(2, "杨海健1", 20, "6666", "6666", "男", new Date()));
		users.put("66666", new User(3, "杨海健2", 20, "66666", "66666", "男", new Date()));
		users.put("666666", new User(4, "杨海健3", 20, "666666", "666666", "男", new Date()));
	}

	@Override
	public boolean save(User user) {

		users.put(user.getUserId(), user);

		return true;
	}

	@Override
	public User login(User user) {

		User user_ = users.get(user.getUserId());

		if (user_ == null) {
			return null;
		}
		if (!user_.getPasswd().equals(user.getPasswd())) {
			return null;
		}
		return user_;
	}
}
