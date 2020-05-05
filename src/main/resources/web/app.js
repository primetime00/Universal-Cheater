var app = angular.module('uCheatApp', ["ngRoute", 'onsen']);
app.config(function($routeProvider, $locationProvider) {
  $routeProvider
  .when("/", {
    templateUrl : "app/main/main.html",
    controller: 'mainCtrl',
    state: 0
  })
  .when("/system", {
    templateUrl : "app/system/system.html",
    controller: 'systemCtrl',
    state: 0
  })
  .when("/games", {
    templateUrl : "app/games/games.html",
    controller: 'gamesCtrl',
    state: 1
  })
  .when("/cheats", {
    templateUrl : "app/cheats/cheats.html",
    controller: 'cheatsCtrl',
    state: 2
  });
  $locationProvider.html5Mode(true);
});

app.controller('headerCtrl', ['$rootScope', '$scope', 'appCom', function($rootScope, $scope, appCom) {
   $scope.title = "Loading...";
   $scope.state = 0;
   appCom.registerTitleChange(function(t) {
        $scope.title = t;
   });


   $scope.backClick = function() {
        $rootScope.$broadcast('backButton');
   }

    $scope.$on("$routeChangeSuccess",
        function handleRouteChangeEvent( event, c ) {
            if (c !== undefined)
                $scope.state = c.$$route.state;
        }
    );
}]);