/*
 * Copyright (C) 2020 Stefano Fornari.
 * All Rights Reserved.  No use, copying or distribution of this
 * work may be made except in accordance with a valid license
 * agreement from Stefano Fornari.  This notice must be
 * included on all copies, modifications and derivatives of this
 * work.
 *
 * STEFANO FORNARI MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY
 * OF THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. STEFANO FORNARI SHALL NOT BE LIABLE FOR ANY
 * DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 */
package ste.falco;

import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 *
 */
public class MotionDetector
       extends SoundMotionDetector
       implements GpioPinListenerDigital {

    private GpioPinDigitalInput PIN = null;

    public MotionDetector(final String sound) {
        super(sound);
    }

    public void startup() throws Exception {
        super.startup();
        PIN = GpioFactory.getInstance()
                         .provisionDigitalInputPin(RaspiPin.GPIO_04, "Motion sensor", PinPullResistance.PULL_DOWN);
        PIN.addListener(this);
    }

    /**
     * For now we just unprovision the pin, we do not shutdown the GPIO
     * controller.
     */
    public void shutdown() {
        if (PIN != null) {
            GpioFactory.getInstance().unprovisionPin(PIN);
            PIN = null;
        }
    }

    public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
        // display pin state on console
        System.out.println(" --> GPIO PIN STATE CHANGE: " + event.getPin() + " = " + event.getState());
        if (event.getState().isHigh()) {
            moved();
        }
    }

    @Override
    protected void finalize() {
        System.out.println("MotionDetector closed in finalize(); use shutdown() explicitely instead!");
        System.out.println(ExceptionUtils.getStackTrace(new IllegalStateException()));

        shutdown();
    }

    /**
     * This is supposed to be overridden to provide the specific behaviour when
     * motion is detected. The only thing that the base implementation does it
     * to make sure startup() has been called before. Derived classes can use it
     * in their own implementations to achieve the same goal.
     *
     * @throws IllegalStateException if the instance has not been started up
     */
    //
    // TODO: to review with the new hierarchy
    //
    public void moved() {
        System.out.println("CHECK1");
        if (PIN == null) {
            throw new IllegalStateException("moved() called before the instance is started up; make sure to call startup()");
        }
        super.moved();
    }

    //
    // TODO: write bugfree code to call super...
    //
    @Override
    public boolean isLive() {
        return super.isLive() && (PIN != null);
    }

    // ----------------------------------------------------------- AutoCloseable

    @Override
    public void close() {
        super.close();
        shutdown();
    }

}
