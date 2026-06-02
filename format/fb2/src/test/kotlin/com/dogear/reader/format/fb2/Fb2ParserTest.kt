package com.dogear.reader.format.fb2

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class Fb2ParserTest {

    private val sample = """
        <?xml version="1.0" encoding="UTF-8"?>
        <FictionBook xmlns:l="http://www.w3.org/1999/xlink">
          <description>
            <title-info>
              <genre>sf</genre>
              <author><first-name>Ada</first-name><last-name>Vance</last-name></author>
              <book-title>The Quiet Library</book-title>
              <lang>en</lang>
              <coverpage><image l:href="#cover.jpg"/></coverpage>
            </title-info>
          </description>
          <body>
            <section>
              <title><p>Chapter One</p></title>
              <p>Hello <emphasis>world</emphasis>.</p>
            </section>
          </body>
          <binary id="cover.jpg" content-type="image/jpeg">aGVsbG8=</binary>
        </FictionBook>
    """.trimIndent()

    @Test
    fun metadata_isParsed() {
        val meta = Fb2Parser.metadata(Fb2Parser.parse(sample.toByteArray()))
        assertThat(meta.title).isEqualTo("The Quiet Library")
        assertThat(meta.authors).containsExactly("Ada Vance")
        assertThat(meta.language).isEqualTo("en")
    }

    @Test
    fun cover_resolvesEmbeddedBinary() {
        val doc = Fb2Parser.parse(sample.toByteArray())
        val id = Fb2Parser.coverId(doc)
        assertThat(id).isEqualTo("cover.jpg")
        val (bytes, mime) = Fb2Parser.binary(doc, id!!)!!
        assertThat(bytes.toString(Charsets.UTF_8)).isEqualTo("hello") // base64 of "hello"
        assertThat(mime).isEqualTo("image/jpeg")
    }

    @Test
    fun body_isConvertedToHtml() {
        val html = Fb2Parser.bodyHtml(Fb2Parser.parse(sample.toByteArray()))
        assertThat(html).contains("Hello")
        assertThat(html).contains("<em>world</em>")
        assertThat(html).doesNotContain("<emphasis>")
    }
}
