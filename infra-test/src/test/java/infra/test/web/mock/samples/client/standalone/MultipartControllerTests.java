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

package infra.test.web.mock.samples.client.standalone;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import infra.http.MediaType;
import infra.http.codec.multipart.MultipartBodyBuilder;
import infra.stereotype.Controller;
import infra.test.web.mock.client.MockMvcWebTestClient;
import infra.test.web.reactive.server.WebTestClient;
import infra.web.annotation.PostMapping;
import infra.web.annotation.PutMapping;
import infra.web.annotation.RequestParam;
import infra.web.annotation.RequestPart;
import infra.web.multipart.Part;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link infra.test.web.mock.samples.standalone.MultipartControllerTests}.
 *
 * @author Rossen Stoyanchev
 */
class MultipartControllerTests {

  private final WebTestClient testClient = MockMvcWebTestClient.bindToController(new MultipartController()).build();

  @Test
  public void multipartRequestWithSingleFile() throws Exception {

    byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
    Map<String, String> json = Collections.singletonMap("name", "yeeeah");

    MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
    bodyBuilder.part("file", fileContent).filename("orig");
    bodyBuilder.part("json", json, MediaType.APPLICATION_JSON);

    testClient.post().uri("/Part")
            .bodyValue(bodyBuilder.build())
            .exchange()
            .expectStatus().isFound()
            .expectBody().isEmpty();

    // Now try the same with HTTP PUT
    testClient.put().uri("/Part-via-put")
            .bodyValue(bodyBuilder.build())
            .exchange()
            .expectStatus().isFound()
            .expectBody().isEmpty();
  }

  @Test
  public void multipartRequestWithSingleFileNotPresent() {
    testClient.post().uri("/Part")
            .exchange()
            .expectStatus().isFound();
  }

  @Test
  public void multipartRequestWithFileArray() throws Exception {
    byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
    Map<String, String> json = Collections.singletonMap("name", "yeeeah");

    MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
    bodyBuilder.part("file", fileContent).filename("orig");
    bodyBuilder.part("file", fileContent).filename("orig");
    bodyBuilder.part("json", json, MediaType.APPLICATION_JSON);

    testClient.post().uri("/multipartfilearray")
            .bodyValue(bodyBuilder.build())
            .exchange()
            .expectStatus().isFound()
            .expectBody().isEmpty();
  }

  @Test
  public void multipartRequestWithFileArrayNoMultipart() {
    testClient.post().uri("/multipartfilearray")
            .exchange()
            .expectStatus().isFound();
  }

  @Test
  public void multipartRequestWithOptionalFile() throws Exception {
    byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
    Map<String, String> json = Collections.singletonMap("name", "yeeeah");

    MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
    bodyBuilder.part("file", fileContent).filename("orig");
    bodyBuilder.part("json", json, MediaType.APPLICATION_JSON);

    testClient.post().uri("/optionalfile")
            .bodyValue(bodyBuilder.build())
            .exchange()
            .expectStatus().isFound()
            .expectBody().isEmpty();
  }

  @Test
  public void multipartRequestWithOptionalFileNotPresent() throws Exception {
    Map<String, String> json = Collections.singletonMap("name", "yeeeah");

    MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
    bodyBuilder.part("json", json, MediaType.APPLICATION_JSON);

    testClient.post().uri("/optionalfile")
            .bodyValue(bodyBuilder.build())
            .exchange()
            .expectStatus().isFound()
            .expectBody().isEmpty();
  }

  @Test
  public void multipartRequestWithOptionalFileArray() throws Exception {
    byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
    Map<String, String> json = Collections.singletonMap("name", "yeeeah");

    MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
    bodyBuilder.part("file", fileContent).filename("orig");
    bodyBuilder.part("file", fileContent).filename("orig");
    bodyBuilder.part("json", json, MediaType.APPLICATION_JSON);

    testClient.post().uri("/optionalfilearray")
            .bodyValue(bodyBuilder.build())
            .exchange()
            .expectStatus().isFound()
            .expectBody().isEmpty();
  }

  @Test
  public void multipartRequestWithOptionalFileArrayNotPresent() throws Exception {
    Map<String, String> json = Collections.singletonMap("name", "yeeeah");

    MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
    bodyBuilder.part("json", json, MediaType.APPLICATION_JSON);

    testClient.post().uri("/optionalfilearray")
            .bodyValue(bodyBuilder.build())
            .exchange()
            .expectStatus().isFound()
            .expectBody().isEmpty();
  }

  @Test
  public void multipartRequestWithOptionalFileList() throws Exception {
    byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
    Map<String, String> json = Collections.singletonMap("name", "yeeeah");

    MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
    bodyBuilder.part("file", fileContent).filename("orig");
    bodyBuilder.part("file", fileContent).filename("orig");
    bodyBuilder.part("json", json, MediaType.APPLICATION_JSON);

    testClient.post().uri("/optionalfilelist")
            .bodyValue(bodyBuilder.build())
            .exchange()
            .expectStatus().isFound()
            .expectBody().isEmpty();
  }

  @Test
  public void multipartRequestWithOptionalFileListNotPresent() throws Exception {
    Map<String, String> json = Collections.singletonMap("name", "yeeeah");

    MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
    bodyBuilder.part("json", json, MediaType.APPLICATION_JSON);

    testClient.post().uri("/optionalfilelist")
            .bodyValue(bodyBuilder.build())
            .exchange()
            .expectStatus().isFound()
            .expectBody().isEmpty();
  }

  @Test
  public void multipartRequestWithMockParts() throws Exception {
    byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
    Map<String, String> json = Collections.singletonMap("name", "yeeeah");

    MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
    bodyBuilder.part("file", fileContent).filename("orig");
    bodyBuilder.part("json", json, MediaType.APPLICATION_JSON);

    testClient.post().uri("/Part")
            .bodyValue(bodyBuilder.build())
            .exchange()
            .expectStatus().isFound()
            .expectBody().isEmpty();
  }

  @Test
  public void multipartRequestWrapped() throws Exception {
    Map<String, String> json = Collections.singletonMap("name", "yeeeah");

    MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
    bodyBuilder.part("json", json, MediaType.APPLICATION_JSON);

    WebTestClient client = MockMvcWebTestClient.bindToController(new MultipartController())
//            .filter(new RequestWrappingFilter())
            .build();

    client.post().uri("/Part")
            .bodyValue(bodyBuilder.build())
            .exchange()
            .expectStatus().isFound()
            .expectBody().isEmpty();
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  @Controller
  private static class MultipartController {

    @PostMapping("/Part")
    public String processMultipartFile(@RequestParam(required = false) Part file,
            @RequestPart(required = false) Map<String, String> json) {

      return "redirect:/index";
    }

    @PutMapping("/Part-via-put")
    public String processMultipartFileViaHttpPut(@RequestParam(required = false) Part file,
            @RequestPart(required = false) Map<String, String> json) {

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
            @RequestParam Part @Nullable [] file, @RequestPart Map<String, String> json) throws IOException {

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
    public String processPart(@RequestParam Part part, @RequestPart Map<String, String> json) {
      return "redirect:/index";
    }

    @PostMapping("/json")
    public String processMultipart(@RequestPart Map<String, String> json) {
      return "redirect:/index";
    }
  }

}
