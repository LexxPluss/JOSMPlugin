/**
 * Copyright (c) 2024, LexxPluss Inc.
 * All rights reserved.
 * License: GPL. For details, see LICENSE file.
 */

package org.openstreetmap.josm.plugins.lexxpluss;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.awt.event.ActionEvent;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.UserInfo;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.actions.OpenFileAction;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.plugins.piclayer.layer.PicLayerAbstract;
import org.openstreetmap.josm.plugins.piclayer.layer.PicLayerFromFile;

/**
 * Action for opening/saving using sftp.
 */
public class SftpAction extends JosmAction {

    /**
     * The ssh session.
     */
    Session session = null;

    /**
     * The sftp channel.
     */
    ChannelSftp channel = null;

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
     * Downloads the osm/png/cal file.
     */
    private void download() {
        try {
            var jsch = new JSch();
            session = jsch.getSession(
                    ToolsSettings.getUser(),
                    ToolsSettings.getHost()
            );
            session.setUserInfo(new UserInfoImpl());
            session.connect();
            channel = (ChannelSftp)session.openChannel("sftp");
            channel.connect();
            var remoteOsmPath = ToolsSettings.getOsmPath();
            var remoteBasePath = getBasePath(remoteOsmPath);
            var remotePngPath = remoteBasePath + ".png";
            var remotePngCalPath = remotePngPath + ".cal";
            var tmpdir = Paths.get(System.getProperty("java.io.tmpdir"));
            var localOsmPath = Files.createTempFile(tmpdir, "lexxpluss_tools", ".osm").toString();
            var localBasePath = getBasePath(localOsmPath);
            var localPngPath = localBasePath + ".png";
            var localPngCalPath = localPngPath + ".cal";
            System.out.println("Local base path: " + localBasePath);
            sftpGet(channel, remoteOsmPath, localOsmPath);
            sftpGet(channel, remotePngPath, localPngPath);
            sftpGet(channel, remotePngCalPath, localPngCalPath);
            var task = new SftpOpenFileTask(localBasePath);
            MainApplication.worker.submit(task);
        } catch (JSchException | IOException e) {
            System.err.println("JSchException: " + e.getMessage());
        }
    }

    /**
     * Returns the base path of the path.
     * @param path the path
     * @return the base path
     */
    private String getBasePath(String path) {
        var index = path.lastIndexOf('.');
        return index > 0 ? path.substring(0, index) : path;
    }

    /**
     * Downloads the file using sftp.
     * @param channel the channel
     * @param remotePath the remote path
     * @param localPath the local path
     */
    private void sftpGet(ChannelSftp channel, String remotePath, String localPath) {
        try {
            channel.get(remotePath, localPath, null, ChannelSftp.OVERWRITE);
        } catch (SftpException e) {
            System.err.println("SftpException: " + e.getMessage());
        }
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

    /**
     * Open file task for sftp.
     */
    private class SftpOpenFileTask extends OpenFileAction.OpenFileTask {

        /**
         * The base path.
         */
        String basePath = null;

        /**
         * Constructs a new {@code SftpOpenFileTask}.
         * @param basePath the base path
         */
        public SftpOpenFileTask(String basePath) {
            super(Arrays.asList(new File[]{new File(basePath + ".osm")}), null);
            this.basePath = basePath;
        }

        @Override
        protected void finish() {
            super.finish();
            var newLayerPos = MainApplication.getLayerManager().getLayers().size();
            for (var l : MainApplication.getLayerManager().getLayersOfType(PicLayerAbstract.class)) {
                var pos = MainApplication.getLayerManager().getLayers().indexOf(l);
                if (newLayerPos > pos)
                    newLayerPos = pos;
            }
            var layer = new PicLayerFromFile(new File(basePath + ".png"));
            try {
                layer.initialize();
            } catch (IOException e) {
                System.err.println("IOException: " + e.getMessage());
            }
            MainApplication.getLayerManager().addLayer(layer);
            MainApplication.getMap().mapView.moveLayer(layer, newLayerPos++);
            if (channel != null) {
                channel.disconnect();
                channel = null;
            }
            if (session != null) {
                session.disconnect();
                session = null;
            }
        }
    }
}
