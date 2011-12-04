package com.urs.motionVector;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.io.BufferedInputStream;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;


public class motionVector
{

	static BufferedImage img;

	static int width = 352;
	static int height = 288;

	static String fileName = "";
	static int[][] bins;
	
	static ArrayList<ArrayList> scenes = new ArrayList<ArrayList>();
	static ArrayList<Integer> frames = new ArrayList<Integer>();

	public static void main(String[] args)
	{
		fileName = args[0];
		img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		width = 320;
		height = 240;
		bins = new int[3][256];
		VideoProcessing();
		AudioProcessing(args[1]);
	}

	private static void VideoProcessing()
	{
		int VIDEOLEN;
		int DEBUG = 1;
		
		try
		{
			File file = new File(fileName);
			long vidLen = file.length();
			vidLen = vidLen / (height * width * 3);
			vidLen = vidLen / 24;
			vidLen = vidLen / (60);
			VIDEOLEN = (int) vidLen;
			InputStream is = new FileInputStream(file);
			long len = width * height * 3;
			byte[] bytes = new byte[(int) len];
			int[] histPrev = new int[256];
			int histDifferencePrev = 0;

			if (DEBUG == 1)
			{
				VIDEOLEN = 3 * 60;
			}

			for (int i = 0; i < VIDEOLEN * 24; i++)
			{
				int[] histCurrent = new int[256];

				is.read(bytes);
				int ind = 0;
				for (int y = 0; y < height; y++)
				{
					for (int x = 0; x < width; x++)
					{
						short r = (short) (bytes[ind] & 0xff);
						short g = (short) (bytes[ind + height * width] & 0xff);
						short b = (short) (bytes[ind + height * width * 2] & 0xff);
						int Y = (int) (0.299 * r + 0.587 * g + 0.114 * b);
						histCurrent[Math.abs(Y)]++;
						ind++;
					}
				}
				// if first frame, nothing to compare; add it to scene
				if (i == 0)
				{
					frames.add(i);
				}
				// if not first frame, compute histogram
				if (i != 0)
				{
					int histDifferenceCurrent = 0;
					for (int j = 0; j < 256; j++)
					{
						histDifferenceCurrent += Math.pow(
								(histPrev[j] - histCurrent[j]), 2);
					}
					if (i > 1)
					{
						if (histDifferenceCurrent == 0
								|| histDifferencePrev == 0)
						{
							frames.add(i);
						} else if ((histDifferencePrev / histDifferenceCurrent) > 10)
						{
							scenes.add(frames);
							frames = null;
							frames = new ArrayList<Integer>();
							frames.add(i);
						} else
						{
							frames.add(i);
						}
					}
					if (i == 1)
						frames.add(i);
					histDifferencePrev = histDifferenceCurrent;
					if (i == (VIDEOLEN * 24) - 1)
					{
						scenes.add(frames);
					}
				}
				histPrev = histCurrent.clone();
				histCurrent = null;
			}

			System.out.println(scenes.size());
			int midPt_W = (width / 2);
			int midPt_H = (height / 2);
			int[][] prevFrameBlk = new int[16][16];
			int[][] curFrameBlk = new int[8][8];
			int[][] tempPrevFrameBlk = new int[16][16];

			ArrayList<Integer> meanDiff = new ArrayList<Integer>();
			ArrayList<Integer> MotionVector = new ArrayList<Integer>();
			int row, col;
			row = col = 0;
			InputStream videoFS = new FileInputStream(file);
			byte[] FrameByteData = new byte[(int) len];
			int[][] FrameData2D = new int[height][width];

			for (int frameNo = 0; frameNo < VIDEOLEN * 24; frameNo++)
			{
				int ind = 0;
				int rw, cl;
				rw = cl = 0;

				if (frameNo == 0)
				{
					// Add MV as ZERO for FRAME ZERO
					MotionVector.add(0);

					videoFS.read(FrameByteData);
					// Convert FrameByteData to 2d array
					for (int y = 0; y < height; y++)
					{
						for (int x = 0; x < width; x++)
						{
							short r = (short) (FrameByteData[ind] & 0xff);
							short g = (short) (FrameByteData[ind + height
									* width] & 0xff);
							short b = (short) (FrameByteData[ind + height
									* width * 2] & 0xff);
							int Y = (int) (0.299 * r + 0.587 * g + 0.114 * b);
							FrameData2D[rw][cl] = Math.abs(Y);
							ind++;
							cl++;
						}
						rw++;
						cl = 0;
					}
					row = col = 0;
					for (int y = (midPt_H - 8); y < (midPt_H + 8); y++)
					{
						for (int x = (midPt_W - 8); x < (midPt_W + 8); x++)
						{
							prevFrameBlk[row][col] = FrameData2D[y][x];
							col++;
						}
						row++;
						col = 0;
					}
					continue;
				} else if (frameNo == 1)
				{
					ind = 0;
					videoFS.read(FrameByteData);
					rw = cl = 0;
					for (int y = 0; y < height; y++)
					{
						for (int x = 0; x < width; x++)
						{
							short r = (short) (FrameByteData[ind] & 0xff);
							short g = (short) (FrameByteData[ind + height
									* width] & 0xff);
							short b = (short) (FrameByteData[ind + height
									* width * 2] & 0xff);
							int Y = (int) (0.299 * r + 0.587 * g + 0.114 * b);
							FrameData2D[rw][cl] = Math.abs(Y);
							ind++;
							cl++;
						}
						rw++;
						cl = 0;
					}
					row = col = 0;
					for (int y = (midPt_H - 4); y < (midPt_H + 4); y++)
					{
						for (int x = (midPt_W - 4); x < (midPt_W + 4); x++)
						{
							curFrameBlk[row][col] = FrameData2D[y][x];
							col++;
						}
						row++;
						col = 0;
					}
					row = col = 0;
					for (int y = (midPt_H - 8); y < (midPt_H + 8); y++)
					{
						for (int x = (midPt_W - 8); x < (midPt_W + 8); x++)
						{
							tempPrevFrameBlk[row][col] = FrameData2D[y][x];
							col++;
						}
						row++;
						col = 0;
					}
				}
				/*
				 * Compute mean average difference and store per frame per scene
				 */

				int tempMeanDiff = 0;
				int nextRow = 0;
				int colOffset = 0;
				while (nextRow < 16)
				{
					while ((colOffset + 8) < 16)
					{
						for (int i = 0; i < 8; i++)
						{
							for (int j = 0; j < 8; j++)
							{
								// System.out.println(j +" " +colOffset);
								tempMeanDiff += Math.abs(prevFrameBlk[i
										+ nextRow][j + colOffset]
										- curFrameBlk[i][j]);
							}
						}
						meanDiff.add(tempMeanDiff / 64);
						colOffset += 4;
					}
					nextRow += 8;
					colOffset = 0;
				}
				Collections.sort(meanDiff);
				MotionVector.add(meanDiff.get(0));
				meanDiff.clear();

				// copy previous frame data
				for (int i = 0; i < 16; i++)
				{
					for (int j = 0; j < 16; j++)
					{
						prevFrameBlk[i][j] = tempPrevFrameBlk[i][j];
					}
				}

				// Read the frame data for next calculation!
				ind = 0;
				videoFS.read(FrameByteData);
				rw = cl = 0;
				for (int y = 0; y < height; y++)
				{
					for (int x = 0; x < width; x++)
					{
						short r = (short) (FrameByteData[ind] & 0xff);
						short g = (short) (FrameByteData[ind + height * width] & 0xff);
						short b = (short) (FrameByteData[ind + height * width
								* 2] & 0xff);
						int Y = (int) (0.299 * r + 0.587 * g + 0.114 * b);
						FrameData2D[rw][cl] = Math.abs(Y);
						ind++;
						cl++;
					}
					rw++;
					cl = 0;
				}
				row = col = 0;
				for (int y = (midPt_H - 4); y < (midPt_H + 4); y++)
				{
					for (int x = (midPt_W - 4); x < (midPt_W + 4); x++)
					{
						curFrameBlk[row][col] = FrameData2D[y][x];
						col++;
					}
					row++;
					col = 0;
				}
				row = col = 0;
				for (int y = (midPt_H - 8); y < (midPt_H + 8); y++)
				{
					for (int x = (midPt_W - 8); x < (midPt_W + 8); x++)
					{
						tempPrevFrameBlk[row][col] = FrameData2D[y][x];
						col++;
					}
					row++;
					col = 0;
				}

			}
			if (DEBUG == 1)
			{
				int loc = 0;
				for (int MV : MotionVector)
				{
					System.out.println("MotionVector[" + loc++ + "]=" + MV);
				}
			}
		} catch (FileNotFoundException e)
		{
			e.printStackTrace();
			System.exit(0);
		} catch (IOException e)
		{
			e.printStackTrace();
			System.exit(0);
		}
	}

