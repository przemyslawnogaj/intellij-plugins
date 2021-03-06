package org.jetbrains.vuejs.language

import com.intellij.lang.HtmlScriptContentProvider
import com.intellij.lang.Language
import com.intellij.lexer.HtmlHighlightingLexer
import com.intellij.psi.tree.IElementType
import com.intellij.psi.xml.XmlTokenType

class VueHighlightingLexer : HtmlHighlightingLexer(), VueHandledLexer {
  private var seenTemplate:Boolean = false

  init {
    registerHandler(XmlTokenType.XML_NAME, VueLangAttributeHandler())
    registerHandler(XmlTokenType.XML_NAME, VueTemplateTagHandler())
    registerHandler(XmlTokenType.XML_TAG_END, VueTagClosedHandler())
    val scriptCleaner = VueTemplateCleaner()
    registerHandler(XmlTokenType.XML_END_TAG_START, scriptCleaner)
    registerHandler(XmlTokenType.XML_EMPTY_ELEMENT_END, scriptCleaner)
  }

  override fun getTokenType(): IElementType? {
    val type = super.getTokenType()
    if (type == XmlTokenType.TAG_WHITE_SPACE && baseState() == 0) return XmlTokenType.XML_REAL_WHITE_SPACE
    return type
  }

  override fun findScriptContentProvider(mimeType: String?): HtmlScriptContentProvider? {
    val type = super.findScriptContentProvider(mimeType ?: "text/ecmascript-6")
    return type ?: scriptContentViaLang()
  }

  override fun getStyleLanguage(): Language? {
    return styleViaLang(ourDefaultStyleLanguage) ?: super.getStyleLanguage()
  }

  override fun seenScript() = seenScript
  override fun seenStyle() = seenStyle
  override fun seenTemplate() = seenTemplate
  override fun seenTag() = seenTag
  override fun seenAttribute() = seenAttribute
  override fun getScriptType() = scriptType
  override fun getStyleType() = styleType
  override fun inTagState(): Boolean = isHtmlTagState(baseState())

  override fun setSeenScriptType() {
    seenContentType = true
  }

  override fun setSeenScript() {
    seenScript = true
  }

  override fun setSeenStyleType() {
    seenStylesheetType = true
  }

  override fun setSeenTemplate(template:Boolean) {
    seenTemplate = template
  }

  override fun setSeenTag(tag: Boolean) {
    seenTag = tag
  }

  override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
    seenTemplate = initialState and VueTemplateTagHandler.SEEN_TEMPLATE != 0
    super.start(buffer, startOffset, endOffset, initialState)
  }

  override fun getState(): Int {
    val state = super.getState()
    return state or if (seenTemplate) VueTemplateTagHandler.SEEN_TEMPLATE else 0
  }

  override fun endOfTheEmbeddment(name:String?):Boolean {
    return super.endOfTheEmbeddment(name) ||
           seenTemplate && "template" == name
  }

  private fun baseState() = state and BASE_STATE_MASK
}

