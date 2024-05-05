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

package cn.taketoday.test.web.mock.samples.standalone;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import cn.taketoday.http.HttpMethod;
import cn.taketoday.mock.web.MockMultipartFile;
import cn.taketoday.mock.web.MockPart;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.test.web.mock.MockMvc;
import cn.taketoday.test.web.mock.request.MockMultipartHttpServletRequestBuilder;
import cn.taketoday.validation.BindingResult;
import cn.taketoday.web.RedirectModel;
import cn.taketoday.web.annotation.PostMapping;
import cn.taketoday.web.annotation.PutMapping;
import cn.taketoday.web.annotation.RequestParam;
import cn.taketoday.web.annotation.RequestPart;
import cn.taketoday.web.multipart.MultipartFile;

import static cn.taketoday.test.web.mock.request.MockMvcRequestBuilders.multipart;
import static cn.taketoday.test.web.mock.request.MockMvcRequestBuilders.post;
import static cn.taketoday.test.web.mock.result.MockMvcResultMatchers.status;
import static cn.taketoday.test.web.mock.setup.MockMvcBuilders.standaloneSetup;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Jaebin Joo
 * @author Sam Brannen
 */
class MultipartControllerTests {

  @ParameterizedTest
  @ValueSource(strings = { "/multipartfile", "/multipartfile-via-put", "/part" })
  void multipartRequestWithSingleFileOrPart(String url) throws Exception {
    byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);

    byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
    MockMultipartFile jsonPart = new MockMultipartFile("json", "json", "application/json", json);

    MockMultipartHttpServletRequestBuilder requestBuilder = switch (url) {
      case "/multipartfile" -> multipart(url).file(new MockMultipartFile("file", "orig", null, fileContent));
      case "/multipartfile-via-put" -> multipart(HttpMethod.PUT, url).file(new MockMultipartFile("file", "orig", null, fileContent));
      default -> multipart(url).part(new MockPart("part", "orig", fileContent));
    };

