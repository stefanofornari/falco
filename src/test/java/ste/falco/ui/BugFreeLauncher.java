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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 *
 */
public class BugFreeLauncher {

    @Rule
    public TemporaryFolder TMPDIR = new TemporaryFolder();

    @Test
    public void launcher_from_home_with_nohup() throws Exception {
        String[] ret = launch();  // the process will end because of missing
                                   // pi4j libraries, therefore it exits
                                   // immediately

        then(ret[1]).isEmpty();
        thenOutputContains(new File(ret[0], "logs/output.log"), "Welcome to Falco\n-- started");
    }

    @Test
    public void launcher_from_not_home() throws Exception {
        String[] ret = launch(false); // the process will end because of missing
                                      // pi4j libraries, // therefore it exits
                                      // immediately

        then(ret[1]).isEmpty();
        thenOutputContains(new File(ret[0], "logs/output.log"), "Welcome to Falco\n-- started");
    }

    @Test
    public void launcher_with_help() throws Exception {
        String[] ret = launch("--help"); // the process will end because of --help

        then(ret[1]).contains("usage:").doesNotContain("-- started");
    }

    // --------------------------------------------------------- private methods

    private String[] launch(boolean changeHome, String... args) throws Exception {
        String[] ret = new String[2];

        File home = TMPDIR.newFolder(); ret[0] = home.getAbsolutePath();
        FileUtils.copyDirectory(new File("src/main/falco"), home);
        File falco = new File(home, "bin/falco");
        falco.setExecutable(true);

        List<String> command = new ArrayList<>();
        if (!changeHome) {
            command.add(new File(home, "bin/falco").getAbsolutePath());
        } else {
            command.add("bin/falco");
        }
        Collections.addAll(command, args);

        ProcessBuilder pb = new ProcessBuilder();
        if (changeHome) {
            pb.directory(home);
        }
        pb.command(command);

        Map<String, String> env = pb.environment();
        env.put("CLASSPATH", System.getProperty("java.class.path"));

        Process p = pb.start(); p.waitFor();

        ret[1] = new String(p.getInputStream().readAllBytes());

        return ret;
    }

    private String[] launch(String... command) throws Exception {
        return launch(true, command);
    }


    private void thenOutputContains(File out, String text) {
        then(out).exists().hasContent(text);
    }
}
