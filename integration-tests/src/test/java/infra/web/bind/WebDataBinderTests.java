/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.web.bind;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.beans.PropertyEditorSupport;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import infra.beans.PropertyValue;
import infra.beans.PropertyValues;
import infra.beans.testfixture.beans.ITestBean;
import infra.beans.testfixture.beans.TestBean;
import infra.core.ResolvableType;
import infra.lang.Nullable;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockMultipartHttpMockRequest;
import infra.validation.BindingResult;
import infra.web.HandlerMatchingMetadata;
import infra.web.RequestContext;
import infra.web.bind.annotation.BindParam;
import infra.web.bind.support.BindParamNameResolver;
import infra.web.mock.MockMultipartMockRequestContext;
import infra.web.mock.MockRequestContext;
import infra.web.mock.bind.MockRequestParameterPropertyValues;
import infra.web.multipart.support.StringMultipartFileEditor;
import infra.web.testfixture.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/2 16:39
 */
class WebDataBinderTests {

  HttpMockRequestImpl request = new HttpMockRequestImpl();

  MockRequestContext context = new MockRequestContext(request, null);

  @Test
  void testBindingWithNestedObjectCreation() throws Exception {
    TestBean tb = new TestBean();

    WebDataBinder binder = new WebDataBinder(tb, "person");
    binder.registerCustomEditor(ITestBean.class, new PropertyEditorSupport() {
      @Override
      public void setAsText(String text) throws IllegalArgumentException {
        setValue(new TestBean());
      }
    });

    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addParameter("spouse", "someValue");
    request.addParameter("spouse.name", "test");
    binder.bind(new MockRequestContext(null, request, null));

    assertThat(tb.getSpouse()).isNotNull();
    assertThat(tb.getSpouse().getName()).isEqualTo("test");
  }

  @Test
  void testBindingWithNestedObjectCreationThroughAutoGrow() throws Exception {
    TestBean tb = new TestBeanWithConcreteSpouse();

    WebDataBinder binder = new WebDataBinder(tb, "person");
    binder.setIgnoreUnknownFields(false);

    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addParameter("concreteSpouse.name", "test");
    binder.bind(new MockRequestContext(null, request, null));

    assertThat(tb.getSpouse()).isNotNull();
    assertThat(tb.getSpouse().getName()).isEqualTo("test");
  }

  @Test
  void testFieldPrefixCausesFieldReset() throws Exception {
    TestBean target = new TestBean();
    WebDataBinder binder = new WebDataBinder(target);

    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addParameter("_postProcessed", "visible");
    request.addParameter("postProcessed", "on");
    binder.bind(new MockRequestContext(null, request, null));
    assertThat(target.isPostProcessed()).isTrue();

    request.removeParameter("postProcessed");
    binder.bind(new MockRequestContext(null, request, null));
    assertThat(target.isPostProcessed()).isFalse();
  }

  @Test
  void testFieldPrefixCausesFieldResetWithIgnoreUnknownFields() throws Exception {
    TestBean target = new TestBean();
    WebDataBinder binder = new WebDataBinder(target);
    binder.setIgnoreUnknownFields(false);

    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addParameter("_postProcessed", "visible");
    request.addParameter("postProcessed", "on");
    binder.bind(new MockRequestContext(null, request, null));
    assertThat(target.isPostProcessed()).isTrue();

    request.removeParameter("postProcessed");
    binder.bind(new MockRequestContext(null, request, null));
    assertThat(target.isPostProcessed()).isFalse();
  }

  @Test
  void testFieldWithArrayIndex() {
    TestBean target = new TestBean();
    WebDataBinder binder = new WebDataBinder(target);
    binder.setIgnoreUnknownFields(false);

    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addParameter("stringArray[0]", "ONE");
    request.addParameter("stringArray[1]", "TWO");
    binder.bind(new MockRequestContext(request, null));
    assertThat(target.getStringArray()).containsExactly("ONE", "TWO");
  }

