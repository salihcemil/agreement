"use strict";

// Similar to the IOU creation modal - see createIOUModal.js for comments.
angular.module('demoAppModule').controller('SettleModalCtrl', function($http, $uibModalInstance, $uibModal, apiBaseURL, id) {
    const settleModal = this;

    settleModal.id = id;
    settleModal.form = {};

    settleModal.settle = () => {

            $uibModalInstance.close();

            const issueIOUEndpoint =
                apiBaseURL +
                `consumeIOU?id=${id}`;

            $http.put(issueIOUEndpoint).then(
                (result) => settleModal.displayMessage(result),
                (result) => settleModal.displayMessage(result)
            );

    };

    settleModal.displayMessage = (message) => {
        const settleMsgModal = $uibModal.open({
            templateUrl: 'settleMsgModal.html',
            controller: 'settleMsgModalCtrl',
            controllerAs: 'settleMsgModal',
            resolve: {
                message: () => message
            }
        });

        settleMsgModal.result.then(() => {}, () => {});
    };

    settleModal.cancel = () => $uibModalInstance.dismiss();


});

angular.module('demoAppModule').controller('settleMsgModalCtrl', function($uibModalInstance, message) {
    const settleMsgModal = this;
    settleMsgModal.message = message.data;
});