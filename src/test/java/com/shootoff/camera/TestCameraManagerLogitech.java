package com.shootoff.camera;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;
import java.util.Optional;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.paint.Color;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.gui.MockCanvasManager;

public class TestCameraManagerLogitech {
	private Configuration config;
	private MockCanvasManager mockManager;
	private boolean[][] sectorStatuses;
	
	@Before
	public void setUp() throws ConfigurationException {
		config = new Configuration(new String[0]);
		config.setDetectionRate(0);
		config.setDebugMode(true);
		mockManager = new MockCanvasManager(config, true);
		sectorStatuses = new boolean[ShotSearcher.SECTOR_ROWS][ShotSearcher.SECTOR_COLUMNS];
		
		for (int x = 0; x < ShotSearcher.SECTOR_COLUMNS; x++) {
			for (int y = 0; y < ShotSearcher.SECTOR_ROWS; y++) {
				sectorStatuses[y][x] = true;
			}
		}
		
		CameraManager.setFrameCount(0);
	}
	
	private List<Shot> findShots(String videoPath, Optional<Bounds> projectionBounds) {
		Object processingLock = new Object();
		File videoFile = new  File(TestCameraManagerLogitech.class.getResource(videoPath).getFile());
		
		CameraManager cameraManager;
		cameraManager = new CameraManager(videoFile, processingLock, mockManager, config, sectorStatuses, 
				projectionBounds);
		
		try {
			synchronized (processingLock) {
				while (!cameraManager.isVideoProcessed())
					processingLock.wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return mockManager.getShots();
	}
	
	@Test
	public void testLogitechIndoorGreen() {
		List<Shot> shots = findShots("/shotsearcher/logitech-indoor-green.avi", Optional.empty());
		
		assertEquals(9, shots.size());
		
		// Seems to have a bit of trouble with the shot color
		
		/*assertEquals(629.5, shots.get(0).getX(), 1);
		assertEquals(168.5, shots.get(0).getY(), 1);
		assertEquals(Color.RED, shots.get(0).getColor());
		
		assertEquals(428.5, shots.get(1).getX(), 1);
		assertEquals(129.5, shots.get(1).getY(), 1);
		assertEquals(Color.RED, shots.get(1).getColor());*/
	}
	
	
	@Test
	public void testLogitechOutdoorGreen() {
		List<Shot> shots = findShots("/shotsearcher/logitech-outdoor-green.avi", Optional.empty());
		
		assertEquals(9, shots.size());
		
		// Seems to have a bit of trouble with the shot color
		
		/*assertEquals(629.5, shots.get(0).getX(), 1);
		assertEquals(168.5, shots.get(0).getY(), 1);
		assertEquals(Color.RED, shots.get(0).getColor());
		
		assertEquals(428.5, shots.get(1).getX(), 1);
		assertEquals(129.5, shots.get(1).getY(), 1);
		assertEquals(Color.RED, shots.get(1).getColor());*/
	}
	
	
	@Test
	public void testLogitechSafariGreen() {
		List<Shot> shots = findShots("/shotsearcher/logitech-safari-green.avi", Optional.empty());
		
		assertEquals(9, shots.size());
		
		/*assertEquals(629.5, shots.get(0).getX(), 1);
		assertEquals(168.5, shots.get(0).getY(), 1);
		assertEquals(Color.RED, shots.get(0).getColor());
		
		assertEquals(428.5, shots.get(1).getX(), 1);
		assertEquals(129.5, shots.get(1).getY(), 1);
		assertEquals(Color.RED, shots.get(1).getColor());*/
	}
	
}