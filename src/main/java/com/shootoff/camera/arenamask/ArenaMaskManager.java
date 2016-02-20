/*
 * ShootOFF - Software for Laser Dry Fire Training
 * Copyright (C) 2016 phrack
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.shootoff.camera.arenamask;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.Semaphore;

import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.BufferUtils;
import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;

public class ArenaMaskManager implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(ArenaMaskManager.class);

	
	private Buffer cBuffer;
	private long delay = 0;
	
	private Mat mask = new Mat();
	private Size dsize = null;
	
	private final static int LUM_MA_LENGTH = 2;
	
	private int avgLums = 0;
	
	public volatile Mask maskFromArena = null;
	
	public Semaphore sem = new Semaphore(1);
	
	public volatile boolean isStreaming = true;

	@Override
	public void run() {
		logger.debug("Starting arenaMaskManager thread");
		
		//startRecordingStream(new File("testingArenaMask.mp4"));
		
		while (isStreaming)
		{	
			try {
				sem.acquire();
			} catch (InterruptedException e) {
				continue;
			}
			if (maskFromArena == null)
			{
				sem.release();
				continue;
			}
			
			//while (!cBuffer.isEmpty())
			//{
				
				//Mask nextMask = ((Mask)cBuffer.remove());
				Mask nextMask = maskFromArena;
				long curDelay = System.currentTimeMillis() - nextMask.timestamp;
				
				if (curDelay > delay)
				{
					sem.release();
					continue;
				}
				
				
				Mat nextMat = nextMask.getLumMask(dsize);
				
				recordMask(nextMask);
				
				int nextMaskAvgLum = nextMask.getAvgMaskLum();
				
				maskFromArena = null;
				
				sem.release();
									
				//logger.debug("aMM {} - {}", cBuffer.size(), curDelay);

				//logger.warn("updatingMask {} {} - {} {} {}", nextMat.cols(), nextMat.rows(), nextMask.timestamp, avgLums, nextMask.getAvgMaskLum());

				
				for (int y = 0; y < nextMat.rows(); y++)
				{
					for (int x = 0; x < nextMat.cols(); x++)
					{
						byte[] maskpx = { 0 };
						mask.get(y,x, maskpx);
						
						byte[] nextmatpx = { 0 };
						nextMat.get(y, x, nextmatpx);
						
						int newLum = nextmatpx[0] * (avgLums/nextMaskAvgLum);
						
						
						maskpx[0] = (byte) ((byte)((maskpx[0] * (LUM_MA_LENGTH-1)) + newLum) / LUM_MA_LENGTH);
					
						//if (x==320&&y==240)
						//	logger.warn("pixel {} {} - {} {} {}", x, y, mask.get(y,x)[0], newLum, curMask[0]);

						mask.put(y, x, maskpx);
						
					
					}
				}
			//}		
				

		}
		
		stopRecordingStream();
	}
	
	
	public void setDelay(long delay)
	{
		this.delay = delay;
		
		//this.delay = 150;
	}
	
	public ArenaMaskManager()
	{
		cBuffer = BufferUtils.synchronizedBuffer(new CircularFifoBuffer(70));
		
		
	}
	
	public void start(int width, int height)
	{
		mask = new Mat(height, width, CvType.CV_8UC1);
		dsize = new Size(width, height);

		(new Thread(this)).start();
	}
	
	public Mat getMask()
	{
		return mask;
	}
	
	
	private boolean recordingStream = false;
	private boolean isFirstStreamFrame = true;
	private IMediaWriter videoWriterStream;
	private long recordingStartTime;
	private void recordMask(Mask mask)
	{
		if (recordingStream) {
			BufferedImage image = ConverterFactory.convertToType(mask.bImage, BufferedImage.TYPE_3BYTE_BGR);
			IConverter converter = ConverterFactory.createConverter(image, IPixelFormat.Type.YUV420P);

			IVideoPicture frame = converter.toPicture(image,
					(System.currentTimeMillis() - recordingStartTime) * 1000);
			frame.setKeyFrame(isFirstStreamFrame);
			frame.setQuality(0);
			isFirstStreamFrame = false;

			videoWriterStream.encodeVideo(0, frame);
		}
	}
	
	public void startRecordingStream(File videoFile) {
		logger.debug("Writing Video Feed To: {}", videoFile.getAbsoluteFile());
		
		int width = (int)dsize.width;
		int height = (int)dsize.height;
			
		
		videoWriterStream = ToolFactory.makeWriter(videoFile.getName());
		videoWriterStream.addVideoStream(0, 0, ICodec.ID.CODEC_ID_H264, width, height);
		recordingStartTime = System.currentTimeMillis();
		isFirstStreamFrame = true;

		recordingStream = true;
	}

	public void stopRecordingStream() {
		recordingStream = false;
		videoWriterStream.close();
	}
	
	@SuppressWarnings("unchecked")
	public synchronized void insert(BufferedImage bImage, long timestamp)
	{
		cBuffer.add(new Mask(bImage, timestamp));
	}
	
	public synchronized Mat getMaskForTimestamp(Size dsize, long timestamp)
	{
		long delayedTimestamp = timestamp - delay;
		
		long peekTimestamp = peekTimestamp();

		if (cBuffer.isEmpty())
			return null;

		
		while (peekTimestamp < delayedTimestamp-3)
		{
			/*if ((delayedTimestamp%30)==0)
			{
				logger.warn("getArenaMask {} remove", peekTimestamp - delayedTimestamp);
			}*/
				
			
			if (cBuffer.isEmpty())
				return null;
			cBuffer.remove();
			peekTimestamp = peekTimestamp();
		}
		
		/*if (peekTimestamp > delayedTimestamp)
		{
			if ((delayedTimestamp%30)==0)
			{
				logger.warn("getArenaMask {} future", peekTimestamp - delayedTimestamp);
			}
			return null;
		}*/
		/*if ((delayedTimestamp%30)==0)
		{
			logger.warn("getArenaMask {} good", peekTimestamp - delayedTimestamp);
		}*/
		
		return ((Mask)cBuffer.get()).getMask(dsize);
	}
	
	private synchronized long peekTimestamp()
	{
		if (cBuffer.isEmpty())
			return -1;
		
		return ((Mask)cBuffer.get()).timestamp;
	}


	public void updateAvgLums(int lumsMovingAverageAcrossFrame, long currentFrameTimestamp) {
		avgLums = lumsMovingAverageAcrossFrame;
	}

	
}