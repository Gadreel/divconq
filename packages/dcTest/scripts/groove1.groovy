public class Tester  {
    public Closure printIt(rec) {
        println "this is in the test class";
		
        println "Age 1: " + rec.getFieldAsInteger("Age");
		
        println "Age 2: " + rec.Age;
		
		rec.Age += 5;
		
        println "Age 3: " + rec.Age;
		
        println "Age 4: " + rec.getFieldAsInteger("Age");
		
        println "Cnt 1: " + rec.getFieldCount();
		
		rec.clear();
		
        println "Cnt 2: " + rec.getFieldCount();
		
        println "Age 5: " + rec.Age;
		
        println "Dyn 1: " + rec.keyCount(99);
		
        println "Dyn 2: " + rec.keyCount();
		
		Thread.start({
			println "this is in the new thread";
		});
		
		return {
			println "function result output";
		};
    }
}