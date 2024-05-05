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

package cn.taketoday.web.interceptor;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.test.classpath.ClassPathExclusions;
import cn.taketoday.web.HandlerInterceptor;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.Interceptor;
import cn.taketoday.web.annotation.RestController;

import static cn.taketoday.test.web.mock.request.MockMvcRequestBuilders.get;
import static cn.taketoday.test.web.mock.result.MockMvcResultMatchers.jsonPath;
import static cn.taketoday.test.web.mock.result.MockMvcResultMatchers.status;
import static cn.taketoday.test.web.mock.setup.MockMvcBuilders.standaloneSetup;

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
