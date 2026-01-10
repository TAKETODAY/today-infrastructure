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

package infra.demo;

import infra.app.Application;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.web.annotation.GET;
import infra.web.annotation.RestController;
import infra.web.config.annotation.EnableWebMvc;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/14 22:22
 */
@EnableWebMvc
@RestController
@EnableAutoConfiguration
public class DemoApp {

  public static void main(String[] args) {
//    WebApplication.run(DemoApp.class, args);
    Application.forBuilder()
            .sources(DemoApp.class)
            .run(args);
  }

  @GET("/index")
  public String index() {
    return "Hello World!";
  }

}
