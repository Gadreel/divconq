function fun1(name, rec) {
    print('Hi there from Javascript, ' + name);
    print('Age 1: ' + rec.Age);
	
	rec.Age += 2;
	
    print('Age 2: ' + rec.Age);
	
	var age = rec.getFieldAsInteger('Age');
	
    print('Age 3: ' + age);
	
	rec.cbTest(function(e) {
		print(e.Data);
		rec.Age = 9;
	});
	
    return "greetings from javascript";
};

var fun2 = function (object) {
    print("JS Class Definition: " + Object.prototype.toString.call(object));
};