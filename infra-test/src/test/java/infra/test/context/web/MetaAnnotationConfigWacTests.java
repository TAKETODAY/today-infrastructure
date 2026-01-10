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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;

import infra.beans.factory.annotation.Autowired;
import infra.mock.api.MockContext;
import infra.test.context.ContextConfiguration;
import infra.test.context.junit.jupiter.InfraExtension;
import infra.web.mock.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that verifies meta-annotation support for {@link WebAppConfiguration}
 * and {@link ContextConfiguration}.
 *
 * @author Sam Brannen
 * @see WebTestConfiguration
 * @since 4.0
 */
@ExtendWith(InfraExtension.class)
@WebTestConfiguration
class MetaAnnotationConfigWacTests {

  @Autowired
  WebApplicationContext wac;

  @Autowired
  MockContext mockContext;

  @Autowired
  String foo;

  @Test
  void fooEnigmaAutowired() {
    assertThat(foo).isEqualTo("enigma");
  }

  @Test
  void basicWacFeatures() throws Exception {
    assertThat(wac.getMockContext()).as("MockContext should be set in the WAC.").isNotNull();

    assertThat(mockContext).as("MockContext should have been autowired from the WAC.").isNotNull();

    Object rootWac = mockContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
    assertThat(rootWac).as("Root WAC must be stored in the MockContext as: "
            + WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE).isNotNull();
    assertThat(rootWac).as("test WAC and Root WAC in MockContext must be the same object.").isSameAs(wac);
    assertThat(wac.getMockContext()).as("MockContext instances must be the same object.").isSameAs(mockContext);

    assertThat(mockContext.getRealPath("index.jsp")).as("Getting real path for MockContext resource.").isEqualTo(new File("src/main/webapp/index.jsp").getCanonicalPath());
  }

}
