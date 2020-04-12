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

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.RaspiPin;
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
                then(x).hasMessage("'" + RES + "' does not exist in classpath");
            }
        }
    }
    
    @Test
    public void is_a_MotionDetector() {
        SoundMotionDetector smd = new SoundMotionDetector("/sounds/test1.wav");
        then(smd).isInstanceOf(MotionDetector.class);
    }
    
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
                    return (rec.events.size() == 2);
                }

            };

            new WaitFor(2500, c);
            then(rec.events).containsExactly("Start", "Stop");

            //
            // we want to be able to play again without reinitializing everything
            //
            rec.events.clear();
            smd.moved();

            new WaitFor(2500, c);
            then(rec.events).containsExactly("Start", "Stop");
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
    public void startup_calls_super() throws Exception {
        GpioController gpio = GpioFactory.getInstance();
        try (SoundMotionDetector smd = new SoundMotionDetector("/sounds/test1.wav")) {
            smd.startup();

            then(gpio.getProvisionedPin(RaspiPin.GPIO_04)).isNotNull();
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
