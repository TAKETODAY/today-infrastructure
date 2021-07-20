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

package cn.taketoday.context.loader;

import org.junit.Test;

import java.util.List;

import cn.taketoday.context.utils.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY 2021/7/20 22:17
 */
public class YamlStrategiesReaderTests {

  @Test
  public void readInternal() {
    final YamlStrategiesReader reader = new YamlStrategiesReader();
    final MultiValueMap<String, String> read = reader.read("classpath:META-INF/today.strategies.yaml");

    assertThat(read)
            .hasSize(1)
            .containsKey("cn.taketoday.context.loader.PropertyValueResolver");

    final List<String> strings = read.get("cn.taketoday.context.loader.PropertyValueResolver");

    assertThat(strings)
            .hasSize(4)
            .contains("cn.taketoday.context.loader.StrategiesDetectorTests$MyPropertyValueResolver");
  }





}
