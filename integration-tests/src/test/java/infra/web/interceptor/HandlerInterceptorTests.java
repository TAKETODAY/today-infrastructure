/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.interceptor;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import infra.beans.testfixture.beans.TestBean;
import infra.test.classpath.ClassPathExclusions;
import infra.web.HandlerInterceptor;
import infra.web.RequestContext;
import infra.web.annotation.GET;
import infra.web.annotation.Interceptor;
import infra.web.annotation.RestController;

import static infra.test.web.mock.request.MockMvcRequestBuilders.get;
import static infra.test.web.mock.result.MockMvcResultMatchers.jsonPath;
import static infra.test.web.mock.result.MockMvcResultMatchers.status;
import static infra.test.web.mock.setup.MockMvcBuilders.standaloneSetup;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/9/16 22:09
 */
@ClassPathExclusions("jackson-dataformat-xml*")
class HandlerInterceptorTests {

  @Test
  void modifyResults() throws Exception {
    standaloneSetup(new InterceptedController())
            .perform(get("/list"))
            .andExpect(status().is(200))
            .andExpect(jsonPath("$[0].name").value("TODAY"))
            .andExpect(jsonPath("$[1].name").value("Harry"))
            .andExpect(jsonPath("$[2].name").value("add"))

    ;

  }

  @RestController
  @Interceptor(ModifyResult.class)
  static class InterceptedController {

    @GET("/list")
    public List<TestBean> list() {
      ArrayList<TestBean> testBeans = new ArrayList<>();
      testBeans.add(new TestBean("TODAY"));
      testBeans.add(new TestBean("Harry"));
      return testBeans;
    }

  }

  static class ModifyResult implements HandlerInterceptor {

    @Override
    public void afterProcess(RequestContext request, Object handler, Object result) {
      if (result instanceof List list) {
        list.add(new TestBean("add"));
      }
    }

  }

}
