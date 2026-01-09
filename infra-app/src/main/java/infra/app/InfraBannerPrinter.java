/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.app;

import org.jspecify.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import infra.aot.hint.RuntimeHints;
import infra.aot.hint.RuntimeHintsRegistrar;
import infra.core.ansi.AnsiColor;
import infra.core.ansi.AnsiOutput;
import infra.core.env.Environment;
import infra.core.io.Resource;
import infra.core.io.ResourceLoader;
import infra.lang.Version;
import infra.logging.Logger;

import static infra.app.Banner.BANNER_LOCATION_TXT;

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

  Banner print(Environment environment, @Nullable Class<?> sourceClass, Logger logger) {
    Banner banner = getBanner(environment);
    logger.info(createStringFromBanner(banner, environment, sourceClass));
    return new PrintedBanner(banner, sourceClass);
  }

  Banner print(Environment environment, @Nullable Class<?> sourceClass, PrintStream out) {
    Banner banner = getBanner(environment);
    banner.printBanner(environment, sourceClass, out);
    return new PrintedBanner(banner, sourceClass);
  }

  private Banner getBanner(Environment environment) {
    // Text Banner
    String location = environment.getProperty(Banner.BANNER_LOCATION, BANNER_LOCATION_TXT);
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

  private String createStringFromBanner(Banner banner,
          Environment environment, @Nullable Class<?> mainApplicationClass) {
    Charset charset = environment.getProperty(Banner.BANNER_CHARSET, Charset.class, StandardCharsets.UTF_8);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (PrintStream printStream = new PrintStream(baos, false, charset)) {
      banner.printBanner(environment, mainApplicationClass, printStream);
    }
    return baos.toString(charset);
  }

  /**
   * Decorator that allows a {@link Banner} to be printed again without needing to
   * specify the source class.
   */
  private record PrintedBanner(Banner banner, @Nullable Class<?> sourceClass) implements Banner {

    @Override
    public void printBanner(Environment environment, @Nullable Class<?> sourceClass, PrintStream out) {
      if (sourceClass == null) {
        sourceClass = this.sourceClass;
      }
      banner.printBanner(environment, sourceClass, out);
    }

  }

  private static final class DefaultBanner implements Banner {

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

  static class Hints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
      hints.resources().registerPattern(Banner.BANNER_LOCATION_TXT);
    }

  }
}