  @Test
  public void testFieldWithEmptyArrayIndex() {
    TestBean target = new TestBean();
    WebDataBinder binder = new WebDataBinder(target);
    binder.setIgnoreUnknownFields(false);

    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addParameter("stringArray[]", "ONE");
    request.addParameter("stringArray[]", "TWO");
    binder.bind(new MockRequestContext(request, null));
    assertThat(target.getStringArray()).containsExactly("ONE", "TWO");
  }

  @Test
  public void testFieldDefault() throws Exception {
    TestBean target = new TestBean();
    WebDataBinder binder = new WebDataBinder(target);

    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addParameter("!postProcessed", "off");
    request.addParameter("postProcessed", "on");
    binder.bind(new MockRequestContext(request, null));
    assertThat(target.isPostProcessed()).isTrue();

    request.removeParameter("postProcessed");
    binder.bind(new MockRequestContext(request, null));
    assertThat(target.isPostProcessed()).isFalse();
  }

  @Test
  public void testCollectionFieldsDefault() throws Exception {
    TestBean target = new TestBean();
    target.setSomeSet(null);
    target.setSomeList(null);
    target.setSomeMap(null);
    WebDataBinder binder = new WebDataBinder(target);

    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addParameter("_someSet", "visible");
    request.addParameter("_someList", "visible");
    request.addParameter("_someMap", "visible");

    binder.bind(new MockRequestContext(request, null));
    assertThat(target.getSomeSet()).isNotNull().isInstanceOf(Set.class);
    assertThat(target.getSomeList()).isNotNull().isInstanceOf(List.class);
    assertThat(target.getSomeMap()).isNotNull().isInstanceOf(Map.class);
  }

  @Test
  public void testFieldDefaultPreemptsFieldMarker() throws Exception {
    TestBean target = new TestBean();
    WebDataBinder binder = new WebDataBinder(target);

    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addParameter("!postProcessed", "on");
    request.addParameter("_postProcessed", "visible");
    request.addParameter("postProcessed", "on");
    binder.bind(new MockRequestContext(request, null));
    assertThat(target.isPostProcessed()).isTrue();

    request.removeParameter("postProcessed");
    binder.bind(new MockRequestContext(request, null));
    assertThat(target.isPostProcessed()).isTrue();

    request.removeParameter("!postProcessed");
    binder.bind(new MockRequestContext(request, null));
    assertThat(target.isPostProcessed()).isFalse();
  }

  @Test
  public void testFieldDefaultWithNestedProperty() throws Exception {
    TestBean target = new TestBean();
    target.setSpouse(new TestBean());
    WebDataBinder binder = new WebDataBinder(target);

    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addParameter("!spouse.postProcessed", "on");
    request.addParameter("_spouse.postProcessed", "visible");
    request.addParameter("spouse.postProcessed", "on");
    binder.bind(new MockRequestContext(request, null));
    assertThat(((TestBean) target.getSpouse()).isPostProcessed()).isTrue();

    request.removeParameter("spouse.postProcessed");
    binder.bind(new MockRequestContext(request, null));
    assertThat(((TestBean) target.getSpouse()).isPostProcessed()).isTrue();

    request.removeParameter("!spouse.postProcessed");
    binder.bind(new MockRequestContext(request, null));
    assertThat(((TestBean) target.getSpouse()).isPostProcessed()).isFalse();
  }

  @Test
  public void testFieldDefaultNonBoolean() throws Exception {
    TestBean target = new TestBean();
    WebDataBinder binder = new WebDataBinder(target);

    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addParameter("!name", "anonymous");
    request.addParameter("name", "Scott");
    binder.bind(new MockRequestContext(request, null));
    assertThat(target.getName()).isEqualTo("Scott");

    request.removeParameter("name");
    binder.bind(new MockRequestContext(request, null));
    assertThat(target.getName()).isEqualTo("anonymous");
  }

