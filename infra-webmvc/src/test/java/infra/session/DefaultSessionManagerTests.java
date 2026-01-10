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

import java.util.List;

import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.http.HttpCookie;
import infra.http.ResponseCookie;
import infra.session.config.EnableSession;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY 2021/4/30 22:24
 * @since 3.0
 */
class DefaultSessionManagerTests {

  @EnableSession
  static class AppConfig {

  }

  @Test
  public void testWebSession() {

    AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
    applicationContext.register(AppConfig.class);
    applicationContext.refresh();
    SessionManager sessionManager = applicationContext.getBean(SessionManager.class);
    MockRequestContext context = new MockRequestContext();

    Session noneExistingSession = sessionManager.getSession(context, false);

    assertThat(noneExistingSession).isNull();

    Session createdSession = sessionManager.getSession(context);
    assertThat(createdSession).isNotNull();

    // CookieTokenResolver
    CookieSessionIdResolver cookieTokenResolver = applicationContext.getBean(CookieSessionIdResolver.class);
    List<ResponseCookie> responseCookies = context.responseCookies();
    String sessionId = createdSession.getId();
    HttpCookie sessionCookie = cookieTokenResolver.createCookie(sessionId);

    assertThat(responseCookies).hasSize(1);
    assertThat(responseCookies.get(0)).isEqualTo(sessionCookie);

    // WebSessionStorage
    SessionRepository sessionStorage = applicationContext.getBean(SessionRepository.class);
    Session session = sessionStorage.retrieveSession(sessionId);

    assertThat(session).isEqualTo(createdSession);
    assertThat(sessionStorage.contains(sessionId)).isTrue();
    sessionStorage.removeSession(sessionId);
    assertThat(sessionStorage.contains(sessionId)).isFalse();

  }

}
