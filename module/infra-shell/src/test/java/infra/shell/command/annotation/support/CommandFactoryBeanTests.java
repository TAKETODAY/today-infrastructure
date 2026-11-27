/*
 * Copyright 2026-present the original author or authors.
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

package infra.shell.command.annotation.support;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.context.ApplicationContext;
import infra.core.TypeDescriptor;
import infra.core.conversion.Converter;
import infra.core.conversion.ConverterFactory;
import infra.core.conversion.GenericConverter;
import infra.core.conversion.support.ConfigurableConversionService;
import infra.core.env.Environment;
import infra.shell.command.CommandArgument;
import infra.shell.command.CommandContext;
import infra.shell.command.CommandOption;
import infra.shell.command.adapter.MethodInvokerCommandAdapter;
import infra.shell.command.annotation.Argument;
import infra.shell.command.annotation.Arguments;
import infra.shell.command.annotation.Command;
import infra.shell.command.annotation.CommandGroup;
import infra.shell.command.annotation.Option;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author David Pilar
 */
class CommandFactoryBeanTests {

  private static ApplicationContext mockApplicationContext() {
    return mockApplicationContext("");
  }

  private static ApplicationContext mockApplicationContext(String globalPrefix) {
    ApplicationContext context = mock(ApplicationContext.class);
    when(context.getBeansOfType(Converter.class)).thenReturn(Collections.emptyMap());
    when(context.getBeansOfType(GenericConverter.class)).thenReturn(Collections.emptyMap());
    when(context.getBeansOfType(ConverterFactory.class)).thenReturn(Collections.emptyMap());
    Environment environment = mock(Environment.class);
    when(environment.getProperty("shell.command.prefix", "")).thenReturn(globalPrefix);
    when(context.getEnvironment()).thenReturn(environment);
    return context;
  }

  @Test
  void testOptionNames() {
    // given
    ApplicationContext context = mockApplicationContext();
    when(context.getBean(TestClass.class)).thenReturn(new TestClass());
    CommandFactoryBean commandFactoryBean = new CommandFactoryBean(TestClass.class.getDeclaredMethods()[0]);
    commandFactoryBean.setApplicationContext(context);

    // when
    infra.shell.command.Command result = commandFactoryBean.getObject();

    // then
    assertThat(result.getName()).isEqualTo("hello");
    List<CommandOption> options = result.getOptions();
    assertThat(options.size()).isEqualTo(4);

    assertThat(options.get(0).longName()).isEqualTo("myOption1");
    assertThat(options.get(0).shortName()).isEqualTo(' ');

    assertThat(options.get(1).longName()).isEqualTo("myOption2");
    assertThat(options.get(1).shortName()).isEqualTo('m');

    assertThat(options.get(2).longName()).isEqualTo("longNameOption3");
    assertThat(options.get(2).shortName()).isEqualTo('l');

    assertThat(options.get(3).longName()).isEqualTo("longNameOption4");
    assertThat(options.get(3).shortName()).isEqualTo(' ');
  }

  @Test
  void testVariadicArgumentsMetadata() {
    // given
    infra.shell.command.Command result = buildCommand("collect");

    // then
    List<CommandArgument> arguments = result.getArguments();
    assertThat(arguments.size()).isEqualTo(1);
    CommandArgument argument = arguments.get(0);
    assertThat(argument.index()).isEqualTo(0);
    assertThat(argument.description()).isEqualTo("the values to collect");
    assertThat(argument.type()).isEqualTo(Integer.class);
    assertThat(argument.variadic()).isTrue();
  }

  @Test
  void testVariadicArgumentsMetadataForArray() {
    // given
    infra.shell.command.Command result = buildCommand("collectArray");

    // then
    List<CommandArgument> arguments = result.getArguments();
    assertThat(arguments.size()).isEqualTo(1);
    assertThat(arguments.get(0).type()).isEqualTo(String.class);
    assertThat(arguments.get(0).variadic()).isTrue();
  }

  @Test
  void testVariadicArgumentsFollowingAnIndexedArgument() {
    // given
    infra.shell.command.Command result = buildCommand("mixed");

    // then
    List<CommandArgument> arguments = result.getArguments();
    assertThat(arguments.size()).isEqualTo(2);
    assertThat(arguments.get(0).index()).isEqualTo(0);
    assertThat(arguments.get(0).variadic()).isFalse();
    assertThat(arguments.get(1).index()).isEqualTo(1);
    assertThat(arguments.get(1).description()).isEqualTo("the rest");
    assertThat(arguments.get(1).type()).isEqualTo(String.class);
    assertThat(arguments.get(1).variadic()).isTrue();
  }

