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

package infra.web.view.freemarker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import infra.beans.testfixture.beans.TestBean;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.support.StaticMessageSource;
import infra.core.io.ClassPathResource;
import infra.util.FileCopyUtils;
import infra.util.StringUtils;
import infra.web.AbstractDelegatingServerHttpExchange;
import infra.web.ServerHttpExchange;
import infra.web.i18n.AcceptHeaderLocaleResolver;
import infra.web.mock.MockServerHttpExchange;
import infra.web.mock.MockRequest;
import infra.web.mock.MockResponse;
import infra.web.view.BindStatus;
import infra.web.view.BindingAccessor;
import infra.web.view.ViewRenderingException;

import static infra.freemarker.config.FreeMarkerProperties.DEFAULT_HTTP_EXCHANGE_ATTRIBUTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Darren Davison
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 25.01.2005
 */
class FreeMarkerMacroTests {

  private static final String TEMPLATE_FILE = "infra/web/view/freemarker/test.ftl";

  private final AnnotationConfigApplicationContext wac = new AnnotationConfigApplicationContext();

  private final MockRequest request = new MockRequest();

  private final MockResponse response = new MockResponse();

  private final MockServerHttpExchange context = new MockServerHttpExchange(wac, request, response);

  private final FreeMarkerConfigurer fc = new FreeMarkerConfigurer();

  @TempDir
  private Path templateLoaderPath;

  @BeforeEach
  void setUp() throws Exception {
    fc.setTemplateLoaderPaths("classpath:/", "file://" + this.templateLoaderPath);
    fc.afterPropertiesSet();

    wac.getBeanFactory().registerSingleton("freeMarkerConfigurer", fc);
    StaticMessageSource messages = new StaticMessageSource();
    messages.addMessage("hello", Locale.ENGLISH, "Howdy");
    messages.addMessage("world", Locale.ENGLISH, "Mundo");
    messages.addMessage("helloArgs", Locale.ENGLISH, "Howdy[{0}]");
    wac.getBeanFactory().registerSingleton("messageSource", messages);
    wac.refresh();
    request.addPreferredLocale(Locale.ENGLISH);

    wac.registerSingleton(AcceptHeaderLocaleResolver.BEAN_NAME, new AcceptHeaderLocaleResolver());
  }

  @Test
  void testExposeInfraMacroHelpers() throws Exception {
    FreeMarkerView fv = new FreeMarkerView() {

      @Override
      protected void processTemplate(Template template, SimpleHash fmModel, ServerHttpExchange response) throws IOException, TemplateException {
        Map model = fmModel.toMap();
        assertThat(model.get(DEFAULT_HTTP_EXCHANGE_ATTRIBUTE)).isInstanceOf(ServerHttpExchange.class);
        BindingAccessor bindings = (BindingAccessor) model.get(BINDINGS_ATTRIBUTE);
        BindStatus status = bindings.getBindStatus("tb.name");
        assertThat(status.getExpression()).isEqualTo("name");
        assertThat(status.getValue()).isEqualTo("juergen");
      }
    };
    fv.setUrl(TEMPLATE_FILE);
    fv.setApplicationContext(wac);
    fv.setExchangeAttribute(DEFAULT_HTTP_EXCHANGE_ATTRIBUTE);

    Map<String, Object> model = new HashMap<>();
    model.put("tb", new TestBean("juergen", 99));
    fv.render(model, context);
  }

  @Test
  void testName() throws Exception {
    assertThat(getMacroOutput("NAME")).isEqualTo("Darren");
  }

