package com.urs.motionVector;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

public class motionVector
{

	/**
	 * @param args
	 */
	static BufferedImage img;

	static int width = 352;
	static int height = 288;

	static String fileName = "";
	static int[][] bins;

	public static void main(String[] args)
	{
		// TODO Auto-generated method stub
		fileName = args[0];
		img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		width = 320;
		height = 240;
		bins = new int[3][256];
		ReadRGB();
	}

	private static void ReadRGB()
	{
		// TODO Auto-generated method stub
		int VIDEOLEN = 10 * 60;
		try
		{
			File file = new File(fileName);
			InputStream is = new FileInputStream(file);
			ArrayList<ArrayList> scenes = new ArrayList<ArrayList>();
			long len = width * height * 3;
			byte[] bytes = new byte[(int) len];
			int[] histPrev = new int[256];
			int histDifferencePrev = 0;
			ArrayList<Integer> frames = new ArrayList<Integer>();
			
			int[][] SceneMV = new int[1000][1000];
			int[] meanDiff = new int[200]; // CHECK!!
			
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
						int pix = 0xff000000 | ((r & 0xff) << 16)
								| ((g & 0xff) << 8) | (b & 0xff);
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
			int[][] prevFrameBlk = new int[256][256];
			int[][] curFrameBlk = new int[256][256];

			int row, col;
			row = col = 0;
			InputStream videoFS = new FileInputStream(file);
			byte[] FrameByteData = new byte[(int)len];

			for (int SceneNum = 0; SceneNum < scenes.size(); SceneNum++)
			{
				for (int FrameNo = 0; FrameNo < scenes.get(SceneNum).size(); FrameNo = FrameNo + 1)
				{
					if(FrameNo==0)
					{
						SceneMV[SceneNum][FrameNo] = -100;// JUNK VALUE!! -- CHECK!
						continue;
					}
					else if(FrameNo > 1)
					{
						for (int i = 0; i < VIDEOLEN * 24; i++)
						{
							if (i + 1 == (FrameNo))
								break;							
							else
								videoFS.skip(len);
						}
					}
					// Previous Frame BLK: 16 * 16 pts
					int index = 0;
					
					videoFS.read(FrameByteData);
					
					// Convert FrameByteData to 2d array
					int ind = 0;
					int[][] FrameData2D = new int[height][width];
					int rw, cl;
					rw=cl=0;
					for (int y = 0; y < height; y++)
					{
						for (int x = 0; x < width; x++)
						{
							short r = (short) (FrameByteData[ind] & 0xff);
							short g = (short) (FrameByteData[ind + height * width] & 0xff);
							short b = (short) (FrameByteData[ind + height * width * 2] & 0xff);
							int Y = (int) (0.299 * r + 0.587 * g + 0.114 * b);
							FrameData2D[rw][cl] = Math.abs(Y);
							ind++;
							cl++;
						}
						rw++;
						cl=0;
					}
					for (int y = (midPt_H - 8); y < (midPt_H + 8); y++)
					{
						for (int x = (midPt_W - 8); x < (midPt_W + 8); x++)
						{
							prevFrameBlk[row][col] = FrameData2D[y][x];
							index++;
							col++;
						}
						row++;
						col = 0;
					}
					// Current Frame BLK: 8 * 8 pts
					ind = 0;
					videoFS.read(FrameByteData);
					rw=cl=0;
					for (int y = 0; y < height; y++)
					{
						for (int x = 0; x < width; x++)
						{
							short r = (short) (FrameByteData[ind] & 0xff);
							short g = (short) (FrameByteData[ind + height * width] & 0xff);
							short b = (short) (FrameByteData[ind + height * width * 2] & 0xff);
							int Y = (int) (0.299 * r + 0.587 * g + 0.114 * b);
							FrameData2D[rw][cl] = Math.abs(Y);
							ind++;
							cl++;
						}
						rw++;
						cl=0;
					}
					row = col = 0;
					for (int y = (midPt_H - 4); y < (midPt_H + 4); y++)
					{
						for (int x = (midPt_W - 4); x < (midPt_W + 4); x++)
						{
							curFrameBlk[row][col] = FrameData2D[y][x];
							index++;
							col++;
						}
						row++;
						col = 0;
					}

					/*
					 * Compute mean average difference and store per frame per
					 * scene
					 */

					int tempMeanDiff = 0;
					int nextRow = 0;
					int colOffset = 0;
					int pos=0;
					while(nextRow<16)
					{
						while(colOffset<16)
						{
							for (int i = 0; i < 8; i++)
							{
								for (int j = 0; j < 8; j++)
								{
									tempMeanDiff += Math.abs(prevFrameBlk[i+nextRow][j+colOffset]
											- curFrameBlk[i][j]);
								}
							}
							meanDiff[pos++] = (tempMeanDiff/64);
							colOffset+=8;
						}
						nextRow+=8;
						colOffset=0;
					}
					Arrays.sort(meanDiff);
					SceneMV[SceneNum][FrameNo]=(int)meanDiff[0];
					System.out.println("Mean Diff"+" "+SceneMV[SceneNum][FrameNo]+" "+ "Frame No:"+FrameNo+" "+ "Scene No:"+SceneNum);
					

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

	// private static
}
