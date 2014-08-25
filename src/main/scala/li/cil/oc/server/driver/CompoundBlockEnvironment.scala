package li.cil.oc.server.driver

import java.nio.charset.Charset

import com.google.common.hash.Hashing
import li.cil.oc.api.driver
import li.cil.oc.api.network._
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.{OpenComputers, api}
import net.minecraft.nbt.NBTTagCompound

class CompoundBlockEnvironment(val name: String, val environments: (driver.Block, ManagedEnvironment)*) extends ManagedEnvironment {
  // Block drivers with visibility < network usually won't make much sense,
  // but let's play it safe and use the least possible visibility based on
  // the drivers we encapsulate.
  val node = api.Network.newNode(this, (environments.filter(_._2.node != null).map(_._2.node.reachability) ++ Seq(Visibility.None)).max).
    withComponent(name).
    create()

  val updatingEnvironments = environments.map(_._2).filter(_.canUpdate)

  // Force all wrapped components to be neighbor visible, since we as their
  // only neighbor will take care of all component-related interaction.
  for ((_, environment) <- environments) environment.node match {
    case component: Component => component.setVisibility(Visibility.Neighbors)
    case _ =>
  }

  override def canUpdate = environments.exists(_._2.canUpdate)

  override def update() {
    for (environment <- updatingEnvironments) {
      environment.update()
    }
  }

  override def onMessage(message: Message) {}

  override def onConnect(node: Node) {
    if (node == this.node) {
      for ((_, environment) <- environments if environment.node != null) {
        node.connect(environment.node)
      }
    }
  }

  override def onDisconnect(node: Node) {
    if (node == this.node) {
      for ((_, environment) <- environments if environment.node != null) {
        environment.node.remove()
      }
    }
  }

  override def load(nbt: NBTTagCompound) {
    // Ignore existing data if the underlying type is different.
    if (nbt.hasKey("typeHash") && nbt.getLong("typeHash") != typeHash) return
    node.load(nbt)
    for ((driver, environment) <- environments) {
      val name = driver.getClass.getName
      if (nbt.hasKey(name)) {
        try {
          environment.load(nbt.getCompoundTag(name))
        } catch {
          case e: Throwable => OpenComputers.log.warn("A block component of type '%s' (provided by driver '%s') threw an error while loading.".format(environment.getClass.getName, name), e)
        }
      }
    }
  }

  override def save(nbt: NBTTagCompound) {
    nbt.setLong("typeHash", typeHash)
    node.save(nbt)
    for ((driver, environment) <- environments) {
      val name = driver.getClass.getName
      try {
        nbt.setNewCompoundTag(name, environment.save)
      } catch {
        case e: Throwable => OpenComputers.log.warn("A block component of type '%s' (provided by driver '%s') threw an error while saving.".format(environment.getClass.getName, name), e)
      }
    }
  }

  private def typeHash = {
    val hash = Hashing.sha256().newHasher()
    environments.map(_._2.getClass.getName).sorted.foreach(hash.putString(_, Charset.defaultCharset()))
    hash.hash().asLong()
  }
}