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

package infra.context.condition;

import infra.context.annotation.Condition;
import infra.context.annotation.ConditionContext;
import infra.context.condition.ConditionalOnJava.Range;
import infra.core.JavaVersion;
import infra.core.Ordered;
import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.Order;
import infra.core.type.AnnotatedTypeMetadata;

/**
 * {@link Condition} that checks for a required version of Java.
 *
 * @author Oliver Gierke
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ConditionalOnJava
 * @since 4.0 2022/4/4 12:23
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
class OnJavaCondition extends InfraCondition {

  private static final JavaVersion JVM_VERSION = JavaVersion.current();

  @Override
  @SuppressWarnings("NullAway")
  public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
    MergedAnnotation<ConditionalOnJava> annotation = metadata.getAnnotation(ConditionalOnJava.class);
    Range range = annotation.getEnum("range", Range.class);
    JavaVersion version = annotation.getEnum("value", JavaVersion.class);
    return getMatchOutcome(range, JVM_VERSION, version);
  }

  protected ConditionOutcome getMatchOutcome(Range range, JavaVersion runningVersion, JavaVersion version) {
    boolean match = isWithin(runningVersion, range, version);
    String expected = String.format((range != Range.EQUAL_OR_NEWER) ? "(older than %s)" : "(%s or newer)", version);
    ConditionMessage message = ConditionMessage.forCondition(ConditionalOnJava.class, expected)
            .foundExactly(runningVersion);
    return new ConditionOutcome(match, message);
  }

  /**
   * Determines if the {@code runningVersion} is within the specified range of versions.
   *
   * @param runningVersion the current version.
   * @param range the range
   * @param version the bounds of the range
   * @return if this version is within the specified range
   */
  private boolean isWithin(JavaVersion runningVersion, Range range, JavaVersion version) {
    if (range == Range.EQUAL_OR_NEWER) {
      return runningVersion.isEqualOrNewerThan(version);
    }
    if (range == Range.OLDER_THAN) {
      return runningVersion.isOlderThan(version);
    }
    throw new IllegalStateException("Unknown range " + range);
  }

}
