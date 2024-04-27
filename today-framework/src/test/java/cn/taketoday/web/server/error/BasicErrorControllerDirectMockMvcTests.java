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

package cn.taketoday.web.server.error;

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

import cn.taketoday.annotation.config.context.PropertyPlaceholderAutoConfiguration;
import cn.taketoday.annotation.config.http.HttpMessageConvertersAutoConfiguration;
import cn.taketoday.annotation.config.web.ErrorMvcAutoConfiguration;
import cn.taketoday.annotation.config.web.RandomPortWebServerConfig;
import cn.taketoday.annotation.config.web.WebMvcAutoConfiguration;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.EnableAspectJAutoProxy;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.framework.Application;
import cn.taketoday.framework.builder.ApplicationBuilder;
import cn.taketoday.framework.test.util.ApplicationContextTestUtils;
import cn.taketoday.http.MediaType;
import cn.taketoday.test.context.junit.jupiter.InfraExtension;
import cn.taketoday.test.util.ReflectionTestUtils;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.test.web.servlet.MvcResult;
import cn.taketoday.test.web.servlet.setup.MockMvcBuilders;
import cn.taketoday.web.config.EnableWebMvc;
import cn.taketoday.web.server.error.BasicErrorController;

import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.get;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BasicErrorController} using {@link MockMvc} but not
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
            .run("--server.port=0", "--server.error.whitelabel.enabled=false"));

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

    @Pointcut("within(@cn.taketoday.stereotype.Controller *)")
    private void controllerPointCut() {
    }

    @Around("controllerPointCut()")
    Object mvcAdvice(ProceedingJoinPoint pjp) throws Throwable {
      return pjp.proceed();
    }

  }

}
