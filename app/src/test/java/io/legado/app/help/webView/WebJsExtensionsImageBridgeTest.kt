package io.legado.app.help.webView

import org.junit.Assert.assertTrue
import org.junit.Test

class WebJsExtensionsImageBridgeTest {

    @Test
    fun injectedBridgeContainsAsyncImageHelpers() {
        val injection = WebJsExtensions.JS_INJECTION

        assertTrue(injection.contains("function imageToPngDataUrlAwait(url, maxDimension)"))
        assertTrue(injection.contains("java.request(\"imageToPngDataUrlAwait\""))
        assertTrue(injection.contains("maxDimension == null ? null : String(maxDimension)"))
        assertTrue(injection.contains("function imageAssetDataUrlAwait(path)"))
        assertTrue(injection.contains("java.request(\"imageAssetDataUrlAwait\""))
    }
}
