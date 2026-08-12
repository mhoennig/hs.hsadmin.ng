package net.hostsharing.hsadminng.hs.scenarios;

import org.junit.jupiter.api.Test;

import static net.hostsharing.hsadminng.hs.scenarios.MarkdownTableCellRenderer.toMarkdownTableCell;
import static org.assertj.core.api.Assertions.assertThat;

class MarkdownTableCellRendererUnitTest {

    @Test
    void rendersEmptyValueAsEmptyCell() {
        assertThat(toMarkdownTableCell("")).isEmpty();
    }

    @Test
    void escapesMarkdownAndHtmlSpecialCharacters() {
        assertThat(toMarkdownTableCell("a & <b> | c")).isEqualTo("a &amp; &lt;b&gt; &#124; c");
    }

    @Test
    void joinsLinesWithBrKeepingAllLineBreakFlavors() {
        assertThat(toMarkdownTableCell("one\ntwo\r\nthree\rfour")).isEqualTo("one<br>two<br>three<br>four");
    }

    @Test
    void rendersLeadingIndentAsNbspWithTabsCountingDouble() {
        assertThat(toMarkdownTableCell("  x\n\ty")).isEqualTo("&nbsp;&nbsp;x<br>&nbsp;&nbsp;y");
    }

    @Test
    void escapesOnlyAfterTheIndent() {
        assertThat(toMarkdownTableCell(" <x> ")).isEqualTo("&nbsp;&lt;x&gt; ");
    }
}
