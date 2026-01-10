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

package demo.controller;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import infra.http.converter.HttpMessageNotReadableException;
import infra.web.HttpMediaTypeNotAcceptableException;
import infra.web.annotation.ControllerAdvice;
import infra.web.annotation.ExceptionHandler;
import infra.web.annotation.GET;
import infra.web.annotation.POST;
import infra.web.annotation.PathVariable;
import infra.web.annotation.RequestBody;
import infra.web.annotation.RestController;

/**
 * @author TODAY 2021/8/29 22:20
 */
@ControllerAdvice
@RestController
public class DemoController {

  @GET("/demo")
  public void demo() {

  }

  @GET("hello")
  public String string() {
    return "hello";
  }

  @GET("/body")
  Body test(Body body) {
    return body;
  }

  @POST("/body")
  Body postBody(@RequestBody Body body) {
    return body;
  }

  @GET("/path/{var}")
  String pathVariable(@PathVariable String var) {
    return var;
  }

  @ExceptionHandler(Throwable.class)
  public void throwable(Throwable throwable) {
    throwable.printStackTrace();
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public HttpMessageNotReadableException throwable(HttpMessageNotReadableException throwable) {
    return throwable;
  }

  @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
  public HttpMediaTypeNotAcceptableException throwable(HttpMediaTypeNotAcceptableException throwable) {
    return throwable;
  }

  static class Body {
    List<UserForm> userList;
    UserForm[] userArray;
    Set<UserForm> userSet;
    Map<String, UserForm> mapUser;

    public Map<String, UserForm> getMapUser() {
      return mapUser;
    }

    public void setMapUser(Map<String, UserForm> mapUser) {
      this.mapUser = mapUser;
    }

    public UserForm[] getUserArray() {
      return userArray;
    }

    public void setUserArray(UserForm[] userArray) {
      this.userArray = userArray;
    }

    public List<UserForm> getUserList() {
      return userList;
    }

    public void setUserList(List<UserForm> userList) {
      this.userList = userList;
    }

    public Set<UserForm> getUserSet() {
      return userSet;
    }

    public void setUserSet(Set<UserForm> userSet) {
      this.userSet = userSet;
    }
  }

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
      if (!(o instanceof UserForm userForm))
        return false;
      return age == userForm.age && Objects.equals(name, userForm.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(age, name);
    }

  }
}
