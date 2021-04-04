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

import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Test;
import ste.xtest.concurrent.Condition;
import ste.xtest.concurrent.WaitFor;
import ste.xtest.logging.ListLogHandler;

/**
 * TODO: cli.moctor.move() ddoes not make sense, we need to simulate a move via
 * a provider
 */
public class BugFreeFalcoCLIMain extends BugFreeCLIBase {

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


        new WaitFor(5000, new Condition() {
            @Override
            public boolean check() {
                return (h.getRecords().size() > 0);
            }
        });


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
}
