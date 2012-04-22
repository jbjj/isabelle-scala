/*  Title:      Pure/System/main.scala
    Author:     Makarius

Default Isabelle application wrapper.
*/

package isabelle

import scala.swing.TextArea


object Main
{
  def main(args: Array[String]) =
  {
    val (out, rc) =
      try {
        Platform.init_laf()
        Isabelle_System.init()
        Isabelle_System.isabelle_tool("jedit", args: _*)
      }
      catch { case exn: Throwable => (Exn.message(exn), 2) }

    if (rc != 0) {
      val text = new TextArea(out + "\nReturn code: " + rc)
      text.editable = false
      Library.dialog(null, "Isabelle", "Isabelle output", text)
    }

    System.exit(rc)
  }
}

