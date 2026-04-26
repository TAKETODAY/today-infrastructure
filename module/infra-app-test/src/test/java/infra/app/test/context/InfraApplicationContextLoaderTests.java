package infra.app.test.context;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import infra.aot.hint.RuntimeHints;
import infra.aot.hint.predicate.RuntimeHintsPredicates;
import infra.app.Application;
import infra.app.ApplicationContextFactory;
import infra.app.InfraConfiguration;
import infra.app.test.context.InfraTest.UseMainMethod;
import infra.web.reactive.context.GenericReactiveWebApplicationContext;
import infra.beans.factory.BeanCreationException;
import infra.context.ApplicationContext;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.PropertySource;
import infra.core.env.StandardEnvironment;
import infra.test.context.ActiveProfiles;
import infra.test.context.ApplicationContextFailureProcessor;
import infra.test.context.BootstrapUtils;
import infra.test.context.ContextConfiguration;
import infra.test.context.ContextHierarchy;
import infra.test.context.MergedContextConfiguration;
import infra.test.context.TestContext;
import infra.test.context.TestContextManager;
import infra.test.context.TestPropertySource;
import infra.test.context.support.TestPropertySourceUtils;
import infra.test.util.ReflectionTestUtils;
import infra.test.util.TestPropertyValues;
import infra.web.mock.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/4/13 22:03
 */
class InfraApplicationContextLoaderTests {

  @BeforeEach
  void setUp() {
    ContextLoaderApplicationContextFailureProcessor.reset();
  }

  @Test
  void environmentPropertiesSimple() {
    Map<String, Object> config = getMergedContextConfigurationProperties(SimpleConfig.class);
    assertKey(config, "key", "myValue");
    assertKey(config, "anotherKey", "anotherValue");
  }

  @Test
  void environmentPropertiesSimpleNonAlias() {
    Map<String, Object> config = getMergedContextConfigurationProperties(SimpleConfigNonAlias.class);
    assertKey(config, "key", "myValue");
    assertKey(config, "anotherKey", "anotherValue");
  }

  @Test
  void environmentPropertiesOverrideDefaults() {
    Map<String, Object> config = getMergedContextConfigurationProperties(OverrideConfig.class);
    assertKey(config, "server.port", "2345");
  }

  @Test
  void environmentPropertiesAppend() {
    Map<String, Object> config = getMergedContextConfigurationProperties(AppendConfig.class);
    assertKey(config, "key", "myValue");
    assertKey(config, "otherKey", "otherValue");
  }

  @Test
  void environmentPropertiesSeparatorInValue() {
    Map<String, Object> config = getMergedContextConfigurationProperties(SameSeparatorInValue.class);
    assertKey(config, "key", "my=Value");
    assertKey(config, "anotherKey", "another:Value");
  }

  @Test
  void environmentPropertiesAnotherSeparatorInValue() {
    Map<String, Object> config = getMergedContextConfigurationProperties(AnotherSeparatorInValue.class);
    assertKey(config, "key", "my:Value");
    assertKey(config, "anotherKey", "another=Value");
  }

  @Test // gh-4384
  @Disabled
  void environmentPropertiesNewLineInValue() {
    Map<String, Object> config = getMergedContextConfigurationProperties(NewLineInValue.class);
    assertKey(config, "key", "myValue");
    assertKey(config, "variables", "foo=FOO\n bar=BAR");
  }

  @Test
  void noActiveProfiles() {
    assertThat(getActiveProfiles(SimpleConfig.class)).isEmpty();
  }

  @Test
  void multipleActiveProfiles() {
    assertThat(getActiveProfiles(MultipleActiveProfiles.class)).containsExactly("profile1", "profile2");
  }

  @Test
    // gh-28776
  void testPropertyValuesShouldTakePrecedenceWhenInlinedPropertiesPresent() {
    TestContext context = new ExposedTestContextManager(SimpleConfig.class).getExposedTestContext();
    StandardEnvironment environment = (StandardEnvironment) context.getApplicationContext().getEnvironment();
    TestPropertyValues.of("key=thisValue").applyTo(environment);
    assertThat(environment.getProperty("key")).isEqualTo("thisValue");
    assertThat(environment.getPropertySources().get("active-test-profiles")).isNull();
  }

