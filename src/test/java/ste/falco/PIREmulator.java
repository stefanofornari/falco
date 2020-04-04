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

import com.pi4j.io.gpio.PinMode;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.SimulatedGpioProvider;

/**
 *
 */
public class PIREmulator extends SimulatedGpioProvider {
        
    public PIREmulator() {
        export(RaspiPin.GPIO_04, PinMode.DIGITAL_INPUT);
        setState(RaspiPin.GPIO_04, PinState.LOW);
    }
    
    @Override
    public String getName() {
        //
        // NOTE: this name is required by GPIO otherwise it throws an error...
        //
        return "RaspberryPi GPIO Provider";
    }
    
    public boolean isHigh() {
        return getState(RaspiPin.GPIO_04).isHigh();
    }
    
    public boolean isLow() {
        return getState(RaspiPin.GPIO_04).isLow();
    }
    
    public void up() {
        setState(RaspiPin.GPIO_04, PinState.HIGH);
    }
    
    public void down() {
        setState(RaspiPin.GPIO_04, PinState.LOW);
    }

}
