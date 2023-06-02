/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.aop.config;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.aop.aspectj.AspectJAfterAdvice;
import cn.taketoday.aop.aspectj.AspectJAfterReturningAdvice;
import cn.taketoday.aop.aspectj.AspectJAfterThrowingAdvice;
import cn.taketoday.aop.aspectj.AspectJAroundAdvice;
import cn.taketoday.aop.aspectj.AspectJExpressionPointcut;
import cn.taketoday.aop.aspectj.AspectJMethodBeforeAdvice;
import cn.taketoday.aop.aspectj.AspectJPointcutAdvisor;
import cn.taketoday.aop.aspectj.DeclareParentsAdvisor;
import cn.taketoday.aop.support.DefaultBeanFactoryPointcutAdvisor;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.BeanReference;
import cn.taketoday.beans.factory.config.ConstructorArgumentValues;
import cn.taketoday.beans.factory.config.RuntimeBeanNameReference;
import cn.taketoday.beans.factory.config.RuntimeBeanReference;
import cn.taketoday.beans.factory.parsing.CompositeComponentDefinition;
import cn.taketoday.beans.factory.parsing.ParseState;
import cn.taketoday.beans.factory.support.AbstractBeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionBuilder;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.factory.xml.BeanDefinitionParser;
import cn.taketoday.beans.factory.xml.ParserContext;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;
import cn.taketoday.util.xml.DomUtils;

/**
 * {@link BeanDefinitionParser} for the {@code <aop:config>} tag.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Adrian Colyer
 * @author Mark Fisher
 * @author Ramnivas Laddad
 * @since 4.0
 */
class ConfigBeanDefinitionParser implements BeanDefinitionParser {

  private static final String ASPECT = "aspect";
  private static final String EXPRESSION = "expression";
  private static final String ID = "id";
  private static final String POINTCUT = "pointcut";
  private static final String ADVICE_BEAN_NAME = "adviceBeanName";
  private static final String ADVISOR = "advisor";
  private static final String ADVICE_REF = "advice-ref";
  private static final String POINTCUT_REF = "pointcut-ref";
  private static final String REF = "ref";
  private static final String BEFORE = "before";
  private static final String DECLARE_PARENTS = "declare-parents";
  private static final String TYPE_PATTERN = "types-matching";
  private static final String DEFAULT_IMPL = "default-impl";
  private static final String DELEGATE_REF = "delegate-ref";
  private static final String IMPLEMENT_INTERFACE = "implement-interface";
  private static final String AFTER = "after";
  private static final String AFTER_RETURNING_ELEMENT = "after-returning";
  private static final String AFTER_THROWING_ELEMENT = "after-throwing";
  private static final String AROUND = "around";
  private static final String RETURNING = "returning";
  private static final String RETURNING_PROPERTY = "returningName";
  private static final String THROWING = "throwing";
  private static final String THROWING_PROPERTY = "throwingName";
  private static final String ARG_NAMES = "arg-names";
  private static final String ARG_NAMES_PROPERTY = "argumentNames";
  private static final String ASPECT_NAME_PROPERTY = "aspectName";
  private static final String DECLARATION_ORDER_PROPERTY = "declarationOrder";
  private static final String ORDER_PROPERTY = "order";
  private static final int METHOD_INDEX = 0;
  private static final int POINTCUT_INDEX = 1;
  private static final int ASPECT_INSTANCE_FACTORY_INDEX = 2;

  private final ParseState parseState = new ParseState();

  @Override
  @Nullable
  public BeanDefinition parse(Element element, ParserContext parserContext) {
    CompositeComponentDefinition compositeDef =
            new CompositeComponentDefinition(element.getTagName(), parserContext.extractSource(element));
    parserContext.pushContainingComponent(compositeDef);

    configureAutoProxyCreator(parserContext, element);

    List<Element> childElts = DomUtils.getChildElements(element);
    for (Element elt : childElts) {
      String localName = parserContext.getDelegate().getLocalName(elt);
      switch (localName) {
        case ASPECT -> parseAspect(elt, parserContext);
        case ADVISOR -> parseAdvisor(elt, parserContext);
        case POINTCUT -> parsePointcut(elt, parserContext);
      }
    }

    parserContext.popAndRegisterContainingComponent();
    return null;
  }

