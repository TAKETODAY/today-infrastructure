/*
 * Copyright 2012-present the original author or authors.
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

package infra.app.webmvc.test.config.mockmvc;

import infra.app.webmvc.test.config.WebMvcTest;
import infra.stereotype.Controller;
import infra.web.annotation.GET;
import infra.web.annotation.POST;
import infra.web.annotation.PathVariable;
import infra.web.annotation.ResponseBody;

/**
 * Example {@link Controller @Controller} used with {@link WebMvcTest @WebMvcTest} tests.
 *
 * @author Phillip Webb
 */
@Controller
public class ExampleController2 {

  @GET("/two")
  @ResponseBody
  public String two(ExampleArgument argument) {
    return argument + "two";
  }

  @GET("/two/{id}")
  @ResponseBody
  public String two(@PathVariable ExampleId id) {
    return id.getId() + "two";
  }

  @POST("/two/{id}")
  @ResponseBody
  public ExampleResult twoUpdate(@PathVariable String id) {
    return new ExampleResult(id);
  }

}
