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
import java.util.concurrent.TimeUnit;
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
        String[] ret = launch("--help"); // the process will end because of --help
        thenFileContainsButNot(new File(ret[0], "logs/output.log"), "Welcome to Falco\nUsage:");
    }

    @Test
    public void launcher_from_not_home() throws Exception {
        String[] ret = launch("--help");  // the process will end because of --help
        thenFileContainsButNot(new File(ret[0], "logs/output.log"), "Welcome to Falco\nUsage:");
    }

    @Test
    public void launcher_with_help() throws Exception {
        String[] ret = launch("--help"); // the process will end because of --help
        then(ret[1]).contains("Usage:").doesNotContain("-- started");

        ret = launch("-h"); // the process will end because of -h
        then(ret[1]).contains("Usage:").doesNotContain("-- started");
    }

    @Test
    public void launcher_with_unkown_parameter() throws Exception {
        String[] ret = launch("--invalidoption");
        then(ret[1])
            .contains("Unknown option: '--invalidoption'")
            .contains("Usage:")
            .doesNotContain("-- started");
    }

    @Test
    public void no_heartbeat() throws Exception {
        String[] ret = launch(3000, false, false, "--nogpio", "--noheartbeat");
        thenFileContainsButNot(new File(ret[0], "logs/falco.0.log"), "Heartbeat disabled", "heartbeat");
    }

    @Test
    public void no_gpio() throws Exception {
        String[] ret = launch(3000, false, false, "--nogpio");
        thenFileContainsButNot(new File(ret[0], "logs/falco.0.log"), "falco started", "Unable to load [libpi4j.so]");
    }

    // --------------------------------------------------------- private methods

    private String[] launch(long timeout, boolean changeHome, boolean readOut, String... args) throws Exception {
        String[] ret = new String[2];

        File home = TMPDIR.newFolder(); ret[0] = home.getAbsolutePath();
        FileUtils.copyDirectory(new File("src/main/falco"), home);
        FileUtils.copyFile(
            new File("src/test/falco/conf/logging-finest.properties"),
            new File(home, "conf/logging.properties")
        );
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


        Process p = pb.start(); p.waitFor(timeout, TimeUnit.MILLISECONDS);
        if (readOut) {
            ret[1] = new String(p.getInputStream().readAllBytes());
        } else {
            ret[1] = null;
        }

        p.destroy(); // just in case the process did not end

        return ret;
    }

    private String[] launch(String... command) throws Exception {
        return launch(5000, true, true, command);
    }

    private String[] launch(boolean changeHome, String... args) throws Exception {
        return launch(5000, changeHome, true, args);

    }

    private void thenFileContainsButNot(File file, String include, String exclude) throws Exception {
        then(file).exists();

        String content = FileUtils.readFileToString(file, "UTF-8");
        System.out.println(content);
        then(content).contains(include);
        if (exclude != null) {
            then(content).doesNotContain(exclude);
        }
    }

    private void thenFileContainsButNot(File file, String include) throws Exception {
        thenFileContainsButNot(file, include, null);
    }
}
