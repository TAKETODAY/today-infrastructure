/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.test.web.mock.samples.client.standalone;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import infra.http.MediaType;
import infra.http.client.MultipartBodyBuilder;
import infra.mock.api.http.Part;
import infra.stereotype.Controller;
import infra.test.web.mock.client.MockMvcWebTestClient;
import infra.test.web.reactive.server.WebTestClient;
import infra.web.annotation.PostMapping;
import infra.web.annotation.PutMapping;
import infra.web.annotation.RequestParam;
import infra.web.annotation.RequestPart;
import infra.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link infra.test.web.mock.samples.standalone.MultipartControllerTests}.
 *
 * @author Rossen Stoyanchev
 */
public class MultipartControllerTests {

  private final WebTestClient testClient = MockMvcWebTestClient.bindToController(new MultipartController()).build();

  @Test
  public void multipartRequestWithSingleFile() throws Exception {

    byte[] fileContent = "bar".getBytes(StandardCharsets.UTF_8);
    Map<String, String> json = Collections.singletonMap("name", "yeeeah");

    MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
    bodyBuilder.part("file", fileContent).filename("orig");
    bodyBuilder.part("json", json, MediaType.APPLICATION_JSON);

    testClient.post().uri("/multipartfile")
            .bodyValue(bodyBuilder.build())
            .exchange()
            .expectStatus().isFound()
            .expectBody().isEmpty();

    // Now try the same with HTTP PUT
    testClient.put().uri("/multipartfile-via-put")
            .bodyValue(bodyBuilder.build())
            .exchange()
            .expectStatus().isFound()
            .expectBody().isEmpty();
  }

  @Test
  public void multipartRequestWithSingleFileNotPresent() {
    testClient.post().uri("/multipartfile")
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

    testClient.post().uri("/multipartfile")
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

    client.post().uri("/multipartfile")
            .bodyValue(bodyBuilder.build())
            .exchange()
            .expectStatus().isFound()
            .expectBody().isEmpty();
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  @Controller
  private static class MultipartController {

    @PostMapping("/multipartfile")
    public String processMultipartFile(@RequestParam(required = false) MultipartFile file,
            @RequestPart(required = false) Map<String, String> json) {

      return "redirect:/index";
    }

    @PutMapping("/multipartfile-via-put")
    public String processMultipartFileViaHttpPut(@RequestParam(required = false) MultipartFile file,
            @RequestPart(required = false) Map<String, String> json) {

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
            @RequestParam @Nullable MultipartFile file, @RequestPart Map<String, String> json) {

      return "redirect:/index";
    }

    @PostMapping("/optionalfilearray")
    public String processOptionalFileArray(
            @RequestParam MultipartFile @Nullable [] file, @RequestPart Map<String, String> json) throws IOException {

      if (file != null) {
        byte[] content = file[0].getBytes();
        assertThat(file[1].getBytes()).isEqualTo(content);
      }
      return "redirect:/index";
    }

    @PostMapping("/optionalfilelist")
    public String processOptionalFileList(
            @RequestParam @Nullable List<MultipartFile> file, @RequestPart Map<String, String> json)
            throws IOException {

      if (file != null) {
        byte[] content = file.get(0).getBytes();
        assertThat(file.get(1).getBytes()).isEqualTo(content);
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