  @Test
  void bindingValuesAndErrorsEscapeExactlyOnceWithMessagesDisabled() throws Exception {
    storeTemplateInTempDir("<#outputformat 'HTML'><@infra.formInput 'bean.name'/>|"
            + "<@infra.bind 'bean.name'/>${infra.status.errorMessage}</#outputformat>");
    var errors = new infra.validation.BeanPropertyBindingResult(new TestBean("actual"), "bean");
    errors.addError(new infra.validation.FieldError("bean", "name", "<Tom & Sue>", false,
            new String[] { "invalid" }, null, "fallback"));
    wac.getBean("messageSource", StaticMessageSource.class)
            .addMessage("invalid", Locale.GERMANY, "<Fehler & Text>");
    FreeMarkerView view = new FreeMarkerView();
    view.setConfiguration(fc.getConfiguration());
    view.setUrl("tmp.ftl");
    view.setExposeMessages(false);
    view.setLocale(Locale.GERMANY);
    view.render(Map.of(infra.validation.BindingResult.MODEL_KEY_PREFIX + "bean", errors), context);
    assertThat(getOutput()).contains("value=\"&lt;Tom &amp; Sue&gt;\"", "|&lt;Fehler &amp; Text&gt;");
    assertThat(context.getAttribute("bindings")).isNull();
    assertThat(context.getAttribute("messages")).isNull();
  }

  @Test
  void bindEscapesValueAndDisplayValueAtHtmlOutput() throws Exception {
    var bean = new TestBean("<Tom & \"Sue\"> &lt;tag&gt;");

    assertThat(renderMessages("<@infra.bind 'bean.name'/>${infra.status.value}|${infra.status.displayValue}",
            Map.of("bean", bean), null))
            .isEqualTo("&lt;Tom &amp; &quot;Sue&quot;&gt; &amp;lt;tag&amp;gt;|"
                    + "&lt;Tom &amp; &quot;Sue&quot;&gt; &amp;lt;tag&amp;gt;");
    assertThat(bean.getName()).isEqualTo("<Tom & \"Sue\"> &lt;tag&gt;");
  }

  @Test
  void bindEscapesRejectedValueAndFormattedErrorWithoutChangingBindingResult() throws Exception {
    var bean = new TestBean("actual");
    var errors = new infra.validation.BeanPropertyBindingResult(bean, "bean");
    String rejectedValue = "<rejected & \"value\"> &lt;";
    errors.addError(new infra.validation.FieldError("bean", "name", rejectedValue, false,
            new String[] { "invalid" }, new Object[] { "<argument & text>" }, "fallback"));
    wac.getBean("messageSource", StaticMessageSource.class)
            .addMessage("invalid", Locale.GERMANY, "<Fehler> {0} &lt;");

    assertThat(renderMessages("<@infra.bind 'bean.name'/>${infra.status.value}|"
                    + "${infra.status.errorMessage}|<#list infra.status.errorMessages as error>${error}</#list>",
            Map.of(infra.validation.BindingResult.MODEL_KEY_PREFIX + "bean", errors), Locale.GERMANY))
            .isEqualTo("&lt;rejected &amp; &quot;value&quot;&gt; &amp;lt;|"
                    + "&lt;Fehler&gt; &lt;argument &amp; text&gt; &amp;lt;|"
                    + "&lt;Fehler&gt; &lt;argument &amp; text&gt; &amp;lt;");
    assertThat(errors.getFieldValue("name")).isEqualTo(rejectedValue);
    assertThat(bean.getName()).isEqualTo("actual");
    assertThat(errors.getFieldError("name").getDefaultMessage()).isEqualTo("fallback");
  }

  @Test
  void boundFormMacrosEscapeMarkupAndCompareUnescapedOptionValues() throws Exception {
    String value = "</textarea><script>\"Tom & Sue\"</script>";
    String escaped = "&lt;/textarea&gt;&lt;script&gt;&quot;Tom &amp; Sue&quot;&lt;/script&gt;";

    String output = renderMessages("<@infra.formInput 'bean.name'/>"
                    + "<@infra.formTextarea 'bean.name'/>"
                    + "<@infra.formSingleSelect 'bean.name', options/>",
            Map.of("bean", new TestBean(value), "options", Map.of(value, value)), null);

    assertThat(output).contains("value=\"" + escaped + "\"",
                    "\n" + escaped + "</textarea>")
            .doesNotContain("<script>", "&amp;lt;/textarea");
    assertThat(output.replaceAll("\\s+", " "))
            .contains("<option value=\"" + escaped + "\" selected=\"selected\" >" + escaped + "</option>");
  }

