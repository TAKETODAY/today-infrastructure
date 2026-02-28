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
import java.util.Map;

import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import infra.beans.testfixture.beans.TestBean;
import infra.core.io.ClassPathResource;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.util.FileCopyUtils;
import infra.util.StringUtils;
import infra.web.BindStatus;
import infra.web.RequestContext;
import infra.web.i18n.AcceptHeaderLocaleResolver;
import infra.web.mock.MockRequestContext;
import infra.web.mock.support.StaticWebApplicationContext;

import static infra.web.view.config.AbstractTemplateViewResolverProperties.DEFAULT_REQUEST_CONTEXT_ATTRIBUTE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Darren Davison
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 25.01.2005
 */
class FreeMarkerMacroTests {

  private static final String TEMPLATE_FILE = "infra/web/view/freemarker/test.ftl";

  private final StaticWebApplicationContext wac = new StaticWebApplicationContext();

  private final HttpMockRequestImpl request = new HttpMockRequestImpl();

  private final MockHttpResponseImpl response = new MockHttpResponseImpl();

  private final MockRequestContext context = new MockRequestContext(wac, request, response);

  private final FreeMarkerConfigurer fc = new FreeMarkerConfigurer();

  @TempDir
  private Path templateLoaderPath;

  @BeforeEach
  void setUp() throws Exception {
    fc.setTemplateLoaderPaths("classpath:/", "file://" + this.templateLoaderPath);
    fc.afterPropertiesSet();

    wac.getBeanFactory().registerSingleton("freeMarkerConfigurer", fc);
    wac.refresh();

    wac.registerSingleton(AcceptHeaderLocaleResolver.BEAN_NAME, new AcceptHeaderLocaleResolver());
  }

  @Test
  void testExposeInfraMacroHelpers() throws Exception {
    FreeMarkerView fv = new FreeMarkerView() {

      @Override
      protected void processTemplate(Template template, SimpleHash fmModel, RequestContext response) throws IOException, TemplateException {
        Map model = fmModel.toMap();
        assertThat(model.get(DEFAULT_REQUEST_CONTEXT_ATTRIBUTE)).isInstanceOf(RequestContext.class);
        RequestContext rc = (RequestContext) model.get(DEFAULT_REQUEST_CONTEXT_ATTRIBUTE);
        BindStatus status = rc.getBindStatus("tb.name");
        assertThat(status.getExpression()).isEqualTo("name");
        assertThat(status.getValue()).isEqualTo("juergen");
      }
    };
    fv.setUrl(TEMPLATE_FILE);
    fv.setApplicationContext(wac);
    fv.setRequestContextAttribute(DEFAULT_REQUEST_CONTEXT_ATTRIBUTE);

    Map<String, Object> model = new HashMap<>();
    model.put("tb", new TestBean("juergen", 99));
    fv.render(model, context);
  }

  @Test
  void testName() throws Exception {
    assertThat(getMacroOutput("NAME")).isEqualTo("Darren");
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

    DummyMacroRequestContext rc = new DummyMacroRequestContext(context);
    Map<String, String> msgMap = new HashMap<>();
    msgMap.put("hello", "Howdy");
    msgMap.put("world", "Mundo");
    rc.setMessageMap(msgMap);
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
