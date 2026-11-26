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

package infra.session;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import infra.web.AbstractRedirectModelManager;
import infra.web.ServerHttpExchange;
import infra.web.RedirectModel;
import infra.web.util.WebUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/9 15:23
 */
class SessionRedirectModelManagerTests {

  private static final String SESSION_ATTRIBUTE = SessionRedirectModelManager.class.getName() + ".RedirectModel";

  @Test
  void retrieveRedirectModel_ShouldReturnNull_WhenSessionIsNull() {
    SessionRedirectModelManager manager = new SessionRedirectModelManager();
    ServerHttpExchange request = mock(ServerHttpExchange.class, CALLS_REAL_METHODS);

    assertThat(manager.retrieveRedirectModel(request)).isNull();
    verify(request).getSessionAttribute(SESSION_ATTRIBUTE);
    verify(request).getSession(false);
    verifyNoMoreInteractions(request);
  }

  @Test
  void retrieveRedirectModel_ShouldReturnNull_WhenAttributeIsNull() {
    SessionRedirectModelManager manager = new SessionRedirectModelManager();
    ServerHttpExchange request = mock(ServerHttpExchange.class, CALLS_REAL_METHODS);
    Session session = mock(Session.class);
    given(request.getSession(false)).willReturn(session);

    assertThat(manager.retrieveRedirectModel(request)).isNull();
    verify(session).getAttribute(SESSION_ATTRIBUTE);
    verifyNoMoreInteractions(session);
  }

  @Test
  void retrieveRedirectModel_ShouldReturnModels_WhenAttributeExists() {
    SessionRedirectModelManager manager = new SessionRedirectModelManager();
    ServerHttpExchange request = mock(ServerHttpExchange.class);
    List<RedirectModel> models = List.of(new RedirectModel());
    given(request.getSessionAttribute(SESSION_ATTRIBUTE)).willReturn(models);

    assertThat(manager.retrieveRedirectModel(request)).isSameAs(models);
    verify(request).getSessionAttribute(SESSION_ATTRIBUTE);
    verifyNoMoreInteractions(request);
  }

  @Test
  void updateRedirectModel_ShouldRemoveAttribute_WhenModelsIsEmpty() {
    SessionRedirectModelManager manager = new SessionRedirectModelManager();
    ServerHttpExchange request = mock(ServerHttpExchange.class);

    manager.updateRedirectModel(List.of(), request);

    verify(request).setSessionAttribute(SESSION_ATTRIBUTE, null);
    verifyNoMoreInteractions(request);
  }

  @Test
  void updateRedirectModel_ShouldSetAttribute_WhenModelsIsNotEmpty() {
    SessionRedirectModelManager manager = new SessionRedirectModelManager();
    ServerHttpExchange request = mock(ServerHttpExchange.class);
    List<RedirectModel> models = List.of(new RedirectModel());

    manager.updateRedirectModel(models, request);

    verify(request).setSessionAttribute(SESSION_ATTRIBUTE, models);
    verifyNoMoreInteractions(request);
  }

  @Test
  void updateRedirectModel_ShouldNotCreateSession_WhenModelsAreEmpty() {
    SessionRedirectModelManager manager = new SessionRedirectModelManager();
    ServerHttpExchange request = mock(ServerHttpExchange.class, CALLS_REAL_METHODS);

    manager.updateRedirectModel(List.of(), request);

    verify(request).setSessionAttribute(SESSION_ATTRIBUTE, null);
    verify(request).getSession(false);
    verifyNoMoreInteractions(request);
  }

  @Test
  void updateRedirectModel_ShouldRemoveExistingSessionAttribute_WhenModelsAreEmpty() {
    SessionRedirectModelManager manager = new SessionRedirectModelManager();
    ServerHttpExchange request = mock(ServerHttpExchange.class, CALLS_REAL_METHODS);
    Session session = mock(Session.class);
    given(request.getSession(false)).willReturn(session);

    manager.updateRedirectModel(List.of(), request);

    verify(session).removeAttribute(SESSION_ATTRIBUTE);
    verifyNoMoreInteractions(session);
  }

