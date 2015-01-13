/**
 * Custom View for capturing the user's signature
 *
 * Code taken from repository of Gianluca Cacace:
 * https://github.com/gcacace/android-signaturepad
 *
 * This falls under Apache License 2.0
 */
package com.github.gcacace.signaturepad.utils;

public class ControlTimedPoints {

    public TimedPoint c1;
    public TimedPoint c2;

    public ControlTimedPoints(TimedPoint c1, TimedPoint c2) {
        this.c1 = c1;
        this.c2 = c2;
    }

}
