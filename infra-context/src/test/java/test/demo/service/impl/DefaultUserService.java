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

package test.demo.service.impl;

import infra.beans.factory.annotation.Autowired;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.stereotype.Service;
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
