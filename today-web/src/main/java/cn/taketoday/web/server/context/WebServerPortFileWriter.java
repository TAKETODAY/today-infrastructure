/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.web.server.context;

import java.io.File;
import java.util.Locale;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationListener;
import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.FileCopyUtils;
import cn.taketoday.util.StringUtils;

/**
 * An {@link ApplicationListener} that saves embedded server port and management port into
 * file. This application listener will be triggered whenever the server starts, and the
 * file name can be overridden at runtime with a System property or environment variable
 * named "PORTFILE" or "portfile".
 *
 * @author David Liu
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class WebServerPortFileWriter implements ApplicationListener<WebServerInitializedEvent> {

  private static final String DEFAULT_FILE_NAME = "application.port";

  private static final String[] PROPERTY_VARIABLES = { "PORTFILE", "portfile" };

  private static final Logger logger = LoggerFactory.getLogger(WebServerPortFileWriter.class);

  private final File file;

  /**
   * Create a new {@link WebServerPortFileWriter} instance using the filename
   * 'application.port'.
   */
  public WebServerPortFileWriter() {
    this(new File(DEFAULT_FILE_NAME));
  }

  /**
   * Create a new {@link WebServerPortFileWriter} instance with a specified filename.
   *
   * @param filename the name of file containing port
   */
  public WebServerPortFileWriter(String filename) {
    this(new File(filename));
  }

  /**
   * Create a new {@link WebServerPortFileWriter} instance with a specified file.
   *
   * @param file the file containing port
   */
  public WebServerPortFileWriter(File file) {
    Assert.notNull(file, "File is required");
    String override = getSystemProperties(PROPERTY_VARIABLES);
    if (override != null) {
      this.file = new File(override);
    }
    else {
      this.file = file;
    }
  }

  @Override
  public void onApplicationEvent(WebServerInitializedEvent event) {
    File portFile = getPortFile(event.getApplicationContext());
    try {
      String port = String.valueOf(event.getWebServer().getPort());
      createParentDirectory(portFile);
      FileCopyUtils.copy(port.getBytes(), portFile);
      portFile.deleteOnExit();
    }
    catch (Exception ex) {
      logger.warn("Cannot create port file {}", file);
    }
  }

  /**
   * Return the actual port file that should be written for the given application
   * context. The default implementation builds a file from the source file and the
   * application context namespace if available.
   *
   * @param applicationContext the source application context
   * @return the file that should be written
   */
  protected File getPortFile(ApplicationContext applicationContext) {
    String namespace = getServerNamespace(applicationContext);
    if (StringUtils.isEmpty(namespace)) {
      return this.file;
    }
    String name = this.file.getName();
    String extension = StringUtils.getFilenameExtension(this.file.getName());
    name = name.substring(0, name.length() - extension.length() - 1);
    if (isUpperCase(name)) {
      name = name + "-" + namespace.toUpperCase(Locale.ENGLISH);
    }
    else {
      name = name + "-" + namespace.toLowerCase(Locale.ENGLISH);
    }
    if (StringUtils.isNotEmpty(extension)) {
      name = name + "." + extension;
    }
    return new File(this.file.getParentFile(), name);
  }

  private String getServerNamespace(ApplicationContext applicationContext) {
    if (applicationContext instanceof WebServerApplicationContext) {
      return ((WebServerApplicationContext) applicationContext).getServerNamespace();
    }
    return null;
  }

  private boolean isUpperCase(String name) {
    for (int i = 0; i < name.length(); i++) {
      if (Character.isLetter(name.charAt(i)) && !Character.isUpperCase(name.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  private void createParentDirectory(File file) {
    File parent = file.getParentFile();
    if (parent != null) {
      parent.mkdirs();
    }
  }

  public static String getSystemProperties(String... properties) {
    for (String property : properties) {
      try {
        String override = System.getProperty(property);
        override = (override != null) ? override : System.getenv(property);
        if (override != null) {
          return override;
        }
      }
      catch (Throwable ex) {
        System.err.printf("Could not resolve '%s' as system property: %s%n", property, ex);
      }
    }
    return null;
  }

}
