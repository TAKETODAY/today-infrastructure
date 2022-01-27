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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import cn.taketoday.core.DefaultMultiValueMap;
import cn.taketoday.core.MultiValueMap;
import cn.taketoday.core.annotation.SynthesizingMethodParameter;
import cn.taketoday.core.bytecode.beans.BeanMap;
import cn.taketoday.web.MockMultipartFile;
import cn.taketoday.web.MockRequestContext;
import cn.taketoday.web.WebNestedRuntimeException;
import cn.taketoday.web.handler.MockResolvableMethodParameter;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.multipart.MultipartFile;
import lombok.Data;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY 2021/3/19 20:13
 * @since 3.0
 */
public class DataBinderParameterResolverTests {

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

  void test(MultipartFileUserForm user) { }

  void test(List<UserForm> userList, UserForm[] userArray,
            Set<UserForm> userSet, Map<String, UserForm> mapUser) { }

  static final ResolvableMethodParameter testUser;
  static final ResolvableMethodParameter testMultipartFileUserForm;

  static final ResolvableMethodParameter testUserSet;
  static final ResolvableMethodParameter testListUsers;
  static final ResolvableMethodParameter testUserArray;
  static final ResolvableMethodParameter testMapUser;

  static {
    try {
      Method test = DataBinderParameterResolverTests.class.getDeclaredMethod("test", UserForm.class);
      Method multipartFileUserForm = DataBinderParameterResolverTests.class.getDeclaredMethod("test", MultipartFileUserForm.class);
      Method testList = DataBinderParameterResolverTests.class
              .getDeclaredMethod("test", List.class, UserForm[].class, Set.class, Map.class);

      testUser = createParameter(0, test, "user");
      testMultipartFileUserForm = createParameter(0, multipartFileUserForm, "user");

      testListUsers = createParameter(0, testList, "userList");
      testUserArray = createParameter(1, testList, "userArray");
      testUserSet = createParameter(2, testList, "userSet");
      testMapUser = createParameter(3, testList, "mapUser");
    }
    catch (NoSuchMethodException e) {
      throw new WebNestedRuntimeException(e);
    }
  }

  static class ParameterMockRequestContext extends MockRequestContext {
    Map<String, String[]> parameters;
    MultiValueMap<String, MultipartFile> multipartFiles;

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
    public MultiValueMap<String, MultipartFile> multipartFiles() {
      return multipartFiles;
    }

    public void setMultipartFiles(MultiValueMap<String, MultipartFile> multipartFiles) {
      this.multipartFiles = multipartFiles;
    }

  }

  @Test
  public void testSimpleResolveParameter() throws Throwable {
    UserForm today = new UserForm().setAge(20).setName("TODAY");

    DataBinderParameterResolver resolver = new DataBinderParameterResolver();

    Map<String, Object> map = BeanMap.create(today);
    Map<String, String[]> params = map.entrySet().stream()
            .filter(entry -> entry.getValue() != null)
            .collect(Collectors.toMap(Map.Entry::getKey, (entry) -> {
              Object value = entry.getValue();
              return new String[] { value.toString() };
            }));

    ParameterMockRequestContext context = new ParameterMockRequestContext(params);
    // new version
    Object newVersion = resolver.resolveParameter(context, testUser);
    assertThat(newVersion).isEqualTo(today);
  }

