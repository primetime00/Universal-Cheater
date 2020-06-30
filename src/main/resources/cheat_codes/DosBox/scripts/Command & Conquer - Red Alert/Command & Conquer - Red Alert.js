(function(context) {

/*
    context.timeCheat = new CheatBuilder("Freeze Time", "D8 18 D8 18 D8 18 D8 18 00 00")
                    .setIdentity(1)
                    .setScript(_script)
                    .setStopSearchOnResult(true)
                    .addCode(new CodeBuilder(33, "d 49000 s 2").build())
					.setTrigger(new Trigger("button", "toggle", 0))
                    .build();
	*/				
    context.healthCheat = new CheatBuilder("Unlimited Health", "01 00 00 00 ?? ?? 20 10 ?? 00 00 00 00 00 00 00 ??")
                    .setIdentity(2)
                    .setScript(_script)
                    .setStopSearchOnResult(false)
                    .addCode(new CodeBuilder(24, "d 1000 s 2")
						.build())
                    .build();
					


    context.initialize = function() {
        //timeCheat.setScriptHandler("ON_TRIGGER", context.timeTrigger);
		healthCheat.setScriptHandler("BEFORE_WRITE", context.checkHealth);
		healthCheat.setScriptHandler("ON_RESULTS", context.filterResults);
		
    };
	
	context.filterResults = function(results, position, memory) {
		for (var i=0; i<results.length; ++i) {
			var res = results.get(i);
			var amount = res.getBytesValue(memory, 134, 1);
			if (amount != 1 && amount != 8) {
				res.setValid(false);
			}
		}
	}
	
	context.checkHealth = function(results, codes) {
		for (var i=0; i<results.size(); ++i) {
			var res = results.get(i);
			var val = res.readValue(134, 1);
			if (val != 1 && val != 8) {
				print("Bad val:", val);
				res.setValid(false);
			}
		}
	}
	
/*
    context.timeTrigger = function(info, results) {
		if (results.size() == 0)
			return;
		var res = results.get(0);
		print("trigger", info.isDown());
		if (info.isDown()) {
			var freezeValue = res.readValue(33, 2);
			timeCheat.modifyCodeValue(0, 0, "d "+freezeValue+" s 2");
		}
	}*/

    context.cheatSuccess = function(cheat, codesWritten, totalCodes) {
    }

    context.cheatFailed = function(cheat) {
        print("Failed to find AOB", cheat.getName())
    }

    context.searchComplete = function() {
    };

    context.info = function() {
    };

})(this);