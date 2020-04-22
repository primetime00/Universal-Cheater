(function(context) {

    context.cheat1 = new Cheat("Cheat 1", "46 41 4B 45 00",
        new Code(7, "9",
            new Filter(5, "1"), new Detect(7, "3", "7")),
        new Code(11, "999"));
    context.cheat2 = new Cheat("Sharable Cheat", "46 41 4B 45 00",
        new Code(-12, "9"));
    context.cheat3 = new Cheat("Sharable Cheat 2", "46 41 4B 45 00",
        new Code(-8, "9"));

    context.initialize = function() {
        logMessage("initialize");
        print("Initializing script...");
    };

    context.search = function(memory, base) {
        logMessage("search");
        print("Searching for cheats...");
        cheatSearch(cheat1, memory, base);
        //cheatSearch(cheat2, memory, base);
        //cheatSearch(cheat3, memory, base);
        logMessage("cheat1 results: " + cheat1.getNumberOfValidResults())
    };

    context.write = function() {
        print("Writing cheats...");
        writeCheat(cheat1);
        writeCheat(cheat2);
        writeCheat(cheat3);
    };

    context.searchComplete = function() {
        logMessage("search complete");
        print("Searching is complete");
        print("cheat1 results: " + cheat1.getNumberOfValidResults())
        print("cheat2 results: " + cheat2.getNumberOfValidResults())
        print("cheat3 results: " + cheat3.getNumberOfValidResults())

    };

    context.info = function() {
    };
})(this)