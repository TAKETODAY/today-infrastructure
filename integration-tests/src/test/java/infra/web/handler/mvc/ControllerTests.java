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

package infra.web.handler.mvc;

import org.junit.jupiter.api.Test;

import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.mock.web.MockRequest;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.mock.MockRequestContext;
import infra.web.view.ModelAndView;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/8 17:32
 */
class ControllerTests {

  @Test
  public void parameterizableViewController() throws Throwable {
    String viewName = "viewName";
    ParameterizableViewController pvc = new ParameterizableViewController();
    pvc.setViewName(viewName);
    // We don't care about the params.
    ConfigurableApplicationContext wac = new AnnotationConfigApplicationContext();
    wac.refresh();
    MockRequestContext context = new MockRequestContext(wac, new MockRequest("GET", "foo.html"), new MockHttpResponseImpl());
    ModelAndView mv = (ModelAndView) pvc.handleRequest(context);
    assertThat(mv.getModel().size() == 0).as("model has no data").isTrue();
    assertThat(mv.getViewName().equals(viewName)).as("model has correct viewname").isTrue();
    assertThat(pvc.getViewName().equals(viewName)).as("getViewName matches").isTrue();
  }

}
