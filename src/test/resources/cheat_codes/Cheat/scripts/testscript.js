(function(context) {

    context.cheat1 = new CheatBuilder("Cheat 1", "5F 46 41 4B 45 00")
                    .addCode(new CodeBuilder(10, "9")
                        .addOperation(new Filter(16, "1"))
                        .addOperation(new Detect(10, "3", "7"))
                        .build())
                    .addCode(new CodeBuilder(6, "d 999 s2").build())
                    .build();

    context.cheat2 = new CheatBuilder("Sharable Cheat", "5F 46 41 4B 45 00")
                     .addCode(new CodeBuilder(12, "9").build())
                     .build();
    context.cheat3 = new CheatBuilder("Sharable Cheat 2", "5F 46 41 4B 45 00")
                     .addCode(new CodeBuilder(13, "9").build())
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
        print("cheat1 results: " + cheat1.getNumberOfValidResults())
        print("cheat2 results: " + cheat2.getNumberOfValidResults())
        print("cheat3 results: " + cheat3.getNumberOfValidResults())

    };

    context.info = function() {
    };
})(this)