  @Test
  void bindLeavesValuesAndErrorsUnescapedInPlainTextOutput() throws Exception {
    var errors = new infra.validation.BeanPropertyBindingResult(new TestBean("actual"), "bean");
    errors.addError(new infra.validation.FieldError("bean", "name", "<Tom & Sue> &lt;", false,
            new String[] { "missing" }, null, "<invalid & value>"));

    assertThat(renderMessages("<#outputformat 'plainText'><@infra.bind 'bean.name'/>"
                    + "${infra.status.value}|${infra.status.displayValue}|${infra.status.errorMessage}</#outputformat>",
            Map.of(infra.validation.BindingResult.MODEL_KEY_PREFIX + "bean", errors), null))
            .isEqualTo("<Tom & Sue> &lt;|<Tom & Sue> &lt;|<invalid & value>");
  }

  @Test
  void disabledBindingExposureLeavesNameAvailable() throws Exception {
    storeTemplateInTempDir("${bindings}");
    FreeMarkerView view = new FreeMarkerView();
    view.setConfiguration(fc.getConfiguration());
    view.setUrl("tmp.ftl");
    view.setExposeBindings(false);
    context.setAttribute("bindings", "request");
    view.render(Map.of("bindings", "business"), context);
    assertThat(getOutput()).isEqualTo("business");
    assertThat(context.getAttribute("bindings")).isEqualTo("business");
  }

  @Test
  void bindingsNameIsReserved() {
    assertThatExceptionOfType(ViewRenderingException.class)
            .isThrownBy(() -> renderMessages("unused", Map.of("bindings", "business"), null));
  }

  @Test
  void testMessage() throws Exception {
    assertThat(getMacroOutput("MESSAGE")).isEqualTo("Howdy Mundo");
  }

  @Test
  void testDefaultMessage() throws Exception {
    assertThat(getMacroOutput("DEFAULTMESSAGE")).isEqualTo("hi planet");
  }

  @Test
  void testMessageArgs() throws Exception {
    assertThat(getMacroOutput("MESSAGEARGS")).isEqualTo("Howdy[World]");
  }

  @Test
  void testMessageArgsWithDefaultMessage() throws Exception {
    assertThat(getMacroOutput("MESSAGEARGSWITHDEFAULTMESSAGE")).isEqualTo("Hi");
  }

  @Test
  void messagesAndMacrosEscapeExactlyOnce() throws Exception {
    StaticMessageSource source = wac.getBean("messageSource", StaticMessageSource.class);
    source.addMessage("special", Locale.ENGLISH, "<b>Tom & \"Sue\"</b>");
    assertThat(renderMessages("""
            ${messages.getMessage('special')}|<@infra.message 'special'/>|\
            <@infra.messageText 'missing', '<default & text>'/>|\
            <@infra.messageArgs 'helloArgs', ['<World & friends>']/>
            """, Map.of(), null))
            .isEqualTo("&lt;b&gt;Tom &amp; &quot;Sue&quot;&lt;/b&gt;|&lt;b&gt;Tom &amp; &quot;Sue&quot;&lt;/b&gt;|"
                    + "&lt;default &amp; text&gt;|Howdy[&lt;World &amp; friends&gt;]");
  }

  @Test
  void explicitLocaleControlsMessagesAndFormatting() throws Exception {
    wac.getBean("messageSource", StaticMessageSource.class).addMessage("hello", Locale.GERMANY, "Hallo");
    assertThat(renderMessages("${messages.getMessage('hello')}|${.locale}|${1234.5?string('0.0')}",
            Map.of(), Locale.GERMANY)).isEqualTo("Hallo|de_DE|1234,5");
  }

  @Test
  void messagesRemainUnescapedOutsideHtmlOutput() throws Exception {
    wac.getBean("messageSource", StaticMessageSource.class).addMessage("special", Locale.ENGLISH, "<tag> & text");
    assertThat(renderMessages("<#outputformat 'plainText'>${messages.getMessage('special')}</#outputformat>",
            Map.of(), null)).isEqualTo("<tag> & text");
  }

