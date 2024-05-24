/**
 * Copyright (c) 2024, LexxPluss Inc.
 * All rights reserved.
 * License: GPL. For details, see LICENSE file.
 */

package org.openstreetmap.josm.plugins.lexxpluss;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.awt.event.ActionEvent;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.UserInfo;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.actions.OpenFileAction;
import org.openstreetmap.josm.gui.MainApplication;

/**
 * Action for opening/saving using sftp.
 */
public class SftpAction extends JosmAction {

    /**
     * Constructs a new {@code SftpAction}.
     */
    public SftpAction() {
        super("Open/Save using sftp... (LexxPluss)", "mapmode/sftpaction", "Open/Save using sftp for LexxPluss", null, false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        var dialog = new SftpDialog();
        dialog.showDialog();
        var value = dialog.getValue();
        System.out.println("Value: " + value);
        if (value != 3)
            dialog.saveSettings();
        if (value == 1)
            download();
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(true);
    }

    /**
     * Downloads the osm file.
     */
    private void download() {
        try {
            var jsch = new JSch();
            var session = jsch.getSession(
                    ToolsSettings.getUser(),
                    ToolsSettings.getHost()
            );
            session.setUserInfo(new UserInfoImpl());
            session.connect();
            var channel = (ChannelSftp)session.openChannel("sftp");
            channel.connect();
            var tmpdir = Paths.get(System.getProperty("java.io.tmpdir"));
            var path = Files.createTempFile(tmpdir, "lexxpluss_tools", ".osm").toString();
            channel.get(ToolsSettings.getOsmPath(), path, null, ChannelSftp.OVERWRITE);
            open(path);
        } catch (JSchException | SftpException | IOException e) {
            System.err.println("JSchException: " + e.getMessage());
        }
    }

    /**
     * Opens the downloaded osm file.
     * @param path the path to the file
     */
    private void open(String path) {
        var files = new File[]{new File(path)};
        var task = new OpenFileAction.OpenFileTask(Arrays.asList(files), null);
        MainApplication.worker.submit(task);
    }

    /**
     * Implementation of UserInfo.
     */
    private static class UserInfoImpl implements UserInfo {

        /**
         * Constructs a new {@code UserInfoImpl}.
         */
        public UserInfoImpl() {
            super();
        }

        @Override
        public String getPassphrase() {
            return ToolsSettings.getPassword();
        }

        @Override
        public String getPassword() {
            return ToolsSettings.getPassword();
        }

        @Override
        public boolean promptPassword(String s) {
            return true;
        }

        @Override
        public boolean promptPassphrase(String s) {
            return true;
        }

        @Override
        public boolean promptYesNo(String s) {
            return true;
        }

        @Override
        public void showMessage(String s) {
            System.out.println("Message: " + s);
        }
    }
}
