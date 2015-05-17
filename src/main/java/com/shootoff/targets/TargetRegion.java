package com.shootoff.targets;

import java.util.Map;

import javafx.scene.paint.Color;

public interface TargetRegion {
	public static final Color UNSELECTED_STROKE_COLOR = Color.BLACK;
	public static final Color SELECTED_STROKE_COLOR = Color.GOLD;
	
	public void changeWidth(double widthDelta);
	public void changeHeight(double heightDelta);
	public RegionType getType();
	public boolean tagExists(String name);
	public String getTag(String name);
	public Map<String, String> getAllTags();
	public void setTags(Map<String, String> newTags);
}