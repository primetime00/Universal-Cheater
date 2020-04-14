(function(context) {
    context.GunAOB = AOBTools.createAOB("32 30 4D 4D 00 00 00 00 00");

    context.initialize = function() {
        Log.debug("Initializing Strike Commander Ammo script");
        context.searchComplete = false;
		context.searchList = createSearchList();
    };

    context.search = function(position, memory) {
		if (searchComplete)
			return;
        var results = ScriptTools.searchArray(position, memory, 0, GunAOB);
		searchList.addAll(results, position);
        scanResults(searchList, memory, position);
    };

	context.createInfo = function(info) {
		info['ammo'] = 0;
		info['ammoCheat'] = null;
		info['ammoValue'] = null;
		info['verifier'] = null;
		info['isPlayer'] = false;
	};

    context.scanResults = function(resultsSet, memory, position) {
		var results = resultsSet.getValidList(position);
        for (var i = 0; i<results.size(); ++i) {
            var res = results.get(i);
			if (!("ammo" in res.getScriptData())) {
				createInfo(res.getScriptData());
			}
			var info = res.getScriptData();
            var ammo = res.getBytesValue(memory, -7, 2);
			var valid = res.getBytesValue(memory, -12, 1);
			if (valid != 8) {
				res.setValid(false);
				continue;
			}
			if (ammo < 0  || ammo > 1000) {
				res.setValid(false);
				continue;
			}
			info['ammo'] = ammo;
			info['ammoCheat'] = res.createCheat("Unlimited Ammo", -7);
			info['ammoValue'] = createValue("1000");
			info['missleValue'] = createValue("2");
			info['verifier'] = res.createVerifier(0, GunAOB);
			info['isPlayer'] = false;
        }
    };

    context.apply = function() {
		var resultList = searchList.getAllValidList();
		if (resultList.size() == 0)
			return;
		if (searchList.getScriptData()['hasPlayer'] == undefined) {//we need to narrow down our list
			pruneResults();
			return;
		}
		var scriptData = resultList[0].getScriptData();
		var ammoCheat = scriptData['ammoCheat'];
		if (!("missleCheats" in scriptData))
			processMissleCheats(scriptData, resultList[0]);
		var verifier = scriptData['verifier'];
		var value = scriptData['ammoValue'];
		if (verifier.verify()) {
			ammoCheat.write(value);
			for (var i=0; i<scriptData['missleCheats'].length; ++i) {
				var ms = scriptData['missleCheats'][i];
				ms.write(scriptData['missleValue']);
			}
		}
		else {
			searchList.clear();
			searchComplete = false;
		}
    };

	context.processMissleCheats = function(scriptData, res) {
		var ammoCheat = scriptData['ammoCheat'];
		scriptData['missleCheats'] = []
		var pos = -7+35;
		for (var i=0; i<8; ++i) {
			var m1 = res.readValue(pos, 1);
			if (m1 > 0 && m1 < 7) {
				scriptData['missleCheats'].push(res.createCheat("Unlimited Missle " + i, pos))
			}
			pos+=35;
		}
	};

	context.pruneResults = function() {
		var validList = searchList.getAllValidList();
		for (var i=0; i<validList.size(); ++i) {
			var res = validList.get(i);
			var info = res.getScriptData();
			var oldAmmo = info['ammo'];
			var newAmmo = info['ammoCheat'].readValue(0, 2);
			if (oldAmmo != newAmmo) {//this one changed, it is valid
				if (oldAmmo - newAmmo <= 50) {
					searchComplete = true;
					info['isPlayer'] = true;
					searchList.getScriptData()['hasPlayer'] = true;
					removeAllNonPlayer(validList);
				}
			}
		}
	}

	context.removeAllNonPlayer = function(vList) {
		for (var i=0; i<vList.size(); ++i) {
			var res = vList.get(i);
			var info = res.getScriptData();
			if (info['isPlayer'] != true) {
				res.setValid(false);
			}
		}
	}

    context.getCheats = function() {
		if (searchList.getScriptData()['hasPlayer'] == true) {
			var validList = searchList.getAllValidList();
			for (var i=0; i<validList.size(); ++i) {
				var res = validList.get(i);
				var info = res.getScriptData();
				if (info['isPlayer'] == true) {
					var cheats = []
					cheats.push(info['ammoCheat'])
					if ("missleCheats" in info) {
						cheats = cheats.concat(info['missleCheats']);
					}
					return ScriptTools.getStatus(cheats);
				}
			}
		}
        return ScriptTools.getStatus([]);
    };


})(this);