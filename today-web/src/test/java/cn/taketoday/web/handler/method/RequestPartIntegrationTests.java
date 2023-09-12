/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.web.handler.method;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.core.ApplicationTemp;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.http.HttpEntity;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.RequestEntity;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.http.client.HttpComponentsClientHttpRequestFactory;
import cn.taketoday.http.converter.AllEncompassingFormHttpMessageConverter;
import cn.taketoday.http.converter.ByteArrayHttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.ResourceHttpMessageConverter;
import cn.taketoday.http.converter.json.MappingJackson2HttpMessageConverter;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.util.FileSystemUtils;
import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MimeTypeUtils;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.RequestPart;
import cn.taketoday.web.client.RestTemplate;
import cn.taketoday.web.config.EnableWebMvc;
import cn.taketoday.web.config.WebMvcConfigurer;
import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.servlet.DispatcherServlet;
import cn.taketoday.web.servlet.support.AnnotationConfigWebApplicationContext;
import jakarta.servlet.MultipartConfigElement;

import static cn.taketoday.http.HttpMethod.POST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test access to parts of a multipart request with {@link RequestPart}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Sam Brannen
 */
class RequestPartIntegrationTests {

  private RestTemplate restTemplate;

  private static Server server;

  private static String baseUrl;

  private static Path tempDirectory;

  @BeforeAll
  static void startServer() throws Exception {
    // Let server pick its own random, available port.
    server = new Server(0);

    tempDirectory = ApplicationTemp.createDirectory("RequestPartIntegrationTests");

    ServletContextHandler handler = new ServletContextHandler();
    handler.setContextPath("/");

    AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
    context.setServletContext(handler.getServletContext());
    context.register(StandardMultipartResolverTestConfig.class);
    context.register(DispatcherServlet.class);
    context.refresh();

    DispatcherServlet bean = context.getBean(DispatcherServlet.class);

    ServletHolder standardResolverServlet = new ServletHolder(bean);
    standardResolverServlet.getRegistration().setMultipartConfig(new MultipartConfigElement(tempDirectory.toString()));
    handler.addServlet(standardResolverServlet, "/standard-resolver/*");

    server.setHandler(handler);
    server.start();

    Connector[] connectors = server.getConnectors();
    NetworkConnector connector = (NetworkConnector) connectors[0];
    baseUrl = "http://localhost:" + connector.getLocalPort();
  }

  @AfterAll
  static void stopServer() throws Exception {
    try {
      if (server != null) {
        server.stop();
      }
    }
    finally {
      FileSystemUtils.deleteRecursively(tempDirectory);
    }
  }

  @BeforeEach
  void setup() {
    ByteArrayHttpMessageConverter emptyBodyConverter = new ByteArrayHttpMessageConverter();
    emptyBodyConverter.setSupportedMediaTypes(Collections.singletonList(MediaType.APPLICATION_JSON));

    List<HttpMessageConverter<?>> converters = new ArrayList<>(3);
    converters.add(emptyBodyConverter);
    converters.add(new ByteArrayHttpMessageConverter());
    converters.add(new ResourceHttpMessageConverter());
    converters.add(new MappingJackson2HttpMessageConverter());

    AllEncompassingFormHttpMessageConverter converter = new AllEncompassingFormHttpMessageConverter();
    converter.setPartConverters(converters);

    restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
    restTemplate.setMessageConverters(Collections.singletonList(converter));
  }

  @Test
  void standardMultipartResolver() throws Exception {
    testCreate(baseUrl + "/standard-resolver/test", "Jason");
    testCreate(baseUrl + "/standard-resolver/test", "Arjen");
  }

  @Test
    // SPR-13319
  void standardMultipartResolverWithEncodedFileName() throws Exception {
    String boundaryText = MimeTypeUtils.generateMultipartBoundaryString();
    Map<String, String> params = Collections.singletonMap("boundary", boundaryText);

    String content =
            "--" + boundaryText + "\r\n" +
                    "Content-Disposition: form-data; name=\"file\"; filename*=\"utf-8''%C3%A9l%C3%A8ve.txt\"\r\n" +
                    "Content-Type: text/plain\r\n" +
                    "Content-Length: 7\r\n" +
                    "\r\n" +
                    "content\r\n" +
                    "--" + boundaryText + "--\r\n ";

    RequestEntity<byte[]> requestEntity =
            RequestEntity.post(URI.create(baseUrl + "/standard-resolver/spr13319"))
                    .contentType(new MediaType(MediaType.MULTIPART_FORM_DATA, params))
                    .body(content.getBytes(StandardCharsets.US_ASCII));

    ByteArrayHttpMessageConverter converter = new ByteArrayHttpMessageConverter();
    converter.setSupportedMediaTypes(Collections.singletonList(MediaType.MULTIPART_FORM_DATA));
    this.restTemplate.setMessageConverters(Collections.singletonList(converter));

    ResponseEntity<Void> responseEntity = restTemplate.exchange(requestEntity, Void.class);
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  private void testCreate(String url, String basename) {
    MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
    parts.add("json-data", new HttpEntity<>(new TestData(basename)));
    parts.add("file-data", new ClassPathResource("logo.svg", getClass()));
    parts.add("empty-data", new HttpEntity<>(new byte[0])); // SPR-12860

    HttpHeaders headers = HttpHeaders.create();
    headers.setContentType(new MediaType("application", "octet-stream", StandardCharsets.ISO_8859_1));
    parts.add("iso-8859-1-data", new HttpEntity<>(new byte[] { (byte) 0xC4 }, headers)); // SPR-13096

    URI location = restTemplate.postForLocation(url, parts);
    assertThat(location.toString()).isEqualTo(("http://localhost:8080/test/" + basename + "/logo.svg"));
  }

  @Configuration
  @EnableWebMvc
  static class RequestPartTestConfig implements WebMvcConfigurer {

    @Bean
    public RequestPartTestController controller() {
      return new RequestPartTestController();
    }
  }

  @Configuration
  @SuppressWarnings("unused")
  @EnableWebMvc
  static class StandardMultipartResolverTestConfig extends RequestPartTestConfig {

  }

  @Controller
  @SuppressWarnings("unused")
  private static class RequestPartTestController {

    @RequestMapping(value = "/test", method = POST, consumes = { "multipart/mixed", "multipart/form-data" })
    public ResponseEntity<Object> create(@RequestPart(name = "json-data") TestData testData,
            @RequestPart("file-data") Optional<MultipartFile> file,
            @RequestPart(name = "empty-data", required = false) TestData emptyData,
            @RequestPart(name = "iso-8859-1-data") byte[] iso88591Data) {

      assertThat(iso88591Data).isEqualTo(new byte[] { (byte) 0xC4 });

      String url = "http://localhost:8080/test/" + testData.getName() + "/" + file.get().getOriginalFilename();
      HttpHeaders headers = HttpHeaders.create();
      headers.setLocation(URI.create(url));
      return new ResponseEntity<>(headers, HttpStatus.CREATED);
    }

    @RequestMapping(value = "/spr13319", method = POST, consumes = "multipart/form-data")
    public ResponseEntity<Void> create(@RequestPart("file") MultipartFile multipartFile) {
      assertThat(multipartFile.getOriginalFilename()).isEqualTo("élève.txt");
      return ResponseEntity.ok().build();
    }
  }

  @SuppressWarnings("unused")
  private static class TestData {

    private String name;

    public TestData() {
      super();
    }

    public TestData(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

}
