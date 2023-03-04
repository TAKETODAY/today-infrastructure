/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.annotation.config.freemarker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.StringWriter;

import cn.taketoday.context.annotation.config.AutoConfigurations;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;
import cn.taketoday.framework.test.system.CapturedOutput;
import cn.taketoday.framework.test.system.OutputCaptureExtension;
import cn.taketoday.test.BuildOutput;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FreeMarkerAutoConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Kazuki Shimizu
 */
@ExtendWith(OutputCaptureExtension.class)
class FreeMarkerAutoConfigurationTests {

  private final BuildOutput buildOutput = new BuildOutput(getClass());

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(FreeMarkerAutoConfiguration.class));

  @Test
  void renderNonWebAppTemplate() {
    this.contextRunner.run((context) -> {
      var freemarker = context.getBean(freemarker.template.Configuration.class);
      StringWriter writer = new StringWriter();
      freemarker.getTemplate("message.ftl").process(new DataModel(), writer);
      assertThat(writer.toString()).contains("Hello World");
    });
  }

  @Test
  void nonExistentTemplateLocation(CapturedOutput output) {
    this.contextRunner
            .withPropertyValues("infra.freemarker.templateLoaderPath:"
                    + "classpath:/does-not-exist/,classpath:/also-does-not-exist")
            .run((context) -> assertThat(output).contains("Cannot find template location"));
  }

  @Test
  void emptyTemplateLocation(CapturedOutput output) {
    File emptyDirectory = new File(this.buildOutput.getTestResourcesLocation(), "empty-templates/empty-directory");
    emptyDirectory.mkdirs();
    this.contextRunner
            .withPropertyValues("infra.freemarker.templateLoaderPath:classpath:/empty-templates/empty-directory/")
            .run((context) -> assertThat(output).doesNotContain("Cannot find template location"));
  }

  @Test
  void nonExistentLocationAndEmptyLocation(CapturedOutput output) {
    new File(this.buildOutput.getTestResourcesLocation(), "empty-templates/empty-directory").mkdirs();
    this.contextRunner
            .withPropertyValues("infra.freemarker.templateLoaderPath:"
                    + "classpath:/does-not-exist/,classpath:/empty-templates/empty-directory/")
            .run((context) -> assertThat(output).doesNotContain("Cannot find template location"));
  }

  public static class DataModel {

    public String getGreeting() {
      return "Hello World";
    }

  }

}
