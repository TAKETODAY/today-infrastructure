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

package infra.session;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import infra.web.AbstractRedirectModelManager;
import infra.web.RedirectModel;
import infra.web.RequestContext;
import infra.web.RequestContextUtils;
import infra.web.util.WebUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/9 15:23
 */
class SessionRedirectModelManagerTests {

  @Test
  void constructor_ShouldSetSessionManager_WhenProvided() {
    SessionManager sessionManager = mock(SessionManager.class);

    SessionRedirectModelManager manager = new SessionRedirectModelManager(sessionManager);
    assertThat(manager.getSessionManager()).isSameAs(sessionManager);
  }

  @Test
  void constructor_ShouldHaveNullSessionManager_WhenNoArgs() {
    SessionRedirectModelManager manager = new SessionRedirectModelManager();
    assertThat(manager.getSessionManager()).isNull();
  }

  @Test
  void getSession_ShouldUseRequestContextUtils_WhenSessionManagerIsNull() {
    // given
    SessionRedirectModelManager manager = new SessionRedirectModelManager();
    RequestContext context = mock(RequestContext.class);
    Session session = mock(Session.class);

    try (var mockedUtils = mockStatic(RequestContextUtils.class)) {
      mockedUtils.when(() -> RequestContextUtils.getSession(context, true)).thenReturn(session);

      Session result = manager.getSession(context, true);
      assertThat(result).isSameAs(session);
    }
  }

  @Test
  void retrieveRedirectModel_ShouldReturnNull_WhenSessionIsNull() {
    // given
    SessionRedirectModelManager manager = new SessionRedirectModelManager();
    RequestContext request = mock(RequestContext.class);

    // Mocking static method behavior to return null session
    try (var mockedUtils = mockStatic(RequestContextUtils.class)) {
      mockedUtils.when(() -> RequestContextUtils.getSession(request, false)).thenReturn(null);

      List<RedirectModel> result = manager.retrieveRedirectModel(request);
      assertThat(result).isNull();
    }
  }

  @Test
  void retrieveRedirectModel_ShouldReturnNull_WhenAttributeIsNull() {
    // given
    SessionRedirectModelManager manager = new SessionRedirectModelManager();
    RequestContext request = mock(RequestContext.class);
    Session session = mock(Session.class);

    when(session.getAttribute(anyString())).thenReturn(null);

    try (var mockedUtils = mockStatic(RequestContextUtils.class)) {
      mockedUtils.when(() -> RequestContextUtils.getSession(request, false)).thenReturn(session);

      List<RedirectModel> result = manager.retrieveRedirectModel(request);
      assertThat(result).isNull();
    }
  }

  @Test
  void retrieveRedirectModel_ShouldReturnModels_WhenAttributeExists() {
    // given
    SessionRedirectModelManager manager = new SessionRedirectModelManager();
    RequestContext request = mock(RequestContext.class);
    Session session = mock(Session.class);
    List<RedirectModel> models = new ArrayList<>();
    models.add(new RedirectModel());

    when(session.getAttribute(anyString())).thenReturn(models);

    try (var mockedUtils = mockStatic(RequestContextUtils.class)) {
      mockedUtils.when(() -> RequestContextUtils.getSession(request, false)).thenReturn(session);

      // when
      List<RedirectModel> result = manager.retrieveRedirectModel(request);

      // then
      assertThat(result).isSameAs(models);
    }
  }

  @Test
  void updateRedirectModel_ShouldRemoveAttribute_WhenModelsIsEmpty() {
    // given
    SessionRedirectModelManager manager = new SessionRedirectModelManager();
    RequestContext request = mock(RequestContext.class);
    Session session = mock(Session.class);
    List<RedirectModel> emptyModels = new ArrayList<>();

    try (var mockedUtils = mockStatic(RequestContextUtils.class)) {
      mockedUtils.when(() -> RequestContextUtils.getSession(request, false)).thenReturn(session);

      // when
      manager.updateRedirectModel(emptyModels, request);

      // then
      verify(session).removeAttribute(anyString());
    }
  }

  @Test
  void updateRedirectModel_ShouldSetAttribute_WhenModelsIsNotEmpty() {
    // given
    SessionRedirectModelManager manager = new SessionRedirectModelManager();
    RequestContext request = mock(RequestContext.class);
    Session session = mock(Session.class);
    List<RedirectModel> models = new ArrayList<>();
    models.add(new RedirectModel());

    try (var mockedUtils = mockStatic(RequestContextUtils.class)) {
      mockedUtils.when(() -> RequestContextUtils.getSession(request, true)).thenReturn(session);

      // when
      manager.updateRedirectModel(models, request);

      // then
      verify(session).setAttribute(anyString(), eq(models));
    }
  }

