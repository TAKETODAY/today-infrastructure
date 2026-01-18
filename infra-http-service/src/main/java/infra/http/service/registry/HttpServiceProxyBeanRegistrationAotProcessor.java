/*
 * Copyright 2002-present the original author or authors.
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

package infra.http.service.registry;

import org.jspecify.annotations.Nullable;

import javax.lang.model.element.Modifier;

import infra.aot.generate.GeneratedMethod;
import infra.aot.generate.GenerationContext;
import infra.beans.factory.aot.BeanRegistrationAotContribution;
import infra.beans.factory.aot.BeanRegistrationAotProcessor;
import infra.beans.factory.aot.BeanRegistrationCode;
import infra.beans.factory.aot.BeanRegistrationCodeFragments;
import infra.beans.factory.aot.BeanRegistrationCodeFragmentsDecorator;
import infra.beans.factory.support.InstanceSupplier;
import infra.beans.factory.support.RegisteredBean;
import infra.javapoet.ClassName;
import infra.javapoet.CodeBlock;

import static infra.http.service.registry.AbstractHttpServiceRegistrar.HTTP_SERVICE_GROUP_NAME_ATTRIBUTE;
import static infra.http.service.registry.AbstractHttpServiceRegistrar.HTTP_SERVICE_PROXY_REGISTRY_BEAN_NAME;

/**
 * {@link BeanRegistrationAotProcessor} for HTTP service proxy support.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see AbstractHttpServiceRegistrar
 */
final class HttpServiceProxyBeanRegistrationAotProcessor implements BeanRegistrationAotProcessor {

  @Override
  public @Nullable BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
    Object value = registeredBean.getMergedBeanDefinition().getAttribute(HTTP_SERVICE_GROUP_NAME_ATTRIBUTE);
    if (value instanceof String groupName) {
      return BeanRegistrationAotContribution.withCustomCodeFragments(codeFragments ->
              new HttpServiceProxyRegistrationCodeFragments(codeFragments, groupName, registeredBean.getBeanClass()));
    }
    return null;
  }

  private static class HttpServiceProxyRegistrationCodeFragments extends BeanRegistrationCodeFragmentsDecorator {

    private static final String REGISTERED_BEAN_PARAMETER = "registeredBean";

    private final String groupName;

    private final Class<?> clientType;

    HttpServiceProxyRegistrationCodeFragments(BeanRegistrationCodeFragments delegate,
            String groupName, Class<?> clientType) {
      super(delegate);
      this.groupName = groupName;
      this.clientType = clientType;
    }

    @Override
    public ClassName getTarget(RegisteredBean registeredBean) {
      return ClassName.get(registeredBean.getBeanClass());
    }

    @Override
    public CodeBlock generateInstanceSupplierCode(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode, boolean allowDirectSupplierShortcut) {
      GeneratedMethod generatedMethod = beanRegistrationCode.getMethods()
              .add("getHttpServiceProxy", method -> {
                method.addJavadoc("Create the HTTP service proxy for {@link $T} and group {@code $L}.",
                        this.clientType, this.groupName);
                method.addModifiers(Modifier.PRIVATE, Modifier.STATIC);
                method.addParameter(RegisteredBean.class, REGISTERED_BEAN_PARAMETER);
                method.returns(Object.class);
                method.addStatement("return $L.getBeanFactory().getBean($S, $T.class).getClient($S, $T.class)",
                        REGISTERED_BEAN_PARAMETER, HTTP_SERVICE_PROXY_REGISTRY_BEAN_NAME,
                        HttpServiceProxyRegistry.class, this.groupName, this.clientType);
              });
      return CodeBlock.of("$T.of($L)", InstanceSupplier.class, generatedMethod.toMethodReference().toCodeBlock());
    }

  }

}
