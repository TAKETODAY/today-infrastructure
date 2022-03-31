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

package cn.taketoday.test.context.junit.jupiter;

import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Primary;
import cn.taketoday.test.context.junit.jupiter.comics.Cat;
import cn.taketoday.test.context.junit.jupiter.comics.Dog;
import cn.taketoday.test.context.junit.jupiter.comics.Person;

/**
 * Demo config for tests.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@Configuration
public class TestConfig {

  @Bean
  Person dilbert() {
    return new Person("Dilbert");
  }

  @Bean
  Person wally() {
    return new Person("Wally");
  }

  @Bean
  Dog dogbert() {
    return new Dog("Dogbert");
  }

  @Primary
  @Bean
  Cat catbert() {
    return new Cat("Catbert");
  }

  @Bean
  Cat garfield() {
    return new Cat("Garfield");
  }

}
