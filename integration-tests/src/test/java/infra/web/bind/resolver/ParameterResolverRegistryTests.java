/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.web.bind.resolver;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import infra.beans.support.BeanInstantiator;
import infra.core.MethodParameter;
import infra.core.annotation.SynthesizingMethodParameter;
import infra.mock.web.MockContextImpl;
import infra.web.InternalServerException;
import infra.web.handler.method.ModelAttributeMethodProcessor;
import infra.web.handler.method.ResolvableMethodParameter;
import infra.web.mock.support.AnnotationConfigWebApplicationContext;
import infra.web.MockMethodParameter;
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
      if (!(o instanceof final UserForm userForm))
        return false;
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

  static final ResolvableMethodParameter testUser;
  static final ResolvableMethodParameter testUserSet;
  static final ResolvableMethodParameter testListUsers;
  static final ResolvableMethodParameter testUserArray;
  static final ResolvableMethodParameter testMapUser;

  static final MethodParameter sharedParameter;

//  void sharedParameter(UserForm user) { }

  static {
    try {
      final Method test = ParameterResolverRegistryTests.class.getDeclaredMethod("test", UserForm.class);
      final Method testList = ParameterResolverRegistryTests.class
              .getDeclaredMethod("test", List.class, UserForm[].class, Set.class, Map.class);

      testUser = createParameter(0, test, "user");

      testListUsers = createParameter(0, testList, "userList");
      testUserArray = createParameter(1, testList, "userArray");
      testUserSet = createParameter(2, testList, "userSet");
      testMapUser = createParameter(3, testList, "mapUser");

      sharedParameter = testUser.getParameter();
    }
    catch (NoSuchMethodException e) {
      throw new InternalServerException(null, e);
    }
  }

  ResolvableMethodParameter mockParameter(int index, Class<?> type, String name) {
    BeanInstantiator instantiator = BeanInstantiator.forSerialization(MockMethodParameter.class);
    MockMethodParameter parameter = (MockMethodParameter) instantiator.instantiate();

    parameter.setName(name);
    parameter.setParameterClass(type);
    parameter.setParameterIndex(index);
    parameter.setParameter(sharedParameter);
    return parameter;
  }

  ResolvableMethodParameter mockParameter(Class<?> type, String name) {
    return mockParameter(0, type, name);
  }

  @Test
  void registerDefaults() {

    var context = new AnnotationConfigWebApplicationContext();
    context.refresh();
    context.setMockContext(new MockContextImpl());

    ParameterResolvingRegistry registry = new ParameterResolvingRegistry();
    registry.setApplicationContext(context);
    ParameterResolvingStrategies defaultStrategies = registry.getDefaultStrategies();

    assertThat(defaultStrategies).isEmpty();

    registry.registerDefaultStrategies(defaultStrategies);
    assertThat(defaultStrategies).isNotEmpty();

    // contains

    assertThat(defaultStrategies.contains(ModelMethodProcessor.class)).isTrue();
    assertThat(defaultStrategies.contains(ParameterResolvingStrategy.class)).isFalse();
    assertThat(registry.contains(ModelMethodProcessor.class)).isTrue();

  }

  @Test
  void lookupStrategy() {

    var context = new AnnotationConfigWebApplicationContext();
    context.setMockContext(new MockContextImpl());
    context.refresh();
    ParameterResolvingRegistry registry = new ParameterResolvingRegistry();
    registry.setApplicationContext(context);
    registry.registerDefaultStrategies(); // register defaults

    ParameterResolvingStrategy strategy = registry.findStrategy(testUser);
    assertThat(strategy).isNotNull().isInstanceOf(ModelAttributeMethodProcessor.class);
  }

  static ResolvableMethodParameter createParameter(int idx, Method method, String name) {
    SynthesizingMethodParameter parameter = SynthesizingMethodParameter.forExecutable(method, idx);
    return new ResolvableMethodParameter(parameter);
  }

}
