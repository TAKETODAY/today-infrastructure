<!DOCTYPE html>
<html lang="zh">
<body><h1>发生错误</h1>
<div id='created'>${timestamp?datetime}</div>
<div>出现意外错误 (type=${message?default(error)}, status=${status})</div>
</body>
</html>