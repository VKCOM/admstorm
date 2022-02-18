package com.vk.admstorm.utils.extensions

import org.jdom.Element

fun Element.writeString(name: String, value: String) {
    val opt = Element("option")
    opt.setAttribute("name", name)
    opt.setAttribute("value", value)
    addContent(opt)
}

fun Element.readString(name: String): String? =
    children
        .find { it.name == "option" && it.getAttributeValue("name") == name }
        ?.getAttributeValue("value")

fun Element.writeBool(name: String, value: Boolean) = writeString(name, value.toString())

fun Element.readBool(name: String): Boolean? = readString(name)?.toBoolean()
