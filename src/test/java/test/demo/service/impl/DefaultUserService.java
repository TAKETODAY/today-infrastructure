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
package test.demo.service.impl;

import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.annotation.Service;
import lombok.extern.slf4j.Slf4j;
import test.demo.config.Config;
import test.demo.config.User;
import test.demo.repository.UserRepository;
import test.demo.service.UserService;

/**
 * 
 * @author Today <br>
 *         2018-11-15 16:52
 */
@Slf4j
@Service
public class DefaultUserService implements UserService {

	@Autowired
	private UserRepository userRepository;

//	@Autowired
//	public DefaultUserService(@Autowired(required = true) UserRepository userDao, @Props(prefix = "site.") Config config) {
//		this.userRepository = userDao;
//	}

	@Autowired
	public DefaultUserService(@Props(prefix = "site.") Config config) {
		log.info("Creating 'UserService'");
	}

	@Override
	public User login(User user) {
		return userRepository.login(user);
	}

	@Override
	public boolean register(User user) {
		return userRepository.save(user);
	}

}
