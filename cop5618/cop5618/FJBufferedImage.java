package cop5618;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;
import java.util.function.Function;

import javax.imageio.ImageIO;

/*
import org.junit.BeforeClass;
*/
public class FJBufferedImage extends BufferedImage {

	private static boolean isSet = false;
	private static boolean isGet = false;

	class Imagetasker extends RecursiveAction {

		int xStart, yStart, w, h, rgbArray[], offset;

		public Imagetasker(int xStart, int yStart, int w, int h, int[] rgbArray, int offset) {
			super();
			this.xStart = xStart;
			this.yStart = yStart;
			this.w = w;
			this.h = h;
			this.rgbArray = rgbArray;
			this.offset = offset;
		}

		@Override
		protected void compute() {

			int noOfPixelsToBeSet = h * w;
			if (noOfPixelsToBeSet <= 10000) { // if the no. of pixels to set is
												// 10000, do it serially
				if (isSet) {
					FJBufferedImage.super.setRGB(xStart, yStart, w, h, rgbArray, offset, w);
				} else if (isGet) {
					FJBufferedImage.super.getRGB(xStart, yStart, w, h, rgbArray, offset, w);
				}
			} else {
				int halfHeight = h / 2;
				Imagetasker left = new Imagetasker(xStart, yStart, w, halfHeight, rgbArray, offset);
				left.fork();
				int newH = h - halfHeight;
				int newYStart = yStart + halfHeight;
				int newOffset = newYStart * w;
				Imagetasker right = new Imagetasker(xStart, newYStart, w, newH, rgbArray, newOffset);
				right.compute();
				left.join();
			}
		}
	}

	static ForkJoinPool forkJoinPool = ForkJoinPool.commonPool(); // This
																	// encourages
																	// the pool
																	// to use
																	// all the
																	// available
																	// processors.

	/** Constructors */

	public FJBufferedImage(int width, int height, int imageType) {
		super(width, height, imageType);
	}

	public FJBufferedImage(int width, int height, int imageType, IndexColorModel cm) {
		super(width, height, imageType, cm);
	}

	public FJBufferedImage(ColorModel cm, WritableRaster raster, boolean isRasterPremultiplied,
			Hashtable<?, ?> properties) {
		super(cm, raster, isRasterPremultiplied, properties);
	}

	/**
	 * Creates a new FJBufferedImage with the same fields as source.
	 * 
	 * @param source
	 * @return
	 */
	public static FJBufferedImage BufferedImageToFJBufferedImage(BufferedImage source) {
		Hashtable<String, Object> properties = null;
		String[] propertyNames = source.getPropertyNames();
		if (propertyNames != null) {
			properties = new Hashtable<String, Object>();
			for (String name : propertyNames) {
				properties.put(name, source.getProperty(name));
			}
		}
		return new FJBufferedImage(source.getColorModel(), source.getRaster(), source.isAlphaPremultiplied(),
				properties);
	}

	@Override
	public void setRGB(int xStart, int yStart, int w, int h, int[] rgbArray, int offset, int scansize) {
		/**** IMPLEMENT THIS METHOD USING PARALLEL DIVIDE AND CONQUER *****/
		setSet();
		forkJoinPool.invoke(new Imagetasker(xStart, yStart, w, h, rgbArray, offset));
	}

	@Override
	public int[] getRGB(int xStart, int yStart, int w, int h, int[] rgbArray, int offset, int scansize) {
		/**** IMPLEMENT THIS METHOD USING PARALLEL DIVIDE AND CONQUER *****/
		setGet();
		forkJoinPool.invoke(new Imagetasker(xStart, yStart, w, h, rgbArray, offset));
		return rgbArray;
	}
	
	public static void setSet() {
		FJBufferedImage.isGet = false;
		FJBufferedImage.isSet = true;
	}

	public static void setGet() {
		FJBufferedImage.isSet = false;
		FJBufferedImage.isGet = true;
	}

}
