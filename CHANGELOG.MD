
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






