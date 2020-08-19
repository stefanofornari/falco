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

import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Test;
import ste.falco.SoundMotionDetector;
import ste.falco.ui.FalcoCLI.TrafficControl;
import ste.falco.ui.FalcoCLI.TrafficControlMBean;
import ste.xtest.reflect.PrivateAccess;

/**
 * NOTE: inheriting from BugFreeCLIBase to take advantage of PIR clean up
 */
public class BugFreeTraffifControlMBean extends BugFreeCLIBase {

    @Test
    public void move_moves() throws Exception {
        FalcoCLIStub stub = new FalcoCLIStub();
        TrafficControlMBean bean = new TrafficControl(stub);

        bean.move();
        then(stub.EVENTS).containsExactly("moved"); stub.EVENTS.clear();

        bean.play();
        then(stub.EVENTS).containsExactly("played"); stub.EVENTS.clear();

        bean.reinit();
        then(stub.EVENTS).containsExactly("stopped"); stub.EVENTS.clear();
    }

    @Test
    public void play_plays() throws Exception {
        FalcoCLIStub stub = new FalcoCLIStub();
        TrafficControlMBean bean = new TrafficControl(stub);

        bean.play();
        then(stub.EVENTS).containsExactly("played"); stub.EVENTS.clear();
    }

    @Test
    public void reinit_stops() throws Exception {
        FalcoCLIStub stub = new FalcoCLIStub();
        TrafficControlMBean bean = new TrafficControl(stub);

        bean.reinit();
        then(stub.EVENTS).containsExactly("stopped"); stub.EVENTS.clear();
    }

    @Test
    public void setVolume_sets_volume() throws Exception {
        FalcoCLIStub stub = new FalcoCLIStub();
        TrafficControlMBean bean = new TrafficControl(stub);

        bean.setVolume(0);
        then(stub.EVENTS).containsExactly("set_volume 0.0"); stub.EVENTS.clear();

        bean.setVolume(1.0);
        then(stub.EVENTS).containsExactly("set_volume 1.0"); stub.EVENTS.clear();
    }

    // --------------------------------------------------------- private methods

    // ------------------------------------------------- TrafficControlMBeanStub

    private class FalcoCLIStub extends FalcoCLI {

        public final List<String> EVENTS;

        private FalcoCLIStub() throws Exception {
            super();
            this.EVENTS = new ArrayList();
            startup();
        }

        @Override
        public void startup() throws Exception {
            PrivateAccess.setInstanceValue(this, "moctor", new SoundMotionDetector("/sounds/test1.wav") {
                @Override
                public void moved() {
                    EVENTS.add("moved");
                }
                @Override
                public void setVolume(double volume) {
                    EVENTS.add("set_volume " + volume);
                }
            });
        }

        @Override
        public void play() {
            EVENTS.add("played");
        }

        @Override
        public void shutdown() {
            EVENTS.add("stopped");
        }
    }

}
