package li.cil.oc.client.renderer.font;

import java.nio.ByteBuffer;

/**
 * Common interface for classes providing glyph data in a format that can be
 * rendered using the {@link li.cil.oc.client.renderer.font.DynamicFontRenderer}.
 */
public interface IGlyphProvider {
    /**
     * Called when the resource manager is reloaded.
     * <p/>
     * This should usually also be called from the implementation's constructor.
     */
    public void initialize();

    /**
     * Get a byte array of RGBA data describing the specified char.
     * <p/>
     * This is only called once for each char per resource reload cycle (i.e.
     * it may called multiple times, but only if {@link #initialize()} was
     * called in-between). This means implementations may be relatively
     * inefficient (be reasonable) in generating the RGBA data.
     * <p/>
     * The returned buffer is expected to be of a format so that it can be
     * directly passed on to <code>glTexSubImage2D</code>, meaning a byte array
     * with 4 byte per pixel, row by row.
     * <p/>
     * <b>Important</b>: remember to rewind the buffer, if necessary.
     *
     * @param charCode the char to get the render glyph data for.
     * @return the RGBA byte array representing the char.
     * @see li.cil.oc.client.renderer.font.FontParserUnifont#getGlyph(int) See the Unifont parser for a reference implementation.
     */
    public ByteBuffer getGlyph(int charCode);

    /**
     * Get the single-width glyph width for this provider, in pixels.
     * <p/>
     * Each glyph provided is expected to have the same width multiplier; i.e.
     * a glyphs actual width (in pixels) is expected to be this value times
     * {@link li.cil.oc.util.FontUtil#wcwidth(int)} (for a specific char).
     */
    public int getGlyphWidth();

    /**
     * Get the glyph height for this provider, in pixels.
     * <p/>
     * Each glyph provided is expected to have the same height.
     */
    public int getGlyphHeight();
}
