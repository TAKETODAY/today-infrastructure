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

import org.assertj.core.api.AssertProvider;

import java.util.List;
import java.util.stream.Stream;

import cn.taketoday.aot.agent.RecordedInvocation;

/**
 * A wrapper for {@link RecordedInvocation} that is the starting point for
 * {@code RuntimeHints} AssertJ assertions.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see RuntimeHintsInvocationsAssert
 * @since 4.0
 */
public class RuntimeHintsInvocations implements AssertProvider<RuntimeHintsInvocationsAssert> {

  private final List<RecordedInvocation> invocations;

  RuntimeHintsInvocations(List<RecordedInvocation> invocations) {
    this.invocations = invocations;
  }

  @Override
  public RuntimeHintsInvocationsAssert assertThat() {
    return new RuntimeHintsInvocationsAssert(this);
  }

  Stream<RecordedInvocation> recordedInvocations() {
    return this.invocations.stream();
  }

}
