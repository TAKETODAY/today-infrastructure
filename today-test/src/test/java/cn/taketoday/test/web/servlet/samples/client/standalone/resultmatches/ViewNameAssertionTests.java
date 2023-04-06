/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.test.web.servlet.samples.client.standalone.resultmatches;

import org.junit.jupiter.api.Test;

import cn.taketoday.stereotype.Controller;
import cn.taketoday.test.web.reactive.server.EntityExchangeResult;
import cn.taketoday.test.web.reactive.server.WebTestClient;
import cn.taketoday.test.web.servlet.client.MockMvcWebTestClient;
import cn.taketoday.web.annotation.RequestMapping;

import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.status;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link cn.taketoday.test.web.servlet.samples.standalone.resultmatchers.UrlAssertionTests}.
 *
 * @author Rossen Stoyanchev
 */
public class ViewNameAssertionTests {

  private final WebTestClient client =
          MockMvcWebTestClient.bindToController(new SimpleController())
                  .alwaysExpect(status().isOk())
                  .build();

  @Test
  public void testEqualTo() throws Exception {
    MockMvcWebTestClient.resultActionsFor(performRequest())
            .andExpect(view().name("mySpecialView"))
            .andExpect(view().name(equalTo("mySpecialView")));
  }

  @Test
  public void testHamcrestMatcher() throws Exception {
    MockMvcWebTestClient.resultActionsFor(performRequest())
            .andExpect(view().name(containsString("Special")));
  }

  private EntityExchangeResult<Void> performRequest() {
    return client.get().uri("/").exchange().expectBody().isEmpty();
  }

  @Controller
  private static class SimpleController {

    @RequestMapping("/")
    public String handle() {
      return "mySpecialView";
    }
  }
}
