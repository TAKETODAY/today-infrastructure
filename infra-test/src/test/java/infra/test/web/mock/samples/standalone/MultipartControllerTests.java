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

package infra.test.web.mock.samples.standalone;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import infra.http.HttpMethod;
import infra.mock.web.MockMemoryFilePart;
import infra.mock.web.MockMemoryPart;
import infra.stereotype.Controller;
import infra.test.web.mock.MockMvc;
import infra.test.web.mock.request.MockMultipartHttpRequestBuilder;
import infra.validation.BindingResult;
import infra.web.RedirectModel;
import infra.web.annotation.PostMapping;
import infra.web.annotation.PutMapping;
import infra.web.annotation.RequestParam;
import infra.web.annotation.RequestPart;
import infra.web.multipart.Part;

import static infra.test.web.mock.request.MockMvcRequestBuilders.multipart;
import static infra.test.web.mock.request.MockMvcRequestBuilders.post;
import static infra.test.web.mock.result.MockMvcResultMatchers.status;
import static infra.test.web.mock.setup.MockMvcBuilders.standaloneSetup;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Jaebin Joo
 * @author Sam Brannen
 */
@Disabled
class MultipartControllerTests {

  @ParameterizedTest
  @ValueSource(strings = { "/Part", "/Part-via-put", "/part" })
  void multipartRequestWithSingleFileOrPart(String url) throws Exception {
    byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);

    byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
    MockMemoryFilePart jsonPart = new MockMemoryFilePart("json", "json", "application/json", json);

    MockMultipartHttpRequestBuilder requestBuilder = switch (url) {
      case "/Part" -> multipart(url).file(new MockMemoryFilePart("file", "orig", null, fileContent));
      case "/Part-via-put" -> multipart(HttpMethod.PUT, url).file(new MockMemoryFilePart("file", "orig", null, fileContent));
      default -> multipart(url).part(new MockMemoryPart("part", "orig", fileContent));
    };

