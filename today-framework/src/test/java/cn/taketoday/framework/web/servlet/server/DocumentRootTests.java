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

package cn.taketoday.framework.web.servlet.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.URL;
import java.security.CodeSource;
import java.security.cert.Certificate;

import cn.taketoday.logging.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DocumentRoot}.
 *
 * @author Phillip Webb
 */
class DocumentRootTests {

  @TempDir
  File tempDir;

  private DocumentRoot documentRoot = new DocumentRoot(LoggerFactory.getLogger(getClass()));

  @Test
  void explodedWarFileDocumentRootWhenRunningFromExplodedWar() throws Exception {
    File codeSourceFile = new File(this.tempDir, "test.war/WEB-INF/lib/spring-boot.jar");
    codeSourceFile.getParentFile().mkdirs();
    codeSourceFile.createNewFile();
    File directory = this.documentRoot.getExplodedWarFileDocumentRoot(codeSourceFile);
    assertThat(directory).isEqualTo(codeSourceFile.getParentFile().getParentFile().getParentFile());
  }

  @Test
  void explodedWarFileDocumentRootWhenRunningFromPackagedWar() {
    File codeSourceFile = new File(this.tempDir, "test.war");
    File directory = this.documentRoot.getExplodedWarFileDocumentRoot(codeSourceFile);
    assertThat(directory).isNull();
  }

  @Test
  void codeSourceArchivePath() throws Exception {
    CodeSource codeSource = new CodeSource(new URL("file", "", "/some/test/path/"), (Certificate[]) null);
    File codeSourceArchive = this.documentRoot.getCodeSourceArchive(codeSource);
    assertThat(codeSourceArchive).isEqualTo(new File("/some/test/path/"));
  }

  @Test
  void codeSourceArchivePathContainingSpaces() throws Exception {
    CodeSource codeSource = new CodeSource(new URL("file", "", "/test/path/with%20space/"), (Certificate[]) null);
    File codeSourceArchive = this.documentRoot.getCodeSourceArchive(codeSource);
    assertThat(codeSourceArchive).isEqualTo(new File("/test/path/with space/"));
  }

}