  @Test
  public void testWithCommaSeparatedStringArray() throws Exception {
    TestBean target = new TestBean();
    WebDataBinder binder = new WebDataBinder(target);

    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addParameter("stringArray", "bar");
    request.addParameter("stringArray", "abc");
    request.addParameter("stringArray", "123,def");
    binder.bind(new MockRequestContext(request, null));
    assertThat(target.getStringArray().length).as("Expected all three items to be bound").isEqualTo(3);

    request.removeParameter("stringArray");
    request.addParameter("stringArray", "123,def");
    binder.bind(new MockRequestContext(request, null));
    assertThat(target.getStringArray().length).as("Expected only 1 item to be bound").isEqualTo(1);
  }

  @Test
  public void testEnumBinding() {
    EnumHolder target = new EnumHolder();
    WebDataBinder binder = new WebDataBinder(target);

    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addParameter("myEnum", "FOO");
    binder.bind(new MockRequestContext(request, null));
    assertThat(target.getMyEnum()).isEqualTo(MyEnum.FOO);
  }

  @Test
  public void testMultipartFileAsString() {
    TestBean target = new TestBean();
    WebDataBinder binder = new WebDataBinder(target);
    binder.registerCustomEditor(String.class, new StringMultipartFileEditor());

    MockMultipartHttpMockRequest request = new MockMultipartHttpMockRequest();
    request.addFile(new MockMultipartFile("name", "Juergen".getBytes()));
    binder.bind(new MockMultipartMockRequestContext(request, null));
    assertThat(target.getName()).isEqualTo("Juergen");
  }

  @Test
  public void testMultipartFileAsStringArray() {
    TestBean target = new TestBean();
    WebDataBinder binder = new WebDataBinder(target);
    binder.registerCustomEditor(String.class, new StringMultipartFileEditor());

    MockMultipartHttpMockRequest request = new MockMultipartHttpMockRequest();
    request.addFile(new MockMultipartFile("stringArray", "Juergen".getBytes()));
    binder.bind(new MockMultipartMockRequestContext(request, null));
    assertThat(target.getStringArray().length).isEqualTo(1);
    assertThat(target.getStringArray()[0]).isEqualTo("Juergen");
  }

  @Test
  public void testMultipartFilesAsStringArray() {
    TestBean target = new TestBean();
    WebDataBinder binder = new WebDataBinder(target);
    binder.registerCustomEditor(String.class, new StringMultipartFileEditor());

    MockMultipartHttpMockRequest request = new MockMultipartHttpMockRequest();
    request.addFile(new MockMultipartFile("stringArray", "Juergen".getBytes()));
    request.addFile(new MockMultipartFile("stringArray", "Eva".getBytes()));
    binder.bind(new MockMultipartMockRequestContext(request, null));
    assertThat(target.getStringArray().length).isEqualTo(2);
    assertThat(target.getStringArray()[0]).isEqualTo("Juergen");
    assertThat(target.getStringArray()[1]).isEqualTo("Eva");
  }

  @Test
  public void testNoPrefix() throws Exception {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addParameter("forname", "Tony");
    request.addParameter("surname", "Blair");
    request.addParameter("age", "" + 50);

    MockRequestParameterPropertyValues pvs = new MockRequestParameterPropertyValues(request);
    doTestTony(pvs);
  }

  @Test
  public void testPrefix() throws Exception {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addParameter("test_forname", "Tony");
    request.addParameter("test_surname", "Blair");
    request.addParameter("test_age", "" + 50);

    MockRequestParameterPropertyValues pvs = new MockRequestParameterPropertyValues(request);
    boolean condition = !pvs.contains("forname");
    assertThat(condition).as("Didn't find normal when given prefix").isTrue();
    assertThat(pvs.contains("test_forname")).as("Did treat prefix as normal when not given prefix").isTrue();

    pvs = new MockRequestParameterPropertyValues(request, "test");
    doTestTony(pvs);
  }

