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

package cn.taketoday.web.socket.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This class level annotation declares that the class it decorates
 * is a web socket endpoint that will be deployed and made available in the URI-space
 * of a web socket server. The annotation allows the developer to
 * define the URL (or URI template) which this endpoint will be published, and other
 * important properties of the endpoint to the websocket runtime, such as the encoders
 * it uses to send messages.
 *
 * <p>The annotated class
 * must have a public no-arg constructor.
 *
 * <p>For example:
 * <pre><code>
 * &#64;EndpointMapping("/hello");
 * public class HelloServer {
 *
 *     &#64;OnMessage
 *     public void processGreeting(String message, Session session) {
 *         System.out.println("Greeting received:" + message);
 *     }
 *
 * }
 * </code></pre>
 *
 * @author dannycoward
 * @author TODAY 2021/5/7 13:24
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EndpointMapping {

  /**
   * The URI or URI-template, level-1 (<a href="http://http://tools.ietf.org/html/rfc6570">See RFC 6570</a>) where the endpoint will be deployed. The URI us relative to the
   * root of the web socket container and must begin with a leading "/". Trailing "/"'s are ignored. Examples:
   * <pre><code>
   * &#64;EndpointMapping("/chat")
   * &#64;EndpointMapping("/chat/{user}")
   * &#64;EndpointMapping("/booking/{privilege-level}")
   * </code></pre>
   *
   * @return the URI or URI-template
   */
  String[] value();

}
