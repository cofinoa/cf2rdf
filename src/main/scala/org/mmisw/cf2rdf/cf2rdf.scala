package org.mmisw.cf2rdf

import java.io.PrintWriter

import com.typesafe.config.ConfigFactory
import org.apache.jena.system.JenaSystem

/**
 * Main cf2rdf program.
 */
object cf2rdf extends App {
  JenaSystem.init()

  val config = ConfigFactory.load().getConfig("cf2rdf")

  val xmlFilename   = config.getString("xml")
  val namespace     = config.getString("namespace")

  val baseFilename   = xmlFilename.replaceAll("\\.xml$", "")

  val rdfFilename   = baseFilename + ".rdf"
  val statsFilename = baseFilename + ".conv-stats.txt"

  val mapper = if (config.hasPath("nvs")) Some(new OrrNvsMapper(config.getString("nvs"))) else None

  val xmlIn = scala.xml.XML.loadFile(xmlFilename)
  val converter = new Converter(xmlIn, namespace, mapper)
  val model = converter.convert

  def getStats: String = {
    val propsStr = (converter.props map (kv => s"${kv._1}: ${kv._2}")) mkString "; "
    s"""cf2rdf conversion
         |input:  $xmlFilename
         |output: $rdfFilename
         |
         |vocabulary properties from input file:
         | $propsStr
         |
         |conversion stats:
         |$stats
      """.stripMargin
  }

  def saveModel() {
    val writer = model.getWriter("RDF/XML-ABBREV")
    writer.setProperty("showXmlDeclaration", "true")
    writer.setProperty("relativeURIs", "same-document,relative")
    writer.setProperty("xmlbase", namespace)
    writer.write(model, new java.io.FileOutputStream(rdfFilename), null)
  }

  def writeStats(statsStr: String) {
    val pw = new PrintWriter(statsFilename)
    pw.printf(statsStr)
    pw.close()
  }

  saveModel()
  val statsStr = getStats
  writeStats(statsStr)
  println(statsStr)
}
