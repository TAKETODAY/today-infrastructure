/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework.test.json;

import com.google.gson.Gson;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link GsonTester}. Shows typical usage.
 *
 * @author Andy Wilkinson
 * @author Diego Berrueta
 */
class GsonTesterIntegrationTests {

  private GsonTester<ExampleObject> simpleJson;

  private GsonTester<List<ExampleObject>> listJson;

  private GsonTester<Map<String, Integer>> mapJson;

  private GsonTester<String> stringJson;

  private Gson gson;

  private static final String JSON = "{\"name\":\"Spring\",\"age\":123}";

  @BeforeEach
  void setup() {
    this.gson = new Gson();
    GsonTester.initFields(this, this.gson);
  }

  @Test
  void typicalTest() throws Exception {
    String example = JSON;
    assertThat(this.simpleJson.parse(example).getObject().getName()).isEqualTo("Spring");
  }

  @Test
  void typicalListTest() throws Exception {
    String example = "[" + JSON + "]";
    assertThat(this.listJson.parse(example)).asList().hasSize(1);
    assertThat(this.listJson.parse(example).getObject().get(0).getName()).isEqualTo("Spring");
  }

  @Test
  void typicalMapTest() throws Exception {
    Map<String, Integer> map = new LinkedHashMap<>();
    map.put("a", 1);
    map.put("b", 2);
    assertThat(this.mapJson.write(map)).extractingJsonPathNumberValue("@.a").isEqualTo(1);
  }

  @Test
  void stringLiteral() throws Exception {
    String stringWithSpecialCharacters = "myString";
    assertThat(this.stringJson.write(stringWithSpecialCharacters)).extractingJsonPathStringValue("@")
            .isEqualTo(stringWithSpecialCharacters);
  }

}
