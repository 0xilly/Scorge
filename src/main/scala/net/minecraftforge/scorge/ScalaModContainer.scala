package net.minecraftforge.scorge

import java.util
import java.util.stream.Collectors

import net.minecraftforge.fml.LifecycleEventProvider.LifecycleEvent
import net.minecraftforge.fml.common.ModContainer
import net.minecraftforge.fml.language.{IModInfo, ModFileScanData}
import net.minecraftforge.fml.loading.FMLLoader
import net.minecraftforge.fml.Logging._
import net.minecraftforge.fml.ModLoadingStage._

import org.apache.commons.lang3.tuple.Pair
import org.apache.logging.log4j.LogManager
import org.objectweb.asm.Type

class ScalaModContainer(info:IModInfo, clazzName:String, mcl:ClassLoader, mfsd:ModFileScanData) extends ModContainer(info) {

  private val LOGGER = LogManager.getLogger("SCORGE")
  triggerMap.put(BEGIN, this.constructMod)
  triggerMap.put(PREINIT, this.preInitMod)

  private var modInstance:Any = _
  private var modClazz:Class[_] = _

  try {
    modClazz = Class.forName(clazzName, true, mcl)
    LOGGER.error(LOADING, "Loaded scala mod clazz {} with {}", modClazz.getName, modClazz.getClassLoader)
  } catch {
    case e: ClassNotFoundException =>
      LOGGER.error(LOADING, "Failed to load clazz {}", clazzName, e)
      throw new RuntimeException(e)
  }

  private def preInitMod(le:LifecycleEvent): Unit = {
    val instanceFeilds:util.List[Pair[String, String]] = mfsd.getAnnotations.stream
      .filter(a => AnyRef.equals(a.getClassType, Type.getType(modClazz)) &&
        AnyRef.equals(a.getClassType, Type.getType(modClazz)))
      .map(a => Pair.of(a.getMemberName, a.getAnnotationData.get("value").asInstanceOf[String]))
      .collect(Collectors.toList())

    instanceFeilds.forEach(f => {
      try {
        val feild = modClazz.getDeclaredField(f.getLeft)
        feild.setAccessible(true)
        feild.set(modInstance, FMLLoader.getModLoader.getModList.getModObjectById(f.getRight).get)
      } catch {
        case e @(_:NoSuchFieldException | IllegalAccessException) =>
          LOGGER.error(LOADING, "Unable to set l {} to mod with id {}", f.getLeft, f.getRight)
      }
    })
  }

  private def constructMod(e:LifecycleEvent): Unit = {
    try {
      LOGGER.debug(LOADING, "Loading mod instance {} of type {}", getModId, modClazz.getName)
      this.modInstance = modClazz.newInstance
      LOGGER.debug(LOADING, "Loaded mod instance {} of type {}", getModId, modClazz.getName)

    } catch {
      case e @(_:InstantiationException | IllegalAccessError) => {
        LOGGER.error(LOADING, "Failed to create mod instance", e)
      }
    }
  }

  /**
    * Does this mod match the supplied mod?
    *
    * @param mod to compare
    * @return if the mod matches
    */
  override def matches(mod:Any): Boolean = mod == modInstance

  /**
    * @return the mod object instance
    */
  override def getMod: Any = modInstance
}
