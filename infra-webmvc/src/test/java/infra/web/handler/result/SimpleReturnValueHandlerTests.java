/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.handler.result;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;

import infra.http.HttpHeaders;
import infra.http.HttpStatus;
import infra.http.MediaType;
import infra.http.ResponseEntity;
import infra.web.BindingContext;
import infra.web.ServerHttpExchange;
import infra.web.handler.SelectableReturnValueHandler;
import infra.web.handler.method.HandlerMethod;
import infra.web.handler.method.support.HttpEntityMethodProcessor;
import infra.web.mock.MockServerHttpExchange;
import infra.web.view.ModelAndView;
import infra.web.view.ViewReturnValueHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

class SimpleReturnValueHandlerTests {

  @Test
  void copiesNonEmptyHeadersOnly() {
    ServerHttpExchange context = mock(ServerHttpExchange.class);
    var handler = new HttpHeadersReturnValueHandler();
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("X-Sample", "value");
    handler.handleReturnValue(context, null, null);
    handler.handleReturnValue(context, null, HttpHeaders.forWritable());
    then(context).shouldHaveNoInteractions();
    handler.handleReturnValue(context, null, headers);
    then(context).should().addHeaders(headers);
  }

  @Test
  void appliesStatusOnlyForHttpStatusResults() {
    ServerHttpExchange context = mock(ServerHttpExchange.class);
    var handler = new HttpStatusReturnValueHandler();
    handler.handleReturnValue(context, null, null);
    then(context).shouldHaveNoInteractions();
    handler.handleReturnValue(context, null, HttpStatus.ACCEPTED);
    then(context).should().setStatus(HttpStatus.ACCEPTED);
  }

  @Test
  void rendersBindingModelForVoidResult() throws Exception {
    ServerHttpExchange context = new MockServerHttpExchange();
    ViewReturnValueHandler renderer = mock(ViewReturnValueHandler.class);
    var handler = new VoidReturnValueHandler(renderer);
    HandlerMethod method = mock(HandlerMethod.class);
    given(method.isReturn(void.class)).willReturn(true);
    assertThat(handler.supportsHandler(method, null)).isTrue();
    assertThat(handler.supportsHandler(new Object(), null)).isTrue();
    assertThat(handler.supportsHandler(method, "value")).isFalse();
    handler.handleReturnValue(context, method, null);
    then(renderer).shouldHaveNoInteractions();

    BindingContext binding = new BindingContext();
    BindingContext.set(context, binding);
    ModelAndView model = binding.getModelAndView();
    model.setViewName("sample");
    handler.handleReturnValue(context, method, null);
    then(renderer).should().renderView(context, model);
  }

  @Test
  void delegatesObjectReturnValueSelection() throws Exception {
    ServerHttpExchange context = mock(ServerHttpExchange.class);
    SelectableReturnValueHandler delegate = mock(SelectableReturnValueHandler.class);
    var handler = new ObjectHandlerMethodReturnValueHandler(delegate);
    HandlerMethod method = mock(HandlerMethod.class);
    given(method.isReturn(Object.class)).willReturn(true);
    given(delegate.supportsReturnValue("result")).willReturn(true);
    assertThat(handler.supportsHandlerMethod(method)).isTrue();
    assertThat(handler.supportsReturnValue("result")).isTrue();
    handler.handleReturnValue(context, method, "result");
    then(delegate).should().handleReturnValue(context, method, "result");
  }

  @Test
  void selectsEmitterForStreamingEntity() throws Exception {
    ServerHttpExchange context = mock(ServerHttpExchange.class);
    HttpEntityMethodProcessor processor = mock(HttpEntityMethodProcessor.class);
    ResponseBodyEmitterReturnValueHandler emitterHandler = mock(ResponseBodyEmitterReturnValueHandler.class);
    var handler = new ResponseEntityReturnValueHandler(processor, emitterHandler);
    ResponseBodyEmitter emitter = mock(ResponseBodyEmitter.class);
    ResponseEntity<ResponseBodyEmitter> entity = ResponseEntity.ok(emitter);
    given(emitterHandler.supportsBodyType(emitter.getClass())).willReturn(true);
    handler.handleReturnValue(context, null, entity);
    then(emitterHandler).should().handleReturnValue(context, null, entity);
    then(processor).should(never()).handleReturnValue(context, null, entity);
  }

  @Test
  void writesImageUsingTheConfiguredFormatAndContentType() throws Exception {
    ServerHttpExchange context = mock(ServerHttpExchange.class);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    given(context.getOutputStream()).willReturn(output);
    var handler = new RenderedImageReturnValueHandler();
    handler.setFormatName("gif");
    handler.setContentType("image/gif");
    assertThat(handler.getFormatName()).isEqualTo("gif");
    assertThat(handler.getContentType()).isEqualTo("image/gif");
    handler.setContentType(MediaType.IMAGE_GIF);

    BufferedImage image = new BufferedImage(2, 3, BufferedImage.TYPE_INT_RGB);
    handler.handleReturnValue(context, null, image);

    then(context).should().setContentType("image/gif");
    assertThat(new String(output.toByteArray(), 0, 3, StandardCharsets.US_ASCII)).isEqualTo("GIF");
    BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(output.toByteArray()));
    assertThat(decoded.getWidth()).isEqualTo(2);
    assertThat(decoded.getHeight()).isEqualTo(3);
  }

  @Test
  void defaultSupportChecksUseTheDeclaredContract() {
    HandlerMethodReturnValueHandler methodHandler = (context, handler, value) -> { };
    assertThat(methodHandler.supportsHandler(mock(HandlerMethod.class))).isFalse();
    SmartReturnValueHandler smart = new SmartReturnValueHandler() {
      @Override
      public boolean supportsReturnValue(Object value) {
        return value instanceof String;
      }

      @Override
      public void handleReturnValue(ServerHttpExchange exchange, Object handler, Object value) {
      }
    };
    assertThat(smart.supportsHandler(new Object(), "sample")).isTrue();
    assertThat(smart.supportsHandler(new Object(), 1)).isFalse();
  }

}
