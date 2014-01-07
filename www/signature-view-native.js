var SignatureViewNative = {
    getSignature: function(filename, successCallback, errorCallback) {
        var rest = Array.prototype.slice.call(arguments, 3);
        rest.unshift(filename);
        cordova.exec(successCallback, errorCallback, 'Signature', 'new', rest);
    }
};
module.exports = SignatureViewNative;