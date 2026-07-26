package org.r7c.pdf.ui;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.swing.JPanel;
import javax.swing.JViewport;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Renders one page of an open PDF at a time and lets the user rubber-band a rectangle on it to mark
 * where a new visible signature field should go. {@link #setSelectionEnabled(boolean)} disables
 * drawing when the user instead picked an existing signature field to sign into.
 */
public class PdfViewerPanel extends JPanel {

    /** Fallback pixels-per-PDF-point, used until a page has actually been fit to a viewport width. */
    private static final float DEFAULT_SCALE = 1.5f;

    private static final int MIN_SELECTION_PX = 5;
    /** Mat around the rendered page, in component pixels; keeps the page off the panel's raw edge. */
    private static final int PAGE_MARGIN = 28;

    private PDDocument document;
    private PDFRenderer renderer;
    private int currentPageIndex;
    private BufferedImage pageImage;
    /** Pixels-per-PDF-point the page is currently rendered at; also fed into {@link CoordinateConverter}. */
    private float scale = DEFAULT_SCALE;

    private boolean selectionEnabled = true;
    private Rectangle selection;
    private Point dragStart;

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
                dragStart = clampToImage(e.getPoint());
                selection = new Rectangle(dragStart);
                repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragStart == null) {
                    return;
                }
                Point current = clampToImage(e.getPoint());
                selection = rectFrom(dragStart, current);
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (dragStart == null) {
                    return;
                }
                dragStart = null;
                if (selection == null || selection.width < MIN_SELECTION_PX || selection.height < MIN_SELECTION_PX) {
                    selection = null;
                }
                repaint();
                onChange.run();
            }
        };
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
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
        renderCurrentPage();
    }

    /** Recomputes the render scale so the current page's width fills the enclosing viewport, and re-renders. */
    public void fitWidth() throws IOException {
        if (document == null) {
            return;
        }
        float pageWidthPt = document.getPage(currentPageIndex).getMediaBox().getWidth();
        int availablePx = getViewportWidth() - 2 * PAGE_MARGIN;
        if (availablePx > 0 && pageWidthPt > 0) {
            scale = availablePx / pageWidthPt;
        }
        renderCurrentPage();
    }

    /** Visible width of the enclosing {@link JViewport}, if any, else this panel's own width. */
    private int getViewportWidth() {
        Container parent = getParent();
        if (parent instanceof JViewport viewport) {
            return viewport.getExtentSize().width;
        }
        return getWidth();
    }

    public void setSelectionEnabled(boolean selectionEnabled) {
        this.selectionEnabled = selectionEnabled;
        if (!selectionEnabled) {
            clearSelection();
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

    private void renderCurrentPage() throws IOException {
        pageImage = renderer.renderImage(currentPageIndex, scale);
        selection = null;
        setPreferredSize(new Dimension(
                pageImage.getWidth() + 2 * PAGE_MARGIN, pageImage.getHeight() + 2 * PAGE_MARGIN));
        revalidate();
        repaint();
        onChange.run();
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
        }
        g2.dispose();
    }
}
