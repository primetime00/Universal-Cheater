(function(context) {
	var aob = "00 00 00 00 00 00 00 00 ?? ?? ?? ?? ?? 01 ?? ?? ?? 12 ?? 00 00 ?? ??"
	var speedAOB = "?? 6C 61 73 72 2D 33"
	var timeAOB = "A6 64 01 00 01 00 10 00 10 00 01 00 00 00 01 01 00 02 01 00 01 01 01 01 01 0C 0C 0C"

    context.shieldCheat = new CheatBuilder("Infinite Shields", aob)
                    .setIdentity(1)
                    .setScript(_script)
                    .setStopSearchOnResult(true)
                    .addCode(new CodeBuilder(8, "b 10271027").build())
                    .build();

    context.laserCheat = new CheatBuilder("Infinite Laser", aob)
                    .setIdentity(2)
                    .addCode(new CodeBuilder(100, "d 127 s 2")
							 .addOperation(new Instance(1))
							 .build())
                    .setStopSearchOnResult(true)
                    .setScript(_script)
                    .build();

    context.laserRateCheat = new CheatBuilder("Fast Lasers", aob)
                    .setIdentity(3)
                    .addCode(new CodeBuilder(34, "b 00000000")
							 .addOperation(new Instance(1))
							 .build())
                    .setStopSearchOnResult(true)
                    .setScript(_script)
                    .build();
					
    context.missileCheat = new CheatBuilder("Unlimited Projectiles", aob)
                    .setIdentity(4)
                    .setStopSearchOnResult(true)
                    .setScript(_script)
                    .build();
					
    context.missileRateCheat = new CheatBuilder("Fast Projectiles", aob)
                    .setIdentity(5)
                    .addCode(new CodeBuilder(48, "b 0000")
							 .addOperation(new Instance(1))
							 .build())
                    .setStopSearchOnResult(true)
                    .setScript(_script)
                    .build();
					
    context.instantLockCheat = new CheatBuilder("Instant Lock", aob)
                    .setIdentity(6)
                    .addCode(new CodeBuilder(52, "b 000F")
							 .addOperation(new Instance(1))
							 .build())
                    .setStopSearchOnResult(true)
                    .setScript(_script)
                    .build();
					
    context.timeCheat = new CheatBuilder("Unlimited Mission Time", timeAOB)
                    .setIdentity(8)
                    .addCode(new CodeBuilder(1956, "d 15203 s 2")
							 .build())
                    .setStopSearchOnResult(true)
                    .setScript(_script)
                    .build();
					
					
    context.boostCheat = new CheatBuilder("Engine Boost", speedAOB)
                    .setIdentity(7)
                    .setStopSearchOnResult(true)
					.addCode(new CodeBuilder(0xF6F+(84*(0)), "d 3000 s 2")
						.build())							
                    .setScript(_script)
					.setTrigger(new Trigger("keypress", "hold", 90))
                    .build();

					
					
					


    context.initialize = function() {
		context.foundPlayer = {};
		context.boostUnit = 0;
		context.numberOfLasers = 0;
        shieldCheat.setScriptHandler("ON_RESULTS", context.shieldResults);
        laserCheat.setScriptHandler("AFTER_WRITE", context.laserWrite);
		boostCheat.setScriptHandler("ON_TRIGGER", context.triggerBoost);
		//boostCheat.setScriptHandler("ON_RESULTS", context.boostResults);
		context.bMonitor = boostCheat.addMonitor(0x3675, 1);
		boostCheat.setScriptHandler("ON_MONITOR_CHANGE", context.boostMonitorChanged);
    };
	
	context.boostMonitorChanged = function(monitor, result, value) {
		print("MON");
		if (monitor == bMonitor) {
			print("Found unit", value);
			boostUnit = value;
			boostCheat.removeCodes();
			boostCheat.addCode(new CodeBuilder(0xF6F+(84*(boostUnit)), "d 3000 s 2")
				.build());			
		}
	}
	
	context.findLocations = function(numLasers) {
		missileCheat.removeCodes();
		missileCheat.addCode(new CodeBuilder(100+(4*(numLasers))+1, "d 9 s 1")
									.addOffset(100+(4*(numLasers))+5, "d 9 s 1")
									.addOperation(new Instance(1))
									.build());		
	}

    context.shieldResults = function(results, position, memory) {
		if (foundPlayer[position] == undefined)			
			foundPlayer[position] = false;
        for (var i=0; i<results.length; ++i) {
            //filter invalid results
            var amount = results[i].getBytesValue(memory, 8, 2);
            if (amount > 10000) {
                results[i].setValid(false);
                continue;
            }
            var playerResults = searchRange(memory, position, results[i].getOffset()-9000, 9000, "6E 75 6C 6C 20 70 6F 69 6E 74 65 72");
            var isPlayer = playerResults.size() > 0;
            if (isPlayer) {
                results[i].setValid(true);
				print("FOUND PLAYER");
				numberOfLasers = results[i].readValue(16, 1)
                foundPlayer[position] = true;
            }
            else {
                results[i].setValid(false);
            }
        }
		print("found", foundPlayer[position]);
    }
	
    context.laserWrite = function(results) {
		if (numberOfLasers == 0) {
			print("Invalid number of lasers...");
			return;
		}
        for (var i=0; i<results.length; ++i) {
            var pos = 104;
            var base = results[i].getAddress();
            for  (var j=1; j<numberOfLasers; ++j) {
				writeResult(results[i], pos, "d 127 s 1");
                pos+=4;
            }
        }
    }
	
	context.triggerBoost = function(info, results) {
		if (results.size() == 0)
			return;
		var res = results.get(0);
		if (!info.isDown()) { //we stopped our boost
			writeResult(res, 0xF6F+(84*boostUnit), "d 80 s 2");
		}
		
	}

    context.cheatSuccess = function(cheat, codesWritten, totalCodes) {
    }

    context.cheatFailed = function(cheat) {
        print("Failed to find AOB", cheat.getName())
		missileCheat.removeCodes();
		if (cheat == boostCheat) {
			boostCheat.removeCodes();
			boostCheat.queueReset();
		}
		foundPlayer = {};
		numberOfLasers = 0;		
    }

    context.searchComplete = function() {
		var foundAny = false;
		for (var m in foundPlayer) {
			if (foundPlayer[m])
			{
				foundAny = true;
				break;
			}
		}
		if (foundAny && missileCheat.getCodes().size() == 0) {
			findLocations(numberOfLasers);
		}
		else if (!foundAny) {
			numberOfLasers = 0;
			shieldCheat.queueReset();	
		}
    };

    context.info = function() {
    };

})(this);