  private static infra.shell.command.Command buildCommand(String methodName) {
    ApplicationContext context = mockApplicationContext();
    when(context.getBean(ArgumentCommands.class)).thenReturn(new ArgumentCommands());
    Method method = Arrays.stream(ArgumentCommands.class.getDeclaredMethods())
            .filter(m -> m.getName().equals(methodName))
            .findFirst()
            .orElseThrow();
    CommandFactoryBean commandFactoryBean = new CommandFactoryBean(method);
    commandFactoryBean.setApplicationContext(context);
    return commandFactoryBean.getObject();
  }

  @Test
  public void testCommandGroup() {
    // given
    ApplicationContext context = mockApplicationContext();
    when(context.getBean(GreetingCommands.class)).thenReturn(new GreetingCommands());
    Method[] declaredMethods = GreetingCommands.class.getDeclaredMethods();
    Method hiMethod = Arrays.stream(declaredMethods)
            .filter(m -> m.getName().equals("hi"))
            .findFirst()
            .orElseThrow();
    CommandFactoryBean commandFactoryBean = new CommandFactoryBean(hiMethod);
    commandFactoryBean.setApplicationContext(context);

    // when
    infra.shell.command.Command result = commandFactoryBean.getObject();

    // then
    assertThat(result.getName()).isEqualTo("greeting hi");
    assertThat(result.getGroup()).isEqualTo("Greeting Commands");
  }

  @Test
  public void testCommandGroupOverride() {
    // given
    ApplicationContext context = mockApplicationContext();
    when(context.getBean(GreetingCommands.class)).thenReturn(new GreetingCommands());
    Method[] declaredMethods = GreetingCommands.class.getDeclaredMethods();
    Method byeMethod = Arrays.stream(declaredMethods)
            .filter(m -> m.getName().equals("bye"))
            .findFirst()
            .orElseThrow();
    CommandFactoryBean commandFactoryBean = new CommandFactoryBean(byeMethod);
    commandFactoryBean.setApplicationContext(context);

    // when
    infra.shell.command.Command result = commandFactoryBean.getObject();

    // then
    assertThat(result.getName()).isEqualTo("greeting bye");
    assertThat(result.getGroup()).isEqualTo("Farewell Commands");
  }

  @Test
  void converterBeansAreAppliedToConfigurableConversionService() throws Exception {
    ApplicationContext context = mockConverterApplicationContext();
    doReturn(Map.of("messageConverter", new MessageConverter())).when(context).getBeansOfType(Converter.class);

    runConvertCommandAndAssert(context);
  }

  @Test
  void genericConverterBeansAreAppliedToConfigurableConversionService() throws Exception {
    ApplicationContext context = mockConverterApplicationContext();
    doReturn(Map.of("messageGenericConverter", new MessageGenericConverter())).when(context)
            .getBeansOfType(GenericConverter.class);

    runConvertCommandAndAssert(context);
  }

  @Test
  void converterFactoryBeansAreAppliedToConfigurableConversionService() throws Exception {
    ApplicationContext context = mockConverterApplicationContext();
    doReturn(Map.of("messageConverterFactory", new MessageConverterFactory())).when(context)
            .getBeansOfType(ConverterFactory.class);

    runConvertCommandAndAssert(context);
  }

  private static ApplicationContext mockConverterApplicationContext() {
    ApplicationContext context = mockApplicationContext();
    when(context.getBean(ConverterTarget.class)).thenReturn(new ConverterTarget());
    when(context.getBean(ConfigurableConversionService.class))
            .thenThrow(new NoSuchBeanDefinitionException(ConfigurableConversionService.class));
    when(context.getBean(jakarta.validation.Validator.class))
            .thenThrow(new NoSuchBeanDefinitionException(jakarta.validation.Validator.class));
    return context;
  }

  private static void runConvertCommandAndAssert(ApplicationContext context) throws Exception {
    Method method = Arrays.stream(ConverterTarget.class.getDeclaredMethods())
            .filter(m -> m.getName().equals("run"))
            .findFirst()
            .orElseThrow();
    CommandFactoryBean factory = new CommandFactoryBean(method);
    factory.setApplicationContext(context);
    MethodInvokerCommandAdapter command = (MethodInvokerCommandAdapter) factory.getObject();

    ConverterTarget.lastSeen = null;
    CommandContext ctx = mock(CommandContext.class);
    when(ctx.getOptionByLongName("msg")).thenReturn(CommandOption.with().longName("msg").value("hello").build());
    when(ctx.outputWriter()).thenReturn(new java.io.PrintWriter(new java.io.StringWriter()));
    command.doExecute(ctx);

    assertThat(ConverterTarget.lastSeen).isNotNull();
    assertThat(ConverterTarget.lastSeen.text).isEqualTo("hello");
  }

