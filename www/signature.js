var signature = {
    getSignature: function(successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, 'Signature', 'new', []);
    },
};
module.exports = signature;