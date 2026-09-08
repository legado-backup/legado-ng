package io.legado.app.model.login

import io.legado.app.data.entities.BookSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginUiV2Test {

    @Test
    fun parsesDynamicRowsAndActions() {
        val rows = LoginUiV2.parseRender(
            """{"rows":[
                {"key":"phone","name":"手机号","type":"text","hint":"11位"},
                {"name":"说明","type":"label"},
                {"key":"line","name":"线路","type":"select","options":["主","备"]},
                {"key":"remember","name":"记住","type":"toggle","value":"true"},
                {"name":"发码","type":"button","action":"send","countdown":60}
            ]}"""
        )
        assertEquals(5, rows!!.size)
        assertEquals("11位", rows[0].hint)
        assertEquals(listOf("主", "备"), rows[2].options)

        val command = LoginUiV2.parseActionResult(
            """{"state":{"step":"code"},"error":{"phone":"格式错误"},
                "login":{"token":"t"},"close":true,"unknown":1}"""
        )
        assertEquals("""{"step":"code"}""", command.stateJson)
        assertEquals("格式错误", command.error!!["phone"])
        assertEquals("""{"token":"t"}""", command.loginJson)
        assertTrue(command.close)
        assertEquals(listOf("unknown"), command.unknownKeys)
    }

    @Test
    fun rejectsAmbiguousRowsAndMalformedCommands() {
        assertTrue(LoginUiV2.isV2(LoginUiV2.MARKER))
        assertFalse(LoginUiV2.isV2("[]"))
        assertNull(
            LoginUiV2.parseRender(
                """{"rows":[{"key":"x","name":"甲","type":"text"},
                    {"key":"x","name":"乙","type":"toggle"}]}"""
            )
        )
        assertTrue(LoginUiV2.parseActionResult("not json").malformed)
        assertTrue(LoginUiV2.parseActionResult("""{"close":"false"}""").malformed)
    }

    @Test
    fun singleFileJsLoginUsesTheDedicatedHostFacade() {
        val source = BookSource(
            bookSourceUrl = "https://example.com/single-file-login",
            bookSourceName = "单文件登录测试",
            loginUi = LoginUiV2.MARKER,
            mainJs = """
                function loginUi(state) {
                    return {rows:[{
                        name:typeof java.gzipUtf8ToBase64 + ":" + state.marker,
                        type:"button",
                        action:"refresh"
                    }]};
                }
                function loginAction(action, state, form) {
                    return {state:{
                        action:action,
                        previous:state.previous,
                        value:form.value,
                        host:typeof java.gzipUtf8ToBase64
                    }};
                }
            """.trimIndent(),
        )

        val rows = LoginUiV2.parseRender(
            source.evalLoginUiV2("""{"marker":"ready"}""")
        )
        assertEquals("function:ready", rows!!.single().name)

        val command = LoginUiV2.parseActionResult(
            source.evalLoginActionV2(
                "refresh",
                """{"previous":"old"}""",
                """{"value":"new"}""",
            )
        )
        assertEquals(
            """{"action":"refresh","previous":"old","value":"new","host":"function"}""",
            command.stateJson,
        )
    }

    @Test
    fun declarativeLoginKeepsTheLegacyBaseSourceHost() {
        val source = BookSource(
            bookSourceUrl = "https://example.com/declarative-login",
            bookSourceName = "声明式登录测试",
            loginUi = LoginUiV2.MARKER,
            loginUrl = """
                function loginUi(state) {
                    return {rows:[{name:String(java === source),type:"label"}]};
                }
                function loginAction(action, state, form) { return {}; }
            """.trimIndent(),
        )

        val rows = LoginUiV2.parseRender(source.evalLoginUiV2("{}"))

        assertEquals("true", rows!!.single().name)
    }
}
