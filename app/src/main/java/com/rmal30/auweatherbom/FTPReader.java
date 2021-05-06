package com.rmal30.auweatherbom;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;

public class FTPReader {
    public FTPClient ftp;
    public String root;

    public FTPReader(String root){
        this.root = root;
    }

    //Connect and login to a ftp site
    public void setup() {
        try {
            if(ftp==null || !ftp.sendNoOp()) {
                ftp = new FTPClient();
                ftp.setBufferSize(65536);
                URL url = new URL(this.root);
                ftp.connect(url.getHost(), 21);
                ftp.login("anonymous", String.valueOf(Math.floor(Math.random() * 10000) + "@b.c"));
                ftp.setFileType(FTP.BINARY_FILE_TYPE);
                ftp.enterLocalPassiveMode();
            }
        } catch (Exception e) {
            ftp = null;
        }
    }

    public boolean isOpen(){
        try {
            return this.ftp != null && this.ftp.sendNoOp();
        } catch (IOException e){
            e.printStackTrace();
            return false;
        }
    }

    private ByteArrayOutputStream readFileStream(String path, String filename) {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(65536);
        try {
            URL url = new URL(path + filename);
            this.setup();
            ftp.retrieveFile(url.getPath(), outputStream);
            if(outputStream.size()==0) {
                return null;
            }
            return outputStream;
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //Find out when FTP data files were last updated
    public String getDateModified(String path, String filename) {
        this.setup();
        try {
            return ftp.getModificationTime(new URL(path + filename).getPath());
        }catch(Exception e) {
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
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String readText(String path, String filename) {
        final ByteArrayOutputStream outputStream = readFileStream(path, filename);
        try {
            String s = outputStream.toString("UTF-8");
            outputStream.close();
            return s;
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
