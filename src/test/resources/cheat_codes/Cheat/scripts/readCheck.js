(function(context) {

    context.preRead = function(t, d) {
        logMessage("PreRead");
        return false;
    }

    context.preRead2 = function(t, d) {
        logMessage("2PreRead");
        return true;
    }


    context.readCheck = new CheatBuilder("Cheat 1", "5F 46 41 4B 45 00")
                    .addCode(new CodeBuilder(10, "9").addReadBeforeWriteHandler(preRead).build())
                    .addCode(new CodeBuilder(12, "d 9 s 4").addReadBeforeWriteHandler(preRead2).build())
                    .build();

    context.initialize = function() {
        logMessage("Initialize");
        print("Initializing script...");
    };



    context.cheatSuccess = function(cheat, codesWritten, totalCodes) {
        logMessage("Cheat success");
    }

    context.cheatFailed = function(cheat) {
        logMessage("Cheat failed");
    }


    context.searchComplete = function() {
        logMessage("Search Complete");
        print("Searching is complete");
    };

    context.info = function() {
    };
})(this)