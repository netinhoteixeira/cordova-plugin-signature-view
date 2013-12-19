Cordova Signature View plugin
=============================

This plugin provides a simple "pad" for requesting a user's signature
which you can use in Cordova/Phonegap applications.  For efficiency
reasons, the pad is implemented natively (currently only for Android).

This works best with an "active" pen, to get the most detailed output.
It captures normal "touch" events, so you could use it with a pressure
pen or fingers, but the results won't be as accurate.

In principle, you could use the plugin as a generic drawing/sketch
capturing system, but it provides no drawing "tools" of any kind.

Usage
-----

This extension provides a single class-like object which has a method
attached to it called `getSignature`.  You can load it by requiring
the module `nl.codeyellow.signature.Signature` and it has the
following signature (no pun intended):

	:::javascript
	Signature.getSignature(success, error, file, [title]);

The `success` argument is a callback function accepting one argument,
which is either null (in case the user canceled the dialog) or an
[ImageData](http://www.w3.org/html/wg/drafts/2dcontext/html5_canvas/#imagedata)
object containing the raw binary image data.

The `error` argument is a callback function accepting one argument,
which is a string containing an error message indicating what went
wrong.

The `file` argument is a string indicating the filename to which the
JPEG image should be saved.

The `title` argument is an optional string which indicates what the
dialog should show as a heading.

Beware when converting the image data to a data URI: some mobile
browsers (and IE) have length limitations on data URIs.  It's better
to use the image data with the canvas/2dcontext `putImageData` method
or something equivalent.

Example
-------

Here's a minimal working example.  It assumes there's an element in
your HTML document which has an id of `signature`, which can receive
images, and that the "file" plugin has been installed.

You'll need to take care of actually managing the files yourself.

	:::javascript
	var Signature = cordova.require('nl.codeyellow.signature.Signature');
	window.requestFileSystem(LocalFileSystem.PERSISTENT, 0, gotFS, fail);
	function fail() { alert("Couldn't access FS!"); }
	function gotFS(fs) {
		Signature.getSignature(
			// This should be unique but for the demo we don't care.
			// Please note that replacing a file with the same name
			// won't have any effect.
			fs.fullPath + '/image.jpg',
			function (filename) {
				/* This is the "success" callback. */
				if (!filename) return; // User clicked cancel, we got no image data.
		
				var sig = document.getElementById('signature');
				var imgs = sig.getElementsByTagName('img');
				for (var i = 0; i < imgs.length; i++) {
					sig.removeChild(imgs[i]);
				}

				var img = new Image();
				img.src = fs.toURL() + '/' + filename;
				console.log(img.src);
				sig.appendChild(img);
			}, function (msg) {
				/* This is the "error" callback. */
				alert("Could not obtain a signature due to an error: "+msg);
			},
			/* This final string is optional and defaults to a similar string. */
			'Please put your signature down below');
	}