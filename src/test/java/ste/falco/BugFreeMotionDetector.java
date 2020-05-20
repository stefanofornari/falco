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

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.exception.GpioPinExistsException;
import static org.assertj.core.api.AssertionsForClassTypes.fail;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;

/**
 *
 *
 */

public class BugFreeMotionDetector extends BugFreePIRBase {

    @Rule
    public final SystemOutRule STDOUT = new SystemOutRule().enableLog();

    @Test
    public void startup_provisions_GPIO_04() throws Exception {
        GpioController gpio = GpioFactory.getInstance();
        MotionDetector smd = new InnerMotionDetector();
        then(gpio.getProvisionedPins()).isEmpty();
        smd.startup();

        //
        // only PIN_04 should be providions
        //
        then(gpio.getProvisionedPins()).hasSize(1);
        then(gpio.getProvisionedPin(RaspiPin.GPIO_04)).isNotNull();

        smd.shutdown();
    }

    @Test
    public void shutdown_unprovisions_the_pin() throws Exception {
        GpioController gpio = GpioFactory.getInstance();
        MotionDetector smd = new InnerMotionDetector();

        //
        // if not yet provisioned, silently execute
        //
        smd.shutdown();
        then(gpio.getProvisionedPins()).isEmpty();

        //
        // now when provisione
        //
        smd.startup(); smd.shutdown();
        then(gpio.getProvisionedPins()).isEmpty();
    }

    @Test
    public void call_moved_when_motion_detected() throws Exception {
        try (InnerMotionDetector moctor = new InnerMotionDetector()) {
            moctor.startup();


            //
            // NOTE: pint state changes and event handling is concurrent, we need to
            // give it time to propagate
            //
            PIR.up(); Thread.sleep(50); then(moctor.count).isEqualTo(1);

            PIR.down();

            PIR.up(); Thread.sleep(50); then(moctor.count).isEqualTo(2);
        }
    }

    @Test
    public void is_auto_closeable() throws Exception {
        GpioController gpio = GpioFactory.getInstance();

        //
        // no exception
        //
        try (MotionDetector moctor = new InnerMotionDetector()) {
            moctor.startup();
        } finally {
            then(gpio.getProvisionedPins()).isEmpty();
        }

        //
        // with exception
        //
        try (MotionDetector moctor = new InnerMotionDetector()) {
            moctor.startup(); moctor.startup();
        } catch (GpioPinExistsException x) {
            then(gpio.getProvisionedPins()).isEmpty();
        }
    }

    @Test
    public void finalize_shutdowns_the_detector_with_message() throws Exception {
        GpioController gpio = GpioFactory.getInstance();

        MotionDetector moctor = new InnerMotionDetector();
        moctor.startup();

        moctor.finalize();

        then(gpio.getProvisionedPins()).isEmpty();
        then(STDOUT.getLog()).contains("MotionDetector closed in finalize(); use shutdown() explicitely instead!")
                             .contains("java.lang.IllegalStateException\n");

    }

    @Test
    public void error_if_moved_is_called_before_startup() {
        try (MotionDetector smd = new MotionDetector() {}) {
            smd.moved();
            fail("moved when not ready");
        } catch (IllegalStateException x) {
            then(x).hasMessage("moved() called before the instance is started up; make sure to call startup()");
        }
    }

    @Test
    public void moved_ok() throws Exception {
        try (MotionDetector smd = new MotionDetector() {}) {
            smd.startup();
            smd.moved();
        }
    }

    @Test
    public void shutdown_resets_the_instance() throws Exception {
        //
        // NOTE: after the instance is reset, it must be started up again before
        // using it.
        //
        try (MotionDetector smd = new MotionDetector() {}) {
            smd.startup(); smd.moved(); smd.shutdown();
            smd.moved();
        } catch (IllegalStateException x) {
            then(x).hasMessage("moved() called before the instance is started up; make sure to call startup()");
        }
    }

    @Test
    public void isLive_when_started_up_not_otherwise() throws Exception {
        try (MotionDetector smd = new MotionDetector() {}) {
            then(smd.isLive()).isFalse();
            smd.startup(); then(smd.isLive()).isTrue();
            smd.shutdown(); then(smd.isLive()).isFalse();
        }
    }

    // ----------------------------------------------------- InnerMotionDetector

    private class InnerMotionDetector extends MotionDetector {
        public int count = 0;

        @Override
        public void moved() {
            count += 1;
        }
    }
}
