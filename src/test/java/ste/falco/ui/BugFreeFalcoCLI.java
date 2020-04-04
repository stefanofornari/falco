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

import ste.falco.BugFreePIRBase;
import ste.falco.ui.FalcoCLI;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPin;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import ste.xtest.logging.ListLogHandler;

/**
 *
 */
public class BugFreeFalcoCLI extends BugFreePIRBase {
    @Rule
    public final SystemOutRule STDOUT = new SystemOutRule().enableLog();
    
    @Rule
    public final TestRule watcherRule = new TestWatcher() {
        protected void starting(Description description) {
          String out = Thread.currentThread().getName() + ": " + description.getMethodName();
          System.out.printf(
              "\n%s\n%s\n", out, StringUtils.repeat("-", out.length())
          );
        };
    };

    @Before
    public void before() {
        GpioController gpio = GpioFactory.getInstance();
        Collection<GpioPin> pins = gpio.getProvisionedPins();
        for (GpioPin pin: pins.toArray(new GpioPin[pins.size()])) {
            System.out.println("unprovisioning " + pin);
            gpio.unprovisionPin(pin);
        };
    }
        
    @Test
    public void help() throws Exception {
        //
        // NOTE: if help is invoked, the process shall exit immediately
        //
        Future f = Executors.newCachedThreadPool().submit(new Runnable() {
            @Override
            public void run() {
                try { FalcoCLI.main("--help"); } catch (Exception x) {};
            }
        });
        
        Thread.sleep(500); // let's give the service the time to start and stay up
        
        then(f.isDone()).isTrue();
        then(STDOUT.getLog()).contains("Welcome to Falco")
                             .contains("usage:")
                             .contains("com.github.stefanofornari.falco.FalcoCLI");
    }
    
    @Test
    public void start_service() throws Exception {
        Future f = Executors.newCachedThreadPool().submit(new Runnable() {
            @Override
            public void run() {
                try { FalcoCLI.main(); } catch (Exception x) {};
            }
        });
        
        Thread.sleep(500); // let's give the service the time to start and stay up
        
        then(f.isDone()).isFalse();
        then(STDOUT.getLog()).contains("Welcome to Falco\n-- started")
                             .doesNotContain("usage:");
    }
    
    @Test
    public void log_motion_event() throws Exception {
        Logger LOG = Logger.getLogger("ste.falco");
        ListLogHandler h = new ListLogHandler();
        LOG.addHandler(h);
        
        Future f = Executors.newCachedThreadPool().submit(new Runnable() {
            @Override
            public void run() {
                try { FalcoCLI.main(); } catch (Exception x) {};
            }
        });
        
        Thread.sleep(500); // let's give the service the time to start and stay up
        
        PIR.up(); Thread.sleep(50); PIR.down();
        then(h.getMessages()).containsExactly("falco started", "motion detected");
        
        PIR.up(); Thread.sleep(50); PIR.down();
        then(h.getMessages()).containsExactly("falco started", "motion detected", "motion detected");
        
        LOG.removeHandler(h);
    }
}
