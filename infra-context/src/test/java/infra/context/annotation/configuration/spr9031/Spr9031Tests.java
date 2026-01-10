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

package infra.context.annotation.configuration.spr9031;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.ComponentScan;
import infra.context.annotation.ComponentScan.Filter;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.context.annotation.configuration.spr9031.scanpackage.Spr9031Component;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Unit tests cornering bug SPR-9031.
 *
 * @author Chris Beams
 * @since 4.0
 */
public class Spr9031Tests {

  /**
   * Use of @Import to register LowLevelConfig results in ASM-based annotation
   * processing.
   */
  @Test
  public void withAsmAnnotationProcessing() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(HighLevelConfig.class);
    ctx.refresh();
    assertThat(ctx.getBean(LowLevelConfig.class).scanned).isNotNull();
  }

  /**
   * Direct registration of LowLevelConfig results in reflection-based annotation
   * processing.
   */
  @Test
  public void withoutAsmAnnotationProcessing() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(LowLevelConfig.class);
    ctx.refresh();
    assertThat(ctx.getBean(LowLevelConfig.class).scanned).isNotNull();
  }

  @Configuration
  @Import(LowLevelConfig.class)
  static class HighLevelConfig { }

  @Configuration
  @ComponentScan(
          basePackages = "infra.context.annotation.configuration.spr9031.scanpackage",
          includeFilters = { @Filter(MarkerAnnotation.class) })
  static class LowLevelConfig {
    // fails to wire when LowLevelConfig is processed with ASM because nested @Filter
    // annotation is not parsed
    @Autowired
    Spr9031Component scanned;
  }

  @Retention(RetentionPolicy.RUNTIME)
  public @interface MarkerAnnotation { }
}
