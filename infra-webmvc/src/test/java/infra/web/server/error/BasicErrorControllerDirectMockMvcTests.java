/*
 * Copyright 2012-present the original author or authors.
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

package infra.web.server.error;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URL;

import infra.app.config.context.PropertyPlaceholderAutoConfiguration;
import infra.app.Application;
import infra.app.builder.ApplicationBuilder;
import infra.app.test.util.ApplicationContextTestUtils;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.Configuration;
import infra.context.annotation.EnableAspectJAutoProxy;
import infra.context.annotation.Import;
import infra.http.MediaType;
import infra.http.converter.config.HttpMessageConvertersAutoConfiguration;
import infra.test.context.junit.jupiter.InfraExtension;
import infra.test.util.ReflectionTestUtils;
import infra.test.web.mock.MockMvc;
import infra.test.web.mock.MvcResult;
import infra.test.web.mock.setup.MockMvcBuilders;
import infra.web.config.annotation.EnableWebMvc;
import infra.web.server.netty.RandomPortWebServerConfig;
import infra.web.config.ErrorMvcAutoConfiguration;
import infra.web.config.WebMvcAutoConfiguration;

import static infra.test.web.mock.request.MockMvcRequestBuilders.get;
import static infra.test.web.mock.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link infra.web.server.error.BasicErrorController} using {@link MockMvc} but not
 * {@link InfraExtension}.
 *
 * @author Dave Syer
 * @author Sebastien Deleuze
 */
class BasicErrorControllerDirectMockMvcTests {

  private ConfigurableApplicationContext wac;

  private MockMvc mockMvc;

  @AfterEach
  void close() {
    ReflectionTestUtils.setField(URL.class, "factory", null);
    ApplicationContextTestUtils.closeAll(this.wac);
  }

  void setup(ConfigurableApplicationContext context) {
    this.wac = context;
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
  }

  @Test
  void errorPageAvailableWithParentContext() throws Exception {
    setup(new ApplicationBuilder(ParentConfiguration.class)
            .child(ChildConfiguration.class).run());
    MvcResult response = this.mockMvc.perform(get("/error").accept(MediaType.TEXT_HTML))
            .andExpect(status().is5xxServerError())
            .andReturn();
    String content = response.getResponse().getContentAsString();
    assertThat(content).contains("status=999");
  }

  @Test
  void errorPageAvailableWithMvcIncluded() throws Exception {
    setup(new Application(WebMvcIncludedConfiguration.class)
            .run("--server.port=0"));
    MvcResult response = this.mockMvc.perform(get("/error").accept(MediaType.TEXT_HTML))
            .andExpect(status().is5xxServerError())
            .andReturn();
    String content = response.getResponse().getContentAsString();
    assertThat(content).contains("status=999");
  }

  @Test
  void errorPageNotAvailableWithWhitelabelDisabled() throws Exception {
    setup(new Application(WebMvcIncludedConfiguration.class)
            .run("--server.port=0", "--web.error.whitelabel.enabled=false"));

    this.mockMvc.perform(get("/error").accept(MediaType.TEXT_HTML))
            .andExpect(status().is(500));
  }

  @Test
  void errorControllerWithAop() throws Exception {
    setup(new Application(WithAopConfiguration.class)
            .run("--server.port=0"));
    MvcResult response = this.mockMvc.perform(get("/error").accept(MediaType.TEXT_HTML))
            .andExpect(status().is5xxServerError())
            .andReturn();
    String content = response.getResponse().getContentAsString();
    assertThat(content).contains("status=999");
  }

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  @Import({
          RandomPortWebServerConfig.class,
          WebMvcAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
          ErrorMvcAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
  protected @interface MinimalWebConfiguration {

  }

  @Configuration(proxyBeanMethods = false)
  @MinimalWebConfiguration
  static class ParentConfiguration {

  }

  @Configuration(proxyBeanMethods = false)
  @MinimalWebConfiguration
  @EnableWebMvc
  static class WebMvcIncludedConfiguration {

    // For manual testing
    public static void main(String[] args) {
      Application.run(WebMvcIncludedConfiguration.class, args);
    }

  }

  @Configuration(proxyBeanMethods = false)
  @MinimalWebConfiguration
  static class VanillaConfiguration {

    // For manual testing
    static void main(String[] args) {
      Application.run(VanillaConfiguration.class, args);
    }

  }

  @Configuration(proxyBeanMethods = false)
  @MinimalWebConfiguration
  static class ChildConfiguration {

    // For manual testing
    static void main(String[] args) {
      new ApplicationBuilder(ParentConfiguration.class).child(ChildConfiguration.class).run(args);
    }

  }

  @Configuration(proxyBeanMethods = false)
  @EnableAspectJAutoProxy(proxyTargetClass = false)
  @MinimalWebConfiguration
  @Aspect
  static class WithAopConfiguration {

    @Pointcut("within(@infra.stereotype.Controller *)")
    private void controllerPointCut() {
    }

    @Around("controllerPointCut()")
    Object mvcAdvice(ProceedingJoinPoint pjp) throws Throwable {
      return pjp.proceed();
    }

  }

}
