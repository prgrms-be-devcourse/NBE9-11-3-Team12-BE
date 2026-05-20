package com.rungo.api.global.exception

class CustomException(
    val errorCode: ErrorCode
) : RuntimeException(errorCode.message)