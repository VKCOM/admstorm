package com.vk.admstorm.admscript.utils

data class DataResponse<out TModel>(
    val value: TModel? = null,
    val errorMessage: String? = null,
)
