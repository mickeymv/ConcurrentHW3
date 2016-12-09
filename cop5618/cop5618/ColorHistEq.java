package cop5618;

import java.awt.List;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.util.ArrayList;
//import java.awt.image.Color;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ColorHistEq {

	static String[] labels = { "getRGB", "convert to HSB and create brightness map", "parallel prefix",
			"probability array", "equalize pixels", "setRGB" };

	static ColorHistEq histInstance = new ColorHistEq();

	static int bins = 256; // number of bins in the histogram

	class HSBPixel {
		float h;
		float s;
		float b;

		public HSBPixel(float[] hsbArr) {
			h = hsbArr[0];
			s = hsbArr[1];
			b = hsbArr[2];
		}

		@Override
		public String toString() {
			return String.valueOf(b);
		}
	};

	static Timer colorHistEq_serial(BufferedImage image, BufferedImage newImage) {
		Timer time = new Timer(labels);
		/**
		 * IMPLEMENT SERIAL METHOD
		 */
		ColorModel colorModel = ColorModel.getRGBdefault();
		int w = image.getWidth();
		int h = image.getHeight();
		time.now();
		int[] sourceRGBPixelArray = image.getRGB(0, 0, w, h, new int[w * h], 0, w);
		time.now(); // getRGB

		Map<Object, Long> binsMap = Arrays.stream(sourceRGBPixelArray) // get
																		// array
																		// of
																		// pixels
																		// from
																		// image
																		// and
																		// convert
																		// to
																		// IntStream
				// convert to stream of HSBPixel
				.mapToObj(pixel -> {
					float[] hsbFloatArrayofPixel = new float[] { 0, 0, 0 };
					java.awt.Color.RGBtoHSB(colorModel.getRed(pixel), colorModel.getGreen(pixel),
							colorModel.getBlue(pixel), hsbFloatArrayofPixel);
					return histInstance.new HSBPixel(hsbFloatArrayofPixel);
				})
				// get histogram's bins map with number of pixels in each bin
				.collect(Collectors.groupingBy(pixel -> (int) (pixel.b * bins), Collectors.counting()));

		time.now(); // convert to HSB and create brightness map

		double[] binsArray = binsMap.entrySet().stream().map(Map.Entry::getValue).mapToDouble(hsbCounts -> hsbCounts)
				.toArray();
		/*
		 * System.out.println("\n\nThe binsCount are: \n\n" + binsMap.get(0) +
		 * ", "+binsMap.get(1)+ ", "+ binsMap.get(2) + ", "+ binsMap.get(3) +
		 * ", "+ binsMap.get(4) + "\n\n");
		 */
		/*
		 * collect(Collectors.groupingBy(hsb -> { if (hsb.b <= 0.1) { return 0;
		 * } else if (hsb.b <= 0.2) { return 1; } else if (hsb.b <= 0.3) {
		 * return 2; } else if (hsb.b <= 0.4) { return 3; } else if (hsb.b <=
		 * 0.5) { return 4; } else if (hsb.b <= 0.6) { return 5; } else if
		 * (hsb.b <= 0.7) { return 6; } else if (hsb.b <= 0.8) { return 7; }
		 * else if (hsb.b <= 0.9) { return 8; } else { return 9; } }));
		 */

		// time.now(); // convert to HSB and create brightness map

		/*
		 * // count of pixels in each histogram bin double[] binsArray =
		 * binsMap.entrySet().stream().map(Map.Entry::getValue).mapToDouble(
		 * hsbList -> hsbList.size()) .toArray();
		 * 
		 * System.out.println("\n\nThe binsCount are: \n\n" + binsArray[0] +
		 * ", "+binsArray[1]+ binsArray[2] + ", "+ binsArray[3] + ", "+
		 * binsArray[4] + "\n\n");
		 */

		// calculate prefix sum
		Arrays.parallelPrefix(binsArray, (x, y) -> x + y);

		time.now(); // parallel prefix

		double noOfPixels = w * h;

		// get the cumulative probability array (divide prefix counts by size of
		// pixels)
		final double[] binsCPArray = Arrays.stream(binsArray).map(x -> (x / noOfPixels)).toArray();

		time.now(); // cumulative probability array

		int[] outputRGBPixelArray = Arrays.stream(sourceRGBPixelArray).mapToObj(pixel -> {
			float[] hsbFloatArrayofPixel = new float[] { 0, 0, 0 };
			java.awt.Color.RGBtoHSB(colorModel.getRed(pixel), colorModel.getGreen(pixel), colorModel.getBlue(pixel),
					hsbFloatArrayofPixel);
			return histInstance.new HSBPixel(hsbFloatArrayofPixel);
		}).mapToInt(hsb -> java.awt.Color.HSBtoRGB(hsb.h, hsb.s,
				(float) binsCPArray[Math.min((int) (hsb.b * bins), bins - 1)])).toArray();

		/*
		 * int[] outputRGBPixelArray =
		 * Arrays.stream(sourceRGBPixelArray).mapToObj(pixel -> { float[]
		 * hsbFloatArrayofPixel = new float[] { 0, 0, 0 };
		 * java.awt.Color.RGBtoHSB(colorModel.getRed(pixel),
		 * colorModel.getGreen(pixel), colorModel.getBlue(pixel),
		 * hsbFloatArrayofPixel); return histInstance.new
		 * HSBPixel(hsbFloatArrayofPixel); }).map(hsb -> { if (hsb.b <= 0.1) {
		 * hsb.b = (float) binsCPArray[0]; } else if (hsb.b <= 0.2) { hsb.b =
		 * (float) binsCPArray[1]; } else if (hsb.b <= 0.3) { hsb.b = (float)
		 * binsCPArray[2]; } else if (hsb.b <= 0.4) { hsb.b = (float)
		 * binsCPArray[3]; } else if (hsb.b <= 0.5) { hsb.b = (float)
		 * binsCPArray[4]; } else if (hsb.b <= 0.6) { hsb.b = (float)
		 * binsCPArray[5]; } else if (hsb.b <= 0.7) { hsb.b = (float)
		 * binsCPArray[6]; } else if (hsb.b <= 0.8) { hsb.b = (float)
		 * binsCPArray[7]; } else if (hsb.b <= 0.9) { hsb.b = (float)
		 * binsCPArray[8]; } else { hsb.b = (float) binsCPArray[9]; } return
		 * hsb; }).mapToInt(hsb -> java.awt.Color.HSBtoRGB(hsb.h, hsb.s,
		 * hsb.b)).toArray();
		 * 
		 */

		time.now(); // equalize pixels
		// int[] outputRGBPixelArray = null;
		newImage.setRGB(0, 0, w, h, outputRGBPixelArray, 0, w);

		time.now(); // setRGB
		return time;
	}

	static Timer colorHistEq_parallel(FJBufferedImage image, FJBufferedImage newImage) {
		Timer time = new Timer(labels);
		/**
		 * IMPLEMENT PARALLEL METHOD
		 */
		ColorModel colorModel = ColorModel.getRGBdefault();
		int w = image.getWidth();
		int h = image.getHeight();
		time.now();
		int[] sourceRGBPixelArray = image.getRGB(0, 0, w, h, new int[w * h], 0, w);
		time.now(); // getRGB

		Map<Object, Long> binsMap = Arrays.stream(sourceRGBPixelArray).parallel() // get
				// array
				// of
				// pixels
				// from
				// image
				// and
				// convert
				// to
				// IntStream
				// convert to stream of HSBPixel
				.mapToObj(pixel -> {
					float[] hsbFloatArrayofPixel = new float[] { 0, 0, 0 };
					java.awt.Color.RGBtoHSB(colorModel.getRed(pixel), colorModel.getGreen(pixel),
							colorModel.getBlue(pixel), hsbFloatArrayofPixel);
					return histInstance.new HSBPixel(hsbFloatArrayofPixel);
				})
				// get histogram's bins map with number of pixels in each bin
				.collect(Collectors.groupingBy(pixel -> (int) (pixel.b * bins), Collectors.counting()));

		time.now(); // convert to HSB and create brightness map

		double noOfPixels = w * h;

		// count of pixels in each histogram bin
		double[] binsArray = binsMap.entrySet().stream().parallel().map(Map.Entry::getValue).mapToDouble(hsb -> hsb).toArray();

		// calculate prefix sum
		Arrays.parallelPrefix(binsArray, (x, y) -> x + y);

		time.now(); // parallel prefix

		// get the cumulative probability array (divide prefix counts by size of
		// pixels)
		final double[] binsCPArray = Arrays.stream(binsArray).parallel().map(x -> (x / noOfPixels)).toArray();

		time.now(); // cumulative probability array

		int[] outputRGBPixelArray = Arrays.stream(sourceRGBPixelArray).parallel().mapToObj(pixel -> {
			float[] hsbFloatArrayofPixel = new float[] { 0, 0, 0 };
			java.awt.Color.RGBtoHSB(colorModel.getRed(pixel), colorModel.getGreen(pixel), colorModel.getBlue(pixel),
					hsbFloatArrayofPixel);
			return histInstance.new HSBPixel(hsbFloatArrayofPixel);
		}).mapToInt(hsb -> java.awt.Color.HSBtoRGB(hsb.h, hsb.s,
				(float) binsCPArray[Math.min((int) (hsb.b * bins), bins - 1)])).toArray();

		time.now(); // equalize pixels

		newImage.setRGB(0, 0, w, h, outputRGBPixelArray, 0, w);

		time.now(); // setRGB
		return time;
	}

}
