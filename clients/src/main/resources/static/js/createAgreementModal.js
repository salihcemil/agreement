"use strict";

angular.module('demoAppModule').controller('CreateAgreementModalCtrl', function($http, $uibModalInstance, $uibModal, apiBaseURL, peers) {
    const createAgreementModal = this;

    createAgreementModal.peers = peers;
    createAgreementModal.form = {};
    createAgreementModal.formError = false;

    /** Validate and create an Agreement. */
    createAgreementModal.create = () => {
        if (invalidFormInput()) {
            createAgreementModal.formError = true;
        } else {
            createAgreementModal.formError = false;

            const amount = createAgreementModal.form.amount;
            const currency = createAgreementModal.form.currency;
            const party = createAgreementModal.form.counterparty;

            $uibModalInstance.close();

            // We define the Agreement creation endpoint.
            const issueAgreementEndpoint =
                apiBaseURL +
                `create-agreement?issuer=${party}&quantity=${amount}&currencyCode=${currency}`;

        console.log(issueAgreementEndpoint);
            // We hit the endpoint to create the Agreement and handle success/failure responses.
            $http.put(issueAgreementEndpoint).then(
                (result) => createAgreementModal.displayMessage(result),
                (result) => createAgreementModal.displayMessage(result)
            );
        }
    };

    /** Displays the success/failure response from attempting to create an Agreement. */
    createAgreementModal.displayMessage = (message) => {
        const createAgreementMsgModal = $uibModal.open({
            templateUrl: 'createAgreementMsgModal.html',
            controller: 'createAgreementMsgModalCtrl',
            controllerAs: 'createAgreementMsgModal',
            resolve: {
                message: () => message
            }
        });

        // No behavAgreementr on close / dismiss.
        createAgreementMsgModal.result.then(() => {}, () => {});
    };

    /** Closes the Agreement creation modal. */
    createAgreementModal.cancel = () => $uibModalInstance.dismiss();

    // Validates the Agreement.
    function invalidFormInput() {
        return isNaN(createAgreementModal.form.amount) || (createAgreementModal.form.counterparty === undefined);
    }
});

// Controller for the success/fail modal.
angular.module('demoAppModule').controller('createAgreementMsgModalCtrl', function($uibModalInstance, message) {
    const createAgreementMsgModal = this;
    createAgreementMsgModal.message = message.data;
});