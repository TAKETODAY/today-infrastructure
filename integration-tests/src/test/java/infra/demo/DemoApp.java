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
