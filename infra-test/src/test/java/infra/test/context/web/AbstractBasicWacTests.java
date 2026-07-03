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

package infra.test.context.web;

import org.junit.Test;
import org.junit.runner.RunWith;

import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.test.context.junit4.InfraRunner;
import infra.web.RequestContext;
import infra.web.mock.MockRequest;
import infra.web.mock.MockResponse;
import infra.web.mock.MockSession;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 4.0
 */
@RunWith(InfraRunner.class)
@WebAppConfiguration
public abstract class AbstractBasicWacTests {

  @Autowired
  protected ApplicationContext wac;

  @Autowired
  protected MockRequest request;

  @Autowired
  protected MockResponse response;

  @Autowired
  protected MockSession session;

  @Autowired
  protected RequestContext webRequest;

  @Autowired
  protected String foo;

  @Test
  public void basicWacFeatures() throws Exception {

    assertThat(request).as("MockRequest should have been autowired from the WAC.").isNotNull();
    assertThat(response).as("MockResponse should have been autowired from the WAC.").isNotNull();
    assertThat(session).as("MockSession should have been autowired from the WAC.").isNotNull();
    assertThat(webRequest).as("RequestContext should have been autowired from the WAC.").isNotNull();

  }

}
