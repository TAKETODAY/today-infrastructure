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

package cn.taketoday.context.factory;

import org.junit.Test;

import lombok.Data;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY 2021/5/2 22:18
 */
public class BeanPropertiesTests {

  @Data
  static class VO {
    int age;
    float aFloat;
    String name;
    double aDouble;
    String missed;
  }

  @Data
  static class DTO {
    int age;
    float aFloat;
    double aDouble;
    String name;
  }

  @Test
  public void test() {
    final VO vo = new VO();
    vo.age = 10;
    vo.aDouble = 10.1D;
    vo.aFloat = 10.2f;
    vo.name = "TODAY";
    vo.missed = "missed";

    final DTO dto = new DTO();
    BeanProperties.copy(vo, dto);

    assertThat(dto).isNotNull();
    assertThat(dto.age).isEqualTo(vo.age);
    assertThat(dto.aDouble).isEqualTo(vo.aDouble);
    assertThat(dto.aFloat).isEqualTo(vo.aFloat);
    assertThat(dto.name).isNotNull().isEqualTo(vo.name);

    //

    final DTO copy = BeanProperties.copy(vo, DTO.class);

    assertThat(copy).isNotNull();
    assertThat(copy.age).isEqualTo(vo.age);
    assertThat(copy.aDouble).isEqualTo(vo.aDouble);
    assertThat(copy.aFloat).isEqualTo(vo.aFloat);
    assertThat(copy.name).isNotNull().isEqualTo(vo.name);
  }

  @Test
  public void ignoreProperties() {
    final VO vo = new VO();
    vo.age = 10;
    vo.aDouble = 10.1D;
    vo.aFloat = 10.2f;
    vo.name = "TODAY";
    vo.missed = "missed";

    final DTO dto = new DTO();
    BeanProperties.copy(vo, dto, "name");

    assertThat(dto).isNotNull();
    assertThat(dto.name).isNull();

    final DTO copy = BeanProperties.copy(vo, DTO.class, "name");

    assertThat(copy).isNotNull();
    assertThat(copy.age).isEqualTo(vo.age);
    assertThat(copy.aDouble).isEqualTo(vo.aDouble);
    assertThat(copy.aFloat).isEqualTo(vo.aFloat);
    assertThat(copy.name).isNull();

  }

}
