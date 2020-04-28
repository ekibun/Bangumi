package soko.ekibun.bangumi

import org.junit.Test
import soko.ekibun.bangumi.util.HtmlUtil

class SpanTest {

    @Test
    fun test() {
        val html = """test<br />
<span style="font-weight:bold;">Bold</span>
 <span style="font-style:italic">Italic</span> <span style="text-decoration: underline;">underline</span> <span style="text-decoration: line-through;">remove</span> <br />
<span style="background-color:#555;color:#555;border:1px solid #555;">mask</span><br />
<span style="color: #FF0000;">red</span><br />
<span style="font-size:18px; line-height:18px;">size</span><br />
<a href="http://bgm.tv" target="_blank" rel="nofollow external noopener" class="l">url</a><br />
<img src="http://chii.in/img/ico/bgm88-31.gif" class="code" alt="" /><br />
<li> list item<br />
line break<br />
<li> list item<div class="quote"><q><span style="text-decoration: underline;">quote</span><br />
<div class="quote"><q>new line</q></div></q></div><div class="codeHighlight"><pre>
1
2
3
4
  code<br />
&nbsp; &nbsp; &lt;a&gt;&lt;/a&gt;</pre></div>"""
        val span = HtmlUtil.html2span(html)
        println(HtmlUtil.span2bbcode(span))
    }
}