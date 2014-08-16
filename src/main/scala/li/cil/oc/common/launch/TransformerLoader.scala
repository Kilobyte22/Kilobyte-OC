package li.cil.oc.common.launch

import java.util

import cpw.mods.fml.relauncher.IFMLLoadingPlugin
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.{MCVersion, TransformerExclusions}
import li.cil.oc.common.asm.ClassTransformer

@TransformerExclusions(Array("li.cil.oc.common.asm"))
@MCVersion("1.7.10")
class TransformerLoader extends IFMLLoadingPlugin {
  override def getAccessTransformerClass = null

  override def getASMTransformerClass = Array(classOf[ClassTransformer].getName)

  override def getModContainerClass = null

  override def getSetupClass = null

  override def injectData(data: util.Map[String, AnyRef]) {}
}
