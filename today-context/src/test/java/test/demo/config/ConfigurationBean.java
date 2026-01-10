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

package test.demo.config;

import infra.context.annotation.Configuration;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.stereotype.Singleton;
import jakarta.annotation.PostConstruct;

/**
 * @author Today <br>
 *
 * 2018-09-06 15:30
 */
@Configuration
public class ConfigurationBean {
  private static final Logger log = LoggerFactory.getLogger(ConfigurationBean.class);

  @PostConstruct
  public void init() {
    log.info("ConfigurationBean.init()");
  }

  //  @Prototype("prototype_user")
  public User user() {
    User user = new User();
    user.setId(12);
    return user;
  }

  @Singleton("user__")
  public User user__() {
    User user = new User();
    user.setId(12);
    return user;
  }

}
