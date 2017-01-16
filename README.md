> 基于soapui实现的接口自动化测试工具。

[soapui自动化系列教程地址](http://blog.csdn.net/lj745280746/article/details/48443367)

如有已运行的服务器可以按照教程第一二节添加接口、生成用例，然后修改task_file.json
按需修改qtp_script.groovy
执行qtp_script.groovy

-----------------

若使用本教程测试接口，需先安装nodejs+express。cd到qtp_soapui目录，执行`npm install`
使用soapui导入qtp_soapui.xml文件

按教程第三节配置task_file路径
执行script->testStep->auto_script

**如需使用rsa加密功能，查看教程第四节导入第三方包，重启soapui**

------------------------------------------------------

*去年兴起随手写的，现在看来功能不够丰富，只当玩具了，现做备份。有机会再整一下Postman或JMeter*