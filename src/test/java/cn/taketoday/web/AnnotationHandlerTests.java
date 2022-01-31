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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import cn.taketoday.web.annotation.EnableViewController;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.POST;
import cn.taketoday.web.annotation.RestController;
import cn.taketoday.web.client.RestTemplate;
import cn.taketoday.web.config.WebMvcConfiguration;
import cn.taketoday.web.handler.ViewController;
import cn.taketoday.web.registry.ViewControllerHandlerRegistry;
import lombok.Data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author TODAY <br>
 * 2020-04-28 15:39
 */
@Disabled
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

  public void testViewController(ViewControllerHandlerRegistry registry) throws Exception {
    Object defaultHandler = registry.getDefaultHandler();
    assertNull(defaultHandler);



    ViewController viewController = registry.getViewController("/view/controller/null");
    assertNotNull(viewController);
    assertNull(viewController.getStatus());
    assertNull(viewController.getResource());
    assertNull(viewController.getContentType());
    assertNull(viewController.getHandler());

    assertNotNull(registry.getViewController("/view/controller/text"));
    assertEquals(registry.getViewController("/view/controller/text").getResource(), "body:text");
    assertNull(registry.getViewController("/view/controller/text/123"));
  }

  @Test
  public void testRestController() throws Exception {
    assertEquals(httpGetText("http://localhost:81/index/123"), "123");
    assertEquals(httpGetText("http://localhost:81/index/query?q=123"), "123");
    assertEquals(httpGetText("http://localhost:81/view/controller/text"), "text");
    assertEquals(httpGetText("http://localhost:81/view/controller/buffer"), "text");
    assertEquals(httpGetText("http://localhost:81/view/controller/null"), "");
//      httpGetText("http://localhost:81/index"); 404
    testViewController(context.getBean(ViewControllerHandlerRegistry.class));
  }

  RestTemplate restTemplate = new RestTemplate();

  String httpGetText(String url) {
    return restTemplate.getForObject(url, String.class);
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

    UserForm expected = restTemplate.postForObject("http://localhost:81/test-bean", params, UserForm.class);

    assertThat(expected.address.place).isEqualTo("address");
    assertThat(expected.age).isEqualTo(23);
    assertThat(expected.name).isEqualTo("TODAY");
    assertThat(expected.listString).hasSize(1).contains("list1");
    assertThat(expected.array).hasSize(2).contains("arr", "aaa");
    assertThat(expected.map).hasSize(1).containsEntry("key", "value");

  }

}
