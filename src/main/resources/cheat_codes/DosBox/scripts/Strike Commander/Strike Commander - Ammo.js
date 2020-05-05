(function(context) {
    context.ammoCheat = new CheatBuilder("Unlimited Aircraft Ammo And Countermeasures (fire 20MM once)", "32 30 4D 4D 00 00 00 00 00")
                    .addCode(new CodeBuilder(-7, "1000")
                        .addOperation(new Filter(-12, "8"))
                        .addOperation(new Filter(-7, "1", "1000"))
                        .addOperation(new Detect(-7, "3", "100", 2))
                        .build())
                    .build();

    context.initialize = function() {
        context.missilesCodesEnabled = false;
        context.missileCodes = []
        var pos = -7+35;
        for (var i=0; i<8; ++i) {
            var code = new Code(pos, "2");
            missileCodes.push(code);
            pos+=35;
        }
        ammoCheat.getCodes().get(0).addOffset(464, "99");
        ammoCheat.getCodes().get(0).addOffset(466, "99");
    };

    context.cheatSuccess = function(cheat, codesWritten, totalCodes) {
        if (cheat == ammoCheat) {
            if (!missilesCodesEnabled) {
                print("adding missiles");
                createMissileCheats();
            }
        }

    }

    context.cheatFailed = function(cheat) {
        if (cheat == ammoCheat) {
            if (missilesCodesEnabled) {
                print("removing missiles");
                removeMissileCheats();
            }
        }
    }

    context.searchComplete = function() {

    };

    context.info = function() {
    };

    context.createMissileCheats = function() {
        var res = ammoCheat.getResults().getAllValidList().get(0); //should be our only result
        for (var i=0; i<8; ++i) {
            var code = missileCodes[i];
            var val = res.readValue(code.getFirstOffset(), 1);
            if (val > 0 && val < 7) {
                ammoCheat.addCode(code);
            }
        }
        missilesCodesEnabled = true;
    }

    context.removeMissileCheats = function() {
        var codes = ammoCheat.getCodes();
        codes.removeAll(missileCodes);
        missilesCodesEnabled = false;
    }

})(this);