  /**
   * Configures the auto proxy creator needed to support the {@link BeanDefinition BeanDefinitions}
   * created by the '{@code <aop:config/>}' tag. Will force class proxying if the
   * '{@code proxy-target-class}' attribute is set to '{@code true}'.
   *
   * @see AopNamespaceUtils
   */
  private void configureAutoProxyCreator(ParserContext parserContext, Element element) {
    AopNamespaceUtils.registerAspectJAutoProxyCreatorIfNecessary(parserContext, element);
  }

  /**
   * Parses the supplied {@code <advisor>} element and registers the resulting
   * {@link cn.taketoday.aop.Advisor} and any resulting {@link cn.taketoday.aop.Pointcut}
   * with the supplied {@link BeanDefinitionRegistry}.
   */
  private void parseAdvisor(Element advisorElement, ParserContext parserContext) {
    AbstractBeanDefinition advisorDef = createAdvisorBeanDefinition(advisorElement, parserContext);
    String id = advisorElement.getAttribute(ID);

    try {
      parseState.push(new AdvisorEntry(id));
      String advisorBeanName = id;
      if (StringUtils.hasText(advisorBeanName)) {
        parserContext.getRegistry().registerBeanDefinition(advisorBeanName, advisorDef);
      }
      else {
        advisorBeanName = parserContext.getReaderContext().registerWithGeneratedName(advisorDef);
      }

      Object pointcut = parsePointcutProperty(advisorElement, parserContext);
      if (pointcut instanceof BeanDefinition) {
        advisorDef.getPropertyValues().add(POINTCUT, pointcut);
        parserContext.registerComponent(
                new AdvisorComponentDefinition(advisorBeanName, advisorDef, (BeanDefinition) pointcut));
      }
      else if (pointcut instanceof String) {
        advisorDef.getPropertyValues().add(POINTCUT, new RuntimeBeanReference((String) pointcut));
        parserContext.registerComponent(
                new AdvisorComponentDefinition(advisorBeanName, advisorDef));
      }
    }
    finally {
      parseState.pop();
    }
  }

  /**
   * Create a {@link RootBeanDefinition} for the advisor described in the supplied. Does <strong>not</strong>
   * parse any associated '{@code pointcut}' or '{@code pointcut-ref}' attributes.
   */
  private AbstractBeanDefinition createAdvisorBeanDefinition(Element advisorElement, ParserContext parserContext) {
    RootBeanDefinition advisorDefinition = new RootBeanDefinition(DefaultBeanFactoryPointcutAdvisor.class);
    advisorDefinition.setSource(parserContext.extractSource(advisorElement));

    String adviceRef = advisorElement.getAttribute(ADVICE_REF);
    if (StringUtils.isBlank(adviceRef)) {
      parserContext.getReaderContext().error(
              "'advice-ref' attribute contains empty value.", advisorElement, parseState.snapshot());
    }
    else {
      advisorDefinition.getPropertyValues().add(
              ADVICE_BEAN_NAME, new RuntimeBeanNameReference(adviceRef));
    }

    if (advisorElement.hasAttribute(ORDER_PROPERTY)) {
      advisorDefinition.getPropertyValues().add(
              ORDER_PROPERTY, advisorElement.getAttribute(ORDER_PROPERTY));
    }

    return advisorDefinition;
  }

