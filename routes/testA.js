var express = require('express');

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