  @Test
  void messagesNameIsReserved() {
    assertThatExceptionOfType(ViewRenderingException.class)
            .isThrownBy(() -> renderMessages("unused", Map.of("messages", "business value"), null))
            .withMessageContaining("existing model object");
  }

  @Test
  void requestMessagesNameIsReserved() {
    context.setAttribute("messages", "business value");
    assertThatExceptionOfType(ViewRenderingException.class)
            .isThrownBy(() -> renderMessages("unused", Map.of(), null));
  }

  @Test
  void disabledMessageExposureLeavesModelAndRequestNamesAvailable() throws Exception {
    storeTemplateInTempDir("${messages}");
    FreeMarkerView view = new FreeMarkerView();
    view.setConfiguration(fc.getConfiguration());
    view.setUrl("tmp.ftl");
    view.setExposeMessages(false);
    context.setAttribute("messages", "request value");
    view.render(Map.of("messages", "model value"), context);
    assertThat(getOutput()).isEqualTo("model value");
    assertThat(context.getAttribute("messages")).isEqualTo("model value");
  }

  @Test
  void disabledMessageExposureDoesNotCreateAccessor() throws Exception {
    storeTemplateInTempDir("${messages!'absent'}");
    FreeMarkerView view = new FreeMarkerView() {
      @Override
      protected infra.context.support.MessageSourceAccessor createMessageSourceAccessor(ServerHttpExchange context, Locale locale) {
        throw new AssertionError("Message accessor must not be created");
      }
    };
    view.setConfiguration(fc.getConfiguration());
    view.setUrl("tmp.ftl");
    view.setExposeMessages(false);
    view.render(Map.of(), context);
    assertThat(getOutput()).isEqualTo("absent");
  }

  @Test
  void repeatedRenderingDoesNotLeakMessagesIntoRequestOrView() throws Exception {
    wac.getBean("messageSource", StaticMessageSource.class).addMessage("hello", Locale.GERMANY, "Hallo");
    storeTemplateInTempDir("${messages.getMessage('hello')}");
    FreeMarkerView view = new FreeMarkerView();
    view.setConfiguration(fc.getConfiguration());
    view.setUrl("tmp.ftl");
    view.render(Map.of(), context);
    assertThat(getOutput()).isEqualTo("Howdy");
    assertThat(context.getAttribute("messages")).isNull();
    MockRequest secondRequest = new MockRequest();
    secondRequest.addPreferredLocale(Locale.GERMANY);
    MockResponse secondResponse = new MockResponse();
    MockServerHttpExchange secondContext = new MockServerHttpExchange(wac, secondRequest, secondResponse);
    view.render(Map.of(), secondContext);
    assertThat(secondResponse.getContentAsString()).isEqualTo("Hallo");
    assertThat(secondContext.getAttribute("messages")).isNull();
  }

  @Test
  void decoratedContextRetainsCustomMessageSourceAndLocaleResolver() throws Exception {
    StaticMessageSource source = new StaticMessageSource();
    source.addMessage("hello", Locale.GERMANY, "Custom Hallo");
    ServerHttpExchange custom = new AbstractDelegatingServerHttpExchange() {
      @Override
      public ServerHttpExchange delegate() {
        return context;
      }

      @Override
      public infra.context.MessageSource getMessageSource() {
        return source;
      }
    };
    ServerHttpExchange decorated = new AbstractDelegatingServerHttpExchange() {
      @Override
      public ServerHttpExchange delegate() {
        return custom;
      }
    };
    var resolver = (AcceptHeaderLocaleResolver) wac.getBean(AcceptHeaderLocaleResolver.BEAN_NAME);
    resolver.setSupportedLocales(java.util.List.of(Locale.GERMANY));
    resolver.setDefaultLocale(Locale.GERMANY);
    storeTemplateInTempDir("${messages.getMessage('hello')}|${.locale}");
    FreeMarkerView view = new FreeMarkerView();
    view.setConfiguration(fc.getConfiguration());
    view.setUrl("tmp.ftl");
    view.render(Map.of(), decorated);
    assertThat(getOutput()).isEqualTo("Custom Hallo|de_DE");
  }

