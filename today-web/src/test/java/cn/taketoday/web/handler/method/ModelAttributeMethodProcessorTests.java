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

package cn.taketoday.web.handler.method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.List;

import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.annotation.SynthesizingMethodParameter;
import cn.taketoday.format.support.DefaultFormattingConversionService;
import cn.taketoday.validation.BindingResult;
import cn.taketoday.validation.Errors;
import cn.taketoday.web.BindingContext;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.bind.MethodArgumentNotValidException;
import cn.taketoday.web.bind.WebDataBinder;
import cn.taketoday.web.bind.annotation.ModelAttribute;
import cn.taketoday.web.bind.annotation.SessionAttributes;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/20 9:42
 */
class ModelAttributeMethodProcessorTests {

  private ServletRequestContext request;

  private BindingContext container;

  private ModelAttributeMethodProcessor processor;

  private ResolvableMethodParameter paramNamedValidModelAttr;
  private ResolvableMethodParameter paramErrors;
  private ResolvableMethodParameter paramInt;
  private ResolvableMethodParameter paramModelAttr;
  private ResolvableMethodParameter paramBindingDisabledAttr;
  private ResolvableMethodParameter paramNonSimpleType;
  private ResolvableMethodParameter beanWithConstructorArgs;

  private HandlerMethod returnParamNamedModelAttrHandler;
  private HandlerMethod returnParamNonSimpleTypeHandler;