    standaloneSetup(new MultipartController()).build()
            .perform(requestBuilder.file(jsonPart))
            .andExpect(status().isFound());
  }

  @Test
  void multipartRequestWithSingleFileNotPresent() throws Exception {
    standaloneSetup(new MultipartController()).build()
            .perform(multipart("/Part"))
            .andExpect(status().isFound());
  }

  @Test
  void multipartRequestWithFileArray() throws Exception {
    byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
    MockMemoryFilePart filePart1 = new MockMemoryFilePart("file", "orig", null, fileContent);
    MockMemoryFilePart filePart2 = new MockMemoryFilePart("file", "orig", null, fileContent);

    byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
    MockMemoryFilePart jsonPart = new MockMemoryFilePart("json", "json", "application/json", json);

    standaloneSetup(new MultipartController()).build()
            .perform(multipart("/multipartfilearray").file(filePart1).file(filePart2).file(jsonPart))
            .andExpect(status().isFound());
  }

  @Test
  void multipartRequestWithFileArrayNotPresent() throws Exception {
    standaloneSetup(new MultipartController()).build()
            .perform(multipart("/multipartfilearray"))
            .andExpect(status().isFound());
  }

  @Test
  void multipartRequestWithFileArrayNoMultipart() throws Exception {
    standaloneSetup(new MultipartController()).build()
            .perform(post("/multipartfilearray"))
            .andExpect(status().isFound());
  }

  @Test
  void multipartRequestWithFileList() throws Exception {
    byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
    MockMemoryFilePart filePart1 = new MockMemoryFilePart("file", "orig", null, fileContent);
    MockMemoryFilePart filePart2 = new MockMemoryFilePart("file", "orig", null, fileContent);

    byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
    MockMemoryFilePart jsonPart = new MockMemoryFilePart("json", "json", "application/json", json);

    standaloneSetup(new MultipartController()).build()
            .perform(multipart("/multipartfilelist").file(filePart1).file(filePart2).file(jsonPart))
            .andExpect(status().isFound());
  }

  @Test
  void multipartRequestWithFileListNotPresent() throws Exception {
    standaloneSetup(new MultipartController()).build()
            .perform(multipart("/multipartfilelist"))
            .andExpect(status().isFound());
  }

  @Test
  void multipartRequestWithFileListNoMultipart() throws Exception {
    standaloneSetup(new MultipartController()).build()
            .perform(post("/multipartfilelist"))
            .andExpect(status().isFound());
  }

  @Test
  void multipartRequestWithOptionalFile() throws Exception {
    byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
    MockMemoryFilePart filePart = new MockMemoryFilePart("file", "orig", null, fileContent);

    byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
    MockMemoryFilePart jsonPart = new MockMemoryFilePart("json", "json", "application/json", json);

    standaloneSetup(new MultipartController()).build()
            .perform(multipart("/optionalfile").file(filePart).file(jsonPart))
            .andExpect(status().isFound());
  }

  @Test
  void multipartRequestWithOptionalFileNotPresent() throws Exception {
    byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
    MockMemoryFilePart jsonPart = new MockMemoryFilePart("json", "json", "application/json", json);

    standaloneSetup(new MultipartController()).build()
            .perform(multipart("/optionalfile").file(jsonPart))
            .andExpect(status().isFound());
  }

  @Test
  void multipartRequestWithOptionalFileArray() throws Exception {
    byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
    MockMemoryFilePart filePart1 = new MockMemoryFilePart("file", "orig", null, fileContent);
    MockMemoryFilePart filePart2 = new MockMemoryFilePart("file", "orig", null, fileContent);

    byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
    MockMemoryFilePart jsonPart = new MockMemoryFilePart("json", "json", "application/json", json);

    standaloneSetup(new MultipartController()).build()
            .perform(multipart("/optionalfilearray").file(filePart1).file(filePart2).file(jsonPart))
            .andExpect(status().isFound());
  }

  @Test
  void multipartRequestWithOptionalFileArrayNotPresent() throws Exception {
    byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
    MockMemoryFilePart jsonPart = new MockMemoryFilePart("json", "json", "application/json", json);

    standaloneSetup(new MultipartController()).build()
            .perform(multipart("/optionalfilearray").file(jsonPart))
            .andExpect(status().isFound());
  }

  @Test
  void multipartRequestWithOptionalFileList() throws Exception {
    byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
    MockMemoryFilePart filePart1 = new MockMemoryFilePart("file", "orig", null, fileContent);
    MockMemoryFilePart filePart2 = new MockMemoryFilePart("file", "orig", null, fileContent);

    byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
    MockMemoryFilePart jsonPart = new MockMemoryFilePart("json", "json", "application/json", json);

    standaloneSetup(new MultipartController()).build()
            .perform(multipart("/optionalfilelist").file(filePart1).file(filePart2).file(jsonPart))
            .andExpect(status().isFound());
  }

  @Test
  void multipartRequestWithOptionalFileListNotPresent() throws Exception {
    byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
    MockMemoryFilePart jsonPart = new MockMemoryFilePart("json", "json", "application/json", json);

    standaloneSetup(new MultipartController()).build()
            .perform(multipart("/optionalfilelist").file(jsonPart))
            .andExpect(status().isFound());
  }

  @Test
  void multipartRequestWithDataBindingToFile() throws Exception {
    byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
    MockMemoryPart filePart = new MockMemoryPart("file", "orig", fileContent);

    standaloneSetup(new MultipartController()).build()
            .perform(multipart("/multipartfilebinding").part(filePart))
            .andExpect(status().isFound());
  }

  @Test
  void multipartRequestWrapped() throws Exception {
    byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
    MockMemoryFilePart jsonPart = new MockMemoryFilePart("json", "json", "application/json", json);

//    Filter filter = new RequestWrappingFilter();
    MockMvc mockMvc = standaloneSetup(new MultipartController())./*addFilter(filter).*/build();

    mockMvc.perform(multipart("/json").file(jsonPart)).andExpect(status().isFound());
  }

  @Controller
  private static class MultipartController {

    @PostMapping("/Part")
    public String processMultipartFile(@RequestParam(required = false) Part file,
            @RequestPart(required = false) Map<String, String> json) throws IOException {

      return "redirect:/index";
    }

    @PutMapping("/Part-via-put")
    public String processMultipartFileViaHttpPut(@RequestParam(required = false) Part file,
            @RequestPart(required = false) Map<String, String> json) throws IOException {

      return processMultipartFile(file, json);
    }

    @PostMapping("/multipartfilearray")
    public String processMultipartFileArray(@RequestParam(required = false) Part[] file,
            @RequestPart(required = false) Map<String, String> json) throws IOException {

      if (file != null && file.length > 0) {
        byte[] content = file[0].getContentAsByteArray();
        assertThat(file[1].getContentAsByteArray()).isEqualTo(content);
      }
      return "redirect:/index";
    }

    @PostMapping("/multipartfilelist")
    public String processMultipartFileList(@RequestParam(required = false) List<Part> file,
            @RequestPart(required = false) Map<String, String> json) throws IOException {

      if (file != null && !file.isEmpty()) {
        byte[] content = file.get(0).getContentAsByteArray();
        assertThat(file.get(1).getContentAsByteArray()).isEqualTo(content);
      }
      return "redirect:/index";
    }

    @PostMapping("/optionalfile")
    public String processOptionalFile(
            @RequestParam @Nullable Part file, @RequestPart Map<String, String> json) {

      return "redirect:/index";
    }

    @PostMapping("/optionalfilearray")
    public String processOptionalFileArray(
            @RequestParam Part @Nullable [] file, @RequestPart Map<String, String> json)
            throws IOException {

      if (file != null) {
        byte[] content = file[0].getContentAsByteArray();
        assertThat(file[1].getContentAsByteArray()).isEqualTo(content);
      }
      return "redirect:/index";
    }

    @PostMapping("/optionalfilelist")
    public String processOptionalFileList(
            @RequestParam @Nullable List<Part> file, @RequestPart Map<String, String> json)
            throws IOException {

      if (file != null) {
        byte[] content = file.get(0).getContentAsByteArray();
        assertThat(file.get(1).getContentAsByteArray()).isEqualTo(content);
      }

      return "redirect:/index";
    }

    @PostMapping("/part")
    public String processPart(@RequestPart Part part, @RequestPart Map<String, String> json) {
      return "redirect:/index";
    }

    @PostMapping("/json")
    public String processMultipart(@RequestPart Map<String, String> json) {
      return "redirect:/index";
    }

    @PostMapping("/multipartfilebinding")
    public String processMultipartFileBean(
            MultipartFileBean multipartFileBean, RedirectModel model, BindingResult bindingResult)
            throws IOException {

      if (!bindingResult.hasErrors()) {
        Part file = multipartFileBean.getFile();
        if (file != null) {
          model.addAttribute("fileContent", file.getContentAsByteArray());
        }
      }
      return "redirect:/index";
    }
  }

  private static class MultipartFileBean {

    private Part file;

    public Part getFile() {
      return file;
    }

    @SuppressWarnings("unused")
    public void setFile(Part file) {
      this.file = file;
    }
  }

}
