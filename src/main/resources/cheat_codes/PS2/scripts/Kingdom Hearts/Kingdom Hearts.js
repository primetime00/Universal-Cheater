(function(context) {
	var String = Java.type("java.lang.String")
	var aob = "?? ?? ?? ?? 00 00 00 00 00 00 00 00 00 ?? 06 01 00 00 00 00 90 03 4F 00"
	var abilityAOB = "76 6F 69 63 65 2F ?? ?? 5F 73 6F 72 61 2E 76 73 62 00"
	var offset = -288;
	var abilityOffset = -1411814;
	var gummiOffset = -1363122;

    context.jumpCheat = new CheatBuilder("Moon Jump", aob)
                    .setIdentity(1)
                    .setScript(_script)
					.setTrigger(new Trigger("keypress", "hold", 81))
                    .setStopSearchOnResult(true)
                    .addCode(new CodeBuilder(offset, "d 0.0 s 4").build())
                    .build();
					
	var soraAbilityBuilder = new CheatBuilder("Sora Ability Load", abilityAOB)
                    .setIdentity(2)
                    .setScript(_script)
                    .setStopSearchOnResult(true);
				for (var i = 0; i< 47; ++i) {
                    soraAbilityBuilder.addCode(new CodeBuilder(abilityOffset+i, String.format("d %d s 1", i+1)).build());
				}				
	context.soraAbilitiesCheat = soraAbilityBuilder.build();
	
	var maxGummiBuilder = new CheatBuilder("Max Gummi Items", abilityAOB)
                    .setIdentity(3)
                    .setScript(_script)
                    .setStopSearchOnResult(true);
				for (var i = 0; i< 70; ++i) {
                    maxGummiBuilder.addCode(new CodeBuilder(gummiOffset+i, String.format("d 99 s 1", i+1)).build());
				}				
	context.gummiCheat = maxGummiBuilder.build();	


    context.initialize = function() {
        jumpCheat.setScriptHandler("BEFORE_WRITE", context.jumpBeforeWrite);
    };
	
	context.jumpBeforeWrite = function(results, codes) {
		var res = results.get(0);
		var pos = res.readFloat(offset, 4);
		pos = pos - 100.0;
		print("pos = ", pos, res.toString());
		var c1 = codes.get(0);
		var offsetValue = c1.findOffsetValue(offset);
		offsetValue.setValue(String.format("d %f s 4", pos));
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