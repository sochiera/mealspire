package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HtmlTextExtractorTest {

    @Test
    public void stripsTags() {
        String text = HtmlTextExtractor.toPlainText("<h1>Bigos</h1><p>Pyszny bigos.</p>");
        assertTrue(text.contains("Bigos"));
        assertTrue(text.contains("Pyszny bigos."));
        assertFalse(text.contains("<"));
    }

    @Test
    public void removesScriptAndStyle() {
        String html = "<style>.a{color:red}</style><script>var x=1;</script><p>Treść</p>";
        String text = HtmlTextExtractor.toPlainText(html);
        assertTrue(text.contains("Treść"));
        assertFalse(text.contains("color:red"));
        assertFalse(text.contains("var x"));
    }

    @Test
    public void decodesCommonEntities() {
        assertEquals("sos & dodatki", HtmlTextExtractor.toPlainText("sos &amp; dodatki"));
        assertTrue(HtmlTextExtractor.toPlainText("a &lt;b&gt; c").contains("<b>"));
        assertTrue(HtmlTextExtractor.toPlainText("x&nbsp;y").contains("x y"));
    }

    @Test
    public void collapsesWhitespace() {
        String text = HtmlTextExtractor.toPlainText("<p>a</p>\n\n   <p>b</p>");
        assertFalse(text.contains("   "));
        assertTrue(text.contains("a"));
        assertTrue(text.contains("b"));
    }
}
