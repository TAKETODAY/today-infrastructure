/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

import javax.annotation.Resource;

import cn.taketoday.beans.Autowired;
import cn.taketoday.context.Props;
import cn.taketoday.beans.Service;
import cn.taketoday.logger.Logger;
import cn.taketoday.logger.LoggerFactory;
import test.demo.config.Config;
import test.demo.config.User;
import test.demo.repository.UserRepository;
import test.demo.service.UserService;

/**
 *
 * @author Today <br>
 *         2018-11-15 16:52
 */
@Service
public class DefaultUserService implements UserService {
    private static final Logger log = LoggerFactory.getLogger(DefaultUserService.class);

    @Resource
//	@Autowired
//	@Inject
    private UserRepository userRepository;

    private UserRepository userRepository1;

//	@Autowired
//	public DefaultUserService(@Autowired(required = true) UserRepository userDao, @Props(prefix = "site.") Config config) {
//		this.userRepository = userDao;
//	}

    @Autowired
    public DefaultUserService(@Props(prefix = "site.") Config config) {
        log.info("Creating 'userService'");
    }

    @Override
    public User login(User user) {
        return userRepository.login(user);
    }

    @Override
    public boolean register(User user) {
        return userRepository.save(user);
    }

    public UserRepository getUserRepository() {
        return userRepository1;
    }

    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Autowired
//  @Autowired
//  @Inject
    public void setUserRepository1(UserRepository userRepository1) {
        this.userRepository1 = userRepository1;
    }

}
