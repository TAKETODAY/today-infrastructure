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
package test.demo;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Date;

import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.context.support.ApplicationPropertySourcesProcessor;
import cn.taketoday.context.support.StandardApplicationContext;
import test.demo.config.User;
import test.demo.repository.UserRepository;
import test.demo.repository.impl.DefaultUserRepository;
import test.demo.service.UserService;
import test.demo.service.impl.DefaultUserService;

/**
 * @author TODAY <br>
 * 2019-09-01 11:22
 */
class DemoTests {

  @Test
  void testLogin() throws NoSuchBeanDefinitionException, BeanDefinitionStoreException, IOException {

    try (StandardApplicationContext context = new StandardApplicationContext()) {

      ApplicationPropertySourcesProcessor processor = new ApplicationPropertySourcesProcessor(context);
      processor.setPropertiesLocation("info.properties");
      processor.postProcessEnvironment();
      context.scan("test.demo.service.impl", "test.demo.repository.impl");

      context.refresh();

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

  static class BeanTEST {

    public BeanTEST newInstance(int name) {
      return new BeanTEST();
    }

    public BeanTEST newInstance(String name) {
      return new BeanTEST();
    }

    public static BeanTEST newInstance() {
      return new BeanTEST();
    }

  }

  //  @Test
  void test() {
    boolean isStatic = true;
    Method[] candidates = BeanTEST.class.getDeclaredMethods();
    Method uniqueCandidate = null;
    for (Method candidate : candidates) {
      if (Modifier.isStatic(candidate.getModifiers()) == isStatic && isFactoryMethod(candidate)) {
        if (uniqueCandidate == null) {
          uniqueCandidate = candidate;
        }
        else if (isParamMismatch(uniqueCandidate, candidate)) {
          uniqueCandidate = null;
          break;
        }
      }
    }

    System.out.println(uniqueCandidate);
  }

  private boolean isFactoryMethod(Method candidate) {
    return candidate.getName().equals("newInstance");
  }

  private boolean isParamMismatch(Method uniqueCandidate, Method candidate) {
    int uniqueCandidateParameterCount = uniqueCandidate.getParameterCount();
    int candidateParameterCount = candidate.getParameterCount();
    return (uniqueCandidateParameterCount != candidateParameterCount ||
            !Arrays.equals(uniqueCandidate.getParameterTypes(), candidate.getParameterTypes()));
  }

}
