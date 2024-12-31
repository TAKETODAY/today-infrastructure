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

package infra.test.web.mock.samples.spr;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import infra.test.context.junit.jupiter.web.JUnitWebConfig;
import infra.test.web.mock.MockMvc;
import infra.web.annotation.PostMapping;
import infra.web.annotation.RestController;
import infra.web.bind.annotation.ModelAttribute;
import infra.web.mock.WebApplicationContext;

import static infra.test.web.mock.request.MockMvcRequestBuilders.post;
import static infra.test.web.mock.result.MockMvcResultMatchers.content;
import static infra.test.web.mock.result.MockMvcResultMatchers.status;
import static infra.test.web.mock.setup.MockMvcBuilders.webAppContextSetup;

@JUnitWebConfig(RequestDataBinderIntegrationTests.WebKeyValueController.class)
class RequestDataBinderIntegrationTests {

  @Test
  void postMap(WebApplicationContext wac) throws Exception {
    MockMvc mockMvc = webAppContextSetup(wac).build();
    mockMvc.perform(post("/map")
                    .param("someMap[a]", "valueA")
                    .param("someMap[b]", "valueB"))
            .andExpect(status().isOk())
            .andExpect(content().string("valueB"));
  }

  @Test
  void postArray(WebApplicationContext wac) throws Exception {
    MockMvc mockMvc = webAppContextSetup(wac).build();
    mockMvc.perform(post("/array")
                    .param("someArray[0]", "valueA")
                    .param("someArray[1]", "valueB"))
            .andExpect(status().isOk())
            .andExpect(content().string("valueB"));
  }

  @Test
  void postArrayWithEmptyIndex(WebApplicationContext wac) throws Exception {
    MockMvc mockMvc = webAppContextSetup(wac).build();
    mockMvc.perform(post("/array")
                    .param("someArray[]", "valueA")
                    .param("someArray[]", "valueB"))
            .andExpect(status().isOk())
            .andExpect(content().string("valueB"));
  }

  @Test
  void postArrayWithoutIndex(WebApplicationContext wac) throws Exception {
    MockMvc mockMvc = webAppContextSetup(wac).build();
    mockMvc.perform(post("/array")
                    .param("someArray", "valueA")
                    .param("someArray", "valueB"))
            .andExpect(status().isOk())
            .andExpect(content().string("valueB"));
  }

  @Test
  void postList(WebApplicationContext wac) throws Exception {
    MockMvc mockMvc = webAppContextSetup(wac).build();
    mockMvc.perform(post("/list")
                    .param("someList[0]", "valueA")
                    .param("someList[1]", "valueB"))
            .andExpect(status().isOk())
            .andExpect(content().string("valueB"));
  }

  @Test
  void postListWithEmptyIndex(WebApplicationContext wac) throws Exception {
    MockMvc mockMvc = webAppContextSetup(wac).build();
    mockMvc.perform(post("/list")
                    .param("someList[]", "valueA")
                    .param("someList[]", "valueB"))
            .andExpect(status().isOk())
            .andExpect(content().string("valueB"));
  }

  @Test
  void postListWithoutIndex(WebApplicationContext wac) throws Exception {
    MockMvc mockMvc = webAppContextSetup(wac).build();
    mockMvc.perform(post("/list")
                    .param("someList", "valueA")
                    .param("someList", "valueB"))
            .andExpect(status().isOk())
            .andExpect(content().string("valueB"));
  }

  record PayloadWithMap(Map<String, String> someMap) { }

  record PayloadWithArray(String[] someArray) { }

  record PayloadWithList(List<String> someList) { }

  @RestController
  @SuppressWarnings("unused")
  static class WebKeyValueController {

    @PostMapping("/map")
    String postMap(@ModelAttribute("payload") PayloadWithMap payload) {
      return payload.someMap.get("b");
    }

    @PostMapping("/array")
    String postArray(@ModelAttribute("payload") PayloadWithArray payload) {
      return payload.someArray[1];
    }

    @PostMapping("/list")
    String postList(@ModelAttribute("payload") PayloadWithList payload) {
      return payload.someList.get(1);
    }
  }

}
