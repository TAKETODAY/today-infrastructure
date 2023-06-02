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

package cn.taketoday.framework.diagnostics.analyzer;

import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.List;

import cn.taketoday.framework.diagnostics.analyzer.NoSuchMethodFailureAnalyzer.ClassDescriptor;
import cn.taketoday.framework.diagnostics.analyzer.NoSuchMethodFailureAnalyzer.NoSuchMethodDescriptor;
import cn.taketoday.util.MimeType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NoSuchMethodFailureAnalyzer}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Scott Frederick
 */
class NoSuchMethodFailureAnalyzerTests {

  @Test
  void parseHotspotErrorMessage() {
    NoSuchMethodDescriptor descriptor = new NoSuchMethodFailureAnalyzer().getNoSuchMethodDescriptor(
            "'boolean cn.taketoday.util.MimeType.isMoreSpecific(cn.taketoday.util.MimeType)'");
    assertThat(descriptor).isNotNull();
    assertThat(descriptor.errorMessage).isEqualTo(
            "'boolean cn.taketoday.util.MimeType.isMoreSpecific(cn.taketoday.util.MimeType)'");
    assertThat(descriptor.className).isEqualTo("cn.taketoday.util.MimeType");
    assertThat(descriptor.candidateLocations.size()).isEqualTo(1);
    List<ClassDescriptor> typeHierarchy = descriptor.typeHierarchy;
    assertThat(typeHierarchy).hasSize(1);
    URL location = typeHierarchy.get(0).location;
//    assertThat(location).asString().contains("today-infrastructure/today-core/target/classes");
    assertThat(location).asString().contains("today-infrastructure/today-core/target/");
  }

//  @Test
//  void whenAMethodOnAClassIsMissingThenNoSuchMethodErrorIsAnalyzed() {
//    Throwable failure = createFailureForMissingMethod();
//    assertThat(failure).isNotNull();
////    failure.printStackTrace();
//    FailureAnalysis analysis = new NoSuchMethodFailureAnalyzer().analyze(failure);
//    assertThat(analysis).isNotNull();
//    assertThat(analysis.getDescription())
//            .contains(NoSuchMethodFailureAnalyzerTests.class.getName() + ".createFailureForMissingMethod(")
//            .contains("isMoreSpecific(")
//            .contains("calling method's class, " + NoSuchMethodFailureAnalyzerTests.class.getName() + ",")
//            .contains("called method's class, cn.taketoday.util.MimeType,");
//    assertThat(analysis.getAction()).contains(NoSuchMethodFailureAnalyzerTests.class.getName())
//            .contains("cn.taketoday.util.MimeType");
//  }

//
//  @Test
//  void whenAnInheritedMethodIsMissingThenNoSuchMethodErrorIsAnalyzed() {
//    Throwable failure = createFailureForMissingInheritedMethod();
//    assertThat(failure).isNotNull();
//    FailureAnalysis analysis = new NoSuchMethodFailureAnalyzer().analyze(failure);
//    assertThat(analysis).isNotNull();
//    assertThat(analysis.getDescription()).contains(R2dbcMappingContext.class.getName() + ".<init>(")
//            .contains(R2dbcMappingContext.class.getName() + ".setForceQuote(")
//            .contains("calling method's class, cn.taketoday.data.r2dbc.mapping.R2dbcMappingContext,")
//            .contains("called method's class, cn.taketoday.data.r2dbc.mapping.R2dbcMappingContext,")
//            .contains("    cn.taketoday.data.r2dbc.mapping.R2dbcMappingContext")
//            .contains("    cn.taketoday.data.relational.core.mapping.RelationalMappingContext")
//            .contains("    cn.taketoday.data.mapping.context.AbstractMappingContext");
//    assertThat(analysis.getAction()).contains("cn.taketoday.data.r2dbc.mapping.R2dbcMappingContext");
//  }

  private Throwable createFailureForMissingMethod() {
    try {
      System.out.println(MimeType.class.getProtectionDomain().getCodeSource().getLocation());
      MimeType mimeType = new MimeType("application", "json");
      System.out.println(mimeType.isMoreSpecific(null));
      return null;
    }
    catch (Throwable ex) {
      return ex;
    }
  }

//  private Throwable createFailureForMissingInheritedMethod() {
//    try {
//      new R2dbcMappingContext();
//      return null;
//    }
//    catch (Throwable ex) {
//      return ex;
//    }
//  }

}
