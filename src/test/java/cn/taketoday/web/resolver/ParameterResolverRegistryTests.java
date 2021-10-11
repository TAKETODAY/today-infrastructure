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

package cn.taketoday.web.resolver;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import cn.taketoday.beans.support.BeanInstantiator;
import cn.taketoday.web.WebNestedRuntimeException;
import cn.taketoday.web.handler.MethodParameter;
import cn.taketoday.web.mock.MockMethodParameter;
import cn.taketoday.web.servlet.StandardWebServletApplicationContext;
import cn.taketoday.web.support.JacksonMessageBodyConverter;
import lombok.Data;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY 2021/9/26 21:47
 */
class ParameterResolverRegistryTests {

  @Data
  public static class UserForm {
    int age;

    String name;

    String[] arr;

    List<String> stringList;

    Map<String, Integer> map;

    UserForm nested;
    List<UserForm> nestedList;
    Map<String, UserForm> nestedMap;

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (!(o instanceof UserForm))
        return false;
      final UserForm userForm = (UserForm) o;
      return age == userForm.age && Objects.equals(name, userForm.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(age, name);
    }

  }

  void test(UserForm user) { }

  void test(List<UserForm> userList, UserForm[] userArray,
            Set<UserForm> userSet, Map<String, UserForm> mapUser) { }

  static final MethodParameter testUser;
  static final MethodParameter testUserSet;
  static final MethodParameter testListUsers;
  static final MethodParameter testUserArray;
  static final MethodParameter testMapUser;

  static final Parameter sharedParameter;

//  void sharedParameter(UserForm user) { }

  static {
    try {
      final Method test = ParameterResolverRegistryTests.class.getDeclaredMethod("test", UserForm.class);
      final Method testList = ParameterResolverRegistryTests.class
              .getDeclaredMethod("test", List.class, UserForm[].class, Set.class, Map.class);

      testUser = new MethodParameter(0, test, "user");

      testListUsers = new MethodParameter(0, testList, "userList");
      testUserArray = new MethodParameter(1, testList, "userArray");
      testUserSet = new MethodParameter(2, testList, "userSet");
      testMapUser = new MethodParameter(3, testList, "mapUser");

      sharedParameter = testUser.getParameter();
    }
    catch (NoSuchMethodException e) {
      throw new WebNestedRuntimeException(e);
    }
  }

  MethodParameter mockParameter(int index, Class<?> type, String name) {
    BeanInstantiator instantiator = BeanInstantiator.forSerialization(MockMethodParameter.class);
    MockMethodParameter parameter = (MockMethodParameter) instantiator.instantiate();

    parameter.setName(name);
    parameter.setParameterClass(type);
    parameter.setParameterIndex(index);

    return parameter;
  }

  MethodParameter mockParameter(Class<?> type, String name) {
    return mockParameter(0, type, name);
  }

  @Test
  void registerDefaults() {

    try (StandardWebServletApplicationContext context = new StandardWebServletApplicationContext()) {

      ParameterResolvingRegistry registry = new ParameterResolvingRegistry();
      registry.setApplicationContext(context);
      registry.setMessageConverter(new JacksonMessageBodyConverter());
      ParameterResolvingStrategies defaultStrategies = registry.getDefaultStrategies();

      assertThat(defaultStrategies).isEmpty();

      registry.registerDefaults(defaultStrategies);
      assertThat(defaultStrategies).isNotEmpty();

      // contains

      assertThat(defaultStrategies.contains(DataBinderParameterResolver.class)).isTrue();
      assertThat(defaultStrategies.contains(ParameterResolvingStrategy.class)).isFalse();
      assertThat(registry.contains(DataBinderParameterResolver.class)).isTrue();
      assertThat(registry.contains(DataBinderParameterResolver.AnnotationBinderParameter.class)).isFalse();
    }

  }

  @Test
  void lookupStrategy() {

    try (StandardWebServletApplicationContext context = new StandardWebServletApplicationContext()) {
      ParameterResolvingRegistry registry = new ParameterResolvingRegistry();
      registry.setApplicationContext(context);
      registry.setMessageConverter(new JacksonMessageBodyConverter());
      registry.registerDefaultParameterResolvers(); // register defaults

      ParameterResolvingStrategy strategy = registry.findStrategy(testUser);
      assertThat(strategy).isNotNull().isInstanceOf(DataBinderParameterResolver.class);

      // mock
      testConverterParameterResolver(registry, int.class);
      testConverterParameterResolver(registry, long.class);
      testConverterParameterResolver(registry, float.class);
      testConverterParameterResolver(registry, short.class);
      testConverterParameterResolver(registry, double.class);
      testConverterParameterResolver(registry, boolean.class);

      testConverterParameterResolver(registry, String.class);
      testConverterParameterResolver(registry, Integer.class);
      testConverterParameterResolver(registry, Long.class);
      testConverterParameterResolver(registry, Short.class);
      testConverterParameterResolver(registry, Double.class);
      testConverterParameterResolver(registry, Boolean.class);
      testConverterParameterResolver(registry, Float.class);

    }
  }

  private void testConverterParameterResolver(ParameterResolvingRegistry registry, Class<?> type) {
    MethodParameter test = mockParameter(type, "test");
    assertThat(registry.findStrategy(test)).isNotNull().isInstanceOf(ConverterParameterResolver.class);
  }

}
