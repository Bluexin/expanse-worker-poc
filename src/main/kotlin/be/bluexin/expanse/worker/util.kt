package be.bluexin.expanse.worker

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

val JSON = jacksonObjectMapper().apply {
    this.setVisibility(
        this.serializationConfig.defaultVisibilityChecker
            .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
            .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
            .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
            .withCreatorVisibility(JsonAutoDetect.Visibility.NONE)
    )
}
