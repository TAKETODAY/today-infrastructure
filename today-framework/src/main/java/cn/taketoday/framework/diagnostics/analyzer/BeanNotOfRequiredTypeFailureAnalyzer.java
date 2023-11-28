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
import java.lang.reflect.Proxy;

import cn.taketoday.beans.factory.BeanNotOfRequiredTypeException;
import cn.taketoday.framework.diagnostics.AbstractFailureAnalyzer;
import cn.taketoday.framework.diagnostics.FailureAnalysis;

/**
 * An {@link AbstractFailureAnalyzer} that performs analysis of failures caused by a
 * {@link BeanNotOfRequiredTypeException}.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class BeanNotOfRequiredTypeFailureAnalyzer extends AbstractFailureAnalyzer<BeanNotOfRequiredTypeException> {

  private static final String ACTION = "Consider injecting the bean as one of its "
          + "interfaces or forcing the use of CGLib-based "
          + "proxies by setting proxyTargetClass=true on @EnableAsync and/or @EnableCaching.";

  @Override
  protected FailureAnalysis analyze(Throwable rootFailure, BeanNotOfRequiredTypeException cause) {
    if (!Proxy.isProxyClass(cause.getActualType())) {
      return null;
    }
    return new FailureAnalysis(getDescription(cause), ACTION, cause);
  }

  private String getDescription(BeanNotOfRequiredTypeException ex) {
    StringWriter description = new StringWriter();
    PrintWriter printer = new PrintWriter(description);
    printer.printf("The bean '%s' could not be injected because it is a JDK dynamic proxy%n%n", ex.getBeanName());
    printer.printf("The bean is of type '%s' and implements:%n", ex.getActualType().getName());
    for (Class<?> actualTypeInterface : ex.getActualType().getInterfaces()) {
      printer.println("\t" + actualTypeInterface.getName());
    }
    printer.printf("%nExpected a bean of type '%s' which implements:%n", ex.getRequiredType().getName());
    for (Class<?> requiredTypeInterface : ex.getRequiredType().getInterfaces()) {
      printer.println("\t" + requiredTypeInterface.getName());
    }
    return description.toString();
  }

}
