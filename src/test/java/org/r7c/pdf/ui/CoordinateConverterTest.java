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

/*
 * sign-pdf
 *
 * Copyright (C) 2025 - 2026  Owner of the domain org.r7c
 *
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
 */
package org.r7c.pdf.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CoordinateConverterTest {

    private static final float DELTA = 0.01f;

    @Test
    void screenRectToPdfPoints_scale15() throws Exception {
        float scale = 1.5f;
        // A rectangle drawn flush against the top-left of the rendered image.
        float[] rect = CoordinateConverter.screenRectToPdfPoints(0, 60, 150, 60, scale);

        assertEquals(0f, rect[0], DELTA);
        assertEquals(40f, rect[1], DELTA); // 60px / 1.5 = 40pt tall, f
        assertEquals(100f, rect[2], DELTA); // 150px / 1.5
        assertEquals(40f, rect[3], DELTA);  // 60px / 1.5
    }

    @Test
    void screenRectToPdfPoints_scale2() {
        float scale = 2f;
        float[] rect = CoordinateConverter.screenRectToPdfPoints(20, 1400, 60, 200, scale);

        assertEquals(10f, rect[0], DELTA);
        assertEquals(700f, rect[1], DELTA);
        assertEquals(30f, rect[2], DELTA);
        assertEquals(100f, rect[3], DELTA);
    }
}