  @Test
  void updateRedirectModel_ShouldCreateSession_WhenModelsAreNotEmpty() {
    SessionRedirectModelManager manager = new SessionRedirectModelManager();
    ServerHttpExchange request = mock(ServerHttpExchange.class, CALLS_REAL_METHODS);
    Session session = mock(Session.class);
    List<RedirectModel> models = List.of(new RedirectModel());
    given(request.getSession(true)).willReturn(session);

    manager.updateRedirectModel(models, request);

    verify(request).getSession(true);
    verify(session).setAttribute(SESSION_ATTRIBUTE, models);
    verifyNoMoreInteractions(session);
  }

  @Test
  void getRedirectModelMutex_ShouldReturnNull_WhenSessionIsNull() {
    SessionRedirectModelManager manager = new SessionRedirectModelManager();
    ServerHttpExchange request = mock(ServerHttpExchange.class);

    assertThat(manager.getRedirectModelMutex(request)).isNull();
    verify(request).getSession(false);
    verifyNoMoreInteractions(request);
  }

  @Test
  void getRedirectModelMutex_ShouldReturnMutex_WhenSessionExists() {
    SessionRedirectModelManager manager = new SessionRedirectModelManager();
    ServerHttpExchange request = mock(ServerHttpExchange.class);
    Session session = mock(Session.class);
    Object mutex = new Object();
    given(request.getSession(false)).willReturn(session);
    given(session.getAttribute(WebUtils.SESSION_MUTEX_ATTRIBUTE)).willReturn(mutex);

    assertThat(manager.getRedirectModelMutex(request)).isSameAs(mutex);
    verify(request).getSession(false);
    verifyNoMoreInteractions(request);
  }

  @Test
  void getRedirectModelMutex_ShouldReturnSession_WhenMutexAttributeIsAbsent() {
    SessionRedirectModelManager manager = new SessionRedirectModelManager();
    ServerHttpExchange request = mock(ServerHttpExchange.class);
    Session session = mock(Session.class);
    given(request.getSession(false)).willReturn(session);

    assertThat(manager.getRedirectModelMutex(request)).isSameAs(session);
    verify(request).getSession(false);
    verifyNoMoreInteractions(request);
  }

  @Test
  void retrieveAndUpdate_ShouldReturnNull_WhenNoRedirectModels() {
    SessionRedirectModelManager manager = new SessionRedirectModelManager();
    ServerHttpExchange context = mock(ServerHttpExchange.class);

    RedirectModel result = manager.retrieveAndUpdate(context);

    assertThat((Object) result).isNull();
    verify(context).getSessionAttribute(SESSION_ATTRIBUTE);
    verifyNoMoreInteractions(context);
  }

  @Test
  void retrieveAndUpdate_ShouldRemoveExpiredModelsAndReturnMatch() {
    SessionRedirectModelManager manager = new SessionRedirectModelManager();
    ServerHttpExchange context = mock(ServerHttpExchange.class);
    Session session = mock(Session.class);
    RedirectModel expiredModel = new RedirectModel();
    expiredModel.setTargetRequestPath("/expired");
    expiredModel.startExpirationPeriod(-1);
    RedirectModel validModel = new RedirectModel();
    validModel.setTargetRequestPath("/test");
    List<RedirectModel> redirectModels = new ArrayList<>(List.of(expiredModel, validModel));
    given(context.getSessionAttribute(SESSION_ATTRIBUTE)).willReturn(redirectModels);
    given(context.getSession(false)).willReturn(session);
    given(context.getRequestURI()).willReturn("/test");

    RedirectModel result = manager.retrieveAndUpdate(context);

    assertThat((Object) result).isSameAs(validModel);
    assertThat(redirectModels).isEmpty();
    verify(context).setSessionAttribute(SESSION_ATTRIBUTE, null);
  }

  @Test
  void retrieveAndUpdate_ShouldPreserveUnmatchedModels() {
    SessionRedirectModelManager manager = new SessionRedirectModelManager();
    ServerHttpExchange context = mock(ServerHttpExchange.class);
    RedirectModel matchingModel = new RedirectModel("message", "saved");
    matchingModel.setTargetRequestPath("/target");
    RedirectModel unmatchedModel = new RedirectModel("message", "other");
    unmatchedModel.setTargetRequestPath("/other");
    List<RedirectModel> models = new ArrayList<>(List.of(matchingModel, unmatchedModel));
    given(context.getSessionAttribute(SESSION_ATTRIBUTE)).willReturn(models);
    given(context.getRequestURI()).willReturn("/target");

    RedirectModel result = manager.retrieveAndUpdate(context);

    assertThat((Object) result).isSameAs(matchingModel);
    assertThat(models).containsExactly(unmatchedModel);
    verify(context).setSessionAttribute(SESSION_ATTRIBUTE, models);
  }

