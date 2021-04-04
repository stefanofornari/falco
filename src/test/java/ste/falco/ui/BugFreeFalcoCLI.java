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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import javax.sound.sampled.Clip;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Test;
import ste.falco.BugFreeSoundMotionDetector;
import ste.falco.ui.FalcoCLI.Heartbeat;
import ste.xtest.concurrent.Condition;
import ste.xtest.concurrent.WaitFor;
import ste.xtest.logging.ListLogHandler;
import ste.xtest.reflect.PrivateAccess;
import ste.xtest.time.FixedClock;

/**
 * TODO: cli.moctor.move() ddoes not make sense, we need to simulate a move via
 * a provider
 */
public class BugFreeFalcoCLI extends BugFreeCLIBase {

    private static final LocalTime MORNING = LocalTime.of(8, 0);
    private static final LocalTime NIGHT = LocalTime.of(20, 0);

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
                             .contains("Usage:")
                             .contains("ste.falco.FalcoCLI");
    }

    @Test
    public void unkown_parameter() throws Exception {
        Future f = Executors.newCachedThreadPool().submit(new Runnable() {
            @Override
            public void run() {
                try { FalcoCLI.main("--invalidoption"); } catch (Exception x) {};
            }
        });
        Thread.sleep(500); // let's give the service the time to start and stay up

        then(f.isDone()).isTrue();
        then(STDOUT.getLog())
            .contains("Unknown option: '--invalidoption'")
            .contains("Usage:")
            .doesNotContain("-- started");
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
                             .doesNotContain("Usage:");
    }

    @Test
    public void log_motion_event() throws Exception {
        Logger LOG = Logger.getLogger("ste.falco");
        ListLogHandler h = new ListLogHandler();
        LOG.addHandler(h);

        Executors.newCachedThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try { FalcoCLI.main(); } catch (Exception x) {};
            }
        });

        Thread.sleep(500); // let's give the service the time to start and stay up

        //
        // caveat: if the test runs outside the daylight window, we have one more log message
        //
        final LocalTime now = LocalTime.now();

        PIR.up(); Thread.sleep(50); PIR.down();
        if (now.isAfter(MORNING) && now.isBefore(NIGHT)) {
            then(h.getMessages()).containsExactly("falco started", "motion detected");
        } else {
            then(h.getMessages()).containsExactly("falco started", "motion detected", "too early or not in day light - I am muted");
        }

        PIR.up(); Thread.sleep(50); PIR.down();
        if (now.isAfter(MORNING) && now.isBefore(NIGHT)) {
            then(h.getMessages()).containsExactly("falco started", "motion detected", "motion detected", "too early or not in day light - I am muted");
        } else {
            then(h.getMessages()).containsExactly("falco started", "motion detected", "too early or not in day light - I am muted", "motion detected", "too early or not in day light - I am muted");
        }

        LOG.removeHandler(h);
    }

    @Test
    public void heartbeat() throws Exception {
        try (FalcoCLI cli = new FalcoCLI()) {
            cli.startup();
            Heartbeat hb = (Heartbeat)PrivateAccess.getInstanceValue(cli, "heartbeatTask");
            then(hb.period).isEqualTo(5*60*1000); // 5 minutes in milliseconds
        }

        CounterTask counter = new CounterTask(25);
        try (FalcoCLI cli = new FalcoCLI()) {
            PrivateAccess.setInstanceValue(cli, "heartbeatTask", counter);
            cli.startup();

            Thread.sleep(100); then(counter.value).isGreaterThan(0).isLessThan(6);
        }

        counter = new CounterTask(50);
        try (FalcoCLI cli = new FalcoCLI()) {
            PrivateAccess.setInstanceValue(cli, "heartbeatTask", counter);
            cli.startup();

            Thread.sleep(100); then(counter.value).isGreaterThan(0).isLessThan(4);
        }
    }

    @Test
    public void do_not_play_too_frequently() throws Exception {
        BugFreeSoundMotionDetector.ClipEventsRecorder rec = new BugFreeSoundMotionDetector.ClipEventsRecorder();

        FixedClock clock = new FixedClock(ZoneId.systemDefault());
        clock.millis = Instant.parse("2007-12-03T13:15:30.00Z").toEpochMilli();  // just to make sure we are in daylight
        try (FalcoCLI cli = new FalcoCLI()) {
            cli.startup();

            Clip clip = (Clip)PrivateAccess.getInstanceValue(cli.moctor, "clip");
            clip.addLineListener(rec);
            PrivateAccess.setInstanceValue(cli.moctor, "CLOCK", clock);
            PrivateAccess.setInstanceValue(cli.moctor, "lastMoved", LocalDateTime.now(clock).minusHours(24));

            cli.moctor.moved();  // first time: play

            new WaitFor(5000, new Condition() {
                @Override
                public boolean check() {
                    return (rec.events.size() >= 2);
                }
            });

            clock.millis += 5*60*1000;  // 5 minutes later
            cli.moctor.moved();  Thread.sleep(50); // second time in a row: don't play
            then(rec.events).hasSize(4);

            clock.millis += 6*60*1000;  // 11 minutes later
            cli.moctor.moved();  // third time: play

            new WaitFor(5000, new Condition() {
                @Override
                public boolean check() {
                    return (rec.events.size() >= 4);
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
             cli.startup();
             PrivateAccess.setInstanceValue(cli.moctor, "lastMoved", LocalDateTime.now().plusMinutes(10));

             cli.moctor.moved();

             then(h.getMessages()).containsExactly("motion detected", "too early or not in day light - I am muted");
         }
    }

    @Test
    public void daylight() throws Exception {
        BugFreeSoundMotionDetector.ClipEventsRecorder rec = new BugFreeSoundMotionDetector.ClipEventsRecorder();

        try (FalcoCLI cli = new FalcoCLI()) {
            cli.startup();

            Clip clip = (Clip)PrivateAccess.getInstanceValue(cli.moctor, "clip");
            clip.addLineListener(rec);

            final ZonedDateTime TODAY = ZonedDateTime.now();

            FixedClock clock = new FixedClock(TODAY.getZone());
            PrivateAccess.setInstanceValue(cli.moctor, "CLOCK", clock);

            //
            // play between 8:00AM and 8:00PM
            //

            for (int h=8; h<20; ++h) {
                rec.events.clear();

                LocalTime time = LocalTime.of(h, h); // just not to use always H:00
                LocalDateTime datetime = LocalDateTime.of(TODAY.toLocalDate(), time);
                clock.millis = datetime.toEpochSecond(TODAY.getOffset()) * 1000;

                cli.moctor.moved();

                new WaitFor(5000, new Condition() {
                    @Override
                    public boolean check() {
                        return (rec.events.size() >= 2);
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

                cli.moctor.moved(); Thread.sleep(250);

                then(rec.events).isEmpty();
            }

            for (int h=0; h<8; ++h) {
                LocalTime time = LocalTime.of(h, h); // just not to use always H:00
                LocalDateTime datetime = LocalDateTime.of(TODAY.toLocalDate(), time);
                clock.millis = datetime.toEpochSecond(TODAY.getOffset()) * 1000;

                cli.moctor.moved(); Thread.sleep(50);

                then(rec.events).isEmpty();
            }
        }
    }

    /**
     * Volume is a value between 0.0 and 2.0 with the following meaning:
     *
     * 0.5 - no sound
     * 0.5 - half the volume of the clip
     * 1.0 - the natural volume of the clip
     * 2.0 - double the volume of the clip
     *
     * @throws Exception
     */
    @Test
    public void get_and_setset_and_get_volume_ok() throws Exception {
        try (FalcoCLI cli = new FalcoCLI()) {
            cli.startup();

            cli.moctor.setVolume(0);
            then(cli.moctor.getVolume()).isZero();

            cli.moctor.setVolume(0.75d);
            then(cli.moctor.getVolume()).isEqualTo(0.75);

            cli.moctor.setVolume(2.0d);
            then(cli.moctor.getVolume()).isEqualTo(2.0d);
        }
    }

    @Test
    public void set_volume_out_of_range() throws Exception {
        try (FalcoCLI cli = new FalcoCLI()) {
            cli.startup(); cli.moctor.setVolume(-0.4);
        } catch (IllegalArgumentException x) {
            then(x).hasMessage("invalid volume -0.4 - it must in range (0.0, 2.0)");
        }

        try (FalcoCLI cli = new FalcoCLI()) {
            cli.startup(); cli.moctor.setVolume(2.1);
        } catch (IllegalArgumentException x) {
            then(x).hasMessage("invalid volume 2.1 - it must in range (0.0, 2.0)");
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
            ++value;
        }
    }

    // --------------------------------------------------------- private methods
}
