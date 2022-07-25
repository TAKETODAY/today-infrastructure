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
package test.demo.service.impl;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.stereotype.Service;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import jakarta.annotation.Resource;
import test.demo.config.Config;
import test.demo.config.User;
import test.demo.repository.UserRepository;
import test.demo.service.UserService;

/**
 * @author Today <br>
 * 2018-11-15 16:52
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
  public DefaultUserService(Config config) {
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
