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

import org.jspecify.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import infra.app.diagnostics.AbstractFailureAnalyzer;
import infra.app.diagnostics.FailureAnalysis;
import infra.util.ClassUtils;

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

  @Nullable
  @Override
  protected FailureAnalysis analyze(Throwable rootFailure, NoSuchMethodError cause) {
    NoSuchMethodDescriptor callerDescriptor = getCallerMethodDescriptor(cause);
    if (callerDescriptor == null) {
      return null;
    }
    String message = cause.getMessage();
    NoSuchMethodDescriptor calledDescriptor = getNoSuchMethodDescriptor((message != null) ? message : "");
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

    public NoSuchMethodDescriptor(String errorMessage, String className,
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