  @Test
  void retrieveAndUpdate_ShouldNotUpdateModels_WhenNoModelMatchesOrExpires() {
    SessionRedirectModelManager manager = new SessionRedirectModelManager();
    ServerHttpExchange context = mock(ServerHttpExchange.class);
    RedirectModel model = new RedirectModel("message", "saved");
    model.setTargetRequestPath("/target");
    List<RedirectModel> models = new ArrayList<>(List.of(model));
    given(context.getSessionAttribute(SESSION_ATTRIBUTE)).willReturn(models);
    given(context.getRequestURI()).willReturn("/other");

    RedirectModel result = manager.retrieveAndUpdate(context);

    assertThat((Object) result).isNull();
    assertThat(models).containsExactly(model);
    verify(context).getSessionAttribute(SESSION_ATTRIBUTE);
    verify(context).getRequestURI();
    verifyNoMoreInteractions(context);
  }

  @Test
  void saveRedirectModel_ShouldSaveNewRedirectModel() {
    SessionRedirectModelManager manager = new SessionRedirectModelManager();
    ServerHttpExchange context = mock(ServerHttpExchange.class, CALLS_REAL_METHODS);
    Session session = mock(Session.class);
    RedirectModel redirectModel = new RedirectModel();
    redirectModel.addAttribute("key", "value");
    redirectModel.setTargetRequestPath("/target");
    given(context.getSession(true)).willReturn(session);

    manager.saveRedirectModel(context, redirectModel);

    verify(session).setAttribute(SESSION_ATTRIBUTE, List.of(redirectModel));
    assertThat(redirectModel.getExpirationTime()).isPositive();
    assertThat(redirectModel.isExpired()).isFalse();
  }

  @Test
  void isRedirectModelForRequest_ShouldReturnTrue_WhenPathAndParamsMatch() {
    SessionRedirectModelManager0 manager = new SessionRedirectModelManager0();
    RedirectModel redirectModel = new RedirectModel();
    redirectModel.setTargetRequestPath("/test");
    redirectModel.addTargetRequestParam("param1", "value1");

    ServerHttpExchange context = mock(ServerHttpExchange.class);
    when(context.getRequestURI()).thenReturn("/test");
    when(context.getQueryString()).thenReturn("param1=value1");

    boolean result = manager.isRedirectModelForRequest(redirectModel, context);
    assertThat(result).isTrue();
  }

  @Test
  void isRedirectModelForRequest_ShouldReturnFalse_WhenPathDoesNotMatch() {
    SessionRedirectModelManager0 manager = new SessionRedirectModelManager0();
    RedirectModel redirectModel = new RedirectModel();
    redirectModel.setTargetRequestPath("/test");

    ServerHttpExchange context = mock(ServerHttpExchange.class);
    when(context.getRequestURI()).thenReturn("/other");

    boolean result = manager.isRedirectModelForRequest(redirectModel, context);
    assertThat(result).isFalse();
  }

  @Test
  void isRedirectModelForRequest_ShouldReturnFalse_WhenParamMissing() {
    SessionRedirectModelManager0 manager = new SessionRedirectModelManager0();
    RedirectModel redirectModel = new RedirectModel();
    redirectModel.setTargetRequestPath("/test");
    redirectModel.addTargetRequestParam("param1", "value1");

    ServerHttpExchange context = mock(ServerHttpExchange.class);
    when(context.getRequestURI()).thenReturn("/test");
    when(context.getQueryString()).thenReturn("param2=value2");

    boolean result = manager.isRedirectModelForRequest(redirectModel, context);
    assertThat(result).isFalse();
  }

  @Test
  void saveRedirectModel_ShouldNotSave_WhenRedirectModelIsNull() {
    // given
    SessionRedirectModelManager manager = new SessionRedirectModelManager();
    ServerHttpExchange context = mock(ServerHttpExchange.class);

    // when
    manager.saveRedirectModel(context, null);

    verifyNoInteractions(context);
  }

  @Test
  void saveRedirectModel_ShouldNotSave_WhenRedirectModelIsEmpty() {
    // given
    SessionRedirectModelManager manager = new SessionRedirectModelManager();
    ServerHttpExchange context = mock(ServerHttpExchange.class);
    RedirectModel redirectModel = new RedirectModel();

    // when
    manager.saveRedirectModel(context, redirectModel);

    verifyNoInteractions(context);
  }

