
1.0.1
	除去字符编码只用UTF-8
	
1.2.0
	取消tags
	取消后缀
	
	
Java1.8
	通过反射得到参数名称


###	通过@ActionMapping配置请求
	@ActionMapping(value = {"/index.action", "/index.jsp"}, method = RequestMethod.GET)


```java
	/**
	 * 判断请求是否合法
	 * @param requestMethod
	 * @param mappingMethod
	 * @return
	 */
	public final boolean hasMethod(final String requestMethod, RequestMethod[] mappingMethod) {
		for (int i = 0; i < mappingMethod.length; i++) {
			if (mappingMethod[i].hasMethod(requestMethod)) {
				return true;
			}
		}
		return false;
	}

```

###	请求参数基本完毕
	完成数组注入
	完成一级POJO参数注入

出去View的action，所有的Action都由ActionHandler处理，减少内存开销


优化POJO注入
```java
	/**
	 * 设置POJO属性
	 * @param request
	 * @param parameterName
	 * @param bean
	 * @param field
	 * @return
	 */
	private final boolean set(HttpServletRequest request, final String parameterName, Object bean, Field field)  {
		
		Object parseDigit = null;
		
		try {
			final Class<?> type = field.getType();
			if(type.isArray()) {
				parseDigit = NumberUtils.stringToArray(request.getParameterValues(parameterName), type);
			} else if(type.getSuperclass() == Number.class) {
				System.out.println("number");
				parseDigit = NumberUtils.parseDigit(request.getParameter(parameterName), type);
			} else if (type == String.class) {
				parseDigit = request.getParameter(parameterName);
			}
			field.set(bean, parseDigit);
			return true;
		} catch (IllegalArgumentException | IllegalAccessException | ConversionException e) {
			return false;
		}
		
	}

```

计划支持注入集合



