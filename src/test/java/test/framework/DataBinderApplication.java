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

package test.framework;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.framework.WebApplication;
import lombok.Data;

/**
 * @author TODAY 2021/4/8 20:02
 * @since 3.0
 */
//@EnableTomcatHandling
//@RestController
public class DataBinderApplication {

  public static void main(String[] args) {
    WebApplication.run(DataBinderApplication.class);
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

  //http://localhost:8080/data-binder?userList%5B0%5D.age=20&userList%5B0%5D.name=YHJ
  // &userArray%5B1%5D.name=TODAY&mapUser%5Byhj%5D.name=MAP-TODAY&userSet%5B0%5D.name=set-today&userList%5B10%5D.name=TODAY-10
  @GET("/data-binder")
  Body test(List<UserForm> userList, UserForm[] userArray,
          Set<UserForm> userSet, Map<String, UserForm> mapUser) {

    System.out.println(userList);
    System.out.println(Arrays.toString(userArray));
    System.out.println(userSet);
    System.out.println(mapUser);

    return new Body(userList, userArray, userSet, mapUser);
  }

  @Data
  static class Body {
    final List<UserForm> userList;
    final UserForm[] userArray;
    final Set<UserForm> userSet;
    final Map<String, UserForm> mapUser;

    Body(List<UserForm> userList,
            UserForm[] userArray,
            Set<UserForm> userSet,
            Map<String, UserForm> mapUser) {
      this.userList = userList;
      this.userArray = userArray;
      this.userSet = userSet;
      this.mapUser = mapUser;
    }
  }

}
