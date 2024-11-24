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

package infra.context.annotation.configuration.spr9031;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.ComponentScan;
import infra.context.annotation.ComponentScan.Filter;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.context.annotation.configuration.spr9031.scanpackage.Spr9031Component;
import infra.context.annotation.AnnotationConfigApplicationContext;

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
