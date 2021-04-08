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

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import cn.taketoday.web.MockRequestContext;
import cn.taketoday.web.exception.WebNestedRuntimeException;
import cn.taketoday.web.handler.MethodParameter;
import cn.taketoday.web.multipart.MultipartFile;
import lombok.Data;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY 2021/4/8 14:38
 * @since 3.0
 */
public class DataBinderCollectionParameterResolverTests {

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
      final Method test = DataBinderCollectionParameterResolverTests.class.getDeclaredMethod("test", UserForm.class);
      final Method testList = DataBinderCollectionParameterResolverTests.class
              .getDeclaredMethod("test", List.class, UserForm[].class, Set.class);

      testUser = new MethodParameter(0, test, "user");

      testListUsers = new MethodParameter(0, testList, "userList");
      testUserArray = new MethodParameter(1, testList, "userArray");
      testUserSet = new MethodParameter(2, testList, "testUserSet");

    }
    catch (NoSuchMethodException e) {
      throw new WebNestedRuntimeException(e);
    }
  }

  static class ParameterMockRequestContext extends MockRequestContext {
    final Map<String, String[]> parameters;
    Map<String, List<MultipartFile>> multipartFiles;

    ParameterMockRequestContext() {
      this.parameters = new HashMap<>();
    }

    ParameterMockRequestContext(Map<String, String[]> parameters) {
      this.parameters = parameters;
    }

    @Override
    public Map<String, String[]> getParameters() {
      return parameters;
    }

    @Override
    public Map<String, List<MultipartFile>> multipartFiles() {
      return multipartFiles;
    }

    public void setMultipartFiles(Map<String, List<MultipartFile>> multipartFiles) {
      this.multipartFiles = multipartFiles;
    }
  }

  final Map<String, String[]> params = new HashMap<String, String[]>() {
    {
      put("userList[0].age", "20");
      put("userList[0].name", "TODAY");
      put("userList[10].name", "TODAY");

      put("userList[0].map[1]", "1");
      put("userList[0].map[2]", "2");
      put("userList[10].map[2]", "2");

      put("userList[0].arr[0]", "1");
      put("userList[0].arr[1]", "2");
      put("userList[0].arr[2]", "3");
      put("userList[10].arr[2]", "3");
    }

    public void put(String key, String value) {
      super.put(key, new String[] { value });
    }
  };

  @Test
  @SuppressWarnings("unchecked")
  public void resolveCollection() throws Throwable {

    final UserForm today = new UserForm().setAge(20).setName("TODAY");
    List<UserForm> list = Arrays.asList(today);

    final DataBinderCollectionParameterResolver resolver = new DataBinderCollectionParameterResolver();
    final ParameterMockRequestContext context = new ParameterMockRequestContext(params);

    // new version
    final Object parameter = resolver.resolveParameter(context, testListUsers);

    assertThat(parameter)
            .isInstanceOf(List.class);

    List<UserForm> res = (List<UserForm>) parameter;

    final UserForm userForm = res.get(0);

    assertThat(userForm).isEqualTo(today);
    assertThat(userForm.name).isEqualTo("TODAY");
    assertThat(userForm.age).isEqualTo(20);


//    assertThat(newVersion).isEqualTo(today);
  }


}