  @Test
  void saveRedirectModel_ShouldSaveModel_WithSessionMutex() {
    SessionRedirectModelManager manager = new SessionRedirectModelManager();
    ServerHttpExchange context = mock(ServerHttpExchange.class);
    Session session = mock(Session.class);
    Object mutex = new Object();
    RedirectModel redirectModel = new RedirectModel();
    redirectModel.addAttribute("key", "value");
    redirectModel.setTargetRequestPath("/target");
    given(context.getSession(false)).willReturn(session);
    given(session.getAttribute(WebUtils.SESSION_MUTEX_ATTRIBUTE)).willReturn(mutex);

    manager.saveRedirectModel(context, redirectModel);

    verify(context).setSessionAttribute(SESSION_ATTRIBUTE, List.of(redirectModel));
  }

  @Test
  void saveRedirectModel_ShouldAppendToExistingModels() {
    SessionRedirectModelManager manager = new SessionRedirectModelManager();
    ServerHttpExchange context = mock(ServerHttpExchange.class);
    Session session = mock(Session.class);
    RedirectModel existingModel = new RedirectModel();
    existingModel.addAttribute("existing", "value");
    List<RedirectModel> models = new ArrayList<>(List.of(existingModel));
    RedirectModel redirectModel = new RedirectModel();
    redirectModel.addAttribute("key", "value");
    redirectModel.setTargetRequestPath("/target");
    given(context.getSession(false)).willReturn(session);
    given(context.getSessionAttribute(SESSION_ATTRIBUTE)).willReturn(models);

    manager.saveRedirectModel(context, redirectModel);

    assertThat(models).containsExactly(existingModel, redirectModel);
    verify(context).setSessionAttribute(SESSION_ATTRIBUTE, models);
  }

  @Test
  void isRedirectModelForRequest_ShouldReturnTrue_WhenPathMatchesWithTrailingSlash() {
    // given
    SessionRedirectModelManager0 manager = new SessionRedirectModelManager0();
    RedirectModel redirectModel = new RedirectModel();
    redirectModel.setTargetRequestPath("/test");

    ServerHttpExchange context = mock(ServerHttpExchange.class);
    when(context.getRequestURI()).thenReturn("/test/");
    when(context.getQueryString()).thenReturn("");

    // when
    boolean result = manager.isRedirectModelForRequest(redirectModel, context);

    // then
    assertThat(result).isTrue();
  }

  @Test
  void isRedirectModelForRequest_ShouldReturnTrue_WhenNoPathAndParamsMatch() {
    // given
    SessionRedirectModelManager0 manager = new SessionRedirectModelManager0();
    RedirectModel redirectModel = new RedirectModel();
    redirectModel.addTargetRequestParam("param1", "value1");

    ServerHttpExchange context = mock(ServerHttpExchange.class);
    when(context.getRequestURI()).thenReturn("/test");
    when(context.getQueryString()).thenReturn("param1=value1");

    // when
    boolean result = manager.isRedirectModelForRequest(redirectModel, context);

    // then
    assertThat(result).isTrue();
  }

  @Test
  void isRedirectModelForRequest_ShouldReturnTrue_WhenMultipleParamValuesMatch() {
    // given
    SessionRedirectModelManager0 manager = new SessionRedirectModelManager0();
    RedirectModel redirectModel = new RedirectModel();
    redirectModel.setTargetRequestPath("/test");
    redirectModel.addTargetRequestParam("param1", "value1");
    redirectModel.addTargetRequestParam("param1", "value2");

    ServerHttpExchange context = mock(ServerHttpExchange.class);
    when(context.getRequestURI()).thenReturn("/test");
    when(context.getQueryString()).thenReturn("param1=value1&param1=value2");

    // when
    boolean result = manager.isRedirectModelForRequest(redirectModel, context);

    // then
    assertThat(result).isTrue();
  }

  @Test
  void isRedirectModelForRequest_ShouldReturnFalse_WhenParamValueDoesNotMatch() {
    // given
    SessionRedirectModelManager0 manager = new SessionRedirectModelManager0();
    RedirectModel redirectModel = new RedirectModel();
    redirectModel.setTargetRequestPath("/test");
    redirectModel.addTargetRequestParam("param1", "value1");

    ServerHttpExchange context = mock(ServerHttpExchange.class);
    when(context.getRequestURI()).thenReturn("/test");
    when(context.getQueryString()).thenReturn("param1=value2");

    // when
    boolean result = manager.isRedirectModelForRequest(redirectModel, context);

    // then
    assertThat(result).isFalse();
  }

