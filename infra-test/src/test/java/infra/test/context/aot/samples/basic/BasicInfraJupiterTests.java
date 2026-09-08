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

package infra.test.context.aot.samples.basic;

import org.junit.jupiter.api.Nested;

import infra.aot.AotDetector;
import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.annotation.Value;
import infra.context.ApplicationContext;
import infra.core.env.Environment;
import infra.test.context.ActiveProfiles;
import infra.test.context.BootstrapWith;
import infra.test.context.ContextLoader;
import infra.test.context.MergedContextConfiguration;
import infra.test.context.TestExecutionListeners;
import infra.test.context.TestPropertySource;
import infra.test.context.aot.AotTestAttributes;
import infra.test.context.aot.samples.common.MessageService;
import infra.test.context.aot.samples.management.ManagementConfiguration;
import infra.test.context.env.YamlTestProperties;
import infra.test.context.junit.jupiter.JUnitConfig;
import infra.test.context.support.AbstractTestExecutionListener;
import infra.test.context.support.AnnotationConfigContextLoader;
import infra.test.context.support.DefaultTestContextBootstrapper;
import infra.test.context.support.GenericXmlContextLoader;

import static infra.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 4.0
 */
@BootstrapWith(BasicInfraJupiterTests.CustomXmlBootstrapper.class)
// Override the default loader configured by the CustomXmlBootstrapper.
@JUnitConfig(classes = { BasicTestConfiguration.class, ManagementConfiguration.class }, loader = AnnotationConfigContextLoader.class)
@TestExecutionListeners(listeners = BasicInfraJupiterTests.DummyTestExecutionListener.class, mergeMode = MERGE_WITH_DEFAULTS)
@TestPropertySource(properties = "test.engine = jupiter")
// We cannot use `classpath*:` in AOT tests until gh-31088 is resolved.
// @YamlTestProperties("classpath*:**/aot/samples/basic/test?.yaml")
@YamlTestProperties({
        "classpath:infra/test/context/aot/samples/basic/test1.yaml",
        "classpath:infra/test/context/aot/samples/basic/test2.yaml"
})
public class BasicInfraJupiterTests {

  @org.junit.jupiter.api.Test
  void test(@Autowired ApplicationContext context, @Autowired MessageService messageService,
          @Value("${test.engine}") String testEngine) {
    assertThat(messageService.generateMessage()).isEqualTo("Hello, AOT!");
    assertThat(testEngine).isEqualTo("jupiter");
    assertEnvProperties(context);
  }

  @Nested
  @TestPropertySource(properties = "foo=bar")
  @ActiveProfiles(resolver = SpanishActiveProfilesResolver.class)
  public class NestedTests {

    @org.junit.jupiter.api.Test
    void test(@Autowired ApplicationContext context, @Autowired MessageService messageService,
            @Value("${test.engine}") String testEngine, @Value("${foo}") String foo) {
      assertThat(messageService.generateMessage()).isEqualTo("¡Hola, AOT!");
      assertThat(foo).isEqualTo("bar");
      assertThat(testEngine).isEqualTo("jupiter");
      assertEnvProperties(context);
    }

  }

  static void assertEnvProperties(ApplicationContext context) {
    Environment env = context.getEnvironment();
    assertThat(env.getProperty("test.engine")).as("@TestPropertySource").isEqualTo("jupiter");
    assertThat(env.getProperty("test1.prop")).as("@TestPropertySource").isEqualTo("yaml");
    assertThat(env.getProperty("test2.prop")).as("@TestPropertySource").isEqualTo("yaml");
  }

  public static class DummyTestExecutionListener extends AbstractTestExecutionListener {
  }

  public static class CustomXmlBootstrapper extends DefaultTestContextBootstrapper {

    @Override
    protected Class<? extends ContextLoader> getDefaultContextLoaderClass(Class<?> testClass) {
      return GenericXmlContextLoader.class;
    }

    @Override
    protected MergedContextConfiguration processMergedContextConfiguration(MergedContextConfiguration mergedConfig) {
      String stringKey = "@InfraConfiguration-" + mergedConfig.getTestClass().getName();
      String booleanKey1 = stringKey + "-active1";
      String booleanKey2 = stringKey + "-active2";
      AotTestAttributes aotAttributes = AotTestAttributes.getInstance();
      if (AotDetector.useGeneratedArtifacts()) {
        assertThat(aotAttributes.getString(stringKey))
                .as("AOT String attribute must already be present during AOT run-time execution")
                .isEqualTo("org.example.Main");
        assertThat(aotAttributes.getBoolean(booleanKey1))
                .as("AOT boolean attribute 1 must already be present during AOT run-time execution")
                .isTrue();
        assertThat(aotAttributes.getBoolean(booleanKey2))
                .as("AOT boolean attribute 2 must already be present during AOT run-time execution")
                .isTrue();
      }
      else {
        // Set AOT attributes during AOT build-time processing.
        aotAttributes.setAttribute(stringKey, "org.example.Main");
        aotAttributes.setAttribute(booleanKey1, "TrUe");
        aotAttributes.setAttribute(booleanKey2, true);
      }
      return mergedConfig;
    }

  }

}
