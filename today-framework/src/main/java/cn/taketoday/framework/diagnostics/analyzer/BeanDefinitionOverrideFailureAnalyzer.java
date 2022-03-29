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

package cn.taketoday.framework.diagnostics.analyzer;

import java.io.PrintWriter;
import java.io.StringWriter;

import cn.taketoday.beans.factory.support.BeanDefinitionOverrideException;
import cn.taketoday.framework.diagnostics.AbstractFailureAnalyzer;
import cn.taketoday.framework.diagnostics.FailureAnalysis;

/**
 * An {@link AbstractFailureAnalyzer} that performs analysis of failures caused by a
 * {@link BeanDefinitionOverrideException}.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class BeanDefinitionOverrideFailureAnalyzer extends AbstractFailureAnalyzer<BeanDefinitionOverrideException> {

  private static final String ACTION = "Consider renaming one of the beans or enabling "
          + "overriding by setting context.main.allow-bean-definition-overriding=true";

  @Override
  protected FailureAnalysis analyze(Throwable rootFailure, BeanDefinitionOverrideException cause) {
    return new FailureAnalysis(getDescription(cause), ACTION, cause);
  }

  private String getDescription(BeanDefinitionOverrideException ex) {
    StringWriter description = new StringWriter();
    PrintWriter printer = new PrintWriter(description);
    printer.printf("The bean '%s'", ex.getBeanName());
    if (ex.getBeanDefinition().getResourceDescription() != null) {
      printer.printf(", defined in %s,", ex.getBeanDefinition().getResourceDescription());
    }
    printer.printf(" could not be registered. A bean with that name has already been defined ");
    if (ex.getExistingDefinition().getResourceDescription() != null) {
      printer.printf("in %s ", ex.getExistingDefinition().getResourceDescription());
    }
    printer.printf("and overriding is disabled.");
    return description.toString();
  }

}
