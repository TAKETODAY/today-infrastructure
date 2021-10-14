/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.context;

import cn.taketoday.context.annotation.Conditional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import cn.taketoday.context.annotation.Profile;
import cn.taketoday.context.condition.WindowsCondition;
import cn.taketoday.lang.Prototype;
import cn.taketoday.lang.Singleton;
import test.demo.config.User;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Today <br>
 *
 * 2018-11-15 19:59
 */
class ProfileTests {

  static class ProfileTestConfig {

    @Profile("test")
    @Prototype("user")
    public User testUser() {
      return new User().setUserName("TEST");
    }

    @Profile("prod")
    @Singleton("user")
    public User prodUser() {
      return new User().setUserName("PROD");
    }

    @Singleton("yhj")
    @Profile("!test")
    public User yhj() {
      return new User().setUserName("yhj");
    }

    @Singleton("user_windows")
    @Conditional(WindowsCondition.class)
    public User windowsUser() {
      return new User().setUserName("Windows");
    }
  }

  @Test
  void testProfile() throws IOException {
    try (StandardApplicationContext context = new StandardApplicationContext("info.properties")) {

      context.register(ProfileTestConfig.class);
      context.refresh();

      User user = context.getBean("user", User.class);
      System.out.println(user);
      assert "TEST".equals(user.getUserName());
    }
  }

  @Test
  void testConditional() {

    try (StandardApplicationContext context
            = new StandardApplicationContext("info.properties", "test.demo.config")) {
      User yhj = context.getBean("yhj", User.class);
      Assertions.assertThat(yhj).isNull();
      context.register(ProfileTestConfig.class);

      String system = context.getEnvironment().getProperty("os.name");
      if (system != null && system.contains("Windows")) {
        User user = context.getBean("user_windows", User.class);
        assert "Windows".equals(user.getUserName());
      }
      assertThat(context.getEnvironment().getActiveProfiles()).hasSize(2);
    }
  }

}
