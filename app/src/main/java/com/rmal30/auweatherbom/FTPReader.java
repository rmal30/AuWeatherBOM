package com.rmal30.auweatherbom;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;

public class FTPReader {
    public FTPClient ftp;
    public String root;
    public String username;
    public String password;
    public static int BUFFER_SIZE = 65536;

    public FTPReader(String root, String username, String password) {
        this.root = root;
        this.username = username;
        this.password = password;
    }

    //Connect and login to a ftp site
    public void setup() {
        try {
            if(ftp == null || !ftp.sendNoOp()) {
                ftp = new FTPClient();
                ftp.setBufferSize(FTPReader.BUFFER_SIZE);
                URL url = new URL(this.root);
                ftp.connect(url.getHost(), 21);
                ftp.login(this.username, this.password);
                ftp.setFileType(FTP.BINARY_FILE_TYPE);
                ftp.enterLocalPassiveMode();
            }
        } catch (Exception e) {
            ftp = null;
        }
    }

    public boolean isOpen() {
        try {
            return this.ftp != null && this.ftp.sendNoOp();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private ByteArrayOutputStream readFileStream(String path, String filename) {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(FTPReader.BUFFER_SIZE);
        try {
            URL url = new URL(path + filename);
            this.setup();
            ftp.retrieveFile(url.getPath(), outputStream);
            if (outputStream.size() == 0) {
                return null;
            }
            return outputStream;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //Find out when FTP data files were last updated
    public String getDateModified(String path, String filename) {
        this.setup();
        try {
            URL url = new URL(path + filename);
            return ftp.getModificationTime(url.getPath());
        } catch (Exception e) {
            return null;
        }
    }

    //Download file and save it as a byte array
    public byte[] readFile(String path, String filename) {
        final ByteArrayOutputStream outputStream = readFileStream(path, filename);
        try {
            byte[] bytes = outputStream.toByteArray();
            outputStream.close();
            return bytes;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String readText(String path, String filename) {
        final ByteArrayOutputStream outputStream = readFileStream(path, filename);
        try {
            String text = outputStream.toString("UTF-8");
            outputStream.close();
            return text;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