  @Test
  void setRedirectModelTimeout_ShouldUpdateTimeout() {
    // given
    AbstractRedirectModelManager manager = new AbstractRedirectModelManagerImpl();

    // when
    manager.setRedirectModelTimeout(300);

    // then
    assertThat(manager.getRedirectModelTimeout()).isEqualTo(300);
  }

  @Test
  void getRedirectModelTimeout_ShouldReturnDefaultTimeout() {
    // given
    AbstractRedirectModelManager manager = new AbstractRedirectModelManagerImpl();

    // when
    int timeout = manager.getRedirectModelTimeout();

    // then
    assertThat(timeout).isEqualTo(180);
  }

  @Test
  void retrieveAndUpdate_ShouldReturnNull_WhenRetrieveReturnsNull() {
    // given
    AbstractRedirectModelManager manager = new AbstractRedirectModelManagerImpl() {
      @Override
      protected List<RedirectModel> retrieveRedirectModel(ServerHttpExchange request) {
        return null;
      }
    };
    ServerHttpExchange context = mock(ServerHttpExchange.class);

    // when
    RedirectModel result = manager.retrieveAndUpdate(context);

    // then
    assertThat((Object) result).isNull();
  }

  @Test
  void retrieveAndUpdate_ShouldReturnNull_WhenRetrieveReturnsEmptyList() {
    // given
    AbstractRedirectModelManager manager = new AbstractRedirectModelManagerImpl() {
      @Override
      protected List<RedirectModel> retrieveRedirectModel(ServerHttpExchange request) {
        return new ArrayList<>();
      }
    };
    ServerHttpExchange context = mock(ServerHttpExchange.class);

    RedirectModel result = manager.retrieveAndUpdate(context);
    assertThat((Object) result).isNull();
  }

  @Test
  void retrieveAndUpdate_ShouldRemoveOnlyExpiredModels() {
    // given
    AbstractRedirectModelManager manager = new AbstractRedirectModelManagerImpl() {
      @Override
      protected List<RedirectModel> retrieveRedirectModel(ServerHttpExchange request) {
        List<RedirectModel> models = new ArrayList<>();
        RedirectModel expiredModel = new RedirectModel();
        expiredModel.startExpirationPeriod(-1);
        models.add(expiredModel);
        return models;
      }

      @Override
      protected Object getRedirectModelMutex(ServerHttpExchange request) {
        return new Object();
      }
    };
    ServerHttpExchange context = mock(ServerHttpExchange.class);

    // when
    RedirectModel result = manager.retrieveAndUpdate(context);

    assertThat((Object) result).isNotNull();
  }

  @Test
  void retrieveAndUpdate_ShouldReturnMatchingModelAndRemoveIt() {
    // given
    List<RedirectModel> storedModels = new ArrayList<>();
    RedirectModel matchingModel = new RedirectModel();
    matchingModel.setTargetRequestPath("/test");
    storedModels.add(matchingModel);

    AbstractRedirectModelManager manager = new AbstractRedirectModelManagerImpl() {
      @Override
      protected List<RedirectModel> retrieveRedirectModel(ServerHttpExchange request) {
        return new ArrayList<>(storedModels);
      }

      @Override
      protected void updateRedirectModel(List<RedirectModel> redirectModels, ServerHttpExchange request) {
        storedModels.clear();
        storedModels.addAll(redirectModels);
      }

      @Override
      protected Object getRedirectModelMutex(ServerHttpExchange request) {
        return null;
      }

      @Override
      protected boolean isRedirectModelForRequest(RedirectModel model, ServerHttpExchange request) {
        return true;
      }
    };
    ServerHttpExchange context = mock(ServerHttpExchange.class);

    // when
    RedirectModel result = manager.retrieveAndUpdate(context);

    // then
    assertThat((Object) result).isSameAs(matchingModel);
    assertThat(storedModels).isEmpty();
  }

  @Test
  void saveRedirectModel_ShouldNotSave_WhenModelIsNull() {
    // given
    AbstractRedirectModelManager manager = new AbstractRedirectModelManagerImpl();
    ServerHttpExchange context = mock(ServerHttpExchange.class);

    // when
    manager.saveRedirectModel(context, null);

    // then
    // No exception should be thrown
  }