  private void parseAspect(Element aspectElement, ParserContext parserContext) {
    String aspectId = aspectElement.getAttribute(ID);
    String aspectName = aspectElement.getAttribute(REF);

    try {
      parseState.push(new AspectEntry(aspectId, aspectName));
      ArrayList<BeanDefinition> beanDefinitions = new ArrayList<>();
      ArrayList<BeanReference> beanReferences = new ArrayList<>();

      List<Element> declareParents = DomUtils.getChildElementsByTagName(aspectElement, DECLARE_PARENTS);
      for (Element declareParentsElement : declareParents) {
        beanDefinitions.add(parseDeclareParents(declareParentsElement, parserContext));
      }

      // We have to parse "advice" and all the advice kinds in one loop, to get the
      // ordering semantics right.
      NodeList nodeList = aspectElement.getChildNodes();
      boolean adviceFoundAlready = false;
      for (int i = 0; i < nodeList.getLength(); i++) {
        Node node = nodeList.item(i);
        if (isAdviceNode(node, parserContext)) {
          if (!adviceFoundAlready) {
            adviceFoundAlready = true;
            if (StringUtils.isBlank(aspectName)) {
              parserContext.getReaderContext().error(
                      "<aspect> tag needs aspect bean reference via 'ref' attribute when declaring advices.",
                      aspectElement, parseState.snapshot());
              return;
            }
            beanReferences.add(new RuntimeBeanReference(aspectName));
          }
          AbstractBeanDefinition advisorDefinition = parseAdvice(
                  aspectName, i, aspectElement, (Element) node, parserContext, beanDefinitions, beanReferences);
          beanDefinitions.add(advisorDefinition);
        }
      }

      AspectComponentDefinition aspectComponentDefinition = createAspectComponentDefinition(
              aspectElement, aspectId, beanDefinitions, beanReferences, parserContext);
      parserContext.pushContainingComponent(aspectComponentDefinition);

      List<Element> pointcuts = DomUtils.getChildElementsByTagName(aspectElement, POINTCUT);
      for (Element pointcutElement : pointcuts) {
        parsePointcut(pointcutElement, parserContext);
      }

      parserContext.popAndRegisterContainingComponent();
    }
    finally {
      parseState.pop();
    }
  }

  private AspectComponentDefinition createAspectComponentDefinition(
          Element aspectElement, String aspectId, List<BeanDefinition> beanDefs,
          List<BeanReference> beanRefs, ParserContext parserContext) {

    BeanDefinition[] beanDefArray = beanDefs.toArray(new BeanDefinition[0]);
    BeanReference[] beanRefArray = beanRefs.toArray(new BeanReference[0]);
    Object source = parserContext.extractSource(aspectElement);
    return new AspectComponentDefinition(aspectId, beanDefArray, beanRefArray, source);
  }

  /**
   * Return {@code true} if the supplied node describes an advice type. May be one of:
   * '{@code before}', '{@code after}', '{@code after-returning}',
   * '{@code after-throwing}' or '{@code around}'.
   */
  private boolean isAdviceNode(Node aNode, ParserContext parserContext) {
    if (!(aNode instanceof Element)) {
      return false;
    }
    else {
      String name = parserContext.getDelegate().getLocalName(aNode);
      return (BEFORE.equals(name) || AFTER.equals(name) || AFTER_RETURNING_ELEMENT.equals(name) ||
              AFTER_THROWING_ELEMENT.equals(name) || AROUND.equals(name));
    }
  }

  /**
   * Parse a '{@code declare-parents}' element and register the appropriate
   * DeclareParentsAdvisor with the BeanDefinitionRegistry encapsulated in the
   * supplied ParserContext.
   */
  private AbstractBeanDefinition parseDeclareParents(Element declareParentsElement, ParserContext parserContext) {
    BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(DeclareParentsAdvisor.class);
    builder.addConstructorArgValue(declareParentsElement.getAttribute(IMPLEMENT_INTERFACE));
    builder.addConstructorArgValue(declareParentsElement.getAttribute(TYPE_PATTERN));

    String defaultImpl = declareParentsElement.getAttribute(DEFAULT_IMPL);
    String delegateRef = declareParentsElement.getAttribute(DELEGATE_REF);

    if (StringUtils.hasText(defaultImpl) && StringUtils.isBlank(delegateRef)) {
      builder.addConstructorArgValue(defaultImpl);
    }
    else if (StringUtils.hasText(delegateRef) && StringUtils.isBlank(defaultImpl)) {
      builder.addConstructorArgReference(delegateRef);
    }
    else {
      parserContext.getReaderContext().error(
              "Exactly one of the " + DEFAULT_IMPL + " or " + DELEGATE_REF + " attributes must be specified",
              declareParentsElement, parseState.snapshot());
    }

    AbstractBeanDefinition definition = builder.getBeanDefinition();
    definition.setSource(parserContext.extractSource(declareParentsElement));
    parserContext.getReaderContext().registerWithGeneratedName(definition);
    return definition;
  }

