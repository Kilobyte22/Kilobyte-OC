package li.cil.oc.client.renderer.tileentity

import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity.Case
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.util.ForgeDirection
import org.lwjgl.opengl.GL11

object CaseRenderer extends TileEntitySpecialRenderer {
  override def renderTileEntityAt(tileEntity: TileEntity, x: Double, y: Double, z: Double, f: Float) {
    RenderState.checkError(getClass.getName + ".renderTileEntityAt: entering (aka: wasntme)")

    val computer = tileEntity.asInstanceOf[Case]
    if (computer.isRunning) {
      GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)

      RenderState.disableLighting()
      RenderState.makeItBlend()
      RenderState.setBlendAlpha(1)

      GL11.glPushMatrix()

      GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5)

      computer.yaw match {
        case ForgeDirection.WEST => GL11.glRotatef(-90, 0, 1, 0)
        case ForgeDirection.NORTH => GL11.glRotatef(180, 0, 1, 0)
        case ForgeDirection.EAST => GL11.glRotatef(90, 0, 1, 0)
        case _ => // No yaw.
      }

      GL11.glTranslatef(-0.5f, 0.5f, 0.501f)
      GL11.glScalef(1, -1, 1)

      bindTexture(Textures.blockCaseFrontOn)
      val t = Tessellator.instance
      t.startDrawingQuads()
      t.addVertexWithUV(0, 1, 0, 0, 1)
      t.addVertexWithUV(1, 1, 0, 1, 1)
      t.addVertexWithUV(1, 0, 0, 1, 0)
      t.addVertexWithUV(0, 0, 0, 0, 0)
      t.draw()

      GL11.glPopMatrix()
      GL11.glPopAttrib()
    }

    RenderState.checkError(getClass.getName + ".renderTileEntityAt: leaving")
  }
}