  @BeforeEach
  public void setup() throws Throwable {
    this.request = new ServletRequestContext(null, new MockHttpServletRequest(), null);
    this.container = new BindingContext();
    request.setBinding(container);
    this.processor = new ModelAttributeMethodProcessor(false);

    Method method = ModelAttributeHandler.class.getDeclaredMethod("modelAttribute",
            TestBean.class, Errors.class, int.class, TestBean.class,
            TestBean.class, TestBean.class, TestBeanWithConstructorArgs.class);

    this.paramNamedValidModelAttr = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 0));
    this.paramErrors = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 1));
    this.paramInt = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 2));
    this.paramModelAttr = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 3));
    this.paramBindingDisabledAttr = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 4));
    this.paramNonSimpleType = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 5));
    this.beanWithConstructorArgs = new ResolvableMethodParameter(new SynthesizingMethodParameter(method, 6));

    method = getClass().getDeclaredMethod("annotatedReturnValue");
    this.returnParamNamedModelAttrHandler = new HandlerMethod(this, method);

    method = getClass().getDeclaredMethod("notAnnotatedReturnValue");
    this.returnParamNonSimpleTypeHandler = new HandlerMethod(this, method);
  }

  @Test
  public void supportedParameters() throws Throwable {
    assertThat(this.processor.supportsParameter(this.paramNamedValidModelAttr)).isTrue();
    assertThat(this.processor.supportsParameter(this.paramModelAttr)).isTrue();

    assertThat(this.processor.supportsParameter(this.paramErrors)).isFalse();
    assertThat(this.processor.supportsParameter(this.paramInt)).isFalse();
    assertThat(this.processor.supportsParameter(this.paramNonSimpleType)).isFalse();
  }

  @Test
  public void supportedParametersInDefaultResolutionMode() throws Throwable {
    processor = new ModelAttributeMethodProcessor(true);

    // Only non-simple types, even if not annotated
    assertThat(this.processor.supportsParameter(this.paramNamedValidModelAttr)).isTrue();
    assertThat(this.processor.supportsParameter(this.paramErrors)).isTrue();
    assertThat(this.processor.supportsParameter(this.paramModelAttr)).isTrue();
    assertThat(this.processor.supportsParameter(this.paramNonSimpleType)).isTrue();

    assertThat(this.processor.supportsParameter(this.paramInt)).isFalse();
  }

  @Test
  public void supportedReturnTypes() throws Throwable {
    processor = new ModelAttributeMethodProcessor(false);
    assertThat(this.processor.supportsHandlerMethod(returnParamNamedModelAttrHandler)).isTrue();
    assertThat(this.processor.supportsHandlerMethod(returnParamNonSimpleTypeHandler)).isFalse();
  }

  @Test
  public void supportedReturnTypesInDefaultResolutionMode() throws Throwable {
    processor = new ModelAttributeMethodProcessor(true);
    assertThat(this.processor.supportsHandlerMethod(returnParamNamedModelAttrHandler)).isTrue();
    assertThat(this.processor.supportsHandlerMethod(returnParamNonSimpleTypeHandler)).isTrue();
  }

  @Test
  public void bindExceptionRequired() throws Throwable {
    assertThat(this.processor.isBindExceptionRequired(null, this.paramNonSimpleType.getParameter())).isTrue();
    assertThat(this.processor.isBindExceptionRequired(null, this.paramNamedValidModelAttr.getParameter())).isFalse();
  }

  @Test
  public void resolveArgumentFromModel() throws Throwable {
    testGetAttributeFromModel("attrName", this.paramNamedValidModelAttr);
    testGetAttributeFromModel("testBean", this.paramModelAttr);
    testGetAttributeFromModel("testBean", this.paramNonSimpleType);
  }

  @Test
  public void resolveArgumentViaDefaultConstructor() throws Throwable {
    WebDataBinder dataBinder = new WebDataBinder(null);
    dataBinder.setTargetType(ResolvableType.forMethodParameter(paramNamedValidModelAttr.getParameter()));

    BindingContext factory = mock();
    request.setBinding(factory);
    given(factory.createBinder(any(), isNull(), eq("attrName"), any())).willReturn(dataBinder);

    this.processor.resolveArgument(request, this.paramNamedValidModelAttr);
    verify(factory).createBinder(any(), isNull(), eq("attrName"), any());
  }

  @Test
  public void resolveArgumentValidation() throws Throwable {
    String name = "attrName";
    Object target = new TestBean();
    this.container.addAttribute(name, target);

    StubRequestDataBinder dataBinder = new StubRequestDataBinder(target, name);
    BindingContext factory = mock(BindingContext.class);
    request.setBinding(factory);
    ResolvableType type = ResolvableType.forMethodParameter(paramNamedValidModelAttr.getParameter());

    given(factory.createBinder(this.request, target, name, type)).willReturn(dataBinder);

    this.processor.resolveArgument(this.request, paramNamedValidModelAttr);

    assertThat(dataBinder.isBindInvoked()).isTrue();
    assertThat(dataBinder.isValidateInvoked()).isTrue();
  }

  @Test
  public void resolveArgumentBindingDisabledPreviously() throws Throwable {

    String name = "attrName";
    Object target = new TestBean();

    StubRequestDataBinder dataBinder = new StubRequestDataBinder(target, name);
    BindingContext factory = new ModelHandlerTests.BindingContext0(dataBinder);

    factory.addAttribute(name, target);

    // Declare binding disabled (e.g. via @ModelAttribute method)
    factory.setBindingDisabled(name);

    request.setBinding(factory);

    this.processor.resolveArgument(request, paramNamedValidModelAttr);

    assertThat(dataBinder.isBindInvoked()).isFalse();
    assertThat(dataBinder.isValidateInvoked()).isTrue();
  }

  @Test
  public void resolveArgumentBindingDisabled() throws Throwable {
    String name = "noBindAttr";
    Object target = new TestBean();
    StubRequestDataBinder dataBinder = new StubRequestDataBinder(target, name);
    BindingContext factory = new ModelHandlerTests.BindingContext0(dataBinder);
    factory.addAttribute(name, target);

    request.setBinding(factory);
    this.processor.resolveArgument(request, this.paramBindingDisabledAttr);

    assertThat(dataBinder.isBindInvoked()).isFalse();
    assertThat(dataBinder.isValidateInvoked()).isTrue();
  }

  @Test
  public void resolveArgumentBindException() throws Throwable {
    String name = "testBean";
    Object target = new TestBean();
    this.container.getModel().addAttribute(target);

    StubRequestDataBinder dataBinder = new StubRequestDataBinder(target, name);
    dataBinder.getBindingResult().reject("error");
    BindingContext binderFactory = mock(BindingContext.class);
    request.setBinding(binderFactory);

    ResolvableType type = ResolvableType.forMethodParameter(paramNonSimpleType.getParameter());
    given(binderFactory.createBinder(this.request, target, name, type)).willReturn(dataBinder);

    assertThatExceptionOfType(MethodArgumentNotValidException.class)
            .isThrownBy(() -> processor.resolveArgument(request, this.paramNonSimpleType));
    verify(binderFactory).createBinder(this.request, target, name);
  }

  @Test  // SPR-9378
  public void resolveArgumentOrdering() throws Throwable {
    String name = "testBean";
    Object testBean = new TestBean(name);
    StubRequestDataBinder dataBinder = new StubRequestDataBinder(testBean, name);

    BindingContext factory = new ModelHandlerTests.BindingContext0(dataBinder);

    factory.addAttribute(name, testBean);
    factory.addAttribute(BindingResult.MODEL_KEY_PREFIX + name, testBean);

    Object anotherTestBean = new TestBean();
    factory.addAttribute("anotherTestBean", anotherTestBean);

    request.setBinding(factory);

    this.processor.resolveArgument(request, this.paramModelAttr);

    Object[] values = factory.getModel().values().toArray();
    assertThat(values[1]).as("Resolved attribute should be updated to be last").isSameAs(testBean);
    assertThat(values[2]).as("BindingResult of resolved attr should be last").isSameAs(dataBinder.getBindingResult());
  }

  @Test
  public void handleAnnotatedReturnValue() throws Throwable {
    this.processor.handleReturnValue(request, returnParamNamedModelAttrHandler, "expected");
    assertThat(this.container.getModel().get("modelAttrName")).isEqualTo("expected");
  }

  @Test
  public void handleNotAnnotatedReturnValue() throws Throwable {
    TestBean testBean = new TestBean("expected");
    this.processor.handleReturnValue(request, this.returnParamNonSimpleTypeHandler, testBean);
    assertThat(this.container.getModel().get("testBean")).isSameAs(testBean);
  }

  @Test  // gh-25182
  public void resolveConstructorListArgumentFromCommaSeparatedRequestParameter() throws Throwable {
    MockHttpServletRequest mockRequest = new MockHttpServletRequest();
    mockRequest.addParameter("listOfStrings", "1,2");
    ServletRequestContext requestWithParam = new ServletRequestContext(null, mockRequest, null);

    BindingContext factory = mock(BindingContext.class);
    ResolvableType type = ResolvableType.forMethodParameter(beanWithConstructorArgs.getParameter());
    given(factory.createBinder(any(), any(), eq("testBeanWithConstructorArgs"), type))
            .willAnswer(invocation -> {
              WebDataBinder binder = new WebDataBinder(invocation.getArgument(1));
              binder.setTargetType(ResolvableType.forMethodParameter(this.beanWithConstructorArgs.getParameter()));

              // Add conversion service which will convert "1,2" to a list
              binder.setConversionService(new DefaultFormattingConversionService());
              return binder;
            });

    requestWithParam.setBinding(factory);
    Object resolved = this.processor.resolveArgument(requestWithParam, this.beanWithConstructorArgs);
    assertThat(resolved).isInstanceOf(TestBeanWithConstructorArgs.class);
    assertThat(((TestBeanWithConstructorArgs) resolved).listOfStrings).containsExactly("1", "2");
  }

  private void testGetAttributeFromModel(String expectedAttrName, ResolvableMethodParameter param) throws Throwable {
    Object target = new TestBean();
    this.container.addAttribute(expectedAttrName, target);

    WebDataBinder dataBinder = new WebDataBinder(target);
    BindingContext factory = mock(BindingContext.class);
    ResolvableType type = ResolvableType.forMethodParameter(param.getParameter());
    given(factory.createBinder(this.request, target, expectedAttrName, type)).willReturn(dataBinder);

    request.setBinding(factory);
    this.processor.resolveArgument(request, param);
    verify(factory).createBinder(this.request, target, expectedAttrName);
  }

  private static class StubRequestDataBinder extends WebDataBinder {

    private boolean bindInvoked;

    private boolean validateInvoked;

    public StubRequestDataBinder(Object target, String objectName) {
      super(target, objectName);
    }

    public boolean isBindInvoked() {
      return bindInvoked;
    }

    public boolean isValidateInvoked() {
      return validateInvoked;
    }

    @Override
    public void bind(RequestContext request) {
      bindInvoked = true;
    }

    @Override
    public void validate() {
      validateInvoked = true;
    }

    @Override
    public void validate(Object... validationHints) {
      validateInvoked = true;
    }
  }

  @Target({ METHOD, FIELD, CONSTRUCTOR, PARAMETER })
  @Retention(RUNTIME)
  public @interface Valid {
  }

  @SessionAttributes(types = TestBean.class)
  private static class ModelAttributeHandler {

    @SuppressWarnings("unused")
    public void modelAttribute(
            @ModelAttribute("attrName") @Valid TestBean annotatedAttr,
            Errors errors,
            int intArg,
            @ModelAttribute TestBean defaultNameAttr,
            @ModelAttribute(name = "noBindAttr", binding = false) @Valid TestBean noBindAttr,
            TestBean notAnnotatedAttr,
            TestBeanWithConstructorArgs beanWithConstructorArgs) {
    }
  }

  static class TestBeanWithConstructorArgs {

    final List<String> listOfStrings;

    public TestBeanWithConstructorArgs(List<String> listOfStrings) {
      this.listOfStrings = listOfStrings;
    }
  }

  @ModelAttribute("modelAttrName")
  @SuppressWarnings("unused")
  private String annotatedReturnValue() {
    return null;
  }

  @SuppressWarnings("unused")
  private TestBean notAnnotatedReturnValue() {
    return null;
  }

}