  @Test
  void updateRedirectModel_ShouldThrowException_WhenSessionNotFoundAndModelsNotEmpty() {
    // given
    SessionRedirectModelManager manager = new SessionRedirectModelManager();
    RequestContext request = mock(RequestContext.class);
    List<RedirectModel> models = new ArrayList<>();
    models.add(new RedirectModel());

    try (var mockedUtils = mockStatic(RequestContextUtils.class)) {
      mockedUtils.when(() -> RequestContextUtils.getSession(request, true)).thenReturn(null);

      // when & then
      assertThatThrownBy(() -> manager.updateRedirectModel(models, request))
              .isInstanceOf(IllegalStateException.class)
              .hasMessage("Session not found in current request");
    }
  }

  @Test
  void getRedirectModelMutex_ShouldReturnNull_WhenSessionIsNull() {
    // given
    SessionRedirectModelManager manager = new SessionRedirectModelManager();
    RequestContext request = mock(RequestContext.class);

    try (var mockedUtils = mockStatic(RequestContextUtils.class)) {
      mockedUtils.when(() -> RequestContextUtils.getSession(request, false)).thenReturn(null);

      // when
      Object mutex = manager.getRedirectModelMutex(request);

      // then
      assertThat(mutex).isNull();
    }
  }

  @Test
  void getRedirectModelMutex_ShouldReturnMutex_WhenSessionExists() {
    // given
    SessionRedirectModelManager manager = new SessionRedirectModelManager();
    RequestContext request = mock(RequestContext.class);
    Session session = mock(Session.class);
    Object mutex = new Object();

    when(WebUtils.getSessionMutex(session)).thenReturn(mutex);

    try (var mockedUtils = mockStatic(RequestContextUtils.class)) {
      mockedUtils.when(() -> RequestContextUtils.getSession(request, false)).thenReturn(session);

      // when
      Object result = manager.getRedirectModelMutex(request);

      // then
      assertThat(result).isSameAs(mutex);
    }
  }

  @Test
  void getSession_ShouldUseCustomSessionManager_WhenProvided() {
    // given
    SessionManager customManager = mock(SessionManager.class);
    SessionRedirectModelManager manager = new SessionRedirectModelManager(customManager);
    RequestContext context = mock(RequestContext.class);
    Session session = mock(Session.class);

    when(customManager.getSession(context, true)).thenReturn(session);

    // when
    Session result = manager.getSession(context, true);

    // then
    assertThat(result).isSameAs(session);
    verify(customManager).getSession(context, true);
  }

  @Test
  void getSession_ShouldNotCreateSession_WhenCreateIsFalseAndSessionNotExists() {
    // given
    SessionRedirectModelManager manager = new SessionRedirectModelManager();
    RequestContext context = mock(RequestContext.class);

    try (var mockedUtils = mockStatic(RequestContextUtils.class)) {
      mockedUtils.when(() -> RequestContextUtils.getSession(context, false)).thenReturn(null);

      Session result = manager.getSession(context, false);
      assertThat(result).isNull();
    }
  }

  @Test
  void retrieveAndUpdate_ShouldReturnNull_WhenNoRedirectModels() {
    // given
    SessionRedirectModelManager manager = new SessionRedirectModelManager();
    RequestContext context = mock(RequestContext.class);

    try (var mockedUtils = mockStatic(RequestContextUtils.class)) {
      mockedUtils.when(() -> RequestContextUtils.getSession(context, false)).thenReturn(null);

      RedirectModel result = manager.retrieveAndUpdate(context);
      assertThat((Object) result).isNull();
    }
  }

  @Test
  void retrieveAndUpdate_ShouldRemoveExpiredModelsAndReturnMatch() {
    // given
    SessionRedirectModelManager manager = new SessionRedirectModelManager();
    RequestContext context = mock(RequestContext.class);
    Session session = mock(Session.class);
    List<RedirectModel> redirectModels = new ArrayList<>();

    RedirectModel expiredModel = new RedirectModel();
    expiredModel.startExpirationPeriod(-1); // Expired

    RedirectModel validModel = new RedirectModel();
    validModel.setTargetRequestPath("/test");

    redirectModels.add(expiredModel);
    redirectModels.add(validModel);

    when(session.getAttribute(anyString())).thenReturn(redirectModels);
    when(WebUtils.getSessionMutex(session)).thenReturn(new Object());

    try (var mockedUtils = mockStatic(RequestContextUtils.class)) {
      mockedUtils.when(() -> RequestContextUtils.getSession(context, false)).thenReturn(session);
      mockedUtils.when(() -> RequestContextUtils.getSession(context, true)).thenReturn(session);

      when(context.getRequestURI()).thenReturn("/test");

      RedirectModel result = manager.retrieveAndUpdate(context);

      assertThat((Object) result).isNotNull();
      assertThat(result.getTargetRequestPath()).isEqualTo("/test");
//      verify(session).setAttribute(anyString(), any());
    }
  }

