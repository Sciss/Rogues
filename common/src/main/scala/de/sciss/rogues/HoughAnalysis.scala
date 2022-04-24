package de.sciss.rogues

import circledetection.Hough_Transform
import ij.ImagePlus
import ij.io.FileInfo

import java.awt.image.BufferedImage
import de.sciss.file.*

import java.awt.{BasicStroke, Color}
import javax.imageio.ImageIO

object HoughAnalysis:
  val houghMinRadius  = 80
  val houghMaxRadius  = 90 // 120
  val houghNumResults = 20 // 10

  def main(args: Array[String]): Unit =
    val base    = userHome / "Documents" / "projects" / "peek2021" / "swap_space" / "scans"
    val dir     = base / "220423_group"
//    val fileIn  = dir / "IMG_0052ovr_eq_scale.jpg"
    val fileIn  = dir / "IMG_0054ovr_eq_scale-hpf.jpg"
    val fileOut = fileIn.replaceName(s"${fileIn.base}-hough.jpg")
    run(fileIn, fileOutOption = Some(fileOut))

  def run(fileIn: File, fileOutOption: Option[File]): Unit =
    val imgIn0 = ImageIO.read(fileIn)

    def toImageJ(in: BufferedImage): ImagePlus =
      val res = new ImagePlus(fileIn.name, in)
      val fi  = new FileInfo
      fi.fileFormat = FileInfo.IMAGEIO
      fi.fileName   = fileIn.name
      fileIn.parentOption.foreach { p => fi.directory = p.path + File.sep }
      res.setFileInfo(fi)
      res

    val imgInS = imgIn0
    val imgInSP: ImagePlus = toImageJ(imgInS)

    val wS      = imgInSP.getWidth
    val hS      = imgInSP.getHeight
    println(s"Hough input size $wS, $hS")

    val procInS = imgInSP.getProcessor
    val ht      = new Hough_Transform()
    ht.setParameters(houghMinRadius, houghMaxRadius, houghNumResults)
    val htRes: Array[Array[Int]] = ht.runHeadless(procInS)
    val circles = htRes.iterator.map {
      case Array(x, y, r, a) => (x, y, r, a) // Point2D(x / houghScale, y / houghScale)
    } .toList

    println(s"# results: ${circles.size}")
    println(circles.mkString("\n"))
    println("Done.")

    val minAcc = if circles.isEmpty then 0 else circles.map(_._4).min
    val maxAcc = if circles.isEmpty then 1 else circles.map(_._4).max

    fileOutOption.foreach { fileOut =>
      val g = imgInS.createGraphics()
      g.setColor(Color.red)
//      g.setStroke(new BasicStroke(3f))
      circles.foreach { case (x, y, r, a) =>
        import de.sciss.numbers.Implicits.*
        val w = a.linLin(minAcc, maxAcc, 1f, 6f)
        g.setStroke(new BasicStroke(w))
        g.drawOval(x - r, y - r, r * 2, r * 2)
      }
      val fmt = if Seq("jpg", "jpeg").contains(fileOut.extL) then "jpg" else "png"
      ImageIO.write(imgInS, fmt, fileOut)
      g.dispose()
    }
