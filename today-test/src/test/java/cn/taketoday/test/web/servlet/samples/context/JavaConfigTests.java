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

package cn.taketoday.test.web.servlet.samples.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.http.MediaType;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.ContextHierarchy;
import cn.taketoday.test.context.junit.jupiter.InfraExtension;
import cn.taketoday.test.context.web.WebAppConfiguration;
import cn.taketoday.test.web.Person;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.test.web.servlet.setup.MockMvcBuilders;
import cn.taketoday.web.config.DefaultServletHandlerConfigurer;
import cn.taketoday.web.config.EnableWebMvc;
import cn.taketoday.web.config.ResourceHandlerRegistry;
import cn.taketoday.web.config.ViewControllerRegistry;
import cn.taketoday.web.config.WebMvcConfigurer;
import cn.taketoday.web.servlet.WebApplicationContext;
import jakarta.servlet.ServletContext;

import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.get;
import static cn.taketoday.test.web.servlet.result.MockMvcResultHandlers.print;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.content;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.request;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests with Java configuration.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author Sebastien Deleuze
 * @author Michał Rowicki
 */
@ExtendWith(InfraExtension.class)
@WebAppConfiguration("classpath:META-INF/web-resources")
@ContextHierarchy({
        @ContextConfiguration(classes = JavaConfigTests.RootConfig.class),
        @ContextConfiguration(classes = JavaConfigTests.WebConfig.class)
})
public class JavaConfigTests {

  @Autowired
  private WebApplicationContext wac;

  @Autowired
  private PersonDao personDao;

  @Autowired
  private PersonController personController;

  private MockMvc mockMvc;

  @BeforeEach
  public void setup() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    verifyRootWacSupport();
    given(this.personDao.getPerson(5L)).willReturn(new Person("Joe"));
  }

  @Test
  public void person() throws Exception {
    this.mockMvc.perform(get("/person/5").accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpectAll(
                    status().isOk(),
                    request().asyncNotStarted(),
                    content().string("{\"name\":\"Joe\",\"someDouble\":0.0,\"someBoolean\":false}"),
                    jsonPath("$.name").value("Joe")
            );
  }

  @Test
  public void andExpectAllWithOneFailure() {
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> this.mockMvc.perform(get("/person/5").accept(MediaType.APPLICATION_JSON))
                    .andExpectAll(
                            status().isBadGateway(),
                            request().asyncNotStarted(),
                            jsonPath("$.name").value("Joe")))
            .withMessage("Status expected:<502> but was:<200>")
            .satisfies(error -> assertThat(error).hasNoSuppressedExceptions());
  }

  @Test
  public void andExpectAllWithMultipleFailures() {
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
                    this.mockMvc.perform(get("/person/5").accept(MediaType.APPLICATION_JSON))
                            .andExpectAll(
                                    status().isBadGateway(),
                                    request().asyncNotStarted(),
                                    jsonPath("$.name").value("Joe"),
                                    jsonPath("$.name").value("Jane")
                            ))
            .withMessage("Multiple Exceptions (2):\nStatus expected:<502> but was:<200>\nJSON path \"$.name\" expected:<Jane> but was:<Joe>")
            .satisfies(error -> assertThat(error.getSuppressed()).hasSize(2));
  }

  /**
   * Verify that the breaking change introduced in <a
   * href="https://jira.spring.io/browse/SPR-12553">SPR-12553</a> has been reverted.
   *
   * <p>This code has been copied from
   * {@link cn.taketoday.test.context.hierarchies.web.ControllerIntegrationTests}.
   *
   * @see cn.taketoday.test.context.hierarchies.web.ControllerIntegrationTests#verifyRootWacSupport()
   */
  private void verifyRootWacSupport() {
    assertThat(personDao).isNotNull();
    assertThat(personController).isNotNull();

    ApplicationContext parent = wac.getParent();
    assertThat(parent).isNotNull();
    assertThat(parent).isInstanceOf(WebApplicationContext.class);
    WebApplicationContext root = (WebApplicationContext) parent;

    ServletContext childServletContext = wac.getServletContext();
    assertThat(childServletContext).isNotNull();
    ServletContext rootServletContext = root.getServletContext();
    assertThat(rootServletContext).isNotNull();
    assertThat(rootServletContext).isSameAs(childServletContext);

    assertThat(rootServletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)).isSameAs(root);
    assertThat(childServletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)).isSameAs(root);
  }

  @Configuration
  static class RootConfig {

    @Bean
    public PersonDao personDao() {
      return mock();
    }
  }

  @Configuration
  @EnableWebMvc
  static class WebConfig implements WebMvcConfigurer {

    @Autowired
    private RootConfig rootConfig;

    @Bean
    public PersonController personController() {
      return new PersonController(this.rootConfig.personDao());
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
      registry.addResourceHandler("/resources/**").addResourceLocations("/resources/");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
      registry.addViewController("/").setViewName("home");
    }

    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
      configurer.enable();
    }
  }

}
