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

package infra.http.service.invoker;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import infra.core.StringValueResolver;
import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.MergedAnnotations;
import infra.http.HttpMethod;
import infra.http.service.annotation.GetExchange;
import infra.http.service.annotation.HttpExchange;
import infra.util.MultiValueMap;
import infra.web.annotation.GET;
import infra.web.annotation.POST;
import infra.web.annotation.RequestMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/1/19 21:09
 */
class HttpRequestValuesInitializerTests {

  private final MergedAnnotations typeAnnotations = MergedAnnotations.from(Service.class);

  @Test
  void initParams() throws Exception {
    MergedAnnotations annotations = MergedAnnotations.from(Service.class.getDeclaredMethod("execute"));

    var params = initParams(typeAnnotations.get(RequestMapping.class),
            annotations.get(GET.class), null);

    assertThat(params).containsKeys("k", "k1", "k2");
    assertThat(params).containsValues(List.of("v"), List.of(""));
    assertThat(params).containsEntry("k", List.of("v"))
            .containsEntry("k2", List.of(""))
            .containsEntry("k1", List.of(""));

    params = initParams(typeAnnotations.get(HttpExchange.class), annotations.get(GET.class), null);

    assertThat(params).isNull();
    params = initParams(null, annotations.get(GET.class), null);

    assertThat(params).isNull();

    params = initParams(typeAnnotations.get(RequestMapping.class), annotations.get(GET.class), strVal -> "prefix_" + strVal);

    assertThat(params).containsKeys("k", "k1", "k2");
    assertThat(params).containsValues(List.of("prefix_v"), List.of(""));
    assertThat(params).containsEntry("k", List.of("prefix_v")).containsEntry("k1", List.of(""));

    params = initParams(typeAnnotations.get(RequestMapping.class), annotations.get(GET.class), strVal -> null);

    assertThat(params).containsKeys("k1", "k2");
    assertThat(params).containsEntry("k1", List.of("")).containsEntry("k2", List.of(""));

    params = initParams(typeAnnotations.get(RequestMapping.class), annotations.get(GetExchange.class), null);
    assertThat(params).containsKeys("k", "k1", "k2");

    //

    annotations = MergedAnnotations.from(Service.class.getDeclaredMethod("methodParams"));

    params = initParams(typeAnnotations.get(RequestMapping.class),
            annotations.get(GET.class), null);
    assertThat(params).containsKeys("k", "k1", "k2");

    params = initParams(typeAnnotations.get(HttpExchange.class),
            annotations.get(GET.class), null);
    assertThat(params).containsKeys("p", "p1", "p2", "p3");

    params = initParams(typeAnnotations.get(RequestMapping.class),
            annotations.get(GetExchange.class), null);
    assertThat(params).containsKeys("k", "k1", "k2");

  }

  @Test
  void initHttpMethod() throws Exception {
    Method execute = MethodService.class.getDeclaredMethod("execute");
    var initializer = HttpRequestValuesInitializer.create(execute, MethodService.class, null, HttpRequestValues::builder);
    assertThat(initializer).extracting("httpMethod").isEqualTo(HttpMethod.POST);
    assertThat(initializer).extracting("uri").isNull();
    assertThat(initializer).extracting("contentType").isNull();
    assertThat(initializer).extracting("acceptMediaTypes").isNull();
    assertThat(initializer).extracting("otherHeaders").isNull();
    assertThat(initializer).extracting("params").isNull();
    assertThat(initializer).extracting("requestValuesCreator").isNotNull();

    Method executePath = MethodService.class.getDeclaredMethod("executePath");
    initializer = HttpRequestValuesInitializer.create(executePath, MethodService.class, null, HttpRequestValues::builder);
    assertThat(initializer).extracting("httpMethod").isEqualTo(HttpMethod.POST);
    assertThat(initializer).extracting("uri").isNull();
    assertThat(initializer).extracting("contentType").isNull();
    assertThat(initializer).extracting("acceptMediaTypes").isNull();
    assertThat(initializer).extracting("otherHeaders").isNull();
    assertThat(initializer).extracting("params").isNull();
    assertThat(initializer).extracting("requestValuesCreator").isNotNull();

  }

  @Nullable
  MultiValueMap<String, String> initParams(@Nullable MergedAnnotation<?> typeAnnotation,
          MergedAnnotation<?> methodAnnotation, @Nullable StringValueResolver embeddedValueResolver) {
    return HttpRequestValuesInitializer.initKeyValues("params", typeAnnotation, methodAnnotation, embeddedValueResolver);
  }

  @Nullable
  MultiValueMap<String, String> initHeaders(@Nullable MergedAnnotation<?> typeAnnotation,
          MergedAnnotation<?> methodAnnotation, @Nullable StringValueResolver embeddedValueResolver) {
    return HttpRequestValuesInitializer.initKeyValues("headers", typeAnnotation, methodAnnotation, embeddedValueResolver);
  }

  @HttpExchange
  @RequestMapping(params = { "k=v", "k1", "k2=", "" })
  interface Service {

    @GET
    @GetExchange
    void execute();

    @GET(params = { "p=", "p1=v1", "p2=v2", "p3=v1,v2,v3" })
    void methodParams();

    @GET(headers = "h")
    void headers();

  }

  @HttpExchange
  interface MethodService {

    @POST
    void execute();

    @POST("")
    void executePath();

  }

}