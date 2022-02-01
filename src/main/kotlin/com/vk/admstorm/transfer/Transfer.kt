package com.vk.admstorm.transfer

enum class TransferType {
    DOWNLOAD, UPLOAD
}

enum class TransferResult {
    SUCCESS,
    FAIL,
    TRANSFERRING,
    CANCELLED
}

data class TransferFileModel(
    var type: TransferType,
    var source: String,
    var target: String,
    var size: Long,
    var transferred: Long = 0,
    var result: TransferResult = TransferResult.TRANSFERRING,
    var exception: String? = null,
) {
    override fun toString() = "Type: ${type.name}, Source: $source, Target: " +
            "$target, Size: $size, Transferred: $transferred, Result: $result, Exception: $exception"
}

interface OnTransferResult {
    fun onResult(result: TransferFileModel)
}