  /**
   * Must contain: forname=Tony surname=Blair age=50
   */
  protected void doTestTony(PropertyValues pvs) throws Exception {
    assertThat(pvs.toArray().length == 3).as("Contains 3").isTrue();
    assertThat(pvs.contains("forname")).as("Contains forname").isTrue();
    assertThat(pvs.contains("surname")).as("Contains surname").isTrue();
    assertThat(pvs.contains("age")).as("Contains age").isTrue();
    boolean condition1 = !pvs.contains("tory");
    assertThat(condition1).as("Doesn't contain tory").isTrue();

    PropertyValue[] pvArray = pvs.toArray();
    Map<String, String> m = new HashMap<>();
    m.put("forname", "Tony");
    m.put("surname", "Blair");
    m.put("age", "50");
    for (PropertyValue pv : pvArray) {
      Object val = m.get(pv.getName());
      assertThat(val != null).as("Can't have unexpected value").isTrue();
      boolean condition = val instanceof String;
      assertThat(condition).as("Val i string").isTrue();
      assertThat(val.equals(pv.getValue())).as("val matches expected").isTrue();
      m.remove(pv.getName());
    }
    assertThat(m.size() == 0).as("Map size is 0").isTrue();
  }

  @Test
  public void testNoParameters() throws Exception {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockRequestParameterPropertyValues pvs = new MockRequestParameterPropertyValues(request);
    assertThat(pvs.toArray().length == 0).as("Found no parameters").isTrue();
  }

  @Test
  public void testMultipleValuesForParameter() throws Exception {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    String[] original = new String[] { "Tony", "Rod" };
    request.addParameter("forname", original);

    MockRequestParameterPropertyValues pvs = new MockRequestParameterPropertyValues(request);
    assertThat(pvs.toArray().length == 1).as("Found 1 parameter").isTrue();
    boolean condition = pvs.getPropertyValue("forname") instanceof String[];
    assertThat(condition).as("Found array value").isTrue();
    String[] values = (String[]) pvs.getPropertyValue("forname");
    assertThat(Arrays.asList(original)).as("Correct values").isEqualTo(Arrays.asList(values));
  }

  @ParameterizedTest
  @ValueSource(strings = { "Accept", "Authorization", "Connection",
          "Cookie", "From", "Host", "Origin", "Priority", "Range", "Referer", "Upgrade" })
  void filteredHeaders(String headerName) {
    TestBinder binder = new TestBinder();

    HttpMockRequestImpl request = new HttpMockRequestImpl();

    PropertyValues mpvs = new PropertyValues();
    request.addHeader(headerName, "u1");

    MockRequestContext context = new MockRequestContext(request, null);

    binder.addBindValues(mpvs, context);
    assertThat(mpvs).isEmpty();
  }

  @Test
  void headerPredicateWithConstructorArgs() {
    WebDataBinder binder = new WebDataBinder(null);
    binder.addHeaderPredicate(name -> !name.equalsIgnoreCase("Some-Int-Array"));
    binder.setTargetType(ResolvableType.forClass(DataBean.class));
    binder.setNameResolver(new BindParamNameResolver());

    request.addHeader("Some-Int-Array", "1");
    request.addHeader("Some-Int-Array", "2");

    MockRequestContext context = new MockRequestContext(request, null);
    binder.construct(context);

    DataBean bean = (DataBean) binder.getTarget();
    assertThat(bean).isNotNull();
    assertThat(bean.someIntArray()).isNull();
  }

  @Test
  void filteredPriorityHeaderForConstructorBinding() {
    TestBinder binder = new TestBinder();
    binder.setTargetType(ResolvableType.forClass(TestTarget.class));
    request.addHeader("Priority", "u1");

    binder.construct(context);
    BindingResult result = binder.getBindingResult();
    TestTarget target = (TestTarget) result.getTarget();

    assertThat(target.priority).isNull();
  }

