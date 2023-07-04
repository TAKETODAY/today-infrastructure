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

package cn.taketoday.framework;

import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.Arrays;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.aot.ContextAotProcessor;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.framework.Application.AbandonedRunException;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.util.function.ThrowingSupplier;

/**
 * Entry point for AOT processing of a {@link Application}.
 * <p>
 * <strong>For internal use only.</strong>
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ApplicationAotProcessor extends ContextAotProcessor {

  private final String[] applicationArgs;

  /**
   * Create a new processor for the specified application and settings.
   *
   * @param application the application main class
   * @param settings the general AOT processor settings
   * @param applicationArgs the arguments to provide to the main method
   */
  public ApplicationAotProcessor(Class<?> application, Settings settings, String[] applicationArgs) {
    super(application, settings);
    this.applicationArgs = applicationArgs;
  }

  @Override
  protected GenericApplicationContext prepareApplicationContext(Class<?> application) {
    return new AotProcessorHook(application).run(() -> {
      Method mainMethod = application.getMethod("main", String[].class);
      return ReflectionUtils.invokeMethod(mainMethod, null, new Object[] { this.applicationArgs });
    });
  }

  public static void main(String[] args) throws Exception {
    int requiredArgs = 6;
    Assert.isTrue(args.length >= requiredArgs, () -> "Usage: " + ApplicationAotProcessor.class.getName()
            + " <applicationName> <sourceOutput> <resourceOutput> <classOutput> <groupId> <artifactId> <originalArgs...>");
    Class<?> application = Class.forName(args[0]);
    Settings settings = Settings.builder()
            .sourceOutput(Paths.get(args[1]))
            .resourceOutput(Paths.get(args[2]))
            .classOutput(Paths.get(args[3]))
            .groupId((StringUtils.hasText(args[4])) ? args[4] : "unspecified")
            .artifactId(args[5])
            .build();
    String[] applicationArgs = (args.length > requiredArgs)
                               ? Arrays.copyOfRange(args, requiredArgs, args.length)
                               : new String[0];
    new ApplicationAotProcessor(application, settings, applicationArgs).process();
  }

  /**
   * {@link ApplicationHook} used to capture the {@link ApplicationContext} and
   * trigger early exit of main method.
   */
  private static final class AotProcessorHook implements ApplicationHook {

    private final Class<?> application;

    private AotProcessorHook(Class<?> application) {
      this.application = application;
    }

    @Override
    public ApplicationStartupListener getStartupListener(Application application) {
      return new ApplicationStartupListener() {

        @Override
        public void contextLoaded(ConfigurableApplicationContext context) {
          throw new AbandonedRunException(context);
        }

      };
    }

    private <T> GenericApplicationContext run(ThrowingSupplier<T> action) {
      try {
        Application.withHook(this, action);
      }
      catch (AbandonedRunException ex) {
        ApplicationContext context = ex.getApplicationContext();
        Assert.isInstanceOf(GenericApplicationContext.class, context,
                () -> "AOT processing requires a GenericApplicationContext but got a "
                        + context.getClass().getName());
        return (GenericApplicationContext) context;
      }
      throw new IllegalStateException(
              "No application context available after calling main method of '%s'. Does it run a Application?"
                      .formatted(this.application.getName()));
    }

  }

}
