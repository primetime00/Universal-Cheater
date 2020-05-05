angular.module('uCheatApp').controller('systemCtrl', ['$scope', '$location', '$route', 'appCom',
    function($scope, $location, $route, appCom) {
        $scope.systems = [];

        $scope.init = function() {
            appCom.setTitle("Universal Cheater");
            appCom.getSystems($scope.systemCB);
        }

        $scope.systemCB = function(systems) {
            $scope.systems = systems;
            $scope.$apply();
        }

        $scope.onSystem = function(sys) {
            appCom.setSystem(sys);
            $location.path('/games');
        }

        $scope.$on('device.back', function(evt) { //we need to exit the game
            console.log("BACK")
            ons.notification.toast({'message': "Can't go back...", 'timeout':2000})
            $location.path('/system');
            $route.reload();
        });

        $scope.init();
    }
]);