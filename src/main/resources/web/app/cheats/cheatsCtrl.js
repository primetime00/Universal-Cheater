angular.module('uCheatApp').controller('cheatsCtrl', ['$scope', '$location', '$route', '$interval', 'appCom',
    function($scope, $location, $route, $interval, appCom) {
        $scope.cheats = [];
        var ifunc = undefined;
        var hash = 0;
        var starting = true;

        $scope.init = function() {
            var system = appCom.getSystem();
            var game = appCom.getGame();
            $scope.starting = true;
            if (system.length == 0 || !('cht' in game)) {
                appCom.getState(function(resp) {
                    var status = resp.status;
                    var success = $scope.handleOperationStatus('getState', status);
                    if (!success) {
                        $location.path('/');
                        $route.reload();
                        return;
                    }
                    if (resp.system.length == 0) {
                        $location.path('/system');
                        $route.reload();
                        return;
                    }
                    else if (resp.game.cht.length == 0) {
                        appCom.setSystem(resp.system);
                        $location.path('/games');
                        $route.reload();
                        return;
                    }
                    //we should be in a good state
                    appCom.setSystem(resp.system);
                    appCom.setGame(resp.game);
                    $scope.start(resp.system, resp.game);
                });
            }
            else {
                $scope.start(system, game);
            }
        }

        $scope.start = function(system, game) {
            appCom.setTitle(game['game']);
            appCom.getCheats(system, game, $scope.cheatsCB);
        }

        $scope.$on('$destroy', function() {
           $scope.destruct();
       });

       $scope.destruct = function() {
           if (ifunc !== undefined) {
                $interval.cancel(ifunc);
                ifunc = undefined;
           }
       }

        $scope.cheatsCB = function(cheat) {
            if (cheat.status == "FAIL") {//we failed
                ons.notification.toast({'message': cheat.message, 'timeout':2000})
                $location.path('/games');
                $route.reload();
                return;
            }
            //the game should be running. lets start to get the cheat interval
            $scope.starting = false;
            appCom.getState(function(data)
            {
                $scope.onCheatUpdate(true, data);
            }
            );

            ifunc = $interval(function()
            {
                appCom.getState(function(data)
                {
                    $scope.onCheatUpdate(false, data);
                })
            },
            1000);

        }

        $scope.onGame = function(data) {
            appCom.setGame(data);
            $location.path('/cheat');
        }

        $scope.onCheatUpdate = function(initial, data) {
            var status = data.status;
            if (status == "FAIL") {
                //ons.notification.toast({'message': data.message, 'timeout':2000})
                $location.path('/games');
                $route.reload();
                return;
            }
            if (data.hash != $scope.hash) {
                $scope.cheats = data.cheatList;
                $scope.hash = data.hash;
                $scope.$apply();
            }
        }

        $scope.$on('backButton', function(event, args) { //we need to exit the game
            appCom.exitCheat(function(data) {
                if (data.status == "SUCCESS") {//we exited
                    $location.path('/games');
                    $route.reload();
                }
                else {
                    ons.notification.toast({'message': data.message, 'timeout':2000})
                    $location.path('/');
                    $route.reload();
                }
            });
        })

        $scope.getItemColor = function(cheat) {
            if (!('hasCheats' in cheat) || !cheat.hasCheats) {
                return {'color': 'lightgray'}
            }
            if ('trigger' in cheat && 'on' in cheat['trigger'] && cheat['trigger']['on'])
                return {'color': 'green'}
            if ('trigger' in cheat && 'on' in cheat['trigger'] && !cheat['trigger']['on'])
                return {'color': 'red'}
            if (!('trigger' in cheat))
                return {'color': 'black'}
            return {'color': 'yellow'}
        }

        $scope.triggerCheat = function(cheat) {
            appCom.triggerCheat(cheat, function(res) { $scope.handleOperationStatus('triggerCheat', res.status); });
        }


        $scope.toggleCheat = function(cheat) {
            appCom.toggleCheat(cheat, function(res) { $scope.handleOperationStatus('toggleCheat', res.status); });
        }

        $scope.resetCheat = function(cheat) {
            appCom.resetCheat(cheat, function(res) { $scope.handleOperationStatus('resetCheat', res.status); });
        }

        $scope.handleOperationStatus = function(operation, status) {
            if (status == "FAIL") {
                ons.notification.toast({'message': 'failed operation: ' + operation, 'timeout':2000})
                return false;
            }
            return true;
        }

        $scope.getTriggerName = function(cheat) {
            if (!('trigger' in cheat))
                return "";
            var trigger = cheat.trigger;
            if (!('behavior' in trigger))
                return "";
            var tb = trigger['behavior'];
            if (tb == 'toggle') return 'Toggle';
            else if (tb == 'onetime') return 'Trigger';
            else return 'Hold';
        }

        $scope.hasButtonTrigger = function(cheat) {
            if (!('trigger' in cheat))
                return false;
            var trigger = cheat.trigger;
            if (!('type' in trigger))
                return false;
            var type = trigger['type'];
            if (type == 'button') return true;
            return false;
        }

        $scope.$on('network.error', function(evt, error) { //we need to exit the game
            ons.notification.toast({'message': 'Network error: Check Application...', 'timeout':2000})
            $scope.destruct();
            $location.path('/');
            $route.reload();
        });

        $scope.$on('device.back', function(evt) { //we need to exit the game
            console.log("DEVICE BACK")
            appCom.exitCheat(function(data) {
                if (data.status == "SUCCESS") {//we exited
                    $location.path('/games');
                    $route.reload();
                }
                else {
                    ons.notification.toast({'message': data.message, 'timeout':2000})
                    $location.path('/');
                    $route.reload();
                }
            });
        });





        $scope.init();
    }
]);