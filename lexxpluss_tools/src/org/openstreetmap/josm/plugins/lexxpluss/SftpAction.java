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
import javax.swing.Icon;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.UserInfo;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.actions.OpenFileAction;
import org.openstreetmap.josm.actions.SaveAction;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.plugins.piclayer.layer.PicLayerAbstract;
import org.openstreetmap.josm.plugins.piclayer.layer.PicLayerFromFile;
import org.openstreetmap.josm.tools.ImageProvider;

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
        dialog.showDialog(checkSaveable());
        var value = dialog.getValue();
        if (value != 3)
            dialog.saveSettings();
        if (value == 1)
            download();
        else if (value == 2)
            upload();
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(true);
    }

    /**
     * Checks if the layer is saveable.
     * @return true if the layer is saveable
     */
    private boolean checkSaveable() {
        var layer = getLayerManager().getActiveLayer();
        if (layer == null)
            return false;
        var file = layer.getAssociatedFile();
        if (file == null)
            return false;
        var path = file.getAbsolutePath();
        if (path == null)
            return false;
        return layer.isSavable();
    }

    /**
     * Downloads the osm/png/cal file.
     */
    private void download() {
        Session session = null;
        ChannelSftp channel = null;
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
            sftpGet(channel, remoteOsmPath, localOsmPath);
            sftpGet(channel, remotePngPath, localPngPath);
            sftpGet(channel, remotePngCalPath, localPngCalPath);
            var task = new SftpOpenFileTask(localBasePath);
            MainApplication.worker.submit(task);
        } catch (JSchException | IOException e) {
            System.err.println("JSchException: " + e.getMessage());
            notify(ImageProvider.get("download"), "Sftp download failed");
        } finally {
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

    /**
     * Uploads the osm file.
     */
    private void upload() {
        var icon = ImageProvider.get("save");
        var layer = getLayerManager().getActiveLayer();
        if (layer == null) {
            notify(icon, "No active layer");
            return;
        }
        var file = layer.getAssociatedFile();
        if (file == null) {
            notify(icon, "No associated file");
            return;
        }
        var path = file.getAbsolutePath();
        if (path == null) {
            notify(icon, "No file path");
            return;
        }
        if (!SaveAction.getInstance().doSave(true)) {
            notify(icon, "Local save failed");
            return;
        }
        Session session = null;
        ChannelSftp channel = null;
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
            channel.put(path, ToolsSettings.getOsmPath(), null, ChannelSftp.OVERWRITE);
            notify(icon, "Sftp upload successful");
        } catch (JSchException | SftpException e) {
            System.err.println("JSch/SftpException: " + e.getMessage());
            notify(icon, "Sftp upload failed");
        } finally {
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

    /**
     * Notifies the user.
     * @param icon the icon
     * @param message the message
     */
    private void notify(Icon icon, String message) {
        var notification = new Notification(message).setIcon(icon);
        GuiHelper.runInEDT(notification::show);
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
    private static class SftpOpenFileTask extends OpenFileAction.OpenFileTask {

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
            var layerManager = MainApplication.getLayerManager();
            var layers = layerManager.getLayers();
            var newLayerPos = layers.size();
            for (var l : layerManager.getLayersOfType(PicLayerAbstract.class)) {
                var pos = layers.indexOf(l);
                if (newLayerPos > pos)
                    newLayerPos = pos;
            }
            var layer = new PicLayerFromFile(new File(basePath + ".png"));
            try {
                layer.initialize();
            } catch (IOException e) {
                System.err.println("IOException: " + e.getMessage());
            }
            layerManager.addLayer(layer);
            MainApplication.getMap().mapView.moveLayer(layer, newLayerPos);
        }
    }
}
