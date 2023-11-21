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

package cn.taketoday.aot.test.agent;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.ListAssert;
import org.assertj.core.error.BasicErrorMessageFactory;
import org.assertj.core.error.ErrorMessageFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cn.taketoday.aot.agent.RecordedInvocation;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.RuntimeHintsRegistrar;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.TodayStrategies;

/**
 * AssertJ {@link org.assertj.core.api.Assert assertions} that can be applied to
 * {@link RuntimeHintsInvocations}.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class RuntimeHintsInvocationsAssert extends AbstractAssert<RuntimeHintsInvocationsAssert, RuntimeHintsInvocations> {

  private final List<Consumer<RuntimeHints>> configurers = new ArrayList<>();

  RuntimeHintsInvocationsAssert(RuntimeHintsInvocations invocations) {
    super(invocations, RuntimeHintsInvocationsAssert.class);
  }

  public RuntimeHintsInvocationsAssert withRegistrar(RuntimeHintsRegistrar registrar) {
    this.configurers.add(hints -> registrar.registerHints(hints, getClass().getClassLoader()));
    return this;
  }

  public RuntimeHintsInvocationsAssert withStrategiesRegistrars(String location) {
    List<RuntimeHintsRegistrar> registrars = TodayStrategies.forLocation(location).load(RuntimeHintsRegistrar.class);
    this.configurers.add(hints -> registrars.forEach(registrar -> registrar.registerHints(hints, getClass().getClassLoader())));
    return this;
  }

  private void configureRuntimeHints(RuntimeHints hints) {
    this.configurers.forEach(configurer -> configurer.accept(hints));
  }

  /**
   * Verifies that each recorded invocation match at least once hint in the provided {@link RuntimeHints}.
   * <p>
   * Example: <pre class="code">
   * RuntimeHints hints = new RuntimeHints();
   * hints.reflection().registerType(MyType.class);
   * assertThat(invocations).match(hints); </pre>
   *
   * @param runtimeHints the runtime hints configuration to test against
   * @throws AssertionError if any of the recorded invocations has no match in the provided hints
   */
  public void match(RuntimeHints runtimeHints) {
    Assert.notNull(runtimeHints, "RuntimeHints is required");
    configureRuntimeHints(runtimeHints);
    List<RecordedInvocation> noMatchInvocations =
            this.actual.recordedInvocations().filter(invocation -> !invocation.matches(runtimeHints)).toList();
    if (!noMatchInvocations.isEmpty()) {
      throwAssertionError(errorMessageForInvocation(noMatchInvocations.get(0)));
    }
  }

  public ListAssert<RecordedInvocation> notMatching(RuntimeHints runtimeHints) {
    Assert.notNull(runtimeHints, "RuntimeHints is required");
    configureRuntimeHints(runtimeHints);
    return ListAssert.assertThatStream(this.actual.recordedInvocations()
            .filter(invocation -> !invocation.matches(runtimeHints)));
  }

  private ErrorMessageFactory errorMessageForInvocation(RecordedInvocation invocation) {
    if (invocation.isStatic()) {
      return new BasicErrorMessageFactory("%nMissing <%s> for invocation <%s>%nwith arguments %s.%nStacktrace:%n<%s>",
              invocation.getHintType().hintClassName(), invocation.getMethodReference(),
              invocation.getArguments(), formatStackTrace(invocation.getStackFrames()));
    }
    else {
      Class<?> instanceType = (invocation.getInstance() instanceof Class<?> clazz) ? clazz : invocation.getInstance().getClass();
      return new BasicErrorMessageFactory("%nMissing <%s> for invocation <%s> on type <%s> %nwith arguments %s.%nStacktrace:%n<%s>",
              invocation.getHintType().hintClassName(), invocation.getMethodReference(),
              instanceType, invocation.getArguments(),
              formatStackTrace(invocation.getStackFrames()));
    }
  }

  private String formatStackTrace(Stream<StackWalker.StackFrame> stackTraceElements) {
    return stackTraceElements
            .map(f -> f.getClassName() + "#" + f.getMethodName()
                    + ", Line " + f.getLineNumber()).collect(Collectors.joining(System.lineSeparator()));
  }

  /**
   * Verifies that the count of recorded invocations match the expected one.
   * <p>
   * Example: <pre class="code">
   * assertThat(invocations).hasCount(42); </pre>
   *
   * @param count the expected invocations count
   * @return {@code this} assertion object.
   * @throws AssertionError if the number of recorded invocations doesn't match the expected one
   */
  public RuntimeHintsInvocationsAssert hasCount(long count) {
    isNotNull();
    long invocationsCount = this.actual.recordedInvocations().count();
    if (invocationsCount != count) {
      throwAssertionError(new BasicErrorMessageFactory("%nNumber of recorded invocations does not match, expected <%n> but got <%n>.",
              invocationsCount, count));
    }
    return this;
  }

}
