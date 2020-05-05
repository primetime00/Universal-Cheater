angular.module('uCheatApp').service('appCom', ["$rootScope", function($rootScope) {
    this.title = "";
    this.system = "";
    this.game = {}
    this.titleChangeHandlers = new Set();

    ons.ready(function() {
        ons.setDefaultDeviceBackButtonListener(function() {
            $rootScope.$broadcast('device.back');

        });
   });


    this.getState = function(callback) {
        axios.get('getCheatStatus')
            .then(function(resp) {
                callback(resp.data);
            })
            .catch(function(error) {
                $rootScope.$broadcast('network.error', error)
            });
    }

    this.getSystems = function(callback) {
        axios.get('getSystems')
            .then(function(resp) {
                callback(resp.data);
            })
            .catch(function(error) {
                $rootScope.$broadcast('network.error', error)
            });
    }

    this.getGames = function(system, callback) {
        axios.get('getGameCheats/'+system)
            .then(function(resp) {
                callback(resp.data);
            })
            .catch(function(error) {
                $rootScope.$broadcast('network.error', error)
            });
    }

    this.getCheats = function(system, game, callback) {
        axios.post('runGameCheat', {"system": system, "cht": game.cht })
            .then(function(resp) {
                console.log(resp);
                callback(resp.data);
        })
        .catch(function(error) {
            $rootScope.$broadcast('network.error', error)
        });
    }

    this.exitCheat = function(callback) {
        axios.post('exitCheat')
            .then(function(resp) {
                callback(resp.data);
            })
            .catch(function(error) {
                $rootScope.$broadcast('network.error', error)
            });
    }

    this.toggleCheat = function(cheat, callback) {
        axios.post('toggleGameCheat', cheat.id)
            .then(function(resp) {
                if (callback !== null)
                    callback(resp.data);
            })
            .catch(function(error) {
                $rootScope.$broadcast('network.error', error)
            });
    }

    this.resetCheat = function(cheat, callback) {
        axios.post('resetGameCheat', cheat.id)
            .then(function(resp) {
                if (callback !== null)
                    callback(resp.data);
            })
            .catch(function(error) {
                $rootScope.$broadcast('network.error', error)
            });
    }

    this.triggerCheat = function(cheat, callback) {
        axios.post('triggerGameCheat', cheat.id)
            .then(function(resp) {
                if (callback !== null)
                    callback(resp.data);
            })
            .catch(function(error) {
                $rootScope.$broadcast('network.error', error)
            });
    }

    this.setTitle = function(title) {
        this.title = title;
        var s = this.titleChangeHandlers;
        for (var it = s.values(), val= null; val=it.next().value; ) {
            val(title);
        }
    }

    this.registerTitleChange = function(func) {
        this.titleChangeHandlers.add(func);

    }

    this.setSystem = function(sys) {
        this.system = sys;
    }

    this.getSystem = function() {
        return this.system;
    }

    this.setGame = function(game) {
        this.game = game;
    }

    this.getGame = function() {
        return this.game;
    }

}]);