package org.r7c.pdf.ui;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.swing.JPanel;
import javax.swing.JViewport;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Renders one page of an open PDF at a time and lets the user mark where a new visible signature
 * field should go: rubber-band a brand-new rectangle, drag an existing one to move it, or drag one
 * of its edge/corner handles to resize it. A default rectangle is placed in the page's top-right
 * corner as soon as a page is shown, so signing works without the user having to draw anything.
 * {@link #setSelectionEnabled(boolean)} disables all of this when the user instead picked an
 * existing signature field to sign into.
 *
 * <p>Zoom is either automatic ({@link #fitWidth()} / {@link #fitPage()}, which keep re-fitting as
 * the enclosing viewport is resized) or manual ({@link #zoomIn()} / {@link #zoomOut()}, which turn
 * auto-fit off until fit mode is explicitly picked again) — the usual behaviour in image/PDF viewers.
 */
public class PdfViewerPanel extends JPanel {

    /** Fallback pixels-per-PDF-point, used until a page has actually been fit to a viewport width. */
    private static final float DEFAULT_SCALE = 1.5f;

    private static final float MIN_SCALE = 0.2f;
    private static final float MAX_SCALE = 6f;
    private static final float ZOOM_STEP = 1.15f;

    private static final int MIN_SELECTION_PX = 5;
    /** Mat around the rendered page, in component pixels; keeps the page off the panel's raw edge. */
    private static final int PAGE_MARGIN = 28;

    /** Size, in PDF points, of the rectangle placed by default in the page's top-right corner. */
    private static final float DEFAULT_FIELD_WIDTH_PT = 200f;
    private static final float DEFAULT_FIELD_HEIGHT_PT = 80f;
    private static final float DEFAULT_FIELD_MARGIN_PT = 20f;

    /** Half-width, in screen pixels, of the hit/paint area for a resize handle; constant across zoom. */
    private static final int HANDLE_HALF_PX = 5;

    private enum DragMode { NONE, DRAW, MOVE, RESIZE }

    /** Which auto-zoom behaviour, if any, is currently active; {@code NONE} once the user zooms manually. */
    private enum FitMode { NONE, WIDTH, PAGE }

    /** The eight resize handles around a selection rectangle, named by compass position. */
    private enum Handle {
        NW(true, false, true, false), N(false, false, true, false), NE(false, true, true, false),
        W(true, false, false, false), E(false, true, false, false),
        SW(true, false, false, true), S(false, false, false, true), SE(false, true, false, true);

        final boolean left;
        final boolean right;
        final boolean top;
        final boolean bottom;

        Handle(boolean left, boolean right, boolean top, boolean bottom) {
            this.left = left;
            this.right = right;
            this.top = top;
            this.bottom = bottom;
        }
    }

    private PDDocument document;
    private PDFRenderer renderer;
    private int currentPageIndex;
    private BufferedImage pageImage;
    /** Pixels-per-PDF-point the page is currently rendered at; also fed into {@link CoordinateConverter}. */
    private float scale = DEFAULT_SCALE;
    private FitMode fitMode = FitMode.NONE;

    private boolean selectionEnabled = true;
    private Rectangle selection;

    private DragMode dragMode = DragMode.NONE;
    private Handle activeHandle;
    private Point dragStart;
    private Rectangle dragOriginalRect;

    private final ComponentAdapter viewportResizeListener = new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent e) {
            onViewportResized();
        }
    };

    private Runnable onChange = () -> {
    };

    public PdfViewerPanel() {
        setBackground(UiTheme.SURFACE);
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!selectionEnabled || pageImage == null) {
                    return;
                }
                Point p = clampToImage(e.getPoint());
                Handle handle = hitTestHandle(p);
                if (handle != null) {
                    dragMode = DragMode.RESIZE;
                    activeHandle = handle;
                    dragOriginalRect = new Rectangle(selection);
                    dragStart = p;
                } else if (selection != null && selection.contains(p)) {
                    dragMode = DragMode.MOVE;
                    dragOriginalRect = new Rectangle(selection);
                    dragStart = p;
                } else {
                    dragMode = DragMode.DRAW;
                    dragStart = p;
                    selection = new Rectangle(p);
                }
                repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragMode == DragMode.NONE) {
                    return;
                }
                Point current = clampToImage(e.getPoint());
                switch (dragMode) {
                    case DRAW -> selection = rectFrom(dragStart, current);
                    case MOVE -> selection = moveRect(dragOriginalRect,
                            current.x - dragStart.x, current.y - dragStart.y);
                    case RESIZE -> selection = resizeRect(dragOriginalRect, activeHandle,
                            current.x - dragStart.x, current.y - dragStart.y);
                    case NONE -> {
                    }
                }
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (dragMode == DragMode.NONE) {
                    return;
                }
                if (dragMode == DragMode.DRAW && (selection == null
                        || selection.width < MIN_SELECTION_PX || selection.height < MIN_SELECTION_PX)) {
                    selection = null;
                }
                dragMode = DragMode.NONE;
                activeHandle = null;
                dragStart = null;
                dragOriginalRect = null;
                repaint();
                onChange.run();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                updateCursor(e.getPoint());
            }
        };
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    /** Hooks the enclosing {@link JViewport}'s resize events so auto-fit can re-apply on window resize. */
    @Override
    public void addNotify() {
        super.addNotify();
        if (getParent() instanceof JViewport viewport) {
            viewport.addComponentListener(viewportResizeListener);
        }
    }

    @Override
    public void removeNotify() {
        if (getParent() instanceof JViewport viewport) {
            viewport.removeComponentListener(viewportResizeListener);
        }
        super.removeNotify();
    }

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    public void open(File pdfFile) throws IOException {
        close();
        document = Loader.loadPDF(pdfFile);
        renderer = new PDFRenderer(document);
        currentPageIndex = 0;
        fitWidth();
    }

    public void close() {
        if (document != null) {
            try {
                document.close();
            } catch (IOException ignored) {
                // best-effort close of a document we're discarding
            }
            document = null;
            renderer = null;
            pageImage = null;
            selection = null;
        }
    }

    public boolean isOpen() {
        return document != null;
    }

    public int getPageCount() {
        return document == null ? 0 : document.getNumberOfPages();
    }

    /** 1-based, matching {@code SignatureFieldParameters.setPage}. */
    public int getCurrentPageNumber() {
        return currentPageIndex + 1;
    }

    public void nextPage() throws IOException {
        showPage(currentPageIndex + 1);
    }

    public void prevPage() throws IOException {
        showPage(currentPageIndex - 1);
    }

    public void showPage(int zeroBasedIndex) throws IOException {
        if (document == null || zeroBasedIndex < 0 || zeroBasedIndex >= document.getNumberOfPages()) {
            return;
        }
        currentPageIndex = zeroBasedIndex;
        if (fitMode == FitMode.WIDTH) {
            scale = computeFitWidthScale();
        } else if (fitMode == FitMode.PAGE) {
            scale = computeFitPageScale();
        }
        renderPage(true);
    }

    /**
     * Fits the current page's width to the enclosing viewport and keeps re-fitting as the viewport
     * is resized, until {@link #zoomIn()}/{@link #zoomOut()} or {@link #fitPage()} takes over.
     */
    public void fitWidth() throws IOException {
        if (document == null) {
            return;
        }
        fitMode = FitMode.WIDTH;
        applyScale(computeFitWidthScale());
    }

    /**
     * Fits the whole current page (width and height) inside the enclosing viewport, and keeps
     * re-fitting as the viewport is resized, same as {@link #fitWidth()}.
     */
    public void fitPage() throws IOException {
        if (document == null) {
            return;
        }
        fitMode = FitMode.PAGE;
        applyScale(computeFitPageScale());
    }

    /** Zooms in one step; switches off auto-fit (window resizes no longer change the zoom level). */
    public void zoomIn() throws IOException {
        if (document == null) {
            return;
        }
        fitMode = FitMode.NONE;
        applyScale(clampScale(scale * ZOOM_STEP));
    }

    /** Zooms out one step; switches off auto-fit (window resizes no longer change the zoom level). */
    public void zoomOut() throws IOException {
        if (document == null) {
            return;
        }
        fitMode = FitMode.NONE;
        applyScale(clampScale(scale / ZOOM_STEP));
    }

    private void onViewportResized() {
        if (document == null || fitMode == FitMode.NONE) {
            return;
        }
        try {
            applyScale(fitMode == FitMode.WIDTH ? computeFitWidthScale() : computeFitPageScale());
        } catch (IOException e) {
            // best-effort re-fit on resize; leave the page at its last successfully rendered scale
        }
    }

    private float computeFitWidthScale() {
        float pageWidthPt = document.getPage(currentPageIndex).getMediaBox().getWidth();
        int availablePx = getViewportWidth() - 2 * PAGE_MARGIN;
        return availablePx > 0 && pageWidthPt > 0 ? availablePx / pageWidthPt : scale;
    }

    private float computeFitPageScale() {
        PDRectangle mediaBox = document.getPage(currentPageIndex).getMediaBox();
        float pageWidthPt = mediaBox.getWidth();
        float pageHeightPt = mediaBox.getHeight();
        int availableWidthPx = getViewportWidth() - 2 * PAGE_MARGIN;
        int availableHeightPx = getViewportHeight() - 2 * PAGE_MARGIN;
        if (availableWidthPx > 0 && availableHeightPx > 0 && pageWidthPt > 0 && pageHeightPt > 0) {
            return Math.min(availableWidthPx / pageWidthPt, availableHeightPx / pageHeightPt);
        }
        return scale;
    }

    private static float clampScale(float value) {
        return Math.max(MIN_SCALE, Math.min(value, MAX_SCALE));
    }

    /** Visible width of the enclosing {@link JViewport}, if any, else this panel's own width. */
    private int getViewportWidth() {
        Container parent = getParent();
        if (parent instanceof JViewport viewport) {
            return viewport.getExtentSize().width;
        }
        return getWidth();
    }

    /** Visible height of the enclosing {@link JViewport}, if any, else this panel's own height. */
    private int getViewportHeight() {
        Container parent = getParent();
        if (parent instanceof JViewport viewport) {
            return viewport.getExtentSize().height;
        }
        return getHeight();
    }

    public void setSelectionEnabled(boolean selectionEnabled) {
        this.selectionEnabled = selectionEnabled;
        if (!selectionEnabled) {
            clearSelection();
        } else if (selection == null) {
            placeDefaultSelection();
            repaint();
        }
    }

    public void clearSelection() {
        selection = null;
        repaint();
    }

    public boolean hasSelection() {
        return selection != null;
    }

    /** @return {x, y, width, height} in PDF points (origin bottom-left), or null if nothing is selected. */
    public float[] getSelectionPdfPoints() {
        if (selection == null || document == null) {
            return null;
        }
        return CoordinateConverter.screenRectToPdfPoints(
                selection.x, selection.y, selection.width, selection.height, scale);
    }

    /**
     * Re-renders the page at the current {@link #scale}, rescaling any existing selection to match
     * (so zooming/resizing doesn't discard where the user already placed the signature box), unless
     * {@code resetSelection} is set (new document/page: any previous selection belongs to a different
     * page and must not carry over).
     */
    private void applyScale(float newScale) throws IOException {
        if (document == null) {
            return;
        }
        float oldScale = scale;
        scale = newScale;
        if (selection != null && oldScale > 0 && Float.compare(oldScale, newScale) != 0) {
            float ratio = newScale / oldScale;
            selection = new Rectangle(
                    Math.round(selection.x * ratio), Math.round(selection.y * ratio),
                    Math.round(selection.width * ratio), Math.round(selection.height * ratio));
        }
        renderPage(false);
    }

    private void renderPage(boolean resetSelection) throws IOException {
        pageImage = renderer.renderImage(currentPageIndex, scale);
        if (selectionEnabled) {
            if (resetSelection || selection == null) {
                placeDefaultSelection();
            } else {
                clampSelectionToPage();
            }
        } else {
            selection = null;
        }
        setPreferredSize(new Dimension(
                pageImage.getWidth() + 2 * PAGE_MARGIN, pageImage.getHeight() + 2 * PAGE_MARGIN));
        revalidate();
        repaint();
        onChange.run();
    }

    /** Places a default-sized rectangle in the page's top-right corner, sized/positioned in PDF points. */
    private void placeDefaultSelection() {
        if (pageImage == null) {
            return;
        }
        int pageWidth = pageImage.getWidth();
        int pageHeight = pageImage.getHeight();
        int margin = Math.min(Math.round(DEFAULT_FIELD_MARGIN_PT * scale),
                Math.max(Math.min(pageWidth, pageHeight) / 4, 0));
        int width = clamp(Math.round(DEFAULT_FIELD_WIDTH_PT * scale), MIN_SELECTION_PX,
                Math.max(pageWidth - 2 * margin, MIN_SELECTION_PX));
        int height = clamp(Math.round(DEFAULT_FIELD_HEIGHT_PT * scale), MIN_SELECTION_PX,
                Math.max(pageHeight - 2 * margin, MIN_SELECTION_PX));
        int x = Math.max(pageWidth - margin - width, 0);
        int y = margin;
        selection = new Rectangle(x, y, width, height);
    }

    /** Keeps a rescaled selection within the (possibly resized) page image after a zoom/fit change. */
    private void clampSelectionToPage() {
        if (selection == null || pageImage == null) {
            return;
        }
        int width = clamp(selection.width, MIN_SELECTION_PX, pageImage.getWidth());
        int height = clamp(selection.height, MIN_SELECTION_PX, pageImage.getHeight());
        int x = clamp(selection.x, 0, Math.max(pageImage.getWidth() - width, 0));
        int y = clamp(selection.y, 0, Math.max(pageImage.getHeight() - height, 0));
        selection = new Rectangle(x, y, width, height);
    }

    /** Component coordinates (which include the page mat) to page-image pixel coordinates, clamped. */
    private Point clampToImage(Point componentPoint) {
        int x = Math.max(0, Math.min(componentPoint.x - PAGE_MARGIN, pageImage.getWidth()));
        int y = Math.max(0, Math.min(componentPoint.y - PAGE_MARGIN, pageImage.getHeight()));
        return new Point(x, y);
    }

    private static Rectangle rectFrom(Point a, Point b) {
        int x = Math.min(a.x, b.x);
        int y = Math.min(a.y, b.y);
        int w = Math.abs(a.x - b.x);
        int h = Math.abs(a.y - b.y);
        return new Rectangle(x, y, w, h);
    }

    /** Translates {@code original} by (dx, dy), clamped so it stays fully within the page image. */
    private Rectangle moveRect(Rectangle original, int dx, int dy) {
        int maxX = Math.max(pageImage.getWidth() - original.width, 0);
        int maxY = Math.max(pageImage.getHeight() - original.height, 0);
        int x = clamp(original.x + dx, 0, maxX);
        int y = clamp(original.y + dy, 0, maxY);
        return new Rectangle(x, y, original.width, original.height);
    }

    /** Drags whichever edge(s) {@code handle} represents by (dx, dy), clamped to the page and a minimum size. */
    private Rectangle resizeRect(Rectangle original, Handle handle, int dx, int dy) {
        int left = original.x;
        int top = original.y;
        int right = original.x + original.width;
        int bottom = original.y + original.height;

        if (handle.left) {
            left = clamp(left + dx, 0, right - MIN_SELECTION_PX);
        }
        if (handle.right) {
            right = clamp(right + dx, left + MIN_SELECTION_PX, pageImage.getWidth());
        }
        if (handle.top) {
            top = clamp(top + dy, 0, bottom - MIN_SELECTION_PX);
        }
        if (handle.bottom) {
            bottom = clamp(bottom + dy, top + MIN_SELECTION_PX, pageImage.getHeight());
        }
        return new Rectangle(left, top, right - left, bottom - top);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    /** @return the handle under image-relative point {@code p}, or null if none/no selection. */
    private Handle hitTestHandle(Point p) {
        if (selection == null) {
            return null;
        }
        int midX = selection.x + selection.width / 2;
        int midY = selection.y + selection.height / 2;
        int left = selection.x;
        int right = selection.x + selection.width;
        int top = selection.y;
        int bottom = selection.y + selection.height;

        if (near(p, left, top)) return Handle.NW;
        if (near(p, right, top)) return Handle.NE;
        if (near(p, left, bottom)) return Handle.SW;
        if (near(p, right, bottom)) return Handle.SE;
        if (near(p, midX, top)) return Handle.N;
        if (near(p, midX, bottom)) return Handle.S;
        if (near(p, left, midY)) return Handle.W;
        if (near(p, right, midY)) return Handle.E;
        return null;
    }

    private static boolean near(Point p, int cx, int cy) {
        return Math.abs(p.x - cx) <= HANDLE_HALF_PX && Math.abs(p.y - cy) <= HANDLE_HALF_PX;
    }

    private void updateCursor(Point componentPoint) {
        if (!selectionEnabled || pageImage == null) {
            setCursor(Cursor.getDefaultCursor());
            return;
        }
        Point p = clampToImage(componentPoint);
        Handle handle = hitTestHandle(p);
        if (handle != null) {
            setCursor(Cursor.getPredefinedCursor(cursorTypeFor(handle)));
        } else if (selection != null && selection.contains(p)) {
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        } else {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    private static int cursorTypeFor(Handle handle) {
        return switch (handle) {
            case NW -> Cursor.NW_RESIZE_CURSOR;
            case N -> Cursor.N_RESIZE_CURSOR;
            case NE -> Cursor.NE_RESIZE_CURSOR;
            case W -> Cursor.W_RESIZE_CURSOR;
            case E -> Cursor.E_RESIZE_CURSOR;
            case SW -> Cursor.SW_RESIZE_CURSOR;
            case S -> Cursor.S_RESIZE_CURSOR;
            case SE -> Cursor.SE_RESIZE_CURSOR;
        };
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (pageImage == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = pageImage.getWidth();
        int h = pageImage.getHeight();

        // Soft drop shadow: a few translucent rects, offset and shrinking, behind the page.
        for (int i = 6; i >= 1; i--) {
            g2.setColor(new Color(0, 0, 0, 5));
            g2.fillRect(PAGE_MARGIN - i + 2, PAGE_MARGIN - i + 4, w + 2 * i, h + 2 * i);
        }

        g2.drawImage(pageImage, PAGE_MARGIN, PAGE_MARGIN, null);
        g2.setColor(UiTheme.BORDER);
        g2.drawRect(PAGE_MARGIN, PAGE_MARGIN, w - 1, h - 1);

        if (selection != null) {
            int sx = selection.x + PAGE_MARGIN;
            int sy = selection.y + PAGE_MARGIN;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.18f));
            g2.setColor(UiTheme.ACCENT);
            g2.fillRect(sx, sy, selection.width, selection.height);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            g2.setStroke(new BasicStroke(1.5f));
            g2.setColor(UiTheme.ACCENT);
            g2.drawRect(sx, sy, selection.width, selection.height);
            if (selectionEnabled) {
                drawHandles(g2, sx, sy);
            }
        }
        g2.dispose();
    }

    private void drawHandles(Graphics2D g2, int sx, int sy) {
        int midX = sx + selection.width / 2;
        int midY = sy + selection.height / 2;
        int right = sx + selection.width;
        int bottom = sy + selection.height;
        int[][] points = {
                {sx, sy}, {midX, sy}, {right, sy},
                {sx, midY}, {right, midY},
                {sx, bottom}, {midX, bottom}, {right, bottom},
        };
        g2.setStroke(new BasicStroke(1f));
        for (int[] pt : points) {
            int hx = pt[0] - HANDLE_HALF_PX;
            int hy = pt[1] - HANDLE_HALF_PX;
            int size = HANDLE_HALF_PX * 2;
            g2.setColor(Color.WHITE);
            g2.fillRect(hx, hy, size, size);
            g2.setColor(UiTheme.ACCENT);
            g2.drawRect(hx, hy, size, size);
        }
    }
}
