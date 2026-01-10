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

package test.demo.config;

import java.io.File;
import java.io.Serializable;
import java.util.Objects;

import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.annotation.Value;
import infra.stereotype.Prototype;
import infra.stereotype.Singleton;
import jakarta.annotation.PostConstruct;

@Singleton
@SuppressWarnings("serial")
@Prototype("prototype_config")
public final class Config implements Serializable {

  private Integer id;

  @Value("${site.cdn}")
  private String cdn;

  @Value("${site.icp}")
  private String icp;

  @Value("${site.host}")
  private String host;

  @Value("${site.index}")
  private File index;

  @Value("${site.upload}")
  private File upload;

  @Value("${site.keywords}")
  private String keywords;

  @Value("${site.name}")
  private String siteName;

  @Value("${site.copyright}")
  private String copyright;

  @Value("${site.server.path}")
  private File serverPath;

  @Value("${site.description}")
  private String description;

  @Value("${site.otherFooterInfo}")
  private String otherFooterInfo;

  @Autowired(required = false)
  User user;

  User admin;

  //	@Value(value = "#{user}", required = false)
//	User user_;
//
  @PostConstruct
  public void init() {
    System.err.println("admin: " + admin);
  }

  public Config() {

  }

  public User getAdmin() {
    return admin;
  }

  public void setAdmin(User admin) {
    this.admin = admin;
  }

  public String getCdn() {
    return cdn;
  }

  public void setCdn(String cdn) {
    this.cdn = cdn;
  }

  public String getCopyright() {
    return copyright;
  }

  public void setCopyright(String copyright) {
    this.copyright = copyright;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public String getIcp() {
    return icp;
  }

  public void setIcp(String icp) {
    this.icp = icp;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public File getIndex() {
    return index;
  }

  public void setIndex(File index) {
    this.index = index;
  }

  public String getKeywords() {
    return keywords;
  }

  public void setKeywords(String keywords) {
    this.keywords = keywords;
  }

  public String getOtherFooterInfo() {
    return otherFooterInfo;
  }

  public void setOtherFooterInfo(String otherFooterInfo) {
    this.otherFooterInfo = otherFooterInfo;
  }

  public File getServerPath() {
    return serverPath;
  }

  public void setServerPath(File serverPath) {
    this.serverPath = serverPath;
  }

  public String getSiteName() {
    return siteName;
  }

  public void setSiteName(String siteName) {
    this.siteName = siteName;
  }

  public File getUpload() {
    return upload;
  }

  public void setUpload(File upload) {
    this.upload = upload;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Config config))
      return false;
    return Objects.equals(id, config.id) && Objects.equals(cdn, config.cdn) && Objects.equals(icp, config.icp) && Objects.equals(host,
            config.host) && Objects.equals(index, config.index) && Objects.equals(upload, config.upload) && Objects.equals(keywords,
            config.keywords) && Objects.equals(siteName, config.siteName) && Objects.equals(copyright, config.copyright) && Objects.equals(serverPath,
            config.serverPath) && Objects.equals(description, config.description) && Objects.equals(otherFooterInfo, config.otherFooterInfo) && Objects.equals(user,
            config.user) && Objects.equals(admin, config.admin);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, cdn, icp, host, index, upload, keywords, siteName, copyright, serverPath, description, otherFooterInfo, user, admin);
  }
}
