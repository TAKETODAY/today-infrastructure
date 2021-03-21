/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

import junit.framework.TestCase;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import cn.taketoday.context.cglib.beans.BeanMap;
import cn.taketoday.web.MockRequestContext;
import cn.taketoday.web.exception.WebNestedRuntimeException;
import cn.taketoday.web.handler.MethodParameter;
import lombok.Data;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY 2021/3/19 20:13
 */
public class BeanParameterResolverTests extends TestCase {

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
      if (this == o) return true;
      if (!(o instanceof UserForm)) return false;
      final UserForm userForm = (UserForm) o;
      return age == userForm.age && Objects.equals(name, userForm.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(age, name);
    }

  }

  void test(UserForm user) { }

  void test(List<UserForm> userList, UserForm[] userArray, Set<UserForm> userSet) { }

  static final MethodParameter testUser;

  static final MethodParameter testUserSet;
  static final MethodParameter testListUsers;
  static final MethodParameter testUserArray;

  static {
    try {
      final Method test = BeanParameterResolverTests.class.getDeclaredMethod("test", UserForm.class);
      final Method testList = BeanParameterResolverTests.class
              .getDeclaredMethod("test", List.class, UserForm[].class, Set.class);

      testUser = new MethodParameter(0, test.getParameters()[0], "user");

      testListUsers = new MethodParameter(0, testList.getParameters()[0], "userList");
      testUserArray = new MethodParameter(2, testList.getParameters()[1], "userArray");
      testUserSet = new MethodParameter(3, testList.getParameters()[2], "testUserSet");

    }
    catch (NoSuchMethodException e) {
      throw new WebNestedRuntimeException(e);
    }
  }

  static class ParameterMockRequestContext extends MockRequestContext {
    final Map<String, String[]> parameters;

    ParameterMockRequestContext() {
      this.parameters = new HashMap<>();
    }

    ParameterMockRequestContext(Map<String, String[]> parameters) {
      this.parameters = parameters;
    }

    @Override
    public Map<String, String[]> parameters() {
      return parameters;
    }
  }

  public void testSimpleResolveParameter() throws Throwable {
    final UserForm today = new UserForm().setAge(20).setName("TODAY");

    final BeanParameterResolver resolver = new BeanParameterResolver();

    final Map<String, Object> map = BeanMap.create(today);
    final Map<String, String[]> params = map.entrySet().stream()
            .filter(entry -> entry.getValue() != null)
            .collect(Collectors.toMap(Map.Entry::getKey, (entry) -> {
              final Object value = entry.getValue();
              return new String[] { value.toString() };
            }));

    final ParameterMockRequestContext context = new ParameterMockRequestContext(params);
    // new version
    final Object newVersion = resolver.resolveParameter(context, testUser);
    assertThat(newVersion).isEqualTo(today);
  }

  final Map<String, String[]> params = new HashMap<String, String[]>() {
    {
      put("age", "20");
      put("name", "TODAY");

      put("map[1]", "1");
      put("map[2]", "2");

      put("arr[0]", "1");
      put("arr[1]", "2");
      put("arr[2]", "3");

      put("stringList[0]", "1");
      put("stringList[1]", "2");
      put("stringList[2]", "3");

      put("nested.age", "20");
      put("nested.name", "TODAY");
      put("nestedMap[yhj].age", "20");
      put("nestedMap[yhj].name", "TODAY");
      put("nestedList[0].age", "20");
      put("nestedList[0].name", "TODAY");

      put("nested.nested.age", "20");
      put("nested.nested.name", "TODAY");
      put("nested.nestedMap[yhj].age", "20");
      put("nested.nestedMap[yhj].name", "TODAY");
      put("nested.nestedList[0].age", "20");
      put("nested.nestedList[0].name", "TODAY");
    }

    public void put(String key, String value) {
      super.put(key, new String[] { value });
    }
  };

  public void testResolveParameter() {
    final UserForm today = new UserForm().setAge(20).setName("TODAY");
    final BeanParameterResolver resolver = new BeanParameterResolver();
    final ParameterMockRequestContext context = new ParameterMockRequestContext(params);

    // new version
    final Object newVersion = resolver.resolveParameter(context, testUser);
    assertThat(newVersion).isInstanceOf(UserForm.class);

    UserForm user = (UserForm) newVersion;
    assertThat(newVersion)
            .isEqualTo(today)
            .isEqualTo(user.nested)
            .isEqualTo(user.nestedList.get(0))
            .isEqualTo(user.nestedMap.get("yhj"))
    ;

    System.out.println(newVersion);
  }
}
