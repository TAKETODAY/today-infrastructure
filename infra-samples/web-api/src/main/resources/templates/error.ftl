<#-- @ftlvariable name="status" type="int" -->
<#-- @ftlvariable name="error" type="String" -->
<#-- @ftlvariable name="message" type="String" -->
<#-- @ftlvariable name="timestamp" type="java.util.Date" -->

<!DOCTYPE html>
<html lang="zh">
<body><h1>发生错误</h1>
<div id='created'>${timestamp?datetime}</div>
<div>出现意外错误 (type=${message!error}, status=${status})</div>
</body>
</html>