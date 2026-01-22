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

package infra.web.server;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.Path;

import infra.app.Application;
import infra.app.InfraApplication;
import infra.app.config.context.PropertyPlaceholderAutoConfiguration;
import infra.app.config.task.TaskExecutionAutoConfiguration;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.ImportAutoConfiguration;
import infra.core.io.ClassPathResource;
import infra.core.style.ToStringBuilder;
import infra.http.HttpEntity;
import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.http.ResponseEntity;
import infra.http.converter.HttpMessageConverters.ServerBuilder;
import infra.http.converter.config.HttpMessageConvertersAutoConfiguration;
import infra.lang.Assert;
import infra.stereotype.Component;
import infra.util.MultiValueMap;
import infra.web.annotation.POST;
import infra.web.annotation.RequestPart;
import infra.web.annotation.RestController;
import infra.web.client.RestClient;
import infra.web.config.annotation.WebMvcConfigurer;
import infra.web.multipart.Part;
import infra.web.server.netty.RandomPortWebServerConfig;
import infra.web.config.ErrorMvcAutoConfiguration;
import infra.web.config.WebMvcAutoConfiguration;

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
  void upload(@TempDir Path path) {
    load();
    HttpHeaders fooHeaders = HttpHeaders.forWritable();
    fooHeaders.setContentType(MediaType.TEXT_PLAIN);

    HttpHeaders headers = HttpHeaders.forWritable();
    headers.setContentType(MediaType.APPLICATION_JSON);

    var parts = MultiValueMap.forLinkedHashMap();
    parts.add("form", new HttpEntity<>(new Form("desc", path.resolve("temp.tmp").toAbsolutePath().toString()), headers));
    parts.add("file", new HttpEntity<>(new ClassPathResource("infra/web/function/foo.txt"), fooHeaders));

    ResponseEntity<String> response = RestClient.builder()
            .build()
            .post()
            .uri(createURL("/form"))
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .accept(MediaType.TEXT_PLAIN)
            .body(parts)
            .retrieve().toEntity(String.class);

    assertThat(response.getStatusCodeValue()).isEqualTo(200);
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
    String upload(Part file) {
      return "ok";
    }

    @POST("/form")
    String upload(@RequestPart("form") Form form, @RequestPart("file") Part file) throws IOException {
      assertThat(form.desc).isEqualTo("desc");
      assertThat(file.getContentAsString()).isEqualTo("Lorem Ipsum.");
      assertThat(file.transferTo(new File(form.path))).isEqualTo(file.getContentLength());
      return "ok";
    }

  }

  static class Form {

    public String desc;

    public String path;

    public Form() {
    }

    public Form(String desc, String path) {
      this.desc = desc;
      this.path = path;
    }

    @Override
    public String toString() {
      return ToStringBuilder.forInstance(this)
              .append("desc", desc)
              .toString();
    }
  }

  @InfraApplication
  @MinimalWebConfiguration
  @Configuration(proxyBeanMethods = false)
  static class TestConfiguration implements WebMvcConfigurer {

    @Component
    public static UploadHttpHandler uploadHttpHandler() {
      return new UploadHttpHandler();
    }

    @Override
    public void configureMessageConverters(ServerBuilder builder) {
//      builder.withStringConverter(new StringHttpMessageConverter());
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
