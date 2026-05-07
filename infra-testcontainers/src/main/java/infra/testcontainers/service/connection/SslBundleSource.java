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

package infra.testcontainers.service.connection;

import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import infra.beans.factory.BeanFactory;
import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.MergedAnnotations;
import infra.core.ssl.SslBundle;
import infra.core.ssl.SslBundleKey;
import infra.core.ssl.SslOptions;
import infra.core.ssl.SslStoreBundle;
import infra.core.ssl.jks.JksSslStoreBundle;
import infra.core.ssl.jks.JksSslStoreDetails;
import infra.core.ssl.pem.PemSslStoreBundle;
import infra.core.ssl.pem.PemSslStoreDetails;
import infra.lang.Assert;
import infra.util.StringUtils;

/**
 * {@link SslBundle} source created from annotations. Used as a cache key and as a
 * {@link SslBundle} factory.
 *
 * @param ssl the {@link Ssl @Ssl} annotation
 * @param pemKeyStore the {@link PemKeyStore @PemKeyStore} annotation
 * @param pemTrustStore the {@link PemTrustStore @PemTrustStore} annotation
 * @param jksKeyStore the {@link JksKeyStore @JksKeyStore} annotation
 * @param jksTrustStore the {@link JksTrustStore @JksTrustStore} annotation
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
record SslBundleSource(@Nullable Ssl ssl, @Nullable PemKeyStore pemKeyStore, @Nullable PemTrustStore pemTrustStore,
                       @Nullable JksKeyStore jksKeyStore, @Nullable JksTrustStore jksTrustStore) {

  SslBundleSource {
    boolean hasPem = (pemKeyStore != null || pemTrustStore != null);
    boolean hasJks = (jksKeyStore != null || jksTrustStore != null);
    if (hasJks && hasPem) {
      throw new IllegalStateException("PEM and JKS store annotations cannot be used together");
    }
  }

  @Nullable SslBundle getSslBundle() {
    SslStoreBundle stores = stores();
    if (stores == null) {
      return null;
    }
    Ssl ssl = (this.ssl != null) ? this.ssl : MergedAnnotation.valueOf(Ssl.class).synthesize();
    SslOptions options = SslOptions.of(nullIfEmpty(ssl.ciphers()), nullIfEmpty(ssl.enabledProtocols()));
    SslBundleKey key = SslBundleKey.of(nullIfEmpty(ssl.keyPassword()), nullIfEmpty(ssl.keyAlias()));
    String protocol = ssl.protocol();
    return SslBundle.of(stores, key, options, protocol);
  }

  private @Nullable SslStoreBundle stores() {
    if (this.pemKeyStore != null || this.pemTrustStore != null) {
      return new PemSslStoreBundle(pemKeyStoreDetails(), pemTrustStoreDetails());
    }
    if (this.jksKeyStore != null || this.jksTrustStore != null) {
      return new JksSslStoreBundle(jksKeyStoreDetails(), jksTrustStoreDetails());
    }
    return null;
  }

  private @Nullable PemSslStoreDetails pemKeyStoreDetails() {
    PemKeyStore store = this.pemKeyStore;
    return (store != null) ? new PemSslStoreDetails(nullIfEmpty(store.type()), nullIfEmpty(store.certificate()),
            nullIfEmpty(store.privateKey()), nullIfEmpty(store.privateKeyPassword())) : null;
  }

  private @Nullable PemSslStoreDetails pemTrustStoreDetails() {
    PemTrustStore store = this.pemTrustStore;
    return (store != null) ? new PemSslStoreDetails(nullIfEmpty(store.type()), nullIfEmpty(store.certificate()),
            nullIfEmpty(store.privateKey()), nullIfEmpty(store.privateKeyPassword())) : null;
  }

  private @Nullable JksSslStoreDetails jksKeyStoreDetails() {
    JksKeyStore store = this.jksKeyStore;
    return (store != null) ? new JksSslStoreDetails(nullIfEmpty(store.type()), nullIfEmpty(store.provider()),
            nullIfEmpty(store.location()), nullIfEmpty(store.password())) : null;
  }

  private @Nullable JksSslStoreDetails jksTrustStoreDetails() {
    JksTrustStore store = this.jksTrustStore;
    return (store != null) ? new JksSslStoreDetails(nullIfEmpty(store.type()), nullIfEmpty(store.provider()),
            nullIfEmpty(store.location()), nullIfEmpty(store.password())) : null;
  }

  private @Nullable String nullIfEmpty(@Nullable String string) {
    if (StringUtils.isNotEmpty(string)) {
      return string;
    }
    return null;
  }

  private String @Nullable [] nullIfEmpty(String @Nullable [] array) {
    if (array == null || array.length == 0) {
      return null;
    }
    return array;
  }

  static @Nullable SslBundleSource get(MergedAnnotations annotations) {
    return get(null, null, annotations);
  }

  static @Nullable SslBundleSource get(@Nullable BeanFactory beanFactory, @Nullable String beanName,
          @Nullable MergedAnnotations annotations) {
    Ssl ssl = getAnnotation(beanFactory, beanName, annotations, Ssl.class);
    PemKeyStore pemKeyStore = getAnnotation(beanFactory, beanName, annotations, PemKeyStore.class);
    PemTrustStore pemTrustStore = getAnnotation(beanFactory, beanName, annotations, PemTrustStore.class);
    JksKeyStore jksKeyStore = getAnnotation(beanFactory, beanName, annotations, JksKeyStore.class);
    JksTrustStore jksTrustStore = getAnnotation(beanFactory, beanName, annotations, JksTrustStore.class);
    if (ssl == null && pemKeyStore == null && pemTrustStore == null && jksKeyStore == null
            && jksTrustStore == null) {
      return null;
    }
    return new SslBundleSource(ssl, pemKeyStore, pemTrustStore, jksKeyStore, jksTrustStore);
  }

  private static <A extends Annotation> @Nullable A getAnnotation(@Nullable BeanFactory beanFactory,
          @Nullable String beanName, @Nullable MergedAnnotations annotations, Class<A> annotationType) {
    Set<A> found = (beanFactory != null && beanName != null)
            ? beanFactory.findAllAnnotationsOnBean(beanName, annotationType, false) : Collections.emptySet();
    if (annotations != null) {
      found = new LinkedHashSet<>(found);
      annotations.stream(annotationType).map(MergedAnnotation::synthesize).forEach(found::add);
    }
    int size = found.size();
    Assert.state(size <= 1,
            () -> "Expected single %s annotation, but found %d".formatted(annotationType.getName(), size));
    return (size > 0) ? found.iterator().next() : null;
  }

}
