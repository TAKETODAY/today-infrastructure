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

package infra.web.server;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.annotation.config.context.PropertyPlaceholderAutoConfiguration;
import infra.annotation.config.http.HttpMessageConvertersAutoConfiguration;
import infra.annotation.config.task.TaskExecutionAutoConfiguration;
import infra.annotation.config.web.ErrorMvcAutoConfiguration;
import infra.annotation.config.web.WebMvcAutoConfiguration;
import infra.app.Application;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.ImportAutoConfiguration;
import infra.core.io.ClassPathResource;
import infra.http.HttpEntity;
import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.http.RequestEntity;
import infra.http.ResponseEntity;
import infra.lang.Assert;
import infra.stereotype.Component;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import infra.web.annotation.POST;
import infra.web.annotation.RequestPart;
import infra.web.annotation.RestController;
import infra.web.client.RestTemplate;
import infra.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/21 21:19
 */
class MultipartIntegrationTests {

  private @Nullable ConfigurableApplicationContext context;

  @AfterEach
  void closeContext() {
    if (context != null) {
      Application.exit(context);
    }
  }

  @Test
  void upload() {
    load();
    HttpHeaders fooHeaders = HttpHeaders.forWritable();
    fooHeaders.setContentType(MediaType.TEXT_PLAIN);

    MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();

    parts.add("form", new HttpEntity<>(new Form()));
    parts.add("file", new HttpEntity<>(new ClassPathResource("infra/web/function/foo.txt"), fooHeaders));

    ResponseEntity<String> response = new RestTemplate().exchange(
            RequestEntity.post(createURL("/form")).contentType(MediaType.MULTIPART_FORM_DATA).body(parts), String.class);

    assertThat(response.getBody()).isEqualTo("ok");
  }

  private void load(String... arguments) {
    this.context = Application.run(TestConfiguration.class, arguments);
  }

  private String createURL(String path) {
    Assert.state(context != null, "Context is required");
    int port = context.getEnvironment().getRequiredProperty("local.server.port", int.class);
    return "http://localhost:" + port + path;
  }

  @RestController
  static class UploadHttpHandler {

    @POST
    String upload(MultipartFile file) {
      return "ok";
    }

    @POST("/form")
    String upload(@RequestPart("form") Form form, @RequestPart("file") MultipartFile file) {
      return "ok";
    }

  }

  static class Form {

    public String desc;

  }

  @MinimalWebConfiguration
  @Configuration(proxyBeanMethods = false)
  static class TestConfiguration {

    @Component
    public static UploadHttpHandler uploadHttpHandler() {
      return new UploadHttpHandler();
    }

  }

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  @ImportAutoConfiguration({
          RandomPortWebServerConfig.class, TaskExecutionAutoConfiguration.class,
          WebMvcAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
          ErrorMvcAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
  private @interface MinimalWebConfiguration {

  }

}
