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

import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.junit.Test;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 *
 */
public class BugFreeLauncher {
    
    @Rule
    public TemporaryFolder TMPDIR = new TemporaryFolder();
    
    @Test
    public void launcher_from_home() throws Exception {
        Process p = launch("src/main", "bin/falco");
        p.waitFor();  // the process will end because of missing pi4j libraries,
                      // therefore it exits immediately
                      
        then(new String(p.getInputStream().readAllBytes())).contains("-- started");
    }
    
    @Test
    public void launcher_from_not_home() throws Exception {
        Process p = launch(TMPDIR.getRoot().getPath(), new File("src/main/bin/falco").getAbsolutePath());
        p.waitFor();  // the process will end because of missing pi4j libraries,
                      // therefore it exits immediately
                      
        then(new String(p.getInputStream().readAllBytes())).contains("-- started");
    }
    
    @Test
    public void launcher_with_help() throws Exception {
        Process p = launch("src/main", "bin/falco",  "--help");
        p.waitFor();  // the process will end because of --help
                      
        then(new String(p.getInputStream().readAllBytes()))
            .contains("usage:")
            .doesNotContain("-- started");
    }
    
    // --------------------------------------------------------- private methods
    
    private Process launch(String dir, String... command) throws IOException {
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(new File(dir))
          .command(command);
         Map<String, String> env = pb.environment();
         env.put("CLASSPATH", System.getProperty("java.class.path"));
         
         return pb.start();
    }
    
}
