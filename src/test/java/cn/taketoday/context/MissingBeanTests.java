/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright ©  TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.context;

import org.junit.jupiter.api.Test;

import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import lombok.Getter;
import lombok.Setter;
import test.demo.config.User;

/**
 * @author TODAY <br>
 * 2019-02-01 10:48
 */
@Setter
@Getter
@Configuration
class MissingBeanTests {
  private static final Logger log = LoggerFactory.getLogger(MissingBeanTests.class);

  @Test
  void missingBeanName() {
    try (StandardApplicationContext context = new StandardApplicationContext()) {
      context.register(MissingBeanTests.class);
      context.refresh();

      User bean = context.getBean("user", User.class);

      assert context.getBeanDefinitions().size() == 2;
      assert bean.getUserName().equals("default user");
    }

  }

  @MissingBean(name = "user")
  public User user() {
    return new User().setAge(21).setId(1).setPasswd("666").setUserName("default user");
  }

}
