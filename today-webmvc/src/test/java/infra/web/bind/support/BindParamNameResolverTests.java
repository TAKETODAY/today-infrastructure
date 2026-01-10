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

package infra.web.bind.support;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import infra.core.MethodParameter;
import infra.web.bind.annotation.BindParam;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/9 17:03
 */
class BindParamNameResolverTests {

  @Test
  void resolveNameWithBindParamAndValue() throws Exception {
    Method method = TestController.class.getDeclaredMethod("handle", String.class);
    MethodParameter parameter = new MethodParameter(method, 0);
    BindParamNameResolver resolver = new BindParamNameResolver();

    String result = resolver.resolveName(parameter);

    assertThat(result).isEqualTo("customName");
  }

  @Test
  void resolveNameWithBindParamWithoutValue() throws Exception {
    Method method = TestController.class.getDeclaredMethod("handleWithoutValue", String.class);
    MethodParameter parameter = new MethodParameter(method, 0);
    BindParamNameResolver resolver = new BindParamNameResolver();

    String result = resolver.resolveName(parameter);

    assertThat(result).isNull();
  }

  @Test
  void resolveNameWithoutBindParam() throws Exception {
    Method method = TestController.class.getDeclaredMethod("handleWithoutAnnotation", String.class);
    MethodParameter parameter = new MethodParameter(method, 0);
    BindParamNameResolver resolver = new BindParamNameResolver();

    String result = resolver.resolveName(parameter);

    assertThat(result).isNull();
  }

  @Test
  void resolveNameWithEmptyBindParamValue() throws Exception {
    Method method = TestController.class.getDeclaredMethod("handleWithEmptyValue", String.class);
    MethodParameter parameter = new MethodParameter(method, 0);
    BindParamNameResolver resolver = new BindParamNameResolver();

    String result = resolver.resolveName(parameter);

    assertThat(result).isNull();
  }

  static class TestController {
    public void handle(@BindParam("customName") String param) { }

    public void handleWithoutValue(@BindParam String param) { }

    public void handleWithoutAnnotation(String param) { }

    public void handleWithEmptyValue(@BindParam("") String param) { }
  }

}