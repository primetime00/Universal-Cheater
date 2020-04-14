(function(context) {
    context.InvAOB = AOBTools.createAOB("50 4C 41 59 45 52 00 00 00 00 00 00");
	context.InvValue = createValue("6300 6300 6300 6300 6300 6300 6300 6300 6300");

    context.initialize = function() {
        Log.debug("Initializing Strike Commander Inventory Script");
        context.searchComplete = false;
		context.searchList = createSearchList();
    };

    context.search = function(position, memory) {
		if (searchComplete)
			return;
        var results = ScriptTools.searchArray(position, memory, 0, InvAOB);
		searchList.addAll(results, position);
		var searchResults = searchList.getAllValidList();
		for (var i=0; i<searchList.getAllValidList().size(); ++i) {
			var res = searchResults.get(i);
			if (res.getScriptData()['cheat'] == undefined) {
				res.getScriptData()['cheat'] = res.createCheat("Inventory", -42);
				res.getScriptData()['verifier'] = res.createVerifier(0, InvAOB);
			}
		}
    };

    context.apply = function() {
		var resultList = searchList.getAllValidList();
		if (resultList.size() == 0)
			return;
		var searchResults = searchList.getAllValidList();
		for (var i=0; i<searchList.getAllValidList().size(); ++i) {
			var res = searchResults.get(i);
			var info = res.getScriptData();
			if (info['cheat'] != undefined) {
				if (info['verifier'].verify()) {
					info['cheat'].write(InvValue);
				}
				else {
					delete info['cheat'];
					delete info['verifier'];
					searchList.remove(res);
				}
			}
		}
    };

    context.getCheats = function() {
		var searchResults = searchList.getAllValidList();
		var cheats = []
		for (var i=0; i<searchList.getAllValidList().size(); ++i) {
			var res = searchResults.get(i);
			if (res.getScriptData()['cheat'] != undefined) {
				cheats.push(res.getScriptData()['cheat']);
			}
		}
		return ScriptTools.getStatus(cheats);
    };


})(this);