  /**
   * Parses one of '{@code before}', '{@code after}', '{@code after-returning}',
   * '{@code after-throwing}' or '{@code around}' and registers the resulting
   * BeanDefinition with the supplied BeanDefinitionRegistry.
   *
   * @return the generated advice RootBeanDefinition
   */
  private AbstractBeanDefinition parseAdvice(
          String aspectName, int order, Element aspectElement,
          Element adviceElement, ParserContext parserContext,
          List<BeanDefinition> beanDefinitions, List<BeanReference> beanReferences) {

    try {
      parseState.push(new AdviceEntry(parserContext.getDelegate().getLocalName(adviceElement)));

      // create the method factory bean
      RootBeanDefinition methodDefinition = new RootBeanDefinition(MethodLocatingFactoryBean.class);
      methodDefinition.getPropertyValues().add("targetBeanName", aspectName);
      methodDefinition.getPropertyValues().add("methodName", adviceElement.getAttribute("method"));
      methodDefinition.setSynthetic(true);

      // create instance factory definition
      RootBeanDefinition aspectFactoryDef =
              new RootBeanDefinition(SimpleBeanFactoryAwareAspectInstanceFactory.class);
      aspectFactoryDef.getPropertyValues().add("aspectBeanName", aspectName);
      aspectFactoryDef.setSynthetic(true);

      // register the pointcut
      AbstractBeanDefinition adviceDef = createAdviceDefinition(
              adviceElement, parserContext, aspectName, order, methodDefinition, aspectFactoryDef,
              beanDefinitions, beanReferences);

      // configure the advisor
      RootBeanDefinition advisorDefinition = new RootBeanDefinition(AspectJPointcutAdvisor.class);
      advisorDefinition.setSource(parserContext.extractSource(adviceElement));
      advisorDefinition.getConstructorArgumentValues().addGenericArgumentValue(adviceDef);
      if (aspectElement.hasAttribute(ORDER_PROPERTY)) {
        advisorDefinition.getPropertyValues().add(
                ORDER_PROPERTY, aspectElement.getAttribute(ORDER_PROPERTY));
      }

      // register the final advisor
      parserContext.getReaderContext().registerWithGeneratedName(advisorDefinition);

      return advisorDefinition;
    }
    finally {
      parseState.pop();
    }
  }

  /**
   * Creates the RootBeanDefinition for a POJO advice bean. Also causes pointcut
   * parsing to occur so that the pointcut may be associate with the advice bean.
   * This same pointcut is also configured as the pointcut for the enclosing
   * Advisor definition using the supplied PropertyValues.
   */
  private AbstractBeanDefinition createAdviceDefinition(
          Element adviceElement, ParserContext parserContext, String aspectName, int order,
          RootBeanDefinition methodDef, RootBeanDefinition aspectFactoryDef,
          List<BeanDefinition> beanDefinitions, List<BeanReference> beanReferences) {

    RootBeanDefinition adviceDefinition = new RootBeanDefinition(getAdviceClass(adviceElement, parserContext));
    adviceDefinition.setSource(parserContext.extractSource(adviceElement));

    adviceDefinition.getPropertyValues().add(ASPECT_NAME_PROPERTY, aspectName);
    adviceDefinition.getPropertyValues().add(DECLARATION_ORDER_PROPERTY, order);

    if (adviceElement.hasAttribute(RETURNING)) {
      adviceDefinition.getPropertyValues().add(
              RETURNING_PROPERTY, adviceElement.getAttribute(RETURNING));
    }
    if (adviceElement.hasAttribute(THROWING)) {
      adviceDefinition.getPropertyValues().add(
              THROWING_PROPERTY, adviceElement.getAttribute(THROWING));
    }
    if (adviceElement.hasAttribute(ARG_NAMES)) {
      adviceDefinition.getPropertyValues().add(
              ARG_NAMES_PROPERTY, adviceElement.getAttribute(ARG_NAMES));
    }

    ConstructorArgumentValues cav = adviceDefinition.getConstructorArgumentValues();
    cav.addIndexedArgumentValue(METHOD_INDEX, methodDef);

    Object pointcut = parsePointcutProperty(adviceElement, parserContext);
    if (pointcut instanceof BeanDefinition) {
      cav.addIndexedArgumentValue(POINTCUT_INDEX, pointcut);
      beanDefinitions.add((BeanDefinition) pointcut);
    }
    else if (pointcut instanceof String) {
      RuntimeBeanReference pointcutRef = new RuntimeBeanReference((String) pointcut);
      cav.addIndexedArgumentValue(POINTCUT_INDEX, pointcutRef);
      beanReferences.add(pointcutRef);
    }

    cav.addIndexedArgumentValue(ASPECT_INSTANCE_FACTORY_INDEX, aspectFactoryDef);

    return adviceDefinition;
  }

