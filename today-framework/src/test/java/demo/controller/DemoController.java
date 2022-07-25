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

package demo.controller;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import cn.taketoday.http.converter.HttpMessageNotReadableException;
import cn.taketoday.web.HttpMediaTypeNotAcceptableException;
import cn.taketoday.web.annotation.ControllerAdvice;
import cn.taketoday.web.annotation.ExceptionHandler;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.POST;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.annotation.RequestBody;
import cn.taketoday.web.annotation.RestController;
import lombok.Data;

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

  @Data
  static class Body {
    List<UserForm> userList;
    UserForm[] userArray;
    Set<UserForm> userSet;
    Map<String, UserForm> mapUser;

  }

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