  @Test
  void testPropertyValuesShouldTakePrecedenceWhenInlinedPropertiesPresentAndProfilesActive() {
    TestContext context = new ExposedTestContextManager(ActiveProfileWithInlinedProperties.class)
            .getExposedTestContext();
    StandardEnvironment environment = (StandardEnvironment) context.getApplicationContext().getEnvironment();
    TestPropertyValues.of("key=thisValue").applyTo(environment);
    assertThat(environment.getProperty("key")).isEqualTo("thisValue");
    assertThat(environment.getPropertySources().get("active-test-profiles")).isNotNull();
  }

  @Test
  void propertySourceOrdering() {
    TestContext context = new ExposedTestContextManager(PropertySourceOrdering.class).getExposedTestContext();
    ConfigurableEnvironment environment = (ConfigurableEnvironment) context.getApplicationContext()
            .getEnvironment();
    List<String> names = environment.getPropertySources()
            .stream()
            .map(PropertySource::getName)
            .collect(Collectors.toCollection(ArrayList::new));
    String configResource = names.remove(names.size() - 2);
    assertThat(names).containsExactly("configurationProperties", "Inlined Test Properties", "commandLineArgs",
            "systemProperties", "systemEnvironment", "random", "applicationInfo");
    assertThat(configResource).startsWith("Config resource");
  }

  @Test
  void whenEnvironmentChangesWebApplicationTypeToNoneThenContextTypeChangesAccordingly() {
    TestContext context = new ExposedTestContextManager(ChangingWebApplicationTypeToNone.class)
            .getExposedTestContext();
    assertThat(context.getApplicationContext()).isNotInstanceOf(WebApplicationContext.class);
  }

  @Test
  void whenEnvironmentChangesWebApplicationTypeToReactiveThenContextTypeChangesAccordingly() {
    TestContext context = new ExposedTestContextManager(ChangingWebApplicationTypeToReactive.class)
            .getExposedTestContext();
    assertThat(context.getApplicationContext()).isInstanceOf(GenericReactiveWebApplicationContext.class);
  }

  @Test
  void whenUseMainMethodAlwaysAndMainMethodThrowsException() {
    TestContext testContext = new ExposedTestContextManager(UseMainMethodAlwaysAndMainMethodThrowsException.class)
            .getExposedTestContext();
    assertThatIllegalStateException().isThrownBy(testContext::getApplicationContext)
            .havingCause()
            .withMessageContaining("ThrownFromMain");
  }

