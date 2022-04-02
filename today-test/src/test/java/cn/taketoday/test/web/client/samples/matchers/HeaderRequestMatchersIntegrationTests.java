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

import cn.taketoday.http.MediaType;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.StringHttpMessageConverter;
import cn.taketoday.http.converter.json.MappingJackson2HttpMessageConverter;
import cn.taketoday.test.web.Person;
import cn.taketoday.test.web.client.MockRestServiceServer;
import cn.taketoday.web.client.RestTemplate;

import static cn.taketoday.test.web.client.match.MockRestRequestMatchers.header;
import static cn.taketoday.test.web.client.match.MockRestRequestMatchers.requestTo;
import static cn.taketoday.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.containsString;

/**
 * Examples of defining expectations on request headers.
 *
 * @author Rossen Stoyanchev
 */
public class HeaderRequestMatchersIntegrationTests {

  private static final String RESPONSE_BODY = "{\"name\" : \"Ludwig van Beethoven\", \"someDouble\" : \"1.6035\"}";

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
  public void testString() throws Exception {
    this.mockServer.expect(requestTo("/person/1"))
            .andExpect(header("Accept", "application/json, application/*+json"))
            .andRespond(withSuccess(RESPONSE_BODY, MediaType.APPLICATION_JSON));

    executeAndVerify();
  }

  @Test
  public void testStringContains() throws Exception {
    this.mockServer.expect(requestTo("/person/1"))
            .andExpect(header("Accept", containsString("json")))
            .andRespond(withSuccess(RESPONSE_BODY, MediaType.APPLICATION_JSON));

    executeAndVerify();
  }

  private void executeAndVerify() throws URISyntaxException {
    this.restTemplate.getForObject(new URI("/person/1"), Person.class);
    this.mockServer.verify();
  }

}
