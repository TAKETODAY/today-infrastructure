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

package cn.taketoday.framework.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.MDC;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

import static org.assertj.core.api.Assertions.contentOf;

/**
 * Base for {@link LoggingSystem} tests.
 *
 * @author Ilya Lukyanovich
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public abstract class AbstractLoggingSystemTests {

  private static final String JAVA_IO_TMPDIR = "java.io.tmpdir";

  private String originalTempDirectory;

  @BeforeEach
  void configureTempDir(@TempDir Path temp) {
    this.originalTempDirectory = System.getProperty(JAVA_IO_TMPDIR);
    System.setProperty(JAVA_IO_TMPDIR, temp.toAbsolutePath().toString());
  }

  @AfterEach
  void reinstateTempDir() {
    System.setProperty(JAVA_IO_TMPDIR, this.originalTempDirectory);
  }

  @AfterEach
  void clear() {
    for (LoggingSystemProperty property : LoggingSystemProperty.values()) {
      System.getProperties().remove(property.getEnvironmentVariableName());
    }
    MDC.clear();
  }

  protected final String[] getConfigLocations(AbstractLoggingSystem system) {
    return system.getInfraConfigLocations();
  }

  protected final LogFile getLogFile(String file, @Nullable String path) {
    return getLogFile(file, path, true);
  }

  protected final LogFile getLogFile(String file, @Nullable String path, boolean applyToSystemProperties) {
    LogFile logFile = new LogFile(file, path);
    if (applyToSystemProperties) {
      logFile.applyToSystemProperties();
    }
    return logFile;
  }

  protected final String tmpDir() {
    String path = StringUtils.cleanPath(System.getProperty(JAVA_IO_TMPDIR));
    if (path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }
    return path;
  }

  @Nullable
  protected final String getLineWithText(File file, CharSequence outputSearch) {
    return getLineWithText(contentOf(file), outputSearch);
  }

  @Nullable
  protected final String getLineWithText(CharSequence output, CharSequence outputSearch) {
    return Arrays.stream(output.toString().split("\\r?\\n"))
            .filter((line) -> line.contains(outputSearch))
            .findFirst()
            .orElse(null);
  }

}
