package com.breakersoft.plow.json;

import java.util.List;

import com.google.common.collect.Lists;

public class Blueprint {
	
	private String project;
	private String scene;
	private String shot;
	private String name;
	private String username;
	
	private boolean paused;
	private int uid;
	
	private List<BlueprintLayer> layers;
	
	public boolean isPaused() {
		return paused;
	}

	public void setPaused(boolean paused) {
		this.paused = paused;
	}

	public int getUid() {
		return uid;
	}

	public void setUid(int uid) {
		this.uid = uid;
	}

	public String getFullJobName() {
		return String.format("%s-%s.%s-%s_%s", project, scene, shot, username, name);
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getProject() {
		return project;
	}

	public void setProject(String project) {
		this.project = project;
	}

	public String getScene() {
		return scene;
	}

	public void setScene(String scene) {
		this.scene = scene;
	}

	public String getShot() {
		return shot;
	}

	public void setShot(String shot) {
		this.shot = shot;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public List<BlueprintLayer> getLayers() {
		return layers;
	}

	public void setLayers(List<BlueprintLayer> layers) {
		this.layers = layers;
	}
	
	public void addLayer(BlueprintLayer layer) {
		if (layers == null) {
			layers = Lists.newArrayList();
		}
		layers.add(layer);
	}
}