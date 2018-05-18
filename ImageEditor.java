/* ImageEditor - Nadav Koller
   This class defines useful methods for interpreting and
   working with images.
*/

import java.awt.image.BufferedImage;
import java.io.IOException;


public class ImageEditor {

    static final byte A = 0;
    static final byte R = 1;
    static final byte G = 2;
    static final byte B = 3;
    static final int transparentARGB = 0;


    // Converts a BufferedImage to a 2-D array of ARGB values.

    static int[][] imageToARGB (BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        int[][] result = new int[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result[y][x] = img.getRGB(x,y);
            }
        }

        return result;
    }


    // Converts a 2-D array of ARGB values to a BufferedImage.

    static BufferedImage ARGBToImage (int[][] argb) {
        BufferedImage result = new BufferedImage(argb[0].length, argb.length, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < argb.length; y++) {
            for (int x = 0; x < argb[0].length; x++) {
                result.setRGB(x, y, argb[y][x]);
            }
        }

        return result;
    }


    // Converts individual transparency, red, green, and blue
    // colour values into a 32-bit ARGB value.

    static int byteValue (int a, int r, int g, int b) {
        return (a << 24) + (r << 16) + (g << 8) + b;
    }


    // Converts an ARGB values into an array of colour values
    // representing transparency, red, green, and blue.

    static int[] getColours (int argb) {
        int[] values =
                {(argb >> 24) & 0xFF,
                (argb >> 16) & 0xFF,
                (argb >> 8) & 0xFF,
                argb & 0xFF};
        return values;
    }


    // Returns the specific colour value 'colour' from
    // an ARGB value.

    static int getColour (byte colour, int argb) {
        return (argb >> (8 * (3 - colour))) & 0xFF;
    }


    // Changes the transparency of an ARGB value to 'a'.

    static int setAlpha (int a, int argb) {
        return (a << 24) + (argb & 0x00FFFFFF);
    }


    // Prints the values of an int array, useful for debugging.

    static void println (int[] lst) {
        for (Integer i : lst) {
            System.out.print(i + " ");
        }
        System.out.println();
    }


    // Prints a grid of values from a 2-D int array, useful
    // for debugging.

    static void println (int[][] lst) {
        for (int[] i : lst) {
            println(i);
        }
        System.out.println();
    }
}
