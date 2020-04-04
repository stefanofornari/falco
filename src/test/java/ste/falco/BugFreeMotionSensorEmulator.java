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
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * 
 */

public class BugFreeMotionSensorEmulator extends BugFreePIRBase {
    
    @Before
    public void before() {
        pin = GpioFactory.getInstance().provisionDigitalInputPin(RaspiPin.GPIO_04, "Motion sensor", PinPullResistance.PULL_DOWN);
    }
    
    @After
    public void after() {
        GpioController gpio = GpioFactory.getInstance();
        gpio.shutdown();
        gpio.unprovisionPin(pin);
    }
    
    @Test
    public void constructor_sets_default_values() {
        PIREmulator pir = new PIREmulator();
        then(pir.getName()).isEqualTo("RaspberryPi GPIO Provider");
        then(pir.isHigh()).isFalse();
        then(pir.isLow()).isTrue();
    }
    
    @Test
    public void up() {
        PIR.up();
        
        then(pin.getState()).isEqualTo(PinState.HIGH);
    }
    
    @Test
    public void down() {
        PIR.down();
        
        then(pin.getState()).isEqualTo(PinState.LOW);
    }
    
    @Test
    public void up_triggers_the_listener() {
        PIREmulator pir = new PIREmulator();
    }
    
    @Test   
    public void listen_to_changes() throws Exception {
        final List<String> EVENTS = new ArrayList<>();
        
        pin.addListener(new GpioPinListenerDigital() {
            @Override
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                EVENTS.add(String.valueOf(event.getState()));
            }
        });
        
        PIR.up(); Thread.sleep(50); then(EVENTS).containsExactly("HIGH");
        
        PIR.down(); Thread.sleep(50); then(EVENTS).containsExactly("HIGH", "LOW");
        
    }
    
}
