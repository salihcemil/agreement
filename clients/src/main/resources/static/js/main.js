"use strict";

// Define your backend here.
angular.module('demoAppModule', ['ui.bootstrap']).controller('DemoAppCtrl', function($http, $location, $uibModal) {
    const demoApp = this;

    const apiBaseURL = "/api/agreement/";

    // Retrieves the identity of this and other nodes.
    let peers = [];
    $http.get(apiBaseURL + "me").then((response) => demoApp.thisNode = response.data.me);
    $http.get(apiBaseURL + "peers").then((response) => peers = response.data.peers);

    demoApp.openCreateAgreementModal = () => {
        const createAgreementModal = $uibModal.open({
            templateUrl: 'createAgreementModal.html',
            controller: 'CreateAgreementModalCtrl',
            controllerAs: 'createAgreementModal',
            resolve: {
                apiBaseURL: () => apiBaseURL,
                peers: () => peers
            }
        });

        // Ignores the modal result events.
        createAgreementModal.result.then(() => {}, () => {});
    };

    /** Displays the IOU creation modal. */
    demoApp.openCreateIOUModal = () => {
        const createIOUModal = $uibModal.open({
            templateUrl: 'createIOUModal.html',
            controller: 'CreateIOUModalCtrl',
            controllerAs: 'createIOUModal',
            resolve: {
                apiBaseURL: () => apiBaseURL,
                peers: () => peers
            }
        });

        // Ignores the modal result events.
        createIOUModal.result.then(() => {}, () => {});
    };

    demoApp.openConvertModal = () => {
                const convertModal = $uibModal.open({
                    templateUrl: 'convertModal.html',
                    controller: 'ConvertModalCtrl',
                    controllerAs: 'convertModal',
                    resolve: {
                        apiBaseURL: () => apiBaseURL
                    }
                });

                convertModal.result.then(() => {}, () => {});
            };

    /** Displays the cash issuance modal. */
    demoApp.openIssueCashModal = () => {
        const issueCashModal = $uibModal.open({
            templateUrl: 'issueCashModal.html',
            controller: 'IssueCashModalCtrl',
            controllerAs: 'issueCashModal',
            resolve: {
                apiBaseURL: () => apiBaseURL
            }
        });

        issueCashModal.result.then(() => {}, () => {});
    };

    /** Displays the IOU transfer modal. */
    demoApp.openTransferModal = (id) => {
        const transferModal = $uibModal.open({
            templateUrl: 'transferModal.html',
            controller: 'TransferModalCtrl',
            controllerAs: 'transferModal',
            resolve: {
                apiBaseURL: () => apiBaseURL,
                peers: () => peers,
                id: () => id
            }
        });

        transferModal.result.then(() => {}, () => {});
    };



    /** Displays the IOU settlement modal. */
    demoApp.openSettleModal = (id) => {
        const settleModal = $uibModal.open({
            templateUrl: 'settleModal.html',
            controller: 'SettleModalCtrl',
            controllerAs: 'settleModal',
            resolve: {
                apiBaseURL: () => apiBaseURL,
                id: () => id
            }
        });

        settleModal.result.then(() => {}, () => {});
    };

    /** Refreshes the front-end. */
    demoApp.refresh = () => {
        // Update the list of IOUs.
        $http.get(apiBaseURL + "ious").then((response) => demoApp.ious =
            Object.keys(response.data).map((key) => response.data[key].state.data));

        $http.get(apiBaseURL + "agreements").then((response) => demoApp.agreements =
            Object.keys(response.data).map((key) => response.data[key].state.data));

        /*// Update the cash balances.
        $http.get(apiBaseURL + "cash-balances").then((response) => demoApp.cashBalances =
            response.data);*/
    }

    demoApp.refresh();
});

// Causes the webapp to ignore unhandled modal dismissals.
angular.module('demoAppModule').config(['$qProvider', function($qProvider) {
    $qProvider.errorOnUnhandledRejections(false);
}]);