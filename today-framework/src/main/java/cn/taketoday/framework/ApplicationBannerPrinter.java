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

package cn.taketoday.framework;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import cn.taketoday.core.env.Environment;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.framework.ansi.AnsiColor;
import cn.taketoday.framework.ansi.AnsiOutput;
import cn.taketoday.framework.ansi.AnsiStyle;
import cn.taketoday.lang.Version;
import cn.taketoday.logging.Logger;

/**
 * Class used by {@link Application} to print the application banner.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/29 17:58
 */
class ApplicationBannerPrinter {

  static final String BANNER_LOCATION_PROPERTY = "banner.location";

  static final String DEFAULT_BANNER_LOCATION = "banner.txt";

  private static final Banner DEFAULT_BANNER = new DefaultBanner();

  private final ResourceLoader resourceLoader;

  private final Banner fallbackBanner;

  ApplicationBannerPrinter(ResourceLoader resourceLoader, Banner fallbackBanner) {
    this.resourceLoader = resourceLoader;
    this.fallbackBanner = fallbackBanner;
  }

  Banner print(Environment environment, Class<?> sourceClass, Logger logger) {
    Banner banner = getBanner(environment);
    try {
      logger.info(createStringFromBanner(banner, environment, sourceClass));
    }
    catch (UnsupportedEncodingException ex) {
      logger.warn("Failed to create String for banner", ex);
    }
    return new PrintedBanner(banner, sourceClass);
  }

  Banner print(Environment environment, Class<?> sourceClass, PrintStream out) {
    Banner banner = getBanner(environment);
    banner.printBanner(environment, sourceClass, out);
    return new PrintedBanner(banner, sourceClass);
  }

  private Banner getBanner(Environment environment) {
    Banner textBanner = getTextBanner(environment);
    if (textBanner != null) {
      return textBanner;
    }
    if (this.fallbackBanner != null) {
      return this.fallbackBanner;
    }
    return DEFAULT_BANNER;
  }

  private Banner getTextBanner(Environment environment) {
    String location = environment.getProperty(BANNER_LOCATION_PROPERTY, DEFAULT_BANNER_LOCATION);
    Resource resource = this.resourceLoader.getResource(location);
    try {
      if (resource.exists() && !resource.getLocation().toExternalForm().contains("liquibase-core")) {
        return new ResourceBanner(resource);
      }
    }
    catch (IOException ex) {
      // Ignore
    }
    return null;
  }

  private String createStringFromBanner(Banner banner, Environment environment, Class<?> mainApplicationClass)
          throws UnsupportedEncodingException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    banner.printBanner(environment, mainApplicationClass, new PrintStream(baos));
    String charset = environment.getProperty("banner.charset", "UTF-8");
    return baos.toString(charset);
  }

  /**
   * Decorator that allows a {@link Banner} to be printed again without needing to
   * specify the source class.
   */
  private record PrintedBanner(Banner banner, Class<?> sourceClass) implements Banner {

    @Override
    public void printBanner(Environment environment, Class<?> sourceClass, PrintStream out) {
      if (sourceClass == null) {
        sourceClass = this.sourceClass;
      }
      banner.printBanner(environment, sourceClass, out);
    }

  }

  private static class DefaultBanner implements Banner {

    private static final String[] BANNER = {
            "",
            "  .   ____          _            __ _ _",
            " /\\\\ / ___'_ __ _ _(_)_ __  __ _ \\ \\ \\ \\",
            "( ( )\\___ | '_ | '_| | '_ \\/ _` | \\ \\ \\ \\",
            " \\\\/  ___)| |_)| | | | | || (_| |  ) ) ) )",
            "  '  |____| .__|_| |_|_| |_\\__, | / / / /",
            " =========|_|==============|___/=/_/_/_/"
    };

    private static final String SPRING_BOOT = " :: today-infrastructure :: ";

    private static final int STRAP_LINE_SIZE = 42;

    @Override
    public void printBanner(Environment environment, Class<?> sourceClass, PrintStream out) {
      for (String line : BANNER) {
        out.println(line);
      }
      String version = Version.get().toString();
      version = " (v" + version + ")";
      StringBuilder padding = new StringBuilder();
      while (padding.length() < STRAP_LINE_SIZE - (version.length() + SPRING_BOOT.length())) {
        padding.append(" ");
      }

      out.println(AnsiOutput.toString(AnsiColor.GREEN, SPRING_BOOT, AnsiColor.DEFAULT, padding.toString(), AnsiStyle.FAINT, version));
      out.println();
    }
  }

}
