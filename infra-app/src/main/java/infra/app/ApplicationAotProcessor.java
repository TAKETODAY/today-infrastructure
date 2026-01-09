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

import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.Arrays;

import infra.app.Application.AbandonedRunException;
import infra.context.ApplicationContext;
import infra.context.ConfigurableApplicationContext;
import infra.context.aot.ContextAotProcessor;
import infra.context.support.GenericApplicationContext;
import infra.lang.Assert;
import infra.util.ReflectionUtils;
import infra.util.StringUtils;
import infra.util.function.ThrowingSupplier;

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
      ReflectionUtils.invokeMethod(mainMethod, null, new Object[] { this.applicationArgs });
      return Void.class;
    });
  }

  public static void main(String[] args) throws Exception {
    int requiredArgs = 6;
    Assert.isTrue(args.length >= requiredArgs, () -> "Usage: " + ApplicationAotProcessor.class.getName()
            + " <applicationMainClass> <sourceOutput> <resourceOutput> <classOutput> <groupId> <artifactId> <originalArgs...>");
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
        Assert.state(context instanceof GenericApplicationContext,
                () -> "AOT processing requires a GenericApplicationContext but got a "
                        + ((context != null) ? context.getClass().getName() : "null"));
        return (GenericApplicationContext) context;
      }
      throw new IllegalStateException(
              "No application context available after calling main method of '%s'. Does it run a Application?"
                      .formatted(this.application.getName()));
    }

  }

}
