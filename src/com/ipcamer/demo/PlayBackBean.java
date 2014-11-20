package com.ipcamer.demo;

import java.io.Serializable;

public class PlayBackBean implements Serializable{
 private String did;
 private String path;
 private int videotime;
public String getDid() {
	return did;
}
public void setDid(String did) {
	this.did = did;
}
public String getPath() {
	return path;
}
public void setPath(String path) {
	this.path = path;
}
public void setVideotime(int time){
	this.videotime=time;
}
public int getVideotime(){
	return videotime;
}
}
