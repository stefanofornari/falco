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

import java.util.logging.Logger;
import ste.falco.SoundMotionDetector;


/**
 *
 */
public class FalcoCLI extends SoundMotionDetector {
    
    private static Logger LOG = Logger.getLogger("ste.falco");
    
    public static void main(String... args) {
        System.out.println("Welcome to Falco");
        
        if (args.length > 0) {
            System.out.println("usage:");
            System.out.println("ste.falco.FalcoCLI [--help]");
            return;
        }
        
        System.out.println("-- started");
        try (FalcoCLI moctor = new FalcoCLI()) {
            LOG.info("falco started");
            
            while (true) {
                Thread.sleep(200);
            }
        } catch (Exception x) {
            x.printStackTrace();
        }
        
        System.out.println("CHECK!");
    }
    
    public FalcoCLI() throws Exception {
        super("/sounds/red-tailed-hawk-sound.wav");
        startup();
    }
    
    @Override
    public void moved() {
        super.moved();
        LOG.info("motion detected");
    }
}
