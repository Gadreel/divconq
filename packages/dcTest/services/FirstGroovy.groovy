// one class holds an entire Feature
import divconq.service.plugin.Operation
import divconq.service.plugin.Request
import divconq.service.plugin.Response

@Operation()
void handlePing(context, request) {
    println "Pinged.";

    context.return		// nothing to return - must still call complete!!!
}

// write a "handles" method for each Op
@Operation(
    response = @Response(type = "String")
)
void handleTest(context, request) {
    println "Tested.";

    context.return "fun!"
}

@Operation(
    request = @Request(type = "String", required = true),
    response = @Response(type = "String")
)
void handleEcho(context, request) {
    println "Echoing: " + request
    context.return "Echo: " + request
}

@Operation(
	response = @Response(type = "String")
)
void handleThreadTest(context, request) {
    println "Operations are async - you may set return at any later time.  However, services typically must reply within 1 minute.";

    Thread.start({
        println "Finishing async Operation";
        context.return "DONE!"
    });
}

