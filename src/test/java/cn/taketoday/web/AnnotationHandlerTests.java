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
package cn.taketoday.web;

import org.junit.Ignore;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import cn.taketoday.web.annotation.EnableViewController;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.POST;
import cn.taketoday.web.annotation.RestController;
import cn.taketoday.web.config.WebMvcConfiguration;
import cn.taketoday.web.handler.ViewController;
import cn.taketoday.web.registry.ViewControllerHandlerRegistry;
import cn.taketoday.web.utils.HttpUtils;
import lombok.Data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author TODAY <br>
 * 2020-04-28 15:39
 */
@Ignore
@RestController
@EnableViewController
public class AnnotationHandlerTests extends Base implements WebMvcConfiguration {

  @GET("/index/{q}")
  public String index(String q) {
    return q;
  }

  @GET("/index/query")
  public String query(String q) {
    return q;
  }

  @Override
  public void configureViewController(ViewControllerHandlerRegistry registry) {
    registry.addViewController("/view/controller/text").setResource("body:text");
    registry.addViewController("/view/controller/buffer", new StringBuilder("text"));
    registry.addViewController("/view/controller/null");
//        registry.addViewController("/view/controller/text").setResource("text");
  }

  public void testViewController(ViewControllerHandlerRegistry registry) {
    final Object defaultHandler = registry.getDefaultHandler();
    assertNull(defaultHandler);
    final ViewController viewController = registry.getViewController("/view/controller/null");
    assertNotNull(viewController);
    assertNull(viewController.getStatus());
    assertNull(viewController.getResource());
    assertNull(viewController.getContentType());
    assertNull(viewController.getHandlerMethod());

    assertNotNull(registry.getViewController("/view/controller/text"));
    assertEquals(registry.getViewController("/view/controller/text").getResource(), "body:text");
    assertNull(registry.getViewController("/view/controller/text/123"));
  }

  @Test
  public void testRestController() throws IOException {
    assertEquals(HttpUtils.get("http://localhost:81/index/123"), "123");
    assertEquals(HttpUtils.get("http://localhost:81/index/query?q=123"), "123");
    assertEquals(HttpUtils.get("http://localhost:81/view/controller/text"), "text");
    assertEquals(HttpUtils.get("http://localhost:81/view/controller/buffer"), "text");
    assertEquals(HttpUtils.get("http://localhost:81/view/controller/null"), "");
    try {
      HttpUtils.get("http://localhost:81/index");
    }
    catch (FileNotFoundException e) {
      assert true;
    }
    testViewController(context.getBean(ViewControllerHandlerRegistry.class));
  }

  //

  @POST("/test-bean")
  public UserForm testForm(UserForm user) {
    return user;
  }

  @Data
  static class UserForm {
    int age;
    String name;
    String[] array;
    List<String> listString;

    Map<String, String> map;

    Address address;
  }

  @Data
  static class Address {
    String place;
  }

  @Test
  public void testForm() throws Exception {
    String params = "name=TODAY&age=23&listString=list1" +
            "&array=arr&array=aaa&map%5Bkey%5D=value" +
            "&address.place=address";

    final UserForm expected = HttpUtils.post("http://localhost:81/test-bean", params, UserForm.class);
    assertThat(expected.address.place).isEqualTo("address");
    assertThat(expected.age).isEqualTo(23);
    assertThat(expected.name).isEqualTo("TODAY");
    assertThat(expected.listString).hasSize(1).contains("list1");
    assertThat(expected.array).hasSize(2).contains("arr", "aaa");
    assertThat(expected.map).hasSize(1).containsEntry("key", "value");

  }

}
