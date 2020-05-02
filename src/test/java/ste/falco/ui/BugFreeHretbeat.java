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

import java.util.logging.Level;
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

/**
 *
 * @author ste
 */
public class BugFreeHretbeat {

    @Test
    public void plays_heartbeat_with_log() throws Exception {
        BugFreeSoundMotionDetector.ClipEventsRecorder rec = new BugFreeSoundMotionDetector.ClipEventsRecorder();
        Heartbeat hb = new Heartbeat(100);

        //
        // Logging set up
        //
        Logger LOG = Logger.getLogger("ste.falco");
        ListLogHandler h = new ListLogHandler();
        LOG.addHandler(h);
        LOG.setLevel(Level.FINEST);
        // ---

        Clip clip = (Clip)PrivateAccess.getInstanceValue(hb, "clip");
        clip.addLineListener(rec);

        hb.run();

        Condition c = new Condition() {
            @Override
            public boolean check() {
                return (rec.events.size() == 2);
            }

        };

        new WaitFor(2500, c);
        then(rec.events).containsExactly("Start", "Stop");
        then(h.getMessages(Level.FINEST)).containsOnly("heartbeat");
    }

}
