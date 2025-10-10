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