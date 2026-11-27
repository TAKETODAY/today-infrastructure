/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.scripting.support;

import org.junit.jupiter.api.Test;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

import infra.aop.support.AopUtils;
import infra.beans.FatalBeanException;
import infra.beans.factory.BeanCreationException;
import infra.beans.factory.BeanDefinitionStoreException;
import infra.beans.factory.BeanDefinitionValidationException;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.support.BeanDefinitionBuilder;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.context.support.ClassPathXmlApplicationContext;
import infra.context.support.GenericApplicationContext;
import infra.core.Ordered;
import infra.scripting.Messenger;
import infra.scripting.ScriptCompilationException;
import infra.scripting.ScriptFactory;
import infra.scripting.ScriptSource;
import infra.scripting.groovy.GroovyScriptFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Chris Beams
 */
//@EnabledForTestGroups(LONG_RUNNING)
class ScriptFactoryPostProcessorTests {

  private static final String MESSAGE_TEXT = "Bingo";

  private static final String MESSENGER_BEAN_NAME = "messenger";

  private static final String PROCESSOR_BEAN_NAME = "processor";

  private static final String CHANGED_SCRIPT = "package infra.scripting.groovy\n" +
          "import infra.scripting.Messenger\n" +
          "class GroovyMessenger implements Messenger {\n" +
          "  private String message = \"Bingo\"\n" +
          "  public String getMessage() {\n" +
          // quote the returned message (this is the change)...
          "    return \"'\"  + this.message + \"'\"\n" +
          "  }\n" +
          "  public void setMessage(String message) {\n" +
          "    this.message = message\n" +
          "  }\n" +
          "}";

  private static final String EXPECTED_CHANGED_MESSAGE_TEXT = "'" + MESSAGE_TEXT + "'";

  private static final int DEFAULT_SECONDS_TO_PAUSE = 1;

  private static final String DELEGATING_SCRIPT = "inline:package infra.scripting;\n" +
          "class DelegatingMessenger implements Messenger {\n" +
          "  private Messenger wrappedMessenger;\n" +
          "  public String getMessage() {\n" +
          "    return this.wrappedMessenger.getMessage()\n" +
          "  }\n" +
          "  public void setMessenger(Messenger wrappedMessenger) {\n" +
          "    this.wrappedMessenger = wrappedMessenger\n" +
          "  }\n" +
          "}";

