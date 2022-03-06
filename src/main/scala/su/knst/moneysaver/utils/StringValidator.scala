package su.knst.moneysaver.utils

object StringValidator {
  def apply(text: String, minSymbols: Int, maxSymbols: Int = Int.MaxValue, allowSpaces: Boolean = false): Boolean =
    text.length >= minSymbols && text.length <= maxSymbols && (allowSpaces || !text.contains(' '))

  def apply(minSymbols: Int, maxSymbols: Int, allowSpaces: Boolean, text: String*): Boolean =
    text
      .map(this(_, minSymbols, maxSymbols, allowSpaces))
      .reduce(_ && _)

  def throwInvalid(text: String)(implicit settings: StringValidatorSettings): Unit =
    throwInvalid(text, settings.minSymbols, settings.maxSymbols, settings.allowSpaces)

  def throwInvalid(text: String*)(implicit settings: StringValidatorSettings): Unit =
    if (text.exists(!this(_, settings.minSymbols, settings.maxSymbols, settings.allowSpaces)))
      throw new IllegalArgumentException

  def throwInvalid(text: String, minSymbols: Int, maxSymbols: Int = Int.MaxValue, allowSpaces: Boolean = false): Unit = {
    if (!this(text, minSymbols, maxSymbols, allowSpaces))
      throw new IllegalArgumentException
  }

  def settings(minSymbols: Int, maxSymbols: Int, allowSpaces: Boolean) : StringValidatorSettings =
    new StringValidatorSettings(minSymbols, maxSymbols, allowSpaces)
}

class StringValidatorSettings(val minSymbols: Int, val maxSymbols: Int, val allowSpaces: Boolean)
