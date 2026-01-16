/*
 * Copyright 2012-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.app.mail.config;

import org.jspecify.annotations.Nullable;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import infra.context.properties.ConfigurationProperties;

/**
 * Configuration properties for email support.
 *
 * @author Oliver Gierke
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@ConfigurationProperties("mail")
public class MailProperties {

  /**
   * SMTP server host. For instance, 'smtp.example.com'.
   */
  public @Nullable String host;

  /**
   * SMTP server port.
   */
  public @Nullable Integer port;

  /**
   * Login user of the SMTP server.
   */
  public @Nullable String username;

  /**
   * Login password of the SMTP server.
   */
  public @Nullable String password;

  /**
   * Protocol used by the SMTP server.
   */
  public String protocol = "smtp";

  /**
   * Default MimeMessage encoding.
   */
  public Charset defaultEncoding = StandardCharsets.UTF_8;

  /**
   * Additional JavaMail Session properties.
   */
  public final Map<String, String> properties = new HashMap<>();

  /**
   * Session JNDI name. When set, takes precedence over other Session settings.
   */
  public @Nullable String jndiName;

  /**
   * SSL configuration.
   */
  public final Ssl ssl = new Ssl();

  public static class Ssl {

    /**
     * Whether to enable SSL support. If enabled, 'mail.(protocol).ssl.enable'
     * property is set to 'true'.
     */
    public boolean enabled;

    /**
     * SSL bundle name. If set, 'mail.(protocol).ssl.socketFactory' property is set to
     * an SSLSocketFactory obtained from the corresponding SSL bundle.
     * <p>
     * Note that the STARTTLS command can use the corresponding SSLSocketFactory, even
     * if the 'mail.(protocol).ssl.enable' property is not set.
     */
    public @Nullable String bundle;

  }

}
