package com.vk.admstorm.admscript.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer

@Serializer(forClass = DataResponse::class)
class DataResponseSerializer<TModel : Any>(private val dataSerializer: KSerializer<TModel>) : KSerializer<DataResponse<TModel>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("DataResponseSerializer") {
        val dataDescriptor = dataSerializer.descriptor.nullable
        element("value", dataDescriptor)
        element("error_message", dataSerializer.descriptor.nullable)
    }

    override fun deserialize(decoder: Decoder): DataResponse<TModel> {
        val inp = decoder.beginStructure(descriptor)
        var value: TModel? = null
        var error: String? = null
        loop@ while (true) {
            when (val i = inp.decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break@loop
                0 -> value = inp.decodeNullableSerializableElement(descriptor, i, dataSerializer.nullable)
                1 -> error = inp.decodeNullableSerializableElement(descriptor, i, serializer())
                else -> throw SerializationException("Unknown index $i")
            }
        }
        inp.endStructure(descriptor)
        return DataResponse(value, error)
    }

    override fun serialize(encoder: Encoder, value: DataResponse<TModel>) {
        encoder.beginStructure(descriptor).apply {
            encodeNullableSerializableElement(descriptor, 0, dataSerializer, value.value)
            encodeNullableSerializableElement(descriptor, 1, serializer<String?>(), value.errorMessage)
            endStructure(descriptor)
        }
    }
}
