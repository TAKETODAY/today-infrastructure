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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.ConfigurableBeanFactory;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.lang.Configuration;
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
public class MissingBeanTest {
  private static final Logger log = LoggerFactory.getLogger(MissingBeanTest.class);

  private long start;

  private static ConfigurableApplicationContext applicationContext = ///
          new StandardApplicationContext(MissingBeanTest.class);

  private String process;

  @Setter
  @Getter
  private static ConfigurableBeanFactory beanFactory;

  static {
    setBeanFactory(getApplicationContext().getBeanFactory());
  }

  public static ConfigurableApplicationContext getApplicationContext() {
    return applicationContext;
  }

  @AfterAll
  public static void endClass() {
    ConfigurableApplicationContext applicationContext = getApplicationContext();
    if (applicationContext != null) {
      applicationContext.close();
    }
  }

  @Test
  void missingBeanName() {

    setProcess("test missing user bean");

    ConfigurableApplicationContext applicationContext = getApplicationContext();

    User bean = applicationContext.getBean("user", User.class);

    System.err.println(applicationContext.getBeanDefinitions());

    assert applicationContext.getBeanDefinitions().size() == 2;
    assert bean.getUserName().equals("default user");

    System.err.println(bean);
    System.err.println(bean.getUserName());
  }

  @MissingBean("user")
  public User user() {
    return new User().setAge(21).setId(1).setPasswd("666").setUserName("default user");
  }

}