  @Test
  void whenUseMainMethodWhenAvailableAndNoMainMethod() {
    TestContext testContext = new ExposedTestContextManager(UseMainMethodWhenAvailableAndNoMainMethod.class)
            .getExposedTestContext();
    ApplicationContext applicationContext = testContext.getApplicationContext();
    assertThat(applicationContext.getEnvironment().getActiveProfiles()).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(classes = { UsePublicMainMethodWhenAvailableAndMainMethod.class,
          UsePublicParameterlessMainMethodWhenAvailableAndMainMethod.class,
          UsePackagePrivateMainMethodWhenAvailableAndMainMethod.class,
          UsePackagePrivateParameterlessMainMethodWhenAvailableAndMainMethod.class })
  void whenUseMainMethodWhenAvailableAndMainMethod(Class<?> testClass) {
    TestContext testContext = new ExposedTestContextManager(testClass).getExposedTestContext();
    ApplicationContext applicationContext = testContext.getApplicationContext();
    assertThat(applicationContext.getEnvironment().getActiveProfiles()).contains("frommain");
  }

  @Test
  void whenUseMainMethodNever() {
    TestContext testContext = new ExposedTestContextManager(UseMainMethodNever.class).getExposedTestContext();
    ApplicationContext applicationContext = testContext.getApplicationContext();
    assertThat(applicationContext.getEnvironment().getActiveProfiles()).isEmpty();
  }

  @Test
  void whenUseMainMethodWithBeanThrowingException() {
    TestContext testContext = new ExposedTestContextManager(UseMainMethodWithBeanThrowingException.class)
            .getExposedTestContext();
    assertThatIllegalStateException().isThrownBy(testContext::getApplicationContext)
            .havingCause()
            .satisfies((exception) -> {
              assertThat(exception).isInstanceOf(BeanCreationException.class);
              assertThat(exception).isSameAs(ContextLoaderApplicationContextFailureProcessor.contextLoadException);
            });
    assertThat(ContextLoaderApplicationContextFailureProcessor.failedContext).isNotNull();
  }

  @Test
  void whenNoMainMethodWithBeanThrowingException() {
    TestContext testContext = new ExposedTestContextManager(NoMainMethodWithBeanThrowingException.class)
            .getExposedTestContext();
    assertThatIllegalStateException().isThrownBy(testContext::getApplicationContext)
            .havingCause()
            .satisfies((exception) -> {
              assertThat(exception).isInstanceOf(BeanCreationException.class);
              assertThat(exception).isSameAs(ContextLoaderApplicationContextFailureProcessor.contextLoadException);
            });
    assertThat(ContextLoaderApplicationContextFailureProcessor.failedContext).isNotNull();
  }

  @Test
  void whenUseMainMethodWithContextHierarchyThrowsException() {
    TestContext testContext = new ExposedTestContextManager(UseMainMethodWithContextHierarchy.class)
            .getExposedTestContext();
    assertThatIllegalStateException().isThrownBy(testContext::getApplicationContext)
            .havingCause()
            .withMessage("UseMainMethod.ALWAYS cannot be used with @ContextHierarchy tests");
  }

  @Test
  void whenMainMethodNotAvailableReturnsNoAotContribution() throws Exception {
    InfraApplicationContextLoader contextLoader = new InfraApplicationContextLoader();
    MergedContextConfiguration contextConfiguration = BootstrapUtils
            .resolveTestContextBootstrapper(UseMainMethodWhenAvailableAndNoMainMethod.class)
            .buildMergedContextConfiguration();
    RuntimeHints runtimeHints = mock(RuntimeHints.class);
    contextLoader.loadContextForAotProcessing(contextConfiguration, runtimeHints);
    then(runtimeHints).shouldHaveNoInteractions();
  }

  @Test
  void whenMainMethodPresentRegisterReflectionHints() throws Exception {
    InfraApplicationContextLoader contextLoader = new InfraApplicationContextLoader();
    MergedContextConfiguration contextConfiguration = BootstrapUtils
            .resolveTestContextBootstrapper(UsePublicMainMethodWhenAvailableAndMainMethod.class)
            .buildMergedContextConfiguration();
    RuntimeHints runtimeHints = new RuntimeHints();
    contextLoader.loadContextForAotProcessing(contextConfiguration, runtimeHints);
    assertThat(RuntimeHintsPredicates.reflection().onMethodInvocation(ConfigWithPublicMain.class, "main"))
            .accepts(runtimeHints);
  }

  @Test
  void whenSubclassProvidesCustomApplicationContextFactory() {
    TestContext testContext = new ExposedTestContextManager(CustomApplicationContextTest.class)
            .getExposedTestContext();
    assertThat(testContext.getApplicationContext()).isInstanceOf(CustomAnnotationConfigApplicationContext.class);
  }

  private String[] getActiveProfiles(Class<?> testClass) {
    TestContext testContext = new ExposedTestContextManager(testClass).getExposedTestContext();
    ApplicationContext applicationContext = testContext.getApplicationContext();
    return applicationContext.getEnvironment().getActiveProfiles();
  }

  private Map<String, Object> getMergedContextConfigurationProperties(Class<?> testClass) {
    TestContext context = new ExposedTestContextManager(testClass).getExposedTestContext();
    MergedContextConfiguration config = (MergedContextConfiguration) ReflectionTestUtils.getField(context,
            "mergedConfig");
    assertThat(config).isNotNull();
    return TestPropertySourceUtils.convertInlinedPropertiesToMap(config.getPropertySourceProperties());
  }

  private void assertKey(Map<String, Object> actual, String key, Object value) {
    assertThat(actual).as("Key '" + key + "' not found").containsKey(key);
    assertThat(actual).containsEntry(key, value);
  }

  @InfraTest(properties = { "key=myValue", "anotherKey:anotherValue" }, classes = Config.class)
  static class SimpleConfig {

  }

  @InfraTest(properties = { "key=myValue", "anotherKey:anotherValue" }, classes = Config.class)
  static class SimpleConfigNonAlias {

  }

  @InfraTest(properties = "server.port=2345", classes = Config.class)
  static class OverrideConfig {

  }

  @InfraTest(properties = { "key=myValue", "otherKey=otherValue" }, classes = Config.class)
  static class AppendConfig {

  }

  @InfraTest(properties = { "key=my=Value", "anotherKey:another:Value" }, classes = Config.class)
  static class SameSeparatorInValue {

  }

  @InfraTest(properties = { "key=my:Value", "anotherKey:another=Value" }, classes = Config.class)
  static class AnotherSeparatorInValue {

  }

  @InfraTest(properties = { "key=myValue", "variables=foo=FOO\n bar=BAR" }, classes = Config.class)
  static class NewLineInValue {

  }

  @InfraTest(classes = Config.class)
  @ActiveProfiles({ "profile1", "profile2" })
  static class MultipleActiveProfiles {

  }

  @InfraTest(properties = { "key=myValue" }, classes = Config.class)
  @ActiveProfiles({ "profile1" })
  static class ActiveProfileWithInlinedProperties {

  }

  @InfraTest(classes = Config.class, args = "args", properties = "one=1")
  @TestPropertySource(properties = "two=2")
  static class PropertySourceOrdering {

  }

  @InfraTest(classes = Config.class, args = "--app.main.application-type=normal")
  static class ChangingWebApplicationTypeToNone {

  }

  @InfraTest(classes = Config.class, args = "--app.main.application-type=reactive_web")
  static class ChangingWebApplicationTypeToReactive {

  }

  @InfraTest(classes = ConfigWithMainThrowingException.class, useMainMethod = UseMainMethod.ALWAYS)
  static class UseMainMethodAlwaysAndMainMethodThrowsException {

  }

  @InfraTest(classes = ConfigWithNoMain.class, useMainMethod = UseMainMethod.WHEN_AVAILABLE)
  static class UseMainMethodWhenAvailableAndNoMainMethod {

  }

  @InfraTest(classes = ConfigWithPublicMain.class, useMainMethod = UseMainMethod.WHEN_AVAILABLE)
  static class UsePublicMainMethodWhenAvailableAndMainMethod {

  }

  @InfraTest(classes = ConfigWithPublicParameterlessMain.class, useMainMethod = UseMainMethod.WHEN_AVAILABLE)
  static class UsePublicParameterlessMainMethodWhenAvailableAndMainMethod {

  }

  @InfraTest(classes = ConfigWithPackagePrivateMain.class, useMainMethod = UseMainMethod.WHEN_AVAILABLE)
  static class UsePackagePrivateMainMethodWhenAvailableAndMainMethod {

  }

  @InfraTest(classes = ConfigWithPackagePrivateParameterlessMain.class,
          useMainMethod = UseMainMethod.WHEN_AVAILABLE)
  static class UsePackagePrivateParameterlessMainMethodWhenAvailableAndMainMethod {

  }

  @InfraTest(classes = ConfigWithPublicMain.class, useMainMethod = UseMainMethod.NEVER)
  static class UseMainMethodNever {

  }

  @InfraTest(classes = ConfigWithMainWithBeanThrowingException.class, useMainMethod = UseMainMethod.ALWAYS)
  static class UseMainMethodWithBeanThrowingException {

  }

  @InfraTest(classes = ConfigWithNoMainWithBeanThrowingException.class, useMainMethod = UseMainMethod.NEVER)
  static class NoMainMethodWithBeanThrowingException {

  }

  @InfraTest(useMainMethod = UseMainMethod.ALWAYS)
  @ContextHierarchy({ @ContextConfiguration(classes = ConfigWithPublicMain.class),
          @ContextConfiguration(classes = AnotherConfigWithMain.class) })
  static class UseMainMethodWithContextHierarchy {

  }

  @InfraTest
  @ContextConfiguration(classes = Config.class, loader = CustomApplicationContextInfraApplicationContextLoader.class)
  static class CustomApplicationContextTest {

  }

  static class CustomApplicationContextInfraApplicationContextLoader extends InfraApplicationContextLoader {

    @Override
    protected ApplicationContextFactory getApplicationContextFactory(MergedContextConfiguration mergedConfig) {
      return (webApplicationType) -> new CustomAnnotationConfigApplicationContext();
    }

  }

  static class CustomAnnotationConfigApplicationContext extends AnnotationConfigApplicationContext {

  }

  @Configuration(proxyBeanMethods = false)
  static class Config {

  }

  @InfraConfiguration(proxyBeanMethods = false)
  public static class ConfigWithPublicMain {

    public static void main(String[] args) {
      new Application(ConfigWithPublicMain.class).run("--infra.profiles.active=frommain");
    }

  }

  @InfraConfiguration(proxyBeanMethods = false)
  public static class ConfigWithPublicParameterlessMain {

    public static void main() {
      new Application(ConfigWithPublicMain.class).run("--infra.profiles.active=frommain");
    }

  }

  @InfraConfiguration(proxyBeanMethods = false)
  public static class ConfigWithPackagePrivateMain {

    static void main(String[] args) {
      new Application(ConfigWithPublicMain.class).run("--infra.profiles.active=frommain");
    }

  }

  @InfraConfiguration(proxyBeanMethods = false)
  public static class ConfigWithPackagePrivateParameterlessMain {

    static void main() {
      new Application(ConfigWithPublicMain.class).run("--infra.profiles.active=frommain");
    }

  }

  @InfraConfiguration(proxyBeanMethods = false)
  public static class AnotherConfigWithMain {

    public static void main(String[] args) {
      new Application(AnotherConfigWithMain.class).run("--infra.profiles.active=anotherfrommain");
    }

  }

  @InfraConfiguration(proxyBeanMethods = false)
  static class ConfigWithNoMain {

  }

  @InfraConfiguration(proxyBeanMethods = false)
  public static class ConfigWithMainWithBeanThrowingException {

    public static void main(String[] args) {
      new Application(ConfigWithMainWithBeanThrowingException.class).run();
    }

    @Bean
    String failContextLoad() {
      throw new RuntimeException("ThrownFromBeanMethod");
    }

  }

  @InfraConfiguration(proxyBeanMethods = false)
  static class ConfigWithNoMainWithBeanThrowingException {

    @Bean
    String failContextLoad() {
      throw new RuntimeException("ThrownFromBeanMethod");
    }

  }

  @InfraConfiguration(proxyBeanMethods = false)
  public static class ConfigWithMainThrowingException {

    public static void main(String[] args) {
      throw new RuntimeException("ThrownFromMain");
    }

  }

  /**
   * {@link TestContextManager} which exposes the {@link TestContext}.
   */
  static class ExposedTestContextManager extends TestContextManager {

    ExposedTestContextManager(Class<?> testClass) {
      super(testClass);
    }

    final TestContext getExposedTestContext() {
      return super.getTestContext();
    }

  }

  private static final class ContextLoaderApplicationContextFailureProcessor
          implements ApplicationContextFailureProcessor {

    static @Nullable ApplicationContext failedContext;

    static @Nullable Throwable contextLoadException;

    @Override
    public void processLoadFailure(ApplicationContext context, @Nullable Throwable exception) {
      failedContext = context;
      contextLoadException = exception;
    }

    private static void reset() {
      failedContext = null;
      contextLoadException = null;
    }

  }

}