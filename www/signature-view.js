(function () {
    'use strict';

    var signature = {
        getSignature: function (successCallback, errorCallback) {
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
                SignatureViewNative.getSignature.apply(SignatureViewNative.getSignature, arguments);
            } else {
                signature.getSignatureFallback.apply(signature, arguments);
            }
        },
        getSignatureFallback: function (successCallback, errorCallback, title, webpage) {
            title = title || "Please sign below";
            var popup = document.createElement('div'),
                    cleanUp = function () {
                        okButton.removeEventListener('click', okFun);
                        cancelButton.removeEventListener('click', cancelFun);
                        canvas.removeEventListener('touchstart', touchStart);
                        // This next one might've been unset before by the touchstart handler
                        canvas.removeEventListener('mousedown', this.mouseDownEvent);
                        document.removeEventListener('scroll', determineOffset);
                        popup.remove();
                    }.bind(this), okFun = function (ev) {
                var imgData = null;
                if (this.boundingBox) {
                    var ctx = canvas.getContext('2d'),
                            bb = this.boundingBox;
                    imgData = ctx.getImageData(bb.left, bb.top,
                            bb.right - bb.left + 1,
                            bb.bottom - bb.top + 1);
                }
                cleanUp();
                successCallback(imgData);
            }.bind(this), cancelFun = function (ev) {
                cleanUp();
                successCallback(null);
            },
                    okButton, cancelButton, canvas,
                    touchStart = this.touchStart.bind(this),
                    determineOffset = this.determineOffset.bind(this);

            popup.id = 'cordova.signature-view:popupwindow';
            popup.style.position = 'fixed';
            popup.style.top = '0';
            popup.style.left = '0';
            popup.style.width = '100%';
            popup.style.height = '100%';
            popup.style.backgroundColor = 'rgba(0, 0, 0, 0.5)';
            // TODO: Translatable strings for OK/Cancel, make colors configurable. Inline styling is also ugly
            popup.innerHTML = '<div style="position: relative; margin: 2em auto; width: 20%; height: 80%; background-color: black;">' +
                    '  <div style="color: white">' +
                    '    <h3 style="margin: 0.1em 0;" id="cordova.signature-view:title"></h3>' +
                    '    <h3 style="margin: 0.1em 0; position: absolute; right: 0.5em; top: 0; cursor: pointer;" id="cordova.signature-view:cancel">╳</span>' +
                    '  </div>' +
                    // TODO: Find out an elegant way to automatically determine the size of the webpage, and use the rest for signature.
                            (webpage ? '<div style="height: 50%; min-height: 50%; width: 100%"><object>' + webpage + '</object></div>' : '') +
                            '  <div style="position: relative"><canvas style="width: 100%; min-height: ' + (webpage ? '50%' : '100%') + '" id="cordova.signature-view:pad"></canvas></div>' +
                            '  <div><button style="width: 100%" id="cordova.signature-view:ok">ok</button></div>' +
                            '</div>';
                    document.body.appendChild(popup);
                    document.getElementById('cordova.signature-view:title').appendChild(document.createTextNode(title));
                    okButton = document.getElementById('cordova.signature-view:ok');
                    okButton.addEventListener('click', okFun);
                    cancelButton = document.getElementById('cordova.signature-view:cancel');
                    cancelButton.addEventListener('click', cancelFun);

                    // A little bit ugly that we rely on "this" so much here, but hey it works
                    this.el = canvas = document.getElementById('cordova.signature-view:pad');
                    this.determineOffset();
                    canvas.addEventListener('touchstart', touchStart, false);
                    document.addEventListener('scroll', determineOffset, false);
                    this.mouseDownEvent = this.touchStart.bind(this); // So we can unset it upon touch
                    canvas.addEventListener('mousedown', this.mouseDownEvent, false);
                    // Force the canvas to actually be its calculated size.  If we don't do that,
                    // the drawing area will be small and the canvas element will appear
                    // "blown up" with huge pixels.
                    var w = $(canvas).width(), h = $(canvas).height();
                    canvas.width = w;
                    canvas.height = h;
                    this.clear(canvas);
                },
                determineOffset: function () {
                    var el, doc = document.documentElement, x = -doc.offsetLeft, y = -doc.offsetTop;
                    for (el = this.el; el.offsetParent && el.offsetParent != doc; el = el.offsetParent) {
                        x += el.offsetLeft - el.scrollLeft;
                        y += el.offsetTop - el.scrollTop;
                    }
                    this.offset = {x: x, y: y};
                },
                touchStart: function (ev) {
                    // Only react to single-finger touches/mouse click events directly on the target
                    if (ev.eventPhase == Event.AT_TARGET && (!ev.targetTouches || ev.targetTouches.length == 1)) {
                        var t = ev.targetTouches ? ev.targetTouches[0] : ev,
                                canvas = this.el,
                                keepDrawing = true,
                                previousFrame = 0,
                                positions = [],
                                ctx = canvas.getContext('2d'),
                                x = this.offset.x,
                                y = this.offset.y, // XXX This caches it effectively, so scrolling while drawing doesn't work
                                startX = t.clientX - x,
                                startY = t.clientY - y,
                                move = function (ev) {
                                    if (ev.eventPhase == Event.AT_TARGET && (!ev.targetTouches || ev.targetTouches.length == 1)) {
                                        ev.preventDefault();
                                        var t = ev.targetTouches ? ev.targetTouches[0] : ev;
                                        positions.push([t.clientX - x, t.clientY - y, t.force || t.webkitForce || 0.1]);
                                    }
                                }, draw = function (frame) {
                            if (frame - previousFrame >= 40) { // Don't try to draw too often
                                var p = positions;
                                positions = [];
                                var i, l = p.length;
                                if (!this.boundingBox && l > 0)
                                    this.boundingBox = {
                                        left: startX,
                                        top: startY,
                                        right: startX,
                                        bottom: startY
                                    };

                                var bb = this.boundingBox; // For perf, "just in case"

                                for (i = 0; i < l; i++) {
                                    // Force isn't available (on Android)
                                    // ctx.lineWidth = p[2] * 10;
                                    ctx.lineTo(p[i][0], p[i][1]);

                                    bb.left = Math.min(bb.left, p[i][0]);
                                    bb.right = Math.max(bb.right, p[i][0]);
                                    bb.top = Math.min(bb.top, p[i][1]);
                                    bb.bottom = Math.max(bb.bottom, p[i][1]);
                                }
                                ctx.stroke();
                                previousFrame = frame;
                            }
                            if (keepDrawing)
                                animateFrame(draw);
                        }.bind(this), end = function (ev) {
                            if (ev.eventPhase == Event.AT_TARGET) {
                                canvas.removeEventListener('mousemove', move);
                                canvas.removeEventListener('touchmove', move);
                                canvas.removeEventListener('mouseup', end);
                                canvas.removeEventListener('touchend', end);
                                keepDrawing = false;
                            }
                        }, animateFrame = window.requestAnimationFrame ||
                                window.webkitRequestAnimationFrame ||
                                window.mozRequestAnimationFrame ||
                                function (callback) {
                                    return window.setTimeout(callback, 1000 / 50);
                                };

                        ev.preventDefault();

                        ctx.beginPath();
                        ctx.moveTo(startX, startY);
                        // TODO: Make this color configurable
                        ctx.strokeStyle = 'black';

                        animateFrame(draw);
                        if (ev.targetTouches) {
                            canvas.addEventListener('touchmove', move, false);
                            canvas.addEventListener('touchend', end, false);
                            // This prevents the fake mouse event from being dispatched as well
                            canvas.removeEventListener('mousedown', this.mouseDownEvent);
                        } else {
                            canvas.addEventListener('mousemove', move, false);
                            canvas.addEventListener('mouseup', end, false);
                        }
                    }
                },
                clear: function (canvas) {
                    var ctx = canvas.getContext('2d');
                    ctx.beginPath();
                    ctx.fillStyle = 'white';
                    ctx.fillRect(0, 0, canvas.width, canvas.height);
                    if (this.boundingBox)
                        delete this.boundingBox;
                },
            };
            // Export in an AMD-compliant way, without requiring an AMD loader
            if (typeof module === 'object' && module && typeof module.exports === 'object') {
                module.exports = signature;
            } else {
                window.SignatureView = signature;
                if (typeof define === 'function' && define.amd) {
                    define('cordova.signature-view', [], function () {
                        return signature;
                    });
                }
            }
        })();