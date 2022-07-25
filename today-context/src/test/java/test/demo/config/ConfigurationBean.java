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
package test.demo.config;

import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.stereotype.Singleton;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
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
    return new User().setId(12);
  }

  @Singleton("user__")
  public User user__() {
    return new User().setId(12);
  }

}
