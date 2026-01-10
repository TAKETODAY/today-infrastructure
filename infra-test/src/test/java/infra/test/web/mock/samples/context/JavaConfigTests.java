/*
 * Copyright 2002-present the original author or authors.
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

package infra.test.web.mock.samples.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.http.MediaType;
import infra.mock.api.MockContext;
import infra.test.context.ContextConfiguration;
import infra.test.context.ContextHierarchy;
import infra.test.context.aot.DisabledInAotMode;
import infra.test.context.junit.jupiter.InfraExtension;
import infra.test.context.web.WebAppConfiguration;
import infra.test.web.Person;
import infra.test.web.mock.MockMvc;
import infra.test.web.mock.setup.MockMvcBuilders;
import infra.web.config.annotation.EnableWebMvc;
import infra.web.config.annotation.ResourceHandlerRegistry;
import infra.web.config.annotation.ViewControllerRegistry;
import infra.web.config.annotation.WebMvcConfigurer;
import infra.web.mock.WebApplicationContext;

import static infra.test.web.mock.request.MockMvcRequestBuilders.get;
import static infra.test.web.mock.result.MockMvcResultHandlers.print;
import static infra.test.web.mock.result.MockMvcResultMatchers.content;
import static infra.test.web.mock.result.MockMvcResultMatchers.jsonPath;
import static infra.test.web.mock.result.MockMvcResultMatchers.request;
import static infra.test.web.mock.result.MockMvcResultMatchers.status;
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
@DisabledInAotMode // @ContextHierarchy is not supported in AOT.
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
                    content().string("{\"name\":\"Joe\",\"someBoolean\":false,\"someDouble\":0.0}"),
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
   * <p>This code has been copied from
   * {@link infra.test.context.hierarchies.web.ControllerIntegrationTests}.
   *
   * @see infra.test.context.hierarchies.web.ControllerIntegrationTests#verifyRootWacSupport()
   */
  private void verifyRootWacSupport() {
    assertThat(personDao).isNotNull();
    assertThat(personController).isNotNull();

    ApplicationContext parent = wac.getParent();
    assertThat(parent).isNotNull();
    assertThat(parent).isInstanceOf(WebApplicationContext.class);
    WebApplicationContext root = (WebApplicationContext) parent;

    MockContext childMockContext = wac.getMockContext();
    assertThat(childMockContext).isNotNull();
    MockContext rootMockContext = root.getMockContext();
    assertThat(rootMockContext).isNotNull();
    assertThat(rootMockContext).isSameAs(childMockContext);

    assertThat(rootMockContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)).isSameAs(root);
    assertThat(childMockContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)).isSameAs(root);
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

  }

}
