package org.r7c.pdf.ui;

/**
 * Converts a rectangle drawn on a rendered PDF page image (screen pixels, origin top-left) into
 * PDF user-space points (origin top-left), the coordinate system {@code SignatureFieldParameters}
 * expects.
 */
public final class CoordinateConverter {

    private CoordinateConverter() {
    }

    /**
     * @param screenX      rectangle left edge, in pixels of the rendered page image
     * @param screenY      rectangle top edge, in pixels of the rendered page image
     * @param screenWidth  rectangle width, in pixels
     * @param screenHeight rectangle height, in pixels
     * @param renderScale  pixels-per-PDF-point the page was rendered at
     * @return {x, y, width, height} in PDF points, origin top-left
     */
    public static float[] screenRectToPdfPoints(double screenX, double screenY,
                                                  double screenWidth, double screenHeight,
                                                  float renderScale) {
        float pdfX = (float) (screenX / renderScale);
        float pdfWidth = (float) (screenWidth / renderScale);
        float pdfHeight = (float) (screenHeight / renderScale);
        float pdfY =  (float) ((screenY) / renderScale);
        return new float[]{pdfX, pdfY, pdfWidth, pdfHeight};
    }
}
