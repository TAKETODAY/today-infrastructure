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
