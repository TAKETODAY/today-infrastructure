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

package infra.app;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import infra.context.annotation.Configuration;
import infra.context.aot.AbstractAotProcessor.Settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/4 17:39
 */
class ApplicationAotProcessorTests {

  @BeforeEach
  void setup() {
    SampleApplication.argsHolder = null;
    SampleApplication.postRunInvoked = false;
  }

  @Test
  void processApplicationInvokesRunMethod(@TempDir Path directory) {
    String[] arguments = new String[] { "1", "2" };
    ApplicationAotProcessor processor = new ApplicationAotProcessor(SampleApplication.class,
            settings(directory), arguments);
    processor.process();
    assertThat(SampleApplication.argsHolder).isEqualTo(arguments);
    assertThat(SampleApplication.postRunInvoked).isFalse();
  }

  @Test
  void processApplicationWithMainMethodThatDoesNotRun(@TempDir Path directory) {
    ApplicationAotProcessor processor = new ApplicationAotProcessor(BrokenApplication.class,
            settings(directory), new String[0]);
    assertThatIllegalStateException().isThrownBy(processor::process)
            .withMessageContaining("Does it run a Application?");
    assertThat(directory).isEmptyDirectory();
  }

  @Test
  void invokeMainParsesArgumentsAndInvokesRunMethod(@TempDir Path directory) throws Exception {
    String[] mainArguments = new String[] { SampleApplication.class.getName(),
            directory.resolve("source").toString(), directory.resolve("resource").toString(),
            directory.resolve("class").toString(), "com.example", "example", "1", "2" };
    ApplicationAotProcessor.main(mainArguments);
    assertThat(SampleApplication.argsHolder).containsExactly("1", "2");
    assertThat(SampleApplication.postRunInvoked).isFalse();
  }

  @Test
  void invokeMainParsesArgumentsAndInvokesRunMethodWithoutGroupId(@TempDir Path directory) throws Exception {
    String[] mainArguments = new String[] { SampleApplication.class.getName(),
            directory.resolve("source").toString(), directory.resolve("resource").toString(),
            directory.resolve("class").toString(), "", "example", "1", "2" };
    ApplicationAotProcessor.main(mainArguments);
    assertThat(SampleApplication.argsHolder).containsExactly("1", "2");
    assertThat(SampleApplication.postRunInvoked).isFalse();
  }

  @Test
  void invokeMainWithMissingArguments() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> ApplicationAotProcessor.main(new String[] { "Test" }))
            .withMessageContaining("Usage:");
  }

  private Settings settings(Path directory) {
    return Settings.builder()
            .sourceOutput(directory.resolve("source"))
            .resourceOutput(directory.resolve("resource"))
            .classOutput(directory.resolve("class"))
            .groupId("com.example")
            .artifactId("example")
            .build();

  }

  @Configuration(proxyBeanMethods = false)
  public static class SampleApplication {

    @Nullable
    public static String[] argsHolder;

    public static boolean postRunInvoked;

    public static void main(String[] args) {
      argsHolder = args;
      Application.run(SampleApplication.class, args);
      postRunInvoked = true;
    }

  }

  public static class BrokenApplication {

    public static void main(String[] args) {
      // Does not run an application
    }

  }

}