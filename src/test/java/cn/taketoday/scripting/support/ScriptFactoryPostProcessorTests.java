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
package cn.taketoday.scripting.support;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionBuilder;
import cn.taketoday.core.type.EnabledForTestGroups;
import cn.taketoday.scripting.Messenger;
import cn.taketoday.scripting.ScriptCompilationException;
import cn.taketoday.scripting.groovy.GroovyScriptFactory;

import static cn.taketoday.core.type.TestGroup.LONG_RUNNING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Chris Beams
 */
@EnabledForTestGroups(LONG_RUNNING)
class ScriptFactoryPostProcessorTests {

  private static final String MESSAGE_TEXT = "Bingo";

  private static final String MESSENGER_BEAN_NAME = "messenger";

  private static final String PROCESSOR_BEAN_NAME = "processor";

  private static final String CHANGED_SCRIPT = "package cn.taketoday.scripting.groovy\n" +
          "import cn.taketoday.scripting.Messenger\n" +
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

  private static final String DELEGATING_SCRIPT = "inline:package cn.taketoday.scripting;\n" +
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
  void testDoesNothingWhenPostProcessingNonScriptFactoryTypeBeforeInstantiation() {
    assertThat(new ScriptFactoryPostProcessor().postProcessBeforeInstantiation(getClass(), "a.bean")).isNull();
  }

  @Test
  void testThrowsExceptionIfGivenNonAbstractBeanFactoryImplementation() {
    assertThatIllegalStateException().isThrownBy(() ->
            new ScriptFactoryPostProcessor().setBeanFactory(mock(BeanFactory.class)));
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
    new ClassPathXmlApplicationContext("org/springframework/scripting/support/groovyReferences.xml");
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
    builder.addConstructorArgValue("inline:package cn.taketoday.scripting;\n" +
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
