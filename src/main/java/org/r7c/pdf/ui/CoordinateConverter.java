/*-
 * #START_LICENSE#
 * sign-pdf
 * %%
 * Copyright (C) 2017 - 2026 org.r7c | sign-pdf
 * %%
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 * [PROJECT_HOME]\license\eupl-1.2\license.txt or https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 * #END_LICENSE#
 */
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
