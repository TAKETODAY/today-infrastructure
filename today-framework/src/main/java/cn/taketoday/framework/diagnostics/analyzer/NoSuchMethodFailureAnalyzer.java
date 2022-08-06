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
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.taketoday.framework.diagnostics.AbstractFailureAnalyzer;
import cn.taketoday.framework.diagnostics.FailureAnalysis;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

/**
 * An {@link AbstractFailureAnalyzer} that analyzes {@link NoSuchMethodError
 * NoSuchMethodErrors}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class NoSuchMethodFailureAnalyzer extends AbstractFailureAnalyzer<NoSuchMethodError> {

  @Override
  protected FailureAnalysis analyze(Throwable rootFailure, NoSuchMethodError cause) {
    NoSuchMethodDescriptor callerDescriptor = getCallerMethodDescriptor(cause);
    if (callerDescriptor == null) {
      return null;
    }
    NoSuchMethodDescriptor calledDescriptor = getNoSuchMethodDescriptor(cause.getMessage());
    if (calledDescriptor == null) {
      return null;
    }
    String description = getDescription(callerDescriptor, calledDescriptor);
    String action = getAction(callerDescriptor, calledDescriptor);
    return new FailureAnalysis(description, action, cause);
  }

  @Nullable
  private NoSuchMethodDescriptor getCallerMethodDescriptor(NoSuchMethodError cause) {
    StackTraceElement firstStackTraceElement = cause.getStackTrace()[0];
    String message = firstStackTraceElement.toString();
    String className = firstStackTraceElement.getClassName();
    return getDescriptorForClass(message, className);
  }

  @Nullable
  protected NoSuchMethodDescriptor getNoSuchMethodDescriptor(String cause) {
    String message = cleanMessage(cause);
    String className = extractClassName(message);
    return getDescriptorForClass(message, className);
  }

  @Nullable
  private NoSuchMethodDescriptor getDescriptorForClass(String message, @Nullable String className) {
    if (className == null) {
      return null;
    }
    List<URL> candidates = findCandidates(className);
    if (candidates == null) {
      return null;
    }
    Class<?> type = load(className);
    if (type == null) {
      return null;
    }
    List<ClassDescriptor> typeHierarchy = getTypeHierarchy(type);
    if (typeHierarchy == null) {
      return null;
    }
    return new NoSuchMethodDescriptor(message, className, candidates, typeHierarchy);
  }

  private String cleanMessage(String message) {
    int loadedFromIndex = message.indexOf(" (loaded from");
    if (loadedFromIndex == -1) {
      return message;
    }
    return message.substring(0, loadedFromIndex);
  }

  @Nullable
  private String extractClassName(String message) {
    if (message.startsWith("'") && message.endsWith("'")) {
      int splitIndex = message.indexOf(' ');
      if (splitIndex == -1) {
        return null;
      }
      message = message.substring(splitIndex + 1);
    }
    int descriptorIndex = message.indexOf('(');
    if (descriptorIndex == -1) {
      return null;
    }
    String classAndMethodName = message.substring(0, descriptorIndex);
    int methodNameIndex = classAndMethodName.lastIndexOf('.');
    if (methodNameIndex == -1) {
      return null;
    }
    String className = classAndMethodName.substring(0, methodNameIndex);
    return className.replace('/', '.');
  }

  @Nullable
  private List<URL> findCandidates(String className) {
    try {
      return Collections.list(NoSuchMethodFailureAnalyzer.class.getClassLoader()
              .getResources(ClassUtils.convertClassNameToResourcePath(className) + ".class"));
    }
    catch (Throwable ex) {
      return null;
    }
  }

  @Nullable
  private Class<?> load(String className) {
    try {
      return Class.forName(className, false, getClass().getClassLoader());
    }
    catch (Throwable ex) {
      return null;
    }
  }

  @Nullable
  private List<ClassDescriptor> getTypeHierarchy(Class<?> type) {
    try {
      var typeHierarchy = new ArrayList<ClassDescriptor>();
      while (type != null && type != Object.class) {
        typeHierarchy.add(
                new ClassDescriptor(type.getCanonicalName(), type.getProtectionDomain().getCodeSource().getLocation())
        );
        type = type.getSuperclass();
      }
      return typeHierarchy;
    }
    catch (Throwable ex) {
      return null;
    }
  }

  private String getDescription(NoSuchMethodDescriptor callerDescriptor, NoSuchMethodDescriptor calledDescriptor) {
    StringWriter description = new StringWriter();
    PrintWriter writer = new PrintWriter(description);
    writer.println("An attempt was made to call a method that does not"
            + " exist. The attempt was made from the following location:");
    writer.println();
    writer.printf("    %s%n", callerDescriptor.errorMessage);
    writer.println();
    writer.println("The following method did not exist:");
    writer.println();
    writer.printf("    %s%n", calledDescriptor.errorMessage);
    writer.println();
    if (callerDescriptor.candidateLocations.size() > 1) {
      writer.printf("The calling method's class, %s, is available from the following locations:%n",
              callerDescriptor.className);
      writer.println();
      for (URL candidate : callerDescriptor.candidateLocations) {
        writer.printf("    %s%n", candidate);
      }
      writer.println();
      writer.println("The calling method's class was loaded from the following location:");
      writer.println();
      writer.printf("    %s%n", callerDescriptor.typeHierarchy.get(0).location);
    }
    else {
      writer.printf("The calling method's class, %s, was loaded from the following location:%n",
              callerDescriptor.className);
      writer.println();
      writer.printf("    %s%n", callerDescriptor.candidateLocations.get(0));
    }
    writer.println();
    writer.printf("The called method's class, %s, is available from the following locations:%n",
            calledDescriptor.className);
    writer.println();
    for (URL candidate : calledDescriptor.candidateLocations) {
      writer.printf("    %s%n", candidate);
    }
    writer.println();
    writer.println("The called method's class hierarchy was loaded from the following locations:");
    writer.println();
    for (ClassDescriptor type : calledDescriptor.typeHierarchy) {
      writer.printf("    %s: %s%n", type.name, type.location);
    }

    return description.toString();
  }

  private String getAction(NoSuchMethodDescriptor callerDescriptor, NoSuchMethodDescriptor calledDescriptor) {
    if (callerDescriptor.className.equals(calledDescriptor.className)) {
      return "Correct the classpath of your application so that it contains a single, compatible version of "
              + calledDescriptor.className;
    }
    else {
      return "Correct the classpath of your application so that it contains compatible versions of the classes "
              + callerDescriptor.className + " and " + calledDescriptor.className;
    }
  }

  protected static class NoSuchMethodDescriptor {

    public final String className;
    public final String errorMessage;
    public final List<URL> candidateLocations;
    public final List<ClassDescriptor> typeHierarchy;

    public NoSuchMethodDescriptor(
            String errorMessage, String className,
            List<URL> candidateLocations, List<ClassDescriptor> typeHierarchy) {
      this.errorMessage = errorMessage;
      this.className = className;
      this.candidateLocations = candidateLocations;
      this.typeHierarchy = typeHierarchy;
    }

  }

  protected static class ClassDescriptor {

    public final String name;

    public final URL location;

    public ClassDescriptor(String name, URL location) {
      this.name = name;
      this.location = location;
    }

  }

}
