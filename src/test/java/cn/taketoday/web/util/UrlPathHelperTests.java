/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.web.util;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import cn.taketoday.web.RequestContext;
import cn.taketoday.web.mock.MockHttpServletRequest;
import cn.taketoday.web.servlet.ServletRequestContext;
import jakarta.servlet.RequestDispatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link UrlPathHelper}.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Costin Leau
 */
@Disabled
class UrlPathHelperTests {

  private final UrlPathHelper helper = new UrlPathHelper();

  private final MockHttpServletRequest servletRequest = new MockHttpServletRequest();
  final RequestContext request = new ServletRequestContext(servletRequest, null);

  @Test
  void getPathWithinApplication() {
    servletRequest.setContextPath("/petclinic");
    servletRequest.setRequestURI("/petclinic/welcome.html");

    assertThat(helper.getPathWithinApplication(request)).isEqualTo("/welcome.html");
  }

  @Test
  void getPathWithinApplicationForRootWithNoLeadingSlash() {
    servletRequest.setContextPath("/petclinic");
    servletRequest.setRequestURI("/petclinic");

    assertThat(helper.getPathWithinApplication(request)).as("Incorrect root path returned").isEqualTo("/");
  }

  @Test
  void getPathWithinApplicationForSlashContextPath() {
    servletRequest.setContextPath("/");
    servletRequest.setRequestURI("/welcome.html");

    assertThat(helper.getPathWithinApplication(request)).isEqualTo("/welcome.html");
  }

  @Test
  void alwaysUseFullPath() {
    helper.setAlwaysUseFullPath(true);
    servletRequest.setContextPath("/petclinic");
    servletRequest.setServletPath("/main");
    servletRequest.setRequestURI("/petclinic/main/welcome.html");

    assertThat(helper.getLookupPathForRequest(request)).isEqualTo("/main/welcome.html");
  }

  @Test
  void getRequestUri() {
    servletRequest.setRequestURI("/welcome.html");
    assertThat(helper.getRequestUri(request)).isEqualTo("/welcome.html");

    servletRequest.setRequestURI("/foo%20bar");
    assertThat(helper.getRequestUri(request)).isEqualTo("/foo bar");

    servletRequest.setRequestURI("/foo+bar");
    assertThat(helper.getRequestUri(request)).isEqualTo("/foo+bar");

    servletRequest.setRequestURI("/home/" + "/path");
    assertThat(helper.getRequestUri(request)).isEqualTo("/home/path");
  }

  @Test
  void getRequestRemoveSemicolonContent() {
    helper.setRemoveSemicolonContent(true);
    servletRequest.setRequestURI("/foo;f=F;o=O;o=O/bar;b=B;a=A;r=R");
    assertThat(helper.getRequestUri(request)).isEqualTo("/foo/bar");

    servletRequest.setRequestURI("/foo;f=F;o=O;o=O/bar;b=B;a=A;r=R/baz;test");
    assertThat(helper.getRequestUri(request)).isEqualTo("/foo/bar/baz");

    // SPR-13455
    servletRequest.setRequestURI("/foo/;test/1");
    servletRequest.setServletPath("/foo/1");
    assertThat(helper.getRequestUri(request)).isEqualTo("/foo/1");
  }

  @Test
  void getRequestKeepSemicolonContent() {
    helper.setRemoveSemicolonContent(false);

    testKeepSemicolonContent("/foo;a=b;c=d", "/foo;a=b;c=d");
    testKeepSemicolonContent("/test;jsessionid=1234", "/test");
    testKeepSemicolonContent("/test;JSESSIONID=1234", "/test");
    testKeepSemicolonContent("/test;jsessionid=1234;a=b", "/test;a=b");
    testKeepSemicolonContent("/test;a=b;jsessionid=1234;c=d", "/test;a=b;c=d");
    testKeepSemicolonContent("/test;jsessionid=1234/anotherTest", "/test/anotherTest");
    testKeepSemicolonContent("/test;jsessionid=;a=b", "/test;a=b");
    testKeepSemicolonContent("/somethingLongerThan12;jsessionid=1234", "/somethingLongerThan12");
  }

  private void testKeepSemicolonContent(String requestUri, String expectedPath) {
    servletRequest.setRequestURI(requestUri);
    assertThat(helper.getRequestUri(request)).isEqualTo(expectedPath);
  }