  /**
   * Gets the advice implementation class corresponding to the supplied {@link Element}.
   */
  private Class<?> getAdviceClass(Element adviceElement, ParserContext parserContext) {
    String elementName = parserContext.getDelegate().getLocalName(adviceElement);
    return switch (elementName) {
      case AROUND -> AspectJAroundAdvice.class;
      case AFTER -> AspectJAfterAdvice.class;
      case BEFORE -> AspectJMethodBeforeAdvice.class;
      case AFTER_THROWING_ELEMENT -> AspectJAfterThrowingAdvice.class;
      case AFTER_RETURNING_ELEMENT -> AspectJAfterReturningAdvice.class;
      default -> throw new IllegalArgumentException("Unknown advice kind [" + elementName + "].");
    };
  }

  /**
   * Parses the supplied {@code <pointcut>} and registers the resulting
   * Pointcut with the BeanDefinitionRegistry.
   */
  private AbstractBeanDefinition parsePointcut(Element pointcutElement, ParserContext parserContext) {
    String id = pointcutElement.getAttribute(ID);
    String expression = pointcutElement.getAttribute(EXPRESSION);

    AbstractBeanDefinition pointcutDefinition;

    try {
      parseState.push(new PointcutEntry(id));
      pointcutDefinition = createPointcutDefinition(expression);
      pointcutDefinition.setSource(parserContext.extractSource(pointcutElement));

      String pointcutBeanName = id;
      if (StringUtils.hasText(pointcutBeanName)) {
        parserContext.getRegistry().registerBeanDefinition(pointcutBeanName, pointcutDefinition);
      }
      else {
        pointcutBeanName = parserContext.getReaderContext().registerWithGeneratedName(pointcutDefinition);
      }

      parserContext.registerComponent(
              new PointcutComponentDefinition(pointcutBeanName, pointcutDefinition, expression));
    }
    finally {
      parseState.pop();
    }

    return pointcutDefinition;
  }

  /**
   * Parses the {@code pointcut} or {@code pointcut-ref} attributes of the supplied
   * {@link Element} and add a {@code pointcut} property as appropriate. Generates a
   * {@link cn.taketoday.beans.factory.config.BeanDefinition} for the pointcut if  necessary
   * and returns its bean name, otherwise returns the bean name of the referred pointcut.
   */
  @Nullable
  private Object parsePointcutProperty(Element element, ParserContext parserContext) {
    if (element.hasAttribute(POINTCUT) && element.hasAttribute(POINTCUT_REF)) {
      parserContext.getReaderContext().error(
              "Cannot define both 'pointcut' and 'pointcut-ref' on <advisor> tag.",
              element, parseState.snapshot());
      return null;
    }
    else if (element.hasAttribute(POINTCUT)) {
      // Create a pointcut for the anonymous pc and register it.
      String expression = element.getAttribute(POINTCUT);
      AbstractBeanDefinition pointcutDefinition = createPointcutDefinition(expression);
      pointcutDefinition.setSource(parserContext.extractSource(element));
      return pointcutDefinition;
    }
    else if (element.hasAttribute(POINTCUT_REF)) {
      String pointcutRef = element.getAttribute(POINTCUT_REF);
      if (StringUtils.isBlank(pointcutRef)) {
        parserContext.getReaderContext().error(
                "'pointcut-ref' attribute contains empty value.", element, parseState.snapshot());
        return null;
      }
      return pointcutRef;
    }
    else {
      parserContext.getReaderContext().error(
              "Must define one of 'pointcut' or 'pointcut-ref' on <advisor> tag.",
              element, parseState.snapshot());
      return null;
    }
  }

  /**
   * Creates a {@link BeanDefinition} for the {@link AspectJExpressionPointcut} class using
   * the supplied pointcut expression.
   */
  protected AbstractBeanDefinition createPointcutDefinition(String expression) {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(AspectJExpressionPointcut.class);
    beanDefinition.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    beanDefinition.setSynthetic(true);
    beanDefinition.getPropertyValues().add(EXPRESSION, expression);
    return beanDefinition;
  }

}
