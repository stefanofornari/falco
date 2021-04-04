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

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.UnsupportedAudioFileException;
import static org.assertj.core.api.AssertionsForClassTypes.fail;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Test;
import static ste.xtest.Constants.BLANKS;
import ste.xtest.concurrent.Condition;
import ste.xtest.concurrent.WaitFor;
import ste.xtest.reflect.PrivateAccess;

/**
 *
 *
 */

public class BugFreeSoundMotionDetector extends BugFreePIRBase {

    @Test
    public void resource_in_constructor() {
        SoundMotionDetector smd = new SoundMotionDetector("/sounds/test1.wav");
        then(smd.sound).isEqualTo("/sounds/test1.wav");

        for (String BLANK: BLANKS) {
            try {
                smd = new SoundMotionDetector(BLANK);
                fail("missing required argument");
            } catch (IllegalArgumentException x) {
                then(x).hasMessage("sound can not be blank or null");
            }
        }

        for(String RES: new String[] {"does/not/exist.wav", "noteither.wav"}) {
            try {
                smd = new SoundMotionDetector(RES);
                fail("missing valid resource check");
            } catch (IllegalArgumentException x) {
                then(x).hasMessage("'" + RES + "' not found in classpath");
            }
        }
    }

    /**
     * Here we want to play the sound when moed is invoked. Ideally, we would
     * load the sound only once, and then we would play it forever rewinding
     * the stream with setPosition(0). However it seems there is a problem
     * here as reported in https://github.com/stefanofornari/falco/issues/6:
     *
     * After some hours the sound stops playing. The detection and playing logic
     * is ok, the problem seems in java sound:
     * - playing with JMX does not play either
     * - playing with PlaySound plays
     *
     * For this reason we then open the clip at startup and then we close and
     * reopen it at every play.
     *
     * @throws Exception
     */
    @Test
    public void moved_plays_the_sound() throws Exception {
        ClipEventsRecorder rec = new ClipEventsRecorder();
        try (SoundMotionDetector smd = new SoundMotionDetector("/sounds/test1.wav")) {

            smd.startup();

            Clip clip = (Clip)PrivateAccess.getInstanceValue(smd, "clip");
            clip.addLineListener(rec);

            smd.moved();

            Condition c = new Condition() {
                @Override
                public boolean check() {
                    return (rec.events.size() >= 2);
                }

            };

            new WaitFor(2500, c);
            then(rec.events).containsExactly("Start", "Stop", "Close", "Open");
        }
    }

    @Test
    public void handle_unsupported_formats() throws Exception {
        try (SoundMotionDetector smd = new SoundMotionDetector("/sounds/test2.invalid")) {
            smd.startup();
            fail("missing check for unsupported formats");
        } catch (UnsupportedAudioFileException x) {
            then(x).hasMessage("Stream of unsupported format");
        }
    }

    @Test
    public void handle_LineUnavailableException_in_getting_clip() throws Exception {
        try (SoundMotionDetector smd = new SoundMotionDetector("/sounds/test1.wav")) {
            PrivateAccess.setInstanceValue(
                smd, "mixer",
                getMixerWithErrorInGetLine((Mixer)PrivateAccess.getInstanceValue(smd, "mixer"))
            );
            smd.startup();
            fail("no exception");
        } catch (LineUnavailableException x) {
            then(x).hasMessage("line unavailable in getLine");
        }
    }

    @Test
    public void handle_LineUnavailableException_in_opening_clip() throws Exception {
        try (SoundMotionDetector smd = new SoundMotionDetector("/sounds/test1.wav")) {
            PrivateAccess.setInstanceValue(
                smd, "mixer",
                getMixerWithErrorInOpen(
                    (Mixer)PrivateAccess.getInstanceValue(smd, "mixer"),
                    new LineUnavailableException("line unavailable in open")
                )
            );
            smd.startup();
            fail("no exception");
        } catch (LineUnavailableException x) {
            then(x).hasMessage("line unavailable in open");
        }
    }


    @Test
    public void handle_IOException_in_opening_clip() throws Exception {
        try (SoundMotionDetector smd = new SoundMotionDetector("/sounds/test1.wav")) {
            PrivateAccess.setInstanceValue(
                smd, "mixer",
                getMixerWithErrorInOpen(
                    (Mixer)PrivateAccess.getInstanceValue(smd, "mixer"),
                    new IOException("IO error in open")
                )
            );
            smd.startup();
            fail("no exception");
        } catch (IOException x) {
            then(x).hasMessage("IO error in open");
        }
    }

    @Test
    public void error_if_moved_is_called_before_startup() {
        try (SoundMotionDetector smd = new SoundMotionDetector("/sounds/test1.wav")) {
            smd.moved();
            fail("moved when not ready");
        } catch (IllegalStateException x) {
            then(x).hasMessage("moved() called before the instance is started up; make sure to call startup()");
        }
    }

    @Test
    public void shotdown_closes_and_nulls_the_clip() throws Exception {
        SoundMotionDetector smd = new SoundMotionDetector("/sounds/test1.wav");

        smd.startup();
        Clip clip = smd.clip;
        smd.shutdown();

        then(clip.isOpen()).isFalse();
        then(smd.clip).isNull();
    }

    @Test
    public void isLive_returns_true_when_live_false_otherwise() throws Exception  {
        SoundMotionDetector smd = new SoundMotionDetector("/sounds/test1.wav");

        then(smd.isLive()).isFalse();
        smd.startup();
        then(smd.isLive()).isTrue();
        smd.shutdown();
        then(smd.isLive()).isFalse();
    }

    // --------------------------------------------------------- private methods

    private Mixer getMixerWithErrorInGetLine(Mixer mixer) throws Exception {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object o, Method m, Object[] args) throws Throwable {
                    if ("getLine".equals(m.getName())) {
                        throw new LineUnavailableException("line unavailable in getLine");
                    }
                    return m.invoke(mixer, args);
                }
            };

            return (Mixer) Proxy.newProxyInstance(
                               Mixer.class.getClassLoader(),
                               new Class<?>[] { Mixer.class },
                               handler
        );
    }

    private Mixer getMixerWithErrorInOpen(Mixer mixer, Throwable t) throws Exception {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object o, Method m, Object[] args) throws Throwable {
                if ("getLine".equals(m.getName())) {
                    return getClipWithErrorInOpen((Clip)m.invoke(mixer, args), t);
                }
                return m.invoke(mixer, args);
            }
        };

        return (Mixer) Proxy.newProxyInstance(
                               Mixer.class.getClassLoader(),
                               new Class<?>[] { Mixer.class },
                               handler
        );
    }

    private Clip getClipWithErrorInOpen(Clip clip, Throwable t) throws Exception {

        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object o, Method m, Object[] args) throws Throwable {
                if ("open".equals(m.getName())) {
                    throw t;
                }
                return m.invoke(clip, args);
            }
        };

        return (Clip) Proxy.newProxyInstance(Clip.class.getClassLoader(),
                                          new Class<?>[] { Clip.class },
                                          handler);
    }

    // ------------------------------------------------------ ClipEventsRecorder

    //
    // TODO: move to xtest
    //
    public static class ClipEventsRecorder implements LineListener {

        public final List<String> events = new ArrayList<>();

        @Override
        public void update(LineEvent e) {
           events.add(String.valueOf(e.getType()));
        }
    }

}
