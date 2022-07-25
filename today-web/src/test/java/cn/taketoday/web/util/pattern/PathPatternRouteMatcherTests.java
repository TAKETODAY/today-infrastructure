/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.web.util.pattern;

import org.junit.jupiter.api.Test;

import java.util.Map;

import cn.taketoday.http.server.PathContainer;
import cn.taketoday.util.RouteMatcher;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the {@link PathPatternRouteMatcher}.
 *
 * @author Brian Clozel
 * @since 4.0
 */
public class PathPatternRouteMatcherTests {

  @Test
  public void matchRoute() {
    PathPatternRouteMatcher routeMatcher = new PathPatternRouteMatcher();
    RouteMatcher.Route route = routeMatcher.parseRoute("projects.spring-framework");
    assertThat(routeMatcher.match("projects.{name}", route)).isTrue();
  }

  @Test
  public void matchRouteWithCustomSeparator() {
    PathPatternParser parser = new PathPatternParser();
    parser.setPathOptions(PathContainer.Options.create('/', false));
    PathPatternRouteMatcher routeMatcher = new PathPatternRouteMatcher(parser);
    RouteMatcher.Route route = routeMatcher.parseRoute("/projects/spring-framework");
    assertThat(routeMatcher.match("/projects/{name}", route)).isTrue();
  }

  @Test // gh-23310
  public void noDecodingAndNoParamParsing() {
    PathPatternRouteMatcher routeMatcher = new PathPatternRouteMatcher();
    RouteMatcher.Route route = routeMatcher.parseRoute("projects.spring%20framework;p=1");
    assertThat(routeMatcher.match("projects.spring%20framework;p=1", route)).isTrue();
  }

  @Test // gh-23310
  public void separatorOnlyDecoded() {
    PathPatternRouteMatcher routeMatcher = new PathPatternRouteMatcher();
    RouteMatcher.Route route = routeMatcher.parseRoute("projects.spring%2Eframework");
    Map<String, String> vars = routeMatcher.matchAndExtract("projects.{project}", route);
    assertThat(vars).containsEntry("project", "spring.framework");
  }

}
