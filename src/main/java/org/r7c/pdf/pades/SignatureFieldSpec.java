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
package org.r7c.pdf.pades;

/**
 * Where the visible signature goes: either a brand-new signature field (page + rectangle, in PDF
 * points) or an existing, still-empty signature field already present on the PDF, identified by name
 * (see {@link PadesUtils#listAvailableSignatureFields(java.io.File)}).
 */
public final class SignatureFieldSpec {

    private final String existingFieldId;
    private final int page;
    private final float originX;
    private final float originY;
    private final float width;
    private final float height;

    private SignatureFieldSpec(String existingFieldId, int page,
                                float originX, float originY, float width, float height) {
        this.existingFieldId = existingFieldId;
        this.page = page;
        this.originX = originX;
        this.originY = originY;
        this.width = width;
        this.height = height;
    }

    public static SignatureFieldSpec existingField(String fieldId) {
        return new SignatureFieldSpec(fieldId, 0, 0, 0, 0, 0);
    }

    public static SignatureFieldSpec newField(int page, float originX, float originY, float width, float height) {
        return new SignatureFieldSpec(null, page, originX, originY, width, height);
    }

    public boolean isExistingField() {
        return existingFieldId != null;
    }

    public String getExistingFieldId() {
        return existingFieldId;
    }

    public int getPage() {
        return page;
    }

    public float getOriginX() {
        return originX;
    }

    public float getOriginY() {
        return originY;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }
}
