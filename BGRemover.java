/* BGRemover - Nadav Koller
   This class contains the methods necessary to intelligently erase
   the background from an image using an edge detection algorithm
   and a transparency algorithm to create accurate anti-aliasing.
*/

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class BGRemover extends ImageEditor {

    static final String directory = "C:/Users/nadav/Desktop/";
    static final String fileName = "text.png";
    private final byte closeEnoughBuffer = 6;
    private int[][] image;
    private int background;
    private int backgroundMark;
    private int objectMark;


    // Returns the colour in 'original' that is most distinct from the
    // background colour.
    private byte distinctColour (int original) {
        int[] originalColours = getColours(original);
        int[] backgroundColours = getColours(background);

        if (Math.abs(originalColours[R] - backgroundColours[R]) > Math.abs(originalColours[G] - backgroundColours[G])) {
            if (Math.abs(originalColours[R] - backgroundColours[R]) > Math.abs(originalColours[B] - backgroundColours[B]))
                return R;
            else
                return B;
        }
        else {
            if (Math.abs(originalColours[G] - backgroundColours[G]) > Math.abs(originalColours[B] - backgroundColours[B]))
                return G;
            else
                return B;
        }
    }
    
    // Returns the transparency at which 'original', when merged with
    // the background colour, will appear like 'current'.
    private int split (int current, int original) {
        byte comparisonColour = distinctColour(original);
        int currColour = getColour(comparisonColour, current);
        int origColour = getColour(comparisonColour, original);
        int backColour = getColour(comparisonColour, background);

        long alpha = Math.round(255 * (1.0 * (backColour - currColour) / (backColour - origColour)));

        if (alpha < 255)
            return setAlpha((int)alpha,original);
        return 0;
    }
    
    
    // Determines whether 'current' is similar enough to the background
    // colour as specified by the 'closeEnoughBuffer'.
    private boolean closeEnough (int current) {
        return Math.max (Math.max(
                Math.abs(getColour(R, current) - getColour(R, background)),
                Math.abs(getColour(G, current) - getColour(G, background))),
                Math.abs(getColour(B, current) - getColour(B, background))) <= closeEnoughBuffer;
    }
    
    // Determines if the colour 'a' appears to be a more transparent
    // version of 'b' on top of the background colour.
    private boolean isGradient (int a, int b) {
        return split(a, b) != 0;
    }
    
    // Determines if the rate of change of transparency between four
    // pixels is increasing.
    private boolean isConcaveUp (int a, int b, int c, int d) {
        int alpha1 = getColour(A, split(a, d));
        int alpha2 = getColour(A, split(b, d));
        int alpha3 = getColour(A, split(c, d));

        return alpha3 - alpha2 >= alpha2 - alpha1;
    }

    // Determines if 'original1' appears more transparent on the background
    // colour than 'original2'.
    private boolean isMoreTransparent (int original1, int original2, int current) {
        return getColour(A, split(current, original1)) < getColour(A, split(current, original2));
    }

    
    // Returns a 2-D array with each element denoting whether it represents
    // the background, the object, or the edge of an object as determined
    // by scanning the image row-by-row from left to right. If it is a pixel
    // inside the edge of an object, it stores the pixel that it appears to
    // be a "more transparent" version of.
    private int[][] markGradientsLR () {
        int[][] result = new int[image.length][image[0].length];

        for (int y = 0; y < image.length; y++) {
            for (int x = 0; x < image[0].length; x++) {
                if (closeEnough(image[y][x]))
                    result[y][x] = backgroundMark;
                else if (x == 0 || result[y][x-1] == backgroundMark) {
                    int original = -1;
                    int i;

                    for (i = x; i + 1 < image[0].length && isGradient(image[y][i], image[y][i+1])
                            && (i < x + 3 || isConcaveUp(image[y][i-3], image[y][i-2], image[y][i-1], image[y][i]));
                         i++)
                        original = i + 1;

                    for (x = x; x < i; x++) {
                        result[y][x] = original;
                    }
                    result[y][x] = objectMark;
                }
                else
                    result[y][x] = objectMark;
            }
        }

        return result;
    }

    // Similar to markGradientsLR but scans from right to left.
    private int[][] markGradientsRL (int[][] marksSoFar) {
        for (int y = image.length - 1; y >= 0; y--) {
            for (int x = image[0].length - 1; x >= 0; x--) {
                if (marksSoFar[y][x] != backgroundMark) {
                    if (x == image[0].length - 1 || marksSoFar[y][x+1] == backgroundMark) {
                        int original = -1;
                        int i = -1;

                        for (i = x; i > 0 && isGradient(image[y][i], image[y][i - 1])
                                && (i > x - 3 || isConcaveUp(image[y][i+3], image[y][i+2], image[y][i+1], image[y][i]));
                             i--)
                            original = i - 1;

                        for (x = x; x > i; x--) {
                            if (marksSoFar[y][x] == objectMark ||
                                    marksSoFar[y][x] >= 0 && isMoreTransparent(image[y][original], image[y][marksSoFar[y][x]], image[y][x]) ||
                                    marksSoFar[y][x] < 0 && isMoreTransparent(image[y][original], image[marksSoFar[y][x] * -1 - 1][x], image[y][x]))
                                marksSoFar[y][x] = original;
                        }
                    }
                }
            }
        }

        return marksSoFar;
    }

    // Similar to markGradientsLR but scans from top to bottom.
    private int[][] markGradientsTB (int[][] marksSoFar) {
        for (int x = 0; x < image[0].length; x++) {
            for (int y = 0; y < image.length; y++) {
                if (marksSoFar[y][x] != backgroundMark) {
                    if (y == 0 || marksSoFar[y-1][x] == backgroundMark) {
                        int original = -1;
                        int i;

                        for (i = y; i + 1 < image.length && isGradient(image[i][x], image[i+1][x])
                                && (i < y + 3 || isConcaveUp(image[i-3][x], image[i-2][x], image[i-1][x], image[i][x]));
                             i++)
                            original = i + 1;

                        for (y = y; y < i; y++) {
                            if (marksSoFar[y][x] == objectMark ||
                                    marksSoFar[y][x] >= 0 && isMoreTransparent(image[original][x], image[y][marksSoFar[y][x]], image[y][x]) ||
                                    marksSoFar[y][x] < 0 && isMoreTransparent(image[original][x], image[marksSoFar[y][x]*-1 - 1][x], image[y][x]))
                                marksSoFar[y][x] = (original + 1) * -1;
                        }
                    }
                }
            }
        }

        return marksSoFar;
    }

    // Similar to markGradientsLR but scans from bottom to top.
    private int[][] markGradientsBT (int[][] marksSoFar) {
        for (int x = image[0].length - 1; x >= 0; x--) {
            for (int y = image.length - 1; y >= 0; y--) {
                if (marksSoFar[y][x] != backgroundMark) {
                    if (y == image.length - 1 || marksSoFar[y+1][x] == backgroundMark) {
                        int original = -1;
                        int i;

                        for (i = y; i > 0 && isGradient(image[i][x], image[i-1][x])
                                && (i > y - 3 || isConcaveUp(image[i+3][x], image[i+2][x], image[i+1][x], image[i][x]));
                             i--)
                            original = i - 1;

                        for (y = y; y > i; y--) {
                            if (marksSoFar[y][x] == objectMark ||
                                    marksSoFar[y][x] >= 0 && isMoreTransparent(image[original][x], image[y][marksSoFar[y][x]], image[y][x]) ||
                                    marksSoFar[y][x] < 0 && isMoreTransparent(image[original][x], image[marksSoFar[y][x]*-1 - 1][x], image[y][x]))
                                marksSoFar[y][x] = (original + 1) * -1;
                        }
                    }
                }
            }
        }

        return marksSoFar;
    }

    // Returns the final 2-D "gradient" array that results
    // from scanning the image in every direction.
    private int[][] markGradients () {
        int[][] markedImage = markGradientsLR();
        markedImage = markGradientsRL(markedImage);
        markedImage = markGradientsTB(markedImage);
        markedImage = markGradientsBT(markedImage);

        return markedImage;
    }


    // Builds the resultant image by following the instructions
    // laid out in the "gradient" array. That is, changing each
    // edge pixel to be a more transparent version of the pixel
    // denoted in the "gradient" array, and completely erasing
    // any background pixels.
    private int[][] createImage (int[][] map) {
        int[][] result = new int[image.length][image[0].length];

        for (int y = 0; y < result.length; y++) {
            for (int x = 0; x < result[0].length; x++) {
                if (map[y][x] == backgroundMark)
                    result[y][x] = transparentARGB;
                else if (map[y][x] == objectMark)
                    result[y][x] = image[y][x];
                else if (map[y][x] < 0)
                    result[y][x] = split(image[y][x], image[map[y][x]*-1 - 1][x]);
                else
                    result[y][x] = split(image[y][x], image[y][map[y][x]]);
            }
        }

        return result;
    }
    
    // Returns the final result of the process.
    public BufferedImage removeBackground () {
        return ARGBToImage(createImage(markGradients()));
    }

    
    // Initializes the BGRemover object.
    public BGRemover (BufferedImage img, int bg) {
        image = imageToARGB(img);
        background = bg;
        backgroundMark = image.length;
        objectMark = backgroundMark + 1;
    }

    // Creates the BufferedImage from the desired file, 
    // outputs the result as "output.png".
    public static void main (String[] args) throws IOException {
        BGRemover b = new BGRemover(
                ImageIO.read(new File(directory + fileName)),
                byteValue(255,255,255,255));

        File f = new File(directory + "output.png");
        ImageIO.write(b.removeBackground(), "png", f);
    }
}