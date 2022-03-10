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

package cn.taketoday.context.conversionservice;

import org.junit.jupiter.api.Test;

import cn.taketoday.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Keith Donald
 */
public class ConversionServiceContextConfigTests {

  @Test
  public void testConfigOk() {
    ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("cn/taketoday/context/conversionservice/conversionService.xml");
    TestClient client = context.getBean("testClient", TestClient.class);
    assertThat(client.getBars().size()).isEqualTo(2);
    assertThat(client.getBars().get(0).getValue()).isEqualTo("value1");
    assertThat(client.getBars().get(1).getValue()).isEqualTo("value2");
    assertThat(client.isBool()).isTrue();
  }

}
