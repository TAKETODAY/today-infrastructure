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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.io.ByteArrayResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.util.FileCopyUtils;
import cn.taketoday.util.ReflectionUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link AbstractJsonMarshalTester}.
 *
 * @author Phillip Webb
 */
abstract class AbstractJsonMarshalTesterTests {

  private static final String JSON = "{\"name\":\"Spring\",\"age\":123}";

  private static final String MAP_JSON = "{\"a\":" + JSON + "}";

  private static final String ARRAY_JSON = "[" + JSON + "]";

  private static final ExampleObject OBJECT = createExampleObject("Spring", 123);

  private static final ResolvableType TYPE = ResolvableType.forClass(ExampleObject.class);

  @Test
  void writeShouldReturnJsonContent() throws Exception {
    JsonContent<Object> content = createTester(TYPE).write(OBJECT);
    assertThat(content).isEqualToJson(JSON);
  }

  @Test
  void writeListShouldReturnJsonContent() throws Exception {
    ResolvableType type = ResolvableTypes.get("listOfExampleObject");
    List<ExampleObject> value = Collections.singletonList(OBJECT);
    JsonContent<Object> content = createTester(type).write(value);
    assertThat(content).isEqualToJson(ARRAY_JSON);
  }

  @Test
  void writeArrayShouldReturnJsonContent() throws Exception {
    ResolvableType type = ResolvableTypes.get("arrayOfExampleObject");
    ExampleObject[] value = new ExampleObject[] { OBJECT };
    JsonContent<Object> content = createTester(type).write(value);
    assertThat(content).isEqualToJson(ARRAY_JSON);
  }

  @Test
  void writeMapShouldReturnJsonContent() throws Exception {
    ResolvableType type = ResolvableTypes.get("mapOfExampleObject");
    Map<String, Object> value = new LinkedHashMap<>();
    value.put("a", OBJECT);
    JsonContent<Object> content = createTester(type).write(value);
    assertThat(content).isEqualToJson(MAP_JSON);
  }

  @Test
  void createWhenResourceLoadClassIsNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> createTester(null, ResolvableType.forClass(ExampleObject.class)))
            .withMessageContaining("ResourceLoadClass must not be null");
  }

  @Test
  void createWhenTypeIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> createTester(getClass(), null))
            .withMessageContaining("Type must not be null");
  }

  @Test
  void parseBytesShouldReturnObject() throws Exception {
    AbstractJsonMarshalTester<Object> tester = createTester(TYPE);
    assertThat(tester.parse(JSON.getBytes())).isEqualTo(OBJECT);
  }

  @Test
  void parseStringShouldReturnObject() throws Exception {
    AbstractJsonMarshalTester<Object> tester = createTester(TYPE);
    assertThat(tester.parse(JSON)).isEqualTo(OBJECT);
  }

  @Test
  void readResourcePathShouldReturnObject() throws Exception {
    AbstractJsonMarshalTester<Object> tester = createTester(TYPE);
    assertThat(tester.read("example.json")).isEqualTo(OBJECT);
  }

  @Test
  void readFileShouldReturnObject(@TempDir Path temp) throws Exception {
    File file = new File(temp.toFile(), "example.json");
    FileCopyUtils.copy(JSON.getBytes(), file);
    AbstractJsonMarshalTester<Object> tester = createTester(TYPE);
    assertThat(tester.read(file)).isEqualTo(OBJECT);
  }

  @Test
  void readInputStreamShouldReturnObject() throws Exception {
    InputStream stream = new ByteArrayInputStream(JSON.getBytes());
    AbstractJsonMarshalTester<Object> tester = createTester(TYPE);
    assertThat(tester.read(stream)).isEqualTo(OBJECT);
  }

  @Test
  void readResourceShouldReturnObject() throws Exception {
    Resource resource = new ByteArrayResource(JSON.getBytes());
    AbstractJsonMarshalTester<Object> tester = createTester(TYPE);
    assertThat(tester.read(resource)).isEqualTo(OBJECT);
  }

  @Test
  void readReaderShouldReturnObject() throws Exception {
    Reader reader = new StringReader(JSON);
    AbstractJsonMarshalTester<Object> tester = createTester(TYPE);
    assertThat(tester.read(reader)).isEqualTo(OBJECT);
  }

  @Test
  void parseListShouldReturnContent() throws Exception {
    ResolvableType type = ResolvableTypes.get("listOfExampleObject");
    AbstractJsonMarshalTester<Object> tester = createTester(type);
    assertThat(tester.parse(ARRAY_JSON)).asList().containsOnly(OBJECT);
  }

  @Test
  void parseArrayShouldReturnContent() throws Exception {
    ResolvableType type = ResolvableTypes.get("arrayOfExampleObject");
    AbstractJsonMarshalTester<Object> tester = createTester(type);
    assertThat(tester.parse(ARRAY_JSON)).asArray().containsOnly(OBJECT);
  }

  @Test
  void parseMapShouldReturnContent() throws Exception {
    ResolvableType type = ResolvableTypes.get("mapOfExampleObject");
    AbstractJsonMarshalTester<Object> tester = createTester(type);
    assertThat(tester.parse(MAP_JSON)).asMap().containsEntry("a", OBJECT);
  }

  protected static final ExampleObject createExampleObject(String name, int age) {
    ExampleObject exampleObject = new ExampleObject();
    exampleObject.setName(name);
    exampleObject.setAge(age);
    return exampleObject;
  }

  protected final AbstractJsonMarshalTester<Object> createTester(ResolvableType type) {
    return createTester(AbstractJsonMarshalTesterTests.class, type);
  }

  protected abstract AbstractJsonMarshalTester<Object> createTester(Class<?> resourceLoadClass, ResolvableType type);

  /**
   * Access to field backed by {@link ResolvableType}.
   */
  static class ResolvableTypes {

    public List<ExampleObject> listOfExampleObject;

    public ExampleObject[] arrayOfExampleObject;

    public Map<String, ExampleObject> mapOfExampleObject;

    static ResolvableType get(String name) {
      Field field = ReflectionUtils.findField(ResolvableTypes.class, name);
      return ResolvableType.forField(field);
    }

  }

}
