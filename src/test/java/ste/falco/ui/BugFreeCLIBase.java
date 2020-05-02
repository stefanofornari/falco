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
package ste.falco.ui;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPin;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.SystemOutRule;
import ste.falco.*;

/**
 *
 *
 */

abstract public class BugFreeCLIBase extends BugFreePIRBase {

    @Rule
    public final SystemOutRule STDOUT = new SystemOutRule().enableLog();

    protected final MBeanServer JMX = ManagementFactory.getPlatformMBeanServer();
    protected final String TRAFFIC_CONTROL_NAME = "ste.falco.jmx:name=TrafficControl";

    @Before
    public void before() {
        GpioController gpio = GpioFactory.getInstance();
        Collection<GpioPin> pins = gpio.getProvisionedPins();
        for (GpioPin pin: pins.toArray(new GpioPin[pins.size()])) {
            System.out.println("unprovisioning " + pin);
            gpio.unprovisionPin(pin);
        };

        try {
            JMX.unregisterMBean(new ObjectName(TRAFFIC_CONTROL_NAME));
        } catch (Exception x) {
        }
    }
}
