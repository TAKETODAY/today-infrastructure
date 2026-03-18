package infra.web.handler.method;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.function.Predicate;

import infra.beans.BeanUtils;
import infra.context.MessageSourceResolvable;
import infra.context.support.DefaultMessageSourceResolvable;
import infra.core.DefaultParameterNameDiscoverer;
import infra.core.MethodParameter;
import infra.validation.BeanPropertyBindingResult;
import infra.validation.Errors;
import infra.validation.method.MethodValidationResult;
import infra.validation.method.ParameterErrors;
import infra.validation.method.ParameterValidationResult;
import infra.web.ResolvableMethod;
import infra.web.annotation.CookieValue;
import infra.web.annotation.MatrixVariable;
import infra.web.annotation.PathVariable;
import infra.web.annotation.RequestBody;
import infra.web.annotation.RequestHeader;
import infra.web.annotation.RequestParam;
import infra.web.annotation.RequestPart;
import infra.web.annotation.RestController;
import infra.web.bind.annotation.ModelAttribute;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/3/4 15:33
 */
class HandlerMethodValidationExceptionTests {

  private static final Person person = new Person("Faustino1234");

  private static final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

  private final HandlerMethod handlerMethod = handlerMethod(new ValidController(),
          controller -> controller.handle(person, person, person, List.of(), person, "", "", "", "", "", ""));

  private final TestVisitor visitor = new TestVisitor();

  @Test
  void traverse() {
    HandlerMethodValidationException ex =
            new HandlerMethodValidationException(createMethodValidationResult(this.handlerMethod),
                    new MvcParamPredicate(ModelAttribute.class),
                    new MvcParamPredicate(RequestParam.class));

    ex.visitResults(this.visitor);

    assertThat(this.visitor.getOutput()).isEqualTo("""
            @ModelAttribute: modelAttribute1, @ModelAttribute: modelAttribute2, \
            @RequestBody: requestBody, @RequestBody: requestBodyList, @RequestPart: requestPart, \
            @RequestParam: requestParam1, @RequestParam: requestParam2, \
            @RequestHeader: header, @PathVariable: pathVariable, \
            @CookieValue: cookie, @MatrixVariable: matrixVariable""");
  }

  @Test
  void traverseRemaining() {

    HandlerMethodValidationException ex =
            new HandlerMethodValidationException(createMethodValidationResult(this.handlerMethod));

    ex.visitResults(this.visitor);

    assertThat(this.visitor.getOutput()).isEqualTo("""
            Other: modelAttribute1, @ModelAttribute: modelAttribute2, \
            @RequestBody: requestBody, @RequestBody: requestBodyList, @RequestPart: requestPart, \
            Other: requestParam1, @RequestParam: requestParam2, \
            @RequestHeader: header, @PathVariable: pathVariable, \
            @CookieValue: cookie, @MatrixVariable: matrixVariable""");
  }

  @SuppressWarnings("unchecked")
  private static <T> HandlerMethod handlerMethod(T controller, Consumer<T> mockCallConsumer) {
    Method method = ResolvableMethod.on((Class<T>) controller.getClass()).mockCall(mockCallConsumer).method();
    HandlerMethod hm = new HandlerMethod(controller, method);
    for (MethodParameter parameter : hm.getParameters()) {
      parameter.initParameterNameDiscovery(parameterNameDiscoverer);
    }
    return hm;
  }

  private static MethodValidationResult createMethodValidationResult(HandlerMethod handlerMethod) {
    return MethodValidationResult.create(
            handlerMethod.getBean(), handlerMethod.getMethod(),
            Arrays.stream(handlerMethod.getParameters())
                    .map(param -> {
                      if (param.hasParameterAnnotation(Valid.class)) {
                        Errors errors = new BeanPropertyBindingResult(person, param.getParameterName());
                        errors.rejectValue("name", "Size.person.name");
                        return new ParameterErrors(param, person, errors, null, null, null);
                      }
                      else {
                        MessageSourceResolvable error = new DefaultMessageSourceResolvable("Size");
                        return new ParameterValidationResult(
                                param, "123", List.of(error), null, null, null, (e, t) -> null);
                      }
                    })
                    .toList());
  }

