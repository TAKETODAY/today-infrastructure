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

package cn.taketoday.context.aot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.javapoet.ClassName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ContextAotProcessor}.
 *
 * @author Stephane Nicoll
 */
class ContextAotProcessorTests {

  @Test
  void processGeneratesAssets(@TempDir Path directory) {
    GenericApplicationContext context = new AnnotationConfigApplicationContext();
    context.registerBean(SampleApplication.class);
    ContextAotProcessor processor = new DemoContextAotProcessor(SampleApplication.class, directory);
    ClassName className = processor.process();
    assertThat(className).isEqualTo(ClassName.get(SampleApplication.class.getPackageName(),
            "ContextAotProcessorTests_SampleApplication__ApplicationContextInitializer"));
    assertThat(directory).satisfies(hasGeneratedAssetsForSampleApplication());
    context.close();
  }

  @Test
  void processingDeletesExistingOutput(@TempDir Path directory) throws IOException {
    Path sourceOutput = directory.resolve("source");
    Path resourceOutput = directory.resolve("resource");
    Path classOutput = directory.resolve("class");
    Path existingSourceOutput = createExisting(sourceOutput);
    Path existingResourceOutput = createExisting(resourceOutput);
    Path existingClassOutput = createExisting(classOutput);
    ContextAotProcessor processor = new DemoContextAotProcessor(SampleApplication.class,
            sourceOutput, resourceOutput, classOutput);
    processor.process();
    assertThat(existingSourceOutput).doesNotExist();
    assertThat(existingResourceOutput).doesNotExist();
    assertThat(existingClassOutput).doesNotExist();
  }

  @Test
  void processWithEmptyNativeImageArgumentsDoesNotCreateNativeImageProperties(@TempDir Path directory) {
    GenericApplicationContext context = new AnnotationConfigApplicationContext();
    context.registerBean(SampleApplication.class);
    ContextAotProcessor processor = new DemoContextAotProcessor(SampleApplication.class, directory) {
      @Override
      protected List<String> getDefaultNativeImageArguments(String application) {
        return Collections.emptyList();
      }
    };
    processor.process();
    assertThat(directory.resolve("resource/META-INF/native-image/com.example/example/native-image.properties"))
            .doesNotExist();
    context.close();
  }

  private Path createExisting(Path directory) throws IOException {
    Path existing = directory.resolve("existing");
    Files.createDirectories(directory);
    Files.createFile(existing);
    return existing;
  }

  private Consumer<Path> hasGeneratedAssetsForSampleApplication() {
    return directory -> {
      assertThat(directory.resolve(
              "source/cn/taketoday/context/aot/ContextAotProcessorTests_SampleApplication__ApplicationContextInitializer.java"))
              .exists().isRegularFile();
      assertThat(directory.resolve("source/cn/taketoday/context/aot/ContextAotProcessorTests__BeanDefinitions.java"))
              .exists().isRegularFile();
      assertThat(directory.resolve(
              "source/cn/taketoday/context/aot/ContextAotProcessorTests_SampleApplication__BeanFactoryRegistrations.java"))
              .exists().isRegularFile();
      assertThat(directory.resolve("resource/META-INF/native-image/com.example/example/reflect-config.json"))
              .exists().isRegularFile();
      Path nativeImagePropertiesFile = directory
              .resolve("resource/META-INF/native-image/com.example/example/native-image.properties");
      assertThat(nativeImagePropertiesFile).exists().isRegularFile().hasContent("""
              Args = -H:Class=cn.taketoday.context.aot.ContextAotProcessorTests$SampleApplication \\
              --report-unsupported-elements-at-runtime \\
              --no-fallback \\
              --install-exit-handlers
              """);
    };
  }

  private static class DemoContextAotProcessor extends ContextAotProcessor {

    DemoContextAotProcessor(Class<?> application, Path rootPath) {
      this(application, rootPath.resolve("source"), rootPath.resolve("resource"), rootPath.resolve("class"));
    }

    DemoContextAotProcessor(Class<?> application, Path sourceOutput, Path resourceOutput, Path classOutput) {
      super(application, createSettings(sourceOutput, resourceOutput, classOutput, "com.example", "example"));
    }

    private static Settings createSettings(Path sourceOutput, Path resourceOutput,
            Path classOutput, String groupId, String artifactId) {
      return Settings.builder()
              .sourceOutput(sourceOutput)
              .resourceOutput(resourceOutput)
              .classOutput(classOutput)
              .artifactId(artifactId)
              .groupId(groupId)
              .build();
    }

    @Override
    protected GenericApplicationContext prepareApplicationContext(Class<?> application) {
      AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
      context.register(application);
      return context;
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class SampleApplication {

    @Bean
    public String testBean() {
      return "Hello";
    }

  }

}
