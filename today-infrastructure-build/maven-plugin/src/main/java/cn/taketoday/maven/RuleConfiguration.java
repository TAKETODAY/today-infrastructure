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
package cn.taketoday.maven;

import org.codehaus.plexus.util.StringUtils;
import org.jacoco.core.analysis.ICoverageNode.ElementType;
import org.jacoco.report.check.Limit;
import org.jacoco.report.check.Rule;

import java.util.List;

/**
 * Wrapper for {@link Rule} objects to allow Maven style includes/excludes lists
 */
public class RuleConfiguration {

  final Rule rule;

  /**
   * Create a new configuration instance.
   */
  public RuleConfiguration() {
    rule = new Rule();
  }

  /**
   * @param element element type this rule applies to TODO: use ElementType
   * directly once Maven 3 is required.
   */
  public void setElement(final String element) {
    rule.setElement(ElementType.valueOf(element));
  }

  /**
   * @param includes includes patterns
   */
  public void setIncludes(final List<String> includes) {
    rule.setIncludes(StringUtils.join(includes.iterator(), ":"));
  }

  /**
   * @param excludes excludes patterns
   */
  public void setExcludes(final List<String> excludes) {
    rule.setExcludes(StringUtils.join(excludes.iterator(), ":"));
  }

  /**
   * @param limits list of {@link Limit}s configured for this rule
   */
  public void setLimits(final List<Limit> limits) {
    rule.setLimits(limits);
  }

}
