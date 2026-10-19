package com.example.problemfinder.data.authentication.api

/** Account identity and bearer token captured together for an authenticated data request. */
class AuthSession(
    val userId: String,
    val accessToken: String
)
