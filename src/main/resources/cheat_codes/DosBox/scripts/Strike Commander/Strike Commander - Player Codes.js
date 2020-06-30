(function(context) {

    context.ammoCheat = new CheatBuilder("Unlimited Ammo", "?? ?? ?? ?? ?? ?? ?? 32 30 4D 4D 00 00 00 00 00")
                    .setIdentity(10)
                    .setScript(_script)
                    .setStopSearchOnResult(true)
                    .addCode(new CodeBuilder(0, "d 2000 s 2").build())
                    .build();

    context.cmCheat = new CheatBuilder("Unlimited Countermeasures", "?? ?? ?? ?? ?? ?? ?? 32 30 4D 4D 00 00 00 00 00")
                    .setIdentity(11)
                    .addCode(new CodeBuilder(471, "b 63006300").build())
                    .setStopSearchOnResult(true)
                    .setScript(_script)
                    .build();


    context.initialize = function() {
        ammoCheat.setScriptHandler("ON_RESULTS", context.ammoResults);
        cmCheat.setScriptHandler("ON_RESULTS", context.cmResults);
        ammoCheat.setScriptHandler("AFTER_WRITE", context.ammoWrite);
    };

    context.ammoResults = function(results, position, memory) {
        var foundPlayer = undefined;
        for (var i=0; i<results.length; ++i) {
            //filter invalid results
            var amount = results[i].getBytesValue(memory, 0, 2);
            if (amount < 500 || amount > 1000) {
                results[i].setValid(false);
                continue;
            }
            var playerResults = searchRange(memory, position, results[i].getOffset(), 1000, "47 6F 6D 65 72 20 4D");
            var isPlayer = playerResults.size() > 0;
            if (isPlayer) {
                results[i].setValid(true);
                foundPlayer = true;
            }
            else {
                results[i].setValid(false);
            }
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
                    writeResult(results[i], pos, "d 2 s 1");
                }
                pos+=35;
            }
        }
    }

    context.cmResults = function(results, position, memory) {
        var foundPlayer = undefined;
        for (var i=0; i<results.length; ++i) {
            //filter invalid results
            var playerResults = searchRange(memory, position, results[i].getOffset(), 1000, "47 6F 6D 65 72 20 4D");
            var isPlayer = playerResults.size() > 0;
            if (isPlayer) {
                results[i].setValid(true);
                foundPlayer = true;
            }
            else {
                results[i].setValid(false);
            }
        }
        if (!foundPlayer) {
            cmCheat.queueReset();
        }
    }



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