  @SuppressWarnings("unused")
  private record Person(@Size(min = 1, max = 10) String name) {

    @Override
    public String name() {
      return this.name;
    }
  }

  @SuppressWarnings({ "unused", "SameParameterValue", "UnusedReturnValue" })
  @RestController
  static class ValidController {

    void handle(
            @Valid Person modelAttribute1,
            @Valid @ModelAttribute Person modelAttribute2,
            @Valid @RequestBody Person requestBody,
            @RequestBody List<@NotEmpty String> requestBodyList,
            @Valid @RequestPart Person requestPart,
            @Size(min = 5) String requestParam1,
            @Size(min = 5) @RequestParam String requestParam2,
            @Size(min = 5) @RequestHeader String header,
            @Size(min = 5) @PathVariable String pathVariable,
            @Size(min = 5) @CookieValue String cookie,
            @Size(min = 5) @MatrixVariable String matrixVariable) {
    }

  }

  private record MvcParamPredicate(Class<? extends Annotation> type) implements Predicate<MethodParameter> {

    @Override
    public boolean test(MethodParameter param) {
      return (param.hasParameterAnnotation(this.type) ||
              (isDefaultParameter(param) && !hasMvcAnnotation(param)));
    }

    private boolean isDefaultParameter(MethodParameter param) {
      boolean simpleType = BeanUtils.isSimpleValueType(param.getParameterType());
      return ((this.type.equals(RequestParam.class) && simpleType) ||
              (this.type.equals(ModelAttribute.class) && !simpleType));
    }

    private boolean hasMvcAnnotation(MethodParameter param) {
      return Arrays.stream(param.getParameterAnnotations())
              .map(Annotation::annotationType)
              .anyMatch(type -> type.getPackage().equals(RequestParam.class.getPackage()));
    }
  }

  private static class TestVisitor implements HandlerMethodValidationException.Visitor {

    private final StringJoiner joiner = new StringJoiner(", ");

    public String getOutput() {
      return this.joiner.toString();
    }

    @Override
    public void cookieValue(CookieValue cookieValue, ParameterValidationResult result) {
      handle(cookieValue, result);
    }

    @Override
    public void matrixVariable(MatrixVariable matrixVariable, ParameterValidationResult result) {
      handle(matrixVariable, result);
    }

    @Override
    public void modelAttribute(@Nullable ModelAttribute modelAttribute, ParameterErrors errors) {
      handle("@ModelAttribute", errors);
    }

    @Override
    public void pathVariable(PathVariable pathVariable, ParameterValidationResult result) {
      handle(pathVariable, result);
    }

    @Override
    public void requestBody(RequestBody requestBody, ParameterErrors errors) {
      handle(requestBody, errors);
    }

    @Override
    public void requestBodyValidationResult(RequestBody requestBody, ParameterValidationResult result) {
      handle(requestBody, result);
    }

    @Override
    public void requestHeader(RequestHeader requestHeader, ParameterValidationResult result) {
      handle(requestHeader, result);
    }

    @Override
    public void requestParam(@Nullable RequestParam requestParam, ParameterValidationResult result) {
      handle("@RequestParam", result);
    }

    @Override
    public void requestPart(RequestPart requestPart, ParameterErrors errors) {
      handle(requestPart, errors);
    }

    @Override
    public void other(ParameterValidationResult result) {
      handle("Other", result);
    }

    private void handle(Annotation annotation, ParameterValidationResult result) {
      handle("@" + annotation.annotationType().getSimpleName(), result);
    }

    private void handle(String tag, ParameterValidationResult result) {
      this.joiner.add(tag + ": " + result.getMethodParameter().getParameterName());
    }
  }

}