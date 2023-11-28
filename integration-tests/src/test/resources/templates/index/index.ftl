<!DOCTYPE HTML> 
<html>
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
		<title>Index ${q}</title>
	</head>
<body>

<h3>URL: ${url}
<br>
<#list q as it>
	q:${it}<br>
</#list>

<#list Q as w>
		Q:${w}<br>
</#list>

<br>
	userId:${userId}
<br>
	userName:${userName}
<br>
</h3>
</body>
</html>