  @Test
  void missingMessageWithoutDefaultFails() {
    assertThatExceptionOfType(TemplateException.class)
            .isThrownBy(() -> renderMessages("${messages.getMessage('missing')}", Map.of(), null))
            .withRootCauseInstanceOf(infra.context.NoSuchMessageException.class);
  }

  @Test
  void defaultMessagesAndResolvableUseAccessorContracts() throws Exception {
    var resolvable = new infra.context.support.DefaultMessageSourceResolvable(
            new String[] { "missing", "helloArgs" }, new Object[] { "World" }, "fallback");
    assertThat(renderMessages("${messages.getMessage(error)}|${messages.getMessage('missing', '')}",
            Map.of("error", resolvable), null)).isEqualTo("Howdy[World]|");
  }

  @Test
  void nullableDefaultRetainsEmptyStringContractWithoutEscaping() {
    StaticMessageSource source = new StaticMessageSource();
    var messages = new FreeMarkerView.FreeMarkerMessages(source, Locale.ENGLISH);
    assertThat(source.getMessage("missing", null, null, Locale.ENGLISH)).isNull();
    assertThat(messages.getMessage("missing", (String) null)).isEmpty();
    assertThat(messages.getMessage("missing", "")).isEmpty();
    assertThat(messages.getMessage("missing", "<fallback> & text")).isEqualTo("<fallback> & text");
  }

  private String renderMessages(String template, Map<String, ?> model, Locale locale) throws Exception {
    storeTemplateInTempDir("<#outputformat 'HTML'>" + template + "</#outputformat>");
    FreeMarkerView view = new FreeMarkerView();
    view.setUrl("tmp.ftl");
    view.setConfiguration(fc.getConfiguration());
    view.setLocale(locale);
    view.render(model, context);
    return getOutput();
  }

  @Test
  void testForm1() throws Exception {
    assertThat(getMacroOutput("FORM1")).isEqualTo("<input type=\"text\" id=\"name\" name=\"name\" value=\"Darren\" >");
  }

  @Test
  void testForm2() throws Exception {
    assertThat(getMacroOutput("FORM2")).isEqualTo("<input type=\"text\" id=\"name\" name=\"name\" value=\"Darren\" class=\"myCssClass\" >");
  }

  @Test
  void testForm3() throws Exception {
    assertThat(getMacroOutput("FORM3")).isEqualTo("<textarea id=\"name\" name=\"name\" >\nDarren</textarea>");
  }

  @Test
  void testForm4() throws Exception {
    assertThat(getMacroOutput("FORM4")).isEqualTo("<textarea id=\"name\" name=\"name\" rows=10 cols=30>\nDarren</textarea>");
  }

  // TODO verify remaining output for forms 5, 6, 7, 8, and 14 (fix whitespace)

  @Test
  void testForm9() throws Exception {
    assertThat(getMacroOutput("FORM9")).isEqualTo("<input type=\"password\" id=\"name\" name=\"name\" value=\"\" >");
  }

  @Test
  void testForm10() throws Exception {
    assertThat(getMacroOutput("FORM10")).isEqualTo("<input type=\"hidden\" id=\"name\" name=\"name\" value=\"Darren\" >");
  }

  @Test
  void testForm11() throws Exception {
    assertThat(getMacroOutput("FORM11")).isEqualTo("<input type=\"text\" id=\"name\" name=\"name\" value=\"Darren\" >");
  }

  @Test
  void testForm12() throws Exception {
    assertThat(getMacroOutput("FORM12")).isEqualTo("<input type=\"hidden\" id=\"name\" name=\"name\" value=\"Darren\" >");
  }

