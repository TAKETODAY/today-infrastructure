/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.web.server.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import infra.util.StringUtils;
import infra.web.server.WebServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/2/22 23:43
 */
class WebServerPortFileWriterTests {

  @TempDir
  File tempDir;

  @BeforeEach
  @AfterEach
  void reset() {
    System.clearProperty("PORTFILE");
  }

  @Test
  void createPortFile() {
    File file = new File(this.tempDir, "port.file");
    WebServerPortFileWriter listener = new WebServerPortFileWriter(file);
    listener.onApplicationEvent(mockEvent("", 8080));
    assertThat(contentOf(file)).isEqualTo("8080");
  }

  @Test
  void overridePortFileWithDefault() {
    System.setProperty("PORTFILE", new File(this.tempDir, "port.file").getAbsolutePath());
    WebServerPortFileWriter listener = new WebServerPortFileWriter();
    listener.onApplicationEvent(mockEvent("", 8080));
    String content = contentOf(new File(System.getProperty("PORTFILE")));
    assertThat(content).isEqualTo("8080");
  }

  @Test
  void overridePortFileWithExplicitFile() {
    File file = new File(this.tempDir, "port.file");
    System.setProperty("PORTFILE", new File(this.tempDir, "override.file").getAbsolutePath());
    WebServerPortFileWriter listener = new WebServerPortFileWriter(file);
    listener.onApplicationEvent(mockEvent("", 8080));
    String content = contentOf(new File(System.getProperty("PORTFILE")));
    assertThat(content).isEqualTo("8080");
  }

  @Test
  void createManagementPortFile() {
    File file = new File(this.tempDir, "port.file");
    WebServerPortFileWriter listener = new WebServerPortFileWriter(file);
    listener.onApplicationEvent(mockEvent("", 8080));
    listener.onApplicationEvent(mockEvent("management", 9090));
    assertThat(contentOf(file)).isEqualTo("8080");
    String managementFile = file.getName();
    managementFile = managementFile.substring(0,
            managementFile.length() - StringUtils.getFilenameExtension(managementFile).length() - 1);
    managementFile = managementFile + "-management." + StringUtils.getFilenameExtension(file.getName());
    String content = contentOf(new File(file.getParentFile(), managementFile));
    assertThat(content).isEqualTo("9090");
    assertThat(collectFileNames(file.getParentFile())).contains(managementFile);
  }

  @Test
  void createUpperCaseManagementPortFile() {
    File file = new File(this.tempDir, "port.file");
    file = new File(file.getParentFile(), file.getName().toUpperCase(Locale.ENGLISH));
    WebServerPortFileWriter listener = new WebServerPortFileWriter(file);
    listener.onApplicationEvent(mockEvent("management", 9090));
    String managementFile = file.getName();
    managementFile = managementFile.substring(0,
            managementFile.length() - StringUtils.getFilenameExtension(managementFile).length() - 1);
    managementFile = managementFile + "-MANAGEMENT." + StringUtils.getFilenameExtension(file.getName());
    String content = contentOf(new File(file.getParentFile(), managementFile));
    assertThat(content).isEqualTo("9090");
    assertThat(collectFileNames(file.getParentFile())).contains(managementFile);
  }

  @Test
  void getPortFileWhenPortFileNameDoesNotHaveExtension() {
    File file = new File(this.tempDir, "portfile");
    WebServerPortFileWriter listener = new WebServerPortFileWriter(file);
    WebServerApplicationContext applicationContext = mock(WebServerApplicationContext.class);
    given(applicationContext.getServerNamespace()).willReturn("management");
    assertThat(listener.getPortFile(applicationContext).getName()).isEqualTo("portfile-management");
  }

  private WebServerInitializedEvent mockEvent(String namespace, int port) {
    WebServer webServer = mock(WebServer.class);
    given(webServer.getPort()).willReturn(port);
    WebServerApplicationContext applicationContext = mock(WebServerApplicationContext.class);
    given(applicationContext.getServerNamespace()).willReturn(namespace);
    given(applicationContext.getWebServer()).willReturn(webServer);
    WebServerInitializedEvent event = mock(WebServerInitializedEvent.class);
    given(event.getApplicationContext()).willReturn(applicationContext);
    given(event.getWebServer()).willReturn(webServer);
    return event;
  }

  private Set<String> collectFileNames(File directory) {
    Set<String> names = new HashSet<>();
    if (directory.isDirectory()) {
      for (File file : directory.listFiles()) {
        names.add(file.getName());
      }
    }
    return names;
  }

}