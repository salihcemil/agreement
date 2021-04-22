"use strict";

angular.module('demoAppModule').controller('ConvertModalCtrl', function ($http, $uibModalInstance, $uibModal, apiBaseURL) {
    const convertModal = this;

    convertModal.form = {};

    convertModal.convert = () => {
            $uibModalInstance.close();
////////////////////////
            const convertEndpoint =
                apiBaseURL +
                `convertAgreements`;

            $http.put(convertEndpoint).then(
                (result) => transferModal.displayMessage(result),
                (result) => transferModal.displayMessage(result)
            );
    };

    convertModal.displayMessage = (message) => {
        const convertMsgModal = $uibModal.open({
            templateUrl: 'convertMsgModal.html',
            controller: 'convertMsgModalCtrl',
            controllerAs: 'convertMsgModal',
            resolve: { message: () => message }
        });

        convertMsgModal.result.then(() => {}, () => {});
    };

    convertModal.cancel = () => $uibModalInstance.dismiss();

});

angular.module('demoAppModule').controller('convertMsgModalCtrl', function ($uibModalInstance, message) {
    const convertMsgModal = this;
    convertMsgModal.message = message.data;
});