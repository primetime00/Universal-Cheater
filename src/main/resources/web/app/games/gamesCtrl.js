angular.module('uCheatApp').controller('gamesCtrl', ['$scope', '$location', '$route', 'appCom',
    function($scope, $location, $route, appCom) {
        $scope.games = [];

        $scope.init = function() {
            var system = appCom.getSystem();
            if (system.length == 0) {
                $location.path('/system');
                $route.reload();
            }
            else {
                appCom.setTitle(system);
                appCom.getGames(system, $scope.gamesCB);
            }
        }

        $scope.gamesCB = function(gameData) {
            var status = gameData.status;
            if (status != 'SUCCESS') { //something bad happened go back to beginning
                $location.path('/');
                $route.reload();
                return;
            }
            var list = gameData.gameList;

            $scope.games = list;
            $scope.$apply();
        }

        $scope.onGame = function(data) {
            appCom.setGame(data);
            $location.path('/cheats');
        }

        $scope.$on('backButton', function(event, args) {
            $location.path('/system');
            $route.reload();
        })

        $scope.$on('device.back', function(evt) { //we need to exit the game
            console.log("DEVICE BACK")
            $location.path('/system');
            $route.reload();
        });


        $scope.init();
    }
]);