/*
 * Copyright 2012-present the original author or authors.
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

package infra.app.diagnostics.analyzer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URL;
import java.util.List;

import infra.app.diagnostics.FailureAnalysis;
import infra.app.diagnostics.analyzer.NoSuchMethodFailureAnalyzer.ClassDescriptor;
import infra.app.diagnostics.analyzer.NoSuchMethodFailureAnalyzer.NoSuchMethodDescriptor;

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
            "'boolean infra.util.MimeType.isMoreSpecific(infra.util.MimeType)'");
    assertThat(descriptor).isNotNull();
    assertThat(descriptor.errorMessage).isEqualTo(
            "'boolean infra.util.MimeType.isMoreSpecific(infra.util.MimeType)'");
    assertThat(descriptor.className).isEqualTo("infra.util.MimeType");
    assertThat(descriptor.candidateLocations.size()).isEqualTo(1);
    List<ClassDescriptor> typeHierarchy = descriptor.typeHierarchy;
    assertThat(typeHierarchy).hasSize(1);
    URL location = typeHierarchy.get(0).location;
    assertThat(location).asString().contains("infra-core");
  }

  @Test
  void reportsTheCallerAndCalledClassForALinkageError() {
    String caller = getClass().getName();
    String called = Child.class.getName();
    String missingMethod = called.replace('.', '/') + ".missing()V";
    NoSuchMethodError failure = linkageError(caller, missingMethod + " (loaded from somewhere)");

    FailureAnalysis analysis = new NoSuchMethodFailureAnalyzer().analyze(new IllegalStateException(failure));

    assertThat(analysis).isNotNull();
    assertThat(analysis.getCause()).isSameAs(failure);
    assertThat(analysis.getDescription()).contains(caller + ".invoke(", missingMethod,
            "calling method's class, " + caller, "called method's class, " + called,
            Child.class.getCanonicalName() + ":", Parent.class.getCanonicalName() + ":")
            .doesNotContain("loaded from somewhere");
    assertThat(analysis.getAction()).endsWith("compatible versions of the classes " + caller + " and " + called);
  }

  @Test
  void recommendsASingleVersionWhenTheCallerAndCalledClassAreTheSame() {
    String type = Child.class.getName();
    FailureAnalysis analysis = new NoSuchMethodFailureAnalyzer().analyze(linkageError(type, "'void " + type + ".missing()'"));

    assertThat(analysis).isNotNull();
    assertThat(analysis.getAction()).endsWith("a single, compatible version of " + type);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = { "'invalid'", "missing()", "'void missing.Type.method()'", "'void java.lang.String.method()'" })
  void ignoresFailuresWithoutADescriptorForTheCalledClass(String message) {
    assertThat(new NoSuchMethodFailureAnalyzer().analyze(linkageError(getClass().getName(), message))).isNull();
  }

  @Test
  void ignoresFailuresWithoutADescriptorForTheCaller() {
    assertThat(new NoSuchMethodFailureAnalyzer().analyze(linkageError("missing.Caller", "'void infra.util.MimeType.missing()'")))
            .isNull();
  }

  private NoSuchMethodError linkageError(String caller, String message) {
    NoSuchMethodError error = new NoSuchMethodError(message);
    error.setStackTrace(new StackTraceElement[] { new StackTraceElement(caller, "invoke", "Caller.java", 42) });
    return error;
  }

  static class Parent { }

  static class Child extends Parent { }

}
