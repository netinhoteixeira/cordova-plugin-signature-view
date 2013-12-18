var signature = {
    getSignature: function(successCallback, errorCallback) {
        var rest = Array.prototype.slice.call(arguments, 2);
        cordova.exec(successCallback, errorCallback, 'Signature', 'new', rest);
    },
};
module.exports = signature;