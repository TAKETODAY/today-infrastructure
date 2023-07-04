/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.framework;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.aot.AbstractAotProcessor.Settings;
import cn.taketoday.lang.Nullable;

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