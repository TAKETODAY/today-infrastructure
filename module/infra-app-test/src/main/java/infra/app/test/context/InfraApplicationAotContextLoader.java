package infra.app.test.context;

import infra.aot.hint.ExecutableMode;
import infra.aot.hint.RuntimeHints;
import infra.context.ApplicationContext;
import infra.context.ApplicationContextInitializer;
import infra.test.context.MergedContextConfiguration;
import infra.test.context.aot.AotContextLoader;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/4/13 20:57
 */
class InfraApplicationAotContextLoader extends InfraApplicationContextLoader implements AotContextLoader {

  @Override
  public ApplicationContext loadContextForAotProcessing(MergedContextConfiguration mergedConfig, RuntimeHints runtimeHints) throws Exception {
    return loadContext(mergedConfig, Mode.AOT_PROCESSING, null,
            mainMethod -> runtimeHints.reflection().registerMethod(mainMethod, ExecutableMode.INVOKE));
  }

  @Override
  public ApplicationContext loadContextForAotRuntime(MergedContextConfiguration mergedConfig, ApplicationContextInitializer initializer) throws Exception {
    return loadContext(mergedConfig, Mode.AOT_RUNTIME, initializer, null);
  }

}