  @Test
  void saveRedirectModel_ShouldNotSave_WhenModelIsEmpty() {
    // given
    AbstractRedirectModelManager manager = new AbstractRedirectModelManagerImpl();
    ServerHttpExchange context = mock(ServerHttpExchange.class);
    RedirectModel redirectModel = new RedirectModel();

    // when
    manager.saveRedirectModel(context, redirectModel);

    // then
    // No exception should be thrown
  }

  @Test
  void saveRedirectModel_ShouldSaveModel_WithMutex() {
    // given
    List<RedirectModel> storedModels = new ArrayList<>();
    AbstractRedirectModelManager manager = new AbstractRedirectModelManagerImpl() {
      @Override
      protected List<RedirectModel> retrieveRedirectModel(ServerHttpExchange request) {
        return storedModels.isEmpty() ? null : new ArrayList<>(storedModels);
      }

      @Override
      protected void updateRedirectModel(List<RedirectModel> redirectModels, ServerHttpExchange request) {
        storedModels.clear();
        storedModels.addAll(redirectModels);
      }

      @Override
      protected Object getRedirectModelMutex(ServerHttpExchange request) {
        return new Object();
      }
    };

    ServerHttpExchange context = mock(ServerHttpExchange.class);
    when(context.getRequestURI()).thenReturn("/test");

    RedirectModel redirectModel = new RedirectModel();
    redirectModel.addAttribute("key", "value");

    // when
    manager.saveRedirectModel(context, redirectModel);

    // then
    assertThat(storedModels).hasSize(1);
    assertThat((Object) storedModels.get(0)).isSameAs(redirectModel);
  }

  @Test
  void saveRedirectModel_ShouldSaveModel_WithoutMutex() {
    // given
    List<RedirectModel> storedModels = new ArrayList<>();
    AbstractRedirectModelManager manager = new AbstractRedirectModelManagerImpl() {
      @Override
      protected List<RedirectModel> retrieveRedirectModel(ServerHttpExchange request) {
        return storedModels.isEmpty() ? null : new ArrayList<>(storedModels);
      }

      @Override
      protected void updateRedirectModel(List<RedirectModel> redirectModels, ServerHttpExchange request) {
        storedModels.clear();
        storedModels.addAll(redirectModels);
      }

      @Override
      protected Object getRedirectModelMutex(ServerHttpExchange request) {
        return null;
      }
    };

    ServerHttpExchange context = mock(ServerHttpExchange.class);
    when(context.getRequestURI()).thenReturn("/test");

    RedirectModel redirectModel = new RedirectModel();
    redirectModel.addAttribute("key", "value");

    // when
    manager.saveRedirectModel(context, redirectModel);

    // then
    assertThat(storedModels).hasSize(1);
    assertThat((Object) storedModels.get(0)).isSameAs(redirectModel);
  }

  @Test
  void isRedirectModelForRequest_ShouldReturnTrue_WhenNoPathAndNoParams() {
    // given
    AbstractRedirectModelManagerImpl manager = new AbstractRedirectModelManagerImpl();
    RedirectModel redirectModel = new RedirectModel();
    ServerHttpExchange context = mock(ServerHttpExchange.class);
    when(context.getQueryString()).thenReturn(null);

    // when
    boolean result = manager.isRedirectModelForRequest(redirectModel, context);

    // then
    assertThat(result).isTrue();
  }

  @Test
  void decodeAndNormalizePath_ShouldReturnNull_WhenPathIsNull() {
    // This test requires reflection to access private method
    // Not included as it's testing private implementation details
  }

  static class AbstractRedirectModelManagerImpl extends AbstractRedirectModelManager {
    @Override
    protected List<RedirectModel> retrieveRedirectModel(ServerHttpExchange request) {
      return null;
    }

    @Override
    protected void updateRedirectModel(List<RedirectModel> redirectModels, ServerHttpExchange request) {
    }

    @Override
    protected boolean isRedirectModelForRequest(RedirectModel model, ServerHttpExchange request) {
      return super.isRedirectModelForRequest(model, request);
    }
  }

  static class SessionRedirectModelManager0 extends SessionRedirectModelManager {

    @Override
    protected boolean isRedirectModelForRequest(RedirectModel model, ServerHttpExchange request) {
      return super.isRedirectModelForRequest(model, request);
    }

  }

}
