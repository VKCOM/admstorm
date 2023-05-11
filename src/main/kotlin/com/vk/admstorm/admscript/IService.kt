package com.vk.admstorm.admscript

import com.vk.admstorm.admscript.utils.DataResponse

interface IService<TModel> {
    fun execCommand(keyName: String): DataResponse<TModel>?
}