	/*
	 * Start code for audio processing
	 * 
	 * Get the scene/shot wise audio RMS values
	 */
	

	public static double RootMeanSquared(byte[] audio)
	{
		double sum = 0;
		double avg = 0;
		for (int i = 0; i < audio.length; i++)
		{
			sum += Math.abs(audio[i]);
		}
		avg = sum / audio.length;

		double rms = 0;
		for (int i = 0; i < audio.length; i++)
		{
			rms = rms + Math.pow(audio[i] - avg, 2);
		}
		rms = rms / audio.length;
		return rms;
	}

	public static void AudioProcessing(String strAudioFilePath)
	{
		InputStream fileInputStream;
		AudioInputStream audioInputStream = null;
		InputStream inputStream = null;
		float AudioBytesPerFrame = (22050*2)/24;
		ArrayList<Double> SceneRMS = new ArrayList<Double>();
		int DEBUG = 1;
		
		try
		{
			fileInputStream = new FileInputStream(strAudioFilePath);
			inputStream = new BufferedInputStream(fileInputStream);
			audioInputStream = AudioSystem.getAudioInputStream(inputStream);
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		}
		try
		{
			//compute scene/shot wise RMS values for all the frames inside the scene
			for(int SceneNo = 0; SceneNo < scenes.size(); SceneNo++)
			{
				int frameCount = scenes.get(SceneNo).size();
				if(frameCount%2==0)
				{
					int numBytes = (int)(frameCount * AudioBytesPerFrame);
					byte[] abData = new byte[numBytes];
					audioInputStream.read(abData, 0, numBytes);
					SceneRMS.add(RootMeanSquared(abData));
				}
				else 
				{
					int numBytes = (int)((frameCount-1)*AudioBytesPerFrame);
					numBytes+=1837;
					byte[] abData = new byte[numBytes];
					audioInputStream.read(abData, 0, numBytes);
					SceneRMS.add(RootMeanSquared(abData));
				}
			}
			if(DEBUG==1)
			{
				int pos=0;
				for(double rms: SceneRMS)
				{
					System.out.println("SceneNo["+ pos++ +"] = "+rms);
				}
			}
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		} 

	}

}
