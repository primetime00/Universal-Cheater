(function(context) {
	var aob = "05 00 52 6D 05 00 76 6E 00 00 F3 5D 00 00 EF 53 00 00 44 5E 00 00 2E 44"
	var String = Java.type("java.lang.String")

    context.itemCheat = new CheatBuilder("First Item Switcher", aob)
                    .setScript(_script)
                    .setStopSearchOnResult(true)
                    .addCode(new CodeBuilder(46, "d 1 s 1").build())
					.setTrigger(new Trigger("button", "onetime", 0))
                    .build();


    context.initialize = function() {
        itemCheat.setScriptHandler("BEFORE_WRITE", context.itemWrite);
    };
	
	context.itemWrite = function(results, codes) {
		var off = 46;		
		var c1 = codes.get(0);
		var offsetValue = c1.findOffsetValue(off);		
		var res = results.get(0);
		var val = res.readValue(off, 2);
		val = val+1;
		print("before write", val);				
		offsetValue.setValue(String.format("d %d s 1", val));
		print("WRITING ITEM 1");
	}
	

    context.cheatSuccess = function(cheat, codesWritten, totalCodes) {
    }

    context.cheatFailed = function(cheat) {
    }

    context.searchComplete = function() {
    };

    context.info = function() {
    };

})(this);