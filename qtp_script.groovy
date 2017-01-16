import com.eviware.soapui.impl.wsdl.testcase.WsdlTestRunContext
import groovy.json.JsonSlurper
import customer.RSAUtils

JSON = new JsonSlurper()

CURRENT_TESTCASE = testRunner.testCase
TEST_SUITE = CURRENT_TESTCASE.parent

//使用JAVA加密 必须导入customer.RSAUtils。customer.jar文件拷贝至目录：<soapui安装目录>\bin\ext 后重启soapui
PUBLIC_KEY_BASE64 = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCC4d0q2qR7G21TmObv5l0fxpMHcD34pqCjJoIl\nvU/Oa+0rsNkkZljvQAenY8ZNpOPzcfUL/F+qwTpuJh5ny6zl9gPloQRd6PcWob1Z+cuSoEAwBZx4\n+Yw/2QAARjxs5e8GeF0IdY/HK/HmpTCKbmKbUxNaftmeRwfgaG/TGZ93CwIDAQAB"

//缓存 后续需要回传的值
CACHE = [:]

//============================================= UTILS =======================================================
//递归 	深度比较返回值与期望是否匹配
def verifyExcept(actual, expect) {
	def rs = true

	//类型判断  java Collection 与 Map 不同的遍历方式
	if (expect instanceof Collection) {
		expect.eachWithIndex {it, i ->
			//log.info "i:" + i + "  it:" + it
			if (it instanceof Map || it instanceof Collection) {
				return rs = verifyExcept(actual[i], it)
			} else {
				def actualValue = actual != null ? actual[i] : null
				if (actualValue != it) {
					log.error "expect: $index:${i}=${it}    actual:${actualValue}"
					return rs = false
				}
			}
		}
	} else if (expect instanceof Map) {
		expect.each {k, v ->
			//log.info "k:" + k + "  v:" + v
			if (v instanceof Map || v instanceof Collection) {
				return rs = verifyExcept(actual[k], v)
			} else {
				def actualValue = actual != null ? actual[k] : null
				if (actualValue != v) {
					log.error "expect: ${k}=${v}    actual:${actualValue}"
					return rs = false
				}
			}
		}
	} else {
		log.error "expect is not Object!"
		rs = false
	}

	return rs
}
//rsa 加密内容
def rsaEncrypt(value) {
	return RSAUtils.encrypt(RSAUtils.getPublicKey(PUBLIC_KEY_BASE64), value)
}
//添加扩展参数
def addExtParams(params, testStep) {
	if (!params || params.size() == 0) return

	params.each {k, v ->
		testStep.setPropertyValue(k, interpreter(v))
	}
}
//解析替换字符串中的自定义语法  正则替换有疑问请查java API
def interpreter(v) {
	def cachePattern = ~/cache\{\s*(.*?)(\[.+\])*\s*\}/
	def rsaParamPattern = ~/rsaEncrypt\{\s*(.*?)\s*\}/
	def randomPattern = ~/random\{\s*(.*?)\s*\}/

	def rs = v
	def sb = new StringBuffer()
	def mC = cachePattern.matcher(v)
	//从缓存中取值 替换 	例："cache{login-token}"
	while(mC.find()) {
		if (mC.group(2)) {
			//使用Eval可以获取多层级的属性值 login-abc['a']['b']
			def tmp = Eval.me('CACHE', CACHE, "CACHE['${mC.group(1)}']${mC.group(2)}")
			mC.appendReplacement(sb, tmp)
		} else {
			mC.appendReplacement(sb, CACHE[mC.group(1)])
		}
	}
	mC.appendTail(sb)
	rs = sb.toString()

	//rsa加密 替换 	例："rsaEncrypt{18600000000}"
	def mR = rsaParamPattern.matcher(rs)
	sb.delete(0, sb.length())
	while(mR.find()) {
		mR.appendReplacement(sb, rsaEncrypt(mR.group(1)))
	}
	mR.appendTail(sb)
	rs = sb.toString()

	//生成随机数 替换	例："soapui自动化-random{1000}"
	def mRd = randomPattern.matcher(rs)
	sb.delete(0, sb.length())
	while(mRd.find()) {
		mRd.appendReplacement(sb, Math.round(Math.random() * Integer.valueOf(mRd.group(1))) + "")
	}
	mRd.appendTail(sb)
	rs = sb.toString()
	
	return rs
}
//缓存需要的数据
def cacheData(items, rsData, prefix) {
	if (!items || items.size() == 0) return
	
	//默认在请求返回数据中取值 也可先缓存固定值共后续请求使用，使数据保持一致
	items.each {
		if (it instanceof Map) {
			it.each {k, v ->
				CACHE["${prefix}-${k}"] = v
			}
		} else {
			CACHE["${prefix}-${it}"] = rsData[it]
		}
	}
}
//============================================= RUN =======================================================
({
	//加载任务
	def taskFile = new File(CURRENT_TESTCASE.properties.task_file.value)
	def tasks = JSON.parseText(taskFile.getText())

	//执行任务
	tasks.each {
	    def caseName = it.reqName.split("-")[0]
	    def stepName = it.reqName.split("-")[1]
	    
	    def testStep = TEST_SUITE.getTestCaseByName(caseName).getTestStepByName(stepName)
		//发送请求前覆盖默认参数
		addExtParams(it.extParams, testStep)
		
	    def testStepContext = new WsdlTestRunContext(testStep)
	    def result = testStep.run(testRunner, testStepContext)

	    log.info "【${caseName}-${stepName} result data】" + result.responseContent
	    def rsJson = JSON.parseText(result.responseContent)

	    if (verifyExcept(rsJson, it.expect)) {
	    	cacheData(it.cache, rsJson, stepName)

    		log.info "【${it.reqName}】success!"
    		assert true
	    } else {
	   	 	log.error "【${it.reqName}】fail!"
	   	 	assert false
	    }
	}
}())

return