  @Test
  void globalPrefixAppliedWhenNoCommandGroupPrefix() throws Exception {
    ApplicationContext context = mockApplicationContext("app");
    when(context.getBean(NoGroupCommands.class)).thenReturn(new NoGroupCommands());
    Method method = Arrays.stream(NoGroupCommands.class.getDeclaredMethods())
            .filter(m -> m.getName().equals("ping"))
            .findFirst()
            .orElseThrow();
    CommandFactoryBean factory = new CommandFactoryBean(method);
    factory.setApplicationContext(context);

    infra.shell.command.Command result = factory.getObject();

    assertThat(result.getName()).isEqualTo("app ping");
  }

  @Test
  void globalAndCommandGroupPrefixesAreChained() throws Exception {
    ApplicationContext context = mockApplicationContext("shell");
    when(context.getBean(GreetingCommands.class)).thenReturn(new GreetingCommands());
    Method method = Arrays.stream(GreetingCommands.class.getDeclaredMethods())
            .filter(m -> m.getName().equals("hi"))
            .findFirst()
            .orElseThrow();
    CommandFactoryBean factory = new CommandFactoryBean(method);
    factory.setApplicationContext(context);

    infra.shell.command.Command result = factory.getObject();

    assertThat(result.getName()).isEqualTo("shell greeting hi");
  }

  @Test
  void noPrefixesLeaveNameUnchanged() throws Exception {
    ApplicationContext context = mockApplicationContext("");
    when(context.getBean(NoGroupCommands.class)).thenReturn(new NoGroupCommands());
    Method method = Arrays.stream(NoGroupCommands.class.getDeclaredMethods())
            .filter(m -> m.getName().equals("ping"))
            .findFirst()
            .orElseThrow();
    CommandFactoryBean factory = new CommandFactoryBean(method);
    factory.setApplicationContext(context);

    infra.shell.command.Command result = factory.getObject();

    assertThat(result.getName()).isEqualTo("ping");
  }

  @Test
  void globalPrefixIsChainedWithMethodNameFallback() throws Exception {
    ApplicationContext context = mockApplicationContext("app");
    when(context.getBean(NoGroupCommands.class)).thenReturn(new NoGroupCommands());
    Method method = Arrays.stream(NoGroupCommands.class.getDeclaredMethods())
            .filter(m -> m.getName().equals("unnamed"))
            .findFirst()
            .orElseThrow();
    CommandFactoryBean factory = new CommandFactoryBean(method);
    factory.setApplicationContext(context);

    infra.shell.command.Command result = factory.getObject();

    assertThat(result.getName()).isEqualTo("app unnamed");
  }

  static class NoGroupCommands {

    @Command(name = "ping")
    public void ping() {
    }

    @Command
    public void unnamed() {
    }

  }

  static class Message {

    String text;

  }

  static class MessageConverter implements Converter<String, Message> {

    @Override
    public Message convert(String source) {
      Message m = new Message();
      m.text = source;
      return m;
    }

  }

  static class MessageGenericConverter implements GenericConverter {

    @Override
    public java.util.Set<GenericConverter.ConvertiblePair> getConvertibleTypes() {
      return java.util.Set.of(new GenericConverter.ConvertiblePair(String.class, Message.class));
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
      Message m = new Message();
      m.text = (String) source;
      return m;
    }

  }

  static class MessageConverterFactory implements ConverterFactory<String, Message> {

    @Override
    public <T extends Message> Converter<String, T> getConverter(Class<T> targetType) {
      return source -> {
        try {
          T m = targetType.getDeclaredConstructor().newInstance();
          m.text = source;
          return m;
        }
        catch (ReflectiveOperationException e) {
          throw new IllegalStateException(e);
        }
      };
    }

  }

  static class ConverterTarget {

    static Message lastSeen;

    @Command(name = "convert")
    public void run(@Option(longName = "msg", required = true) Message msg) {
      lastSeen = msg;
    }

  }

  static class TestClass {

    @Command(name = "hello")
    public void helloMethod(@Option String myOption1, @Option(shortName = 'm') String myOption2,
            @Option(longName = "longNameOption3", shortName = 'l') String myOption3,
            @Option(longName = "longNameOption4") String myOption4) {
      // no-op
    }

  }

  static class ArgumentCommands {

    @Command(name = "collect")
    public void collect(@Arguments(description = "the values to collect") List<Integer> values,
            @Option String name) {
      // no-op
    }

    @Command(name = "collectArray")
    public void collectArray(@Arguments String[] values) {
      // no-op
    }

    @Command(name = "mixed")
    public void mixed(@Argument(index = 0, description = "the target") String target,
            @Arguments(description = "the rest") List<String> rest) {
      // no-op
    }

  }

  @CommandGroup(name = "Greeting Commands", prefix = "greeting")
  static class GreetingCommands {

    @Command(name = "hi")
    public void hi() {
      // no-op
    }

    @Command(name = "bye", group = "Farewell Commands")
    public void bye() {
      // no-op
    }

  }

}
