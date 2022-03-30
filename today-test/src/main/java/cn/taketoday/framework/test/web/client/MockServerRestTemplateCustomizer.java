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

package cn.taketoday.framework.test.web.client;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.test.web.client.MockRestServiceServer;
import cn.taketoday.test.web.client.RequestExpectationManager;
import cn.taketoday.test.web.client.SimpleRequestExpectationManager;
import cn.taketoday.web.client.RestTemplate;
import cn.taketoday.web.client.config.RestTemplateBuilder;
import cn.taketoday.web.client.config.RestTemplateCustomizer;

/**
 * {@link RestTemplateCustomizer} that can be applied to a {@link RestTemplateBuilder}
 * instances to add {@link MockRestServiceServer} support.
 * <p>
 * Typically applied to an existing builder before it is used, for example:
 * <pre class="code">
 * MockServerRestTemplateCustomizer customizer = new MockServerRestTemplateCustomizer();
 * MyBean bean = new MyBean(new RestTemplateBuilder(customizer));
 * customizer.getServer().expect(requestTo("/hello")).andRespond(withSuccess());
 * bean.makeRestCall();
 * </pre>
 * <p>
 * If the customizer is only used once, the {@link #getServer()} method can be used to
 * obtain the mock server. If the customizer has been used more than once the
 * {@link #getServer(RestTemplate)} or {@link #getServers()} method must be used to access
 * the related server.
 *
 * @author Phillip Webb
 * @see #getServer()
 * @see #getServer(RestTemplate)
 * @since 4.0
 */
public class MockServerRestTemplateCustomizer implements RestTemplateCustomizer {

  private final Map<RestTemplate, RequestExpectationManager> expectationManagers = new ConcurrentHashMap<>();

  private final Map<RestTemplate, MockRestServiceServer> servers = new ConcurrentHashMap<>();

  private final Class<? extends RequestExpectationManager> expectationManager;

  private boolean detectRootUri = true;

  public MockServerRestTemplateCustomizer() {
    this.expectationManager = SimpleRequestExpectationManager.class;
  }

  public MockServerRestTemplateCustomizer(Class<? extends RequestExpectationManager> expectationManager) {
    Assert.notNull(expectationManager, "ExpectationManager must not be null");
    this.expectationManager = expectationManager;
  }

  /**
   * Set if root URIs from {@link RootUriRequestExpectationManager} should be detected
   * and applied to the {@link MockRestServiceServer}.
   *
   * @param detectRootUri if root URIs should be detected
   */
  public void setDetectRootUri(boolean detectRootUri) {
    this.detectRootUri = detectRootUri;
  }

  @Override
  public void customize(RestTemplate restTemplate) {
    RequestExpectationManager expectationManager = createExpectationManager();
    if (this.detectRootUri) {
      expectationManager = RootUriRequestExpectationManager.forRestTemplate(restTemplate, expectationManager);
    }
    MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build(expectationManager);
    this.expectationManagers.put(restTemplate, expectationManager);
    this.servers.put(restTemplate, server);
  }

  protected RequestExpectationManager createExpectationManager() {
    return BeanUtils.newInstance(this.expectationManager);
  }

  public MockRestServiceServer getServer() {
    Assert.state(!this.servers.isEmpty(), "Unable to return a single MockRestServiceServer since "
            + "MockServerRestTemplateCustomizer has not been bound to a RestTemplate");
    Assert.state(this.servers.size() == 1, "Unable to return a single MockRestServiceServer since "
            + "MockServerRestTemplateCustomizer has been bound to more than one RestTemplate");
    return this.servers.values().iterator().next();
  }

  public Map<RestTemplate, RequestExpectationManager> getExpectationManagers() {
    return this.expectationManagers;
  }

  public MockRestServiceServer getServer(RestTemplate restTemplate) {
    return this.servers.get(restTemplate);
  }

  public Map<RestTemplate, MockRestServiceServer> getServers() {
    return Collections.unmodifiableMap(this.servers);
  }

}