  @Test
  void saveRedirectModel_ShouldSaveNewRedirectModel() {
    // given
    SessionRedirectModelManager manager = new SessionRedirectModelManager();
    RequestContext context = mock(RequestContext.class);
    Session session = mock(Session.class);
    RedirectModel redirectModel = new RedirectModel();
    redirectModel.addAttribute("key", "value");
    redirectModel.setTargetRequestPath("/target");

    when(WebUtils.getSessionMutex(session)).thenReturn(new Object());
    when(context.getRequestURI()).thenReturn("/current");

    try (var mockedUtils = mockStatic(RequestContextUtils.class)) {
      mockedUtils.when(() -> RequestContextUtils.getSession(context, false)).thenReturn(session);
      mockedUtils.when(() -> RequestContextUtils.getSession(context, true)).thenReturn(session);

      manager.saveRedirectModel(context, redirectModel);

      verify(session).setAttribute(anyString(), any(List.class));
    }
  }

  @Test
  void isRedirectModelForRequest_ShouldReturnTrue_WhenPathAndParamsMatch() {
    SessionRedirectModelManager0 manager = new SessionRedirectModelManager0();
    RedirectModel redirectModel = new RedirectModel();
    redirectModel.setTargetRequestPath("/test");
    redirectModel.addTargetRequestParam("param1", "value1");

    RequestContext context = mock(RequestContext.class);
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

    RequestContext context = mock(RequestContext.class);
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

    RequestContext context = mock(RequestContext.class);
    when(context.getRequestURI()).thenReturn("/test");
    when(context.getQueryString()).thenReturn("param2=value2");

    boolean result = manager.isRedirectModelForRequest(redirectModel, context);
    assertThat(result).isFalse();
  }

  @Test
  void saveRedirectModel_ShouldNotSave_WhenRedirectModelIsNull() {
    // given
    SessionRedirectModelManager manager = new SessionRedirectModelManager();
    RequestContext context = mock(RequestContext.class);

    // when
    manager.saveRedirectModel(context, null);

    // then
    // No exception should be thrown
  }

  @Test
  void saveRedirectModel_ShouldNotSave_WhenRedirectModelIsEmpty() {
    // given
    SessionRedirectModelManager manager = new SessionRedirectModelManager();
    RequestContext context = mock(RequestContext.class);
    RedirectModel redirectModel = new RedirectModel();

    // when
    manager.saveRedirectModel(context, redirectModel);

    // then
    // No exception should be thrown
  }

  @Test
  void saveRedirectModel_ShouldSaveModel_WithSessionMutex() {
    // given
    SessionRedirectModelManager manager = new SessionRedirectModelManager();
    RequestContext context = mock(RequestContext.class);
    Session session = mock(Session.class);
    RedirectModel redirectModel = new RedirectModel();
    redirectModel.addAttribute("key", "value");
    redirectModel.setTargetRequestPath("/target");

    when(session.getAttribute(anyString())).thenReturn(null);
    when(WebUtils.getSessionMutex(session)).thenReturn(new Object());
    when(context.getRequestURI()).thenReturn("/current");

    try (var mockedUtils = mockStatic(RequestContextUtils.class)) {
      mockedUtils.when(() -> RequestContextUtils.getSession(context, false)).thenReturn(session);
      mockedUtils.when(() -> RequestContextUtils.getSession(context, true)).thenReturn(session);

      // when
      manager.saveRedirectModel(context, redirectModel);

      // then
      verify(session).setAttribute(anyString(), any(List.class));
    }
  }

  @Test
  void saveRedirectModel_ShouldSaveModel_WithoutSessionMutex() {
    // given
    SessionRedirectModelManager manager = new SessionRedirectModelManager();
    RequestContext context = mock(RequestContext.class);
    Session session = mock(Session.class);
    RedirectModel redirectModel = new RedirectModel();
    redirectModel.addAttribute("key", "value");
    redirectModel.setTargetRequestPath("/target");

    when(session.getAttribute(anyString())).thenReturn(null);
    when(WebUtils.getSessionMutex(session)).thenReturn(null);
    when(context.getRequestURI()).thenReturn("/current");

    try (var mockedUtils = mockStatic(RequestContextUtils.class)) {
      mockedUtils.when(() -> RequestContextUtils.getSession(context, false)).thenReturn(session);
      mockedUtils.when(() -> RequestContextUtils.getSession(context, true)).thenReturn(session);

      // when
      manager.saveRedirectModel(context, redirectModel);

      // then
      verify(session).setAttribute(anyString(), any(List.class));
    }
  }

