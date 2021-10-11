/**
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
package test.demo;

import org.junit.jupiter.api.Test;

import java.util.Date;

import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.context.StandardApplicationContext;
import test.demo.config.User;
import test.demo.repository.UserRepository;
import test.demo.repository.impl.DefaultUserRepository;
import test.demo.service.UserService;
import test.demo.service.impl.DefaultUserService;

/**
 * @author TODAY <br>
 * 2019-09-01 11:22
 */
class DemoTest {

  @Test
  void testLogin() throws NoSuchBeanDefinitionException, BeanDefinitionStoreException {

    try (StandardApplicationContext context = //
            new StandardApplicationContext("", "test.demo.service.impl", "test.demo.repository.impl")) {

      UserService userService = context.getBean(DefaultUserService.class);

      UserRepository userDao = context.getBean(UserRepository.class);
      DefaultUserRepository userDaoImpl = context.getBean(DefaultUserRepository.class);

//      Map<String, BeanDefinition> beanDefinitionsMap = applicationContext.getEnvironment().getBeanDefinitionRegistry()
//              .getBeanDefinitions();
//      Set<Entry<String, Object>> entrySet = applicationContext.getSingletons().entrySet();
//      for (Entry<String, Object> entry : entrySet) {
//        System.err.println(entry.getKey() + " == " + entry.getValue());
//      }
//      Iterator<Entry<String, BeanDefinition>> iterator = beanDefinitionsMap.entrySet().iterator();
//      while (iterator.hasNext()) {
//        Entry<String, BeanDefinition> entry = iterator.next();
//        System.err.println(entry.getKey() + "\n" + entry.getValue());
//      }
//
//      System.out.println(userDao);
//      System.out.println(userDaoImpl);

      assert userDao != userDaoImpl;

      User login = userService.login(new User(1, "TODAY", 20, "666", "666", "男", new Date()));

      assert login != null : "Login failed";
    }
  }
}