  @Test
  void configInterfaceContainsTypedSettersAndLifecycleMethods() throws Exception {
    var definition = new RootBeanDefinition();
    definition.getPropertyValues().add("message", "hello");
    definition.setInitMethodNames("initialize", "start");
    definition.setDestroyMethodNames("stop", "cleanup");
    var processor = new ScriptFactoryPostProcessor();

    Class<?> config = processor.createConfigInterface(definition, new Class<?>[] { MessageConfig.class });

    assertThat(config.isInterface()).isTrue();
    assertThat(config.getMethod("setMessage", String.class).getReturnType()).isEqualTo(void.class);
    for (String method : new String[] { "initialize", "start", "stop", "cleanup" }) {
      assertThat(config.getMethod(method).getReturnType()).isEqualTo(void.class);
    }
    assertThat(processor.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
  }

  interface MessageConfig {

    void setMessage(String message);

  }

  @Test
  void passesGeneratedConfigInterfaceToScriptFactoryAndAppliesBeanProperties() throws Exception {
    var definition = new RootBeanDefinition(ConfigurableScriptFactory.class);
    definition.getPropertyValues().add("message", "configured");
    var factory = new StandardBeanFactory();
    factory.registerBeanDefinition("messenger", definition);
    var processor = new ScriptFactoryPostProcessor();
    processor.setBeanFactory(factory);

    var messenger = (ConfigurableMessage) processor.postProcessBeforeInstantiation(ConfigurableScriptFactory.class, "messenger");

    assertThat(messenger.getMessage()).isEqualTo("configured");
    assertThat(messenger.interfaces).hasSize(3).startsWith(MessageConfig.class, Messenger.class);
    assertThat(messenger.interfaces[2].getMethod("setMessage", String.class).getReturnType()).isEqualTo(void.class);
  }

  @Test
  void combinesDeclaredInterfacesForTypePredictionAndRefreshableProxy() {
    var definition = new RootBeanDefinition(ConfigurableScriptFactory.class);
    definition.setAttribute(ScriptFactoryPostProcessor.REFRESH_CHECK_DELAY_ATTRIBUTE, 0);
    definition.getPropertyValues().add("message", "refreshable");
    var factory = new StandardBeanFactory();
    factory.registerBeanDefinition("messenger", definition);
    var processor = new ScriptFactoryPostProcessor();
    processor.setBeanFactory(factory);

    Class<?> predicted = processor.predictBeanType(ConfigurableScriptFactory.class, "messenger");
    assertThat(predicted.getInterfaces()).containsExactly(MessageConfig.class, Messenger.class);

    Object proxy = processor.postProcessBeforeInstantiation(ConfigurableScriptFactory.class, "messenger");
    assertThat(AopUtils.isJdkDynamicProxy(proxy)).isTrue();
    assertThat(proxy).isInstanceOf(MessageConfig.class).isInstanceOfSatisfying(Messenger.class,
            messenger -> assertThat(messenger.getMessage()).isEqualTo("refreshable"));
  }

  public static class ConfigurableScriptFactory extends UntypedScriptFactory {

    @Override
    public Class<?>[] getScriptInterfaces() {
      return new Class<?>[] { MessageConfig.class, Messenger.class };
    }

    @Override
    public boolean requiresConfigInterface() {
      return true;
    }

    @Override
    public Object getScriptedObject(ScriptSource scriptSource, Class<?> @Nullable ... actualInterfaces) {
      return new ConfigurableMessage(actualInterfaces);
    }

  }

  public static class ConfigurableMessage implements MessageConfig, Messenger {

    final Class<?> @Nullable [] interfaces;

    private String message = "initial";

    ConfigurableMessage(Class<?> @Nullable [] interfaces) {
      this.interfaces = interfaces;
    }

    @Override
    public String getMessage() {
      return this.message;
    }

    @Override
    public void setMessage(String message) {
      this.message = message;
    }

  }

  @Test
  void predictsUntypedSingletonFromItsInstanceButDoesNotInstantiatePrototype() {
    var singleton = new RootBeanDefinition(UntypedScriptFactory.class);
    var prototype = new RootBeanDefinition(UntypedScriptFactory.class);
    prototype.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    var factory = new StandardBeanFactory();
    factory.registerBeanDefinition("singleton", singleton);
    factory.registerBeanDefinition("prototype", prototype);
    var processor = new ScriptFactoryPostProcessor();
    processor.setBeanFactory(factory);

    assertThat(processor.predictBeanType(UntypedScriptFactory.class, "singleton")).isEqualTo(String.class);
    assertThat(processor.predictBeanType(UntypedScriptFactory.class, "prototype")).isNull();
    assertThat(processor.postProcessBeforeInstantiation(UntypedScriptFactory.class, "prototype")).isEqualTo("scripted");
  }

  @Test
  void defersScriptTypePredictionFailureUntilBeanCreation() {
    var factory = new StandardBeanFactory();
    factory.registerBeanDefinition("broken", new RootBeanDefinition(BrokenScriptFactory.class));
    var processor = new ScriptFactoryPostProcessor();
    processor.setBeanFactory(factory);

    assertThat(processor.predictBeanType(BrokenScriptFactory.class, "broken")).isNull();
    assertThatExceptionOfType(BeanCreationException.class)
            .isThrownBy(() -> processor.postProcessBeforeInstantiation(BrokenScriptFactory.class, "broken"))
            .withMessageContaining("broken", "Could not determine scripted object type")
            .withCauseInstanceOf(IOException.class);
  }

  public static class UntypedScriptFactory implements ScriptFactory {

    @Override
    public String getScriptSourceLocator() {
      return "inline:scripted";
    }

    @Override
    public Class<?> @Nullable [] getScriptInterfaces() {
      return null;
    }

    @Override
    public boolean requiresConfigInterface() {
      return false;
    }

    @Override
    public Object getScriptedObject(ScriptSource scriptSource, Class<?> @Nullable ... actualInterfaces) throws IOException {
      return scriptSource.getScriptAsString();
    }

    @Override
    public @Nullable Class<?> getScriptedObjectType(ScriptSource scriptSource) throws IOException {
      return null;
    }

    @Override
    public boolean requiresScriptedObjectRefresh(ScriptSource scriptSource) {
      return scriptSource.isModified();
    }

  }

  public static class BrokenScriptFactory extends UntypedScriptFactory {

    @Override
    public @Nullable Class<?> getScriptedObjectType(ScriptSource scriptSource) throws IOException {
      throw new IOException("script unavailable");
    }

  }

  @Test
  void testDoesNothingWhenPostProcessingNonScriptFactoryTypeBeforeInstantiation() {
    assertThat(new ScriptFactoryPostProcessor().postProcessBeforeInstantiation(getClass(), "a.bean")).isNull();
  }

  @Test
  void testThrowsExceptionIfGivenNonAbstractBeanFactoryImplementation() {
    assertThatIllegalStateException().isThrownBy(() ->
            new ScriptFactoryPostProcessor().setBeanFactory(mock(BeanFactory.class)));
  }

  @Test
  void createsAClassBasedRefreshableProxyForGroovy() {
    try (var context = new GenericApplicationContext()) {
      var definition = createScriptedGroovyBean();
      definition.setAttribute(ScriptFactoryPostProcessor.REFRESH_CHECK_DELAY_ATTRIBUTE, "0");
      definition.setAttribute(ScriptFactoryPostProcessor.PROXY_TARGET_CLASS_ATTRIBUTE, "true");
      definition.setAttribute(ScriptFactoryPostProcessor.LANGUAGE_ATTRIBUTE, "groovy");
      context.registerBeanDefinition(PROCESSOR_BEAN_NAME, createScriptFactoryPostProcessor(false));
      context.registerBeanDefinition(MESSENGER_BEAN_NAME, definition);

      context.refresh();

      var messenger = context.getBean(MESSENGER_BEAN_NAME, Messenger.class);
      assertThat(AopUtils.isCglibProxy(messenger)).isTrue();
      assertThat(messenger.getMessage()).isEqualTo(MESSAGE_TEXT);
    }
  }

  @Test
  void rejectsAClassBasedRefreshableProxyForAnUnsupportedLanguage() {
    try (var context = new GenericApplicationContext()) {
      var definition = createScriptedGroovyBean();
      definition.setAttribute(ScriptFactoryPostProcessor.PROXY_TARGET_CLASS_ATTRIBUTE, true);
      definition.setAttribute(ScriptFactoryPostProcessor.LANGUAGE_ATTRIBUTE, "javascript");
      context.registerBeanDefinition(PROCESSOR_BEAN_NAME, createScriptFactoryPostProcessor(true));
      context.registerBeanDefinition(MESSENGER_BEAN_NAME, definition);

      assertThatExceptionOfType(BeanCreationException.class).isThrownBy(context::refresh)
              .havingRootCause().isInstanceOf(BeanDefinitionValidationException.class)
              .withMessageContaining("language is not 'groovy'", "javascript");
    }
  }

  @Test
  void rejectsMetadataValuesOfUnsupportedTypes() {
    var definition = createScriptedGroovyBean();
    var processor = new ScriptFactoryPostProcessor();
    definition.setAttribute(ScriptFactoryPostProcessor.REFRESH_CHECK_DELAY_ATTRIBUTE, true);
    definition.setAttribute(ScriptFactoryPostProcessor.PROXY_TARGET_CLASS_ATTRIBUTE, 1);

    assertThatExceptionOfType(BeanDefinitionStoreException.class)
            .isThrownBy(() -> processor.resolveRefreshCheckDelay(definition))
            .withMessageContaining("needs to be of type Number or String");
    assertThatExceptionOfType(BeanDefinitionStoreException.class)
            .isThrownBy(() -> processor.resolveProxyTargetClass(definition))
            .withMessageContaining("needs to be of type Boolean or String");
  }

  @Test
  void testChangeScriptWithRefreshableBeanFunctionality() throws Exception {
    BeanDefinition processorBeanDefinition = createScriptFactoryPostProcessor(true);
    BeanDefinition scriptedBeanDefinition = createScriptedGroovyBean();

    GenericApplicationContext ctx = new GenericApplicationContext();
    ctx.registerBeanDefinition(PROCESSOR_BEAN_NAME, processorBeanDefinition);
    ctx.registerBeanDefinition(MESSENGER_BEAN_NAME, scriptedBeanDefinition);
    ctx.refresh();

    Messenger messenger = (Messenger) ctx.getBean(MESSENGER_BEAN_NAME);
    assertThat(messenger.getMessage()).isEqualTo(MESSAGE_TEXT);
    // cool; now let's change the script and check the refresh behaviour...
    pauseToLetRefreshDelayKickIn(DEFAULT_SECONDS_TO_PAUSE);
    StaticScriptSource source = getScriptSource(ctx);
    source.setScript(CHANGED_SCRIPT);
    Messenger refreshedMessenger = (Messenger) ctx.getBean(MESSENGER_BEAN_NAME);
    // the updated script surrounds the message in quotes before returning...
    assertThat(refreshedMessenger.getMessage()).isEqualTo(EXPECTED_CHANGED_MESSAGE_TEXT);
  }

  @Test
  void testChangeScriptWithNoRefreshableBeanFunctionality() throws Exception {
    BeanDefinition processorBeanDefinition = createScriptFactoryPostProcessor(false);
    BeanDefinition scriptedBeanDefinition = createScriptedGroovyBean();

    GenericApplicationContext ctx = new GenericApplicationContext();
    ctx.registerBeanDefinition(PROCESSOR_BEAN_NAME, processorBeanDefinition);
    ctx.registerBeanDefinition(MESSENGER_BEAN_NAME, scriptedBeanDefinition);
    ctx.refresh();

    Messenger messenger = (Messenger) ctx.getBean(MESSENGER_BEAN_NAME);
    assertThat(messenger.getMessage()).isEqualTo(MESSAGE_TEXT);
    // cool; now let's change the script and check the refresh behaviour...
    pauseToLetRefreshDelayKickIn(DEFAULT_SECONDS_TO_PAUSE);
    StaticScriptSource source = getScriptSource(ctx);
    source.setScript(CHANGED_SCRIPT);
    Messenger refreshedMessenger = (Messenger) ctx.getBean(MESSENGER_BEAN_NAME);
    assertThat(refreshedMessenger.getMessage()).as("Script seems to have been refreshed (must not be as no refreshCheckDelay set on ScriptFactoryPostProcessor)").isEqualTo(MESSAGE_TEXT);
  }

  @Test
  void testRefreshedScriptReferencePropagatesToCollaborators() throws Exception {
    BeanDefinition processorBeanDefinition = createScriptFactoryPostProcessor(true);
    BeanDefinition scriptedBeanDefinition = createScriptedGroovyBean();
    BeanDefinitionBuilder collaboratorBuilder = BeanDefinitionBuilder.rootBeanDefinition(DefaultMessengerService.class);
    collaboratorBuilder.addPropertyReference(MESSENGER_BEAN_NAME, MESSENGER_BEAN_NAME);

    GenericApplicationContext ctx = new GenericApplicationContext();
    ctx.registerBeanDefinition(PROCESSOR_BEAN_NAME, processorBeanDefinition);
    ctx.registerBeanDefinition(MESSENGER_BEAN_NAME, scriptedBeanDefinition);
    final String collaboratorBeanName = "collaborator";
    ctx.registerBeanDefinition(collaboratorBeanName, collaboratorBuilder.getBeanDefinition());
    ctx.refresh();

    Messenger messenger = (Messenger) ctx.getBean(MESSENGER_BEAN_NAME);
    assertThat(messenger.getMessage()).isEqualTo(MESSAGE_TEXT);
    // cool; now let's change the script and check the refresh behaviour...
    pauseToLetRefreshDelayKickIn(DEFAULT_SECONDS_TO_PAUSE);
    StaticScriptSource source = getScriptSource(ctx);
    source.setScript(CHANGED_SCRIPT);
    Messenger refreshedMessenger = (Messenger) ctx.getBean(MESSENGER_BEAN_NAME);
    // the updated script surrounds the message in quotes before returning...
    assertThat(refreshedMessenger.getMessage()).isEqualTo(EXPECTED_CHANGED_MESSAGE_TEXT);
    // ok, is this change reflected in the reference that the collaborator has?
    DefaultMessengerService collaborator = (DefaultMessengerService) ctx.getBean(collaboratorBeanName);
    assertThat(collaborator.getMessage()).isEqualTo(EXPECTED_CHANGED_MESSAGE_TEXT);
  }

  @Test
  void testReferencesAcrossAContainerHierarchy() throws Exception {
    GenericApplicationContext businessContext = new GenericApplicationContext();
    businessContext.registerBeanDefinition("messenger", BeanDefinitionBuilder.rootBeanDefinition(StubMessenger.class).getBeanDefinition());
    businessContext.refresh();

    BeanDefinitionBuilder scriptedBeanBuilder = BeanDefinitionBuilder.rootBeanDefinition(GroovyScriptFactory.class);
    scriptedBeanBuilder.addConstructorArgValue(DELEGATING_SCRIPT);
    scriptedBeanBuilder.addPropertyReference("messenger", "messenger");

    GenericApplicationContext presentationCtx = new GenericApplicationContext(businessContext);
    presentationCtx.registerBeanDefinition("needsMessenger", scriptedBeanBuilder.getBeanDefinition());
    presentationCtx.registerBeanDefinition("scriptProcessor", createScriptFactoryPostProcessor(true));
    presentationCtx.refresh();
  }

  @Test
  void testScriptHavingAReferenceToAnotherBean() throws Exception {
    // just tests that the (singleton) script-backed bean is able to be instantiated with references to its collaborators
    new ClassPathXmlApplicationContext("infra/scripting/support/groovyReferences.xml");
  }

  @Test
  void testForRefreshedScriptHavingErrorPickedUpOnFirstCall() throws Exception {
    BeanDefinition processorBeanDefinition = createScriptFactoryPostProcessor(true);
    BeanDefinition scriptedBeanDefinition = createScriptedGroovyBean();
    BeanDefinitionBuilder collaboratorBuilder = BeanDefinitionBuilder.rootBeanDefinition(DefaultMessengerService.class);
    collaboratorBuilder.addPropertyReference(MESSENGER_BEAN_NAME, MESSENGER_BEAN_NAME);

    GenericApplicationContext ctx = new GenericApplicationContext();
    ctx.registerBeanDefinition(PROCESSOR_BEAN_NAME, processorBeanDefinition);
    ctx.registerBeanDefinition(MESSENGER_BEAN_NAME, scriptedBeanDefinition);
    final String collaboratorBeanName = "collaborator";
    ctx.registerBeanDefinition(collaboratorBeanName, collaboratorBuilder.getBeanDefinition());
    ctx.refresh();

    Messenger messenger = (Messenger) ctx.getBean(MESSENGER_BEAN_NAME);
    assertThat(messenger.getMessage()).isEqualTo(MESSAGE_TEXT);
    // cool; now let's change the script and check the refresh behaviour...
    pauseToLetRefreshDelayKickIn(DEFAULT_SECONDS_TO_PAUSE);
    StaticScriptSource source = getScriptSource(ctx);
    // needs The Sundays compiler; must NOT throw any exception here...
    source.setScript("I keep hoping you are the same as me, and I'll send you letters and come to your house for tea");
    Messenger refreshedMessenger = (Messenger) ctx.getBean(MESSENGER_BEAN_NAME);
    assertThatExceptionOfType(FatalBeanException.class).isThrownBy(refreshedMessenger::getMessage)
            .matches(ex -> ex.contains(ScriptCompilationException.class));
  }

  @Test
  void testPrototypeScriptedBean() throws Exception {
    GenericApplicationContext ctx = new GenericApplicationContext();
    ctx.registerBeanDefinition("messenger", BeanDefinitionBuilder.rootBeanDefinition(StubMessenger.class).getBeanDefinition());

    BeanDefinitionBuilder scriptedBeanBuilder = BeanDefinitionBuilder.rootBeanDefinition(GroovyScriptFactory.class);
    scriptedBeanBuilder.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    scriptedBeanBuilder.addConstructorArgValue(DELEGATING_SCRIPT);
    scriptedBeanBuilder.addPropertyReference("messenger", "messenger");

    final String BEAN_WITH_DEPENDENCY_NAME = "needsMessenger";
    ctx.registerBeanDefinition(BEAN_WITH_DEPENDENCY_NAME, scriptedBeanBuilder.getBeanDefinition());
    ctx.registerBeanDefinition("scriptProcessor", createScriptFactoryPostProcessor(true));
    ctx.refresh();

    Messenger messenger1 = (Messenger) ctx.getBean(BEAN_WITH_DEPENDENCY_NAME);
    Messenger messenger2 = (Messenger) ctx.getBean(BEAN_WITH_DEPENDENCY_NAME);
    assertThat(messenger2).isNotSameAs(messenger1);
  }

  private static StaticScriptSource getScriptSource(GenericApplicationContext ctx) throws Exception {
    ScriptFactoryPostProcessor processor = (ScriptFactoryPostProcessor) ctx.getBean(PROCESSOR_BEAN_NAME);
    BeanDefinition bd = processor.scriptBeanFactory.getBeanDefinition("scriptedObject.messenger");
    return (StaticScriptSource) bd.getConstructorArgumentValues().getIndexedArgumentValue(0, StaticScriptSource.class).getValue();
  }

  private static BeanDefinition createScriptFactoryPostProcessor(boolean isRefreshable) {
    BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(ScriptFactoryPostProcessor.class);
    if (isRefreshable) {
      builder.addPropertyValue("defaultRefreshCheckDelay", 1L);
    }
    return builder.getBeanDefinition();
  }

  private static BeanDefinition createScriptedGroovyBean() {
    BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(GroovyScriptFactory.class);
    builder.addConstructorArgValue("inline:package infra.scripting;\n" +
            "class GroovyMessenger implements Messenger {\n" +
            "  private String message = \"Bingo\"\n" +
            "  public String getMessage() {\n" +
            "    return this.message\n" +
            "  }\n" +
            "  public void setMessage(String message) {\n" +
            "    this.message = message\n" +
            "  }\n" +
            "}");
    builder.addPropertyValue("message", MESSAGE_TEXT);
    return builder.getBeanDefinition();
  }

  private static void pauseToLetRefreshDelayKickIn(int secondsToPause) {
    try {
      Thread.sleep(secondsToPause * 1000);
    }
    catch (InterruptedException ignored) {
    }
  }

  public static class DefaultMessengerService {

    private Messenger messenger;

    public void setMessenger(Messenger messenger) {
      this.messenger = messenger;
    }

    public String getMessage() {
      return this.messenger.getMessage();
    }
  }

}
