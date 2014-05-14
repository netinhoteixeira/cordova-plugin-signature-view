var SignatureViewNative = {
    getSignature: function(successCallback, errorCallback) {
        var rest = Array.prototype.slice.call(arguments, 2);
        cordova.exec(function(data) {
            if (!data) {
                return successCallback(data);
            } else {
                /*
                 * Actually, the imageData width & height properties
                 * are defined as 'long' integers.  That would be 64
                 * bits.
                 */
                var trailerView = new DataView(data),
                width = trailerView.getUint32(data.byteLength - 8),
                height = trailerView.getUint32(data.byteLength - 4),
                // Because the ImageData constructor isn't available
                // in today's browsers, we have to work around it
                // by creating a bogus canvas and extracting the
                // imagedata from it.
                canvas = document.createElement('canvas');
                canvas.width = width;
                canvas.height = height;

                var ctx = canvas.getContext('2d'),
                imgData = ctx.createImageData(width, height),
                // We should be able to use imgData.data.set(), but
                // that doesn't work everywhere (Android WebView, I'm
                // looking at you) so we use this.
                // TODO: See if we can do it faster by looking at the
                // data as if it were a UInt32Array (see also comment below)
                pixelData = new Uint8Array(data, 0, data.byteLength - 8);
                for(var i = 0; i < pixelData.length; i+=4) {
                    // This is strange: It should be ARGB, but it is RGBA...
                    imgData.data[i] = pixelData[i];
                    imgData.data[i+1] = pixelData[i+1];
                    imgData.data[i+2] = pixelData[i+2];
                    imgData.data[i+3] = pixelData[i+3];
                }
                successCallback(imgData);
            }
        }, errorCallback, 'Signature', 'new', rest);
    }
};
module.exports = SignatureViewNative;