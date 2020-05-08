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

import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Test;
import ste.falco.ui.FalcoCLI.TrafficControl;
import ste.xtest.concurrent.Condition;
import ste.xtest.concurrent.WaitFor;
import ste.xtest.logging.ListLogHandler;

/**
 *
 */
public class BugFreeFalcoJMX extends BugFreeCLIBase {

    @Test
    public void register_falco_mbean() throws Exception {
        runMainClass();

        MBeanServer jmx = waitForTrafficControl();

        ObjectInstance obj = jmx.getObjectInstance(new ObjectName(TRAFFIC_CONTROL_NAME));
        then(obj.getClassName()).isEqualTo(TrafficControl.class.getName());
    }

    @Test
    public void simulate_a_move() throws Exception {
        Logger LOG = Logger.getLogger("ste.falco");
        ListLogHandler h = new ListLogHandler();
        LOG.addHandler(h);

        runMainClass();

        MBeanServer jmx = waitForTrafficControl();
        jmx.invoke(
            new ObjectName(TRAFFIC_CONTROL_NAME),
            "move", null, null
        );

        new WaitFor(1000, new Condition() {
            @Override
            public boolean check() {
                return (h.getMessages().size() >= 2);
            }

        });
        then(h.getMessages()).containsSequence("falco started", "motion detected");
    }

    @Test
    public void play() throws Exception {
        Logger LOG = Logger.getLogger("ste.falco");
        ListLogHandler h = new ListLogHandler();
        LOG.addHandler(h);
        LOG.setLevel(Level.FINEST);

        runMainClass();

        MBeanServer jmx = waitForTrafficControl();
        jmx.invoke(
            new ObjectName(TRAFFIC_CONTROL_NAME),
            "play", null, null
        );

        new WaitFor(1000, new Condition() {
            @Override
            public boolean check() {
                return (h.getMessages(Level.FINEST).size() > 1);
            }

        });
        then(h.getMessages(Level.FINEST)).contains("playing /sounds/red-tailed-hawk-sound.wav");
    }

    // --------------------------------------------------------- private methods

    private void runMainClass() {
        Executors.newCachedThreadPool().submit(new Runnable() {
            @Override
            public void run() {
                try { FalcoCLI.main(); } catch (Exception x) {};
            }
        });
    }

    private MBeanServer waitForTrafficControl() {
        new WaitFor(500, new Condition() {
            @Override
            public boolean check() {
                try {
                    //
                    // If not available, an exception is thrown
                    //
                    JMX.getObjectInstance(new ObjectName(TRAFFIC_CONTROL_NAME));
                    return true;
                } catch (Exception x) {
                    return false;
                }
            }
        });

        return JMX;
    }

}