    standaloneSetup(new MultipartController()).build()
            .perform(requestBuilder.file(jsonPart))
            .andExpect(status().isFound());
  }

  @Test
  void multipartRequestWithSingleFileNotPresent() throws Exception {
    standaloneSetup(new MultipartController()).build()
            .perform(multipart("/multipartfile"))
            .andExpect(status().isFound());
  }

  @Test
  void multipartRequestWithFileArray() throws Exception {
    byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
    MockMultipartFile filePart1 = new MockMultipartFile("file", "orig", null, fileContent);
    MockMultipartFile filePart2 = new MockMultipartFile("file", "orig", null, fileContent);

    byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
    MockMultipartFile jsonPart = new MockMultipartFile("json", "json", "application/json", json);

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
    MockMultipartFile filePart1 = new MockMultipartFile("file", "orig", null, fileContent);
    MockMultipartFile filePart2 = new MockMultipartFile("file", "orig", null, fileContent);

    byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
    MockMultipartFile jsonPart = new MockMultipartFile("json", "json", "application/json", json);

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
    MockMultipartFile filePart = new MockMultipartFile("file", "orig", null, fileContent);

    byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
    MockMultipartFile jsonPart = new MockMultipartFile("json", "json", "application/json", json);

    standaloneSetup(new MultipartController()).build()
            .perform(multipart("/optionalfile").file(filePart).file(jsonPart))
            .andExpect(status().isFound());
  }

  @Test
  void multipartRequestWithOptionalFileNotPresent() throws Exception {
    byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
    MockMultipartFile jsonPart = new MockMultipartFile("json", "json", "application/json", json);

    standaloneSetup(new MultipartController()).build()
            .perform(multipart("/optionalfile").file(jsonPart))
            .andExpect(status().isFound());
  }

  @Test
  void multipartRequestWithOptionalFileArray() throws Exception {
    byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
    MockMultipartFile filePart1 = new MockMultipartFile("file", "orig", null, fileContent);
    MockMultipartFile filePart2 = new MockMultipartFile("file", "orig", null, fileContent);

    byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
    MockMultipartFile jsonPart = new MockMultipartFile("json", "json", "application/json", json);

    standaloneSetup(new MultipartController()).build()
            .perform(multipart("/optionalfilearray").file(filePart1).file(filePart2).file(jsonPart))
            .andExpect(status().isFound());
  }

  @Test
  void multipartRequestWithOptionalFileArrayNotPresent() throws Exception {
    byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
    MockMultipartFile jsonPart = new MockMultipartFile("json", "json", "application/json", json);

    standaloneSetup(new MultipartController()).build()
            .perform(multipart("/optionalfilearray").file(jsonPart))
            .andExpect(status().isFound());
  }

  @Test
  void multipartRequestWithOptionalFileList() throws Exception {
    byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
    MockMultipartFile filePart1 = new MockMultipartFile("file", "orig", null, fileContent);
    MockMultipartFile filePart2 = new MockMultipartFile("file", "orig", null, fileContent);

    byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
    MockMultipartFile jsonPart = new MockMultipartFile("json", "json", "application/json", json);

    standaloneSetup(new MultipartController()).build()
            .perform(multipart("/optionalfilelist").file(filePart1).file(filePart2).file(jsonPart))
            .andExpect(status().isFound());
  }

  @Test
  void multipartRequestWithOptionalFileListNotPresent() throws Exception {
    byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
    MockMultipartFile jsonPart = new MockMultipartFile("json", "json", "application/json", json);

    standaloneSetup(new MultipartController()).build()
            .perform(multipart("/optionalfilelist").file(jsonPart))
            .andExpect(status().isFound());
  }

  @Test
  void multipartRequestWithDataBindingToFile() throws Exception {
    byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
    MockPart filePart = new MockPart("file", "orig", fileContent);

    standaloneSetup(new MultipartController()).build()
            .perform(multipart("/multipartfilebinding").part(filePart))
            .andExpect(status().isFound());
  }

  @Test
    // SPR-13317
  void multipartRequestWrapped() throws Exception {
    byte[] json = "{\"name\":\"yeeeah\"}".getBytes(StandardCharsets.UTF_8);
    MockMultipartFile jsonPart = new MockMultipartFile("json", "json", "application/json", json);

//    Filter filter = new RequestWrappingFilter();
    MockMvc mockMvc = standaloneSetup(new MultipartController())./*addFilter(filter).*/build();

    mockMvc.perform(multipart("/json").file(jsonPart)).andExpect(status().isFound());
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  @Controller
  private static class MultipartController {

    @PostMapping("/multipartfile")
    public String processMultipartFile(@RequestParam(required = false) MultipartFile file,
            @RequestPart(required = false) Map<String, String> json) throws IOException {

      return "redirect:/index";
    }

    @PutMapping("/multipartfile-via-put")
    public String processMultipartFileViaHttpPut(@RequestParam(required = false) MultipartFile file,
            @RequestPart(required = false) Map<String, String> json) throws IOException {

      return processMultipartFile(file, json);
    }

    @PostMapping("/multipartfilearray")
    public String processMultipartFileArray(@RequestParam(required = false) MultipartFile[] file,
            @RequestPart(required = false) Map<String, String> json) throws IOException {

      if (file != null && file.length > 0) {
        byte[] content = file[0].getBytes();
        assertThat(file[1].getBytes()).isEqualTo(content);
      }
      return "redirect:/index";
    }

    @PostMapping("/multipartfilelist")
    public String processMultipartFileList(@RequestParam(required = false) List<MultipartFile> file,
            @RequestPart(required = false) Map<String, String> json) throws IOException {

      if (file != null && !file.isEmpty()) {
        byte[] content = file.get(0).getBytes();
        assertThat(file.get(1).getBytes()).isEqualTo(content);
      }
      return "redirect:/index";
    }

    @PostMapping("/optionalfile")
    public String processOptionalFile(
            @RequestParam Optional<MultipartFile> file, @RequestPart Map<String, String> json) {

      return "redirect:/index";
    }

    @PostMapping("/optionalfilearray")
    public String processOptionalFileArray(
            @RequestParam Optional<MultipartFile[]> file, @RequestPart Map<String, String> json)
            throws IOException {

      if (file.isPresent()) {
        byte[] content = file.get()[0].getBytes();
        assertThat(file.get()[1].getBytes()).isEqualTo(content);
      }
      return "redirect:/index";
    }

    @PostMapping("/optionalfilelist")
    public String processOptionalFileList(
            @RequestParam Optional<List<MultipartFile>> file, @RequestPart Map<String, String> json)
            throws IOException {

      if (file.isPresent()) {
        byte[] content = file.get().get(0).getBytes();
        assertThat(file.get().get(1).getBytes()).isEqualTo(content);
      }

      return "redirect:/index";
    }

    @PostMapping("/part")
    public String processPart(@RequestPart MultipartFile part, @RequestPart Map<String, String> json) {
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
        MultipartFile file = multipartFileBean.getFile();
        if (file != null) {
          model.addAttribute("fileContent", file.getBytes());
        }
      }
      return "redirect:/index";
    }
  }

  private static class MultipartFileBean {

    private MultipartFile file;

    public MultipartFile getFile() {
      return file;
    }

    @SuppressWarnings("unused")
    public void setFile(MultipartFile file) {
      this.file = file;
    }
  }

}
