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
