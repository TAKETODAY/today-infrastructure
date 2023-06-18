/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.core.test.tools;

/**
 * Exception thrown when code cannot compile.
 *
 * @author Phillip Webb
 * @since 4.0
 */
@SuppressWarnings("serial")
public class CompilationException extends RuntimeException {

  CompilationException(String errors, SourceFiles sourceFiles, ResourceFiles resourceFiles) {
    super(buildMessage(errors, sourceFiles, resourceFiles));
  }

  private static String buildMessage(String errors, SourceFiles sourceFiles,
          ResourceFiles resourceFiles) {
    StringBuilder message = new StringBuilder();
    message.append("Unable to compile source\n\n");
    message.append(errors);
    message.append("\n\n");
    for (SourceFile sourceFile : sourceFiles) {
      message.append("---- source:   ").append(sourceFile.getPath()).append("\n\n");
      message.append(sourceFile.getContent());
      message.append("\n\n");
    }
    for (ResourceFile resourceFile : resourceFiles) {
      message.append("---- resource: ").append(resourceFile.getPath()).append("\n\n");
      message.append(resourceFile.getContent());
      message.append("\n\n");
    }
    return message.toString();
  }

}