  @Test
  void testForm13() throws Exception {
    assertThat(getMacroOutput("FORM13")).isEqualTo("<input type=\"password\" id=\"name\" name=\"name\" value=\"\" >");
  }

  @Test
  void testForm15() throws Exception {
    String output = getMacroOutput("FORM15");
    assertThat(output).as("Wrong output: " + output)
            .startsWith("<input type=\"hidden\" name=\"_name\" value=\"on\"/>");
    assertThat(output).as("Wrong output: " + output)
            .contains("<input type=\"checkbox\" id=\"name\" name=\"name\" />");
  }

  @Test
  void testForm16() throws Exception {
    String output = getMacroOutput("FORM16");
    assertThat(output).as("Wrong output: " + output)
            .startsWith("<input type=\"hidden\" name=\"_jedi\" value=\"on\"/>");
    assertThat(output).as("Wrong output: " + output)
            .contains("<input type=\"checkbox\" id=\"jedi\" name=\"jedi\" checked=\"checked\" />");
  }

  @Test
  void testForm17() throws Exception {
    assertThat(getMacroOutput("FORM17")).isEqualTo("<input type=\"text\" id=\"spouses0.name\" name=\"spouses[0].name\" value=\"Fred\" >");
  }

  @Test
  void testForm18() throws Exception {
    String output = getMacroOutput("FORM18");
    assertThat(output).as("Wrong output: " + output)
            .startsWith("<input type=\"hidden\" name=\"_spouses[0].jedi\" value=\"on\"/>");
    assertThat(output).as("Wrong output: " + output)
            .contains("<input type=\"checkbox\" id=\"spouses0.jedi\" name=\"spouses[0].jedi\" checked=\"checked\" />");
  }

  private String getMacroOutput(String name) throws Exception {
    String macro = fetchMacro(name);
    assertThat(macro).isNotNull();
    storeTemplateInTempDir(macro);

    DummyMacroExchange rc = new DummyMacroExchange(context);
    rc.setContextPath("/infratest");

    TestBean darren = new TestBean("Darren", 99);
    TestBean fred = new TestBean("Fred");
    fred.setJedi(true);
    darren.setSpouse(fred);
    darren.setJedi(true);
    darren.setStringArray(new String[] { "John", "Fred" });
    request.setAttribute("command", darren);

    Map<String, String> names = new HashMap<>();
    names.put("Darren", "Darren Davison");
    names.put("John", "John Doe");
    names.put("Fred", "Fred Bloggs");
    names.put("Rob&Harrop", "Rob Harrop");

    Configuration config = fc.getConfiguration();
    Map<String, Object> model = new HashMap<>();
    model.put("command", darren);
    model.put("request", rc);
    model.put("msgArgs", new Object[] { "World" });
    model.put("nameOptionMap", names);
    model.put("options", names.values());

    FreeMarkerView view = new FreeMarkerView();
    view.setBeanName("myView");
    view.setUrl("tmp.ftl");
    view.setConfiguration(config);

    view.render(model, context);
    return getOutput();
  }

  private static String fetchMacro(String name) throws Exception {
    for (String macro : loadMacros()) {
      if (macro.startsWith(name)) {
        return macro.substring(macro.indexOf("\n")).trim();
      }
    }
    return null;
  }

  private static String[] loadMacros() throws IOException {
    ClassPathResource resource = new ClassPathResource("test.ftl", FreeMarkerMacroTests.class);
    assertThat(resource.exists()).isTrue();
    String all = FileCopyUtils.copyToString(new InputStreamReader(resource.getInputStream()));
    all = all.replace("\r\n", "\n");
    return StringUtils.delimitedListToStringArray(all, "\n\n");
  }

  private void storeTemplateInTempDir(String macro) throws IOException {
    Files.writeString(this.templateLoaderPath.resolve("tmp.ftl"),
            "<#import \"/infra.ftl\" as infra />\n" + macro
    );
  }

  private String getOutput() throws IOException {
    String output = response.getContentAsString();
    output = output.replace("\r\n", "\n").replaceAll(" +", " ");
    return output.trim();
  }

}
