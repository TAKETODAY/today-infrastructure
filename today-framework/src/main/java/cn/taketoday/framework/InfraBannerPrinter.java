/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import cn.taketoday.core.env.Environment;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.framework.ansi.AnsiColor;
import cn.taketoday.framework.ansi.AnsiOutput;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.Version;
import cn.taketoday.logging.Logger;

/**
 * Class used by {@link Application} to print the application banner.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/29 17:58
 */
class InfraBannerPrinter {

  private final ResourceLoader resourceLoader;

  @Nullable
  private final Banner fallbackBanner;

  InfraBannerPrinter(ResourceLoader resourceLoader, @Nullable Banner fallbackBanner) {
    this.resourceLoader = resourceLoader;
    this.fallbackBanner = fallbackBanner;
  }

  Banner print(Environment environment, Class<?> sourceClass, Logger logger) {
    Banner banner = getBanner(environment);
    logger.info(createStringFromBanner(banner, environment, sourceClass));
    return new PrintedBanner(banner, sourceClass);
  }

  Banner print(Environment environment, Class<?> sourceClass, PrintStream out) {
    Banner banner = getBanner(environment);
    banner.printBanner(environment, sourceClass, out);
    return new PrintedBanner(banner, sourceClass);
  }

  private Banner getBanner(Environment environment) {
    // Text Banner
    String location = environment.getProperty(Banner.BANNER_LOCATION, Banner.BANNER_LOCATION_TXT);
    Resource resource = resourceLoader.getResource(location);
    try {
      if (resource.exists() && !resource.getURL().toExternalForm().contains("liquibase-core")) {
        return new ResourceBanner(resource);
      }
    }
    catch (IOException ex) {
      // Ignore
    }

    if (fallbackBanner != null) {
      return fallbackBanner;
    }
    return new DefaultBanner();
  }

  private String createStringFromBanner(
          Banner banner, Environment environment, Class<?> mainApplicationClass) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    banner.printBanner(environment, mainApplicationClass, new PrintStream(baos));
    Charset charset = environment.getProperty(Banner.BANNER_CHARSET, Charset.class, StandardCharsets.UTF_8);
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

    private static final String BANNER = """
             ______  ____    ___    ___  __  __        ____   _  __   ____   ___    ___\s
            /_  __/ / __ \\  / _ \\  / _ | \\ \\/ /       /  _/  / |/ /  / __/  / _ \\  / _ |
             / /   / /_/ / / // / / __ |  \\  /       _/ /   /    /  / _/   / , _/ / __ |
            /_/    \\____/ /____/ /_/ |_|  /_/       /___/  /_/|_/  /_/    /_/|_| /_/ |_|\
            """;

    @Override
    public void printBanner(Environment environment, @Nullable Class<?> sourceClass, PrintStream out) {
      out.print(BANNER);

      String version = Version.instance.implementationVersion();
      out.println(AnsiOutput.toString(AnsiColor.CYAN, " (v", version, ")"));
      out.println();
    }
  }

}
