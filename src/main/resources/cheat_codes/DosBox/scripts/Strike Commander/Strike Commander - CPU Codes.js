(function(context) {

    context.ammoCheat = new CheatBuilder("No Ammo CPU", "?? ?? 00 ?? 00 ?? ?? ?? ?? 4D 4D 00 00 00 00 00 01 ?? ??")
                    .setIdentity(12)
                    .setResetOnWrite(true)
                    .setScript(_script)
                    .addCode(new CodeBuilder(0, "d 0 s 2").build())
                    .build();

    context.cmCheat = new CheatBuilder("No Countermeasures CPU", "01 04 00 ?? ?? ?? 00 ?? ?? ?? 00 ?? 00 00 ?? ?? 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 04 00 ?? ?? ?? 00 1E 00 1E 00 ?? ?? ?? 00 ?? 00 00")
                    .setIdentity(13)
                    .addCode(new CodeBuilder(39, "b 00000000").build())
                    .setResetOnWrite(true)
                    .setScript(_script)
                    .build();


    context.initialize = function() {
        ammoCheat.setScriptHandler("ON_RESULTS", context.ammoResults);
		cmCheat.setScriptHandler("ON_RESULTS", context.cmResults);
        ammoCheat.setScriptHandler("WRITE_OVERRIDE", function() {return true;});
        cmCheat.setScriptHandler("WRITE_OVERRIDE", function() {return true;});
        ammoCheat.setScriptHandler("AFTER_WRITE", context.ammoWrite);
    };

    context.ammoResults = function(results, position, memory) {
        var foundPlayer = undefined;
        for (var i=0; i<results.length; ++i) {
            //filter invalid results
            var mm = results[i].getBytesValue(memory, 9, 2);
            var cal = results[i].getBytesValue(memory, 7, 1);
            var amount = results[i].getBytesValue(memory, 0, 2);
            var playerResults = searchRange(memory, position, results[i].getOffset(), 1000, "47 6F 6D 65 72 20 4D");
            var isPlayer = playerResults.size() > 0;
            if (mm != 19789 && mm != 28013) {
                results[i].setValid(false);
                continue;
            }
            if (cal != 50 && cal != 51) {
                results[i].setValid(false);
                continue;
            }
            if (isPlayer) {
                results[i].setValid(false);
                foundPlayer = true;
                continue;
            }
            if (amount < 500 || amount > 1000) {
                results[i].setValid(false);
                continue;
            }
            results[i].setValid(true);
        }
        if (!foundPlayer) {
            ammoCheat.queueReset();
        }
    }
	
    context.cmResults = function(results, position, memory) {
        var foundPlayer = undefined;
		print("have ",results.length);
        for (var i=0; i<results.length; ++i) {
            //filter invalid results
            var playerResults = searchRange(memory, position, results[i].getOffset(), 1000, "47 6F 6D 65 72 20 4D");
            var isPlayer = playerResults.size() > 0;
            if (isPlayer) {
                results[i].setValid(false);
                foundPlayer = true;
                continue;
            }
            results[i].setValid(true);
        }
        if (!foundPlayer) {
            ammoCheat.queueReset();
        }
    }
	

    context.ammoWrite = function(results) {
        for (var i=0; i<results.length; ++i) {
            var pos = 35;
            var base = results[i].getAddress()+7;
            for  (var j=0; j<8; ++j) {
                var nameAddress = base+pos;
                var cnt = results[i].readValue(pos, 1);
                if (cnt > 0 && cnt < 3) {
                    writeResult(results[i], pos, "d 0 s 1");
                }
                pos+=35;
            }
        }
    }

    context.cheatFailed = function(cheat) {
        print("Failed to handle cheat", cheat.getName());
    }

    context.searchComplete = function() {
    };

    context.info = function() {
    };

})(this);