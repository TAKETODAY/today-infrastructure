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
