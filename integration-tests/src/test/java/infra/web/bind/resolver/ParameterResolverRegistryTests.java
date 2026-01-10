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
import infra.web.server.InternalServerException;
import infra.web.MockMethodParameter;
import infra.web.handler.method.ModelAttributeMethodProcessor;
import infra.web.handler.method.ResolvableMethodParameter;
import infra.web.mock.support.AnnotationConfigWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY 2021/9/26 21:47
 */
class ParameterResolverRegistryTests {

  public static class UserForm {
    int age;

    String name;

    String[] arr;

    List<String> stringList;

    Map<String, Integer> map;

    UserForm nested;
    List<UserForm> nestedList;
    Map<String, UserForm> nestedMap;

    public int getAge() {
      return age;
    }

    public void setAge(int age) {
      this.age = age;
    }

    public String[] getArr() {
      return arr;
    }

    public void setArr(String[] arr) {
      this.arr = arr;
    }

    public Map<String, Integer> getMap() {
      return map;
    }

    public void setMap(Map<String, Integer> map) {
      this.map = map;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public UserForm getNested() {
      return nested;
    }

    public void setNested(UserForm nested) {
      this.nested = nested;
    }

    public List<UserForm> getNestedList() {
      return nestedList;
    }

    public void setNestedList(List<UserForm> nestedList) {
      this.nestedList = nestedList;
    }

    public Map<String, UserForm> getNestedMap() {
      return nestedMap;
    }

    public void setNestedMap(Map<String, UserForm> nestedMap) {
      this.nestedMap = nestedMap;
    }

    public List<String> getStringList() {
      return stringList;
    }

    public void setStringList(List<String> stringList) {
      this.stringList = stringList;
    }

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
