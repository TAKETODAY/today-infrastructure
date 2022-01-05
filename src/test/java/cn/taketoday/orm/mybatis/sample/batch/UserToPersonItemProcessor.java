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
package cn.taketoday.orm.mybatis.sample.batch;

import cn.taketoday.orm.mybatis.sample.domain.Person;
import cn.taketoday.orm.mybatis.sample.domain.User;
import cn.taketoday.batch.item.ItemProcessor;

public class UserToPersonItemProcessor implements ItemProcessor<User, Person> {

  @Override
  public Person process(final User user) throws Exception {
    final String[] names = user.getName().split(" ");
    if (names.length == 1) {
      return new Person(names[0], null);
    } else {
      return new Person(names[0], names[1]);
    }
  }

}
