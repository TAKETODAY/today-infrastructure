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

package infra.web;

import org.junit.jupiter.api.Test;

import infra.core.DefaultAttributeAccessor;
import infra.web.mock.MockHttpContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatObject;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class MvcRequestStateTests {

  @Test
  void bindingIsSharedByNestedDelegatesAndCanBeReplacedAndCleared() {
    var request = new MockHttpContext();
    var wrapper = new DelegatingHttpContext(new DelegatingHttpContext(request));
    assertThat(BindingContext.has(wrapper)).isFalse();
    assertThat(BindingContext.find(wrapper)).isNull();
    assertThatIllegalStateException().isThrownBy(() -> BindingContext.obtain(wrapper))
            .withMessage("BindingContext is required");
    assertThat(request.getAttributeNames()).isEmpty();

    var original = new BindingContext();
    BindingContext.set(wrapper, original);
    assertThat(BindingContext.has(request)).isTrue();
    assertThat(BindingContext.obtain(request)).isSameAs(original);

    var replacement = new BindingContext();
    BindingContext.set(request, replacement);
    assertThat(BindingContext.obtain(wrapper)).isSameAs(replacement);

    BindingContext.set(wrapper, null);
    assertThat(BindingContext.find(request)).isNull();
  }

  @Test
  void matchingMetadataUsesOnlyTheRequestAttributeContract() {
    HttpContext request = mock(HttpContext.class);
    var attributes = new DefaultAttributeAccessor();
    given(request.getAttribute(anyString())).willAnswer(invocation -> attributes.getAttribute(invocation.getArgument(0)));
    doAnswer(invocation -> {
      attributes.setAttribute(invocation.getArgument(0), invocation.getArgument(1));
      return null;
    }).when(request).setAttribute(anyString(), any());
    var wrapper = new DelegatingHttpContext(new DelegatingHttpContext(request));
    assertThat(HandlerMatchingMetadata.has(wrapper)).isFalse();
    assertThat(HandlerMatchingMetadata.find(wrapper)).isNull();
    assertThatIllegalStateException().isThrownBy(() -> HandlerMatchingMetadata.obtain(wrapper))
            .withMessage("HandlerMatchingMetadata is required");
    assertThat(attributes.getAttributeNames()).isEmpty();

    var original = new HandlerMatchingMetadata(new MockHttpContext());
    HandlerMatchingMetadata.set(request, original);
    assertThat(HandlerMatchingMetadata.has(wrapper)).isTrue();
    assertThat(HandlerMatchingMetadata.obtain(wrapper)).isSameAs(original);

    var replacement = new HandlerMatchingMetadata(new MockHttpContext());
    HandlerMatchingMetadata.set(wrapper, replacement);
    assertThat(HandlerMatchingMetadata.obtain(request)).isSameAs(replacement);

    HandlerMatchingMetadata.set(wrapper, null);
    assertThat(HandlerMatchingMetadata.find(request)).isNull();
  }

  @Test
  void flashMissIsCachedEvenIfAnotherManagerOrAnInputAttributeBecomesAvailable() {
    var request = new MockHttpContext();
    var first = mock(RedirectModelManager.class);
    var second = mock(RedirectModelManager.class);

    assertThatObject(RedirectModel.findInputModel(request, first)).isNull();
    request.setAttribute(RedirectModel.INPUT_ATTRIBUTE, new RedirectModel("late", true));
    assertThatObject(RedirectModel.findInputModel(new DelegatingHttpContext(request), first)).isNull();
    assertThatObject(RedirectModel.findInputModel(request, second)).isNull();
    verify(first).retrieveAndUpdate(request);
    verifyNoInteractions(second);
  }

}