  @Test
  void getLookupPathWithSemicolonContent() {
    helper.setRemoveSemicolonContent(false);

    servletRequest.setContextPath("/petclinic");
    servletRequest.setServletPath("/main");
    servletRequest.setRequestURI("/petclinic;a=b/main;b=c/welcome.html;c=d");

    assertThat(helper.getLookupPathForRequest(request)).isEqualTo("/welcome.html;c=d");
  }

  @Test
  void getLookupPathWithSemicolonContentAndNullPathInfo() {
    helper.setRemoveSemicolonContent(false);

    servletRequest.setContextPath("/petclinic");
    servletRequest.setServletPath("/welcome.html");
    servletRequest.setRequestURI("/petclinic;a=b/welcome.html;c=d");

    assertThat(helper.getLookupPathForRequest(request)).isEqualTo("/welcome.html;c=d");
  }

  @Test
    // gh-27303
  void defaultInstanceReadOnlyBehavior() {
    UrlPathHelper helper = UrlPathHelper.defaultInstance;

    assertThatIllegalArgumentException()
            .isThrownBy(() -> helper.setAlwaysUseFullPath(true))
            .withMessage("This instance cannot be modified");
    assertThatIllegalArgumentException()
            .isThrownBy(() -> helper.setUrlDecode(true))
            .withMessage("This instance cannot be modified");
    assertThatIllegalArgumentException()
            .isThrownBy(() -> helper.setRemoveSemicolonContent(true))
            .withMessage("This instance cannot be modified");
    assertThatIllegalArgumentException()
            .isThrownBy(() -> helper.setDefaultEncoding("UTF-8"))
            .withMessage("This instance cannot be modified");

    assertThat(helper.isUrlDecode()).isTrue();
    assertThat(helper.shouldRemoveSemicolonContent()).isTrue();
  }

  //
  // suite of tests root requests for default servlets (SRV 11.2) on Websphere vs Tomcat and other containers
  // see: https://jira.springframework.org/browse/SPR-7064
  //

  //
  // / mapping (default servlet)
  //

  @Test
  void tomcatDefaultServletRoot() {
    servletRequest.setContextPath("/test");
    servletRequest.setServletPath("/");
    servletRequest.setPathInfo(null);
    servletRequest.setRequestURI("/test/");
    assertThat(helper.getLookupPathForRequest(request)).isEqualTo("/");
  }

  @Test
  void tomcatDefaultServletFile() {
    servletRequest.setContextPath("/test");
    servletRequest.setServletPath("/foo");
    servletRequest.setPathInfo(null);
    servletRequest.setRequestURI("/test/foo");

    assertThat(helper.getLookupPathForRequest(request)).isEqualTo("/foo");
  }

  @Test
  void tomcatDefaultServletFolder() {
    servletRequest.setContextPath("/test");
    servletRequest.setServletPath("/foo/");
    servletRequest.setPathInfo(null);
    servletRequest.setRequestURI("/test/foo/");

    assertThat(helper.getLookupPathForRequest(request)).isEqualTo("/foo/");
  }

  @Test
    //SPR-12372, SPR-13455
  void removeDuplicateSlashesInPath() {
    servletRequest.setContextPath("/SPR-12372");
    servletRequest.setPathInfo(null);
    servletRequest.setServletPath("/foo/bar/");
    servletRequest.setRequestURI("/SPR-12372/foo//bar/");

    assertThat(helper.getLookupPathForRequest(request)).isEqualTo("/foo/bar/");

    servletRequest.setServletPath("/foo/bar/");
    servletRequest.setRequestURI("/SPR-12372/foo/bar//");

    assertThat(helper.getLookupPathForRequest(request)).isEqualTo("/foo/bar/");

    // "normal" case
    servletRequest.setServletPath("/foo/bar//");
    servletRequest.setRequestURI("/SPR-12372/foo/bar//");

    assertThat(helper.getLookupPathForRequest(request)).isEqualTo("/foo/bar//");
  }

  @Test
  void wasDefaultServletRoot() {
    servletRequest.setContextPath("/test");
    servletRequest.setPathInfo("/");
    servletRequest.setServletPath("");
    servletRequest.setRequestURI("/test/");

    assertThat(helper.getLookupPathForRequest(request)).isEqualTo("/");
  }

  @Test
  void wasDefaultServletRootWithCompliantSetting() {
    tomcatDefaultServletRoot();
  }

