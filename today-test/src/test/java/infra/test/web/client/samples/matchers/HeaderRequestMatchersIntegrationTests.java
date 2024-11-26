/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.test.web.client.samples.matchers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import infra.http.MediaType;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.StringHttpMessageConverter;
import infra.http.converter.json.MappingJackson2HttpMessageConverter;
import infra.test.web.Person;
import infra.test.web.client.MockRestServiceServer;
import infra.web.client.RestTemplate;

import static infra.test.web.client.match.MockRestRequestMatchers.header;
import static infra.test.web.client.match.MockRestRequestMatchers.requestTo;
import static infra.test.web.client.response.MockRestResponseCreators.withSuccess;
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
