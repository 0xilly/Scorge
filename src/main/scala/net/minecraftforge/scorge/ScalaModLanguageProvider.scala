package net.minecraftforge.scorge

import java.util.function.{Consumer, Function}
import java.util.stream.Collectors

import net.minecraftforge.fml.Logging.{SCAN, fmlLog}
import net.minecraftforge.fml.common.ModContainer
import net.minecraftforge.fml.language.IModLanguageProvider.IModLanguageLoader
import net.minecraftforge.fml.language.{IModInfo, IModLanguageProvider, ModFileScanData}

import org.objectweb.asm.Type

import scala.beans.BeanProperty

object ScalaModLanguageProvider {

  class ScorgeModTarget (@BeanProperty entryClass:String, @BeanProperty modId:String) extends IModLanguageLoader {

    override def loadMod(info:IModInfo, mcl:ClassLoader, mfsd:ModFileScanData): ModContainer = new ScalaModContainer(info, entryClass, mcl, mfsd)

  }

  val SCODANNOTAION: Type = Type.getType("Lnet/minecraftforge/scorge/Scod")

}

class ScalaModLanguageProvider extends IModLanguageProvider {

  //Git rid of this once I have access to the modfile properties

  override def name:String = "scalaforge"

  import net.minecraftforge.scorge.ScalaModLanguageProvider.ScorgeModTarget
  import net.minecraftforge.scorge.ScalaModLanguageProvider.SCODANNOTAION
  override def getFileVisitor: Consumer[ModFileScanData] = scan => {
    val canidates = scan.getAnnotations.stream()
      .filter(targ => targ.getAnnotationType.equals(SCODANNOTAION))
      .peek(targ => fmlLog.debug(SCAN, "Found @Scod class {} with id {}", targ.getClassType.getClassName, targ.getAnnotationData.get("id")))
      .map(targ =>  new ScorgeModTarget(targ.getClassType.getClassName, targ.getAnnotationData.get("id").asInstanceOf[String]))
      .collect(Collectors.toMap(ScorgeModTarget.getModId(), Function.identity()))
    scan.addLanguageLoader(canidates)

  }

}


