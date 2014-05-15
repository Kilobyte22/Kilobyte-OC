package li.cil.oc.common.tileentity

import li.cil.oc.api.network.Visibility
import li.cil.oc.{Settings, api}
import net.minecraft.item.ItemStack
import li.cil.oc.api.Driver
import li.cil.oc.common.InventorySlots.Tier
import li.cil.oc.common.InventorySlots
import li.cil.oc.util.ItemUtils
import li.cil.oc.api.driver.{Slot, UpgradeContainer}

class RobotAssembler extends traits.Environment with traits.Inventory with traits.Rotatable {
  val node = api.Network.newNode(this, Visibility.None).
    withConnector().
    create()

  def isAssembling = requiredEnergy > 0

  var robot: Option[ItemStack] = None

  var requiredEnergy = 0.0

  def complexity = items.drop(1).foldLeft(0)((acc, stack) => acc + (Option(api.Driver.driverFor(stack.orNull)) match {
    case Some(driver: UpgradeContainer) => (1 + driver.tier(stack.get)) * 2
    case Some(driver) => 1 + driver.tier(stack.get)
    case _ => 0
  }))

  def maxComplexity = {
    val caseTier = ItemUtils.caseTier(items(0).orNull)
    if (caseTier >= 0) Settings.robotComplexityByTier(caseTier) else 0
  }

  def start() {
    if (!isAssembling && robot.isEmpty && complexity <= maxComplexity) {
      // TODO validate all slots, just in case. never trust a client. never trust minecraft.
      val data = new ItemUtils.RobotData()
      data.energy = 50000
      data.containers = items.take(4).drop(1).collect {
        case Some(item) => item
      }
      data.components = items.drop(4).collect {
        case Some(item) => item
      }
      val stack = api.Items.get("robot").createItemStack(1)
      data.save(stack)
      robot = Some(stack)
      requiredEnergy = Settings.get.robotBaseCost + complexity * Settings.get.robotComplexityCost

      for (slot <- 0 until getSizeInventory) items(slot) = None
      onInventoryChanged()
    }
  }

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    super.updateEntity()
    if (robot.isDefined && world.getWorldTime % Settings.get.tickFrequency == 0) {
      val want = math.max(1, math.min(requiredEnergy, Settings.get.assemblerTickAmount * Settings.get.tickFrequency))
      val remainder = node.changeBuffer(-want)
      requiredEnergy -= want - remainder
      if (requiredEnergy <= 0) {
        setInventorySlotContents(0, robot.get)
        robot = None
        requiredEnergy = 0
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def getInvName = Settings.namespace + "container.RobotAssembler"

  override def getSizeInventory = InventorySlots.assembler(0).length

  override def getInventoryStackLimit = 1

  override def isItemValidForSlot(slot: Int, stack: ItemStack) =
    if (slot == 0) {
      val descriptor = api.Items.get(stack)
      !isAssembling &&
        (descriptor == api.Items.get("case1") ||
          descriptor == api.Items.get("case2") ||
          descriptor == api.Items.get("case3"))
    }
    else {
      val caseTier = ItemUtils.caseTier(items(0).orNull)
      caseTier != Tier.None && {
        val info = InventorySlots.assembler(caseTier)(slot)
        Option(Driver.driverFor(stack)) match {
          case Some(driver) if info.slot != Slot.None && info.tier != Tier.None => driver.slot(stack) == info.slot && driver.tier(stack) <= info.tier
          case _ => false
        }
      }
    }
}
