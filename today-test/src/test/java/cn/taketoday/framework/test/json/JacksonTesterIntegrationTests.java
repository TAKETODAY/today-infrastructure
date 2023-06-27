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

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import org.junit.jupiter.api.Test;

import cn.taketoday.core.io.ByteArrayResource;

import java.io.Reader;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link JacksonTester}. Shows typical usage.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author Diego Berrueta
 */
class JacksonTesterIntegrationTests {

  private JacksonTester<ExampleObject> simpleJson;

  private JacksonTester<ExampleObjectWithView> jsonWithView;

  private JacksonTester<List<ExampleObject>> listJson;

  private JacksonTester<Map<String, Integer>> mapJson;

  private JacksonTester<String> stringJson;

  private static final String JSON = "{\"name\":\"Spring\",\"age\":123}";

  @Test
  void typicalTest() throws Exception {
    JacksonTester.initFields(this, new ObjectMapper());
    String example = JSON;
    assertThat(this.simpleJson.parse(example).getObject().getName()).isEqualTo("Spring");
  }

  @Test
  void typicalListTest() throws Exception {
    JacksonTester.initFields(this, new ObjectMapper());
    String example = "[" + JSON + "]";
    assertThat(this.listJson.parse(example)).asList().hasSize(1);
    assertThat(this.listJson.parse(example).getObject().get(0).getName()).isEqualTo("Spring");
  }

  @Test
  void typicalMapTest() throws Exception {
    JacksonTester.initFields(this, new ObjectMapper());
    Map<String, Integer> map = new LinkedHashMap<>();
    map.put("a", 1);
    map.put("b", 2);
    assertThat(this.mapJson.write(map)).extractingJsonPathNumberValue("@.a").isEqualTo(1);
  }

  @Test
  void stringLiteral() throws Exception {
    JacksonTester.initFields(this, new ObjectMapper());
    String stringWithSpecialCharacters = "myString";
    assertThat(this.stringJson.write(stringWithSpecialCharacters)).extractingJsonPathStringValue("@")
            .isEqualTo(stringWithSpecialCharacters);
  }

  @Test
  void parseSpecialCharactersTest() throws Exception {
    JacksonTester.initFields(this, new ObjectMapper());
    // Confirms that the handling of special characters is symmetrical between
    // the serialization (through the JacksonTester) and the parsing (through
    // json-path). By default json-path uses SimpleJson as its parser, which has a
    // slightly different behavior to Jackson and breaks the symmetry. JacksonTester
    // configures json-path to use Jackson for evaluating the path expressions and
    // restores the symmetry. See gh-15727
    String stringWithSpecialCharacters = "\u0006\u007F";
    assertThat(this.stringJson.write(stringWithSpecialCharacters)).extractingJsonPathStringValue("@")
            .isEqualTo(stringWithSpecialCharacters);
  }

  @Test
  void writeWithView() throws Exception {
    JacksonTester.initFields(this, JsonMapper.builder().disable(MapperFeature.DEFAULT_VIEW_INCLUSION).build());
    ExampleObjectWithView object = new ExampleObjectWithView();
    object.setName("Spring");
    object.setAge(123);
    JsonContent<ExampleObjectWithView> content = this.jsonWithView.forView(ExampleObjectWithView.TestView.class)
            .write(object);
    assertThat(content).extractingJsonPathStringValue("@.name").isEqualTo("Spring");
    assertThat(content).doesNotHaveJsonPathValue("age");
  }

  @Test
  void readWithResourceAndView() throws Exception {
    JacksonTester.initFields(this, JsonMapper.builder().disable(MapperFeature.DEFAULT_VIEW_INCLUSION).build());
    ByteArrayResource resource = new ByteArrayResource(JSON.getBytes());
    ObjectContent<ExampleObjectWithView> content = this.jsonWithView.forView(ExampleObjectWithView.TestView.class)
            .read(resource);
    assertThat(content.getObject().getName()).isEqualTo("Spring");
    assertThat(content.getObject().getAge()).isZero();
  }

  @Test
  void readWithReaderAndView() throws Exception {
    JacksonTester.initFields(this, JsonMapper.builder().disable(MapperFeature.DEFAULT_VIEW_INCLUSION).build());
    Reader reader = new StringReader(JSON);
    ObjectContent<ExampleObjectWithView> content = this.jsonWithView.forView(ExampleObjectWithView.TestView.class)
            .read(reader);
    assertThat(content.getObject().getName()).isEqualTo("Spring");
    assertThat(content.getObject().getAge()).isZero();
  }

}
