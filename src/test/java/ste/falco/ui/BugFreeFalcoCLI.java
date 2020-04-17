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
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPin;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import javax.sound.sampled.Clip;
import org.apache.commons.lang3.StringUtils;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import ste.falco.BugFreeSoundMotionDetector;
import ste.falco.ui.FalcoCLI.Heartbeat;
import ste.xtest.concurrent.Condition;
import ste.xtest.concurrent.WaitFor;
import ste.xtest.logging.ListLogHandler;
import ste.xtest.reflect.PrivateAccess;
import ste.xtest.time.FixedClock;

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
                             .contains("ste.falco.FalcoCLI");
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
        then(h.getMessages()).containsExactly("falco started", "motion detected", "motion detected", "too early or not in day light - skip playing");
        
        LOG.removeHandler(h);
    }
    
    @Test
    public void heartbeat() throws Exception {
        
        try (FalcoCLI cli = new FalcoCLI()) {
            Heartbeat hb = (Heartbeat)PrivateAccess.getInstanceValue(cli, "heartbeatTask");
            then(hb.period).isEqualTo(5*60*1000); // 5 minutes in milliseconds
        }
        
        CounterTask counter = new CounterTask(25);
        try (FalcoCLI cli = new FalcoCLI(counter)) {
            Thread.sleep(100); then(counter.value).isGreaterThan(0).isLessThan(6);
        }
        
        counter = new CounterTask(50);
        try (FalcoCLI cli = new FalcoCLI(counter)) {
            Thread.sleep(100); then(counter.value).isGreaterThan(0).isLessThan(4);
        }
    }
    
    @Test
    public void do_not_play_too_frequently() throws Exception {
        BugFreeSoundMotionDetector.ClipEventsRecorder rec = new BugFreeSoundMotionDetector.ClipEventsRecorder();
        
        try (FalcoCLI cli = new FalcoCLI()) {
            Clip clip = (Clip)PrivateAccess.getInstanceValue(cli, "clip");
            clip.addLineListener(rec);
            
            cli.moved();  // first time: play
            
            new WaitFor(5000, new Condition() {
                @Override
                public boolean check() {
                    return (rec.events.size() == 2);
                }
            });
            
            cli.moved();  Thread.sleep(50); // second time in a row: don't play
            then(rec.events).hasSize(2);
            
            FixedClock clock = new FixedClock(ZoneId.systemDefault());
            clock.millis = System.currentTimeMillis() + 10*60*1000;  // 10 minutes in the future
            PrivateAccess.setInstanceValue(cli, "CLOCK", clock);
            
            cli.moved();  Thread.sleep(50); // third time: play
            
            new WaitFor(5000, new Condition() {
                @Override
                public boolean check() {
                    return (rec.events.size() == 4);
                }
            });

        };
    }
    
    @Test
    public void skip_if_too_early_log() throws Exception {
        Logger LOG = Logger.getLogger("ste.falco");
        ListLogHandler h = new ListLogHandler();
        LOG.addHandler(h);
        
         try (FalcoCLI cli = new FalcoCLI()) {
             PrivateAccess.setInstanceValue(cli, "lastMoved", LocalDateTime.now().plusMinutes(10));
            
             cli.moved();
            
             then(h.getMessages()).containsExactly("motion detected", "too early or not in day light - I am muted");
         }
    }
    
    @Test
    public void daylight() throws Exception {
        BugFreeSoundMotionDetector.ClipEventsRecorder rec = new BugFreeSoundMotionDetector.ClipEventsRecorder();
    
        try (FalcoCLI cli = new FalcoCLI()) {
            Clip clip = (Clip)PrivateAccess.getInstanceValue(cli, "clip");
            clip.addLineListener(rec);
            
            final ZonedDateTime TODAY = ZonedDateTime.now();
            
            FixedClock clock = new FixedClock(TODAY.getZone()); 
            PrivateAccess.setInstanceValue(cli, "CLOCK", clock);
            
            //
            // play between 8:00AM and 8:00PM
            //
            
            for (int h=8; h<20; ++h) {
                rec.events.clear();
                
                LocalTime time = LocalTime.of(h, h); // just not to use always H:00
                LocalDateTime datetime = LocalDateTime.of(TODAY.toLocalDate(), time);
                clock.millis = datetime.toEpochSecond(TODAY.getOffset()) * 1000;
                
                cli.moved(); 
                
                new WaitFor(5000, new Condition() {
                    @Override
                    public boolean check() {
                        return (rec.events.size() == 2);
                    }
                });
            }
            rec.events.clear();
            
            //
            // do not play between 8:00PM and 9:00PM 
            //
            for (int h=20; h<24; ++h) {
                LocalTime time = LocalTime.of(h, h); // just not to use always H:00
                LocalDateTime datetime = LocalDateTime.of(TODAY.toLocalDate(), time);
                clock.millis = datetime.toEpochSecond(TODAY.getOffset()) * 1000;
                
                cli.moved(); Thread.sleep(250);
                
                then(rec.events).isEmpty();
            }
            
            for (int h=0; h<8; ++h) {
                LocalTime time = LocalTime.of(h, h); // just not to use always H:00
                LocalDateTime datetime = LocalDateTime.of(TODAY.toLocalDate(), time);
                clock.millis = datetime.toEpochSecond(TODAY.getOffset()) * 1000;
                
                cli.moved(); Thread.sleep(50);
                
                then(rec.events).isEmpty();
            }
            
            
                
        }
    }
    
    
    
    // ------------------------------------------------------------- CounterTask
    
    class CounterTask extends Heartbeat {
        
        public int value = 0;
        
        public CounterTask(int period) throws Exception {
            super(period);
        }

        @Override
        public void run() {
            System.out.println(this.getClass().getName());
            ++value;
        }
    }
}
