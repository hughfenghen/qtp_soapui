var express = require('express');
var uuid = require('node-uuid');

var router = express.Router();

router.all('/', function(req, res, next) {
	var account = req.param('account');
	var pw = req.param('password');
	console.info('=====account:' + account + ' password:' + pw);

	if (!account || !pw) {
		res.send({status: '-1'});
	} else {
		var token = uuid.v4();
		req.setSession(token, {});
		res.send({token: token, status: '1'});
	}
});

module.exports = router;