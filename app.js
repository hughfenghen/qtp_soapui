var express = require('express');
var path = require('path');
var favicon = require('serve-favicon');
var logger = require('morgan');
var cookieParser = require('cookie-parser');
var bodyParser = require('body-parser');

var routes = require('./routes/index');
var users = require('./routes/users');
var login = require('./routes/login');
var customer_routes = require('./routes/routes_customer_config');

var app = express();

// view engine setup
app.set('views', path.join(__dirname, 'views'));
app.set('view engine', 'jade');

// uncomment after placing your favicon in /public
//app.use(favicon(path.join(__dirname, 'public', 'favicon.ico')));
app.use(logger('dev'));
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({
  extended: false
}));
app.use(cookieParser());
app.use(express.static(path.join(__dirname, 'public')));

//--------------------------------customer router-----------------------------
//记录在线用户  模拟session
var USER_SESSION = {};

app.use(function(req, res, next){
  req.setSession = function(token, obj) {
      USER_SESSION[token] = obj;
    }
    next();
});

app.use('/login', login);

app.use(function(req, res, next) {
  var token = req.param('token');

  if (!token || !USER_SESSION[token]) {
    res.send({
      data: {
        status: '0'
      },
      msg: '未登录'
    });
  } else {
    next();
  }
});

app.use('/', routes);
app.use('/users', users);

for (var k in customer_routes) {
  app.use(k, require(customer_routes[k]));
}
//-------------------------------------------------------------
// catch 404 and forward to error handler
app.use(function(req, res, next) {
  var err = new Error('Not Found');
  err.status = 404;
  next(err);
});

// error handlers

// development error handler
// will print stacktrace
if (app.get('env') === 'development') {
  app.use(function(err, req, res, next) {
    res.status(err.status || 500);
    res.render('error', {
      message: err.message,
      error: err
    });
  });
}

// production error handler
// no stacktraces leaked to user
app.use(function(err, req, res, next) {
  res.status(err.status || 500);
  res.render('error', {
    message: err.message,
    error: {}
  });
});

var server = app.listen(3000, function() {
  var host = server.address().address;
  var port = server.address().port;

  console.log('Example app listening at http://%s:%s', host, port);
});

module.exports = app;