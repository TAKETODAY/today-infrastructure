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

package cn.taketoday.test.web.client.samples.matchers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.StringHttpMessageConverter;
import cn.taketoday.http.converter.json.MappingJackson2HttpMessageConverter;
import cn.taketoday.test.web.Person;
import cn.taketoday.test.web.client.MockRestServiceServer;
import cn.taketoday.web.client.RestTemplate;

import static cn.taketoday.test.web.client.match.MockRestRequestMatchers.content;
import static cn.taketoday.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;

/**
 * Examples of defining expectations on request content and content type.
 *
 * @author Rossen Stoyanchev
 * @see JsonPathRequestMatchersIntegrationTests
 * @see XmlContentRequestMatchersIntegrationTests
 * @see XpathRequestMatchersIntegrationTests
 */
public class ContentRequestMatchersIntegrationTests {

  private MockRestServiceServer mockServer;

  private RestTemplate restTemplate;

  @BeforeEach
  public void setup() {
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new StringHttpMessageConverter());
    converters.add(new MappingJackson2HttpMessageConverter());

    this.restTemplate = new RestTemplate();
    this.restTemplate.setMessageConverters(converters);

    this.mockServer = MockRestServiceServer.createServer(this.restTemplate);
  }

  @Test
  public void contentType() throws Exception {
    this.mockServer.expect(content().contentType("application/json")).andRespond(withSuccess());
    executeAndVerify(new Person());
  }

  @Test
  public void contentTypeNoMatch() throws Exception {
    this.mockServer.expect(content().contentType("application/json;charset=UTF-8")).andRespond(withSuccess());
    try {
      executeAndVerify("foo");
    }
    catch (AssertionError error) {
      String message = error.getMessage();
      assertThat(message.startsWith("Content type expected:<application/json;charset=UTF-8>")).as(message).isTrue();
    }
  }

  @Test
  public void contentAsString() throws Exception {
    this.mockServer.expect(content().string("foo")).andRespond(withSuccess());
    executeAndVerify("foo");
  }

  @Test
  public void contentStringStartsWith() throws Exception {
    this.mockServer.expect(content().string(startsWith("foo"))).andRespond(withSuccess());
    executeAndVerify("foo123");
  }

  @Test
  public void contentAsBytes() throws Exception {
    this.mockServer.expect(content().bytes("foo".getBytes())).andRespond(withSuccess());
    executeAndVerify("foo");
  }

  private void executeAndVerify(Object body) throws URISyntaxException {
    this.restTemplate.put(new URI("/foo"), body);
    this.mockServer.verify();
  }

}
