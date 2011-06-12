/*  Title:      Tools/jEdit/src/text_painter.scala
    Author:     Makarius

Replacement painter for jEdit text area.
*/

package isabelle.jedit


import isabelle._

import java.awt.{Graphics, Graphics2D}
import java.util.ArrayList

import org.gjt.sp.jedit.Debug
import org.gjt.sp.jedit.syntax.{DisplayTokenHandler, Chunk}
import org.gjt.sp.jedit.textarea.{JEditTextArea, TextAreaExtension, TextAreaPainter}


class Text_Painter(model: Document_Model, text_area: JEditTextArea) extends TextAreaExtension
{
  private val orig_text_painter: TextAreaExtension =
  {
    val name = "org.gjt.sp.jedit.textarea.TextAreaPainter$PaintText"
    text_area.getPainter.getExtensions.iterator.filter(x => x.getClass.getName == name).toList
    match {
      case List(x) => x
      case _ => error("Expected exactly one " + name)
    }
  }


  /* wrap_margin -- cf. org.gjt.sp.jedit.textarea.TextArea.propertiesChanged */

  private def wrap_margin(): Int =
  {
    val buffer = text_area.getBuffer
    val painter = text_area.getPainter
    val font = painter.getFont
    val font_context = painter.getFontRenderContext

    val soft_wrap = buffer.getStringProperty("wrap") == "soft"
    val max = buffer.getIntegerProperty("maxLineLen", 0)

    if (max > 0) font.getStringBounds(" " * max, font_context).getWidth.toInt
    else if (soft_wrap)
      painter.getWidth - (font.getStringBounds(" ", font_context).getWidth.round.toInt) * 3
    else 0
  }


  /* chunks */

  private def line_chunks(physical_lines: Set[Int]): Map[Text.Offset, Chunk] =
  {
    import scala.collection.JavaConversions._

    val buffer = text_area.getBuffer
    val painter = text_area.getPainter
    val margin = wrap_margin().toFloat

    val out = new ArrayList[Chunk]
    val handler = new DisplayTokenHandler

    var result = Map[Text.Offset, Chunk]()
    for (line <- physical_lines) {
      out.clear
      handler.init(painter.getStyles, painter.getFontRenderContext, painter, out, margin)
      buffer.markTokens(line, handler)

      val line_start = buffer.getLineStartOffset(line)
      for (chunk <- handler.getChunkList.iterator)
        result += (line_start + chunk.offset -> chunk)
    }
    result
  }


  var use = false

  override def paintScreenLineRange(gfx: Graphics2D,
    first_line: Int, last_line: Int, physical_lines: Array[Int],
    start: Array[Int], end: Array[Int], y: Int, line_height: Int)
  {
    if (use) {
      Isabelle.swing_buffer_lock(model.buffer) {
        val painter = text_area.getPainter
        val fm = painter.getFontMetrics

        val all_chunks = line_chunks(Set[Int]() ++ physical_lines.iterator.filter(i => i != -1))

        val x0 = text_area.getHorizontalOffset
        var y0 = y + fm.getHeight - (fm.getLeading + 1) - fm.getDescent
        for (i <- 0 until physical_lines.length) {
          if (physical_lines(i) != -1) {
            all_chunks.get(start(i)) match {
              case Some(chunk) =>
                Chunk.paintChunkList(chunk, gfx, x0, y0, !Debug.DISABLE_GLYPH_VECTOR)
              case None =>
            }
          }
          y0 += line_height
        }
      }
    }
    else
      orig_text_painter.paintScreenLineRange(
        gfx, first_line, last_line, physical_lines, start, end, y, line_height)
  }


  /* activation */

  def activate()
  {
    val painter = text_area.getPainter
    painter.removeExtension(orig_text_painter)
    painter.addExtension(TextAreaPainter.TEXT_LAYER, this)
  }

  def deactivate()
  {
    val painter = text_area.getPainter
    painter.removeExtension(this)
    painter.addExtension(TextAreaPainter.TEXT_LAYER, orig_text_painter)
  }
}
