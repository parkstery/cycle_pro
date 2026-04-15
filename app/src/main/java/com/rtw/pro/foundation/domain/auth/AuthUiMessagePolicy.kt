package com.rtw.pro.foundation.domain.auth

object AuthUiMessagePolicy {
    fun messageFor(error: AuthError): String {
        return when (error) {
            AuthError.Cancelled -> "로그인이 취소되었습니다. 다시 시도해 주세요."
            AuthError.TokenExpired -> "세션이 만료되었습니다. 다시 로그인해 주세요."
            is AuthError.Unknown -> {
                when (error.message) {
                    "app-not-configured" -> "Firebase Auth 설정이 현재 앱과 일치하지 않습니다. google-services.json, Web Client ID, 프로젝트 ID를 같은 Firebase 프로젝트 값으로 맞춰 주세요."
                    "google-no-id-token" ->
                        "Google ID 토큰을 받지 못했습니다. Web Client ID(웹 클라이언트), SHA-1, google-services.json이 같은 Firebase 프로젝트인지 확인해 주세요."
                    "google-invalid-account" ->
                        "Google 계정이 이 앱과 호환되지 않습니다. Play 서비스·계정 상태를 확인하거나 다른 계정으로 시도해 주세요."
                    "network-error" -> "네트워크가 불안정합니다. 연결 상태를 확인해 주세요."
                    "invalid-google-token" -> "로그인 인증이 유효하지 않습니다. 다시 로그인해 주세요."
                    "user-disabled" -> "계정 사용이 제한되었습니다. 고객센터에 문의해 주세요."
                    else -> {
                        if (error.message.startsWith("firebase-sign-in-failed:")) {
                            val payload = error.message.removePrefix("firebase-sign-in-failed:")
                            val raw = payload.substringBefore(":")
                            val detail = payload.substringAfter(":", missingDelimiterValue = "none")
                            if (detail == "none") {
                                "로그인에 실패했습니다. Firebase 코드: $raw"
                            } else {
                                "로그인에 실패했습니다. Firebase 코드: $raw / 상세: $detail"
                            }
                        } else {
                            "로그인에 실패했습니다. 잠시 후 다시 시도해 주세요."
                        }
                    }
                }
            }
        }
    }
}