  Map<String, String[]> params = new HashMap<String, String[]>() {
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

  @Test
  public void testResolveParameter() throws Throwable {
    UserForm today = new UserForm().setAge(20).setName("TODAY");
    DataBinderParameterResolver resolver = new DataBinderParameterResolver();

    ParameterMockRequestContext context = new ParameterMockRequestContext(params);

    // new version
    Object newVersion = resolver.resolveParameter(context, testUser);
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

  // Bind MultipartFile

  @Data
  public static class MultipartFileUserForm {
    int age;

    String name;

    String[] arr;

    List<String> stringList;

    Map<String, Integer> map;

    UserForm nested;
    List<UserForm> nestedList;
    Map<String, UserForm> nestedMap;

    MultipartFile uploadFile;
    List<MultipartFile> uploadFiles;
//    MultipartFile[] uploadFiles;

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (!(o instanceof UserForm))
        return false;
      UserForm userForm = (UserForm) o;
      return age == userForm.age && Objects.equals(name, userForm.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(age, name);
    }

  }

  static class NamedMockMultipartFile extends MockMultipartFile {
    final String name;

    NamedMockMultipartFile(String name) {
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
    }
  }

  @Test
  public void testBindMultipartFile() throws Throwable {

    MultipartFileUserForm today = new MultipartFileUserForm().setAge(20).setName("TODAY");
    DataBinderParameterResolver resolver = new DataBinderParameterResolver();
    ParameterMockRequestContext context = new ParameterMockRequestContext(params);

    MultiValueMap<String, MultipartFile> map = new DefaultMultiValueMap<>();
    List<MultipartFile> uploadFile = Collections.singletonList(new NamedMockMultipartFile("uploadFile"));
    List<MultipartFile> files = Arrays.asList(new NamedMockMultipartFile("uploadFiles"),
            new NamedMockMultipartFile("uploadFiles"));

    map.put("uploadFile", uploadFile);
    map.put("uploadFiles", files);
    context.setMultipartFiles(map);

    // new version
    Object newVersion = resolver.resolveParameter(context, testMultipartFileUserForm);
    assertThat(newVersion).isInstanceOf(MultipartFileUserForm.class);

    System.out.println(newVersion);

  }

  //
  @Test
  @SuppressWarnings("unchecked")
  public void testResolveCollection() throws Throwable {
    Map<String, String[]> params = new HashMap<String, String[]>() {
      {
        put("userList[0].age", "20");
        put("userList[0].name", "TODAY");

        put("userList[0].map[1]", "1");
        put("userList[0].map[2]", "2");

        put("userList[0].arr[0]", "1");
        put("userList[0].arr[1]", "2");
        put("userList[0].arr[2]", "3");

        put("userList[10].name", "TODAY");
        put("userList[10].map[2]", "2");
        put("userList[10].arr[2]", "3");
        // set
        put("userSet[0].age", "20");
        put("userSet[0].name", "TODAY");

        put("userSet[0].map[1]", "1");
        put("userSet[0].map[2]", "2");

        put("userSet[0].arr[0]", "1");
        put("userSet[0].arr[1]", "2");
        put("userSet[0].arr[2]", "3");

        put("userSet[10].name", "TODAY");
        put("userSet[10].map[2]", "2");
        put("userSet[10].arr[2]", "3");
      }

      public void put(String key, String value) {
        super.put(key, new String[] { value });
      }
    };

    UserForm today = new UserForm().setAge(20).setName("TODAY");

    DataBinderCollectionParameterResolver resolver = new DataBinderCollectionParameterResolver();
    ParameterMockRequestContext context = new ParameterMockRequestContext(params);

    // new version
    Object parameter = resolver.resolveParameter(context, testListUsers);

    assertThat(parameter)
            .isInstanceOf(List.class);

    List<UserForm> res = (List<UserForm>) parameter;

    UserForm userForm = res.get(0);

    assertThat(userForm).isEqualTo(today);
    assertThat(userForm.name).isEqualTo("TODAY");
    assertThat(userForm.age).isEqualTo(20);
    assertThat(userForm.arr).hasSize(3);

    assertThat(userForm.arr[0]).isEqualTo("1");
    assertThat(userForm.arr[1]).isEqualTo("2");
    assertThat(userForm.arr[2]).isEqualTo("3");

    assertThat(userForm.map)
            .hasSize(2)
            .containsEntry("1", 1)
            .containsEntry("2", 2);

    UserForm userForm10 = res.get(10);

    assertThat(userForm10.name).isEqualTo("TODAY");
    assertThat(userForm10.age).isZero();

    assertThat(userForm10.arr).hasSize(3);
    assertThat(userForm10.arr[0]).isNull();
    assertThat(userForm10.arr[1]).isNull();
    assertThat(userForm10.arr[2]).isEqualTo("3");

    assertThat(userForm10.map)
            .containsEntry("2", 2)
            .hasSize(1);

    // set

    // new version
    Object setParameter = resolver.resolveParameter(context, testUserSet);

    assertThat(setParameter)
            .isInstanceOf(Set.class);

    Set<UserForm> setRes = (Set<UserForm>) setParameter;

    Iterator<UserForm> iterator = setRes.iterator();
    UserForm userFormSet = iterator.next();

    assertThat(userFormSet).isEqualTo(today);
    assertThat(userFormSet.name).isEqualTo("TODAY");
    assertThat(userFormSet.age).isEqualTo(20);
    assertThat(userFormSet.arr).hasSize(3);

    assertThat(userFormSet.arr[0]).isEqualTo("1");
    assertThat(userFormSet.arr[1]).isEqualTo("2");
    assertThat(userFormSet.arr[2]).isEqualTo("3");

    assertThat(userFormSet.map)
            .hasSize(2)
            .containsEntry("1", 1)
            .containsEntry("2", 2);

    UserForm userFormSet10 = iterator.next();

    assertThat(userFormSet10.name).isEqualTo("TODAY");
    assertThat(userFormSet10.age).isZero();

    assertThat(userFormSet10.arr).hasSize(3);
    assertThat(userFormSet10.arr[0]).isNull();
    assertThat(userFormSet10.arr[1]).isNull();
    assertThat(userFormSet10.arr[2]).isEqualTo("3");

    assertThat(userFormSet10.map)
            .containsEntry("2", 2)
            .hasSize(1);
  }

  @Test

  public void testResolveArray() throws Throwable {
    Map<String, String[]> params = new HashMap<String, String[]>() {
      {
        put("userArray[0].age", "20");
        put("userArray[0].name", "TODAY");

        put("userArray[0].map[1]", "1");
        put("userArray[0].map[2]", "2");

        put("userArray[0].arr[0]", "1");
        put("userArray[0].arr[1]", "2");
        put("userArray[0].arr[2]", "3");

        put("userArray[10].name", "TODAY");
        put("userArray[10].map[2]", "2");
        put("userArray[10].arr[2]", "3");

        put("userList[10].arr[2]", "3");
      }

      public void put(String key, String value) {
        super.put(key, new String[] { value });
      }
    };

    UserForm today = new UserForm().setAge(20).setName("TODAY");

    DataBinderArrayParameterResolver resolver = new DataBinderArrayParameterResolver();
    ParameterMockRequestContext context = new ParameterMockRequestContext(params);

    // array
    Object parameter = resolver.resolveParameter(context, testUserArray);

    assertThat(parameter)
            .isInstanceOf(UserForm[].class);

    UserForm[] res = (UserForm[]) parameter;

    UserForm userForm = res[0];

    assertThat(userForm).isEqualTo(today);
    assertThat(userForm.name).isEqualTo("TODAY");
    assertThat(userForm.age).isEqualTo(20);
    assertThat(userForm.arr).hasSize(3);

    assertThat(userForm.arr[0]).isEqualTo("1");
    assertThat(userForm.arr[1]).isEqualTo("2");
    assertThat(userForm.arr[2]).isEqualTo("3");

    assertThat(userForm.map)
            .hasSize(2)
            .containsEntry("1", 1)
            .containsEntry("2", 2);

    UserForm userForm10 = res[10];

    assertThat(userForm10.name).isEqualTo("TODAY");
    assertThat(userForm10.age).isZero();

    assertThat(userForm10.arr).hasSize(3);
    assertThat(userForm10.arr[0]).isNull();
    assertThat(userForm10.arr[1]).isNull();
    assertThat(userForm10.arr[2]).isEqualTo("3");

    assertThat(userForm10.map)
            .containsEntry("2", 2)
            .hasSize(1);

  }

  @Test
  public void testResolveMap() throws Throwable {
    Map<String, String[]> params = new HashMap<String, String[]>() {
      {

        put("mapUser[yhj].age", "20");
        put("mapUser[yhj].name", "yhj");

        put("mapUser[yhj].arr[0]", "1");
        put("mapUser[yhj].arr[1]", "2");
        put("mapUser[yhj].arr[2]", "3");

        put("mapUser[yhj].map[2]", "2");
        put("mapUser[yhj].map[1]", "1");

        put("mapUser[today].age", "23");
        put("mapUser[today].name", "TODAY");

        put("mapUser[today].arr[0]", "1");
        put("mapUser[today].arr[1]", "2");
        put("mapUser[today].arr[2]", "3");

        put("mapUser[today].map[1]", "1");
        put("mapUser[today].map[2]", "2");

        put("userList[10].arr[2]", "3");
      }

      public void put(String key, String value) {
        super.put(key, new String[] { value });
      }
    };

    UserForm today = new UserForm().setAge(23).setName("TODAY");

    DataBinderMapParameterResolver resolver = new DataBinderMapParameterResolver();
    ParameterMockRequestContext context = new ParameterMockRequestContext(params);

    // array
    Object parameter = resolver.resolveParameter(context, testMapUser);

    assertThat(parameter)
            .isInstanceOf(Map.class);

    Map<String, UserForm> res = (Map<String, UserForm>) parameter;

    UserForm userForm = res.get("yhj");

    assertThat(userForm.name).isEqualTo("yhj");
    assertThat(userForm.age).isEqualTo(20);
    assertThat(userForm.arr).hasSize(3);

    assertThat(userForm.arr[0]).isEqualTo("1");
    assertThat(userForm.arr[1]).isEqualTo("2");
    assertThat(userForm.arr[2]).isEqualTo("3");

    assertThat(userForm.map)
            .hasSize(2)
            .containsEntry("1", 1)
            .containsEntry("2", 2);

    UserForm userForm10 = res.get("today");
    assertThat(userForm10).isEqualTo(today);

    assertThat(userForm10.name).isEqualTo("TODAY");
    assertThat(userForm10.age).isEqualTo(23);

    assertThat(userForm10.arr).hasSize(3);
    assertThat(userForm10.arr[0]).isEqualTo("1");
    assertThat(userForm10.arr[1]).isEqualTo("2");
    assertThat(userForm10.arr[2]).isEqualTo("3");

    assertThat(userForm10.map)
            .containsEntry("2", 2)
            .hasSize(2);

  }

  static ResolvableMethodParameter createParameter(int idx, Method method, String name) {
    SynthesizingMethodParameter parameter = SynthesizingMethodParameter.forExecutable(method, idx);
    return new MockResolvableMethodParameter(parameter, name);
  }

}
