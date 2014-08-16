package li.cil.oc.server.fs

import java.io.{FileNotFoundException, IOException}
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel

import li.cil.oc.api
import li.cil.oc.api.fs.Mode
import net.minecraft.nbt.{NBTTagCompound, NBTTagList}
import net.minecraftforge.common.util.Constants.NBT

import scala.collection.mutable

trait InputStreamFileSystem extends api.fs.FileSystem {
  private val handles = mutable.Map.empty[Int, Handle]

  // ----------------------------------------------------------------------- //

  override def isReadOnly = true

  override def delete(path: String) = false

  override def makeDirectory(path: String) = false

  override def rename(from: String, to: String) = false

  override def setLastModified(path: String, time: Long) = false

  // ----------------------------------------------------------------------- //

  override def open(path: String, mode: Mode) = this.synchronized(if (mode == Mode.Read && exists(path) && !isDirectory(path)) {
    val handle = Iterator.continually((Math.random() * Int.MaxValue).toInt + 1).filterNot(handles.contains).next()
    openInputChannel(path) match {
      case Some(channel) =>
        handles += handle -> new Handle(this, handle, path, channel)
        handle
      case _ => throw new FileNotFoundException()
    }
  } else throw new FileNotFoundException())

  override def getHandle(handle: Int): api.fs.Handle = this.synchronized(handles.get(handle).orNull)

  override def close() = this.synchronized {
    for (handle <- handles.values)
      handle.close()
    handles.clear()
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    val handlesNbt = nbt.getTagList("input", NBT.TAG_COMPOUND)
    (0 until handlesNbt.tagCount).map(handlesNbt.getCompoundTagAt).foreach(handleNbt => {
      val handle = handleNbt.getInteger("handle")
      val path = handleNbt.getString("path")
      val position = handleNbt.getLong("position")
      openInputChannel(path) match {
        case Some(channel) =>
          val fileHandle = new Handle(this, handle, path, channel)
          channel.position(position)
          handles += handle -> fileHandle
        case _ => // The source file seems to have disappeared since last time.
      }
    })
  }

  override def save(nbt: NBTTagCompound) = this.synchronized {
    val handlesNbt = new NBTTagList()
    for (file <- handles.values) {
      assert(file.channel.isOpen)
      val handleNbt = new NBTTagCompound()
      handleNbt.setInteger("handle", file.handle)
      handleNbt.setString("path", file.path)
      handleNbt.setLong("position", file.position)
      handlesNbt.appendTag(handleNbt)
    }
    nbt.setTag("input", handlesNbt)
  }

  // ----------------------------------------------------------------------- //

  protected def openInputChannel(path: String): Option[InputChannel]

  protected trait InputChannel extends ReadableByteChannel {
    def isOpen: Boolean

    def close()

    def position: Long

    def position(newPosition: Long): Long

    def read(dst: Array[Byte]): Int

    override def read(dst: ByteBuffer) = {
      if (dst.hasArray) {
        read(dst.array())
      }
      else {
        val count = math.max(0, dst.limit - dst.position)
        val buffer = new Array[Byte](count)
        val n = read(buffer)
        if (n > 0) dst.put(buffer, 0, n)
        n
      }
    }
  }

  protected class InputStreamChannel(val inputStream: java.io.InputStream) extends InputChannel {
    var isOpen = true

    private var position_ = 0L

    override def close() = if (isOpen) {
      isOpen = false
      inputStream.close()
    }

    override def position = position_

    override def position(newPosition: Long) = {
      inputStream.reset()
      position_ = inputStream.skip(newPosition)
      position_
    }

    override def read(dst: Array[Byte]) = {
      val read = inputStream.read(dst)
      position_ += read
      read
    }
  }

  // ----------------------------------------------------------------------- //

  private class Handle(val owner: InputStreamFileSystem, val handle: Int, val path: String, val channel: InputChannel) extends api.fs.Handle {
    override def position = channel.position

    override def length = owner.size(path)

    override def close() = if (channel.isOpen) {
      owner.handles -= handle
      channel.close()
    }

    override def read(into: Array[Byte]) = channel.read(into)

    override def seek(to: Long) = channel.position(to)

    override def write(value: Array[Byte]) = throw new IOException("bad file descriptor")
  }

}