  @Test
  void headerPredicate() {
    TestBinder binder = new TestBinder();
    binder.addHeaderPredicate(name -> !name.equalsIgnoreCase("Another-Int-Array"));

    PropertyValues mpvs = new PropertyValues();
    request.addHeader("Priority", "u1");
    request.addHeader("Some-Int-Array", "1");
    request.addHeader("Another-Int-Array", "1");

    binder.addBindValues(mpvs, context);

    assertThat(mpvs.size()).isEqualTo(1);
    assertThat(mpvs.getPropertyValue("someIntArray")).isEqualTo("1");
  }

  @Test
  void noUriTemplateVars() {
    TestBean target = new TestBean();
    WebDataBinder binder = new WebDataBinder(target, "");

    binder.bind(context);

    assertThat(target.getName()).isNull();
    assertThat(target.getAge()).isEqualTo(0);
  }

  @Test
  void createBinderViaSetters() {
    context.setMatchingMetadata(new UriVariablesMatchingMetadata(context,
            Map.of("name", "John", "age", "25")));

    request.addHeader("Some-Int-Array", "1");
    request.addHeader("Some-Int-Array", "2");

    TestBean target = new TestBean();
    WebDataBinder binder = new WebDataBinder(target, "");
    binder.bind(context);

    assertThat(target.getName()).isEqualTo("John");
    assertThat(target.getAge()).isEqualTo(25);
    assertThat(target.getSomeIntArray()).containsExactly(1, 2);
  }

  @Test
  void createBinderViaConstructor() {
    context.setMatchingMetadata(new UriVariablesMatchingMetadata(context,
            Map.of("name", "John", "age", "25")));

    request.addHeader("Some-Int-Array", "1");
    request.addHeader("Some-Int-Array", "2");

    WebDataBinder binder = new WebDataBinder(null);
    binder.setTargetType(ResolvableType.forClass(DataBean.class));
    binder.setNameResolver(new BindParamNameResolver());
    binder.construct(context);

    DataBean bean = (DataBean) binder.getTarget();

    assertThat(bean.name()).isEqualTo("John");
    assertThat(bean.age()).isEqualTo(25);
    assertThat(bean.someIntArray()).containsExactly(1, 2);
  }

  @Test
  void uriVarsAndHeadersAddedConditionally() {
    request.addParameter("name", "John");
    request.addParameter("age", "25");
    request.addHeader("name", "Johnny");

    context.setMatchingMetadata(new UriVariablesMatchingMetadata(context, Map.of("age", "26")));

    TestBean target = new TestBean();
    WebDataBinder binder = new WebDataBinder(target, "");
    binder.bind(context);

    assertThat(target.getName()).isEqualTo("John");
    assertThat(target.getAge()).isEqualTo(25);
  }



  public static class EnumHolder {

    private MyEnum myEnum;

    public MyEnum getMyEnum() {
      return myEnum;
    }

    public void setMyEnum(MyEnum myEnum) {
      this.myEnum = myEnum;
    }
  }

  public enum MyEnum {
    FOO, BAR
  }

  static class TestBeanWithConcreteSpouse extends TestBean {
    public void setConcreteSpouse(TestBean spouse) {
      setSpouse(spouse);
    }

    public TestBean getConcreteSpouse() {
      return (TestBean) getSpouse();
    }
  }

  record DataBean(String name, int age, @Nullable @BindParam("Some-Int-Array") Integer[] someIntArray) {

  }

  static class TestTarget {

    final String priority;

    public TestTarget(String priority) {
      this.priority = priority;
    }

  }

  private static class TestBinder extends WebDataBinder {

    public TestBinder() {
      super(null);
    }

    @Override
    protected void addBindValues(PropertyValues pv, RequestContext request) {
      super.addBindValues(pv, request);
    }

  }

  static class UriVariablesMatchingMetadata extends HandlerMatchingMetadata {

    public UriVariablesMatchingMetadata(RequestContext request, Map<String, String> uriVariables) {
      super(request);
      this.uriVariables = uriVariables;
    }

    private final Map<String, String> uriVariables;

    @Override
    public Map<String, String> getUriVariables() {
      return uriVariables;
    }

  }

}
