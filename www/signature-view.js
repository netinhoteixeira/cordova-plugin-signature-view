(function() {
    'use strict';
    
    var signature = {
        getSignature: function(filename, successCallback, errorCallback) {
            // Are we on a cordova device (no desktop browser), and is
            // it one of the supported platforms for the native view?
            // XXX: This really requires waiting for deviceReady.
            // OTOH, who's going to present a signature pad first
            // thing upon startup?
            if (typeof window.cordova === 'object' &&
                typeof window.cordova.require === 'function' &&
                typeof window.device === 'object' &&
                ['Android'].indexOf(window.device.platform) !== -1) {
                var SignatureViewNative = window.cordova.require('nl.codeyellow.signature.Signature');
                SignatureViewNative.getSignature.apply(arguments);
            } else {
                signature.getSignatureFallback.apply(arguments);
            }
        },
        getSignatureFallback: function(a, b, c, d, e, f) {
            /* var rest = Array.prototype.slice.call(arguments, 3);
               rest.unshift(filename); */
            console.log('fallback, yeah!', a, b, c, d, e, f);
        }
    };
    // Export in an AMD-compliant way, without requiring an AMD loader
    if (typeof module === 'object' && module && typeof module.exports === 'object') {
        module.exports = signature;
    } else {
        window.SignatureView = signature;
        if (typeof define === 'function' && define.amd) {
            define('cordova.signature-view', [], function() { return signature; });
        }
    }
})();