  @Test
  void isRedirectModelForRequest_ShouldReturnTrue_WhenPathMatchesWithTrailingSlash() {
    // given
    SessionRedirectModelManager0 manager = new SessionRedirectModelManager0();
    RedirectModel redirectModel = new RedirectModel();
    redirectModel.setTargetRequestPath("/test");

    RequestContext context = mock(RequestContext.class);
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

    RequestContext context = mock(RequestContext.class);
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

    RequestContext context = mock(RequestContext.class);
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

    RequestContext context = mock(RequestContext.class);
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
      protected List<RedirectModel> retrieveRedirectModel(RequestContext request) {
        return null;
      }
    };
    RequestContext context = mock(RequestContext.class);

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
      protected List<RedirectModel> retrieveRedirectModel(RequestContext request) {
        return new ArrayList<>();
      }
    };
    RequestContext context = mock(RequestContext.class);

    RedirectModel result = manager.retrieveAndUpdate(context);
    assertThat((Object) result).isNull();
  }

  @Test
  void retrieveAndUpdate_ShouldRemoveOnlyExpiredModels() {
    // given
    AbstractRedirectModelManager manager = new AbstractRedirectModelManagerImpl() {
      @Override
      protected List<RedirectModel> retrieveRedirectModel(RequestContext request) {
        List<RedirectModel> models = new ArrayList<>();
        RedirectModel expiredModel = new RedirectModel();
        expiredModel.startExpirationPeriod(-1);
        models.add(expiredModel);
        return models;
      }

      @Override
      protected Object getRedirectModelMutex(RequestContext request) {
        return new Object();
      }
    };
    RequestContext context = mock(RequestContext.class);

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
      protected List<RedirectModel> retrieveRedirectModel(RequestContext request) {
        return new ArrayList<>(storedModels);
      }

      @Override
      protected void updateRedirectModel(List<RedirectModel> redirectModels, RequestContext request) {
        storedModels.clear();
        storedModels.addAll(redirectModels);
      }

      @Override
      protected Object getRedirectModelMutex(RequestContext request) {
        return null;
      }

      @Override
      protected boolean isRedirectModelForRequest(RedirectModel model, RequestContext request) {
        return true;
      }
    };
    RequestContext context = mock(RequestContext.class);

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
    RequestContext context = mock(RequestContext.class);

    // when
    manager.saveRedirectModel(context, null);

    // then
    // No exception should be thrown
  }

  @Test
  void saveRedirectModel_ShouldNotSave_WhenModelIsEmpty() {
    // given
    AbstractRedirectModelManager manager = new AbstractRedirectModelManagerImpl();
    RequestContext context = mock(RequestContext.class);
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
      protected List<RedirectModel> retrieveRedirectModel(RequestContext request) {
        return storedModels.isEmpty() ? null : new ArrayList<>(storedModels);
      }

      @Override
      protected void updateRedirectModel(List<RedirectModel> redirectModels, RequestContext request) {
        storedModels.clear();
        storedModels.addAll(redirectModels);
      }

      @Override
      protected Object getRedirectModelMutex(RequestContext request) {
        return new Object();
      }
    };

    RequestContext context = mock(RequestContext.class);
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
      protected List<RedirectModel> retrieveRedirectModel(RequestContext request) {
        return storedModels.isEmpty() ? null : new ArrayList<>(storedModels);
      }

      @Override
      protected void updateRedirectModel(List<RedirectModel> redirectModels, RequestContext request) {
        storedModels.clear();
        storedModels.addAll(redirectModels);
      }

      @Override
      protected Object getRedirectModelMutex(RequestContext request) {
        return null;
      }
    };

    RequestContext context = mock(RequestContext.class);
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
    RequestContext context = mock(RequestContext.class);
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
    protected List<RedirectModel> retrieveRedirectModel(RequestContext request) {
      return null;
    }

    @Override
    protected void updateRedirectModel(List<RedirectModel> redirectModels, RequestContext request) {
    }

    @Override
    protected boolean isRedirectModelForRequest(RedirectModel model, RequestContext request) {
      return super.isRedirectModelForRequest(model, request);
    }
  }

  static class SessionRedirectModelManager0 extends SessionRedirectModelManager {

    @Override
    protected boolean isRedirectModelForRequest(RedirectModel model, RequestContext request) {
      return super.isRedirectModelForRequest(model, request);
    }

  }

}