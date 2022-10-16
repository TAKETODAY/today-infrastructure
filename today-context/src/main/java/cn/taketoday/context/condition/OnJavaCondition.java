/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.context.condition;

import java.util.Map;

import cn.taketoday.context.annotation.Condition;
import cn.taketoday.context.annotation.ConditionContext;
import cn.taketoday.context.condition.ConditionalOnJava.Range;
import cn.taketoday.core.JavaVersion;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.Order;
import cn.taketoday.core.type.AnnotatedTypeMetadata;

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
class OnJavaCondition extends ContextCondition {

  private static final JavaVersion JVM_VERSION = JavaVersion.getJavaVersion();

  @Override
  public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
    Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnJava.class.getName());
    Range range = (Range) attributes.get("range");
    JavaVersion version = (JavaVersion) attributes.get("value");
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
