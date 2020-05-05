angular.module('uCheatApp').controller('mainCtrl', ['$scope', '$location', '$route', 'appCom',
    function($scope, $location, $route, appCom) {
        $scope.init = function() {
            appCom.setTitle("Universal Cheater");
            appCom.getState($scope.statusCB);
        }

        $scope.statusCB = function(status) {
            var state = status.state;
            switch (state) {
                default:
                    console.log('redirect...')
                    $location.path('/system');
                    $route.reload();
                    break;
                case 2:
                    console.log('st', status);
                    appCom.setSystem(status.system);
                    appCom.setGame(status);
                    $location.path('/cheats');
                    $route.reload();
                    break;
            }
        }

        $scope.init();
    }
]);