  @Test
  void wasDefaultServletFile() {
    servletRequest.setContextPath("/test");
    servletRequest.setPathInfo("/foo");
    servletRequest.setServletPath("");
    servletRequest.setRequestURI("/test/foo");

    assertThat(helper.getLookupPathForRequest(request)).isEqualTo("/foo");
  }

  @Test
  void wasDefaultServletFileWithCompliantSetting() {
    tomcatDefaultServletFile();
  }

  @Test
  void wasDefaultServletFolder() {
    servletRequest.setContextPath("/test");
    servletRequest.setPathInfo("/foo/");
    servletRequest.setServletPath("");
    servletRequest.setRequestURI("/test/foo/");

    assertThat(helper.getLookupPathForRequest(request)).isEqualTo("/foo/");
  }

  @Test
  void wasDefaultServletFolderWithCompliantSetting() {
    tomcatDefaultServletFolder();
  }

  //
  // /foo/* mapping
  //

  @Test
  void tomcatCasualServletRoot() {
    servletRequest.setContextPath("/test");
    servletRequest.setPathInfo("/");
    servletRequest.setServletPath("/foo");
    servletRequest.setRequestURI("/test/foo/");

    assertThat(helper.getLookupPathForRequest(request)).isEqualTo("/");
  }

  @Disabled
  @Test
    // test the root mapping for /foo/* w/o a trailing slash - <host>/<context>/foo
  void tomcatCasualServletRootWithMissingSlash() {
    servletRequest.setContextPath("/test");
    servletRequest.setPathInfo(null);
    servletRequest.setServletPath("/foo");
    servletRequest.setRequestURI("/test/foo");

    assertThat(helper.getLookupPathForRequest(request)).isEqualTo("/");
  }

  @Test
  void tomcatCasualServletFile() {
    servletRequest.setContextPath("/test");
    servletRequest.setPathInfo("/foo");
    servletRequest.setServletPath("/foo");
    servletRequest.setRequestURI("/test/foo/foo");

    assertThat(helper.getLookupPathForRequest(request)).isEqualTo("/foo");
  }

  @Test
  void tomcatCasualServletFolder() {
    servletRequest.setContextPath("/test");
    servletRequest.setPathInfo("/foo/");
    servletRequest.setServletPath("/foo");
    servletRequest.setRequestURI("/test/foo/foo/");

    assertThat(helper.getLookupPathForRequest(request)).isEqualTo("/foo/");
  }

  @Test
  void wasCasualServletRootWithCompliantSetting() {
    tomcatCasualServletRoot();
  }

  @Disabled
  @Test
    // test the root mapping for /foo/* w/o a trailing slash - <host>/<context>/foo
  void wasCasualServletRootWithMissingSlash() {
    servletRequest.setContextPath("/test");
    servletRequest.setPathInfo(null);
    servletRequest.setServletPath("/foo");
    servletRequest.setRequestURI("/test/foo");
    assertThat(helper.getLookupPathForRequest(request)).isEqualTo("/");
  }

  @Disabled
  @Test
  void wasCasualServletRootWithMissingSlashWithCompliantSetting() {
    tomcatCasualServletRootWithMissingSlash();
  }

  @Test
  void wasCasualServletFile() {
    servletRequest.setContextPath("/test");
    servletRequest.setPathInfo("/foo");
    servletRequest.setServletPath("/foo");
    servletRequest.setRequestURI("/test/foo/foo");

    assertThat(helper.getLookupPathForRequest(request)).isEqualTo("/foo");
  }

  @Test
  void wasCasualServletFileWithCompliantSetting() {
    tomcatCasualServletFile();
  }

  @Test
  void wasCasualServletFolder() {
    servletRequest.setContextPath("/test");
    servletRequest.setPathInfo("/foo/");
    servletRequest.setServletPath("/foo");
    servletRequest.setRequestURI("/test/foo/foo/");

    assertThat(helper.getLookupPathForRequest(request)).isEqualTo("/foo/");
  }

  @Test
  void wasCasualServletFolderWithCompliantSetting() {
    tomcatCasualServletFolder();
  }

  @Test
  void getOriginatingRequestUri() {
    request.setAttribute(RequestDispatcher.FORWARD_REQUEST_URI, "/path");
    servletRequest.setRequestURI("/forwarded");
    assertThat(helper.getOriginatingRequestUri(request)).isEqualTo("/path");
  }

  @Test
  void getOriginatingRequestUriDefault() {
    servletRequest.setRequestURI("/forwarded");
    assertThat(helper.getOriginatingRequestUri(request)).isEqualTo("/forwarded");
  }

}
