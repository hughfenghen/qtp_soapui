var express = require('express');
var uuid = require('node-uuid');

var router = express.Router();

router.all('/', function(req, res, next) {
	res.send({
		data: {
			result: req.param('b') || 'B',
			status: '1'
		}
	});
});

module.exports = router;