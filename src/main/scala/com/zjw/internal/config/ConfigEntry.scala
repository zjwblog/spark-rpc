package com.zjw.internal.config

/**
 * Created by zjwblog<co.zjwblog@gmail.com> on 2021/5/28
 */
private[zjw] abstract class ConfigEntry[T] (
                                               val key: String,
                                               val alternatives: List[String],
                                               val valueConverter: String => T,
                                               val stringConverter: T => String,
                                               val doc: String,
                                               val isPublic: Boolean) {

  import ConfigEntry._

  registerEntry(this)

  def defaultValueString: String

  protected def readString(reader: ConfigReader): Option[String] = {
    alternatives.foldLeft(reader.get(key))((res, nextKey) => res.orElse(reader.get(nextKey)))
  }

  def readFrom(reader: ConfigReader): T

  def defaultValue: Option[T] = None

  override def toString: String = {
    s"ConfigEntry(key=$key, defaultValue=$defaultValueString, doc=$doc, public=$isPublic)"
  }
}

private class ConfigEntryWithDefault[T] (
                                          key: String,
                                          alternatives: List[String],
                                          _defaultValue: T,
                                          valueConverter: String => T,
                                          stringConverter: T => String,
                                          doc: String,
                                          isPublic: Boolean)
  extends ConfigEntry(key, alternatives, valueConverter, stringConverter, doc, isPublic) {

  override def defaultValue: Option[T] = Some(_defaultValue)

  override def defaultValueString: String = stringConverter(_defaultValue)

  def readFrom(reader: ConfigReader): T = {
    readString(reader).map(valueConverter).getOrElse(_defaultValue)
  }
}

private class ConfigEntryWithDefaultFunction[T] (
                                                  key: String,
                                                  alternatives: List[String],
                                                  _defaultFunction: () => T,
                                                  valueConverter: String => T,
                                                  stringConverter: T => String,
                                                  doc: String,
                                                  isPublic: Boolean)
  extends ConfigEntry(key, alternatives, valueConverter, stringConverter, doc, isPublic) {

  override def defaultValue: Option[T] = Some(_defaultFunction())

  override def defaultValueString: String = stringConverter(_defaultFunction())

  def readFrom(reader: ConfigReader): T = {
    readString(reader).map(valueConverter).getOrElse(_defaultFunction())
  }
}

private class ConfigEntryWithDefaultString[T] (
                                                key: String,
                                                alternatives: List[String],
                                                _defaultValue: String,
                                                valueConverter: String => T,
                                                stringConverter: T => String,
                                                doc: String,
                                                isPublic: Boolean)
  extends ConfigEntry(key, alternatives, valueConverter, stringConverter, doc, isPublic) {

  override def defaultValue: Option[T] = Some(valueConverter(_defaultValue))

  override def defaultValueString: String = _defaultValue

  def readFrom(reader: ConfigReader): T = {
    val value = readString(reader).getOrElse(reader.substitute(_defaultValue))
    valueConverter(value)
  }
}


/**
 * A config entry that does not have a default value.
 */
private[zjw] class OptionalConfigEntry[T](
                                             key: String,
                                             alternatives: List[String],
                                             val rawValueConverter: String => T,
                                             val rawStringConverter: T => String,
                                             doc: String,
                                             isPublic: Boolean)
  extends ConfigEntry[Option[T]](key, alternatives,
    s => Some(rawValueConverter(s)),
    v => v.map(rawStringConverter).orNull, doc, isPublic) {

  override def defaultValueString: String = ConfigEntry.UNDEFINED

  override def readFrom(reader: ConfigReader): Option[T] = {
    readString(reader).map(rawValueConverter)
  }
}

/**
 * A config entry whose default value is defined by another config entry.
 */
private[zjw] class FallbackConfigEntry[T] (
                                              key: String,
                                              alternatives: List[String],
                                              doc: String,
                                              isPublic: Boolean,
                                              val fallback: ConfigEntry[T])
  extends ConfigEntry[T](key, alternatives,
    fallback.valueConverter, fallback.stringConverter, doc, isPublic) {

  override def defaultValueString: String = s"<value of ${fallback.key}>"

  override def readFrom(reader: ConfigReader): T = {
    readString(reader).map(valueConverter).getOrElse(fallback.readFrom(reader))
  }
}

private[zjw] object ConfigEntry {

  val UNDEFINED = "<undefined>"

  private val knownConfigs = new java.util.concurrent.ConcurrentHashMap[String, ConfigEntry[_]]()

  def registerEntry(entry: ConfigEntry[_]): Unit = {
    val existing = knownConfigs.putIfAbsent(entry.key, entry)
    require(existing == null, s"Config entry ${entry.key} already registered!")
  }

  def findEntry(key: String): ConfigEntry[_] = knownConfigs.get(key)

}
