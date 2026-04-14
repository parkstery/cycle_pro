package com.rtw.pro.foundation.data.auth

import com.google.firebase.firestore.FirebaseFirestore
import com.rtw.pro.foundation.domain.auth.UserProfileGateway
import com.rtw.pro.foundation.domain.model.UserProfile

interface FirestoreProfileStore {
    fun upsertUserProfile(profile: UserProfile)
}

class FirebaseFirestoreProfileStore : FirestoreProfileStore {
    override fun upsertUserProfile(profile: UserProfile) {
        val firestore = try {
            FirebaseFirestore.getInstance()
        } catch (_: IllegalStateException) {
            return
        }
        val payload = mapOf(
            "uid" to profile.uid,
            "displayName" to profile.displayName,
            "photoUrl" to profile.photoUrl,
            "createdAt" to profile.createdAt
        )
        firestore.collection("users")
            .document(profile.uid)
            .set(payload)
    }
}

class FirestoreUserProfileGateway(
    private val store: FirestoreProfileStore
) : UserProfileGateway {
    override fun upsert(profile: UserProfile) {
        store.upsertUserProfile(profile)
    }
}
