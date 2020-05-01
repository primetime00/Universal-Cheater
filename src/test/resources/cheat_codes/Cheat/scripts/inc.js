(function(context) {
    context.initializeCheat = function(cheat) {
        context.cheat = cheat;
        logMessage("Cheat Initialized");

        cheat.setScriptHandler("BEFORE_WRITE", function(code) {
            print("Before Write " + code.toString())
            logMessage("Before Write");
        });

        cheat.setScriptHandler("AFTER_WRITE", function(code) {
            print("After Write " + code.toString())
            logMessage("After Write");
        });

    }




})(this)