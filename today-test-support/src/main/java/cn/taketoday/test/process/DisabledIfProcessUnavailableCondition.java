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

package cn.taketoday.test.process;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.annotation.MergedAnnotations.SearchStrategy;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.StringUtils;

/**
 * An {@link ExecutionCondition} that disables execution if specified processes cannot
 * start.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class DisabledIfProcessUnavailableCondition implements ExecutionCondition {

  private static final String USR_LOCAL_BIN = "/usr/local/bin";

  private static final boolean MAC_OS = System.getProperty("os.name").toLowerCase().contains("mac");

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    List<String[]> commands = new ArrayList<>();
    context.getTestClass().map(this::getAnnotationValue).orElse(Stream.empty()).forEach(commands::add);
    context.getTestMethod().map(this::getAnnotationValue).orElse(Stream.empty()).forEach(commands::add);
    try {
      commands.forEach(this::check);
      return ConditionEvaluationResult.enabled("All processes available");
    }
    catch (Throwable ex) {
      return ConditionEvaluationResult.disabled("Process unavailable", ex.getMessage());
    }
  }

  private Stream<String[]> getAnnotationValue(AnnotatedElement testElement) {
    return MergedAnnotations.from(testElement, SearchStrategy.TYPE_HIERARCHY)
        .stream(DisabledIfProcessUnavailable.class)
        .map((annotation) -> annotation.getStringArray(MergedAnnotation.VALUE));
  }

  private void check(String[] command) {
    ProcessBuilder processBuilder = new ProcessBuilder(command);
    try {
      Process process = processBuilder.start();
      process.waitFor();
      Assert.state(process.exitValue() == 0, () -> "Process exited with %d".formatted(process.exitValue()));
      process.destroy();
    }
    catch (Exception ex) {
      String path = processBuilder.environment().get("PATH");
      if (MAC_OS && path != null && !path.contains(USR_LOCAL_BIN)
          && !command[0].startsWith(USR_LOCAL_BIN + "/")) {
        String[] localCommand = command.clone();
        localCommand[0] = USR_LOCAL_BIN + "/" + localCommand[0];
        check(localCommand);
        return;
      }
      throw new IllegalStateException(
          "Unable to start process '%s'".formatted(StringUtils.arrayToDelimitedString(command, " ")));
    }
  }

}
