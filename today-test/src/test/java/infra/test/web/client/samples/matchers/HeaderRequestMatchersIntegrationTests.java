/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
import infra.http.converter.json.JacksonJsonHttpMessageConverter;
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
    converters.add(new JacksonJsonHttpMessageConverter());

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
