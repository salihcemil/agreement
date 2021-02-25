"use strict";

 const app = angular.module('agreeAppModule', []);
/*
const app = angular.module('agreeAppModule', [ ]);
*/

app.config(['$qProvider', function ($qProvider) {
    $qProvider.errorOnUnhandledRejections(false);
}]);


/*
app.controller('agreeAppController', ['$http','$location','$log','$uibModal',function($http, $location,$log,$uibModal) {
*/

app.controller('agreeAppController', function($http, $location,$log,$scope) {
    const agreeApp = this;

 /* const apiBaseURL = "http://localhost:10051/";*/

   const apiBaseURL = "/"

/*
   var host = $location.host();
   const apiBaseURL = host+"/";
*/
    $scope.peers = [];
    $scope.agreements =  [];
   // agreeApp.thisNode = "abc";

   $http.get(apiBaseURL + "me").then((response) => agreeApp.thisNode = response.data.me);


    $http.get(apiBaseURL + "peers").then((response) => $scope.peers = response.data.peers);

 //   $log.info(peers);

   $http.get(apiBaseURL + "agreementsAll").then((response) => $scope.agreements = response.data);



});

