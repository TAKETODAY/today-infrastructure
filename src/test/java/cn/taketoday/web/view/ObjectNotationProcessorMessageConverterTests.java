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

package cn.taketoday.web.view;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import cn.taketoday.web.MockRequestContext;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.User;
import cn.taketoday.web.annotation.RequestBody;
import cn.taketoday.web.exception.WebNestedRuntimeException;
import cn.taketoday.web.handler.JacksonObjectNotationProcessor;
import cn.taketoday.web.handler.MethodParameter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * @author TODAY 2021/3/10 14:58
 */
public class ObjectNotationProcessorMessageConverterTests {

  void test(User user) { }

  void test(JsonNode node) { }

  void test(List<User> userList, JsonNode node, User[] userArray, Set<User> userSet) { }

  void testRequired(@RequestBody(required = true) User userList) { }

  static final MethodParameter testUser;
  static final MethodParameter testJsonNode;

  static final MethodParameter testUserSet;
  static final MethodParameter testListUsers;
  static final MethodParameter testUserArray;
  static final MethodParameter testJsonNodeInList;

  static final MethodParameter testRequired;

  static {
    try {
      final Method test = ObjectNotationProcessorMessageConverterTests.class.getDeclaredMethod("test", User.class);
      final Method testJsonNodeMethod = ObjectNotationProcessorMessageConverterTests.class.getDeclaredMethod("test", JsonNode.class);
      final Method testList = ObjectNotationProcessorMessageConverterTests.class
              .getDeclaredMethod("test", List.class, JsonNode.class, User[].class, Set.class);
      final Method testRequiredM = ObjectNotationProcessorMessageConverterTests.class.getDeclaredMethod("testRequired", User.class);

      testUser = new MethodParameter(0, test, "user");
      testJsonNode = new MethodParameter(0, testJsonNodeMethod, "node");

      testListUsers = new MethodParameter(0, testList, "userList");
      testJsonNodeInList = new MethodParameter(1, testList, "node");
      testUserArray = new MethodParameter(2, testList, "userArray");
      testUserSet = new MethodParameter(3, testList, "testUserSet");

      testRequired = new MethodParameter(0, testRequiredM, "testRequired");
    }
    catch (NoSuchMethodException e) {
      throw new WebNestedRuntimeException(e);
    }
  }

  static class JacksonMockRequestContext extends MockRequestContext {
    final String json;

    JacksonMockRequestContext(String json) {
      this.json = json;
    }

    @Override
    protected InputStream doGetInputStream() {
      return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected OutputStream doGetOutputStream() {
      return new ByteArrayOutputStream();
    }
  }

  @Test
  public void testWrite() throws IOException {
    final User today = new User().setAge(20).setName("TODAY");
    final ObjectMapper mapper = new ObjectMapper();

    final RequestContext context = new JacksonMockRequestContext("");
    final ObjectNotationProcessorMessageConverter converter
            = new ObjectNotationProcessorMessageConverter(new JacksonObjectNotationProcessor());

    converter.write(context, today);

    final String content = context.getOutputStream().toString();

    final User readUser = mapper.readValue(content, User.class);
    final User readTypeReference = mapper.readValue(content, new TypeReference<User>() { });
    assertThat(readUser).isEqualTo(today).isEqualTo(readTypeReference);
  }

  @Test
  public void testRead() throws IOException {
    final ObjectMapper mapper = new ObjectMapper();

    final User today = new User().setAge(20).setName("TODAY");
    final List<User> list = Arrays.asList(today, new User().setAge(21).setName("TODAY1"));

    final User[] objects = list.toArray(new User[0]);

    String json = mapper.writeValueAsString(today);
    String jsonList = mapper.writeValueAsString(list);
    String emptyJson = "";

    final RequestContext context = new JacksonMockRequestContext(json);
    final RequestContext contextList = new JacksonMockRequestContext(jsonList);
    final RequestContext contextEmptyJson = new JacksonMockRequestContext(emptyJson);

    final ObjectNotationProcessorMessageConverter converter
            = new ObjectNotationProcessorMessageConverter(new JacksonObjectNotationProcessor());

    final Object user = converter.read(context, testUser);
    context.getInputStream().reset();
    final Object jsonNode = converter.read(context, testJsonNode);

    final InputStream inputStream = contextList.getInputStream();
    final Object userList = converter.read(contextList, testListUsers);
    inputStream.reset();
    final Object userArray = converter.read(contextList, testUserArray);
    inputStream.reset();
    final Object jsonNodeInList = converter.read(contextList, testJsonNodeInList);
    inputStream.reset();
    // object in list
    final Object userInList = converter.read(contextList, testUser);

    assertThat(user).isNotNull().isEqualTo(today).isEqualTo(userInList);
    assertThat(userList).isEqualTo(list);
    assertThat(userArray).isEqualTo(objects);

    assertThat(jsonNode).isInstanceOf(JsonNode.class).isEqualTo(((JsonNode) jsonNodeInList).get(0));

    // null string
    assertThat(converter.read(contextEmptyJson, testRequired)).isNull();
    assertThat(converter.read(contextEmptyJson, testUser)).isNull();
    assertThat(converter.read(contextEmptyJson, testJsonNode)).isNull();
    assertThat(converter.read(contextEmptyJson, testListUsers)).isNull();
    assertThat(converter.read(contextEmptyJson, testUserArray)).isNull();
    assertThat(converter.read(contextEmptyJson, testJsonNodeInList)).isNull();

    // JsonProcessingException

    final RequestContext jsonProcessing = new JacksonMockRequestContext("{\"}");
    try {
      converter.read(jsonProcessing, testRequired);
      fail("Exception");
    }
    catch (IOException e) {
      assertThat(e.getMessage()).startsWith("Unexpected end-of-input in field name");
    }
  }
}
