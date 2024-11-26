/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.context.condition;

import java.util.Map;

import infra.context.annotation.Condition;
import infra.context.annotation.ConditionContext;
import infra.core.JavaVersion;
import infra.core.Ordered;
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

  private static final JavaVersion JVM_VERSION = JavaVersion.getJavaVersion();

  @Override
  public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
    Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnJava.class.getName());
    ConditionalOnJava.Range range = (ConditionalOnJava.Range) attributes.get("range");
    JavaVersion version = (JavaVersion) attributes.get("value");
    return getMatchOutcome(range, JVM_VERSION, version);
  }

  protected ConditionOutcome getMatchOutcome(ConditionalOnJava.Range range, JavaVersion runningVersion, JavaVersion version) {
    boolean match = isWithin(runningVersion, range, version);
    String expected = String.format((range != ConditionalOnJava.Range.EQUAL_OR_NEWER) ? "(older than %s)" : "(%s or newer)", version);
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
  private boolean isWithin(JavaVersion runningVersion, ConditionalOnJava.Range range, JavaVersion version) {
    if (range == ConditionalOnJava.Range.EQUAL_OR_NEWER) {
      return runningVersion.isEqualOrNewerThan(version);
    }
    if (range == ConditionalOnJava.Range.OLDER_THAN) {
      return runningVersion.isOlderThan(version);
    }
    throw new IllegalStateException("Unknown range " + range);
  }

}
